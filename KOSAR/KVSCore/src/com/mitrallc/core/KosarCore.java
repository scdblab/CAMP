package com.mitrallc.core;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;

import com.mitrallc.common.DynamicArray;
import com.mitrallc.sqltrig.QueryToTrigger;
import com.mitrallc.util.ShutdownHook;
import com.mitrallc.util.SocketIO;
import com.mitrallc.webserver.BaseHttpServer;
import com.mitrallc.webserver.CoreHttpHandler;
import com.mitrallc.webserver.EventMonitor;
import com.mitrallc.webserver.Last100SQLQueries;

public class KosarCore extends Thread {

	static Driver driver = null;
	private static KosarCore core = null;
	private ServerSocket server = null;
	private static Connection conn = null;

	// Maps ClientID to InetAddress (ByteBuffer to ByteBuffer)
	public static ConcurrentHashMap<Object,Object> clientToIPMap = new ConcurrentHashMap<Object,Object>();
	
	// Maps ClientID to Invalidation Ports (ByteBuffer to Byte Array)
	public static ConcurrentHashMap<Object,Object> clientToPortsMap = new ConcurrentHashMap<Object,Object>();
	
	// Maps ClientID to Timestamp (ByteBuffer to Long)
	public static ConcurrentHashMap<Object,Object> pingClientsMap = new ConcurrentHashMap<Object,Object>();

	// Maps Internal Token to Array of Query Strings (ByteBuffer to [ByteBuffer])
	public static ConcurrentHashMap<Object,Object> internalTokenToQueriesMap = new ConcurrentHashMap<Object,Object>();
	
	// Maps Query String to Array of Client ID's (ByteBuffer to [ByteBuffer])
	public static ConcurrentHashMap<Object,Object> queryToClientsMap = new ConcurrentHashMap<Object,Object>();
	
	// Maps Trigger to "1" for Unique Triggers.
	public static HashMap<String, String> triggerMap = new HashMap<String, String>();
	
	// Defines how many worker threads are running at initialization time.
	// Number of Worker Threads must make sense with (be a multiple of) the number of divisions.
	// NumWorkerThreads is adjusted at initialization time to:
	// NumWorkerThreads = NumCacheRegDivisions * ((int) NumWorkerThreads/NumCacheRegDivisions )
	public static int NumWorkerThreads = 10;
	
	// Determines size of workers, number of semaphores and the number of request queues to process
	public static final int NumCacheRegDivisions = 3;
	
	public static Vector<RequestHandler> handlers = new Vector<RequestHandler>();
	
	// Vector of Token Cache Worker threads that process cache requests as they come in to the CORE.
	// Number of Workers, NumCacheRegDivisions, is determined at declaration time.
	public static Vector<TokenCacheWorker> tokenCacheWorkers = new Vector<TokenCacheWorker>();
		
	public static HashMap<String,Integer> invalidationPorts = new HashMap<String,Integer>();
	public static Vector<SocketIO> invalidationSockets = new Vector<SocketIO>();
	
	// Released every time a key comes in to be cached or validated
	// Number of Semaphores, NumCacheRegDivisions, is determined at declaration time.
	public static Vector<Semaphore> tokenCachedWorkToDo = new Vector<Semaphore>();
		
	// Acquired and released to keep access to a given query string unique,
	// while retaining the efficiency of the multi-threaded system.  Non-intrusive.
	// For the data structure queryToClientsMap
	public static Vector<Semaphore> querySemaphores = new Vector<Semaphore>();
	
	// Acquired and released to keep access to a given internal token unique.
	public static Vector<Semaphore> internalTokenSemaphores = new Vector<Semaphore>();
	
	// Number of querySemaphores to be initialized.  
	// This value is used for the hash function
	private static int numProtectionSemaphores = 101;
	
	// Vector containing Queues of requests to be processed.  
	// Added by RequestHandler, removed by TokenCacheWorker
	// Number of Vectors, NumCacheRegDivisions, is determined at initialization time.
	public static Vector<ConcurrentLinkedQueue<Object>> requestsToProcess = new Vector<ConcurrentLinkedQueue<Object>>();
	
	// When the CORE is turned off, the workToDo Vector will be cleared
	// and the tokenCachedWorkToDo semaphore will be released n times (number of token cache workers).
	// This variable will turn to false and end the loop.
	public static boolean coreWorking = true;
	
	// Custom Array that holds the keys received from SQLTrig. They are removed
	// as soon as they are older than the oldest transaction in the system.
	public static DynamicArray keyQueue = new DynamicArray();
	
