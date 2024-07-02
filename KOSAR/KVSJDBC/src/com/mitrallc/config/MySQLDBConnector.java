package com.mitrallc.config;
/**
 * Contains data for connecting to the database
 * 
 * @author Shahram Ghandeharizadeh
 * 
 */
public class MySQLDBConnector {
	private static String type = "mysql";	
	//private static String connectionString = com.mitrallc.sql.KosarSoloDriver.urlPrefix+"jdbc:mysql://127.0.0.1:3306/bgbch1";
	private static String connectionString = "jdbc:mysql://127.0.0.1:3306/bgbch1";
	private static String username = "cosar";		
	private static String password = "gocosar"; 	
	private static String className = "com.mysql.jdbc.Driver";
		
	public static String getConnectionString()
	{
		return connectionString;
	}
	public static String getUsername()
	{
		return username;
	}
	public static String getPassword()
	{
		return password;
	}
	public static String getClassName()
	{
		return className;
	}
	public static String getType() {
		return type;
	}
	
	
	public static void setType(String type) {
		MySQLDBConnector.type = type;
	}
	public static void setConnectionString(String connectionString) {
		MySQLDBConnector.connectionString = com.mitrallc.sql.KosarSoloDriver.urlPrefix+connectionString;
	}
	public static void setUsername(String username) {
		MySQLDBConnector.username = username;
	}
	public static void setPassword(String password) {
		MySQLDBConnector.password = password;
	}
	public static void setClassName(String className) {
		MySQLDBConnector.className = className;
	}
}

