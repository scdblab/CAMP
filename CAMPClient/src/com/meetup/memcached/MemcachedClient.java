/**
 * Copyright (c) 2008 Greg Whalin
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the BSD license
 *
 * This library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.
 *
 * You should have received a copy of the BSD License along with this
 * library.
 *
 * @author Greg Whalin <greg@meetup.com> 
 */
package com.meetup.memcached;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;
import java.nio.*;          
import java.nio.channels.*;
import java.io.*;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

/**
 * This is a Memcached client for the Java platform available from
 *  <a href="http:/www.danga.com/memcached/">http://www.danga.com/memcached/</a>.
 * <br/> 
 * Supports setting, adding, replacing, deleting compressed/uncompressed and<br/>
 * serialized (can be stored as string if object is native class) objects to memcached.<br/>
 * <br/>
 * Now pulls SockIO objects from SockIOPool, which is a connection pool.  The server failover<br/>
 * has also been moved into the SockIOPool class.<br/>
 * This pool needs to be initialized prior to the client working.  See javadocs from SockIOPool.<br/>
 * <br/>
 * Some examples of use follow.<br/>
 * <h3>To create cache client object and set params:</h3>
 * <pre> 
 *	MemcachedClient mc = new MemcachedClient();
 *
 *	// compression is enabled by default	
 *	mc.setCompressEnable(true);
 *
 *	// set compression threshhold to 4 KB (default: 15 KB)	
 *	mc.setCompressThreshold(4096);
 *
 *	// turn on storing primitive types as a string representation
 *	// Should not do this in most cases.	
 *	mc.setPrimitiveAsString(true);
 * </pre>	
 * <h3>To store an object:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "cacheKey1";	
 *	Object value = SomeClass.getObject();	
 *	mc.set(key, value);
 * </pre> 
 * <h3>To store an object using a custom server hashCode:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "cacheKey1";	
 *	Object value = SomeClass.getObject();	
 *	Integer hash = new Integer(45);	
 *	mc.set(key, value, hash);
 * </pre> 
 * The set method shown above will always set the object in the cache.<br/>
 * The add and replace methods do the same, but with a slight difference.<br/>
 * <ul>
 * 	<li>add -- will store the object only if the server does not have an entry for this key</li>
 * 	<li>replace -- will store the object only if the server already has an entry for this key</li>
 * </ul> 
 * <h3>To delete a cache entry:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "cacheKey1";	
 *	mc.delete(key);
 * </pre> 
 * <h3>To delete a cache entry using a custom hash code:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "cacheKey1";	
 *	Integer hash = new Integer(45);	
 *	mc.delete(key, hashCode);
 * </pre> 
 * <h3>To store a counter and then increment or decrement that counter:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "counterKey";	
 *	mc.storeCounter(key, new Integer(100));
 *	System.out.println("counter after adding      1: " mc.incr(key));	
 *	System.out.println("counter after adding      5: " mc.incr(key, 5));	
 *	System.out.println("counter after subtracting 4: " mc.decr(key, 4));	
 *	System.out.println("counter after subtracting 1: " mc.decr(key));	
 * </pre> 
 * <h3>To store a counter and then increment or decrement that counter with custom hash:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "counterKey";	
 *	Integer hash = new Integer(45);	
 *	mc.storeCounter(key, new Integer(100), hash);
 *	System.out.println("counter after adding      1: " mc.incr(key, 1, hash));	
 *	System.out.println("counter after adding      5: " mc.incr(key, 5, hash));	
 *	System.out.println("counter after subtracting 4: " mc.decr(key, 4, hash));	
 *	System.out.println("counter after subtracting 1: " mc.decr(key, 1, hash));	
 * </pre> 
 * <h3>To retrieve an object from the cache:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "key";	
 *	Object value = mc.get(key);	
 * </pre> 
 * <h3>To retrieve an object from the cache with custom hash:</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String key   = "key";	
 *	Integer hash = new Integer(45);	
 *	Object value = mc.get(key, hash);	
 * </pre> 
 * <h3>To retrieve an multiple objects from the cache</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String[] keys      = { "key", "key1", "key2" };
 *	Map&lt;Object&gt; values = mc.getMulti(keys);
 * </pre> 
 * <h3>To retrieve an multiple objects from the cache with custom hashing</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	String[] keys      = { "key", "key1", "key2" };
 *	Integer[] hashes   = { new Integer(45), new Integer(32), new Integer(44) };
 *	Map&lt;Object&gt; values = mc.getMulti(keys, hashes);
 * </pre> 
 * <h3>To flush all items in server(s)</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	mc.flushAll();
 * </pre> 
 * <h3>To get stats from server(s)</h3>
 * <pre>
 *	MemcachedClient mc = new MemcachedClient();
 *	Map stats = mc.stats();
 * </pre> 
 *
 * @author greg whalin <greg@meetup.com> 
 * @author Richard 'toast' Russo <russor@msoe.edu>
 * @author Kevin Burton <burton@peerfear.org>
 * @author Robert Watts <robert@wattsit.co.uk>
 * @author Vin Chawla <vin@tivo.com>
 * @version 1.5
 */
public class MemcachedClient {

	// logger
	private static Logger log =
		Logger.getLogger( MemcachedClient.class.getName() );

	// return codes
	private static final String VALUE        = "VALUE";			// start of value line from server
	private static final String STATS        = "STAT";			// start of stats line from server
	private static final String ITEM         = "ITEM";			// start of item line from server
	private static final String DELETED      = "DELETED";		// successful deletion
	private static final String NOTFOUND     = "NOT_FOUND";		// record not found for delete or incr/decr
	private static final String STORED       = "STORED";		// successful store of data
	private static final String NOTSTORED    = "NOT_STORED";	// data not stored
	private static final String OK           = "OK";			// success
	private static final String END          = "END";			// end of data from server
	private static final String INVALID 	 = "INVALID";
	
	private static final String LEASEVALUE	 = "LVALUE";		// start of a lease token line from server
	private static final String LEASE	 	 = "LEASE";			// start of a lease token line from server for a hold
	private static final String NOVALUE 	 = "NOVALUE";		// no value return

	private static final String ERROR        = "ERROR";			// invalid command name from client
	private static final String CLIENT_ERROR = "CLIENT_ERROR";	// client error in input line - invalid protocol
	private static final String SERVER_ERROR = "SERVER_ERROR";	// server error

	private static final byte[] B_END        = "END\r\n".getBytes();
//	private static final byte[] B_NOTFOUND   = "NOT_FOUND\r\n".getBytes();
//	private static final byte[] B_DELETED    = "DELETED\r\r".getBytes();
//	private static final byte[] B_STORED     = "STORED\r\r".getBytes();

	// default compression threshold
	private static final int COMPRESS_THRESH = 30720;
	
	// default lease token value
	private static final long DEFAULT_TOKEN = 0;
	private static final long TOKEN_HOTMISS = 3;
	
	private static final int DEFAULT_INITIAL_BACKOFF_VALUE = 1;
	private int INITIAL_BACKOFF_VALUE = DEFAULT_INITIAL_BACKOFF_VALUE;	
	
	// Stats for leases
	private static AtomicInteger NumBackoff;

	// Max object size
	private static long MAX_OBJECT_SIZE = 1024 * 1024 * 5;
    
	// values for cache flags 
	public static final int MARKER_BYTE             = 1;
	public static final int MARKER_BOOLEAN          = 8192;
	public static final int MARKER_INTEGER          = 4;
	public static final int MARKER_LONG             = 16384;
	public static final int MARKER_CHARACTER        = 16;
	public static final int MARKER_STRING           = 32;
	public static final int MARKER_STRINGBUFFER     = 64;
	public static final int MARKER_FLOAT            = 128;
	public static final int MARKER_SHORT            = 256;
	public static final int MARKER_DOUBLE           = 512;
	public static final int MARKER_DATE             = 1024;
	public static final int MARKER_STRINGBUILDER    = 2048;
	public static final int MARKER_BYTEARR          = 4096;
	public static final int F_COMPRESSED            = 2;
	public static final int F_SERIALIZED            = 8;
	
	// flags
	private boolean sanitizeKeys;
	private boolean primitiveAsString;
	private boolean compressEnable;
	private long compressThreshold;
	private String defaultEncoding;

	// pool instance
	private SockIOPool pool;

	// which pool to use
	private String poolName;

	// optional passed in classloader
	private ClassLoader classLoader;

	// optional error handler
	private ErrorHandler errorHandler;

	private HashMap<String, Long> i_lease_list;
	private HashMap<String, Long> q_lease_list;
 
	/**
	 * Creates a new instance of MemCachedClient.
	 */
	public MemcachedClient() {
		init();
	}

	/** 
	 * Creates a new instance of MemCachedClient
	 * accepting a passed in pool name.
	 * 
	 * @param poolName name of SockIOPool
	 */
	public MemcachedClient( String poolName ) {
		this.poolName = poolName;
		init();
	}

	/** 
	 * Creates a new instance of MemCacheClient but
	 * acceptes a passed in ClassLoader.
	 * 
	 * @param classLoader ClassLoader object.
	 */
	public MemcachedClient( ClassLoader classLoader ) {
		this.classLoader = classLoader;
		init();
	}

	/** 
	 * Creates a new instance of MemCacheClient but
	 * acceptes a passed in ClassLoader and a passed
	 * in ErrorHandler.
	 * 
	 * @param classLoader ClassLoader object.
	 * @param errorHandler ErrorHandler object.
	 */
	public MemcachedClient( ClassLoader classLoader, ErrorHandler errorHandler ) {
		this.classLoader  = classLoader;
		this.errorHandler = errorHandler;
		init();
	}

	/** 
	 * Creates a new instance of MemCacheClient but
	 * acceptes a passed in ClassLoader, ErrorHandler,
	 * and SockIOPool name.
	 * 
	 * @param classLoader ClassLoader object.
	 * @param errorHandler ErrorHandler object.
	 * @param poolName SockIOPool name
	 */
	public MemcachedClient( ClassLoader classLoader, ErrorHandler errorHandler, String poolName ) {
		this.classLoader  = classLoader;
		this.errorHandler = errorHandler;
		this.poolName     = poolName;
		init();
	}

	/** 
	 * Initializes client object to defaults.
	 *
	 * This enables compression and sets compression threshhold to 15 KB.
	 */
	private void init() {
		this.sanitizeKeys       = true;
		this.primitiveAsString  = false;
		this.compressEnable     = true;
		this.compressThreshold  = COMPRESS_THRESH;
		this.defaultEncoding    = "UTF-8";
		this.poolName           = ( this.poolName == null ) ? "default" : this.poolName;
		
		// Keep track of gumball timestamps for each get-miss
		this.i_lease_list			= new HashMap<String, Long>();
		
		// Keep track of leases for write-hold operations
		this.q_lease_list			= new HashMap<String, Long>();

		// get a pool instance to work with for the life of this instance
		this.pool               = SockIOPool.getInstance( poolName );
		
		if(NumBackoff == null) {
			NumBackoff = new AtomicInteger(0);
		}
	}
	
	public void disableBackoff() {
		this.INITIAL_BACKOFF_VALUE = 0;
	}
	
	public void enableBackoff() {
		this.INITIAL_BACKOFF_VALUE = DEFAULT_INITIAL_BACKOFF_VALUE;
	}

	/** 
	 * Sets an optional ClassLoader to be used for
	 * serialization.
	 * 
	 * @param classLoader 
	 */
	public void setClassLoader( ClassLoader classLoader ) {
		this.classLoader = classLoader;
	}

	/** 
	 * Sets an optional ErrorHandler.
	 * 
	 * @param errorHandler 
	 */
	public void setErrorHandler( ErrorHandler errorHandler ) {
		this.errorHandler = errorHandler;
	}

	/** 
	 * Enables/disables sanitizing keys by URLEncoding.
	 * 
	 * @param sanitizeKeys if true, then URLEncode all keys
	 */
	public void setSanitizeKeys( boolean sanitizeKeys ) {
		this.sanitizeKeys = sanitizeKeys;
	}

	/** 
	 * Enables storing primitive types as their String values. 
	 * 
	 * @param primitiveAsString if true, then store all primitives as their string value.
	 */
	public void setPrimitiveAsString( boolean primitiveAsString ) {
		this.primitiveAsString = primitiveAsString;
	}

	/** 
	 * Sets default String encoding when storing primitives as Strings. 
	 * Default is UTF-8.
	 * 
	 * @param defaultEncoding 
	 */
	public void setDefaultEncoding( String defaultEncoding ) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Enable storing compressed data, provided it meets the threshold requirements.
	 *
	 * If enabled, data will be stored in compressed form if it is<br/>
	 * longer than the threshold length set with setCompressThreshold(int)<br/>
	 *<br/>
	 * The default is that compression is enabled.<br/>
	 *<br/>
	 * Even if compression is disabled, compressed data will be automatically<br/>
	 * decompressed.
	 *
	 * @param compressEnable <CODE>true</CODE> to enable compression, <CODE>false</CODE> to disable compression
	 */
	public void setCompressEnable( boolean compressEnable ) {
		this.compressEnable = compressEnable;
	}
    
	/**
	 * Sets the required length for data to be considered for compression.
	 *
	 * If the length of the data to be stored is not equal or larger than this value, it will
	 * not be compressed.
	 *
	 * This defaults to 15 KB.
	 *
	 * @param compressThreshold required length of data to consider compression
	 */
	public void setCompressThreshold( long compressThreshold ) {
		this.compressThreshold = compressThreshold;
	}
	
	public void setLeaseToken(String key, long token) {
		this.i_lease_list.put(key, token);
	}
	
	public long getLeaseToken(String key) {
		return this.i_lease_list.get(key);
	}
		
	public long getMaxObjectSize() {
		return MAX_OBJECT_SIZE;
	}
	
	public void setMaxObjectSize(long value) {
		MAX_OBJECT_SIZE = value;
	}

