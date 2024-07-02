package com.mitrallc.sql;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SockIOPool {
	// Constants
    private static final Integer ZERO = new Integer( 0 );
	// store instances of pools
    public static final long MAX_RETRY_DELAY = 10 * 60 * 1000;  // max of 10 minute delay for fall off

    private MaintThread maintThread;
    private boolean initialized = false;

    // initial, min and max pool sizes
    private int initConn = 10;
    private long maintSleep = 5 * 60 * 1000;                        // maintenance thread sleep time
    
    private int socketConnectTO = 0;         // default timeout of socket connections
    private boolean aliveCheck = false;                                // default to not check each connection for being alive
    private boolean failover = true;                                // default to failover in event of cache server dead
    private boolean failback = true;                                // only used if failover is also set ... controls putting a dead server back into rotation
    
    // locks
    private final ReentrantLock hostDeadLock = new ReentrantLock();
    // list of all server
    private String server;
    
    // map to hold all available sockets
    private Map<String,Map<SockIO,Long>> availPool;
    // map to hold busy sockets
    private Map<String,Map<SockIO,Long>> busyPool;
    // set to hold sockets to close
    private Map<SockIO,Integer> deadPool;
    
 // dead server map
    private Map<String,Date> hostDead;
    private Map<String,Long> hostDeadDur;
    
	public SockIOPool() {
	}
    
    //this will be called by the client. The server name will be - host_name:port
    public void setServer( String server ) { this.server = server; }
    public String getServer() { return this.server; }
    
    /**
     * Sets the initial number of connections per server in the available pool.
     *
     * @param initConn int number of connections
     */
    public void setInitConn( int initConn ) { this.initConn = initConn; }
    
    /**
     * Returns the current setting for the initial number of connections per server in
     * the available pool.
     *
     * @return number of connections
     */
    public int getInitConn() { return this.initConn; }

    /**
     * Set the sleep time between runs of the pool maintenance thread.
     * If set to 0, then the maint thread will not be started.
     *
     * @param maintSleep sleep time in ms
     */
    public void setMaintSleep( long maintSleep ) { this.maintSleep = maintSleep; }
    
    /**
     * Returns the current maint thread sleep time.
     *
     * @return sleep time in ms
     */
    public long getMaintSleep() { return this.maintSleep; }
    
    /**
     * Sets the socket timeout for connect.
     *
     * @param socketConnectTO timeout in ms
     */
    public void setSocketConnectTO( int socketConnectTO ) { this.socketConnectTO = socketConnectTO; }
    
    /**
     * Returns the socket timeout for connect.
     *
     * @return timeout in ms
     */
    public int getSocketConnectTO() { return this.socketConnectTO; }

    /**
     * Sets the failover flag for the pool.
     *
     * If this flag is set to true, and a socket fails to connect,<br/>
     * the pool will attempt to return a socket from another server<br/>
     * if one exists. If set to false, then getting a socket<br/>
     * will return null if it fails to connect to the requested server.
     *
     * @param failover true/false
     */
    public void setFailover( boolean failover ) { this.failover = failover; }
    
    /**
     * Returns current state of failover flag.
     *
     * @return true/false
     */
    public boolean getFailover() { return this.failover; }

    /**
     * Sets the failback flag for the pool.
     *
     * If this is true and we have marked a host as dead,
     * will try to bring it back. If it is false, we will never
     * try to resurrect a dead host.
     *
     * @param failback true/false
     */
    public void setFailback( boolean failback ) { this.failback = failback; }
    
    /**
     * Returns current state of failover flag.
     *
     * @return true/false
     */
    public boolean getFailback() { return this.failback; }

    /**
     * Sets the aliveCheck flag for the pool.
     *
     * When true, this will attempt to talk to the server on
     * every connection checkout to make sure the connection is
     * still valid. This adds extra network chatter and thus is
     * defaulted off. May be useful if you want to ensure you do
     * not have any problems talking to the server on a dead connection.
     *
     * @param aliveCheck true/false
     */
    public void setAliveCheck( boolean aliveCheck ) { this.aliveCheck = aliveCheck; }


    /**
     * Returns the current status of the aliveCheck flag.
     *
     * @return true / false
     */
    public boolean getAliveCheck() { return this.aliveCheck; }
    
    /**
     * Returns state of pool.
     *
     * @return <CODE>true</CODE> if initialized.
     */
    public boolean isInitialized() {
            return initialized;
    }
    
    public SockIOPool getPool() {
		return this;
	}

	public void initialize() throws ConnectException {

		// check to see if already initialized
		if (initialized && (availPool != null) && (busyPool != null) && (availPool.size()>0)) {
			return;
		}
		// if servers is not set, or it empty, then
        // throw a runtime exception
        if ( server == null) {
                throw new IllegalStateException( "++++ trying to initialize with no servers" );
        }
		// pools
        if(null == availPool)
        	availPool = new ConcurrentHashMap<String,Map<SockIO,Long>>( initConn );
        if(null == busyPool)
        	busyPool = new ConcurrentHashMap<String,Map<SockIO,Long>>( initConn );
        if(null == deadPool)
        	deadPool = new ConcurrentHashMap<SockIO,Integer>();
        
        if(null == hostDeadDur)
        	hostDeadDur = new ConcurrentHashMap<String,Long>();
        if(null == hostDead)
        	hostDead = new ConcurrentHashMap<String,Date>();
       
        createInitialConnections();
        
        // mark pool as initialized
        this.initialized = true;
        
        // start maint thread
        if ( this.maintSleep > 0 )
        	this.startMaintThread();
    }

	private void createInitialConnections() throws ConnectException {
		for (int j = 0; j < initConn; j++) {

			SockIO socket = createSocket(server);

			if (socket == null) {
				break;
			}

			addSocketToPool(availPool, server, socket);
		}
	}
	
	/**
     * Creates a new SockIO obj for the given server.
     *
     * If server fails to connect, then return null and do not try<br/>
     * again until a duration has passed. This duration will grow<br/>
     * by doubling after each failed attempt to connect.
     *
     * @param host host:port to connect to
     * @return SockIO obj or null if failed to create
	 * @throws ConnectException 
     */
    protected SockIO createSocket( String host ) throws ConnectException {
    	SockIO socket = null;
    	
    	//we don't need to do this expiration check
    	/*// if host is dead, then we don't need to try again
        // until the dead status has expired
        // we do not try to put back in if failback is off
        hostDeadLock.lock();
		try {
			if (failover && failback && hostDead.containsKey(host) && hostDeadDur.containsKey(host)) {

				Date store = hostDead.get(host);
				long expire = hostDeadDur.get(host).longValue();

				if ((store.getTime() + expire) > System.currentTimeMillis())
					return null;
			}
		} finally {
			hostDeadLock.unlock();
		}*/
    	
		try {
			socket = new SockIO(this, host, this.socketConnectTO);

			if (!socket.isConnected()) {
				deadPool.put(socket, ZERO);
				socket = null;
			}
		} catch (ConnectException c) {
			throw c;
		} 
		catch (Exception ex) {
			socket = null;
		}
		
		/*// if we failed to get socket, then mark
        // host dead for a duration which falls off
        hostDeadLock.lock();
		try {
			if (socket == null) {
				Date now = new Date();
				hostDead.put(host, now);

				long expire = (hostDeadDur.containsKey(host)) ? (((Long) hostDeadDur
						.get(host)).longValue() * 2) : 1000;

				if (expire > MAX_RETRY_DELAY)
					expire = MAX_RETRY_DELAY;

				hostDeadDur.put(host, new Long(expire));

				// also clear all entries for this host from availPool
				clearHostFromPool(availPool, host);
			} else {
				if (hostDead.containsKey(host) || hostDeadDur.containsKey(host)) {
					hostDead.remove(host);
					hostDeadDur.remove(host);
				}
			}
		} finally {
			hostDeadLock.unlock();
		}*/

		return socket;
    }
    
    /**
     * Returns appropriate SockIO object.
     * Tries to get SockIO from pool.
     *
     * @return SockIO obj connected to server
     * @throws ConnectException 
     */
	public SockIO getSock() throws ConnectException {
    	if ( !this.initialized ) {
            return null;
    	}
    	if(server==null)
    		return null;
    	
    	SockIO sock = getConnection();

        if ( sock != null && sock.isConnected() ) {
                if ( aliveCheck ) {
                        if ( !sock.isAlive() ) {
                                sock.close();
                                try { sock.trueClose(); } catch ( IOException ioe ) { }
                                sock = null;
                        }
                }
        }
        else {
                if ( sock != null ) {
                        deadPool.put( sock, ZERO );
                        sock = null;
                }
        }

        return sock;
    }
	
	/**
	 * This function returns the current ports used by the client to communicate
	 * with the coordinator
	 */
	public byte[] getPorts() {
		if (!this.initialized) {
			return null;
		}
		if (server == null)
			return null;

		if (availPool != null && !availPool.isEmpty()) {

			// take first connected socket
			Map<SockIO, Long> aSockets = availPool.get(server);
			if (aSockets != null && !aSockets.isEmpty()) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int numOfSockets = aSockets.size();
				ByteBuffer bb = ByteBuffer.allocate(4);
				bb.putInt(numOfSockets);
				try {
					baos.write(bb.array());
					bb.clear();
					for (Iterator<SockIO> i = aSockets.keySet().iterator(); i
							.hasNext();) {
						SockIO socket = i.next();
					    bb.putInt(socket.getLocalPortNum());
					    System.out.println(socket.getLocalPortNum());
						baos.write(bb.array());
						bb.clear();
					}

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return baos.toByteArray();
			}
		}
		return null;
	}
	
	/**
     * Returns a SockIO object from the pool for the passed in host.
     *
     * Meant to be called from a more intelligent method<br/>
     * which handles choosing appropriate server<br/>
     * and failover.
     *
     * @return SockIO object or null if fail to retrieve one
	 * @throws ConnectException 
     */
    public SockIO getConnection() throws ConnectException {
    	if ( !this.initialized ) {
            return null;
    	}
    	if(server==null)
    		return null;
    	
		
    	synchronized (this) {
			// if we have items in the pool
			// then we can return it
			if (availPool != null && !availPool.isEmpty()) {

				// take first connected socket
				Map<SockIO, Long> aSockets = availPool.get(server);

				if (aSockets != null && !aSockets.isEmpty()) {

					for (Iterator<SockIO> i = aSockets.keySet().iterator(); i
							.hasNext();) {
						SockIO socket = i.next();

						if (socket.isConnected()) {
							// remove from avail pool
							i.remove();

							// add to busy pool
							addSocketToPool(busyPool, server, socket);

							// return socket
							return socket;
						} else {
							// add to deadpool for later reaping
							deadPool.put(socket, ZERO);

							// remove from avail pool
							i.remove();
						}
					}
				}
			}
		}

		//we don't create a new socket
		return null;
    }
    
	/**
	 * Adds a socket to a given pool for the given host. THIS METHOD IS NOT
	 * THREADSAFE, SO BE CAREFUL WHEN USING!
	 * 
	 * Internal utility method.
	 * 
	 * @param pool
	 *            pool to add to
	 * @param host
	 *            host this socket is connected to
	 * @param socket
	 *            socket to add
	 */
	protected void addSocketToPool(Map<String, Map<SockIO, Long>> pool,
			String host, SockIO socket) {

		if (pool.containsKey(host)) {
			Map<SockIO, Long> sockets = pool.get(host);

			if (sockets != null) {
				sockets.put(socket, new Long(System.currentTimeMillis()));
				return;
			}
		}

		Map<SockIO, Long> sockets = new ConcurrentHashMap<SockIO, Long>();

		sockets.put(socket, new Long(System.currentTimeMillis()));
		pool.put(host, sockets);
	}
	
	/**
     * Removes a socket from specified pool for host.
     * THIS METHOD IS NOT THREADSAFE, SO BE CAREFUL WHEN USING!
     *
     * Internal utility method.
     *
     * @param pool pool to remove from
     * @param host host pool
     * @param socket socket to remove
     */
	protected void removeSocketFromPool(Map<String, Map<SockIO, Long>> pool,
			String host, SockIO socket) {
		if(null!=pool && null!=host)
			if (pool.containsKey(host)) {
				Map<SockIO, Long> sockets = pool.get(host);
				if (sockets != null)
					sockets.remove(socket);
			}
	}
	
	 /**
     * Closes and removes all sockets from specified pool for host.
     * THIS METHOD IS NOT THREADSAFE, SO BE CAREFUL WHEN USING!
     *
     * Internal utility method.
     *
     * @param pool pool to clear
     * @param host host to clear
     */
	protected void clearHostFromPool(Map<String, Map<SockIO, Long>> pool,
			String host) {

		if (pool.containsKey(host)) {
			Map<SockIO, Long> sockets = pool.get(host);

			if (sockets != null && sockets.size() > 0) {
				for (Iterator<SockIO> i = sockets.keySet().iterator(); i
						.hasNext();) {
					SockIO socket = i.next();
					try {
						socket.trueClose();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}

					i.remove();
					socket = null;
				}
			}
		}
	}
	
	/**
	 * Checks a SockIO object in with the pool.
	 * 
	 * This will remove SocketIO from busy pool, and optionally<br/>
	 * add to avail pool.
	 * 
	 * @param socket
	 *            socket to return
	 * @param addToAvail
	 *            add to avail pool if true
	 */
	private void checkIn(SockIO socket, boolean addToAvail) {

		String host = socket.getHost();

		synchronized (this) {
			// remove from the busy pool
			removeSocketFromPool(busyPool, host, socket);

			if (socket.isConnected() && addToAvail) {
				// add to avail pool
				addSocketToPool(availPool, host, socket);
			} else {
				deadPool.put(socket, ZERO);
				socket = null;
			}
		}
	}

	/**
	 * Returns a socket to the avail pool.
	 * 
	 * This is called from SockIO.close(). Calling this method<br/>
	 * directly without closing the SockIO object first<br/>
	 * will cause an IOException to be thrown.
	 * 
	 * @param socket
	 *            socket to return
	 */
	private void checkIn(SockIO socket) {
		checkIn(socket, true);
	}
	
	/**
	 * Closes all sockets in the passed in pool.
	 * 
	 * Internal utility method.
	 * 
	 * @param pool
	 *            pool to close
	 */
	protected void closePool(Map<String, Map<SockIO, Long>> pool) {
		for (Iterator<String> i = pool.keySet().iterator(); i.hasNext();) {
			String host = i.next();
			Map<SockIO, Long> sockets = pool.get(host);

			for (Iterator<SockIO> j = sockets.keySet().iterator(); j.hasNext();) {
				SockIO socket = j.next();

				try {
					socket.trueClose();
				} catch (IOException ioe) {
				}

				j.remove();
				socket = null;
			}
		}
	}
	
	/**
	 * Shuts down the pool.
	 * 
	 * Cleanly closes all sockets.<br/>
	 * Stops the maint thread.<br/>
	 * Nulls out all internal maps<br/>
	 */
	public void shutDown() {
		synchronized (this) {

			if(initialized) {
				if (maintThread != null && maintThread.isRunning()) {
					// stop the main thread
					stopMaintThread();
	
					// wait for the thread to finish
					while (maintThread.isRunning()) {
						try {
							Thread.sleep(500);
						} catch (Exception ex) {
						}
					}
				}
	
				closePool(availPool);
				closePool(busyPool);
				availPool = null;
				busyPool = null;
				hostDeadDur = null;
				hostDead = null;
				maintThread = null;
				initialized = false;
			}
		}
	}
	
	/**
     * Starts the maintenance thread.
     *
     * This thread will manage the size of the active pool<br/>
     * as well as move any closed, but not checked in sockets<br/>
     * back to the available pool.
     */
	protected void startMaintThread() {
		if (maintThread != null) {
			if (!maintThread.isRunning()) {
				maintThread.start();
			}
		} else {
			maintThread = new MaintThread(this);
			maintThread.setInterval(this.maintSleep);
			maintThread.start();
		}
	}

	/**
	 * Stops the maintenance thread.
	 */
	protected void stopMaintThread() {
		if (maintThread != null && maintThread.isRunning())
			maintThread.stopThread();
	}
	
	/**
	 * Runs self maintenance on all internal pools.
	 * 
	 * This is typically called by the maintenance thread to manage pool size.
	 * @throws ConnectException 
	 */
	protected void selfMaint() throws ConnectException{
		//clean out the deadPool
		Set<SockIO> toClose;
		synchronized (deadPool) {
			toClose = deadPool.keySet();
		
			for (SockIO socket : toClose) {
				try {
					socket.trueClose(false);
				} catch (Exception ex) {
				}
				socket = null;
			}
			deadPool.clear();
		}
	}
	
	/**
     * Class which extends thread and handles maintenance of the pool.
     *
     * @author greg whalin <greg@meetup.com>
     * @version 1.5
     */
    protected static class MaintThread extends Thread {
		private SockIOPool pool;
		private long interval = 1000 * 60; // every 1 minute
		private boolean stopThread = false;
		private boolean running;

		protected MaintThread(SockIOPool pool) {
			this.pool = pool;
			this.setDaemon(true);
			this.setName("MaintThread");
		}

		public void setInterval(long interval) {
			this.interval = interval;
		}

		public boolean isRunning() {
			return this.running;
		}
		
		/**
         * sets stop variable
         * and interupts any wait
         */
        public void stopThread() {
                this.stopThread = true;
                this.interrupt();
        }

		/**
		 * Start the thread.
		 */
		public void run() {
			this.running = true;

			while (!this.stopThread) {
				try {
					Thread.sleep(interval);

					// if pool is initialized, then
					// run the maintenance method on itself
					if (pool.isInitialized())
						pool.selfMaint();

				} catch (Exception e) {
					break;
				}
			}

			this.running = false;
		}
    }
    
    /**
     * MemCached client for Java, utility class for Socket IO.
     * This class is a wrapper around a Socket and its streams.
     *
     * @author greg whalin <greg@meetup.com>
     * @author Richard 'toast' Russo <russor@msoe.edu>
     * @version 1.5
     */
    public static class SockIO {
		// pool
        private SockIOPool pool;

        // data
        private String host;
        private Socket sock;

        private DataInputStream in;
        private DataOutputStream out;
        
    	public SockIO(SockIOPool pool, String host, int timeout) throws IOException{
    		this.pool = pool;

            String[] ip = host.split(":");
            // get socket: default is to use non-blocking connect
            sock = getSocket( ip[ 0 ], Integer.parseInt( ip[ 1 ] ), timeout );

            if ( timeout >= 0 )
                setSoTimeout( timeout );

            // wrap streams
            in = new DataInputStream( new BufferedInputStream( sock.getInputStream() ) );
            out = new DataOutputStream( new BufferedOutputStream( sock.getOutputStream() ));

            this.host = host;
		}
    	
    	public SockIO(Socket sock, int timeout) throws IOException{
    		this.pool = null;
            this.host = null;
    		
    		this.sock = sock;

    		if ( timeout >= 0 )
                setSoTimeout( timeout );
    		
    		in = new DataInputStream( new BufferedInputStream( sock.getInputStream() ) );
            out = new DataOutputStream( new BufferedOutputStream( sock.getOutputStream() ));
        }
    	
    	public Socket getSock(){
    		return sock;
    	}
    	
    	public byte[] getLocalIPAddress(){
    		try {
				return InetAddress.getLocalHost().getAddress();
			} catch (UnknownHostException e) {
				return null;
			}
    	}
    	
    	public int getLocalPortNum(){
    		return sock.getLocalPort();
    	}
    	
    	void setSoTimeout(int timeout) throws SocketException {
    		sock.setSoTimeout(timeout);
    	}
    	
    	/**
         * Method which gets a connection from SocketChannel.
         *
         * @param host host to establish connection to
         * @param port port on that host
         * @param timeout connection timeout in ms
         *
         * @return connected socket
         * @throws IOException if errors connecting or if connection times out
         */
        protected static Socket getSocket( String host, int port, int timeout ) throws IOException {
                SocketChannel sock = SocketChannel.open();
                sock.socket().connect( new InetSocketAddress( host, port ), timeout );
                System.out.println("Connected to " + host + " : " + port);
                System.out.println("New Sock at " + sock.socket().getInetAddress() + " : " + sock.socket().getLocalPort());
                return sock.socket();
        }
        
        /**
         * Lets caller get access to underlying channel.
         *
         * @return the backing SocketChannel
         */
        public SocketChannel getChannel() { return sock.getChannel(); }

        /**
         * returns the host this socket is connected to
         *
         * @return String representation of host (hostname:port)
         */
        public String getHost() { return this.host; }

        /**
         * closes socket and all streams connected to it
         *
         * @throws IOException if fails to close streams or socket
         */
        public void trueClose() throws IOException {
                trueClose( true );
        }
        
        /**
         * closes socket and all streams connected to it
         *
         * @throws IOException if fails to close streams or socket
         */
		public void trueClose(boolean addToDeadPool) throws IOException {

			boolean err = false;
			StringBuilder errMsg = new StringBuilder();

			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
					errMsg.append("++++ error closing input stream for socket: "
							+ toString() + " for host: " + getHost() + "\n");
					errMsg.append(ioe.getMessage());
					err = true;
				}
			}

			if (out != null) {
				try {
					out.close();
				} catch (IOException ioe) {
					errMsg.append("++++ error closing output stream for socket: "
							+ toString() + " for host: " + getHost() + "\n");
					errMsg.append(ioe.getMessage());
					err = true;
				}
			}

			if (sock != null) {
				try {
					sock.close();
				} catch (IOException ioe) {
					errMsg.append("++++ error closing socket: " + toString()
							+ " for host: " + getHost() + "\n");
					errMsg.append(ioe.getMessage());
					err = true;
				}
			}

			// check in to pool
			if (addToDeadPool && sock != null)
				pool.checkIn(this, false);

			in = null;
			out = null;
			sock = null;

			if (err)
				throw new IOException(errMsg.toString());
		}
		
		/**
		 * sets closed flag and checks in to connection pool but does not close
		 * connections
		 */
		public void close() {
			// check in to pool
			pool.checkIn(this);
		}
		
		/**
		 * checks if the connection is open
		 * 
		 * @return true if connected
		 */
		public boolean isConnected() {
			return (sock != null && sock.isConnected());
		}

		/**
		 * reads a line intentionally not using the deprecated readLine method
		 * from DataInputStream
		 * 
		 * @return String that was read in
		 * @throws IOException
		 *             if io problems during read
		 */
		public byte[] register(byte[] lookupRequestPort) throws IOException, EOFException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to read from closed socket");
			}
			byte request[] = new byte[lookupRequestPort.length+1];
			request[0] = 5;
			System.arraycopy(lookupRequestPort, 0, request, 1, lookupRequestPort.length);
		    this.write(request);
		    this.flush();
			
			return this.readBytes();
		}

		/*
		 * checks to see that the connection is still working
		 * 
		 * @return true if still alive
		 */
		public boolean isAlive() {
			if (!isConnected())
				return false;

			/*// try to talk to the server w/ a dumb query to ask its version
			try {
				byte b[] = new byte[KosarClient_Server.clientID.length+1];
				System.arraycopy(KosarClient_Server.clientID, 0, b, 1, KosarClient_Server.clientID.length);
				this.write(b);
			    this.flush();
				this.readInt();
			} catch (IOException ex) {
				return false;
			}*/

			return true;
		}
		
		
		/**
		 * Writes the specified byte (the low eight bits of the argument b) to the underlying output stream.
		 * 
		 * @param i
		 * @throws IOException
		 */
		public void write(int i) throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to write to closed socket");
			}
			out.write(i);
		}

		public void writeInt(int i) throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to write to closed socket");
			}
			out.writeInt(i);
		}
		
		public int readInt() throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to read from closed socket");
			}
			return in.readInt();
		}
		
		public byte readByte() throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to read from closed socket");
			}
			
			return in.readByte();
		}
		/**
		 * reads a line intentionally not using the deprecated readLine method
		 * from DataInputStream
		 * 
		 * @return String that was read in
		 * @throws IOException
		 *             if io problems during read
		 */
		public byte[] readBytes() throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new EOFException(
						"++++ attempting to read from closed socket");
			}

			int bytesRead = 0;
			byte[] b = null;
			int length = 0;
			try{
				// number of bytes to be read
				length = in.readInt();
				if (length > 0)
					b = new byte[length];

				while (bytesRead < length) {
					b[bytesRead++] = in.readByte();
				}
			}catch(IndexOutOfBoundsException i){
				System.out.println("Length: "+length+" bytesRead: "+bytesRead);
			}
			if(b==null)
				System.out.println(length+" "+bytesRead);
			return b;
		}
		
		/**
		 * reads up to end of line and returns nothing
		 * 
		 * @throws IOException
		 *             if io problems during read
		 */
		public void clearEOL() throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to read from closed socket");
			}

			byte[] b = new byte[1];
			boolean eol = false;
			while (in.read(b, 0, 1) != -1) {

				// only stop when we see
				// \r (13) followed by \n (10)
				if (b[0] == 13) {
					eol = true;
					continue;
				}

				if (eol) {
					if (b[0] == 10)
						break;

					eol = false;
				}
			}
		}

		/**
		 * reads length bytes into the passed in byte array from dtream
		 * 
		 * @param b
		 *            byte array
		 * @throws IOException
		 *             if io problems during read
		 */
		public int read(byte[] b) throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to read from closed socket");
			}

			int count = 0;
			while (count < b.length) {
				int cnt = in.read(b, count, (b.length - count));
				count += cnt;
			}

			return count;
		}
		
		public int read() throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to read from closed socket");
			}
			return in.read();
		}

		/**
		 * flushes output stream
		 * 
		 * @throws IOException
		 *             if io problems during read
		 */
		public void flush() throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to write to closed socket");
			}
			out.flush();
		}

		/**
		 * writes a byte array to the output stream
		 * 
		 * @param b
		 *            byte array to write
		 * @throws IOException
		 *             if an io error happens
		 */
		public void write(byte[] b) throws IOException {
			if (sock == null || !sock.isConnected()) {
				throw new IOException(
						"++++ attempting to write to closed socket");
			}
			out.writeInt(b.length);
			out.write(b);
		}

		/**
		 * use the sockets hashcode for this object so we can key off of SockIOs
		 * 
		 * @return int hashcode
		 */
		public int hashCode() {
			return (sock == null) ? 0 : sock.hashCode();
		}

		/**
		 * returns the string representation of this socket
		 * 
		 * @return string
		 */
		public String toString() {
			return (sock == null) ? "" : sock.toString();
		}

		/**
		 * Hack to reap any leaking children.
		 */
		protected void finalize() throws Throwable {
			try {
				if (sock != null) {
					sock.close();
					sock = null;
				}
			} catch (Throwable t) {
			} finally {
				super.finalize();
			}
		}
    }
}
