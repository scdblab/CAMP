package com.mitrallc.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

import com.mitrallc.common.Constants;
import com.mitrallc.kosar.kosar;
import com.mitrallc.mysqltrig.regthread;

//import org.apache.log4j.Logger;
//import org.apache.log4j.NDC;

public class Statement implements java.sql.Statement {
	private java.sql.Statement stmt;
	private java.sql.ResultSet rs;
	private boolean rs_from_cache;
	private com.mitrallc.sql.Connection conn;

	private String last_query;
	private static boolean transparentCaching = true;
	private boolean verbose=false;
	//private static Logger logger = Logger.getLogger(Statement.class.getName());
	
	public Statement( java.sql.Statement stmt, com.mitrallc.sql.Connection conn )
	{
		this.stmt = stmt;
		this.conn = conn;
		
		//this.queryResultCachingEnabled = this.conn.queryCachingEnabled();
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.isWrapperFor(arg0);
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.unwrap(arg0);
	}

	@Override
	public void addBatch(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.addBatch(arg0);
	}

	@Override
	public void cancel() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.cancel();
	}

	@Override
	public void clearBatch() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.clearBatch();
	}

	@Override
	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.clearWarnings();
	}

	@Override
	public void close() throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.close();
	}
	
	public static boolean isTransparentCaching() {
		return transparentCaching;
	}

	public static void setTransparentCaching(boolean transparentCaching) {
		Statement.transparentCaching = transparentCaching;
	}
	
	/***
	 * Assumes that the result set isn't used between the executeQuery and the call to
	 * this function.
	 * @param registration_queries
	 */
	public void sendQueryToCache( ) //Vector<QueryRegistration> registration_queries )
	{
		// If caching enabled, store query results into the cache
		if( KosarSoloDriver.kosarEnabled )
		{
			if( this.rs == null || this.last_query == null )
			{
				System.out.println("Error. Parameters not initialized");
			}
			else
			{
				if( !rs_from_cache )
				{
					try
					{		
						//Register triggers associated with the query
						//COSARInterface cosar = CacheConnectionPool.getConnection(this.last_query);
						//cosar.sendQueryToCache(this.last_query, (ResultSet)this.rs, registration_queries, this.conn);
						//CacheConnectionPool.returnConnection(cosar, this.last_query);
					}
					catch( Exception e )
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public boolean execute(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0);
	}

	@Override
	public boolean execute(String arg0, int arg1) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, int[] arg1) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public boolean execute(String arg0, String[] arg1) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.execute(arg0, arg1);
	}

	@Override
	public int[] executeBatch() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.executeBatch();
	}

	@Override
	public com.mitrallc.sql.ResultSet executeQuery(String sql) throws SQLException {
		// TODO Auto-generated method stub
		long Tmiss=0;
		com.mitrallc.sql.ResultSet cached_rowset = null;
		//NDC.push("Statement.executeQuery");
		

		
		// First check if COSAR has the query
		//COSARInterface cosar = null;
		if( KosarSoloDriver.kosarEnabled && this.conn.getAutoCommit() )
		{
			try
			{
				//cosar = CacheConnectionPool.getConnection(sql);
				cached_rowset = KosarSoloDriver.Kache.GetQueryResult(sql);
								
				if( cached_rowset != null )
				{				
					//logger.debug("Cache Hit: " + sql);
					this.rs = cached_rowset;
					this.last_query = sql;
					this.rs_from_cache = true;
					//CacheConnectionPool.returnConnection(cosar, sql);
					//NDC.pop();
					return (new com.mitrallc.sql.ResultSet(cached_rowset));
				}
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}
		
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(sql);

		Tmiss = System.currentTimeMillis();
		
		this.rs = this.stmt.executeQuery(sql);
		this.last_query = sql;
		this.rs_from_cache = false;
		//logger.debug("DBMS Execute Query: " + sql);
		
		if( KosarSoloDriver.kosarEnabled && this.conn.getAutoCommit() && KosarSoloDriver.Kache != null )
		{
			// sendQueryToCache expects a list. Since this has only 1 query, put that in a list
			//  and send that to the cosar client
			
			//System.out.println("Caching: " + sql);
			
						
			if( transparentCaching )
			{
				//Vector<QueryRegistration> query_list = new Vector<QueryRegistration>();
				//query_list.add(new QueryRegistration(sql, false));
				cached_rowset = new com.mitrallc.sql.ResultSet(this.rs);
				KosarSoloDriver.Kache.attemptToCache(sql, cached_rowset, Tmiss);
				//cosar.sendQueryToCache(sql, (ResultSet)this.rs, query_list, this.conn);				
			}
							
		}
		
		//NDC.pop();
		return (new com.mitrallc.sql.ResultSet( cached_rowset == null?rs:cached_rowset ));
	}

	@Override
	public int executeUpdate(String arg0) throws SQLException {
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);

		// TODO Auto-generated method stub
		return this.stmt.executeUpdate(arg0);
	}
	
	@Override
	public int executeUpdate(String arg0, int arg1) throws SQLException {
		// TODO Auto-generated method stub
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);
		
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public int executeUpdate(String arg0, int[] arg1) throws SQLException {
		// TODO Auto-generated method stub
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public int executeUpdate(String arg0, String[] arg1) throws SQLException {
		// TODO Auto-generated method stub
		//Block while triggers are being registered
		regthread.BusyWaitForRegThread(arg0);
		return this.stmt.executeUpdate(arg0, arg1);
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getConnection();
	}

	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getFetchDirection();
	}

	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getFetchSize();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getGeneratedKeys();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMaxFieldSize();
	}

	@Override
	public int getMaxRows() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMaxRows();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMoreResults();
	}

	@Override
	public boolean getMoreResults(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getMoreResults();
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getQueryTimeout();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSet();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSetConcurrency();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSetHoldability();
	}

	@Override
	public int getResultSetType() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getResultSetType();
	}

	@Override
	public int getUpdateCount() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getUpdateCount();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.getWarnings();
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.isClosed();
	}

	@Override
	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return this.stmt.isPoolable();
	}

	@Override
	public void setCursorName(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setCursorName(arg0);
	}

	@Override
	public void setEscapeProcessing(boolean arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setEscapeProcessing(arg0);
	}

	@Override
	public void setFetchDirection(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setFetchDirection(arg0);
	}

	@Override
	public void setFetchSize(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setFetchSize(arg0);
	}

	@Override
	public void setMaxFieldSize(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setMaxFieldSize(arg0);
	}

	@Override
	public void setMaxRows(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setMaxRows(arg0);
	}

	@Override
	public void setPoolable(boolean arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setPoolable(arg0);
	}

	@Override
	public void setQueryTimeout(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		this.stmt.setQueryTimeout(arg0);
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}