	/** 
	 * Checks to see if key exists in cache. 
	 * 
	 * @param key the key to look for
	 * @return true if key found in cache, false if not (or if cache is down)
	 */
	public boolean keyExists( String key ) {
		return ( this.get( key, null, true, false ) != null );
	}
	
	/**
	 * Quarantine and register for a list of keys
	 * @author hieun
	 * @throws Exception 
	 */
	public boolean quarantineAndRegister(String tid, String key) throws Exception {		
		if ( key == null ) {
			log.error( "null value for key passed to quarantineAndRegister()" );
			return false;
		}
		
		try {
			key = sanitizeKey( key );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, key );

			log.error( "failed to sanitize your key!", e );
			return false;
		}
		
		// get SockIO obj from hash or from key
		SockIOPool.SockIO sock = pool.getSock( key, null );

		// return false if unable to get SockIO obj
		if ( sock == null ) {
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, new IOException( "no socket to server available" ), key );
			return false;
		}

		// build command
		StringBuilder command = new StringBuilder( "qareg " ).append( key + " " ).append(tid);
		command.append( "\r\n" );			
		
		try {
			sock.write( command.toString().getBytes() );
			sock.flush();
			
			// if we get appropriate response back, then we return true
			String line = sock.readLine();
			if ( line.contains(LEASE) ) {
				String[] info 	= line.split(" ");
				int markedVal = Integer.parseInt( info[1] );
				
				// no lease is granted
				if (markedVal == 0) {
					if ( log.isInfoEnabled() )
						log.info( "++++ cannot grant lease of key " + key + " because someone is holding" );
					
					return false;
				}
				
				if ( log.isInfoEnabled() )
					log.info( "++++ lease of key: " + key + " from cache was a success" );

				// return sock to pool and bail here
				sock.close();
				sock = null;
				return true;
			}
			else if ( NOTFOUND.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ lease of key: " + key + " from cache failed as the key was not found" );
			}
			else {
				log.error( "++++ error qareg key: " + key );
				log.error( "++++ server response: " + line );
				
				sock.close();
				sock = null;
				throw new Exception("Server error on QaReg request (" + tid + " " + key +"): " + line + 
						" \nCommand = " + command);
			}
		}
		catch ( IOException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, key );

			// exception thrown
			log.error( "++++ exception thrown while writing bytes to server on delete" );
			log.error( e.getMessage(), e );

			try {
				sock.trueClose();
			}
			catch ( IOException ioe ) {
				log.error( "++++ failed to close socket : " + sock.toString() );
			}

			sock = null;
		}

		if ( sock != null ) {
			sock.close();
			sock = null;
		}

		return false;
	}
	
	public boolean deleteAndRelease(String tid) throws Exception {
		if ( tid == null ) {
			log.error( "null value for key passed to deleteAndRelease()" );
			return false;
		}
		
		try {
			tid = sanitizeKey( tid );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, tid );

			log.error( "failed to sanitize your key!", e );
			return false;
		}
		
		// get SockIO obj from hash or from key
		SockIOPool.SockIO sock = pool.getSock( tid, null );

		// return false if unable to get SockIO obj
		if ( sock == null ) {
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, new IOException( "no socket to server available" ), tid );
			return false;
		}

		// build command
		StringBuilder command = new StringBuilder( "dar " ).append( tid );
		command.append( "\r\n" );			
		
		try {
			sock.write( command.toString().getBytes() );
			sock.flush();
			
			// if we get appropriate response back, then we return true
			String line = sock.readLine();
			if ( DELETED.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ dar of transaction id: " + tid + " from cache was a success" );

				// return sock to pool and bail here
				sock.close();
				sock = null;
				return true;
			} else if (INVALID.equals( line )) { 
				if ( log.isInfoEnabled() )
					log.info( "++++ dar of transaction id: " + tid + " from cache found no tid or item" );

				// return sock to pool and bail here
				sock.close();
				sock = null;
				return false;				
			} else if ( NOTFOUND.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ dar of key: " + tid + " from cache failed as the key was not found" );
			}
			else {
				log.error( "++++ error dar key: " + tid );
				log.error( "++++ server response: " + line );
				
				sock.close();
				sock = null;
				throw new Exception("Server error on DaR request (" + tid + " ): " + line + 
						" \nCommand = " + command);
			}
		}
		catch ( IOException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, tid );

			// exception thrown
			log.error( "++++ exception thrown while writing bytes to server on delete" );
			log.error( e.getMessage(), e );

			try {
				sock.trueClose();
			}
			catch ( IOException ioe ) {
				log.error( "++++ failed to close socket : " + sock.toString() );
			}

			sock = null;
		}

		if ( sock != null ) {
			sock.close();
			sock = null;
		}

		return false;		
	}

	/**
	 * Deletes an object from cache given cache key.
	 *
	 * @param key the key to be removed
	 * @return <code>true</code>, if the data was deleted successfully
	 * @throws Exception 
	 */
	public boolean delete( String key ) throws Exception {
		return delete( key, null, null );
	}

	/** 
	 * Deletes an object from cache given cache key and expiration date. 
	 * 
	 * @param key the key to be removed
	 * @param expiry when to expire the record.
	 * @return <code>true</code>, if the data was deleted successfully
	 * @throws Exception 
	 */
	public boolean delete( String key, Date expiry ) throws Exception {
		return delete( key, null, expiry );
	}

	/**
	 * Deletes an object from cache given cache key, a delete time, and an optional hashcode.
	 *
	 *  The item is immediately made non retrievable.<br/>
	 *  Keep in mind {@link #add(String, Object) add} and {@link #replace(String, Object) replace}<br/>
	 *  will fail when used with the same key will fail, until the server reaches the<br/>
	 *  specified time. However, {@link #iqset(String, Object) set} will succeed,<br/>
	 *  and the new value will not be deleted.
	 *
	 * @param key the key to be removed
	 * @param hashCode if not null, then the int hashcode to use
	 * @param expiry when to expire the record.
	 * @return <code>true</code>, if the data was deleted successfully
	 * @throws Exception 
	 */
	public boolean delete( String key, Integer hashCode, Date expiry ) throws Exception {

		if ( key == null ) {
			log.error( "null value for key passed to delete()" );
			return false;
		}

		try {
			key = sanitizeKey( key );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, key );

			log.error( "failed to sanitize your key!", e );
			return false;
		}

		// get SockIO obj from hash or from key
		SockIOPool.SockIO sock = pool.getSock( key, hashCode );

		// return false if unable to get SockIO obj
		if ( sock == null ) {
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, new IOException( "no socket to server available" ), key );
			return false;
		}

		// build command
		StringBuilder command = new StringBuilder( "delete " ).append( key );
		if ( expiry != null )
			command.append( " " + expiry.getTime() / 1000 );

		command.append( "\r\n" );
		
		try {
			sock.write( command.toString().getBytes() );
			sock.flush();
			
			// if we get appropriate response back, then we return true
			String line = sock.readLine();
			if ( DELETED.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ deletion of key: " + key + " from cache was a success" );

				// return sock to pool and bail here
				sock.close();
				sock = null;
				return true;
			}
			else if ( NOTFOUND.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ deletion of key: " + key + " from cache failed as the key was not found" );
			}
			else {
				log.error( "++++ error deleting key: " + key );
				log.error( "++++ server response: " + line );
				
				sock.close();
				sock = null;
				throw new Exception("Server error on Delete request ("+ key +"): " + line + 
						" \nCommand = " + command);
			}
		}
		catch ( IOException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, key );

			// exception thrown
			log.error( "++++ exception thrown while writing bytes to server on delete" );
			log.error( e.getMessage(), e );

			try {
				sock.trueClose();
			}
			catch ( IOException ioe ) {
				log.error( "++++ failed to close socket : " + sock.toString() );
			}

			sock = null;
		}

		if ( sock != null ) {
			sock.close();
			sock = null;
		}

		return false;
	}
	
//	/**
//	 * Obtains a write-lease hold on an object from cache given cache key.
//	 * No other process may set the value for the held key while the hold is
//	 * still valid.
//	 *
//	 * @param key the key to be held
//	 * @return <code>true</code>, if the write-hold was acquired successfully
//	 * @throws Exception 
//	 */
//	public boolean writeHold( String key ) throws Exception {
//		return writeHold( key, null, null );
//	}

	
//	/**
//	 * Obtains a write-lease hold on an object from cache given cache key
//	 * and expiration date.
//	 * No other process may set the value for the held key while the hold is
//	 * still valid.
//	 *
//	 * @param key the key to be held
//	 * @param expiry when to expire the record.
//	 * @return <code>true</code>, if the write-hold was acquired successfully
//	 * @throws Exception 
//	 */
//	public boolean writeHold( String key, Date expiry ) throws Exception {
//		return writeHold( key, null, expiry );
//	}

//	/**
//	 * Obtains a write-lease hold on an object from cache given cache key,
//	 * an expiration date and an optional hashcode.
//	 * No other process may set the value for the held key while the hold is
//	 * still valid.
//	 *
//	 * @param key the key to be held
//	 * @param hashCode if not null, then the int hashcode to use
//	 * @param expiry when to expire the record.
//	 * @return <code>true</code>, if the write-hold was acquired successfully
//	 * @throws Exception 
//	 */
//	public boolean writeHold( String key, Integer hashCode, Date expiry ) throws Exception {
//
//		if ( key == null ) {
//			log.error( "null value for key passed to delete()" );
//			return false;
//		}
//
//		try {
//			key = sanitizeKey( key );
//		}
//		catch ( UnsupportedEncodingException e ) {
//
//			// if we have an errorHandler, use its hook
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnHold( this, e, key );
//
//			log.error( "failed to sanitize your key!", e );
//			return false;
//		}
//
//		// get SockIO obj from hash or from key
//		SockIOPool.SockIO sock = pool.getSock( key, hashCode );
//
//		// return false if unable to get SockIO obj
//		if ( sock == null ) {
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnHold( this, new IOException( "no socket to server available" ), key );
//			return false;
//		}
//
//		// build command
//		StringBuilder command = new StringBuilder( "hold " ).append( key );
//		if ( expiry != null )
//			command.append( " " + expiry.getTime() / 1000 );
//
//		command.append( "\r\n" );
//		
//		try {
//			sock.write( command.toString().getBytes() );
//			sock.flush();
//			
//			// if we get appropriate response back, then we return true
//			String line = sock.readLine();
//			if ( line.startsWith( LEASE ) ) {
//				String[] info 	= line.split(" ");
////				String currkey = info[1];
////				int flag      = Integer.parseInt( info[2] );
//				long token_value = Long.parseLong( info[1] );
////
////				if(token_value == TOKEN_HOTMISS) {
////					value_found = false;
////					incrementCounter(NumBackoff);
//////					System.out.println("Number of exponential backoff occurrences: " + NumBackoff.get() + 
//////							", backoff=" + backoff + " ms");
////				} else {
//					this.q_lease_list.put(key, token_value);
////					value_found = true;
////				}
//				
//				if ( log.isInfoEnabled() )
//					log.info( "++++ hold of key: " + key + " from cache was a success" );
//
//				// return sock to pool and bail here
//				sock.close();
//				sock = null;
//				return true;
//			}
//			else {
//				log.error( "++++ error obtaining hold on key: " + key );
//				log.error( "++++ server response: " + line );
//				
//				sock.close();
//				sock = null;
//				throw new Exception("Server error on Hold request ("+ key +"): " + line + 
//						" \nCommand = " + command);
//			}
//		}
//		catch ( IOException e ) {
//
//			// if we have an errorHandler, use iurrts hook
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnHold( this, e, key );
//
//			// exception thrown
//			log.error( "++++ exception thrown while writing bytes to server on hold" );
//			log.error( e.getMessage(), e );
//
//			try {
//				sock.trueClose();
//			}
//			catch ( IOException ioe ) {
//				log.error( "++++ failed to close socket : " + sock.toString() );
//			}
//
//			sock = null;
//		}
//
//		return false;
//	}
	
	
//	/**
//	 * Obtains a write-lease hold on an object from cache given cache key.
//	 * No other process may set the value for the held key while the hold is
//	 * still valid.
//	 *
//	 * @param key the key to be held
//	 * @return <code>true</code>, if the write-hold was acquired successfully
//	 * @throws Exception 
//	 */
//	public boolean releaseHold( String key ) throws Exception {
//		return releaseHold( key, null, null );
//	}

//	
//	/**
//	 * Obtains a write-lease hold on an object from cache given cache key
//	 * and expiration date.
//	 * No other process may set the value for the held key while the hold is
//	 * still valid.
//	 *
//	 * @param key the key to be held
//	 * @param expiry when to expire the record.
//	 * @return <code>true</code>, if the write-hold was acquired successfully
//	 * @throws Exception 
//	 */
//	public boolean releaseHold( String key, Date expiry ) throws Exception {
//		return releaseHold( key, null, expiry );
//	}

