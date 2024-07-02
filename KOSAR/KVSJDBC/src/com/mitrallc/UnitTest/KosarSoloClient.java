package com.mitrallc.UnitTest;

import java.io.*;
import java.sql.*;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


//author Shahram Ghandeharizadeh

public class KosarSoloClient extends Thread {

	/** The class to use as the jdbc driver. */
	public static final String DRIVER_CLASS = "db.driver";

	/** The URL to connect to the database. */
	public static final String CONNECTION_URL = "db.url";

	/** The user name to use to connect to the database. */
	public static final String CONNECTION_USER = "db.user";

	/** The password to use for establishing the connection. */
	public static final String CONNECTION_PASSWD = "db.passwd";

	/** The code to return when the call succeeds. */
	public static final int SUCCESS = 0;

	/** The field name prefix in the table.*/
	public static String COLUMN_PREFIX = "FIELD";

	public static final String INSERT_IMAGE_PROPERTY = "insertimage";
	public static final String INSERT_IMAGE_PROPERTY_DEFAULT = "false";

	private ArrayList<Connection> conns;
	private boolean initialized = false;
	private static Properties props;
	private static final String DEFAULT_PROP = "";
	private ConcurrentMap<StatementType, PreparedStatement> cachedStatements;
	private PreparedStatement preparedStatement;
	private Connection conn;
	private Statement st = null;  
	private int id = 0;


	/**
	 * The statement type for the prepared statements.
	 */
	private static class StatementType {

		enum Type {
			INSERT(1),
			DELETE(2),
			READ(3),
			UPDATE(4),
			SCAN(5),
			;
			int internalType;
			private Type(int type) {
				internalType = type;
			}

			int getHashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + internalType;
				return result;
			}
		}

		Type type;
		int shardIndex;
		int numFields;
		String tableName;

