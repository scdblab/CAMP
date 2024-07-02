package com.mitrallc.UnitTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


public class TestConnection {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/** The class to use as the jdbc driver. */
		String DRIVER_CLASS = "db.driver";

		/** The URL to connect to the database. */
		String CONNECTION_URL = "db.url";

		/** The user name to use to connect to the database. */
		String CONNECTION_USER = "db.user";

		/** The password to use for establishing the connection. */
		String CONNECTION_PASSWD = "db.passwd";
		
		if (args.length != 4){
			System.out.println("Usage:  TestConnection driver url user password");
			System.out.println("Example:  TestConnection com.mitrallc.sql.KosarSoloDriver kosarsolo:jdbc:mysql://127.0.0.1:3306/hibernatedb shahram password");
			return;
		}
		
		int counter = 0;
		for (String s: args) {
            System.out.println("Arg "+counter+"="+s);
            switch (counter) {
            	case 0: DRIVER_CLASS = s; break;
            	case 1: CONNECTION_URL = s; break;
            	case 2: CONNECTION_USER = s; break;
            	case 3: CONNECTION_PASSWD = s; break;
            	default: 
            		System.out.println("More than 4 input arguments?  Error, this should not have happened.  Exiting!");
            		return;
            }
            counter++;
        }
		
		System.out.println("Testing connection with driver="+DRIVER_CLASS+", connection url="+CONNECTION_URL+", user="+CONNECTION_USER+", password="+CONNECTION_PASSWD);

		try {
			if (DRIVER_CLASS != null) {
				Class.forName(DRIVER_CLASS);
			}

			Connection conn = DriverManager.getConnection(CONNECTION_URL, CONNECTION_USER, CONNECTION_PASSWD);
			if (conn != null){
				Statement st=conn.createStatement();
				st.close();
				System.out.println("Successful Test!  Your database connection is operation.  Congratulations. ");
			} else System.out.println("Error, failed to establish connection.");
		} catch (ClassNotFoundException e) {
			System.out.println("Error, failed to find the driver: " + e);
			System.out.println("Suggested fix:  Verify the jar file for the RDBMS is in the build path.");
			return;
		} catch (SQLException e) {
			System.out.println("Error in database operation: " + e);
			System.out.println("Suggested fix:  Verify the DBMS server is properly configured.  If running across the network, check firewall settings.");
			return;
		}
	}
}