//	/**
//	 * Releases a write-lease hold on an object from cache given cache key,
//	 * an expiration date and an optional hashcode.
//	 * No other process may set the value for the held key while the hold is
//	 * still valid.
//	 *
//	 * @param key the key to be released
//	 * @param hashCode if not null, then the int hashcode to use
//	 * @param expiry when to expire the record.
//	 * @return <code>true</code>, if the write-hold was acquired successfully
//	 * @throws Exception 
//	 */
//	public boolean releaseHold( String key, Integer hashCode, Date expiry ) throws Exception {
//
//		if ( key == null ) {
//			log.error( "null value for key passed to delete()" );
//			return false;
//		}
//
//		try {
//			key = sanitizeKey( key );
//		}
//		catch ( UnsupportedEncodingException e ) {
//
//			// if we have an errorHandler, use its hook
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnDelete( this, e, key );
//
//			log.error( "failed to sanitize your key!", e );
//			return false;
//		}
//
//		// get SockIO obj from hash or from key
//		SockIOPool.SockIO sock = pool.getSock( key, hashCode );
//
//		// return false if unable to get SockIO obj
//		if ( sock == null ) {
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnDelete( this, new IOException( "no socket to server available" ), key );
//			return false;
//		}
//
//		// build command
//		StringBuilder command = new StringBuilder( "release " ).append( key );
//		if ( this.q_lease_list.get(key) == null ) {
//			command.append(" " + DEFAULT_TOKEN);
//			System.out.println("Warning: Lease token not found for release");
//			log.warn( "++++ release of key: " + key + " could not find lease token" );
//		} else {
//			command.append(" " + this.q_lease_list.get(key));
//		}
//		
//		if ( expiry != null )
//			command.append( " " + expiry.getTime() / 1000 );
//
//		command.append( "\r\n" );
//		
//		try {
//			sock.write( command.toString().getBytes() );
//			sock.flush();
//			
//			// if we get appropriate response back, then we return true
//			String line = sock.readLine();
//			if ( DELETED.equals( line ) ) {
//				if ( log.isInfoEnabled() )
//					log.info( "++++ release of key: " + key + " from cache was a success" );
//
//				// return sock to pool and bail here
//				sock.close();
//				sock = null;
//				return true;
//			}
//			else if ( NOTFOUND.equals( line ) ) {
//				if ( log.isInfoEnabled() )
//					log.info( "++++ release of key: " + key + " from cache failed as the key was not found" );
//			}
//			else {
//				log.error( "++++ error releasing key: " + key );
//				log.error( "++++ server response: " + line );
//				
//				sock.close();
//				sock = null;
//				throw new Exception("Server error on Release request ("+ key +"): " + line + 
//						" \nCommand = " + command);
//			}
//		}
//		catch ( IOException e ) {
//
//			// if we have an errorHandler, use its hook
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnDelete( this, e, key );
//
//			// exception thrown
//			log.error( "++++ exception thrown while writing bytes to server on delete" );
//			log.error( e.getMessage(), e );
//
//			try {
//				sock.trueClose();
//			}
//			catch ( IOException ioe ) {
//				log.error( "++++ failed to close socket : " + sock.toString() );
//			}
//
//			sock = null;
//		}
//
//		if ( sock != null ) {
//			sock.close();
//			sock = null;
//		}
//
//		return false;
//	}
	
	/***
	 * Read the key and attempt to Quarantine it. 
	 * The Quarantine may fail if another thread/client holds a Q-lease on the 
	 * key. In this case, an IQException is thrown.
	 * If the value for this key exists in the cache, it is returned. If not, 
	 * null is returned. A null value indicates that the Quarantine was successful.
	 * 
	 * @param key	Key of the key-value pair to lookup in the cache.
	 * @return The value if it exists in the cache. If not, returns null.
	 * @throws IQException
	 * @throws Exception
	 */
	public Object quarantineAndRead(String key) throws IQException, Exception {
		return quarantineAndRead(key, null, false);
	}
	
	public Object quarantineAndRead(
			String key, 
			Integer hashCode, 
			boolean asString) throws IQException, Exception {
		Object result = null;
		
		try {
			// This function is just a wrapper to handle the error case when
			// a Quarantine request fails. readAndQuarantineMain performs the actual
			// logic.
			result = quarantineAndReadMain(key, hashCode, asString);
		} catch (IQException e) {
			// Failed to quarantine the key. Handle by freeing up any acquired leases.
			for (String cleanup_key : q_lease_list.keySet()) {
				if (q_lease_list.get(cleanup_key) == null) {
					System.out.println("Error, lease token missing for " + cleanup_key);
				}
				
				if (!releaseX(cleanup_key, q_lease_list.get(cleanup_key))) {
					throw new IQException("Error releasing xLease during cleanup (after failed xLease)");
				}
			}
			
			// Reset the hold_list to empty
			q_lease_list = new HashMap<String, Long>();
			
			for (String k : i_lease_list.keySet()) {
				if (i_lease_list.get(k) == null) {
					System.out.println("Error, lease token missing for " + k);					
				}
				
				if (!releaseX(k, i_lease_list.get(k))) {
					throw new IQException("Error releasing Lease during cleanup (after failed xLease)");					
				}
			}
			
			i_lease_list = new HashMap<String, Long>();
			
			throw e;
		}
		
		return result;
	}
	
	private Object quarantineAndReadMain(String key, Integer hashCode, boolean asString) throws IQException {
		if ( key == null ) {
			log.error( "key is null for get()" );
			return null;
		}

		try {
			key = sanitizeKey( key );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnGet( this, e, key );

			log.error( "failed to sanitize your key!", e );
			return null;
		}

		// get SockIO obj using cache key
		SockIOPool.SockIO socket = null;
		String cmd = "qaread " + key;
		
		Long lease_token = new Long(0L);
		if (this.q_lease_list.containsKey(key)) {
			lease_token = this.q_lease_list.get(key);
		} else if (this.i_lease_list.containsKey(key)) {
			lease_token = this.i_lease_list.get(key);
		}		
		
		if (lease_token.longValue() != 0L) {
			cmd += " " + lease_token.longValue() + "\r\n";
		} else {
			cmd += "\r\n";
		}
		boolean value_found = false;
		// ready object
		Object o = null;

		socket = pool.getSock( key, hashCode );

		if ( socket == null ) {
			if ( errorHandler != null )
				errorHandler.handleErrorOnGet( this, new IOException( "no socket to server available" ), key );
			return null;
		}

		if(this.q_lease_list == null) 
		{
			this.q_lease_list = new HashMap<String, Long>();
		}
		
		try {
			if ( log.isDebugEnabled() )
				log.debug("++++ memcache raq command: " + cmd);


			socket.write( cmd.getBytes() );
			socket.flush();

			while ( true ) {
				String line = socket.readLine();
				long token_value = 0;

				if ( log.isDebugEnabled() )
					log.debug( "++++ line: " + line );

				if ( line.startsWith( LEASE ) || line.startsWith( VALUE )) {				
					// Quarantine was successful and value was found.
					String[] info = line.split(" ");
					int flag      = Integer.parseInt( info[2] );
					
					if (line.startsWith( LEASE )) {
						token_value = Long.parseLong( info[3] );
					}
					
					int length    = Integer.parseInt( info[4] );

					if ( log.isDebugEnabled() ) {
						log.debug( "++++ key: " + key );
						log.debug( "++++ flags: " + flag );
						log.debug( "++++ length: " + length );
					}
					
					// Handle the QLease token_value
					if (line.startsWith( LEASE )) {
						if (token_value > TOKEN_HOTMISS) {
							this.q_lease_list.put(key, token_value);
						} else {
							socket.close();
							socket = null;
							throw new IQException("Invalid token value("+
									token_value +") observed for " +
									"RaQ of key:" + key);
						}
					}

					// Handle the value
					// read obj into buffer
					byte[] buf = new byte[length];
					socket.read( buf );
					socket.clearEOL();

					value_found = true;

					if ( (flag & F_COMPRESSED) == F_COMPRESSED ) {
						try {
							// read the input stream, and write to a byte array output stream since
							// we have to read into a byte array, but we don't know how large it
							// will need to be, and we don't want to resize it a bunch
							GZIPInputStream gzi = new GZIPInputStream( new ByteArrayInputStream( buf ) );
							ByteArrayOutputStream bos = new ByteArrayOutputStream( buf.length );

							int count;
							byte[] tmp = new byte[2048];
							while ( (count = gzi.read(tmp)) != -1 ) {
								bos.write( tmp, 0, count );
							}

							// store uncompressed back to buffer
							buf = bos.toByteArray();
							gzi.close();
						}
						catch ( IOException e ) {

							// if we have an errorHandler, use its hook
							if ( errorHandler != null )
								errorHandler.handleErrorOnGet( this, e, key );

							log.error( "++++ IOException thrown while trying to uncompress input stream for key: " + key + " -- " + e.getMessage() );
							throw new NestedIOException( "++++ IOException thrown while trying to uncompress input stream for key: " + key, e );
						}
					}

					// we can only take out serialized objects
					if ( ( flag & F_SERIALIZED ) != F_SERIALIZED ) {
						if ( primitiveAsString || asString ) {
							// pulling out string value
							if ( log.isInfoEnabled() )
								log.info( "++++ retrieving object and stuffing into a string." );
							o = new String( buf, defaultEncoding );
						}
						else {
							// decoding object
							try {
								o = NativeHandler.decode( buf, flag );    
							}
							catch ( Exception e ) {

								// if we have an errorHandler, use its hook
								if ( errorHandler != null )
									errorHandler.handleErrorOnGet( this, e, key );

								log.error( "++++ Exception thrown while trying to deserialize for key: " + key, e );
								throw new NestedIOException( e );
							}
						}
					}
					else {
						// deserialize if the data is serialized
						ContextObjectInputStream ois =
								new ContextObjectInputStream( new ByteArrayInputStream( buf ), classLoader );
						try {
							o = ois.readObject();
							if ( log.isInfoEnabled() )
								log.info( "++++ deserializing " + o.getClass() );
						}
						catch ( Exception e ) {
							if ( errorHandler != null )
								errorHandler.handleErrorOnGet( this, e, key );

							o = null;
							log.error( "++++ Exception thrown while trying to deserialize for key: " + key + " -- " + e.getMessage() );
						}
					}
				} else if ( line.startsWith( LEASEVALUE ) || line.startsWith( NOVALUE )) {
					// Quarantine was successful and QLease granted but value
					// was not found.
					String[] info 	= line.split(" ");
					String currkey = info[1];
					
					if (line.startsWith( LEASEVALUE ))
						token_value = Long.parseLong( info[3] );

					value_found = false;
					
					if (line.startsWith( LEASEVALUE )) {
						if(token_value == TOKEN_HOTMISS) {
	//						incrementCounter(NumBackoff);
	//							System.out.println("Number of exponential backoff occurrences: " + NumBackoff.get() + 
	//									", backoff=" + backoff + " ms");
						} else {
							this.q_lease_list.put(currkey, token_value);
						}
					}
				} else if ( line.startsWith( INVALID ) ) {
					// Quarantine was not successful. Throw IQException to signify this.
					socket.close();
					socket = null;
					throw new IQException("Failed QaRead to quarantine key: " + key);
				} else if ( END.equals( line ) ) {
					if ( log.isDebugEnabled() )
						log.debug( "++++ finished reading from cache server" );
					break;
				}

			}

			socket.close();
			socket = null;

		} catch ( IOException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnGet( this, e, key );

			// exception thrown
			log.error( "++++ exception thrown while trying to get object from cache for key: " + key + " -- " + e.getMessage() );

			try {
				socket.trueClose();
			}
			catch ( IOException ioe ) {
				log.error( "++++ failed to close socket : " + socket.toString() );
			}
			socket = null;
		}
				

		if(value_found) {
			return o;
		}

		return null;
	}
	
//	public boolean quarantineAndCompare2(String key, Object value) throws Exception {
//		boolean success = quarantineAndCompareMain2(key, value);
//		if (!success) {
//			// Clean up xLeases already acquired.
//			for (String cleanup_key : q_lease_list.keySet()) {
//				if (q_lease_list.get(cleanup_key) == null) {
//					System.out.println("Error, lease token missing for " + cleanup_key);
//				}
//				
//				if (!releaseX(cleanup_key, q_lease_list.get(cleanup_key))) {
//					throw new Exception("Error releasing xLease during cleanup (after failed xLease)");
//				}
//			}
//			
//			// Reset the hold_list to empty
//			q_lease_list = new HashMap<String, Long>();
//			
//			for (String k : i_lease_list.keySet()) {
//				if (i_lease_list.get(k) == null) {
//					System.out.println("Error, lease token missing for " + k);					
//				}
//				
//				if (!releaseX(k, i_lease_list.get(k))) {
//					throw new Exception("Error releasing Lease during cleanup (after failed xLease)");					
//				}
//			}
//			
//			i_lease_list = new HashMap<String, Long>();
//		}
//		
//		return success;
//	}
//	private boolean quarantineAndCompareMain2(String key, Object value) throws Exception {
//		if (value == null)
//			return false;
//
//		if ( key == null ) {
//			log.error( "null value for key passed to delete()" );
//			return false;
//		}
//
//		try {
//			key = sanitizeKey( key );
//		}
//		catch ( UnsupportedEncodingException e ) {
//
//			// if we have an errorHandler, use its hook
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnHold( this, e, key );
//
//			log.error( "failed to sanitize your key!", e );
//			return false;
//		}
//
//		// get SockIO obj from hash or from key
//		SockIOPool.SockIO sock = pool.getSock( key, null );
//
//		// return false if unable to get SockIO obj
//		if ( sock == null ) {
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnHold( this, new IOException( "no socket to server available" ), key );
//			return false;
//		}
//
//		// build command
//		StringBuilder command = new StringBuilder( "xlease " ).append( key );
////		command.append( " " + hashCode );
//		command.append( "\r\n" );
//		
//		try {
//			sock.write( command.toString().getBytes() );
//			sock.flush();
//			
//			// if we get appropriate response back, then we return true
//			String line = sock.readLine();
//			if ( line.startsWith( LEASE ) ) {
//				String[] info 	= line.split(" ");
////				String currkey = info[1];
////				int flag      = Integer.parseInt( info[2] );
//				long token_value = Long.parseLong( info[1] );
////
////				if(token_value == TOKEN_HOTMISS) {
////					value_found = false;
////					incrementCounter(NumBackoff);
//////					System.out.println("Number of exponential backoff occurrences: " + NumBackoff.get() + 
//////							", backoff=" + backoff + " ms");
////				} else { byte[]
//				
//				if (token_value > TOKEN_HOTMISS) {
//					this.q_lease_list.put(key, token_value);
//					
//					if ( log.isInfoEnabled() )
//						log.info( "++++ hold of key: " + key + " from cache was a success" );
//				}
//
//				// return sock to pool and bail here
//				sock.close();
//				sock = null;
//				if (token_value > TOKEN_HOTMISS) {
//					// Lease token was obtained from the cache server.
//					return true;
//				} else {
//					// Lease token could not be obtained.
//					return false;
//				}
//			}
//			else {
//				log.error( "++++ error obtaining hold on key: " + key );
//				log.error( "++++ server response: " + line );
//				
//				sock.close();
//				sock = null;
//				throw new Exception("Server error on Hold request ("+ key +"): " + line + 
//						" \nCommand = " + command);
//			}
//		}
//		catch ( IOException e ) {
//
//			// if we have an errorHandler, use iurrts hook
//			if ( errorHandler != null )
//				errorHandler.handleErrorOnHold( this, e, key );
//
//			// exception thrown
//			log.error( "++++ exception thrown while writing bytes to server on hold" );
//			log.error( e.getMessage(), e );
//
//			try {
//				sock.trueClose();
//			}
//			catch ( IOException ioe ) {
//				log.error( "++++ failed to close socket : " + sock.toString() );
//			}
//
//			sock = null;
//		}
//
//		return false;
//	}
	
	public void swapAndRelease(String key, Object value) throws IOException {
		if (q_lease_list.get(key) != null) {
			try {
				set( "sar", key, value, null, null, primitiveAsString, true );
			} catch (IQException e) {}
//				System.out.println("cannot swap and release: " + key);
			q_lease_list.remove(key);
		}

		return;
	}
	
	public boolean releaseX(String key) throws Exception {
		Long lease_token = i_lease_list.get(key);
		if (lease_token == null) {
			return false;
		}
		
		boolean success = releaseX(key, lease_token);
		
		// TODO: do a different action if it succeeds or fails?
		// Remove entry from lease_list
		i_lease_list.remove(key);
		return success;
	}
	
	private boolean releaseX(String key, Long lease_token) throws Exception {
		return releaseX(key, lease_token, null);
	}
	
	/***
	 * Release an xLease on a key.
	 * @param key
	 * @param lease_token
	 * @param hashcode
	 * @return
	 * @throws IOException
	 */
	private boolean releaseX(String key, Long lease_token, Integer hashCode) throws Exception {
		if ( key == null || lease_token == null ) {
			log.error( "null value for key passed to delete()" );
			return false;
		}

		try {
			key = sanitizeKey( key );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, key );

			log.error( "failed to sanitize your key!", e );
			return false;
		}

		// get SockIO obj from hash or from key
		SockIOPool.SockIO sock = pool.getSock( key, hashCode );

		// return false if unable to get SockIO obj
		if ( sock == null ) {
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, new IOException( "no socket to server available" ), key );
			return false;
		}

		// build command
		boolean callSuccess = false;
		StringBuilder command = new StringBuilder( "unlease " ).append( key );
		command.append( " " + lease_token.toString() );