	// Map that holds the list of all current transactions.
	//private TreeMap<ByteBuffer, TransactionDataItem> pendingTransactionList = new TreeMap<ByteBuffer, TransactionDataItem>();

	public static Last100SQLQueries last100readQueries = new Last100SQLQueries();
	
	// Atomic Long counter that acts as a transaction order manager.
	// Only incremented; Note; When Atomic Long exceeds Long Max
	// somehow must implement loop around.
	public static AtomicLong AL = new AtomicLong(0);
	
	// This lock protects the keys that are read from the TriggerListener
	// and are managed by the KeyInvalidation Handler.
	public static final ReadWriteLock keyQueueRWLock = new ReentrantReadWriteLock();
	public static final Lock KEY_QUEUE_READ_LOCK = keyQueueRWLock.readLock();
	public static final Lock KEY_QUEUE_WRITE_LOCK = keyQueueRWLock.writeLock();
	
	//This lock protects the transactions so that keys from one transaction 
	//may not be removed by another transaction.  This makes sure
	//keys are not lost when many threads are invalidating IT's.
	public static final ReadWriteLock pendingTransactionQueueRWLock = new ReentrantReadWriteLock();
	public static final Lock PENDING_TRANSACTION_READ_LOCK = pendingTransactionQueueRWLock.readLock();
	public static final Lock PENDING_TRANSACTION_WRITE_LOCK = pendingTransactionQueueRWLock.writeLock();
	
	//Clients connect to the CORE through this port, 
	//which is defined in the Client's config file.
	private static int CLIENT_CONNECT_PORT = 53137;
	
	//Variables read in from the CORE's config file.
	private static final String DB_PORT = "dbport";
	private static final String WEBSERVERPORT = "webserverport";
	private static final String RDBMS = "rdbms";
	private static final String RDBMSDriver = "rdbmsdriver";
	private static final String DB_URL = "url";
	private static final String DB_USER = "dbuser";
	private static final String DB_PASSWORD = "dbpassword";
	private static final String NUM_REPLICAS = "numreplicas";
	private static final String CLIENTREPLICACOMMAND = "clientreplicacommand";
	
	private static final String cfgfile = "kosarcore.cfg";
	private static String CONFIG_FILE = "./"+cfgfile;
	
	public static int copyCount = 0;
	public static int stealCount = 0;
	public static int consumeCount = 0;

	public static Random random = new Random();
	
	public static BaseHttpServer webserver = null;
	private static int db_port = -1;
	private static int webserverPort = -1;
	private static String rdbmstype = null;
	private static String rdbmsdriver = null;
	private static String db_url = null;
	private static String db_user = null;
	private static String db_password = null;
	private static int num_replicas = -1;
	private static String clientReplicaCommand = null;

	static TriggerRegisterThread triggerRegThread = null;
	static CoreTriggerListener triggerListener = null;
	
	public static HashMap<ByteBuffer, AtomicInteger> triggersRegPerClient = new HashMap<ByteBuffer, AtomicInteger>();
	public static HashMap<ByteBuffer, EventMonitor> requestRateEventMonitor = new HashMap<ByteBuffer, EventMonitor>();
	public static HashMap<ByteBuffer, AtomicInteger> numQueryInstances = new HashMap<ByteBuffer, AtomicInteger>();
	
	/**
	 * The CORE constructor loads the configuration file, connects to the database,
	 * starts the trigger registration thread, and begins the triggerRegister worker threads.
	 */
	public KosarCore() {
		
		loadConfig();
		connectToRDBMS();

		if(webserverPort != -1) {
			webserver = new BaseHttpServer(webserverPort, "CORE", new CoreHttpHandler());
			System.out.println("Webserver established at port " + webserverPort);
		}
		// Start Trigger Registration Thread
		triggerRegThread = new TriggerRegisterThread(conn);
		new Thread(triggerRegThread, "TriggerRegThread").start();
		
		//Start Trigger Listener Thread
		triggerListener = new CoreTriggerListener();
		new Thread(triggerListener, "TriggerListener").start();
		
		//Make sure number of worker threads is a multiple of the divisions.
		int divisions = NumWorkerThreads/NumCacheRegDivisions;
		NumWorkerThreads = divisions * NumCacheRegDivisions;
		
		//establishes the querySemaphores used to manipulate queryToClientsMap.
		for(int i = 0; i < numProtectionSemaphores; i++) {
			querySemaphores.add(new Semaphore(1, true));
			internalTokenSemaphores.add(new Semaphore(1,true));
		}
		
		//There is a semaphore, a request queue and list of workers for each
		//division.
		int j = 0;
		for(int i = 0; i < NumCacheRegDivisions; i++) {
			Semaphore semaphore = new Semaphore(0, true);
			tokenCachedWorkToDo.add(semaphore);
			requestsToProcess.add((ConcurrentLinkedQueue<Object>)new ConcurrentLinkedQueue<Object>());
			for(;j < (i+1)*divisions; j++) {
				tokenCacheWorkers.add(new TokenCacheWorker(i));
			}
		}
		for(int i = 0; i < NumWorkerThreads; i++){
			new Thread(tokenCacheWorkers.get(i), "TokenCacheWorker" + i).start();
		}
	}

