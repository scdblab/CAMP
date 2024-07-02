package com.mitrallc.UnitTest;


import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;

import com.mitrallc.config.DBConnector;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.ResultSet;
import com.mitrallc.sql.Statement;

public class TestImplementation {
	public static boolean UseKosarSolo = true;
	
	private static String oracleDriver = "oracle.jdbc.driver.OracleDriver";
	private static String kosarDriver = "com.mitrallc.sql.KosarSoloDriver";
	
	private static PreparedStatement preparedStatement=null;
	
	public static void TestQuery() {
		Connection conn = null; 
		ResultSet rs = null;
		Statement st = null;
		String query;
		Vector<Integer> pendingIds = new Vector<Integer>();
		try {
			if (UseKosarSolo) 
			{
				// Use the COSAR wrapper version of the JDBC driver
				Class.forName (kosarDriver);
				//DriverManager.getConnection(paramString, paramProperties)

				// Initialize the cache connection pool
				//int num_connections_per_server = 10;
				//CacheConnectionPool.addServer(RaysConfig.getCacheServerHostname(), RaysConfig.getCacheServerPort());
				//CacheConnectionPool.init(num_connections_per_server, true);
			}
			else Class.forName (oracleDriver);
			//profile details
			
			String url = DBConnector.getConnectionString(); 
			conn = DriverManager.getConnection(url,DBConnector.getUsername(),DBConnector.getPassword());

			st = (Statement) conn.createStatement();
			query = "SELECT min(userid) from users";
			//query = "SELECT inviterid from friendship where inviteeid='"+5+"' and status='1'";
			rs = st.executeQuery(query);
			while(rs.next()){
				pendingIds.add(rs.getInt(1));
			}	
		}catch(SQLException sx){
			sx.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				if(rs != null)
					rs.close();
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} 
	}
	
