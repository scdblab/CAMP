package com.mitrallc.config;
/**
 * Contains data for connecting to the database
 * 
 * @author Shahram Ghandeharizadeh
 * 
 */
public class DBConnector {
	private static String type = "oracle";	
	//private static String connectionString = com.mitrallc.sql.KosarSoloDriver.urlPrefix+"jdbc:oracle:thin:@10.0.0.195:1521:orcl";
	private static String connectionString = "jdbc:oracle:thin:@10.0.0.195:1521:orcl";
	private static String username = "cosar";		
	private static String password = "gocosar"; 	
	private static String className = "oracle.jdbc.driver.OracleDriver";
		
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
		DBConnector.type = type;
	}
	public static void setConnectionString(String connectionString) {
		DBConnector.connectionString = com.mitrallc.sql.KosarSoloDriver.urlPrefix+connectionString;
	}
	public static void setUsername(String username) {
		DBConnector.username = username;
	}
	public static void setPassword(String password) {
		DBConnector.password = password;
	}
	public static void setClassName(String className) {
		DBConnector.className = className;
	}
}

