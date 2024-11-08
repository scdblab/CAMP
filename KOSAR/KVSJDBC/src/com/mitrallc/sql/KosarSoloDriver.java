package com.mitrallc.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.mitrallc.sqltrig.QueryToTrigger;
import com.mitrallc.webserver.BaseSettings;
import com.mitrallc.webserver.EventMonitor;
import com.mitrallc.webserver.BaseHttpServer;
import com.mitrallc.webserver.Last100SQLQueries;
import com.mitrallc.webserver.MyHttpHandler;

import com.mitrallc.common.ClientDataStructures;
import com.mitrallc.common.DynamicArray;
import com.mitrallc.common.Flags;
import com.mitrallc.common.KeyQueueDataItem;
import com.mitrallc.common.KosarResizableArrayList;
import com.mitrallc.common.TransactionDataItem;
import com.mitrallc.common.XLocks;
import com.mitrallc.communication.CacheModeController;
import com.mitrallc.communication.ClientConnector;
import com.mitrallc.communication.CoordinatorConnector;
import com.mitrallc.communication.CoordinatorReconnector;
import com.mitrallc.communication.InvalidationClientServer;
import com.mitrallc.control.PingThread;
import com.mitrallc.kosar.kosar;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.webserver.EventMonitor;
import com.mitrallc.webserver.BaseHttpServer;

public class KosarSoloDriver implements Driver {
	Driver driver;
	public static String rdbmsdriver = "";
	public static String urlPrefix = "kosarsolo:";
	private static final boolean VERBOSE = false;
	public static kosar Kache = null;
	private static boolean KosarRegThread = false;
	public static regthread TRT;
	public static Semaphore InitTrigThread = new Semaphore(1, true);
	public static ClientDataStructures clientData = new ClientDataStructures();
	public static PingThread pingThread = new PingThread();
	public static DynamicArray keyQueue;	
	public static DynamicArray pendingTransactionArray;
	//A KOSAR system with a core may only have this many replicas of a sql query string cached
	//at a given moment. This may be specified to -1, which dictates no limit.
	//This value is received from the CORE when the client registers itself with it.
	public static int numReplicas;
	
	public static boolean coordConnected = false;
	
	public static volatile ExecutorService triggerRegService = Executors
			.newSingleThreadExecutor();
	private static volatile ExecutorService coordinatorReconnectService = Executors
			.newSingleThreadExecutor();
	public static volatile ExecutorService keyCachingService = Executors
			.newCachedThreadPool();

	private static volatile Future<Boolean> reconnectionCompleted = null;

	private static Flags flags = new Flags();
	private static XLocks lockManager = new XLocks();

	private static final String COREIP = "coreip";
	private static final String PORT = "port";
	private static final String DBPORT = "dbport";
	private static final String INIT_CONNECTIONS = "initconnections";
	private static final String KOSAR_ENABLED = "kosarEnabled";
	private static final String WEBSERVERPORT = "webserverport";
	private static final String RDBMS = "rdbms";
	private static final String RDBMSDriver = "rdbmsdriver";
	private static final String CLIENTSPORT = "clientsport";
	public static BaseHttpServer webServer = null;

	public static EventMonitor KosarTriggerRegEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarKeysCachedEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarCacheHitsEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarNumQueryRequestsEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarEvictedKeysEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarQueryResponseTimeEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarDMLUpdateEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarRTEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarInvalidatedKeysEventMonitor = new EventMonitor(BaseSettings.getGranularity());
	public static EventMonitor KosarInvalKeysAttemptedEventMonitor = new EventMonitor(BaseSettings.getGranularity());

	
	public static Last100SQLQueries last100readQueries = new Last100SQLQueries();
	public static Last100SQLQueries last100updateQueries = new Last100SQLQueries();
	
	public static int copyCount = 0;
	public static int stealCount = 0;
	
	public static String core_ip;
	public static int webserverport;
	public static int server_port;
	public static int db_port;
	public static int init_connections;
	public static int min_connections;
	public static int max_connections;
	public static boolean kosarEnabled;
	private static volatile long lastReconnectTime = 0;
	public static int clientsport;
	
	public static void setLastReconnectTime(long lastReconnectTime) {
		KosarSoloDriver.lastReconnectTime = lastReconnectTime;
	}

	private static final SockIOPool connection_pool = new SockIOPool();

	private static final String cfgfile = "kosar.cfg";
	private static String CONFIG_FILE = "./"+cfgfile;