//		if ( expiry != null )
//			command.append( " " + expiry.getTime() / 1000 );

		command.append( "\r\n" );
		
		try {
			sock.write( command.toString().getBytes() );
			sock.flush();
			
			// if we get appropriate response back, then we return true
			String line = sock.readLine();
			if ( DELETED.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ deletion of key: " + key + " from cache was a success" );

				callSuccess = true;
			}
			else if ( NOTFOUND.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ deletion of key: " + key + " from cache failed as the key was not found" );
				
				// It's ok if the key was not found. That means there was no lease on this key.
				callSuccess = true;
			}
			else {
				log.error( "++++ error deleting key: " + key );
				log.error( "++++ server response: " + line );
				
				sock.close();
				sock = null;
				throw new Exception("Server error on Unlease request ("+ key +"): " + line + 
						" \nCommand = " + command);
			}
		}
		catch ( IOException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnDelete( this, e, key );

			// exception thrown
			log.error( "++++ exception thrown while writing bytes to server on delete" );
			log.error( e.getMessage(), e );

			try {
				sock.trueClose();
			}
			catch ( IOException ioe ) {
				log.error( "++++ failed to close socket : " + sock.toString() );
			}

			sock = null;
		}

		if ( sock != null ) {
			sock.close();
			sock = null;
		}

		return callSuccess;
	}
	
    
	/**
	 * Stores data on the server; only the key and the value are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean set( String key, Object value ) throws IOException {
		try {
			return set( "set", key, value, null, null, primitiveAsString, false );
		} catch (IQException e) {}
		
		return false;
	}

	/**
	 * Stores data on the server; only the key and the value are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean set( String key, Object value, Integer hashCode ) throws IOException {
		try {
			return set( "set", key, value, null, hashCode, primitiveAsString, false );
		} catch (IQException e) {}
		
		return false;
	}

	/**
	 * Stores data on the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean set( String key, Object value, Date expiry ) throws IOException {
		try {
			return set( "set", key, value, expiry, null, primitiveAsString, false );
		} catch (Exception e) {}
		
		return false;
	}

	/**
	 * Stores data on the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean set( String key, Object value, Date expiry, Integer hashCode ) throws IOException {
		try {
			return set( "set", key, value, expiry, hashCode, primitiveAsString, false );
		} catch (IQException e) { }
		
		return false;
	}

    public boolean append( String key, Object value) throws IOException {
    	try {
    		return set("append", key, value, null, null, primitiveAsString, false);
    	} catch (IQException e) {}
    	
    	return false;
    }
     
    public boolean prepend( String key, Object value) throws IOException {
    	try {
    		return set("prepend", key, value, null, null, primitiveAsString, false);
    	} catch (IQException e) {}
    	
    	return false;
    }
    
	/**
	 * Stores data on the server; only the key and the value are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 * @throws IQException 
	 */
	public boolean iqset( String key, Object value ) throws IOException, IQException {
		return set( "iqset", key, value, null, null, primitiveAsString, false );
	}

	/**
	 * Stores data on the server; only the key and the value are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 * @throws IQException 
	 */
	public boolean iqset( String key, Object value, Integer hashCode ) throws IOException, IQException {
		return set( "iqset", key, value, null, hashCode, primitiveAsString, false );
	}

	/**
	 * Stores data on the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 * @throws IQException 
	 */
	public boolean iqset( String key, Object value, Date expiry ) throws IOException, IQException {
		return set( "iqset", key, value, expiry, null, primitiveAsString, false );
	}

	/**
	 * Stores data on the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 * @throws IQException 
	 */
	public boolean iqset( String key, Object value, Date expiry, Integer hashCode ) throws IOException, IQException {
		return set( "iqset", key, value, expiry, hashCode, primitiveAsString, false );
	}	

	/**
	 * Adds data to the server; only the key and the value are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean add( String key, Object value ) throws IOException {
		try {
			return set( "add", key, value, null, null, primitiveAsString, false );		
		} catch (IQException e) {}
		
		return false;
	}

	/**
	 * Adds data to the server; the key, value, and an optional hashcode are passed in.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean add( String key, Object value, Integer hashCode ) throws IOException {
		try {
			return set( "add", key, value, null, hashCode, primitiveAsString, false );
		} catch (IQException e) {
			
		}
		
		return false;
	}

	/**
	 * Adds data to the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean add( String key, Object value, Date expiry ) throws IOException {
		try {
			return set( "add", key, value, expiry, null, primitiveAsString, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Adds data to the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean add( String key, Object value, Date expiry, Integer hashCode ) throws IOException {
		try {
			return set( "add", key, value, expiry, hashCode, primitiveAsString, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Updates data on the server; only the key and the value are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean replace( String key, Object value ) throws IOException {
		try {
			return set( "replace", key, value, null, null, primitiveAsString, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Updates data on the server; only the key and the value and an optional hash are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean replace( String key, Object value, Integer hashCode ) throws IOException {
		try {
			return set( "replace", key, value, null, hashCode, primitiveAsString, false );
		} catch (IQException e) {
			
		}
		
		return false;
	}

	/**
	 * Updates data on the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean replace( String key, Object value, Date expiry ) throws IOException {
		try {
			return set( "replace", key, value, expiry, null, primitiveAsString, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	/**
	 * Updates data on the server; the key, value, and an expiration time are specified.
	 *
	 * @param key key to store data under
	 * @param value value to store
	 * @param expiry when to expire the record
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true, if the data was successfully stored
	 * @throws IOException 
	 */
	public boolean replace( String key, Object value, Date expiry, Integer hashCode ) throws IOException {
		try {
			return set( "replace", key, value, expiry, hashCode, primitiveAsString, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public byte[] serializeObject(Object value, boolean asString) throws IOException {
		byte val[] = null;
		if ( NativeHandler.isHandled( value ) ) {			
			if ( asString ) {
				// useful for sharing data between java and non-java
				// and also for storing ints for the increment method
				try {
					val = value.toString().getBytes( defaultEncoding );
				}
				catch ( UnsupportedEncodingException ue ) {
					log.error( "invalid encoding type used: " + defaultEncoding, ue );
					return null;
				}
			}
			else {
				try {
					if ( log.isInfoEnabled() )
						log.info( "Storing with native handler..." );
//					flags |= NativeHandler.getMarkerFlag( value );
					val    = NativeHandler.encode( value );
				}
				catch ( Exception e ) {
					log.error( "Failed to native handle obj", e );
					return null;
				}
			}
		}
		else {
			// always serialize for non-primitive types
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				(new ObjectOutputStream( bos )).writeObject( value );
				val = bos.toByteArray();
//				flags |= F_SERIALIZED;
			}
			catch ( IOException e ) {
				// if we fail to serialize, then
				// we bail
				log.error( "failed to serialize obj", e );
				log.error( value.toString() );

				return null;
			}
		}
		
		// now try to compress if we want to
		// and if the length is over the threshold 
		if ( compressEnable && val.length > compressThreshold ) {

			try {
				if ( log.isInfoEnabled() ) {
					log.info( "++++ trying to compress data" );
					log.info( "++++ size prior to compression: " + val.length );
				}
				ByteArrayOutputStream bos = new ByteArrayOutputStream( val.length );
				GZIPOutputStream gos = new GZIPOutputStream( bos );
				gos.write( val, 0, val.length );
				gos.finish();
				gos.close();
				
				// store it and set compression flag
				val = bos.toByteArray();
//				flags |= F_COMPRESSED;

				if ( log.isInfoEnabled() )
					log.info( "++++ compression succeeded, size after: " + val.length );
			}
			catch ( IOException e ) {
				log.error( "IOException while compressing stream: " + e.getMessage() );
				log.error( "storing data uncompressed" );
			}
		}
		
		if(val.length > MAX_OBJECT_SIZE) {
			throw new IOException("Payload too large. Max is " + MAX_OBJECT_SIZE + " whereas the payload size is " + val.length);
		}
		
		return val;
	}
	
	private byte[] convertToByteArray(Object value, boolean asString, String key) {
		byte[] val = null;
		
		// Treat a null value as an empty object.
		if (value == null) {
			return new byte[0];
		}
		
		if ( NativeHandler.isHandled( value ) ) {			
			if ( asString ) {
				// useful for sharing data between java and non-java
				// and also for storing ints for the increment method
				try {
					if ( log.isInfoEnabled() )
						log.info( "++++ storing data as a string for key: " + key + " for class: " + value.getClass().getName() );
					val = value.toString().getBytes( defaultEncoding );
				}
				catch ( UnsupportedEncodingException ue ) {

					// if we have an errorHandler, use its hook
					if ( errorHandler != null )
						errorHandler.handleErrorOnSet( this, ue, key );

					log.error( "invalid encoding type used: " + defaultEncoding, ue );
					return null;
				}
			}
			else {
				try {
					if ( log.isInfoEnabled() )
						log.info( "Storing with native handler..." );
//					flags |= NativeHandler.getMarkerFlag( value );
					val    = NativeHandler.encode( value );
				}
				catch ( Exception e ) {

					// if we have an errorHandler, use its hook
					if ( errorHandler != null )
						errorHandler.handleErrorOnSet( this, e, key );

					log.error( "Failed to native handle obj", e );
					return null;
				}
			}
		}
		else {
			// always serialize for non-primitive types
			try {
				if ( log.isInfoEnabled() )
					log.info( "++++ serializing for key: " + key + " for class: " + value.getClass().getName() );
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				(new ObjectOutputStream( bos )).writeObject( value );
				val = bos.toByteArray();
//				flags |= F_SERIALIZED;
			}
			catch ( IOException e ) {

				// if we have an errorHandler, use its hook
				if ( errorHandler != null )
					errorHandler.handleErrorOnSet( this, e, key );

				// if we fail to serialize, then
				// we bail
				log.error( "failed to serialize obj", e );
				log.error( value.toString() );

				return null;
			}
		}
		
		return val;
	}

	/** 
	 * Stores data to cache.
	 *
	 * If data does not already exist for this key on the server, or if the key is being<br/>
	 * deleted, the specified value will not be stored.<br/>
	 * The server will automatically delete the value when the expiration time has been reached.<br/>
	 * <br/>
	 * If compression is enabled, and the data is longer than the compression threshold<br/>
	 * the data will be stored in compressed form.<br/>
	 * <br/>
	 * As of the current release, all objects stored will use java serialization.
	 * 
	 * @param cmdname action to take (set, add, replace)
	 * @param key key to store cache under
	 * @param value object to cache
	 * @param expiry expiration
	 * @param hashCode if not null, then the int hashcode to use
	 * @param asString store this object as a string?
	 * @return true/false indicating success
	 * @throws IQException 
	 */
	private boolean set( 
			String cmdname, 
			String key, 
			Object value, 
			Date expiry, 
			Integer hashCode, 
			boolean asString,
			boolean use_xlease_token) 
					throws IOException, IQException
	{

		if ( cmdname == null || cmdname.trim().equals( "" ) || key == null ) {
			log.error( "key is null or cmd is null/empty for set()" );
			return false;
		}
		

		try {
			key = sanitizeKey( key );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnSet( this, e, key );

			log.error( "failed to sanitize your key!", e );
			return false;
		}

		// A null value is acceptable because SaR may call this with a null value.
		// In that case, value_length will be set to 0 and no value is 
		// written to the server.
//		if ( value == null ) {
//			log.error( "trying to store a null value to cache" );
//			return false;
//		}

		// get SockIO obj
		SockIOPool.SockIO sock = pool.getSock( key, hashCode );
		
		if ( sock == null ) {
			if ( errorHandler != null )
				errorHandler.handleErrorOnSet( this, new IOException( "no socket to server available" ), key );
			return false;
		}
		
		if ( expiry == null )
			expiry = new Date(0);

		// store flags
		int flags = 0;
		
		// byte array to hold data
		byte[] val;

		val = convertToByteArray(value, asString, key);
		if (val == null) {
			// return socket to pool and bail
			sock.close();
			sock = null;
			return false;
		}
		
		// Set the flags
        if ( NativeHandler.isHandled( value ) ) {
			
			if ( asString ) { }
			else {
				flags |= NativeHandler.getMarkerFlag( value );
			}
		}
		else {
			// always serialize for non-primitive types
			flags |= F_SERIALIZED;
		}
		
		// now try to compress if we want to
		// and if the length is over the threshold 
		if ( compressEnable && val.length > compressThreshold ) {

			try {
				if ( log.isInfoEnabled() ) {
					log.info( "++++ trying to compress data" );
					log.info( "++++ size prior to compression: " + val.length );
				}
				ByteArrayOutputStream bos = new ByteArrayOutputStream( val.length );
				GZIPOutputStream gos = new GZIPOutputStream( bos );
				gos.write( val, 0, val.length );
				gos.finish();
				gos.close();
				
				// store it and set compression flag
				val = bos.toByteArray();
				flags |= F_COMPRESSED;

				if ( log.isInfoEnabled() )
					log.info( "++++ compression succeeded, size after: " + val.length );
			}
			catch ( IOException e ) {

				// if we have an errorHandler, use its hook
				if ( errorHandler != null )
					errorHandler.handleErrorOnSet( this, e, key );

				log.error( "IOException while compressing stream: " + e.getMessage() );
				log.error( "storing data uncompressed" );
			}
		}
		
		if(val.length > MAX_OBJECT_SIZE) {
			log.error( "++++ error storing data in cache for key: " + key + " -- length: " + val.length + " Value too large, max is " + MAX_OBJECT_SIZE );
			
			if ( sock != null ) {
				sock.close();
				sock = null;
			}

			throw new IOException("Payload too large. Max is " + MAX_OBJECT_SIZE + " whereas the payload size is " + val.length);
		}

		// now write the data to the cache server
		try {
			String cmd = null;

			if(cmdname.equals("iqset") || cmdname.equals("sar") || cmdname.equals("dar"))
			{				
				Long current_token = null;
				
				if(use_xlease_token) {
					current_token = this.q_lease_list.get(key);
				} else {
					current_token = this.i_lease_list.get(key);
				}
				
				if (current_token == null)
				{
					if (cmdname.equals("iqset"))
						throw new IQException("iqset request with no lease token");
					
					current_token = DEFAULT_TOKEN;
				}

				cmd = String.format( "%s %s %d %d %d %d\r\n", 
						cmdname, key, flags, (expiry.getTime() / 1000), val.length, 
						current_token );
			}
			else
			{
				cmd = String.format( "%s %s %d %d %d\r\n", cmdname, key, flags, (expiry.getTime() / 1000), val.length );
			}
			sock.write( cmd.getBytes() );
			
			if (val.length > 0) {
				sock.write( val );
			}
			sock.write( "\r\n".getBytes() );
			sock.flush();

			// get result code
			String line = sock.readLine();
			if ( log.isInfoEnabled() )
				log.info( "++++ memcache cmd (result code): " + cmd + " (" + line + ")" );

			// Treat this as a swap and release.
			if ( cmd.equals("sar") ) {
				// Remove entry from lease/hold list
				if(use_xlease_token) {
					this.q_lease_list.remove(key);
				} else {
//					this.lease_list.remove(key);
					log.error("ERROR- lease token for SaR should be a QLease");
				}
				
				if ( STORED.equals(line) ) {
					if ( log.isInfoEnabled() )
						log.info("++++ data successfully swapped for key: " + key );
				} else if ( NOTSTORED.equals(line) ) {
					if ( log.isInfoEnabled() )
						log.info( "++++ data not swapped in cache for key: " + key );
				} else if ( INVALID.equals(line) ) {
					if ( log.isInfoEnabled() )
						log.info( "++++ sar ignored for key: " + key );
				} else {
					log.error("++++ sar for key: "+ key +", returned: " + line);
				}				
				
				sock.close();
				sock = null;
				return true;
			}
			
			// Treat this as a regular set.
			if ( STORED.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info("++++ data successfully stored for key: " + key );
				sock.close();
				sock = null;
				
				// Remove entry from lease/hold list
				if (cmdname.equals("iqset") || cmdname.equals("sar") || cmdname.equals("dar")) {
					if(use_xlease_token) {
						this.q_lease_list.remove(key);
					} else {
						this.i_lease_list.remove(key);
					}
				}
				
				return true;
			}
			else if ( NOTSTORED.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ data not stored in cache for key: " + key );
				
				// Remove entry from lease/hold list
				if (cmdname.equals("iqset") || cmdname.equals("sar") || cmdname.equals("dar")) {
					if(use_xlease_token) {
						this.q_lease_list.remove(key);
					} else {
						this.i_lease_list.remove(key);
					}
				}			
			}
			else {
				log.error( "++++ error storing data in cache for key: " + key + " -- length: " + val.length );
				log.error( "++++ server response: " + line );
				
				if (cmdname.equals("iqset") || cmdname.equals("sar") || cmdname.equals("dar")) {
					if(use_xlease_token) {
						this.q_lease_list.remove(key);
					} else {
						this.i_lease_list.remove(key);
					}
				}				
			}
		}
		catch ( IOException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnSet( this, e, key );

			// exception thrown
			log.error( "++++ exception thrown while writing bytes to server on set" );
			log.error( e.getMessage(), e );

			try {
				sock.trueClose();
			}
			catch ( IOException ioe ) {
				log.error( "++++ failed to close socket : " + sock.toString() );
			}

			sock = null;
		}

		if ( sock != null ) {
			sock.close();
			sock = null;
		}

		return false;
	}

	/** 
	 * Store a counter to memcached given a key
	 * 
	 * @param key cache key
	 * @param counter number to store
	 * @return true/false indicating success
	 * @throws IOException 
	 */
	public boolean storeCounter( String key, long counter ) throws IOException {
		try {
			return set( "set", key, new Long( counter ), null, null, true, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	/** 
	 * Store a counter to memcached given a key
	 * 
	 * @param key cache key
	 * @param counter number to store
	 * @return true/false indicating success
	 * @throws IOException 
	 */
	public boolean storeCounter( String key, Long counter ) throws IOException {
		try {
			return set( "set", key, counter, null, null, true, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
    
	/** 
	 * Store a counter to memcached given a key
	 * 
	 * @param key cache key
	 * @param counter number to store
	 * @param hashCode if not null, then the int hashcode to use
	 * @return true/false indicating success
	 * @throws IOException 
	 */
	public boolean storeCounter( String key, Long counter, Integer hashCode ) throws IOException {
		try {
			return set( "set", key, counter, null, hashCode, true, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	/** 
	 * Returns value in counter at given key as long. 
	 *
	 * @param key cache ket
	 * @return counter value or -1 if not found
	 */
	public long getCounter( String key ) {
		return getCounter( key, null );
	}

	/** 
	 * Returns value in counter at given key as long. 
	 *
	 * @param key cache ket
	 * @param hashCode if not null, then the int hashcode to use
	 * @return counter value or -1 if not found
	 */
	public long getCounter( String key, Integer hashCode ) {

		if ( key == null ) {
			log.error( "null key for getCounter()" );
			return -1;
		}

		long counter = -1;
		try {
			counter = Long.parseLong( (String)get( key, hashCode, true, false ) );
		}
		catch ( Exception ex ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnGet( this, ex, key );

			// not found or error getting out
			if ( log.isInfoEnabled() )
				log.info( String.format( "Failed to parse Long value for key: %s", key ) );
		}
		
		return counter;
	}

	/** 
	 * Thread safe way to initialize and increment a counter. 
	 * 
	 * @param key key where the data is stored
	 * @return value of incrementer
	 * @throws IOException 
	 */
	public long addOrIncr( String key ) throws IOException {
		return addOrIncr( key, 0, null );
	}

	/** 
	 * Thread safe way to initialize and increment a counter. 
	 * 
	 * @param key key where the data is stored
	 * @param inc value to set or increment by
	 * @return value of incrementer
	 * @throws IOException 
	 */
	public long addOrIncr( String key, long inc ) throws IOException {
		return addOrIncr( key, inc, null );
	}

	/** 
	 * Thread safe way to initialize and increment a counter. 
	 * 
	 * @param key key where the data is stored
	 * @param inc value to set or increment by
	 * @param hashCode if not null, then the int hashcode to use
	 * @return value of incrementer
	 * @throws IOException 
	 */
	public long addOrIncr( String key, long inc, Integer hashCode ) throws IOException {
		boolean ret = false;
		try {
			ret = set( "add", key, new Long( inc ), null, hashCode, true, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if ( ret ) {
			return inc;
		}
		else {
			return incrdecr( "incr", key, inc, hashCode );
		}
	}

	/** 
	 * Thread safe way to initialize and decrement a counter. 
	 * 
	 * @param key key where the data is stored
	 * @return value of incrementer
	 * @throws IOException 
	 */
	public long addOrDecr( String key ) throws IOException {
		return addOrDecr( key, 0, null );
	}

	/** 
	 * Thread safe way to initialize and decrement a counter. 
	 * 
	 * @param key key where the data is stored
	 * @param inc value to set or increment by
	 * @return value of incrementer
	 * @throws IOException 
	 */
	public long addOrDecr( String key, long inc ) throws IOException {
		return addOrDecr( key, inc, null );
	}

	/** 
	 * Thread safe way to initialize and decrement a counter. 
	 * 
	 * @param key key where the data is stored
	 * @param inc value to set or increment by
	 * @param hashCode if not null, then the int hashcode to use
	 * @return value of incrementer
	 * @throws IOException 
	 */
	public long addOrDecr( String key, long inc, Integer hashCode ) throws IOException {
		boolean ret = false;
		try {
			ret = set( "add", key, new Long( inc ), null, hashCode, true, false );
		} catch (IQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if ( ret ) {
			return inc;
		}
		else {
			return incrdecr( "decr", key, inc, hashCode );
		}
	}

	/**
	 * Increment the value at the specified key by 1, and then return it.
	 *
	 * @param key key where the data is stored
	 * @return -1, if the key is not found, the value after incrementing otherwise
	 */
	public long incr( String key ) {
		return incrdecr( "incr", key, 1, null );
	}

	/** 
	 * Increment the value at the specified key by passed in val. 
	 * 
	 * @param key key where the data is stored
	 * @param inc how much to increment by
	 * @return -1, if the key is not found, the value after incrementing otherwise
	 */
	public long incr( String key, long inc ) {
		return incrdecr( "incr", key, inc, null );
	}

	/**
	 * Increment the value at the specified key by the specified increment, and then return it.
	 *
	 * @param key key where the data is stored
	 * @param inc how much to increment by
	 * @param hashCode if not null, then the int hashcode to use
	 * @return -1, if the key is not found, the value after incrementing otherwise
	 */
	public long incr( String key, long inc, Integer hashCode ) {
		return incrdecr( "incr", key, inc, hashCode );
	}
	
	/**
	 * Decrement the value at the specified key by 1, and then return it.
	 *
	 * @param key key where the data is stored
	 * @return -1, if the key is not found, the value after incrementing otherwise
	 */
	public long decr( String key ) {
		return incrdecr( "decr", key, 1, null );
	}

	/**
	 * Decrement the value at the specified key by passed in value, and then return it.
	 *
	 * @param key key where the data is stored
	 * @param inc how much to increment by
	 * @return -1, if the key is not found, the value after incrementing otherwise
	 */
	public long decr( String key, long inc ) {
		return incrdecr( "decr", key, inc, null );
	}

	/**
	 * Decrement the value at the specified key by the specified increment, and then return it.
	 *
	 * @param key key where the data is stored
	 * @param inc how much to increment by
	 * @param hashCode if not null, then the int hashcode to use
	 * @return -1, if the key is not found, the value after incrementing otherwise
	 */
	public long decr( String key, long inc, Integer hashCode ) {
		return incrdecr( "decr", key, inc, hashCode );
	}

	/** 
	 * Increments/decrements the value at the specified key by inc.
	 * 
	 *  Note that the server uses a 32-bit unsigned integer, and checks for<br/>
	 *  underflow. In the event of underflow, the result will be zero.  Because<br/>
	 *  Java lacks unsigned types, the value is returned as a 64-bit integer.<br/>
	 *  The server will only decrement a value if it already exists;<br/>
	 *  if a value is not found, -1 will be returned.
	 *
	 * @param cmdname increment/decrement
	 * @param key cache key
	 * @param inc amount to incr or decr
	 * @param hashCode if not null, then the int hashcode to use
	 * @return new value or -1 if not exist
	 */
	private long incrdecr( String cmdname, String key, long inc, Integer hashCode ) {

		if ( key == null ) {
			log.error( "null key for incrdecr()" );
			return -1;
		}

		try {
			key = sanitizeKey( key );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnGet( this, e, key );

			log.error( "failed to sanitize your key!", e );
			return -1;
		}

		// get SockIO obj for given cache key
		SockIOPool.SockIO sock = pool.getSock( key, hashCode );

		if ( sock == null ) {
			if ( errorHandler != null )
				errorHandler.handleErrorOnSet( this, new IOException( "no socket to server available" ), key );
			return -1;
		}
		
		try {
			String cmd = String.format( "%s %s %d\r\n", cmdname, key, inc );
			if ( log.isDebugEnabled() )
				log.debug( "++++ memcache incr/decr command: " + cmd );

			sock.write( cmd.getBytes() );
			sock.flush();

			// get result back
			String line = sock.readLine();

			if ( line.matches( "\\d+" ) ) {

				// return sock to pool and return result
				sock.close();
				try {
					return Long.parseLong( line );
				}
				catch ( Exception ex ) {

					// if we have an errorHandler, use its hook
					if ( errorHandler != null )
						errorHandler.handleErrorOnGet( this, ex, key );

					log.error( String.format( "Failed to parse Long value for key: %s", key ) );
				}
 			}
			else if ( NOTFOUND.equals( line ) ) {
				if ( log.isInfoEnabled() )
					log.info( "++++ key not found to incr/decr for key: " + key );
			}
			else {
				log.error( "++++ error incr/decr key: " + key );
				log.error( "++++ server response: " + line );
			}
		}
		catch ( IOException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnGet( this, e, key );

			// exception thrown
			log.error( "++++ exception thrown while writing bytes to server on incr/decr" );
			log.error( e.getMessage(), e );

			try {
				sock.trueClose();
			}
			catch ( IOException ioe ) {
				log.error( "++++ failed to close socket : " + sock.toString() );
			}

			sock = null;
		}
		
		if ( sock != null ) {
			sock.close();
			sock = null;
		}

		return -1;
	}
	
	private static int incrementCounter(AtomicInteger counter) {
        int v;
        do {
            v = counter.get();
        } while (!counter.compareAndSet(v, v + 1));
        return v + 1;
    }
	
	public static int getNumBackoff() {
		return NumBackoff.get();
	}
	
	public Object get( String key ) {
		return get( key, null, false, false );
	}

	/**
	 * Retrieve a key from the server, using a specific hash.
	 *
	 *  If the data was compressed or serialized when compressed, it will automatically<br/>
	 *  be decompressed or serialized, as appropriate. (Inclusive or)<br/>
	 *<br/>
	 *  Non-serialized data will be returned as a string, so explicit conversion to<br/>
	 *  numeric types will be necessary, if desired<br/>
	 *
	 * @param key key where data is stored
	 * @return the object that was previously stored, or null if it was not previously stored
	 */
	public Object iqget( String key ) {
		return get( key, null, false, true );
	}
	
	
	public boolean hasLease(String key) {
		Long lease_token = i_lease_list.get(key);
		
		return (lease_token != null);
	}

	/** 
	 * Retrieve a key from the server, using a specific hash.
	 *
	 *  If the data was compressed or serialized when compressed, it will automatically<br/>
	 *  be decompressed or serialized, as appropriate. (Inclusive or)<br/>
	 *<br/>
	 *  Non-serialized data will be returned as a string, so explicit conversion to<br/>
	 *  numeric types will be necessary, if desired<br/>
	 *
	 * @param key key where data is stored
	 * @param hashCode if not null, then the int hashcode to use
	 * @return the object that was previously stored, or null if it was not previously stored
	 */
	public Object iqget( String key, Integer hashCode ) {
		return get( key, hashCode, false, true );
	}

	/**
	 * Retrieve a key from the server, using a specific hash.
	 *
	 *  If the data was compressed or serialized when compressed, it will automatically<br/>
	 *  be decompressed or serialized, as appropriate. (Inclusive or)<br/>
	 *<br/>
	 *  Non-serialized data will be returned as a string, so explicit conversion to<br/>
	 *  numeric types will be necessary, if desired<br/>
	 *
	 * @param key key where data is stored
	 * @param hashCode if not null, then the int hashcode to use
	 * @param asString if true, then return string val
	 * @return the object that was previously stored, or null if it was not previously stored
	 */
	public Object get( String key, Integer hashCode, boolean asString, boolean lease_requested ) {

		if ( key == null ) {
			log.error( "key is null for get()" );
			return null;
		}

		try {
			key = sanitizeKey( key );
		}
		catch ( UnsupportedEncodingException e ) {

			// if we have an errorHandler, use its hook
			if ( errorHandler != null )
				errorHandler.handleErrorOnGet( this, e, key );

			log.error( "failed to sanitize your key!", e );
			return null;
		}

		// get SockIO obj using cache key
		SockIOPool.SockIO socket = null;
		String cmd;
		
		Long lease_token = new Long(0L);
		if (lease_requested) {
			cmd = "iqget " + key;
			
			if (this.q_lease_list.containsKey(key)) {
				lease_token = this.q_lease_list.get(key);
			} else if (this.i_lease_list.containsKey(key)) {
				lease_token = this.i_lease_list.get(key);
			}
			
			if (lease_token.longValue() != 0L) {
				cmd += " " + lease_token.longValue() + "\r\n";
			} else {
				cmd += "\r\n";
			}
		} else {
			cmd = "get ";
			cmd += key + "\r\n";
		}
		
		boolean value_found = false;
		// ready object
		Object o = null;
		
		// if the operation is an iqget() and this key has lease granted (q lease or i-lease)
		// on the current whalin client, it just try to get the data from the cache
		// if the data is not available, it will not trying back-off!
		if (lease_token.longValue() != 0L) {
			socket = pool.getSock( key, hashCode );
			
			if ( socket == null ) {
				if ( errorHandler != null )
					errorHandler.handleErrorOnGet( this, new IOException( "no socket to server available" ), key );
				return null;
			}
			
			try {
				if ( log.isDebugEnabled() )
					log.debug("++++ memcache iqget command: " + cmd);


				socket.write( cmd.getBytes() );
				socket.flush();

				while ( true ) {
					String line = socket.readLine();

					if ( log.isDebugEnabled() )
						log.debug( "++++ line: " + line );

					if ( line.startsWith( VALUE ) ) {
						String[] info = line.split(" ");
						int flag      = Integer.parseInt( info[2] );
						int length    = Integer.parseInt( info[3] );

						if ( log.isDebugEnabled() ) {
							log.debug( "++++ key: " + key );
							log.debug( "++++ flags: " + flag );
							log.debug( "++++ length: " + length );
						}

						// read obj into buffer
						byte[] buf = new byte[length];
						socket.read( buf );
						socket.clearEOL();

						value_found = true;

						if ( (flag & F_COMPRESSED) == F_COMPRESSED ) {
							try {
								// read the input stream, and write to a byte array output stream since
								// we have to read into a byte array, but we don't know how large it
								// will need to be, and we don't want to resize it a bunch
								GZIPInputStream gzi = new GZIPInputStream( new ByteArrayInputStream( buf ) );
								ByteArrayOutputStream bos = new ByteArrayOutputStream( buf.length );

								int count;
								byte[] tmp = new byte[2048];
								while ( (count = gzi.read(tmp)) != -1 ) {
									bos.write( tmp, 0, count );
								}

								// store uncompressed back to buffer
								buf = bos.toByteArray();
								gzi.close();
							}
							catch ( IOException e ) {

								// if we have an errorHandler, use its hook
								if ( errorHandler != null )
									errorHandler.handleErrorOnGet( this, e, key );

								log.error( "++++ IOException thrown while trying to uncompress input stream for key: " + key + " -- " + e.getMessage() );
								throw new NestedIOException( "++++ IOException thrown while trying to uncompress input stream for key: " + key, e );
							}
						}

						// we can only take out serialized objects
						if ( ( flag & F_SERIALIZED ) != F_SERIALIZED ) {
							if ( primitiveAsString || asString ) {
								// pulling out string value
								if ( log.isInfoEnabled() )
									log.info( "++++ retrieving object and stuffing into a string." );
								o = new String( buf, defaultEncoding );
							}
							else {
								// decoding object
								try {
									o = NativeHandler.decode( buf, flag );    
								}
								catch ( Exception e ) {

									// if we have an errorHandler, use its hook
									if ( errorHandler != null )
										errorHandler.handleErrorOnGet( this, e, key );

									log.error( "++++ Exception thrown while trying to deserialize for key: " + key, e );
									throw new NestedIOException( e );
								}
							}
						}
						else {
							// deserialize if the data is serialized
							ContextObjectInputStream ois =
									new ContextObjectInputStream( new ByteArrayInputStream( buf ), classLoader );
							try {
								o = ois.readObject();
								if ( log.isInfoEnabled() )
									log.info( "++++ deserializing " + o.getClass() );
							}
							catch ( Exception e ) {
								if ( errorHandler != null )
									errorHandler.handleErrorOnGet( this, e, key );

								o = null;
								log.error( "++++ Exception thrown while trying to deserialize for key: " + key + " -- " + e.getMessage() );
							}
						}
					} else if ( line.startsWith( LEASEVALUE ) ) {
						String[] info 	= line.split(" ");
						String currkey = info[1];
						long token_value = Long.parseLong( info[3] );

						if(token_value == TOKEN_HOTMISS) {
							value_found = false;
						} else {							
							this.i_lease_list.put(currkey, token_value);
							value_found = true;
						}
					} else if (line.startsWith(NOVALUE)) {
						value_found = false;
					} else if ( END.equals( line ) ) {
						if ( log.isDebugEnabled() )
							log.debug( "++++ finished reading from cache server" );
						break;
					}

				}

				socket.close();
				socket = null;

			} catch ( IOException e ) {

				// if we have an errorHandler, use its hook
				if ( errorHandler != null )
					errorHandler.handleErrorOnGet( this, e, key );

				// exception thrown
				log.error( "++++ exception thrown while trying to get object from cache for key: " + key + " -- " + e.getMessage() );

				try {
					socket.trueClose();
				}
				catch ( IOException ioe ) {
					log.error( "++++ failed to close socket : " + socket.toString() );
				}
				socket = null;
			}			
		} else {
			Random rand = new Random();
			int backoff = INITIAL_BACKOFF_VALUE;
			
			// Keep trying to get until either the value is found or a valid lease_token is returned.
			while(!value_found) {
				socket = pool.getSock( key, hashCode );
	
				if ( socket == null ) {
					if ( errorHandler != null )
						errorHandler.handleErrorOnGet( this, new IOException( "no socket to server available" ), key );
					return null;
				}
	
				if(this.i_lease_list == null) 
				{
					// Create new gumball hashmap if one didn't exist before.
					this.i_lease_list = new HashMap<String, Long>();
				}
	
				try {
					if ( log.isDebugEnabled() )
						log.debug("++++ memcache iqget command: " + cmd);
	
	
					socket.write( cmd.getBytes() );
					socket.flush();
	
					while ( true ) {
						String line = socket.readLine();
	
						if ( log.isDebugEnabled() )
							log.debug( "++++ line: " + line );
	
						if ( line.startsWith( VALUE ) ) {
							String[] info = line.split(" ");
							int flag      = Integer.parseInt( info[2] );
							int length    = Integer.parseInt( info[3] );
	
							if ( log.isDebugEnabled() ) {
								log.debug( "++++ key: " + key );
								log.debug( "++++ flags: " + flag );
								log.debug( "++++ length: " + length );
							}
	
							// read obj into buffer
							byte[] buf = new byte[length];
							socket.read( buf );
							socket.clearEOL();
	
							value_found = true;
	
							if ( (flag & F_COMPRESSED) == F_COMPRESSED ) {
								try {
									// read the input stream, and write to a byte array output stream since
									// we have to read into a byte array, but we don't know how large it
									// will need to be, and we don't want to resize it a bunch
									GZIPInputStream gzi = new GZIPInputStream( new ByteArrayInputStream( buf ) );
									ByteArrayOutputStream bos = new ByteArrayOutputStream( buf.length );
	
									int count;
									byte[] tmp = new byte[2048];
									while ( (count = gzi.read(tmp)) != -1 ) {
										bos.write( tmp, 0, count );
									}
	
									// store uncompressed back to buffer
									buf = bos.toByteArray();
									gzi.close();
								}
								catch ( IOException e ) {
	
									// if we have an errorHandler, use its hook
									if ( errorHandler != null )
										errorHandler.handleErrorOnGet( this, e, key );
	
									log.error( "++++ IOException thrown while trying to uncompress input stream for key: " + key + " -- " + e.getMessage() );
									throw new NestedIOException( "++++ IOException thrown while trying to uncompress input stream for key: " + key, e );
								}
							}
	
							// we can only take out serialized objects
							if ( ( flag & F_SERIALIZED ) != F_SERIALIZED ) {
								if ( primitiveAsString || asString ) {
									// pulling out string value
									if ( log.isInfoEnabled() )
										log.info( "++++ retrieving object and stuffing into a string." );
									o = new String( buf, defaultEncoding );
								}
								else {
									// decoding object
									try {
										o = NativeHandler.decode( buf, flag );    
									}
									catch ( Exception e ) {
	
										// if we have an errorHandler, use its hook
										if ( errorHandler != null )
											errorHandler.handleErrorOnGet( this, e, key );
	
										log.error( "++++ Exception thrown while trying to deserialize for key: " + key, e );
										throw new NestedIOException( e );
									}
								}
							}
							else {
								// deserialize if the data is serialized
								ContextObjectInputStream ois =
										new ContextObjectInputStream( new ByteArrayInputStream( buf ), classLoader );
								try {
									o = ois.readObject();
									if ( log.isInfoEnabled() )
										log.info( "++++ deserializing " + o.getClass() );
								}
								catch ( Exception e ) {
									if ( errorHandler != null )
										errorHandler.handleErrorOnGet( this, e, key );
	
									o = null;
									log.error( "++++ Exception thrown while trying to deserialize for key: " + key + " -- " + e.getMessage() );
								}
							}
						} else if ( line.startsWith( LEASEVALUE ) ) {
							String[] info 	= line.split(" ");
							String currkey = info[1];
							long token_value = Long.parseLong( info[3] );
	
							if(token_value == TOKEN_HOTMISS) {
								value_found = false;
								incrementCounter(NumBackoff);
							} else {
								this.i_lease_list.put(currkey, token_value);
								value_found = true;
							}
						}
						else if ( END.equals( line ) ) {
							if ( log.isDebugEnabled() )
								log.debug( "++++ finished reading from cache server" );
							break;
						}
	
					}
	
					socket.close();
					socket = null;
	
				} catch ( IOException e ) {
	
					// if we have an errorHandler, use its hook
					if ( errorHandler != null )
						errorHandler.handleErrorOnGet( this, e, key );
	
					// exception thrown
					log.error( "++++ exception thrown while trying to get object from cache for key: " + key + " -- " + e.getMessage() );
	
					try {
						if (socket != null)
							socket.trueClose();
					}
					catch ( IOException ioe ) {
						log.error( "++++ failed to close socket : " + socket.toString() );
					}
					socket = null;
				}
				
				if(backoff == 0) {
					break;
				}
	
				// Only backoff if a lease needs to be acquired. If no lease is required, this
				// function can just return with no value found.
				if (!lease_requested) {
					break;
				}
				
				if(!value_found) {
					// Sleep for random backoff
					try {
						Thread.sleep(rand.nextInt(backoff));
						backoff = 2 * backoff;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		if(value_found) {
			return o;
		}

		return null;
	}
		
	/***
	 * Retrieve gumball from a byte buffer. Assuming 16 byte buffer 
	 * @param buf
	 * @param key
	 * @throws IOException
	 */
//	private void parseGumball( byte[] buf, String key) throws IOException
//	{
//		if( buf.length != 16 )
//		{
//			log.error("Invalid gumball size");
//			return;
//		}
//		
//		DataInputStream bytestream = new DataInputStream(new ByteArrayInputStream(buf));
//		GumballValue gumball = new GumballValue();
//		gumball.seconds = bytestream.readLong();
//		gumball.nanoseconds = bytestream.readLong();
//		
//		bytestream.close();
//		
//		this.lease_list.put(key, gumball);
//		
//		if( log.isDebugEnabled() )
//			log.debug( "++++ gumball value is " + gumball.seconds + ", " + gumball.nanoseconds ); 
//	}

	/** 
	 * Retrieve multiple objects from the memcache.
	 *
	 *  This is recommended over repeated calls to {@link #iqget(String) get()}, since it<br/>
	 *  is more efficient.<br/>
	 *
	 * @param keys String array of keys to retrieve
	 * @return Object array ordered in same order as key array containing results
	 */
	public Object[] getMultiArray( String[] keys ) {
		return getMultiArray( keys, null, false );
	}

	/** 
	 * Retrieve multiple objects from the memcache.
	 *
	 *  This is recommended over repeated calls to {@link #iqget(String) get()}, since it<br/>
	 *  is more efficient.<br/>
	 *
	 * @param keys String array of keys to retrieve
	 * @param hashCodes if not null, then the Integer array of hashCodes
	 * @return Object array ordered in same order as key array containing results
	 */
	public Object[] getMultiArray( String[] keys, Integer[] hashCodes ) {
		return getMultiArray( keys, hashCodes, false );
	}

	/** 
	 * Retrieve multiple objects from the memcache.
	 *
	 *  This is recommended over repeated calls to {@link #iqget(String) get()}, since it<br/>
	 *  is more efficient.<br/>
	 *
	 * @param keys String array of keys to retrieve
	 * @param hashCodes if not null, then the Integer array of hashCodes
	 * @param asString if true, retrieve string vals
	 * @return Object array ordered in same order as key array containing results
	 */
	public Object[] getMultiArray( String[] keys, Integer[] hashCodes, boolean asString ) {

		Map<String,Object> data = getMulti( keys, hashCodes, asString );

		if ( data == null )
			return null;

		Object[] res = new Object[ keys.length ];
		for ( int i = 0; i < keys.length; i++ ) {
			res[i] = data.get( keys[i] );
		}

		return res;
	}

	/**
	 * Retrieve multiple objects from the memcache.
	 *
	 *  This is recommended over repeated calls to {@link #iqget(String) get()}, since it<br/>
	 *  is more efficient.<br/>
	 *
	 * @param keys String array of keys to retrieve
	 * @return a hashmap with entries for each key is found by the server,
	 *      keys that are not found are not entered into the hashmap, but attempting to
	 *      retrieve them from the hashmap gives you null.
	 */
	public Map<String,Object> getMulti( String[] keys ) {
		return getMulti( keys, null, false );
	}
    
	/**
	 * Retrieve multiple keys from the memcache.
	 *
	 *  This is recommended over repeated calls to {@link #iqget(String) get()}, since it<br/>
	 *  is more efficient.<br/>
	 *
	 * @param keys keys to retrieve
	 * @param hashCodes if not null, then the Integer array of hashCodes
	 * @return a hashmap with entries for each key is found by the server,
	 *      keys that are not found are not entered into the hashmap, but attempting to
	 *      retrieve them from the hashmap gives you null.
	 */
	public Map<String,Object> getMulti( String[] keys, Integer[] hashCodes ) {
		return getMulti( keys, hashCodes, false );
	}

	/**
	 * Retrieve multiple keys from the memcache.
	 *
	 *  This is recommended over repeated calls to {@link #iqget(String) get()}, since it<br/>
	 *  is more efficient.<br/>
	 *
	 * @param keys keys to retrieve
	 * @param hashCodes if not null, then the Integer array of hashCodes
	 * @param asString if true then retrieve using String val
	 * @return a hashmap with entries for each key is found by the server,
	 *      keys that are not found are not entered into the hashmap, but attempting to
	 *      retrieve them from the hashmap gives you null.
	 */
	public Map<String,Object> getMulti( String[] keys, Integer[] hashCodes, boolean asString ) {

		if ( keys == null || keys.length == 0 ) {
			log.error( "missing keys for getMulti()" );
			return null;
		}

		Map<String,StringBuilder> cmdMap =
			new HashMap<String,StringBuilder>();

		for ( int i = 0; i < keys.length; ++i ) {

			String key = keys[i];
			if ( key == null ) {
				log.error( "null key, so skipping" );
				continue;
			}

			Integer hash = null;
			if ( hashCodes != null && hashCodes.length > i )
				hash = hashCodes[ i ];

			String cleanKey = key;
			try {
				cleanKey = sanitizeKey( key );
			}
			catch ( UnsupportedEncodingException e ) {

				// if we have an errorHandler, use its hook
				if ( errorHandler != null )
					errorHandler.handleErrorOnGet( this, e, key );

				log.error( "failed to sanitize your key!", e );
				continue;
			}

			// get SockIO obj from cache key
			SockIOPool.SockIO sock = pool.getSock( cleanKey, hash );

			if ( sock == null ) {
				if ( errorHandler != null )
					errorHandler.handleErrorOnGet( this, new IOException( "no socket to server available" ), key );
				continue;
			}

			// store in map and list if not already
			if ( !cmdMap.containsKey( sock.getHost() ) )
				cmdMap.put( sock.getHost(), new StringBuilder( "get" ) );

			cmdMap.get( sock.getHost() ).append( " " + cleanKey );

			// return to pool
			sock.close();
		}
		
		if ( log.isInfoEnabled() )
			log.info( "multi get socket count : " + cmdMap.size() );

		// now query memcache
		Map<String,Object> ret =
			new HashMap<String,Object>( keys.length );

		// now use new NIO implementation
		(new NIOLoader( this )).doMulti( asString, cmdMap, keys, ret );

		// fix the return array in case we had to rewrite any of the keys
		for ( String key : keys ) {

			String cleanKey = key;
			try {
				cleanKey = sanitizeKey( key );
			}
			catch ( UnsupportedEncodingException e ) {

				// if we have an errorHandler, use its hook
				if ( errorHandler != null )
					errorHandler.handleErrorOnGet( this, e, key );

				log.error( "failed to sanitize your key!", e );
				continue;
			}

			if ( ! key.equals( cleanKey ) && ret.containsKey( cleanKey ) ) {
				ret.put( key, ret.get( cleanKey ) );
				ret.remove( cleanKey );
			}

			// backfill missing keys w/ null value
			if ( ! ret.containsKey( key ) )
				ret.put( key, null );
		}

		if ( log.isDebugEnabled() )
			log.debug( "++++ memcache: got back " + ret.size() + " results" );
		return ret;
	}

	/** 
	 * This method loads the data from cache into a Map.
	 *
	 * Pass a SockIO object which is ready to receive data and a HashMap<br/>
	 * to store the results.
	 * 
	 * @param sock socket waiting to pass back data
	 * @param hm hashmap to store data into
	 * @param asString if true, and if we are using NativehHandler, return string val
	 * @throws IOException if io exception happens while reading from socket
	 */
	private void loadMulti( LineInputStream input, Map<String,Object> hm, boolean asString ) throws IOException {

		while ( true ) {
			String line = input.readLine();
			if ( log.isDebugEnabled() )
				log.debug( "++++ line: " + line );

			if ( line.startsWith( VALUE ) ) {
				String[] info = line.split(" ");
				String key    = info[1];
				int flag      = Integer.parseInt( info[2] );
				int length    = Integer.parseInt( info[3] );

				if ( log.isDebugEnabled() ) {
					log.debug( "++++ key: " + key );
					log.debug( "++++ flags: " + flag );
					log.debug( "++++ length: " + length );
				}
				
				// read obj into buffer
				byte[] buf = new byte[length];
				input.read( buf );
				input.clearEOL();

				// ready object
				Object o;
				
				// check for compression
				if ( (flag & F_COMPRESSED) == F_COMPRESSED ) {
					try {
						// read the input stream, and write to a byte array output stream since
						// we have to read into a byte array, but we don't know how large it
						// will need to be, and we don't want to resize it a bunch
						GZIPInputStream gzi = new GZIPInputStream( new ByteArrayInputStream( buf ) );
						ByteArrayOutputStream bos = new ByteArrayOutputStream( buf.length );
						
						int count;
						byte[] tmp = new byte[2048];
						while ( (count = gzi.read(tmp)) != -1 ) {
							bos.write( tmp, 0, count );
						}

						// store uncompressed back to buffer
						buf = bos.toByteArray();
						gzi.close();
					}
					catch ( IOException e ) {

						// if we have an errorHandler, use its hook
						if ( errorHandler != null )
							errorHandler.handleErrorOnGet( this, e, key );

						log.error( "++++ IOException thrown while trying to uncompress input stream for key: " + key + " -- " + e.getMessage() );
						throw new NestedIOException( "++++ IOException thrown while trying to uncompress input stream for key: " + key, e );
					}
				}

				// we can only take out serialized objects
				if ( ( flag & F_SERIALIZED ) != F_SERIALIZED ) {
					if ( primitiveAsString || asString ) {
						// pulling out string value
						if ( log.isInfoEnabled() )
							log.info( "++++ retrieving object and stuffing into a string." );
						o = new String( buf, defaultEncoding );
					}
					else {
						// decoding object
						try {
							o = NativeHandler.decode( buf, flag );    
						}
						catch ( Exception e ) {

							// if we have an errorHandler, use its hook
							if ( errorHandler != null )
								errorHandler.handleErrorOnGet( this, e, key );

							log.error( "++++ Exception thrown while trying to deserialize for key: " + key + " -- " + e.getMessage() );
							throw new NestedIOException( e );
						}
					}
				}
				else {
					// deserialize if the data is serialized
					ContextObjectInputStream ois =
						new ContextObjectInputStream( new ByteArrayInputStream( buf ), classLoader );
					try {
						o = ois.readObject();
						if ( log.isInfoEnabled() )
							log.info( "++++ deserializing " + o.getClass() );
					}
					catch ( InvalidClassException e ) {
						/* Errors de-serializing are to be expected in the case of a 
						 * long running server that spans client restarts with updated 
						 * classes. 
						 */
						// if we have an errorHandler, use its hook
						if ( errorHandler != null )
							errorHandler.handleErrorOnGet( this, e, key );

						o = null;
						log.error( "++++ InvalidClassException thrown while trying to deserialize for key: " + key + " -- " + e.getMessage() );
					}
					catch ( ClassNotFoundException e ) {

						// if we have an errorHandler, use its hook
						if ( errorHandler != null )
							errorHandler.handleErrorOnGet( this, e, key );

						o = null;
						log.error( "++++ ClassNotFoundException thrown while trying to deserialize for key: " + key + " -- " + e.getMessage() );
					}
				}

				// store the object into the cache
				if ( o != null )
					hm.put( key, o );
			}
			else if ( END.equals( line ) ) {
				if ( log.isDebugEnabled() )
					log.debug( "++++ finished reading from cache server" );
				break;
			}
		}
	}

	private String sanitizeKey( String key ) throws UnsupportedEncodingException {
		return ( sanitizeKeys ) ? URLEncoder.encode( key, "UTF-8" ) : key;
	}

	/** 
	 * Invalidates the entire cache.
	 *
	 * Will return true only if succeeds in clearing all servers.
	 * 
	 * @return success true/false
	 */
	public boolean flushAll() {
		return flushAll( null );
	}

	/** 
	 * Invalidates the entire cache.
	 *
	 * Will return true only if succeeds in clearing all servers.
	 * If pass in null, then will try to flush all servers.
	 * 
	 * @param servers optional array of host(s) to flush (host:port)
	 * @return success true/false
	 */
	public boolean flushAll( String[] servers ) {

		// get SockIOPool instance
		// return false if unable to get SockIO obj
		if ( pool == null ) {
			log.error( "++++ unable to get SockIOPool instance" );
			return false;
		}

		// get all servers and iterate over them
		servers = ( servers == null )
			? pool.getServers()
			: servers;

		// if no servers, then return early
		if ( servers == null || servers.length <= 0 ) {
			log.error( "++++ no servers to flush" );
			return false;
		}

		boolean success = true;

		for ( int i = 0; i < servers.length; i++ ) {

			SockIOPool.SockIO sock = pool.getConnection( servers[i] );
			if ( sock == null ) {
				log.error( "++++ unable to get connection to : " + servers[i] );
				success = false;
				if ( errorHandler != null )
					errorHandler.handleErrorOnFlush( this, new IOException( "no socket to server available" ) );
				continue;
			}

			// build command
			String command = "flush_all\r\n";

			try {
				sock.write( command.getBytes() );
				sock.flush();

				// if we get appropriate response back, then we return true
				String line = sock.readLine();
				success = ( OK.equals( line ) )
					? success && true
					: false;
			}
			catch ( IOException e ) {

				// if we have an errorHandler, use its hook
				if ( errorHandler != null )
					errorHandler.handleErrorOnFlush( this, e );

				// exception thrown
				log.error( "++++ exception thrown while writing bytes to server on flushAll" );
				log.error( e.getMessage(), e );

				try {
					sock.trueClose();
				}
				catch ( IOException ioe ) {
					log.error( "++++ failed to close socket : " + sock.toString() );
				}

				success = false;
				sock = null;
			}

			if ( sock != null ) {
				sock.close();
				sock = null;
			}
		}

		return success;
	}

	/** 
	 * Retrieves stats for all servers.
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains stats
	 * with stat name as key and value as value.
	 * 
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map stats() {
		return stats( null );
	}

	/** 
	 * Retrieves stats for passed in servers (or all servers).
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains stats
	 * with stat name as key and value as value.
	 * 
	 * @param servers string array of servers to retrieve stats from, or all if this is null	 
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map stats( String[] servers ) {
		return stats( servers, "stats\r\n", STATS );
	}	

	/** 
	 * Retrieves stats items for all servers.
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains item stats
	 * with itemname:number:field as key and value as value.
	 * 
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map statsItems() {
		return statsItems( null );
	}
	
	/** 
	 * Retrieves stats for passed in servers (or all servers).
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains item stats
	 * with itemname:number:field as key and value as value.
	 * 
	 * @param servers string array of servers to retrieve stats from, or all if this is null
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map statsItems( String[] servers ) {
		return stats( servers, "stats items\r\n", STATS );
	}
	
	/** 
	 * Retrieves stats items for all servers.
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains slabs stats
	 * with slabnumber:field as key and value as value.
	 * 
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map statsSlabs() {
		return statsSlabs( null );
	}
	
	/** 
	 * Retrieves stats for passed in servers (or all servers).
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains slabs stats
	 * with slabnumber:field as key and value as value.
	 * 
	 * @param servers string array of servers to retrieve stats from, or all if this is null
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map statsSlabs( String[] servers ) {
		return stats( servers, "stats slabs\r\n", STATS );
	}
	
	/** 
	 * Retrieves items cachedump for all servers.
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains cachedump stats
	 * with the cachekey as key and byte size and unix timestamp as value.
	 * 
	 * @param slabNumber the item number of the cache dump
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map statsCacheDump( int slabNumber, int limit ) {
		return statsCacheDump( null, slabNumber, limit );
	}
	
	/** 
	 * Retrieves stats for passed in servers (or all servers).
	 *
	 * Returns a map keyed on the servername.
	 * The value is another map which contains cachedump stats
	 * with the cachekey as key and byte size and unix timestamp as value.
	 * 
	 * @param servers string array of servers to retrieve stats from, or all if this is null
	 * @param slabNumber the item number of the cache dump
	 * @return Stats map
	 */
	@SuppressWarnings("rawtypes")
	public Map statsCacheDump( String[] servers, int slabNumber, int limit ) {
		return stats( servers, String.format( "stats cachedump %d %d\r\n", slabNumber, limit ), ITEM );
	}
		
	@SuppressWarnings("rawtypes")
	private Map stats( String[] servers, String command, String lineStart ) {

		if ( command == null || command.trim().equals( "" ) ) {
			log.error( "++++ invalid / missing command for stats()" );
			return null;
		}

		// get all servers and iterate over them
		servers = (servers == null)
			? pool.getServers()
			: servers;

		// if no servers, then return early
		if ( servers == null || servers.length <= 0 ) {
			log.error( "++++ no servers to check stats" );
			return null;
		}

		// array of stats Maps
		Map<String,Map> statsMaps =
			new HashMap<String,Map>();

		for ( int i = 0; i < servers.length; i++ ) {

			SockIOPool.SockIO sock = pool.getConnection( servers[i] );
			if ( sock == null ) {
				log.error( "++++ unable to get connection to : " + servers[i] );
				if ( errorHandler != null )
					errorHandler.handleErrorOnStats( this, new IOException( "no socket to server available" ) );
				continue;
			}

			// build command
			try {
				sock.write( command.getBytes() );
				sock.flush();

				// map to hold key value pairs
				Map<String,String> stats = new HashMap<String,String>();

				// loop over results
				while ( true ) {
					String line = sock.readLine();
					if ( log.isDebugEnabled() )
						log.debug( "++++ line: " + line );

					if ( line.startsWith( lineStart ) ) {
						String[] info = line.split( " ", 3 );						
						String key    = info[1];
						String value  = info[2];

						if ( log.isDebugEnabled() ) {
							log.debug( "++++ key  : " + key );
							log.debug( "++++ value: " + value );
						}

						stats.put( key, value );
					}
					else if ( END.equals( line ) ) {
						// finish when we get end from server
						if ( log.isDebugEnabled() )
							log.debug( "++++ finished reading from cache server" );
						break;
					}
					else if ( line.startsWith( ERROR ) || line.startsWith( CLIENT_ERROR ) || line.startsWith( SERVER_ERROR ) ) {
						log.error( "++++ failed to query stats" );
						log.error( "++++ server response: " + line );
						break;
					}

					statsMaps.put( servers[i], stats );
				}
			}
			catch ( IOException e ) {

				// if we have an errorHandler, use its hook
				if ( errorHandler != null )
					errorHandler.handleErrorOnStats( this, e );

				// exception thrown
				log.error( "++++ exception thrown while writing bytes to server on stats" );
				log.error( e.getMessage(), e );

				try {
					sock.trueClose();
				}
				catch ( IOException ioe ) {
					log.error( "++++ failed to close socket : " + sock.toString() );
				}

				sock = null;
			}

			if ( sock != null ) {
				sock.close();
				sock = null;
			}
		}

		return statsMaps;
	}
	
//	/** a simple hash map function */
//	private String md5Java(byte[] hash){
//		String digest = null;
//		try {
//			MessageDigest md = MessageDigest.getInstance("MD5");
//			hash = md.digest(hash);
//
//			//converting byte array to Hexadecimal String
//			StringBuilder sb = new StringBuilder(2*hash.length);
//			for(byte b : hash){
//				sb.append(String.format("%02x", b&0xff));
//			}
//
//			digest = sb.toString();
//
//		} catch (NoSuchAlgorithmException ex) {
//			System.out.println(ex.getStackTrace());
//		}
//		
//		return digest;
//	}	

	protected final class NIOLoader {
		protected Selector selector;
		protected int numConns = 0;
		protected MemcachedClient mc;
		protected Connection[] conns;

		public NIOLoader( MemcachedClient mc ) {
			this.mc = mc;
		}

		private final class Connection {
		
			public List<ByteBuffer> incoming = new ArrayList<ByteBuffer>();
			public ByteBuffer outgoing;
			public SockIOPool.SockIO sock;
			public SocketChannel channel;
			private boolean isDone = false;
			
			public Connection( SockIOPool.SockIO sock, StringBuilder request ) throws IOException {
				if ( log.isDebugEnabled() )
					log.debug( "setting up connection to "+sock.getHost() );
				
				this.sock = sock;
				outgoing = ByteBuffer.wrap( request.append( "\r\n" ).toString().getBytes() );
				
				channel = sock.getChannel();
				if ( channel == null )
					throw new IOException( "dead connection to: " + sock.getHost() );

				channel.configureBlocking( false );
				channel.register( selector, SelectionKey.OP_WRITE, this );
			}
			
			public void close() {
				try {
					if ( isDone ) {
						// turn off non-blocking IO and return to pool
						if ( log.isDebugEnabled() )
							log.debug( "++++ gracefully closing connection to "+sock.getHost() );
						
						channel.configureBlocking( true );
						sock.close();
						return;
					}
				}
				catch ( IOException e ) {
					log.warn( "++++ memcache: unexpected error closing normally" );
				}
				
				try {
					if ( log.isDebugEnabled() )
						log.debug("forcefully closing connection to "+sock.getHost());

					channel.close();
					sock.trueClose();
				}
				catch ( IOException ignoreMe ) { }
			}
			
			public boolean isDone() {
				// if we know we're done, just say so
				if ( isDone )         
					return true;
				
				// else find out the hard way
				int strPos = B_END.length-1;

				int bi = incoming.size() - 1;
				while ( bi >= 0 && strPos >= 0 ) {
					ByteBuffer buf = incoming.get( bi );
					int pos = buf.position()-1;
					while ( pos >= 0 && strPos >= 0 ) {
					    if ( buf.get( pos-- ) != B_END[strPos--] )
							return false;
					}

					bi--;
				}
				
				isDone = strPos < 0;
				return isDone;
			}
			
			public ByteBuffer getBuffer() {
				int last = incoming.size()-1;
				if ( last >= 0 && incoming.get( last ).hasRemaining() ) {
					return incoming.get( last );
				}
				else {
					ByteBuffer newBuf = ByteBuffer.allocate( 8192 );
					incoming.add( newBuf );
					return newBuf;
				}
			}
			
			public String toString() {
				return "Connection to " + sock.getHost() + " with " + incoming.size() + " bufs; done is " + isDone;
			}
		}
		
		public void doMulti( boolean asString, Map<String, StringBuilder> sockKeys, String[] keys, Map<String, Object> ret ) {
		
			long timeRemaining = 0;
			try {
				selector = Selector.open();
				
				// get the sockets, flip them to non-blocking, and set up data
				// structures
				conns = new Connection[sockKeys.keySet().size()];
				numConns = 0;
				for ( Iterator<String> i = sockKeys.keySet().iterator(); i.hasNext(); ) {
					// get SockIO obj from hostname
					String host = i.next();

					SockIOPool.SockIO sock = pool.getConnection( host );

					if ( sock == null ) {
						if ( errorHandler != null )
							errorHandler.handleErrorOnGet( this.mc, new IOException( "no socket to server available" ), keys );
						return;
					}

					conns[numConns++] = new Connection( sock, sockKeys.get( host ) );
				}
				
				// the main select loop; ends when
				// 1) we've received data from all the servers, or
				// 2) we time out
				long startTime = System.currentTimeMillis();

				long timeout = pool.getMaxBusy();
				timeRemaining = timeout;
				
				while ( numConns > 0 && timeRemaining > 0 ) {
					int n = selector.select( Math.min( timeout,  5000 ) );
					if ( n > 0 ) {
					    // we've got some activity; handle it
					    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					    while ( it.hasNext() ) {
					        SelectionKey key = it.next();
					        it.remove();
					        handleKey( key );
					    }
					}
					else {
					    // timeout likely... better check
						// TODO:  This seems like a problem area that we need to figure out how to handle.
						log.error( "selector timed out waiting for activity" );
					}
					
					timeRemaining = timeout - (System.currentTimeMillis() - startTime);
				}
			}
			catch ( IOException e ) {
				// errors can happen just about anywhere above, from
				// connection setup to any of the mechanics
				handleError( e, keys );
				return;
			}
			finally {
				if ( log.isDebugEnabled() )
					log.debug( "Disconnecting; numConns=" + numConns + "  timeRemaining=" + timeRemaining );
				
				// run through our conns and either return them to the pool
				// or forcibly close them
				try {
					if ( selector != null )
						selector.close();
				}
				catch ( IOException ignoreMe ) { }
				
				for ( Connection c : conns ) {
					if ( c != null )
						c.close();
				}
			}
		
			// Done!  Build the list of results and return them.  If we get
			// here by a timeout, then some of the connections are probably
			// not done.  But we'll return what we've got...
			for ( Connection c : conns ) {
				try {
					if ( c.incoming.size() > 0 && c.isDone() )
						loadMulti( new ByteBufArrayInputStream( c.incoming ), ret, asString );
				}
				catch ( Exception e ) {
					// shouldn't happen; we have all the data already
					log.warn( "Caught the aforementioned exception on "+c );
				}
			}
		}
		
		private void handleError( Throwable e, String[] keys ) {
		    // if we have an errorHandler, use its hook
		    if ( errorHandler != null )
		        errorHandler.handleErrorOnGet( MemcachedClient.this, e, keys );
		
		    // exception thrown
		    log.error( "++++ exception thrown while getting from cache on getMulti" );
		    log.error( e.getMessage() );
		}
		
		private void handleKey( SelectionKey key ) throws IOException {
			if ( log.isDebugEnabled() )
				log.debug( "handling selector op " + key.readyOps() + " for key " + key );
			
			if ( key.isReadable() )
				readResponse( key );
			else if ( key.isWritable() )
				writeRequest( key );
		}
		
		public void writeRequest( SelectionKey key ) throws IOException {
			ByteBuffer buf = ((Connection) key.attachment()).outgoing;
			SocketChannel sc = (SocketChannel)key.channel();
			
			if ( buf.hasRemaining() ) {
				if ( log.isDebugEnabled() )
				    log.debug( "writing " + buf.remaining() + "B to " + ((SocketChannel) key.channel()).socket().getInetAddress() );

				sc.write( buf );
			}
			
			if ( !buf.hasRemaining() ) {
			    if ( log.isDebugEnabled() )
			        log.debug( "switching to read mode for server " + ((SocketChannel)key.channel()).socket().getInetAddress() );

				key.interestOps( SelectionKey.OP_READ );
			}
		}
		
		public void readResponse( SelectionKey key ) throws IOException {
			Connection conn = (Connection)key.attachment();
			ByteBuffer buf = conn.getBuffer();
			int count = conn.channel.read( buf );
			if ( count > 0 ) {
				if ( log.isDebugEnabled() )
					log.debug( "read  " + count + " from " + conn.channel.socket().getInetAddress() );
				
				if ( conn.isDone() ) {
					if ( log.isDebugEnabled() )
						log.debug( "connection done to  " + conn.channel.socket().getInetAddress() );

					key.cancel();
					numConns--;
					return;
				}
			}
		}
	}
}
