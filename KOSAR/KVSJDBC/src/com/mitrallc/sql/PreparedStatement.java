package com.mitrallc.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.mitrallc.common.Constants;
import com.mitrallc.common.DynamicArray;
import com.mitrallc.common.KeyQueueDataItem;
import com.mitrallc.common.TransactionDataItem;
import com.mitrallc.communication.CacheModeController;
import com.mitrallc.kosar.kosar;
import com.mitrallc.kosar.exceptions.KosarSQLException;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sql.SockIOPool.SockIO;
import com.mitrallc.sqltrig.QueryToTrigger;

public class PreparedStatement implements java.sql.PreparedStatement {
	private java.sql.PreparedStatement stmt;
	private com.mitrallc.sql.Connection conn;
	private String input_query;
	private Object[] param_list;
		
	private java.sql.ResultSet rs;
	private com.mitrallc.sql.ResultSet mitraRS;
	private boolean rs_from_cache;
	private String last_query;
	private static boolean transparentCaching = true;
	private static long queryStartTime;
	private static long cmdStartTime;
	private static Logger logger = Logger.getLogger(Statement.class.getName());

	public static final String insert = "INSERT ";
	public static final String delete = "DELETE ";
	public static final String update = "Update ";

	private static final String CLASSNAME_STRING = "class java.lang.String";
	private boolean verbose = false;

	PreparedStatement(String query, java.sql.PreparedStatement pstmt,
			com.mitrallc.sql.Connection conn) {
		this.stmt = pstmt;
		this.conn = conn;
		this.input_query = query;

		this.param_list = new Object[parseNumParams(query)];
	}

	public static int parseNumParams(String query) {
		int start = 0;
		int found = 0;
		int num_params = 0;
		while (true) {
			found = query.indexOf('?', start);
			if (found < start) {
				break;
			} else {
				start = found + 1;
				num_params++;
			}
		}

		return num_params;
	}

	public static String generateQueryString(String input_query, Object[] param_array) {
		String final_query = "";
		int start = 0;
		int end = 0;

		if (param_array.length == 0) {
			return input_query;
		}

		for (int i = 0; i < param_array.length; i++) {
			end = input_query.indexOf('?', start);
			if (end > start) {
				final_query += input_query.substring(start, end);
				if (param_array[i] == null)
					final_query += "null";
				else {
					String obj_class = param_array[i].getClass().toString();
					if (param_array[i].getClass().toString()
							.equals(CLASSNAME_STRING)) {
						final_query += "'";
					}
					final_query += param_array[i].toString();
					if (param_array[i].getClass().toString()
							.equals(CLASSNAME_STRING)) {
						final_query += "'";
					}
				}
				start = end + 1;
			} else {
				return null;
			}
		}

		if (start < input_query.length()) {
			final_query += input_query.substring(start, input_query.length());
		}

		return final_query;
	}

	private String OLDgenerateQueryString(String input_query,
			Object[] param_array) {
		String final_query = "";
		int start = 0;
		int end = 0;

		for (int i = 0; i < param_array.length; i++) {
			end = input_query.indexOf('?', start);
			if (end > start) {
				final_query += input_query.substring(start, end);
				String obj_class = param_array[i].getClass().toString();
				if (param_array[i].getClass().toString()
						.equals(CLASSNAME_STRING)) {
					final_query += "'";
				}
				final_query += param_array[i].toString();
				if (param_array[i].getClass().toString()
						.equals(CLASSNAME_STRING)) {
					final_query += "'";
				}
				start = end + 1;
			} else {
				return null;
			}
		}

		return final_query;
	}

	@Override
	public void addBatch(String arg0) throws SQLException {
		this.stmt.addBatch(arg0);
	}

	@Override
	public void cancel() throws SQLException {
		this.stmt.cancel();
	}

	@Override
	public void clearBatch() throws SQLException {
		this.stmt.clearBatch();
	}

	@Override
	public void clearWarnings() throws SQLException {
		this.stmt.clearWarnings();
	}

	@Override
	public void close() throws SQLException {
		this.stmt.close();
	}

	@Override
	public boolean execute(String arg0) throws SQLException {
		return this.stmt.execute(arg0);
	}