		StatementType(Type type, String tableName, int numFields, int _shardIndex) {
			this.type = type;
			this.tableName = tableName;
			this.numFields = numFields;
			this.shardIndex = _shardIndex;
		}


		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + numFields + 100 * shardIndex;
			result = prime * result
					+ ((tableName == null) ? 0 : tableName.hashCode());
			result = prime * result + ((type == null) ? 0 : type.getHashCode());
			return result;
		}


		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StatementType other = (StatementType) obj;
			if (numFields != other.numFields)
				return false;
			if (shardIndex != other.shardIndex)
				return false;
			if (tableName == null) {
				if (other.tableName != null)
					return false;
			} else if (!tableName.equals(other.tableName))
				return false;
			if (type != other.type)
				return false;
			return true;
		}
	}

	/**
	 * For the given key, returns what shard contains data for this key
	 *
	 * @param key Data key to do operation on
	 * @return Shard index
	 */
	private int getShardIndexByKey(String key) {
		int ret = Math.abs(key.hashCode()) % conns.size();
		//System.out.println(conns.size() + ": Shard instance for "+ key + " (hash " + key.hashCode()+ " ) " + " is " + ret);
		return ret;
	}

	/**
	 * For the given key, returns Connection object that holds connection
	 * to the shard that contains this key
	 *
	 * @param key Data key to get information for
	 * @return Connection object
	 */
	private Connection getShardConnectionByKey(String key) {
		return conns.get(getShardIndexByKey(key));
	}

	private void cleanupAllConnections() {
		try {
			for(Connection con: conns) {
				if(con != null) con.close();
			}
			if(conn != null) conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public KosarSoloClient(Properties props, int i) {
		id = i;
		if (initialized) {
			System.out.println("Client connection already initialized.");
			return;
		}

		String urls = props.getProperty(CONNECTION_URL, DEFAULT_PROP);
		String user = props.getProperty(CONNECTION_USER, DEFAULT_PROP);
		String passwd = props.getProperty(CONNECTION_PASSWD, DEFAULT_PROP);
		String driver = props.getProperty(DRIVER_CLASS);

		try {
			if (driver != null) {
				Class.forName(driver);
			}
			int shardCount = 0;
			conns = new ArrayList<Connection>(3);
			for (String url: urls.split(",")) {
				//System.out.println("Adding shard node URL: " + url);
				conn = DriverManager.getConnection(url, user, passwd);
				// Since there is no explicit commit method in the DB interface, all
				// operations should auto commit.
				conn.setAutoCommit(true);
				shardCount++;
				conns.add(conn);  //TODO: what is this?
			}

			if (conn != null)
				st=conn.createStatement();
			else System.out.println("Error, Connection conn is null.");

			//System.out.println("Using " + shardCount + " shards");

			cachedStatements = new ConcurrentHashMap<StatementType, PreparedStatement>();
		} catch (ClassNotFoundException e) {
			System.out.println("Error in initializing the JDBC driver: " + e);
			System.out.println("KosarSoloDriver Suggested fix:  Verify the jar file for the RDBMS is in the build path.");
			return;
		} catch (SQLException e) {
			System.out.println("Error in database operation: " + e);
			System.out.println("KosarSoloDriver Suggested fix:  Verify the jar file for the RDBMS is in the build path.");
			return;
		} catch (NumberFormatException e) {
			System.out.println("Invalid value for fieldcount property. " + e);
			System.out.println("KosarSoloDriver Suggested fix:  Verify the jar file for the RDBMS is in the build path.");
			return;
		}
		initialized = true;
	}

	public void cleanup(boolean warmup) {
		System.out.println("KosarSoloClient Close Connection.");
		cleanupAllConnections();
	}

	private PreparedStatement createAndCacheInsertStatement(StatementType insertType, String key)
			throws SQLException {
		StringBuilder insert = new StringBuilder("INSERT INTO ");
		insert.append(insertType.tableName);
		insert.append(" VALUES(?");
		for (int i = 0; i < insertType.numFields; i++) {
			insert.append(",?");
		}
		//the insert is wrong it doesnt insert in the correct order of columns&&&
		// insert.append(");");
		insert.append(")");
		PreparedStatement insertStatement = getShardConnectionByKey(key).prepareStatement(insert.toString());
		PreparedStatement stmt = cachedStatements.putIfAbsent(insertType, insertStatement);
		if (stmt == null) return insertStatement;
		else return stmt;
	}

	private PreparedStatement createAndCacheReadStatement(StatementType readType, String key)
			throws SQLException {
		StringBuilder read = new StringBuilder("SELECT * FROM ");
		read.append(readType.tableName);
		read.append(" WHERE ");
		read.append("key");
		read.append(" = ");
		read.append("?");
		PreparedStatement readStatement = getShardConnectionByKey(key).prepareStatement(read.toString());
		PreparedStatement stmt = cachedStatements.putIfAbsent(readType, readStatement);
		if (stmt == null) return readStatement;
		else return stmt;
	}

	private PreparedStatement createAndCacheDeleteStatement(StatementType deleteType, String key)
			throws SQLException {
		StringBuilder delete = new StringBuilder("DELETE FROM ");
		delete.append(deleteType.tableName);
		delete.append(" WHERE ");
		delete.append("key");
		delete.append(" = ?");
		PreparedStatement deleteStatement = getShardConnectionByKey(key).prepareStatement(delete.toString());
		PreparedStatement stmt = cachedStatements.putIfAbsent(deleteType, deleteStatement);
		if (stmt == null) return deleteStatement;
		else return stmt;
	}

	private PreparedStatement createAndCacheUpdateStatement(StatementType updateType, String key)
			throws SQLException {
		StringBuilder update = new StringBuilder("UPDATE ");
		update.append(updateType.tableName);
		update.append(" SET ");
		for (int i = 1; i <= updateType.numFields; i++) {
			update.append(COLUMN_PREFIX);
			update.append(i);
			update.append("=?");
			if (i < updateType.numFields) update.append(", ");
		}
		//the update doesnt update the right column as well &&&&
		update.append(" WHERE ");
		update.append("key");
		update.append(" = ?");
		PreparedStatement insertStatement = getShardConnectionByKey(key).prepareStatement(update.toString());
		PreparedStatement stmt = cachedStatements.putIfAbsent(updateType, insertStatement);
		if (stmt == null) return insertStatement;
		else return stmt;
	}

	private PreparedStatement createAndCacheScanStatement(StatementType scanType, String key)
			throws SQLException {
		StringBuilder select = new StringBuilder("SELECT * FROM ");
		select.append(scanType.tableName);
		select.append(" WHERE ");
		select.append("key");
		select.append(" >= ");
		select.append("?");
		PreparedStatement scanStatement = getShardConnectionByKey(key).prepareStatement(select.toString());
		PreparedStatement stmt = cachedStatements.putIfAbsent(scanType, scanStatement);
		if (stmt == null) return scanStatement;
		else return stmt;
	}

	public int read(String tableName, String key, Set<String> fields,
			HashMap<String, ByteIterator> result) {

		if (tableName == null) {
			return -1;
		}
		if (key == null) {
			return -1;
		}

		try {
			StatementType type = new StatementType(StatementType.Type.READ, tableName, 1, getShardIndexByKey(key));
			PreparedStatement readStatement = cachedStatements.get(type);
			if (readStatement == null) {
				readStatement = createAndCacheReadStatement(type, key);
			}
			readStatement.setString(1, key);
			ResultSet resultSet = readStatement.executeQuery();
			if (!resultSet.next()) {
				resultSet.close();
				return 1;
			}
			if (/*result != null &&*/ fields != null) {
				for (String field : fields) {
					String value = resultSet.getString(field);
					//result.put(field, new ByteIterator(value));
				}
			}
			//added
			else{
				for (int j=0; j<10; j++){
					String value = resultSet.getString("field"+j);
					//result.put(("filed"+(j)), new StringCharacterIterator(value));
				}  
			}
			resultSet.close();
			return SUCCESS;
		} catch (SQLException e) {
			System.out.println("Error in processing read of table " + tableName + ": "+e);
			return -2;
		}
	}

	public int scan(String tableName, String startKey, int recordcount,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
		if (tableName == null) {
			return -1;
		}
		if (startKey == null) {
			return -1;
		}
		try {
			StatementType type = new StatementType(StatementType.Type.SCAN, tableName, 1, getShardIndexByKey(startKey));
			PreparedStatement scanStatement = cachedStatements.get(type);
			if (scanStatement == null) {
				scanStatement = createAndCacheScanStatement(type, startKey);
			}
			scanStatement.setString(1, startKey);
			ResultSet resultSet = scanStatement.executeQuery();
			for (int i = 0; i < recordcount && resultSet.next(); i++) {
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				if (/*result != null &&*/ fields != null) {
					for (String field : fields) {
						String value = resultSet.getString(field);
						//values.put(field, new StringByteIterator(value));
					}
					result.add(values);
				}
				else{
					for (int j=0; j<10; j++){
						String value = resultSet.getString("field"+j);
						//values.put(("filed"+(j)), new StringByteIterator(value));
					}
					result.add(values);
				}
			}
			resultSet.close();
			return SUCCESS;
		} catch (SQLException e) {
			System.out.println("Error in processing scan of table: " + tableName + e);
			return -2;
		}
	}


	public int acceptFriendRequest(int inviterID, int inviteeID) {

		int retVal = SUCCESS;
		if(inviterID < 0 || inviteeID < 0)
			return -1;
		String query;

		query = "UPDATE friendship SET status = 2 WHERE inviterid=? and inviteeid= ? ";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, inviterID);
			preparedStatement.setInt(2, inviteeID);
			preparedStatement.executeUpdate();
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}
		return retVal;		
	}


	public int update(String tableName, String key, HashMap<String, ByteIterator> values) {
		if (tableName == null) {
			return -1;
		}
		if (key == null) {
			return -1;
		}
		try {
			int numFields = values.size();
			StatementType type = new StatementType(StatementType.Type.UPDATE, tableName, numFields, getShardIndexByKey(key));
			PreparedStatement updateStatement = cachedStatements.get(type);
			if (updateStatement == null) {
				updateStatement = createAndCacheUpdateStatement(type, key);
			}
			int index = 1;
			for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
				updateStatement.setString(index++, entry.getValue().toString());
			}
			updateStatement.setString(index, key);
			int result = updateStatement.executeUpdate();
			if (result == 1) return SUCCESS;
			else return 1;
		} catch (SQLException e) {
			System.out.println("Error in processing update to table: " + tableName + e);
			return -1;
		}
	}


	public int insert(String tableName, String key, HashMap<String, ByteIterator> values, boolean insertImage, int imageSize) {
		if (tableName == null) {
			return -1;
		}
		if (key == null) {
			return -1;
		}
		ResultSet rs =null;
		try {
			String query;
			int numFields = values.size();
			//for the additional pic and tpic columns
			if(tableName.equalsIgnoreCase("users") && insertImage)
				numFields = numFields+2;
			query = "INSERT INTO "+tableName+" VALUES (";
			for(int j=0; j<=numFields; j++){
				if(j==(numFields)){
					query+="?)";
					break;
				}else
					query+="?,";
			}

			preparedStatement = getShardConnectionByKey(key).prepareStatement(query);
			preparedStatement.setString(1, key);
			int cnt=2;
			for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
				String field = entry.getValue().toString();
				preparedStatement.setString(cnt, field);
				cnt++;
			}
			if(tableName.equalsIgnoreCase("users") && insertImage){
				File image = new File("C:/BG/userpic"+imageSize+".bmp"); 
				FileInputStream fis = new FileInputStream(image);
				preparedStatement.setBinaryStream(numFields, (InputStream)fis, (int)(image.length()));
				File thumbimage = new File("C:/BG/userpic1.bmp");  //this is always the thumbnail
				FileInputStream fist = new FileInputStream(thumbimage);
				preparedStatement.setBinaryStream(numFields+1, (InputStream)fist, (int)(thumbimage.length()));
			}
			rs = preparedStatement.executeQuery();
			/*int numFields = values.size();
			StatementType type = new StatementType(StatementType.Type.INSERT, tableName, numFields, getShardIndexByKey(key));
			PreparedStatement insertStatement = cachedStatements.get(type);
			if (insertStatement == null) {
				insertStatement = createAndCacheInsertStatement(type, key);
			}
			insertStatement.setString(1, key);
			int index = 2;
			for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
				String field = entry.getValue().toString();
				insertStatement.setString(index++, field);
			}
			int result = insertStatement.executeUpdate();
			if (result == 1) return SUCCESS;
			else return 1;*/
		} catch (SQLException e) {
			System.out.println("Error in processing insert to table: " + tableName + e);
			return -1;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
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
		return 0;
	}


	public int delete(String tableName, String key) {
		if (tableName == null) {
			return -1;
		}
		if (key == null) {
			return -1;
		}
		try {
			StatementType type = new StatementType(StatementType.Type.DELETE, tableName, 1, getShardIndexByKey(key));
			PreparedStatement deleteStatement = cachedStatements.get(type);
			if (deleteStatement == null) {
				deleteStatement = createAndCacheDeleteStatement(type, key);
			}
			deleteStatement.setString(1, key);
			int result = deleteStatement.executeUpdate();
			if (result == 1) return SUCCESS;
			else return 1;
		} catch (SQLException e) {
			System.out.println("Error in processing delete to table: " + tableName + e);
			return -1;
		}
	}
	
	public int getUserProfile(int requesterID, int profileOwnerID,
			HashMap<String, ByteIterator> result, boolean insertImage, boolean testMode) {
		//if (true) return 0;
		//Statement st = null;
		ResultSet rs = null;
		int retVal = SUCCESS;
		if(requesterID < 0 || profileOwnerID < 0)
			return -1;

		String query="";


		try {
			//friend count
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel FROM users, friendship WHERE inviteeid<50 and status = 1 and inviterid = ?";
			//query = "select count(*) as col_0_0_ from HIBERNATE_USER user0_";
			//query = "select userdetail0_.userId as userId1_0_0_, userdetail0_.userName as userName2_0_0_ from UserDetails userdetail0_ where userdetail0_.userId=?";

			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID+1);
			//preparedStatement.setInt(2, profileOwnerID);
			rs = preparedStatement.executeQuery();
//			if (st == null) {
//				System.out.println("Error, st is null");
//			} else rs = st.executeQuery(query);

			if (rs.next())
				System.out.println("Retrieved userid="+rs.getInt(1)+", name="+rs.getString(4)) ;
			else
				System.out.println("No qualifying row.");


		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if (st != null)
					st.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
				retVal = -2;
			}
		}
		return retVal;
	}

	public int getUserProfile2(int requesterID, int profileOwnerID,
			HashMap<String, ByteIterator> result, boolean insertImage, boolean testMode) {
		//if (true) return 0;
		//Statement st = null;
		ResultSet rs = null;
		int retVal = SUCCESS;
		if(requesterID < 0 || profileOwnerID < 0)
			return -1;

		String query="";


		try {
			//friend count
			query = "SELECT count(*) FROM  friendship WHERE (inviterID = ? OR inviteeID = ?) AND status = 2 ";

			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			preparedStatement.setInt(2, profileOwnerID);
			rs = preparedStatement.executeQuery();

			if (rs.next())
				result.put("FriendCount", new StringByteIterator(rs.getString(1))) ;
			else
				result.put("FriendCount", new StringByteIterator("0")) ;


		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				//				if (st != null)
				//					st.close();
				//				if(preparedStatement != null)
				//					preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
				retVal = -2;
			}
		}

		//pending friend request count
		//if owner viwing her own profile, she can view her pending friend requests
		if(requesterID == profileOwnerID){
			//query = "SELECT count(*) FROM  friendship WHERE inviteeID = ? AND status = 1 ";
			try {

				query = "SELECT count(*) FROM  friendship WHERE inviteeID = ? AND status = 1 ";
				//				if (st == null)
				//					System.out.println("Error, Statement st is null.");
				//				else rs = st.executeQuery(query);
				//				if (rs == null)
				//					System.out.println("Error, result set is null.");
				//				else if (rs.next())
				//					result.put("PendingCount", new StringByteIterator(rs.getString(1))) ;
				//				else
				//					result.put("PendingCount", new StringByteIterator("0")) ;

				//				preparedStatement = conn.prepareStatement(query);
				//				preparedStatement.setInt(1, profileOwnerID);
				//				preparedStatement.setInt(2, profileOwnerID);
				//				rs = preparedStatement.executeQuery();

				preparedStatement = conn.prepareStatement(query);
				preparedStatement.setInt(1, profileOwnerID);
				rs = preparedStatement.executeQuery();
				if (rs.next())
					result.put("PendingCount", new StringByteIterator(rs.getString(1))) ;
				else
					result.put("PendingCount", new StringByteIterator("0")) ;
			}catch(SQLException sx){
				retVal = -2;
				sx.printStackTrace();
			}finally{
				try {
					if (rs != null)
						rs.close();
					if(preparedStatement != null)
						preparedStatement.close();
				} catch (SQLException e) {
					e.printStackTrace();
					retVal = -2;
				}
			}
		}
		return retVal;
	}

	public int STMTgetUserProfile(int requesterID, int profileOwnerID,
			HashMap<String, ByteIterator> result, boolean insertImage, boolean testMode) {
		//if (true) return 0;
		//Statement st = null;
		ResultSet rs = null;
		int retVal = SUCCESS;
		if(requesterID < 0 || profileOwnerID < 0)
			return -1;

		String query="";


		try {
			//friend count
			query = "SELECT count(*) FROM  friendship WHERE (inviterID = "+profileOwnerID+" OR inviteeID = "+profileOwnerID+") AND status = 2 ";
			if (st == null)
				System.out.println("Error, Statement st is null.");
			else rs = st.executeQuery(query);
			//			preparedStatement = conn.prepareStatement(query);
			//			preparedStatement.setInt(1, profileOwnerID);
			//			preparedStatement.setInt(2, profileOwnerID);
			//			rs = preparedStatement.executeQuery();
			if (rs.next())
				result.put("FriendCount", new StringByteIterator(rs.getString(1))) ;
			else
				result.put("FriendCount", new StringByteIterator("0")) ;


		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				//				if (st != null)
				//					st.close();
				//				if(preparedStatement != null)
				//					preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
				retVal = -2;
			}
		}

		//pending friend request count
		//if owner viwing her own profile, she can view her pending friend requests
		if(requesterID == profileOwnerID){
			//query = "SELECT count(*) FROM  friendship WHERE inviteeID = ? AND status = 1 ";
			try {

				query = "SELECT count(*) FROM  friendship WHERE inviteeID = "+profileOwnerID+" AND status = 1 ";
				if (st == null)
					System.out.println("Error, Statement st is null.");
				else rs = st.executeQuery(query);
				if (rs == null)
					System.out.println("Error, result set is null.");
				else if (rs.next())
					result.put("PendingCount", new StringByteIterator(rs.getString(1))) ;
				else
					result.put("PendingCount", new StringByteIterator("0")) ;

				//				preparedStatement = conn.prepareStatement(query);
				//				preparedStatement.setInt(1, profileOwnerID);
				//				preparedStatement.setInt(2, profileOwnerID);
				//				rs = preparedStatement.executeQuery();
				//				preparedStatement = conn.prepareStatement(query);
				//				preparedStatement.setInt(1, profileOwnerID);
				//				rs = preparedStatement.executeQuery();
				////				if (rs.next())
				////					result.put("PendingCount", new StringByteIterator(rs.getString(1))) ;
				////				else
				////					result.put("PendingCount", new StringByteIterator("0")) ;
			}catch(SQLException sx){
				retVal = -2;
				sx.printStackTrace();
			}finally{
				try {
					if (rs != null)
						rs.close();
					if(preparedStatement != null)
						preparedStatement.close();
				} catch (SQLException e) {
					e.printStackTrace();
					retVal = -2;
				}
			}
		}

		try {
			//resource count
			query = "SELECT count(*) FROM  resources WHERE wallUserID = "+profileOwnerID;
			if (st == null)
				System.out.println("Error, Statement st is null.");
			else rs = st.executeQuery(query);
			if (rs == null)
				System.out.println("Error, result set is null.");
			else if (rs.next())
				result.put("ResourceCount", new StringByteIterator(rs.getString(1))) ;
			else
				result.put("ResourceCount", new StringByteIterator("0")) ;

			//			preparedStatement = conn.prepareStatement(query);
			//			preparedStatement.setInt(1, profileOwnerID);
			//			rs = preparedStatement.executeQuery();
			////			if (rs.next())
			////				result.put("ResourceCount", new StringByteIterator(rs.getString(1))) ;
			////			else
			////				result.put("ResourceCount", new StringByteIterator("0")) ;
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		try {
			//profile details
			if(insertImage)
				query = "SELECT userid,username, fname, lname, gender, dob, jdate, ldate, address, email, tel, pic FROM  users WHERE UserID = "+profileOwnerID;
			else
				query = "SELECT userid,username, fname, lname, gender, dob, jdate, ldate, address, email, tel FROM  users WHERE UserID = "+profileOwnerID;
			if (st == null)
				System.out.println("Error, Statement st is null.");
			else rs = st.executeQuery(query);
			if (rs == null)
				System.out.println("Error, result set is null.");
			//			preparedStatement = conn.prepareStatement(query);
			//			preparedStatement.setInt(1, profileOwnerID);
			//			rs = preparedStatement.executeQuery();
			ResultSetMetaData md = rs.getMetaData();
			int col = md.getColumnCount();
			if(rs.next()){
				for (int i = 1; i <= col; i++){
					String col_name = md.getColumnName(i);
					String value ="";
					if(col_name.equalsIgnoreCase("pic") ){
						// Get as a BLOB
						Blob aBlob = rs.getBlob(col_name);
						byte[] allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
						value = allBytesInBlob.toString();
						//if test mode dump pic into a file
						//						if(testMode){
						//							//dump to file
						//							try{
						//								FileOutputStream fos = new FileOutputStream(profileOwnerID+"-proimage.bmp");
						//								fos.write(allBytesInBlob);
						//								fos.close();
						//							}catch(Exception ex){
						//							}
						//						}

					}else
						value = rs.getString(col_name);

					result.put(col_name, new StringByteIterator(value));
				}
			}

		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		//		result.put("PendingCount", new StringByteIterator("0")) ;
		//		result.put("ResourceCount", new StringByteIterator("0")) ;
		//		result.put("FriendCount", new StringByteIterator("0")) ;

		return retVal;
	}


	public int getUserProfileOLD(int requesterID, int profileOwnerID,
			HashMap<String, ByteIterator> result, boolean insertImage, boolean testMode) {
		//Statement st = null;
		ResultSet rs = null;
		int retVal = SUCCESS;
		if(requesterID < 0 || profileOwnerID < 0)
			return -1;

		String query="";

		requesterID = 1;
		profileOwnerID = 1;


		try {
			//friend count
			query = "SELECT count(*) FROM  friendship WHERE (inviterID = "+profileOwnerID+" OR inviteeID = "+profileOwnerID+") AND status = 2 ";
			if (st == null)
				System.out.println("Error, Statement st is null.");
			else rs = st.executeQuery(query);
			//			preparedStatement = conn.prepareStatement(query);
			//			preparedStatement.setInt(1, profileOwnerID);
			//			preparedStatement.setInt(2, profileOwnerID);
			//			rs = preparedStatement.executeQuery();
			if (rs.next())
				result.put("FriendCount", new StringByteIterator(rs.getString(1))) ;
			else
				result.put("FriendCount", new StringByteIterator("0")) ;


		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				//				if (st != null)
				//					st.close();
				//				if(preparedStatement != null)
				//					preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
				retVal = -2;
			}
		}

		//pending friend request count
		//if owner viwing her own profile, she can view her pending friend requests
		if(requesterID == profileOwnerID){
			//query = "SELECT count(*) FROM  friendship WHERE inviteeID = ? AND status = 1 ";
			try {

				query = "SELECT count(*) FROM  friendship WHERE inviteeID = "+profileOwnerID+" AND status = 1 ";
				if (st == null)
					System.out.println("Error, Statement st is null.");
				else rs = st.executeQuery(query);
				if (rs == null)
					System.out.println("Error, result set is null.");
				else if (rs.next())
					result.put("PendingCount", new StringByteIterator(rs.getString(1))) ;
				else
					result.put("PendingCount", new StringByteIterator("0")) ;

				//				preparedStatement = conn.prepareStatement(query);
				//				preparedStatement.setInt(1, profileOwnerID);
				//				preparedStatement.setInt(2, profileOwnerID);
				//				rs = preparedStatement.executeQuery();
				//				preparedStatement = conn.prepareStatement(query);
				//				preparedStatement.setInt(1, profileOwnerID);
				//				rs = preparedStatement.executeQuery();
				////				if (rs.next())
				////					result.put("PendingCount", new StringByteIterator(rs.getString(1))) ;
				////				else
				////					result.put("PendingCount", new StringByteIterator("0")) ;
			}catch(SQLException sx){
				retVal = -2;
				sx.printStackTrace();
			}finally{
				try {
					if (rs != null)
						rs.close();
					if(preparedStatement != null)
						preparedStatement.close();
				} catch (SQLException e) {
					e.printStackTrace();
					retVal = -2;
				}
			}
		}

		try {
			//resource count
			query = "SELECT count(*) FROM  resources WHERE wallUserID = "+profileOwnerID;
			if (st == null)
				System.out.println("Error, Statement st is null.");
			else rs = st.executeQuery(query);
			if (rs == null)
				System.out.println("Error, result set is null.");
			else if (rs.next())
				result.put("ResourceCount", new StringByteIterator(rs.getString(1))) ;
			else
				result.put("ResourceCount", new StringByteIterator("0")) ;

			//			preparedStatement = conn.prepareStatement(query);
			//			preparedStatement.setInt(1, profileOwnerID);
			//			rs = preparedStatement.executeQuery();
			////			if (rs.next())
			////				result.put("ResourceCount", new StringByteIterator(rs.getString(1))) ;
			////			else
			////				result.put("ResourceCount", new StringByteIterator("0")) ;
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		try {
			//profile details
			if(insertImage)
				query = "SELECT userid,username, fname, lname, gender, dob, jdate, ldate, address, email, tel, pic FROM  users WHERE UserID = "+profileOwnerID;
			else
				query = "SELECT userid,username, fname, lname, gender, dob, jdate, ldate, address, email, tel FROM  users WHERE UserID = "+profileOwnerID;
			if (st == null)
				System.out.println("Error, Statement st is null.");
			else rs = st.executeQuery(query);
			if (rs == null)
				System.out.println("Error, result set is null.");
			//			preparedStatement = conn.prepareStatement(query);
			//			preparedStatement.setInt(1, profileOwnerID);
			//			rs = preparedStatement.executeQuery();
			ResultSetMetaData md = rs.getMetaData();
			int col = md.getColumnCount();
			if(rs.next()){
				for (int i = 1; i <= col; i++){
					String col_name = md.getColumnName(i);
					String value ="";
					if(col_name.equalsIgnoreCase("pic") ){
						// Get as a BLOB
						//						Blob aBlob = rs.getBlob(col_name);
						//						byte[] allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
						//						value = allBytesInBlob.toString();
						//						//if test mode dump pic into a file
						//						if(testMode){
						//							//dump to file
						//							try{
						//								FileOutputStream fos = new FileOutputStream(profileOwnerID+"-proimage.bmp");
						//								fos.write(allBytesInBlob);
						//								fos.close();
						//							}catch(Exception ex){
						//							}
						//						}

					}else
						value = rs.getString(col_name);

					//					result.put(col_name, new StringByteIterator(value));
				}
			}

		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		//		result.put("PendingCount", new StringByteIterator("0")) ;
		//		result.put("ResourceCount", new StringByteIterator("0")) ;
		//		result.put("FriendCount", new StringByteIterator("0")) ;

		return retVal;
	}



	public int getListOfFriends(int requesterID, int profileOwnerID,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result, boolean friendListReq, boolean insertImage, boolean testMode) {

		int retVal = SUCCESS;
		ResultSet rs = null;
		if(requesterID < 0 || profileOwnerID < 0)
			return -1;

		String query ="";
		if(insertImage)
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel,tpic FROM users, friendship WHERE ((inviterid=? and userid=inviteeid) or (inviteeid=? and userid=inviterid)) and status = 2";
		else
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel FROM users, friendship WHERE ((inviterid=? and userid=inviteeid) or (inviteeid=? and userid=inviterid)) and status = 2";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			preparedStatement.setInt(2, profileOwnerID);
			rs = preparedStatement.executeQuery();
			int cnt =0;
			while (rs.next()){
				cnt++;
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				if (fields != null) {
					for (String field : fields) {
						String value = rs.getString(field);
						//						values.put(field, new StringByteIterator(value));
					}
					result.add(values);
				}else{
					//get the number of columns and their names
					//Statement st = conn.createStatement();
					//ResultSet rst = st.executeQuery("SELECT * FROM users");
					ResultSetMetaData md = rs.getMetaData();
					int col = md.getColumnCount();
					for (int i = 1; i <= col; i++){
						String col_name = md.getColumnName(i);
						String value="";
						if(col_name.equalsIgnoreCase("tpic")){
							// Get as a BLOB
							Blob aBlob = rs.getBlob(col_name);
							byte[] allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
							value = allBytesInBlob.toString();
							if(testMode){
								//dump to file
								try{
									FileOutputStream fos = new FileOutputStream(profileOwnerID+"-"+cnt+"-thumbimage.bmp");
									fos.write(allBytesInBlob);
									fos.close();
								}catch(Exception ex){
								}
							}
						}else
							value = rs.getString(col_name);


						//						values.put(col_name, new StringByteIterator(value));
					}
					result.add(values);
				}
			}
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}


	public int viewPendingRequests(int profileOwnerID,
			Vector<HashMap<String, ByteIterator>> result, boolean friendListReq, boolean insertImage, boolean testMode) {

		int retVal = SUCCESS;
		ResultSet rs = null;
		if(profileOwnerID < 0)
			return -1;

		String query = "";
		if(insertImage)
			query = "SELECT userid, inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel,tpic FROM users, friendship WHERE inviteeid=? and status = 1 and inviterid = userid";
		else 
			query = "SELECT userid,inviterid, inviteeid, username, fname, lname, gender, dob, jdate, ldate, address,email,tel FROM users, friendship WHERE inviteeid=? and status = 1 and inviterid = userid";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			rs = preparedStatement.executeQuery();
			int cnt=0;
			while (rs.next()){
				cnt++;
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				//get the number of columns and their names
				ResultSetMetaData md = rs.getMetaData();
				int col = md.getColumnCount();
				for (int i = 1; i <= col; i++){
					String col_name = md.getColumnName(i);
					String value = "";
					if(col_name.equalsIgnoreCase("tpic")){
						// Get as a BLOB
						Blob aBlob = rs.getBlob(col_name);
						byte[] allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
						value = allBytesInBlob.toString();
						if(testMode){
							//dump to file
							try{
								FileOutputStream fos = new FileOutputStream(profileOwnerID+"-"+cnt+"-thumbimage.bmp");
								fos.write(allBytesInBlob);
								fos.close();
							}catch(Exception ex){
							}

						}
					}else
						value = rs.getString(col_name);

					//values.put(col_name, new StringByteIterator(value));
				}
				result.add(values);
			}
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}


	public int rejectFriendRequest(int inviterID, int inviteeID) {
		int retVal = SUCCESS;
		if(inviterID < 0 || inviteeID < 0)
			return -1;

		//System.out.println(inviterID+" "+inviteeID);
		String query = "DELETE FROM friendship WHERE inviterid=? and inviteeid= ? and status=1";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, inviterID);
			preparedStatement.setInt(2, inviteeID);
			preparedStatement.executeUpdate();
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}
		return retVal;	
	}


	public int CreateFriendship(int memberA, int memberB) {
		int retVal = SUCCESS;
		if(memberA < 0 || memberB < 0)
			return -1;
		try {
			String DML = "INSERT INTO friendship values(?,?,2)";
			preparedStatement = conn.prepareStatement(DML);
			preparedStatement.setInt(1, memberA);
			preparedStatement.setInt(2, memberB);
			preparedStatement.executeUpdate();
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}
		return retVal;
	}


	public int inviteFriends(int inviterID, int inviteeID) {
		int retVal = SUCCESS;
		if(inviterID < 0 || inviteeID < 0)
			return -1;
		String query = "INSERT INTO friendship values(?,?,1)";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, inviterID);
			preparedStatement.setInt(2, inviteeID);
			preparedStatement.executeUpdate();
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}
		return retVal;
	}

	public int unFriendFriend(int friendid1, int friendid2){
		int retVal = SUCCESS;
		if(friendid1 < 0 || friendid2 < 0)
			return -1;

		String query = "DELETE FROM friendship WHERE (inviterid=? and inviteeid= ?) OR (inviterid=? and inviteeid= ?) and status=2";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, friendid1);
			preparedStatement.setInt(2, friendid2);
			preparedStatement.setInt(3, friendid2);
			preparedStatement.setInt(4, friendid1);

			preparedStatement.executeUpdate();
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}
		return retVal;
	}


	public int getTopKResources(int requesterID, int profileOwnerID, int k,
			Vector<HashMap<String, ByteIterator>> result) {
		int retVal = SUCCESS;
		ResultSet rs = null;
		if(profileOwnerID < 0 || requesterID < 0 || k < 0)
			return -1;

		String query = "SELECT * FROM resources WHERE walluserid = ? AND rownum <? ORDER BY rid desc";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			preparedStatement.setInt(2, (k+1));
			rs = preparedStatement.executeQuery();
			while (rs.next()){
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				//get the number of columns and their names
				ResultSetMetaData md = rs.getMetaData();
				int col = md.getColumnCount();
				for (int i = 1; i <= col; i++){
					String col_name = md.getColumnName(i);
					String value = rs.getString(col_name);
					//values.put(col_name, new StringByteIterator(value));
				}
				result.add(values);
			}
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}



	public int getCreatedResources(int resourceCreatorID, Vector<HashMap<String, ByteIterator>> result) {
		int retVal = SUCCESS;
		ResultSet rs = null;
		if(resourceCreatorID < 0)
			return -1;

		String query = "SELECT * FROM resources WHERE creatorid = ?";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, resourceCreatorID);
			rs = preparedStatement.executeQuery();
			while (rs.next()){
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				//get the number of columns and their names
				ResultSetMetaData md = rs.getMetaData();
				int col = md.getColumnCount();
				for (int i = 1; i <= col; i++){
					String col_name = md.getColumnName(i);
					String value = rs.getString(col_name);
					//values.put(col_name, new StringByteIterator(value));
				}
				result.add(values);
			}
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}



	public int getResourceComments(int requesterID, int profileOwnerID,
			int resourceID, Vector<HashMap<String, ByteIterator>> result, boolean manipulationArray) {
		int retVal = SUCCESS;
		ResultSet rs = null;
		if(profileOwnerID < 0 || requesterID < 0 || resourceID < 0)
			return -1;
		String query;
		//get comment cnt
		try {	
			query = "SELECT * FROM manipulation WHERE rid = ?";		
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, resourceID);
			rs = preparedStatement.executeQuery();
			while (rs.next()){
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				//get the number of columns and their names
				ResultSetMetaData md = rs.getMetaData();
				int col = md.getColumnCount();
				for (int i = 1; i <= col; i++){
					String col_name = md.getColumnName(i);
					String value = rs.getString(col_name);
					//values.put(col_name, new StringByteIterator(value));
				}
				result.add(values);
			}
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if (rs != null)
					rs.close();
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}


	public int postCommentOnResource(int commentCreatorID, int profileOwnerID,
			int resourceID, boolean manipulationArray) {
		int retVal = SUCCESS;

		if(profileOwnerID < 0 || commentCreatorID < 0 || resourceID < 0)
			return -1;

		String query = "INSERT INTO manipulation(creatorid, rid, modifierid, timestamp, type, content) VALUES (?,?, ?,'datehihi','post', '1234')";
		try {
			preparedStatement = conn.prepareStatement(query);
			preparedStatement.setInt(1, profileOwnerID);
			preparedStatement.setInt(2, resourceID);
			preparedStatement.setInt(3,commentCreatorID);
			preparedStatement.executeUpdate();
		}catch(SQLException sx){
			retVal = -2;
			sx.printStackTrace();
		}finally{
			try {
				if(preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace();
			}
		}

		return retVal;		
	}


	public HashMap<String, String> getInitialStats() {
		HashMap<String, String> stats = new HashMap<String, String>();
		Statement st = null;
		ResultSet rs = null;
		String query = "";
		try {
			st = conn.createStatement();
			//get user count
			query = "SELECT count(*) from users";
			rs = st.executeQuery(query);
			if(rs.next()){
				stats.put("usercount",rs.getString(1));
			}else
				stats.put("usercount","0"); //sth is wrong - schema is missing
			if(rs != null ) rs.close();
			//get user offset
			query = "SELECT min(userid) from users";
			rs = st.executeQuery(query);
			String offset = "0";
			if(rs.next()){
				offset = rs.getString(1);
			}
			//get resources per user
			query = "SELECT count(*) from resources where creatorid="+Integer.parseInt(offset);
			rs = st.executeQuery(query);
			if(rs.next()){
				stats.put("resourcesperuser",rs.getString(1));
			}else{
				stats.put("resourcesperuser","0");
			}
			if(rs != null) rs.close();	
			//get number of friends per user
			query = "select count(*) from friendship where (inviterid="+Integer.parseInt(offset) +" OR inviteeid="+Integer.parseInt(offset) +") AND status=2" ;
			rs = st.executeQuery(query);
			if(rs.next()){
				stats.put("avgfriendsperuser",rs.getString(1));
			}else
				stats.put("avgfriendsperuser","0");
			if(rs != null) rs.close();
			query = "select count(*) from friendship where (inviteeid="+Integer.parseInt(offset) +") AND status=1" ;
			rs = st.executeQuery(query);
			if(rs.next()){
				stats.put("avgpendingperuser",rs.getString(1));
			}else
				stats.put("avgpendingperuser","0");


		}catch(SQLException sx){
			sx.printStackTrace();
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
		return stats;
	}

	public int queryPendingFriendshipIds(int inviteeid, Vector<Integer> pendingIds){
		int retVal = SUCCESS;
		Statement st = null;
		ResultSet rs = null;
		String query = "";
		if(inviteeid < 0)
			retVal = -1;
		try {
			st = conn.createStatement();
			query = "SELECT inviterid from friendship where inviteeid='"+inviteeid+"' and status='1'";
			rs = st.executeQuery(query);
			while(rs.next()){
				pendingIds.add(rs.getInt(1));
			}	
		}catch(SQLException sx){
			sx.printStackTrace();
			retVal = -2;
		}finally{
			try {
				if(rs != null)
					rs.close();
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				retVal = -2;
			}
		}
		return retVal;
	}


	public int queryConfirmedFriendshipIds(int profileId, Vector<Integer> confirmedIds){
		int retVal = SUCCESS;
		Statement st = null;
		ResultSet rs = null;
		String query = "";
		if(profileId < 0)
			retVal = -1;
		try {
			st = conn.createStatement();
			query = "SELECT inviterid, inviteeid from friendship where (inviteeid="+profileId+" OR inviterid="+profileId+") and status='2'";
			rs = st.executeQuery(query);
			while(rs.next()){
				if(rs.getInt(1) != profileId)
					confirmedIds.add(rs.getInt(1));
				else
					confirmedIds.add(rs.getInt(2));
			}	
		}catch(SQLException sx){
			sx.printStackTrace();
			retVal = -2;
		}finally{
			try {
				if(rs != null)
					rs.close();
				if(st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
				retVal = -2;
			}
		}
		return retVal;

	}

	public void createSchema(Properties props){

		Statement stmt = null;

		try {
			stmt = conn.createStatement();

			dropSequence(stmt, "MIDINC");
			dropSequence(stmt, "RIDINC");
			dropSequence(stmt, "USERIDINC");
			dropSequence(stmt, "USERIDS");

			dropTable(stmt, "friendship");
			dropTable(stmt, "manipulation");
			dropTable(stmt, "resources");
			dropTable(stmt, "users");

			stmt.executeUpdate("CREATE SEQUENCE  MIDINC  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 201 CACHE 20 NOORDER  NOCYCLE");
			stmt.executeUpdate("CREATE SEQUENCE  RIDINC  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 21 CACHE 20 NOORDER  NOCYCLE ");
			stmt.executeUpdate("CREATE SEQUENCE  USERIDINC  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 21 CACHE 20 NOORDER  NOCYCLE ");
			stmt.executeUpdate("CREATE SEQUENCE  USERIDS  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE");

			stmt.executeUpdate("CREATE TABLE FRIENDSHIP"
					+ "(INVITERID NUMBER, INVITEEID NUMBER,"
					+ "STATUS NUMBER DEFAULT 1" + ") NOLOGGING");

			stmt.executeUpdate("CREATE TABLE MANIPULATION"
					+ "(	MID NUMBER," + "CREATORID NUMBER, RID NUMBER,"
					+ "MODIFIERID NUMBER, TIMESTAMP VARCHAR2(200),"
					+ "TYPE VARCHAR2(200), CONTENT VARCHAR2(200)"
					+ ") NOLOGGING");

			stmt.executeUpdate("CREATE TABLE RESOURCES"
					+ "(	RID NUMBER,CREATORID NUMBER,"
					+ "WALLUSERID NUMBER, TYPE VARCHAR2(200),"
					+ "BODY VARCHAR2(200), DOC VARCHAR2(200)"
					+ ") NOLOGGING");

			if (Boolean.parseBoolean(props.getProperty(INSERT_IMAGE_PROPERTY,
					INSERT_IMAGE_PROPERTY_DEFAULT))) {
				stmt.executeUpdate("CREATE TABLE USERS"
						+ "(USERID NUMBER, USERNAME VARCHAR2(200), "
						+ "PW VARCHAR2(200), FNAME VARCHAR2(200), "
						+ "LNAME VARCHAR2(200), GENDER VARCHAR2(200),"
						+ "DOB VARCHAR2(200),JDATE VARCHAR2(200), "
						+ "LDATE VARCHAR2(200), ADDRESS VARCHAR2(200),"
						+ "EMAIL VARCHAR2(200), TEL VARCHAR2(200), PIC BLOB, TPIC BLOB"
						+ ") NOLOGGING");
			} else {
				stmt.executeUpdate("CREATE TABLE USERS"
						+ "(USERID NUMBER, USERNAME VARCHAR2(200), "
						+ "PW VARCHAR2(200), FNAME VARCHAR2(200), "
						+ "LNAME VARCHAR2(200), GENDER VARCHAR2(200),"
						+ "DOB VARCHAR2(200),JDATE VARCHAR2(200), "
						+ "LDATE VARCHAR2(200), ADDRESS VARCHAR2(200),"
						+ "EMAIL VARCHAR2(200), TEL VARCHAR2(200)"
						+ ") NOLOGGING");

			}

			stmt.executeUpdate("ALTER TABLE USERS MODIFY (USERID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE USERS ADD CONSTRAINT USERS_PK PRIMARY KEY (USERID) ENABLE");
			stmt.executeUpdate("ALTER TABLE MANIPULATION ADD CONSTRAINT MANIPULATION_PK PRIMARY KEY (MID) ENABLE");
			stmt.executeUpdate("ALTER TABLE MANIPULATION MODIFY (MID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE MANIPULATION MODIFY (CREATORID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE MANIPULATION MODIFY (RID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE MANIPULATION MODIFY (MODIFIERID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE FRIENDSHIP ADD CONSTRAINT FRIENDSHIP_PK PRIMARY KEY (INVITERID, INVITEEID) ENABLE");
			stmt.executeUpdate("ALTER TABLE FRIENDSHIP MODIFY (INVITERID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE FRIENDSHIP MODIFY (INVITEEID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE RESOURCES ADD CONSTRAINT RESOURCES_PK PRIMARY KEY (RID) ENABLE");
			stmt.executeUpdate("ALTER TABLE RESOURCES MODIFY (RID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE RESOURCES MODIFY (CREATORID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE RESOURCES MODIFY (WALLUSERID NOT NULL ENABLE)");
			stmt.executeUpdate("ALTER TABLE FRIENDSHIP ADD CONSTRAINT FRIENDSHIP_USERS_FK1 FOREIGN KEY (INVITERID)"
					+ "REFERENCES USERS (USERID) ON DELETE CASCADE ENABLE");
			stmt.executeUpdate("ALTER TABLE FRIENDSHIP ADD CONSTRAINT FRIENDSHIP_USERS_FK2 FOREIGN KEY (INVITEEID)"
					+ "REFERENCES USERS (USERID) ON DELETE CASCADE ENABLE");
			stmt.executeUpdate("ALTER TABLE MANIPULATION ADD CONSTRAINT MANIPULATION_RESOURCES_FK1 FOREIGN KEY (RID)"
					+ "REFERENCES RESOURCES (RID) ON DELETE CASCADE ENABLE");
			stmt.executeUpdate("ALTER TABLE MANIPULATION ADD CONSTRAINT MANIPULATION_USERS_FK1 FOREIGN KEY (CREATORID)"
					+ "REFERENCES USERS (USERID) ON DELETE CASCADE ENABLE");
			stmt.executeUpdate("ALTER TABLE MANIPULATION ADD CONSTRAINT MANIPULATION_USERS_FK2 FOREIGN KEY (MODIFIERID)"
					+ "REFERENCES USERS (USERID) ON DELETE CASCADE ENABLE");
			stmt.executeUpdate("ALTER TABLE RESOURCES ADD CONSTRAINT RESOURCES_USERS_FK1 FOREIGN KEY (CREATORID)"
					+ "REFERENCES USERS (USERID) ON DELETE CASCADE ENABLE");
			stmt.executeUpdate("ALTER TABLE RESOURCES ADD CONSTRAINT RESOURCES_USERS_FK2 FOREIGN KEY (WALLUSERID)"
					+ "REFERENCES USERS (USERID) ON DELETE CASCADE ENABLE");
			stmt.executeUpdate("CREATE OR REPLACE TRIGGER MINC before insert on manipulation "
					+ "for each row "
					+ "WHEN (new.mid is null) begin "
					+ "select midInc.nextval into :new.mid from dual;"
					+ "end;");
			stmt.executeUpdate("ALTER TRIGGER MINC ENABLE");

			stmt.executeUpdate("CREATE OR REPLACE TRIGGER RINC before insert on resources "
					+ "for each row "
					+ "WHEN (new.rid is null) begin "
					+ "select ridInc.nextval into :new.rid from dual;"
					+ "end;");
			stmt.executeUpdate("ALTER TRIGGER RINC ENABLE");

			stmt.executeUpdate("CREATE OR REPLACE TRIGGER UINC before insert on users "
					+ "for each row "
					+ "WHEN (new.userid is null) begin "
					+ "select useridInc.nextval into :new.userid from dual;"
					+ "end;");
			stmt.executeUpdate("ALTER TRIGGER UINC ENABLE");

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	public void buildIndexes(Properties props){
		Statement stmt  = null;
		try {
			stmt = conn.createStatement();
			long startIdx = System.currentTimeMillis();

			dropIndex(stmt, "RESOURCE_CREATORID");
			dropIndex(stmt, "RESOURCES_WALLUSERID");
			dropIndex(stmt, "FRIENDSHIP_INVITEEID");
			dropIndex(stmt, "FRIENDSHIP_INVITERID");
			dropIndex(stmt, "MANIPULATION_RID");
			dropIndex(stmt, "MANIPULATION_CREATORID");
			stmt.executeUpdate("CREATE INDEX RESOURCE_CREATORID ON RESOURCES (CREATORID)"
					+ "COMPUTE STATISTICS NOLOGGING");

			stmt.executeUpdate("CREATE INDEX FRIENDSHIP_INVITEEID ON FRIENDSHIP (INVITEEID)"
					+ "COMPUTE STATISTICS NOLOGGING");

			stmt.executeUpdate("CREATE INDEX MANIPULATION_RID ON MANIPULATION (RID)"
					+ "COMPUTE STATISTICS NOLOGGING");

			stmt.executeUpdate("CREATE INDEX RESOURCES_WALLUSERID ON RESOURCES (WALLUSERID)"
					+ "COMPUTE STATISTICS NOLOGGING");

			stmt.executeUpdate("CREATE INDEX FRIENDSHIP_INVITERID ON FRIENDSHIP (INVITERID)"
					+ "COMPUTE STATISTICS NOLOGGING");

			stmt.executeUpdate("CREATE INDEX MANIPULATION_CREATORID ON MANIPULATION (CREATORID)"
					+ "COMPUTE STATISTICS NOLOGGING");

			stmt.executeUpdate("analyze table users compute statistics");
			stmt.executeUpdate("analyze table resources compute statistics");
			stmt.executeUpdate("analyze table friendship compute statistics");
			stmt.executeUpdate("analyze table manipulation compute statistics");
			long endIdx = System.currentTimeMillis();
			System.out
			.println("Time to build database index structures(ms):"
					+ (endIdx - startIdx));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void dropSequence(Statement st, String seqName) {
		try {
			st.executeUpdate("drop sequence " + seqName);
		} catch (SQLException e) {
		}
	}

	public static void dropIndex(Statement st, String idxName) {
		try {
			st.executeUpdate("drop index " + idxName);
		} catch (SQLException e) {
		}
	}

	public static void dropTable(Statement st, String tableName) {
		try {
			st.executeUpdate("drop table " + tableName);
		} catch (SQLException e) {
		}
	}
	
	public int inviteFriend(int inviterID, int inviteeID) {
		int retVal = SUCCESS;
		
		//System.out.println("Invoke inviteFriend with inviterID="+inviterID+" inviteeID="+inviteeID);
		
		if (inviterID < 0 || inviteeID < 0)
			return -1;
		CallableStatement proc = null;
		try {
            proc = conn.prepareCall("{ call INVITEFRIEND(?, ?) }");
			proc.setInt(1, inviterID);
		    proc.setInt(2, inviteeID);
		    proc.execute();
		}catch(SQLException sx){
			retVal = -2;
			System.out.println("inviteFriend (inviterID="+inviterID+", inviteeID"+inviteeID+") failed!");
			sx.printStackTrace(System.out);
			try {
			    Thread.sleep(1000);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			if (sx.toString().contains("Failed to connect to KOSAR KVS CORE"))
				retVal = inviteFriend(inviterID, inviteeID);
			else retVal = SUCCESS;
		}finally{
			try {
				if(proc != null)
					proc.close();
			} catch (SQLException e) {
				retVal = -2;
				e.printStackTrace(System.out);
			}
		}	
		return retVal;
	}

	public void run() {
		int numusers = 1000;
//		Vector<HashMap<String,ByteIterator>> result = new Vector<HashMap<String,ByteIterator>>();
//		Set<String> set = new HashSet<String>();
//		int retval = getListOfFriends(0,0,set, result, false, false, false);
//		System.out.println("retval " + retval);
//		//KosarSoloDriver.TRT.MySQLRegQry("SELECT * FROM FRIENDSHIP WHERE INVITERID=0");
		//KosarSoloDriver.TRT.MySQLRegQry("SELECT * FROM USERS WHERE USERID=0");
		//KosarSoloDriver.TRT.MySQLRegQry("SELECT * FROM FRIENDSHIP WHERE INVITERID=0 OR INVITEEID=0 AND STATUS=2");
		//KosarSoloDriver.TRT.MySQLRegQry("SELECT * FROM USERS WHERE USERID < 30 OR USERID > 970");

		HashMap res2 = new HashMap<String,String>();
		//		inviteFriends(35, 3);
		//getUserProfile(1,1,result, false, false);
		int res = inviteFriend(10, 50);
		long start = System.currentTimeMillis();
		for (int i=0; i < 10000; i++){
			getUserProfile(i, i, res2, false, false);
			//if(i%100==0)
			//System.out.println("retval " + i + "= " + retval1);
		}
		long diff = System.currentTimeMillis() - start;
		System.out.println("Duration: " + diff + " millis.");
		this.cleanup(false);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int NumThreads = 1;
		Thread[] t = new Thread[NumThreads];

		//Stand alone test
		Properties p = new Properties();
		
		//MySQL
		p.setProperty(CONNECTION_URL, "kosarsolo:jdbc:mysql://127.0.0.1:3306/bgbench1?autoReconnect=true");

		//Oracle
		//p.setProperty(CONNECTION_URL, "kosarsolo:jdbc:oracle:thin:cosar/gocosar@//10.0.1.20:1521/ORCL");
		p.setProperty(CONNECTION_USER, "cosar");
		p.setProperty(CONNECTION_PASSWD, "gocosar");
		p.setProperty(DRIVER_CLASS, "com.mitrallc.sql.KosarSoloDriver");
		

		

		for (int i=0; i < NumThreads; i++){
			KosarSoloClient KS = new KosarSoloClient(p,i);
			t[i] = new Thread(KS);
		}
		

		
		for (int i=0; i < NumThreads; i++) 
			t[i].start();
		
		System.out.println("KosarSoloClient:  Waiting for "+NumThreads+" to complete.");

		for (int i=0; i < NumThreads; i++){
			try {
				t[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("KosarSoloClient:  All done!");
	}

}