	//Maps a 4-byte Client ID to a SockIOPool Object for Client-Client communication
	public static HashMap<ByteBuffer, SockIOPool> clientPoolMap = new HashMap<ByteBuffer, SockIOPool>();
	
	// ~ Static fields/initializers
	// ---------------------------------------------
	//
	// Register ourselves with the DriverManager
	//
	static {
		System.out.println("KosarSoloDriver");
		try {
			// load server details
			Properties serverProperties = new Properties();
			String sysEnvStr = System.getenv("KOSAR_HOME");
			if (sysEnvStr == null)
				sysEnvStr = System.getenv("kosar_home");
			if (sysEnvStr != null){
				sysEnvStr = sysEnvStr.trim();
				if (! sysEnvStr.endsWith(cfgfile)) CONFIG_FILE = sysEnvStr+"/"+cfgfile;
				else CONFIG_FILE=sysEnvStr;
				System.out.println("Config file "+CONFIG_FILE);
			}
		
			serverProperties
					.load(new FileInputStream(new File(CONFIG_FILE)));
			kosarEnabled = ((serverProperties.getProperty(KOSAR_ENABLED) == null ? "false"
					: serverProperties.getProperty(KOSAR_ENABLED))
					.equals("true"));
			
			/** Creating the Webserver **/
			String port = serverProperties.getProperty(WEBSERVERPORT);
			if(port != null) {
				webserverport = Integer.decode(port);
				webServer = new BaseHttpServer(webserverport, "CLIENT", new MyHttpHandler());
			}
			
			String rdbmstype = serverProperties.getProperty(RDBMS);
			if (rdbmstype != null && rdbmstype.compareToIgnoreCase("mysql")==0){
				QueryToTrigger.setTargetSystem(QueryToTrigger.RDBMS.MySQL);
			} else if (rdbmstype != null && rdbmstype.compareToIgnoreCase("oracle")==0) {
				QueryToTrigger.setTargetSystem(QueryToTrigger.RDBMS.Oracle);
			} else {
				System.out.println("KosarSoloDriver ERROR:  RDBMS type is either not defined or unknown.");
				System.out.println("KosarSoloDriver Suggested Fix:  Specify an rdbms tag with a target RDBMS in the configuration file, e.g., rdbms=mysql");
				System.out.println("KosarSoloDriver Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarSoloDriver Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying whether kosar is enabled, port number of the web server, listening port for clients, and RDBMS specs.");
				System.exit(-1);
			}

			rdbmsdriver = serverProperties.getProperty(RDBMSDriver);
			if (rdbmsdriver == null) {
				System.out.println("KosarSoloDriver ERROR:  RDBMS driver is not defined.");
				System.out.println("KosarSoloDriver Suggested Fix:  Specify an rdbms tag with a target RDBMS in the configuration file, e.g., rdbms=mysql");
				System.out.println("KosarSoloDriver Suggested Fix:  The path to the configuration file is set by specifying a value for the environment variable KOSAR_HOME");
				System.out.println("KosarSoloDriver Suggested Fix:  KOSAR_HOME should be the path to a directory containing "+cfgfile+" specifying whether kosar is enabled, port number of the web server, listening port for clients, and RDBMS specs.");
				System.exit(-1);
			}
			
			if(kosarEnabled) {
				core_ip = serverProperties.getProperty(COREIP);
				server_port = Integer
						.valueOf((serverProperties.getProperty(PORT) == null ? "0"
								: serverProperties.getProperty(PORT)));
				db_port = Integer
						.valueOf(serverProperties.getProperty(DBPORT) == null ? "0"
								: serverProperties.getProperty(DBPORT));
				init_connections = Integer.valueOf(serverProperties
						.getProperty(INIT_CONNECTIONS) == null ? "0"
						: serverProperties.getProperty(INIT_CONNECTIONS));
				clientsport = Integer.valueOf(serverProperties
						.getProperty(CLIENTSPORT) == null ? "-1"
						: serverProperties.getProperty(CLIENTSPORT));
				// Try to connect to coordinator
				if (core_ip == null
						|| core_ip.length() == 0
						|| isNotAnIPAddress(core_ip)) {
					flags.setCoordinatorExists(false);
					CacheModeController.enableQueryCaching();
					
					/* If there is no coordinator and Kosar is enabled, the double delete step needs
					 * to be handled by the client. The following data structures: pendingTransactionList
					 * and keyQueue support this function. 
					 */
					pendingTransactionArray = new DynamicArray();
					keyQueue = new DynamicArray();
				} else {
					flags.setCoordinatorExists(true);
					
					// Initialize Client-to-Client listener
					if(clientsport >= 0) {
						ClientConnector clientConnect = new ClientConnector(clientsport);
						clientConnect.start();
					}
					/*// Initialize invalidation listener
					InvalidationClientServer invalidationConnect = new InvalidationClientServer();
					new Thread(invalidationConnect).start();*/
				}
			} else {
				flags.setCoordinatorExists(false);
				CacheModeController.disableQueryCaching();
			}

		} catch (FileNotFoundException f) {
			System.out.println("Error in "+KosarSoloDriver.class.getName()+": KOSAR configuration file "+cfgfile+" is missing.");
			System.out.println("KosarSoloDriver Suggested Fix:  Define the environmental variable KOSAR_HOME to a directory containing a file named "+cfgfile+" - This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t\t kosarEnabled=true");
			System.out.println("\t\t webserverport=9091");
			System.out.println("\t\t rdbms=mysql");
			System.out.println("\t\t rdbmsdriver=com.mysql.jdbc.Driver");
			f.printStackTrace();
			flags.setCoordinatorExists(false);
			CacheModeController.disableQueryCaching();
			throw new RuntimeException("Can't register driver!");
		} catch (IOException io) {
			System.out.println("Error in "+KosarSoloDriver.class.getName()+": KOSAR configuration file "+cfgfile+" is missing.");
			System.out.println("KosarSoloDriver Suggested Fix:  Define the environmental variable KOSAR_HOME to a directory containing a file named "+cfgfile+" - This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t\t kosarEnabled=true");
			System.out.println("\t\t webserverport=9091");
			System.out.println("\t\t rdbms=mysql");
			System.out.println("\t\t rdbmsdriver=com.mysql.jdbc.Driver");
			io.printStackTrace();
			flags.setCoordinatorExists(false);
			CacheModeController.disableQueryCaching();
			throw new RuntimeException("Can't register driver!");
		}
		catch (Exception E) {
			System.out.println("Error in "+KosarSoloDriver.class.getName()+": KOSAR configuration file "+cfgfile+" is missing.");
			System.out.println("KosarSoloDriver Suggested Fix:  Define the environmental variable KOSAR_HOME to a directory containing a file named "+cfgfile+" - This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t This file must specify the following tags: kosarEnabled, webserverport, rdbms {mysql,oracle...}, driver name.  Example");
			System.out.println("\t\t kosarEnabled=true");
			System.out.println("\t\t webserverport=9091");
			System.out.println("\t\t rdbms=mysql");
			System.out.println("\t\t rdbmsdriver=com.mysql.jdbc.Driver");
			throw new RuntimeException("Can't register driver!");
		}
		
		try {
			java.sql.DriverManager.registerDriver(new KosarSoloDriver());
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	// ~ Constructors
	// -----------------------------------------------------------

	/**
	 * Construct a new driver and register it with DriverManager
	 * 
	 * @throws SQLException
	 *             if a database error occurs.
	 */
	public KosarSoloDriver() throws SQLException {
		// Required for Class.forName().newInstance()
		//this.driver = new oracle.jdbc.driver.OracleDriver();
		//this.driver = new com.mysql.jdbc.Driver();
		
		try {
			Object newObject = Class.forName(rdbmsdriver).newInstance();
			this.driver = (Driver) newObject;
		} catch (InstantiationException e1) {
			System.out.println("KosarSoloDriver Error:  Failed to find the necessary class driver for the RDBMS driver "+rdbmsdriver+".");
			System.out.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out.println("Fatal error:  Cannot proceed forward, exiting!");
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		} catch (IllegalAccessException e1) {
			System.out.println("KosarSoloDriver Error:  Failed to find the necessary class driver for the RDBMS driver "+rdbmsdriver+".");
			System.out.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out.println("Fatal error:  Cannot proceed forward, exiting!");
			
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			System.out.println("KosarSoloDriver Error:  Failed to find the necessary class driver for the RDBMS driver "+rdbmsdriver+".");
			System.out.println("Suggested Fix:  Verify the jar file of the specified RDBMS driver is in the build path of the project.");
			System.out.println("Fatal error:  Cannot proceed forward, exiting!");
			
			e1.printStackTrace();
		}
		
		if (VERBOSE)
			System.out.println("Initializing KosarSoloDriver");

	}

	private static boolean isNotAnIPAddress(String coordinator_address2) {
		return false;
	}

	@Override
	public boolean acceptsURL(String arg0) throws SQLException {
		if (VERBOSE)
			System.out.println("acceptsURL( " + arg0 + " )");

		String urlWithoutCOSAR = arg0;

		// Check if "cosar:" prefix is in the URL
		// If it is, strip the prefix and pass remaining URL on to underlying
		// driver
		int start = arg0.indexOf(urlPrefix);
		if (start >= 0) {
			urlWithoutCOSAR = arg0.substring(urlPrefix.length());
		} else {
			if (VERBOSE)
				System.out.println("Not a COSAR URL: " + arg0);
		}

		return this.driver.acceptsURL(urlWithoutCOSAR);
	}

	/***
	 * Attempts to create a wrapped version of a database connection to the
	 * given URL.
	 */
	@Override
	public Connection connect(String arg0, Properties arg1) throws SQLException {
		Connection conn = null;

		if (VERBOSE)
			System.out.println("connect( " + arg0 + " )");

		// Check if "cosar:" prefix is in the URL
		// If it is, strip the prefix and pass remaining URL on to underlying
		// driver
		String urlWithoutCOSAR = arg0;
		int start = arg0.indexOf(urlPrefix);
		if (start >= 0) {
			urlWithoutCOSAR = arg0.substring(urlPrefix.length());
			if (VERBOSE)
				System.out.println("final connect to " + urlWithoutCOSAR);
		}

		if (!KosarRegThread) {
			try {
				InitTrigThread.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (!KosarRegThread) {
				try {
					conn = this.driver.connect(urlWithoutCOSAR, arg1);
				} catch (SQLException e) {
					throw new KosarSQLException(e.getMessage());
				} // Connection
					// for
					// SQLTrig
				TRT = new regthread(conn);
				TRT.start();
				KosarRegThread = true;
			}
			InitTrigThread.release();
		}

		if (Kache == null){
			//Allocate a connection for kosar
			try {
				conn = this.driver.connect(urlWithoutCOSAR, arg1);
				if (conn == null)System.out.println("\n\tKosarSoloDriver Error:  Failed to establish a db connection");
				Kache = new kosar(urlWithoutCOSAR, arg1, conn);
			} catch (SQLException e) {
				throw new KosarSQLException(e.getMessage());
			}
		}
		
		try {
			conn = this.driver.connect(urlWithoutCOSAR, arg1);
		} catch (SQLException e) {
			throw new KosarSQLException(e.getMessage());
		}
		if (conn != null) {
			conn = new com.mitrallc.sql.Connection(conn);
		}
		if (!(core_ip == null)
				&& (!(core_ip.length() == 0))
				&& !isNotAnIPAddress(core_ip) && !coordConnected) {
			coordConnected = true;
			CoordinatorConnector connectThread = new CoordinatorConnector();
			new Thread(connectThread).start();
		}
		
		return conn;
	}

	public static int getNumReplicas() {
		return numReplicas;
	}

	public static void setNumReplicas(int numReplicas) {
		KosarSoloDriver.numReplicas = numReplicas;
	}

	@Override
	public int getMajorVersion() {
		return this.driver.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return this.driver.getMinorVersion();
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1)
			throws SQLException {
		return this.driver.getPropertyInfo(arg0, arg1);
	}

	@Override
	public boolean jdbcCompliant() {
		return this.driver.jdbcCompliant();
	}

	public static SockIOPool getConnectionPool() {
		return connection_pool;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	public static XLocks getLockManager() {
		return lockManager;
	}

	public static void setLockManager(XLocks lockManager) {
		KosarSoloDriver.lockManager = lockManager;
	}

	public static Flags getFlags() {
		return flags;
	}

	public static void setLockManager(Flags f) {
		KosarSoloDriver.flags = f;
	}

	public static void startReconnectThread(long startReconnectTime) {
		synchronized (CoordinatorReconnector.class) {
			// each thread will send it's start time to the function
			// start reconnect thread only if start time is greater than the
			// last time reconnection was done successfully
			if (startReconnectTime > KosarSoloDriver.lastReconnectTime
			// and if any previous reconnection was completed
					&& (null == reconnectionCompleted || reconnectionCompleted
							.isDone())) {
				// coordinator is disconnected
				KosarSoloDriver.flags.setCoordinatorConnected(false);
				reconnectionCompleted = coordinatorReconnectService
						.submit(new CoordinatorReconnector());
			}
		}
	}
}