	@Override
	public boolean execute(String arg0, int arg1) throws SQLException {
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, int[] arg1) throws SQLException {
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, String[] arg1) throws SQLException {
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public int[] executeBatch() throws SQLException {
		return this.stmt.executeBatch();
	}

	@Override
	public ResultSet executeQuery(String arg0) throws KosarSQLException {
		return this.executeQuery(arg0, false);
	}

	public ResultSet executeQuery(String sql, boolean preparedStatement)
			throws KosarSQLException {
		int rt = 0;

		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(sql);

		if(KosarSoloDriver.webServer != null) {
			cmdStartTime = System.currentTimeMillis();
			KosarSoloDriver.KosarNumQueryRequestsEventMonitor.newEvent(1);
			KosarSoloDriver.last100readQueries.add(sql);
		}
		try {
			// If this is an update operation, connect to DB directly.
			if (this.input_query.toUpperCase().trim().startsWith(insert)
					|| this.input_query.toUpperCase().trim().startsWith(update)
					|| this.input_query.toUpperCase().trim().startsWith(delete)){
				System.out.println("WARNING:  executeQuery is used to process an SQL update command.");
				this.executeUpdate();
				return null;
			}

			NDC.push("PreparedStatement.executeQuery");

			/*
			 * If this is not an update statement, check if:
			 * 
			 * 1. Kosar is enabled (controlled by setting kosarenabled to true
			 * in the config file)
			 * 
			 * 2. CacheRead is allowed (Cache read is allowed by default if
			 * Kosar is enabled. If the client loses connection to the
			 * coordinator for a duration of time longer than the standard lease
			 * duration, cache read is disallowed.
			 * 
			 * 3. The connection has auto-commit turned on.
			 */
			//			System.out.println("kosarEnabled="+KosarSoloDriver.kosarEnabled);
			//			System.out.println("isCachedReadAllowed="+CacheModeController.isCacheReadAllowed());
			//			System.out.println("AutoCommit="+this.conn.getAutoCommit());
			if (KosarSoloDriver.kosarEnabled
					&& CacheModeController.isCacheReadAllowed() == true 
					&& this.conn.getAutoCommit()
					&& QueryToTrigger.IsQuerySupported(sql)) {
				
				com.mitrallc.sql.ResultSet cached_rowset = null;
				cached_rowset = KosarSoloDriver.Kache.GetQueryResult(sql);
				//System.out.println(sql);
				if (cached_rowset != null) {
					logger.debug("Cache Hit: " + sql);
					this.rs = cached_rowset;
					this.last_query = sql;
					this.rs_from_cache = true;
					NDC.pop();
					if(KosarSoloDriver.webServer != null){
						rt = (int)(System.currentTimeMillis()-cmdStartTime);
						KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
						KosarSoloDriver.KosarCacheHitsEventMonitor.newEvent(1);
					}
					return cached_rowset;
				}
				long Tmiss = System.currentTimeMillis();
				while (true) {
					if (KosarSoloDriver.getFlags().coordinatorExists() 
							&& KosarSoloDriver.getFlags()
							.isCoordinatorConnected()) { 
						
						//Get decision from core given a sql query(db, copyFromClient, stealFromClient)
						byte[] reply = obtainDecisionFromCORE(sql);
						int commandLength = ByteBuffer.wrap(Arrays.copyOfRange(reply, 0, 4)).getInt();
						
						String command = new String(Arrays.copyOfRange(reply, 4, 4+commandLength), "UTF-8");
						reply = Arrays.copyOfRange(reply, 4+commandLength, reply.length);
						
						if(command.equalsIgnoreCase("querydb")) {
							//Go to RDBMS.  Don't read any more bytes from reply[]
							executeQueryAgainstDB(preparedStatement, sql);
							logger.debug("DBMS Execute Query: " + sql);
							attemptToCache(sql, Tmiss);
						} else {
							int index = 0;
							int length = 0;
							byte[] clientID;
							byte[] ip;
							
							/*
							 * Reply comes from CORE in the following format:
							 * 
							 * 	int: 	clientID0 
							 * 	int: 	ip0.length 
							 * 	byte[]: ip0 
							 * 	int:	clientID1 
							 * 	byte[]:	ip1.length 
							 * 	int:	ip1 
							 * 	... 
							 * 	int:	clientIDn 
							 * 	int:	ipn.length 
							 * 	byte[]:	ipn
							 * 
							 * If the first ip doesn't have the cached pair, go to the next.
							 */
							boolean success = false;
							while(! (index >= reply.length) ) {
								clientID = Arrays.copyOfRange(reply, index, index+4);
								index += 4;
								length = ByteBuffer.wrap(Arrays.copyOfRange(reply, index, index+4)).getInt();
								index += 4;
								ip = Arrays.copyOfRange(reply, index, index+length);
								index += length;
								if(!(ByteBuffer.wrap(clientID).compareTo(
										ByteBuffer.wrap(KosarSoloDriver.clientData.getID())) == 0)) {
									SockIOPool pool = getPoolFromIP(clientID, ip);
									if(pool != null) {
										byte[] rs = getResultSetFromClient(pool, sql, command);
										if(rs != null) {
											this.mitraRS = new com.mitrallc.sql.ResultSet();
											this.mitraRS.deserialize(rs);
											if(command.equalsIgnoreCase("steal") || command.equalsIgnoreCase("copy"))
												attemptToCache(sql, Tmiss);
											success = true;
											break;
										}
									}
								}
							}
							if(!success) {
								//Could not find the sql from another client. Go to DB directly
								executeQueryAgainstDB(preparedStatement, sql);
								logger.debug("DBMS Execute Query: " + sql);
								
								//Be careful. Do not cache if the CORE does not know this client has KV pair cached.
								if(command.equalsIgnoreCase("copy") || command.equalsIgnoreCase("steal"))
									attemptToCache(sql, Tmiss);
							}
						}
					} else {						
						//If there is no coordinator, cache the item directly.
						executeQueryAgainstDB(preparedStatement, sql);
						logger.debug("DBMS Execute Query: " + sql);
						attemptToCache(sql, Tmiss);
					}
					if(this.mitraRS != null) break;
				} //end while

			} else {
				// If kosar does not exist or cache read is not allowed or
				// auto-commit is not on, execute against DB.
				executeQueryAgainstDB(preparedStatement, sql);
				logger.debug("DBMS Execute Query: " + sql);
			}
			NDC.pop();
		} catch (SQLException s) {
			throw new KosarSQLException(s.getMessage());
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported Encoding Exception for command");
			e.printStackTrace();
		}
		if (KosarSoloDriver.webServer != null){
				rt = (int)(System.currentTimeMillis()-cmdStartTime);
				KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
		}
		return this.mitraRS;
	}

	/**
	 * Send Client request for ResultSet for requested query sql.
	 * Send Message Format:
	 * 		int: Command.length
	 * 		byte[] (String.getBytes()): Command ("steal", "copy" or "consume")
	 * 		byte[] (String.getBytes()): sql query
	 * 
	 * Response from Client:
	 * 		int: 0 (found) or -1 (not found)
	 * 		byte[] resultSet (serialized com.mitrallc.sql.ResultSet)
	 */
	private byte[] getResultSetFromClient(SockIOPool pool, String sql, String command) {
		SockIO sockIO = null;
		try {
			while(true) {
				sockIO = pool.getSock();
				if(sockIO == null) {
					introduceDelay();
					continue;
				}
				else {
					if (!command.equalsIgnoreCase("steal") 
							&& !command.equalsIgnoreCase("copy")
							&& !command.equalsIgnoreCase("consume")) {
						System.out.println("Error PreparedStatement: Bad return code from CORE.");
						System.out.println("No suggestions.  Fatal error with sockets. Exiting.");
						System.exit(-1);
					}
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ByteBuffer bb = ByteBuffer.allocate(4);
					bb.putInt(command.getBytes().length);
					baos.write(bb.array());
					baos.write(command.getBytes());
					baos.write(sql.getBytes());
					baos.flush();
					sockIO.write(baos.toByteArray());
					sockIO.flush();
					baos.reset();
					byte[] rs = sockIO.readBytes();
					int response = ByteBuffer.wrap(Arrays.copyOfRange(rs, 0, 4)).getInt();
					if(response == 0) {
						return Arrays.copyOfRange(rs, 4, rs.length);
					} else return null;
				}
			}
		} catch(IOException ioex) {
			System.out
					.println("Error: PreparedStatement - Failed to request sql from Client. Going to next client or database.");
			return null;  //note, finally executes before this returns.
		} finally {
			if(KosarSoloDriver.getFlags().coordinatorExists()
					&& sockIO != null)
				sockIO.close();
		}
	}
	/**
	 * Looks for Existing Pool in clientPoolMap with the ClientID as key.
	 * If found, return pool
	 * Else, create new one.
	 */
	private SockIOPool getPoolFromIP(byte[] clientID, byte[] ip) {
		
		String address = null;
		SockIOPool pool = null;
		try {
			address = InetAddress.getByAddress(ip).getHostAddress();
			pool = null;
			synchronized(KosarSoloDriver.clientPoolMap) {
				if((pool = KosarSoloDriver.clientPoolMap.get(ByteBuffer.wrap(clientID))) == null) {
					System.out.println("Creating new socket pool for " + Arrays.toString(clientID));
					pool = new SockIOPool();
					pool.setServer(address+":"+KosarSoloDriver.clientsport);
					pool.setInitConn(KosarSoloDriver.init_connections);
					pool.initialize();
					KosarSoloDriver.clientPoolMap.put(ByteBuffer.wrap(clientID), pool);
				}
			}
		} catch (UnknownHostException e) {
			System.out.println("Could not obtain address from IP sent from CORE");
			System.out.println("Check integrity of ip array " + Arrays.toString(ip) + " for client " + Arrays.toString(clientID));
			e.printStackTrace();
		} catch (ConnectException e) {
			System.out.println("Error:PreparedStatement - Could not connect to Client.");
			e.printStackTrace();
		}
		return pool;
	}
	
	/**
	 * Send to the CORE the client id, sql query and internal tokens.
	 * CORE will deal with the data and return a command.
	 * @param sql
	 * @return
	 */
	private byte[] obtainDecisionFromCORE(String sql) {
		byte[] response = null;
		Vector<String> internal_key_list = 
				KosarSoloDriver.Kache.getInternalTokensFromQry(sql);
		/*
		 * Message the CORE with the following format:
		 * clientID sqlLength sql it0.length it0 it1.length it1 ... itn.length itn
		 */
		try {
			/* Construct Message for CORE */
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(Constants.CLIENT_KEY_CACHED);
			baos.write(bb.array());
			bb.clear();
			// Append client id.
			baos.write(KosarSoloDriver.clientData.getID());
			byte[] sqlBytes = sql.getBytes();
			bb.putInt(sqlBytes.length);
			baos.write(bb.array());
			bb.clear();
			baos.write(sqlBytes);
			for(String it : internal_key_list) {
				byte[] itBytes = it.getBytes();
				bb.putInt(itBytes.length);
				baos.write(bb.array());
				bb.clear();
				baos.write(itBytes);
			}
			baos.flush();
			/* Send Message to CORE */
			SockIO socket = null;
			try {
				while(true) {
					socket = KosarSoloDriver.getConnectionPool().getSock();
					if(socket == null) {
						introduceDelay();
						continue;
					} else {
						socket.write(baos.toByteArray());
						socket.flush();
						response = socket.readBytes();
						break;
					}
				}
			} catch(IOException ioex) {
				System.out.println("Fatal error.  Client could not send message to CORE");
				ioex.printStackTrace();
			} finally {
				if(socket != null)
					socket.close();
			}
			baos.reset();
		} catch(IOException ioex){
			ioex.printStackTrace();
		} 
		return response;
	}

	private void executeQueryAgainstDB(boolean preparedStatement, String sql)
			throws SQLException {
		queryStartTime = System.currentTimeMillis();
		if (preparedStatement) {
			this.mitraRS = new com.mitrallc.sql.ResultSet(
					this.stmt.executeQuery());
		} else {
			this.mitraRS = new com.mitrallc.sql.ResultSet(
					this.stmt.executeQuery(sql));
		}
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarQueryResponseTimeEventMonitor.newEvent(
					(int)(System.currentTimeMillis()-queryStartTime));

		this.last_query = sql;
		this.rs_from_cache = false;

	}

	public static void introduceDelay() {
		try {
			Thread.sleep(Constants.sleepTime);
		} catch (InterruptedException e) {
		}
	}


	public void attemptToCache(String sql, long Tmiss)
			throws KosarSQLException {
		try {
			if (CacheModeController.isCacheUpdateAllowed()
					&& this.conn.getAutoCommit()
					&& KosarSoloDriver.Kache != null) {

				if (transparentCaching) {
					KosarSoloDriver.Kache.attemptToCache(sql,
							(new com.mitrallc.sql.ResultSet(this.mitraRS)),
							Tmiss);
				}
			}
		} catch (SQLException s) {
			//KosarSoloDriver.getLockManager().unlockKey(sql);
			throw new KosarSQLException(s.getMessage());
		}
	}


	@Override
	public int executeUpdate(String arg0) throws SQLException {
		int retVal;


		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);

		if(KosarSoloDriver.webServer != null)
			cmdStartTime = System.currentTimeMillis();
		if (KosarSoloDriver.kosarEnabled) {
			if (KosarSoloDriver.getFlags().coordinatorExists()) {
				if (KosarSoloDriver.getFlags().isCoordinatorConnected()) {
					// 1. Send a message to the coordinator indicating that a
					// write transaction has started.
					/*while (true) {
						SockIO socket = null;
						try {
							// Create Message
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							baos.write(Constants.CLIENT_WR_START);
							baos.write('|');
							baos.write(KosarSoloDriver.clientData.getID());
							String updateKey = Long.toString(Thread
									.currentThread().getId());
							baos.write(updateKey.getBytes());
							if (null == socket)
								socket = KosarSoloDriver.getConnectionPool()
								.getSock();
							if (socket != null) {
								socket.write(baos.toByteArray());
								socket.flush();
								break;
							} else
								introduceDelay();
						} catch (ConnectException c) {
							KosarSoloDriver.startReconnectThread(System
									.currentTimeMillis());
							logger.debug("Execute Update: " + this.stmt);
							break;
						} catch (SocketTimeoutException e) {
							System.out
							.println("Execute Update-Transaction Started:socket time out exception");
							introduceDelay();
						} catch (IOException e) {
							System.out
							.println("Execute Update-Transaction Started:io exception");
							KosarSoloDriver.startReconnectThread(System
									.currentTimeMillis());
							logger.debug("Execute Update-Transaction Started: "
									+ this.stmt);
							break;
						} finally {
							if (KosarSoloDriver.getFlags()
									.isCoordinatorConnected() && null != socket) {
								socket.close();
							}
						}
					}*/
					// 2. Go ahead if the update

					retVal = this.stmt.executeUpdate();

					// 3. When the update returns, send a message to the
					// coordinator
					// indicating the transaction has ended.
					/*while (true) {
						SockIO soc = null;
						try {
							// Create Message
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							baos.write(Constants.CLIENT_WR_STOP);
							baos.write('|');
							baos.write(KosarSoloDriver.clientData.getID());
							String updateKey = Long.toString(Thread
									.currentThread().getId());
							baos.write(updateKey.getBytes());
							baos.write('|');
							if (retVal > 0) {
								baos.write('1');
							} else {
								baos.write('0');
							}

							if (null == soc)
								soc = KosarSoloDriver.getConnectionPool()
								.getSock();
							if (soc != null) {
								soc.write(baos.toByteArray());
								soc.flush();
								break;
							} else {
								introduceDelay();
							}
						} catch (ConnectException c) {
							KosarSoloDriver.startReconnectThread(System
									.currentTimeMillis());
							logger.debug("Execute Update-Transaction Over: "
									+ this.stmt);
							break;
						} catch (SocketTimeoutException e) {
							System.out
							.println("Execute Update-Transaction Over:socket time out exception");
							introduceDelay();
						} catch (IOException e) {
							System.out
							.println("Execute Update-Transaction Over:io exception");
							KosarSoloDriver.startReconnectThread(System
									.currentTimeMillis());
							logger.debug("DBMS Execute Update: " + this.stmt);
							break;
						} finally {
							if (KosarSoloDriver.getFlags()
									.isCoordinatorConnected() && null != soc) {
								soc.close();
							}
						}
					}*/
				} else {
					retVal = this.stmt.executeUpdate();
				}
			} else {
				/* Deletion of Keys from the Cache without Coordinator
				 * Beginning of Transaction; set start time of transaction;
				 * Data structure found & described in com.mitrallc.common.DynamicArray
				 */
				Constants.PENDING_TRANSACTION_WRITE_LOCK.lock();
				long currentStart = Constants.AI.incrementAndGet();
				KosarSoloDriver.pendingTransactionArray.add(currentStart);
				Constants.PENDING_TRANSACTION_WRITE_LOCK.unlock();

				/*
				 * Execution of this statement goes to the DBMS.
				 * Returns to com.mitrallc.kosar.TriggerListener
				 */
				retVal = this.stmt.executeUpdate();

				/*
				 * For keys whose timestamp lies between the start
				 * and end times of the transaction, remove from the cache.
				 * Those keys have been updated in some way and should not
				 * be read from the cache.
				 */
				Constants.KEY_QUEUE_READ_LOCK.lock();
				long endtime = Constants.AI.incrementAndGet();
				for(int i = 0; i < KosarSoloDriver.keyQueue.size(); i++)
					if(KosarSoloDriver.keyQueue.getCounter(i) > currentStart
							&& KosarSoloDriver.keyQueue.getCounter(i) < endtime) {
						String keylist = new String(KosarSoloDriver.keyQueue.getKeyList(i));
						String[] its = keylist.trim().split(" ");
						for (int j = 0; j < its.length; j++)
							KosarSoloDriver.Kache.DeleteIT(its[j]);
					}
				Constants.KEY_QUEUE_READ_LOCK.unlock();

				/*
				 * If the current transaction is the oldest in the array,
				 * and we need to remove keys from the queue whose timestamp
				 * is before the lowest of either the start-time of the next oldest
				 * transaction or the end-time of the oldest.
				 * 
				 * Explanation: If the current transaction (oldest) ended AFTER
				 * another transaction begins, we don't want to remove keys from the
				 * queue that correspond to the next transaction.  Otherwise, keys
				 * may not be deleted
				 */	
				long cleanupTime=-1;
				Constants.PENDING_TRANSACTION_READ_LOCK.lock();
				if(KosarSoloDriver.pendingTransactionArray.getIndexOf(currentStart) == 0) {
					if(KosarSoloDriver.pendingTransactionArray.size() > 1
							&& KosarSoloDriver.pendingTransactionArray.getCounter(1) < endtime)
						cleanupTime = KosarSoloDriver.pendingTransactionArray.getCounter(1);
					else cleanupTime = endtime;
				}
				Constants.PENDING_TRANSACTION_READ_LOCK.unlock();

				if (cleanupTime != -1){
					Constants.KEY_QUEUE_WRITE_LOCK.lock();
					KosarSoloDriver.keyQueue.removeUpTo(cleanupTime);
					/*for(int i = 0; i < KosarSoloDriver.keyQueue.size(); i++)
								if(KosarSoloDriver.keyQueue.getCounter(i) < cleanupTime)
									KosarSoloDriver.keyQueue.remove(
											KosarSoloDriver.keyQueue.getCounter(i));*/
					Constants.KEY_QUEUE_WRITE_LOCK.unlock();
				}

				/*
				 * Finally, remove the transaction from the list.
				 */
				Constants.PENDING_TRANSACTION_WRITE_LOCK.lock();
				KosarSoloDriver.pendingTransactionArray.remove(currentStart);
				Constants.PENDING_TRANSACTION_WRITE_LOCK.unlock();
			}
		} else {
			retVal = this.stmt.executeUpdate();
		}
		if(KosarSoloDriver.webServer != null) {
			int rt = (int)(System.currentTimeMillis()-cmdStartTime);
			KosarSoloDriver.KosarRTEventMonitor.newEvent(rt);
			KosarSoloDriver.KosarDMLUpdateEventMonitor.newEvent(rt);
			KosarSoloDriver.last100updateQueries.add(arg0);
		}
		return retVal;
	}