	public static void TestQuery3() {
		Connection conn = null; 
		ResultSet rs = null;
		try{ 
			if (UseKosarSolo) 
			{
				// Use the COSAR wrapper version of the JDBC driver
				Class.forName (kosarDriver);
				//DriverManager.getConnection(paramString, paramProperties)

				// Initialize the cache connection pool
				//int num_connections_per_server = 10;
				//CacheConnectionPool.addServer(RaysConfig.getCacheServerHostname(), RaysConfig.getCacheServerPort());
				//CacheConnectionPool.init(num_connections_per_server, true);
			}
			else Class.forName (oracleDriver);
			//profile details
			
			String url = DBConnector.getConnectionString(); 
			conn = DriverManager.getConnection(url,DBConnector.getUsername(),DBConnector.getPassword());


			String query = "SELECT userid,username, fname, lname, gender, dob, jdate, ldate, address, email, tel, pic FROM  users WHERE UserID = ? or UserID=7 or UserID=9";
			try {
				preparedStatement = (PreparedStatement) conn.prepareStatement(query);
				preparedStatement.setInt(1, 5);
				rs = (ResultSet) preparedStatement.executeQuery();
				java.sql.ResultSetMetaData md = (java.sql.ResultSetMetaData) rs.getMetaData();
				int col = md.getColumnCount();
				System.out.println("number of columns "+col);
				while(rs.next()){
					for (int i = 1; i <= col; i++){
						String col_name = md.getColumnName(i);
						String value ="";
						if(col_name.equalsIgnoreCase("pic") ){
							// Get as a BLOB
							Blob aBlob = rs.getBlob(col_name);
							byte[] allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
							value = allBytesInBlob.toString();
							//if test mode dump pic into a file
						}else
							value = rs.getString(col_name);
						System.out.println(""+col_name+"="+value);
					}
				}

			}catch(SQLException sx){
				sx.printStackTrace();
			}finally{
				try {
					if (rs != null)
						rs.close();
					if(preparedStatement != null)
						preparedStatement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
		
	public static void TestQuery2() {

		Connection conn = null; 

		try{ 
			if (UseKosarSolo) 
			{
				// Use the COSAR wrapper version of the JDBC driver
				Class.forName (kosarDriver);
				//DriverManager.getConnection(paramString, paramProperties)

				// Initialize the cache connection pool
				//int num_connections_per_server = 10;
				//CacheConnectionPool.addServer(RaysConfig.getCacheServerHostname(), RaysConfig.getCacheServerPort());
				//CacheConnectionPool.init(num_connections_per_server, true);
			}
			else Class.forName (oracleDriver);

			String url = DBConnector.getConnectionString(); 
			conn = DriverManager.getConnection(url,DBConnector.getUsername(),DBConnector.getPassword());

			System.out.println("connected");//createSchema();
			int profileOwnerID=1;
			String query = "SELECT count(*) FROM  friendship WHERE (inviterID = ? OR inviteeID = ?) AND status = 2 ";
			preparedStatement = (PreparedStatement) conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			preparedStatement.setInt(2, profileOwnerID);
			ResultSet rs = (ResultSet) preparedStatement.executeQuery();
			if (rs.next())
				System.out.println("Found a record.");
			rs.close();
			preparedStatement.close();
			conn.close();

			
		}catch(Exception e){
			e.printStackTrace();
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}    		
		}
	}
	
	public static void TestQuery4() {

		Connection conn = null; 

		try{ 
			if (UseKosarSolo) 
			{
				// Use the COSAR wrapper version of the JDBC driver
				Class.forName (kosarDriver);
				//DriverManager.getConnection(paramString, paramProperties)

				// Initialize the cache connection pool
				//int num_connections_per_server = 10;
				//CacheConnectionPool.addServer(RaysConfig.getCacheServerHostname(), RaysConfig.getCacheServerPort());
				//CacheConnectionPool.init(num_connections_per_server, true);
			}
			else Class.forName (oracleDriver);
			
			conn = DriverManager.getConnection(DBConnector.getConnectionString(),
					DBConnector.getUsername(),
					DBConnector.getPassword());
			conn.setAutoCommit(true);
			String sql = "create table usertable(userid varchar2(10), username varchar2(10), pwd varchar2(10))";
			PreparedStatement cstmt = conn.prepareStatement(sql);
			cstmt.execute();
			cstmt.close();
			
			String insert = "insert into usertable values(?,?,?)";
			PreparedStatement stmt = conn.prepareStatement(insert);
			for(int i=0;i<50000;++i) {
				stmt.setString(1, i+"");
				stmt.setString(2, "u"+i);
				stmt.setString(3, "p"+i);
				stmt.addBatch();
				if(i%1000 == 0) {
					stmt.executeBatch();
				}
			}
			stmt.executeBatch();
			stmt.close();
			conn.close();


			//get user offset
			final String query1 = "Select username from usertable where userid = ? "; 
			final String query2 = "Select userid from usertable where userid = ? "; 
			final String query3 = "Select pwd from usertable where userid = ? "; 
			final String query4 = "Select userid,username from usertable where userid = ? "; 
			final String query5 = "Select userid,pwd from usertable where userid = ? "; 
			final String query6 = "Select username,pwd from usertable where userid = ? "; 
			final String query7 = "Select userid, username, pwd from usertable where userid = ? "; 
			
			String set1[] = {/*query1, query2, */query3};
			String set2[] = {query4, query5};
			String set3[] = {query6, query7};
			
			Thread one = new Thread(new TestRunner(set1));
			one.start();
			/*new Thread(new TestRunner(set2)).start();
			new Thread(new TestRunner(set3)).start();
			
			//one.join();
			new Thread(new TestRunner(set1)).start();
			new Thread(new TestRunner(set3)).start();
			new Thread(new TestRunner(set2)).start();
			new Thread(new TestRunner(set1)).start();
			*/
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			if(null!=conn)
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestQuery4();
	}

	private static void TestCoordinator() {
	}
	
	static class TestRunner implements Runnable {
		final String url = DBConnector.getConnectionString();
		String queries[] = null;
		public TestRunner(String queries[]) {
			this.queries = queries;
		}
		//System.out.println("connected");//createSchema();
		
			@Override
			public void run() {
				Connection conn;
				try {
					conn = DriverManager.getConnection(url,
							DBConnector.getUsername(),
							DBConnector.getPassword());
					conn.setAutoCommit(true);

					for(String query:queries)
						executeSet(query,100,conn); 
					executeUpdate(queries[0], 100, conn);
					System.out.println("updated successfully"+this.toString());
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				//System.out.println(KosarSoloDriver.pendingTransactionList.size());
			}
			
			protected void executeUpdate(String queryTemplate, int numRows, Connection conn) throws SQLException {
				PreparedStatement st = conn
						.prepareStatement(/*queryTemplate*/"update usertable set pwd=? where userid=?");

				for (int i = 1; i <= numRows+10000; ++i) {

					//System.out.println(i);
					st.setString(2, String.valueOf(i));
					st.setString(1, "p179"+i);
					// get user offset
					try {
						st.executeUpdate();
					} catch(SQLException s) {
						try{
							conn = DriverManager.getConnection(url,
									DBConnector.getUsername(),
									DBConnector.getPassword());
							conn.setAutoCommit(true);
							st = conn
									.prepareStatement(queryTemplate);
						}catch (Exception e) {
							
						}
						//s.printStackTrace();
					}
				}
				st.close();		
			}
			
			protected void executeSet(String queryTemplate, int numRows, Connection conn) throws SQLException {
				PreparedStatement st = conn
						.prepareStatement(queryTemplate);

				for (int i = 1; i <= numRows; ++i) {

					//System.out.println(i);
					st.setString(1, String.valueOf(i));
					// get user offset
					try {
						java.sql.ResultSet rs = st.executeQuery();
						if (rs.next()) {
							/*if((i%1000)==0)
								System.out.println(rs.getString(1));*/
						}
						//System.out.println(offset);
						rs.close();
					} catch(SQLException s) {
						try{
							conn = DriverManager.getConnection(url,
									DBConnector.getUsername(),
									DBConnector.getPassword());
							conn.setAutoCommit(true);
							st = conn
									.prepareStatement(queryTemplate);
						}catch (Exception e) {
							
						}
						//s.printStackTrace();
					}
				}
				st.close();		
			}
	}
}