	/**
	 * Reads in Configuration File. Each entry is handled specifically,
	 * as each is critical in the CORE's functionality.
	 */
	private static void loadConfig() {
		try {
			// load server details
			Properties serverProperties = new Properties();
			
			String sysEnvStr = System.getenv("KOSAR_HOME");
			if (sysEnvStr != null){
				sysEnvStr = sysEnvStr.trim();
				if (! sysEnvStr.endsWith(cfgfile)) CONFIG_FILE = sysEnvStr+cfgfile;
			}
		
			serverProperties.load(new FileInputStream(new File(CONFIG_FILE)));

			rdbmstype = serverProperties.getProperty(RDBMS);
			if (rdbmstype != null && rdbmstype.compareToIgnoreCase("mysql")==0){
				QueryToTrigger.setTargetSystem(QueryToTrigger.RDBMS.MySQL);
			} else if (rdbmstype != null && rdbmstype.compareToIgnoreCase("oracle")==0) {
				QueryToTrigger.setTargetSystem(QueryToTrigger.RDBMS.Oracle);
			} else {
				System.out.println("KosarCore ERROR:  RDBMS type is either not defined or unknown.");
				System.out.println("KosarCore Suggested Fix:  Specify an rdbms tag with a target RDBMS in the configuration file, e.g., rdbms=mysql");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.exit(-1);
			}
			
			rdbmsdriver = serverProperties.getProperty(RDBMSDriver);
			if (rdbmsdriver == null) {
				System.out
						.println("KosarCore ERROR:  RDBMS driver is not defined.");
				System.out
						.println("KosarCore Suggested Fix:  Specify an rdbms driver in the configuration file, e.g., rdbmsdriver=com.mysql.jdbc.Driver");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.exit(-1);
			}

			db_port = Integer
					.valueOf(serverProperties.getProperty(DB_PORT) == null ? "0"
							: serverProperties.getProperty(DB_PORT));
			if (db_port <= 0) {
				System.out
						.println("KosarCore ERROR:  RDBMS port is not defined or invalid.");
				System.out
						.println("KosarCore Suggested Fix:  Specify an rdbms port in the configuration file, e.g., dbport=1521");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.exit(-1);
			}

			db_url = serverProperties.getProperty(DB_URL);
			if (db_url == null) {
				System.out
						.println("KosarCore ERROR:  RDBMS url is not defined.");
				System.out
						.println("KosarCore Suggested Fix:  Specify an rdbms url in the configuration file, e.g., url=jdbc:mysql://10.0.1.10:3306");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.exit(-1);
			}

			db_user = serverProperties.getProperty(DB_USER);
			if (db_user == null) {
				System.out
						.println("KosarCore ERROR:  RDBMS username is not defined.");
				System.out
						.println("KosarCore Suggested Fix:  Specify an rdbms username in the configuration file, e.g., dbuser=cosar");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.exit(-1);
			}

			db_password = serverProperties.getProperty(DB_PASSWORD);
			if (db_password == null) {
				System.out
						.println("KosarCore ERROR:  RDBMS password is not defined.");
				System.out
						.println("KosarCore Suggested Fix:  Specify an rdbms password in the configuration file, e.g., dbpassword=gocosar");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.exit(-1);
			}

			num_replicas = Integer
					.valueOf(serverProperties.getProperty(NUM_REPLICAS) == null ? "-1"
							: serverProperties.getProperty(NUM_REPLICAS));
			if(num_replicas < -1 || num_replicas == 0) {
				System.out.println("KosarCore ERROR: Number of replicas for a query string cannot be less than -1 or 0.");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.out.println("KosarCore Suggested Fix: Specify -1 for limitless replicas or integer > 0 for specified number of replicas. " +
						"Default is set to -1 if one is not specified.");
				System.exit(-1);
			}
			
			clientReplicaCommand = serverProperties.getProperty(CLIENTREPLICACOMMAND);
			if (clientReplicaCommand == null) {
				System.out
						.println("KosarCore ERROR:  Replica configuration for the steal or consume command is not specified.");
				System.out
						.println("KosarCore Suggested Fix:  Specify a command for the CORE to issue in the case of system replica threshold in the configuration file, e.g., clientreplicacommand=steal");
				System.out.println("KosarCore Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
				System.exit(-1);
			}
			
			webserverPort = Integer
					.valueOf(serverProperties.getProperty(WEBSERVERPORT) == null ? "-1"
							: serverProperties.getProperty(WEBSERVERPORT));
			
		} catch (FileNotFoundException f) {
			System.out
					.println("Error in "
							+ KosarCore.class.getName()
							+ " package is com.mitrallc.core, KOSAR configuration file is missing.");
			System.out
					.println("Suggested Fix, either change the CONFIG_FILE constant or create a config file at the following path "
							+ CONFIG_FILE);
			System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
			System.out.println("KosarCore Suggested Fix: Specify -1 for limitless replicas or integer > 0 for specified number of replicas. " +
					"Default is set to -1 if one is not specified.");
			f.printStackTrace();
			System.exit(-1);
		} catch (IOException io) {
			System.out
					.println("Error in "
							+ KosarCore.class.getName()
							+ " package is com.mitrallc.core, KOSAR configuration file is missing.");
			System.out
					.println("Suggested Fix, either change the CONFIG_FILE constant or create a config file at the following path "
							+ CONFIG_FILE);
			System.out.println("KosarCore Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying the port number of the web server, RDBMS specs and connection information and number of replicas per KV pair in the system, ");
			System.out.println("KosarCore Suggested Fix: Specify -1 for limitless replicas or integer > 0 for specified number of replicas. " +
					"Default is set to -1 if one is not specified.");
			io.printStackTrace();
			System.exit(-1);
		} catch (Exception E) {
			System.out
					.println("Error: Unable to register driver.  Make sure JDBC jar file is correctly imported.");
			throw new RuntimeException("Can't register driver!");
		}
	}

	/**
	 * Establishes Connection to RDBMS using the specified driver in the configuration file.
	 */
	public static void connectToRDBMS() {
		try {
			Object newObject = Class.forName(rdbmsdriver).newInstance();
			driver = (Driver) newObject;
		} catch (InstantiationException e1) {
			System.out
					.println("KosarCore Error:  Failed to instantiate the necessary class driver for the RDBMS driver "
							+ rdbmsdriver + ".");
			System.out
					.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out
					.println("Fatal error:  Cannot proceed forward, exiting!");
			e1.printStackTrace();
			System.exit(-1);
		} catch (IllegalAccessException e1) {
			System.out
					.println("Error: KosarCore - Illegal Access to RDBMS Driver.");
			System.out
					.println("May not have access to JDBC Driver. Check Driver parameters.");
			System.out
					.println("Fatal error:  Cannot proceed forward, exiting!");
			e1.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e1) {
			System.out
					.println("KosarCore Error:  Failed to find the necessary class driver for the RDBMS driver "
							+ rdbmsdriver + ".");
			System.out
					.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out
					.println("Fatal error:  Cannot proceed forward, exiting!");
			e1.printStackTrace();
			System.exit(-1);
		}

		conn = null;
		Properties connectionProperties = new Properties();
		connectionProperties.put("user", db_user);
		connectionProperties.put("password", db_password);
		try {
			conn = driver.connect(db_url, connectionProperties);
			System.out.println("Successful connection to db at: " + db_url);
		} catch (SQLException e) {
			System.out.println("Unable to establish connection with database. Check properties");
			System.out.println("Driver: " + driver.toString());
			System.out.println("URL: " + db_url);
			System.out.println("User: " + db_user);
			System.out.println("Password: " + db_password);
			e.printStackTrace();
		}

		
	}

	/**
	 * Releases RegisterThread worker Semaphores and turns off the toggle that keeps them running.
	 */
	public static void stopWorkers() {
		coreWorking = false;
		for(int i = 0; i < NumCacheRegDivisions; i++) {
			for(int j = 0; j < NumWorkerThreads; j++) {
				tokenCachedWorkToDo.get(i).release();
				try {
					tokenCacheWorkers.get(i).join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * The CORE waits for connections from Kosar Clients and passes them to a RequestHandler
	 */
	@Override
	public void run() {
		int handlerID = 0;
		Socket socket = null;
		try {
			server = new ServerSocket(CLIENT_CONNECT_PORT);
			System.out.println("KVSCore Client Listener thread active on port: "
					+ server.getLocalPort());
			while (true) {
				try {
					// wait for a connection from a client
					socket = server.accept();
					RequestHandler handler = new RequestHandler(socket, handlerID++);
					handler.setName("Request Handler " + handlerID);
					handlers.add(handler);
					handler.start();
					//System.out.println("Connection from Client " + id + " Received.");
				} catch (Exception e) {
					System.out.println("Coordinator server closed");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != server && !server.isClosed())
					server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Utility method to compute the hash code for querySemaphores.
	 * Uses the sql query as input (v), and the length of the query string.
	 * @param v
	 * @param length
	 * @return
	 */
	public static int computeHashCode(byte[] v, int length){
		int val = 0;
		for (int i=0; i < v.length; i++){
			val += v[i];
		}
		return val % numProtectionSemaphores;
	}
	public void shutdown() {
		stopWorkers();
		for(RequestHandler rh : handlers) {
			rh.stop();
		}
		triggerRegThread.interrupt();
		triggerListener.interrupt();
		this.stop();
	}
	public static void resetData() {
		stopWorkers();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clientToIPMap = new ConcurrentHashMap<Object,Object>();
		clientToPortsMap = new ConcurrentHashMap<Object,Object>();
		pingClientsMap = new ConcurrentHashMap<Object,Object>();
		internalTokenToQueriesMap = new ConcurrentHashMap<Object,Object>();
		queryToClientsMap = new ConcurrentHashMap<Object,Object>();
		triggerMap = new HashMap<String, String>();
		AL = new AtomicLong(0);
		keyQueue = new DynamicArray();
		requestsToProcess = new Vector<ConcurrentLinkedQueue<Object>>();
		handlers = new Vector<RequestHandler>();
		tokenCachedWorkToDo = new Vector<Semaphore>();
		tokenCacheWorkers = new Vector<TokenCacheWorker>();
		
		//Make sure number of worker threads is a multiple of the divisions.
		int divisions = NumWorkerThreads/NumCacheRegDivisions;
		NumWorkerThreads = divisions * NumCacheRegDivisions;
		
		coreWorking = true;
		
		//There is a semaphore, a request queue and list of workers for each
		//division.
		int j = 0;
		for(int i = 0; i < NumCacheRegDivisions; i++) {
			Semaphore semaphore = new Semaphore(0, true);
			tokenCachedWorkToDo.add(semaphore);
			requestsToProcess.add((ConcurrentLinkedQueue<Object>)new ConcurrentLinkedQueue<Object>());
			for(;j < (i+1)*divisions; j++) {
				tokenCacheWorkers.add(new TokenCacheWorker(i));
			}
		}
		for(int i = 0; i < NumWorkerThreads; i++){
			new Thread(tokenCacheWorkers.get(i), "TokenCacheWorker" + i).start();
		}
		System.out.println("Started " + NumWorkerThreads + " Cache Worker Threads.");
	}
	public static void main(String[] args) {
		core = new KosarCore();

		Runtime.getRuntime().addShutdownHook(
				new ShutdownHook(core, core.getClass().toString()));
		core.setName("Kosar CORE Main Thread");
		core.start();
	}
	
	

	/********* Getters & Setters *********/
	public static int getNumReplicas() {
		return num_replicas;
	}		

	public static void setNumReplicas(int replicas) {
		num_replicas = replicas;
	}
	
	public static String getClientReplicaCommand() {
		return clientReplicaCommand;
	}

	public static int getCopyCount() {
		return copyCount;
	}

	public static Connection getConn() {
		return conn;
	}

	public static void setConn(Connection conn) {
		KosarCore.conn = conn;
	}

	public static TriggerRegisterThread getTriggerRegThread() {
		return triggerRegThread;
	}

	public static void setTriggerRegThread(
			TriggerRegisterThread triggerRegThread) {
		KosarCore.triggerRegThread = triggerRegThread;
	}

	public ServerSocket getServer() {
		return server;
	}

	public void setServer(ServerSocket server) {
		this.server = server;
	}

	public static int getIndexForWorkToDo() {
		return Math.abs(random.nextInt() % KosarCore.NumCacheRegDivisions);
	}
}