	@Override
	public int executeUpdate(String arg0, int arg1) throws SQLException {
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public int executeUpdate(String arg0, int[] arg1) throws SQLException {
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public int executeUpdate(String arg0, String[] arg1) throws SQLException {
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return this.stmt.getConnection();
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return this.stmt.getFetchDirection();
	}

	@Override
	public int getFetchSize() throws SQLException {
		return this.stmt.getFetchSize();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return this.stmt.getGeneratedKeys();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return this.stmt.getMaxFieldSize();
	}

	@Override
	public int getMaxRows() throws SQLException {
		return this.stmt.getMaxRows();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return this.stmt.getMoreResults();
	}

	@Override
	public boolean getMoreResults(int arg0) throws SQLException {
		return this.stmt.getMoreResults(arg0);
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return this.stmt.getQueryTimeout();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return this.stmt.getResultSet();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return this.stmt.getResultSetConcurrency();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return this.stmt.getResultSetHoldability();
	}

	@Override
	public int getResultSetType() throws SQLException {
		return this.stmt.getResultSetType();
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return this.stmt.getUpdateCount();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return this.stmt.getWarnings();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return this.stmt.isClosed();
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return this.stmt.isPoolable();
	}

	@Override
	public void setCursorName(String arg0) throws SQLException {
		this.stmt.setCursorName(arg0);
	}

	@Override
	public void setEscapeProcessing(boolean arg0) throws SQLException {
		this.stmt.setEscapeProcessing(arg0);
	}

	@Override
	public void setFetchDirection(int arg0) throws SQLException {
		this.stmt.setFetchDirection(arg0);
	}

	@Override
	public void setFetchSize(int arg0) throws SQLException {
		this.stmt.setFetchSize(arg0);
	}

	@Override
	public void setMaxFieldSize(int arg0) throws SQLException {
		this.stmt.setMaxFieldSize(arg0);
	}

	@Override
	public void setMaxRows(int arg0) throws SQLException {
		this.stmt.setMaxRows(arg0);
	}

	@Override
	public void setPoolable(boolean arg0) throws SQLException {
		this.stmt.setPoolable(arg0);
	}

	@Override
	public void setQueryTimeout(int arg0) throws SQLException {
		this.stmt.setQueryTimeout(arg0);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.stmt.isWrapperFor(iface);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return this.stmt.unwrap(iface);
	}

	@Override
	public void addBatch() throws SQLException {
		this.stmt.addBatch();
	}

	@Override
	public void clearParameters() throws SQLException {
		this.stmt.clearParameters();
	}

	@Override
	public boolean execute() throws SQLException {

		return this.stmt.execute();
	}

	@Override
	public ResultSet executeQuery() throws SQLException {

		// System.out.println("ExecuteQuery(): "+this.input_query);
		return this.executeQuery(
				this.generateQueryString(this.input_query, this.param_list),
				true);
	}

	@Override
	public int executeUpdate() throws SQLException {
		return this.executeUpdate(this.generateQueryString(this.input_query,
				this.param_list));
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {

		return this.stmt.getMetaData();
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {

		return this.stmt.getParameterMetaData();
	}

	@Override
	public void setArray(int arg0, Array arg1) throws SQLException {

		this.stmt.setArray(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setAsciiStream(int arg0, InputStream arg1) throws SQLException {

		this.stmt.setAsciiStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setAsciiStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {

		this.stmt.setAsciiStream(arg0, arg1, arg2);
	}

	@Override
	public void setAsciiStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {

		this.stmt.setAsciiStream(arg0, arg1, arg2);
	}

	@Override
	public void setBigDecimal(int arg0, BigDecimal arg1) throws SQLException {

		this.stmt.setBigDecimal(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBinaryStream(int arg0, InputStream arg1) throws SQLException {

		this.stmt.setBinaryStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBinaryStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {

		this.stmt.setBinaryStream(arg0, arg1, arg2);
	}

	@Override
	public void setBinaryStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {

		this.stmt.setBinaryStream(arg0, arg1, arg2);
	}

	@Override
	public void setBlob(int arg0, Blob arg1) throws SQLException {

		this.stmt.setBlob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBlob(int arg0, InputStream arg1) throws SQLException {

		this.stmt.setBlob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBlob(int arg0, InputStream arg1, long arg2)
			throws SQLException {

		this.stmt.setBlob(arg0, arg1, arg2);
	}

	@Override
	public void setBoolean(int arg0, boolean arg1) throws SQLException {

		this.stmt.setBoolean(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setByte(int arg0, byte arg1) throws SQLException {

		this.stmt.setByte(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setBytes(int arg0, byte[] arg1) throws SQLException {

		this.stmt.setBytes(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setCharacterStream(int arg0, Reader arg1) throws SQLException {

		this.stmt.setCharacterStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setCharacterStream(int arg0, Reader arg1, int arg2)
			throws SQLException {

		this.stmt.setCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void setCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {

		this.stmt.setCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void setClob(int arg0, Clob arg1) throws SQLException {

		this.stmt.setClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setClob(int arg0, Reader arg1) throws SQLException {

		this.stmt.setClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setClob(int arg0, Reader arg1, long arg2) throws SQLException {

		this.stmt.setClob(arg0, arg1, arg2);
	}

	@Override
	public void setDate(int arg0, Date arg1) throws SQLException {

		this.stmt.setDate(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setDate(int arg0, Date arg1, Calendar arg2) throws SQLException {

		this.stmt.setDate(arg0, arg1, arg2);

	}

	@Override
	public void setDouble(int arg0, double arg1) throws SQLException {

		this.stmt.setDouble(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setFloat(int arg0, float arg1) throws SQLException {

		this.stmt.setFloat(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setInt(int arg0, int arg1) throws SQLException {
		this.stmt.setInt(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setLong(int arg0, long arg1) throws SQLException {

		this.stmt.setLong(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNCharacterStream(int arg0, Reader arg1) throws SQLException {

		this.stmt.setNCharacterStream(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {

		this.stmt.setNCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void setNClob(int arg0, NClob arg1) throws SQLException {

		this.stmt.setNClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNClob(int arg0, Reader arg1) throws SQLException {

		this.stmt.setNClob(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException {

		this.stmt.setNClob(arg0, arg1, arg2);
	}

	@Override
	public void setNString(int arg0, String arg1) throws SQLException {

		this.stmt.setNString(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNull(int arg0, int arg1) throws SQLException {

		this.stmt.setNull(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setNull(int arg0, int arg1, String arg2) throws SQLException {

		this.stmt.setNull(arg0, arg1, arg2);
	}

	@Override
	public void setObject(int arg0, Object arg1) throws SQLException {

		this.stmt.setObject(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setObject(int arg0, Object arg1, int arg2) throws SQLException {

		this.stmt.setObject(arg0, arg1, arg2);
	}

	@Override
	public void setObject(int arg0, Object arg1, int arg2, int arg3)
			throws SQLException {

		this.stmt.setObject(arg0, arg1, arg2, arg3);
	}

	@Override
	public void setRef(int arg0, Ref arg1) throws SQLException {

		this.stmt.setRef(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setRowId(int arg0, RowId arg1) throws SQLException {

		this.stmt.setRowId(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {

		this.stmt.setSQLXML(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setShort(int arg0, short arg1) throws SQLException {

		this.stmt.setShort(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setString(int arg0, String arg1) throws SQLException {

		this.stmt.setString(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setTime(int arg0, Time arg1) throws SQLException {

		this.stmt.setTime(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException {

		this.stmt.setTime(arg0, arg1, arg2);
	}

	@Override
	public void setTimestamp(int arg0, Timestamp arg1) throws SQLException {

		this.stmt.setTimestamp(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2)
			throws SQLException {

		this.stmt.setTimestamp(arg0, arg1, arg2);
	}

	@Override
	public void setURL(int arg0, URL arg1) throws SQLException {

		this.stmt.setURL(arg0, arg1);
		this.param_list[arg0 - 1] = arg1;
	}

	@Override
	public void setUnicodeStream(int arg0, InputStream arg1, int arg2)
			throws SQLException {

		this.stmt.setUnicodeStream(arg0, arg1, arg2);
	}

	@Override
	public void closeOnCompletion() throws SQLException {

	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return false;
	}

}
