package com.mitrallc.sqltrig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class OracleDBAdapter {
	//public static String schema = "COSAR";
	private static boolean verbose = false;
	private static ConcurrentHashMap<String,String> pkey = new ConcurrentHashMap();
	private static ConcurrentHashMap<String,Vector> TableAttrs = new ConcurrentHashMap();
	private static ConcurrentHashMap<String,Integer> AttrType = new ConcurrentHashMap();
	private static ConcurrentHashMap<String,String> AttrTypeString = new ConcurrentHashMap();
	private static ConcurrentHashMap<String,String> ITQ = new ConcurrentHashMap();
	
	private static Logger logger = Logger.getLogger(OracleDBAdapter.class.getName());

	private static ConcurrentHashMap<String, String> TablesInDB = new ConcurrentHashMap(); //This data structure maintains the known tables

	public static String GetAttrType(String tblname, String attrname, Connection db_conn)
	{
		String result = "";
		String qry = "SELECT data_type FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '"+tblname+"' and column_name='"+attrname+"';";
		return result;
	}
	
	public static boolean DoesTableExist(String tablename, Connection db_conn)
	{
	     boolean tblexists = false;
	     String tbl = tablename.toUpperCase().trim();
	     ResultSet rs=null;
	     if (TablesInDB.containsKey(tbl)) {
	    	// System.out.println(TablesInDB.keySet());
	    	 return true;	 
	     }
	     else {
	    	 
	    	 //check the database to see if the table exists and if so insert it in the TablesInDB hash table
	        try{
	           DatabaseMetaData odm = db_conn.getMetaData();
	           rs = odm.getColumns(null, null, tbl, null);
	           if (rs.next())
	           {
	                tblexists = true;
	                TablesInDB.put(tbl,"exists");
	               
	           }
	          
	          
	        } catch (Exception e) {
	           e.printStackTrace();
	        }
	        finally {
	            try {
	                if (rs!=null) 
	                	rs.close();
	            } catch (Exception e) {
	                System.out.println("Error in GetPrimaryKey method of OracleDBAdapter:  Failed to close the result set.");
	                e.printStackTrace();
	            }
	        }
	     }
	     System.out.println(TablesInDB.keySet());
	     return tblexists;
	}

	
	
	public static String TokenizeAttr(Vector proj, Vector V){
		String tk = "";
		if (proj == null || proj.size()==0) tk = "n";
		else{
			for (int i=0; i<proj.size(); i++)
			tk += proj.elementAt(i).toString().trim().substring(0,1);
		}
		switch (V.size()%10){
		case 1: tk += "A"+V.size(); break;
		case 2: tk += "B"+V.size(); break;
		case 3: tk += "C"+V.size(); break;
		case 4: tk += "D"+V.size(); break;
		case 5: tk += "E"+V.size(); break;
		case 6: tk += "F"+V.size(); break;
		case 7: tk += "G"+V.size(); break;
		case 8: tk += "H"+V.size(); break;
		case 9: tk += "I"+V.size(); break;
		default: tk += "J"+V.size(); System.out.println("Error in TokenizeAttr; failed to generate token.\n"); break;
		}
		return tk;
	}
	
	public static boolean RegisterITQ(String IT, String Q)
	{
		String query;
		if (IT.indexOf("(") >= 0 || IT.indexOf(")") >=0){
			logger.debug("Error, IT is unacceptable and will break the trigger.");
			logger.debug("\t IT: "+IT);
			logger.debug("\t qry: "+Q);
		}
		if (ITQ.get(IT) != null){
			query =  ITQ.get(IT);

			if (!query.equals(Q)){
				logger.debug("Error, Internal Token "+IT+" is associated with two different queries");
				logger.debug("\t Query 1: "+query);
				logger.debug("\t Query 2: "+Q+"\n");
				return false;
			}
		} else ITQ.put(IT, Q);
		return true;
	}
	
	public static int GetColumnType(Connection db_conn, String tablename, String columnname)
	{
		boolean results = true;
		ResultSet rs = null;
		Integer TPO = null;
		int tp = -1;
		
		String tbl = tablename.toUpperCase().trim();
		String clmn = columnname.toUpperCase().trim();
		//If the cache lookup succeeds then just return
		String key = tbl+clmn;

		if (AttrType.get(key) != null)
				TPO =  AttrType.get(key);
		if (TPO != null) {
			return TPO.intValue();
		}
		
		try {
			if (verbose) System.out.println("Catalog name is "+db_conn.getCatalog());
		     
			DatabaseMetaData odm = db_conn.getMetaData();
			rs = odm.getColumns(null, null, tbl, null);

			while (rs.next())
			{	
				if (rs.getString("COLUMN_NAME").toUpperCase().equals(clmn)){
					tp = rs.getInt("DATA_TYPE");
					break;
				}
				//System.out.println(""+rs.getString(4));
			}
			AttrType.put(key, tp);
		} catch(Exception e){
			//if (rs!=null) rs.close();
			e.printStackTrace();
			results = false;
		}
		finally {
			try {
				if (rs!=null) rs.close();
			} catch (Exception e) {
				System.out.println("Error in GetPrimaryKey method of OracleDBAdapter:  Failed to close the result set.");
				e.printStackTrace();
			}
		}
		return tp;
	}
	
	public static String GetColumnTypeAsString(Connection db_conn, String tablename, String columnname)
	{
		boolean results = true;
		ResultSet rs = null;
		String TPO = null;
		String tp = "";
		
		String tbl = tablename.toUpperCase().trim();
		String clmn = columnname.toUpperCase().trim();
		//If the cache lookup succeeds then just return
		String key = tbl+clmn;

		if (AttrTypeString.get(key) != null)
				TPO =  AttrTypeString.get(key);
		if (TPO != null) {
			return TPO;
		}
		
		try {
			if (verbose) System.out.println("Catalog name is "+db_conn.getCatalog());
		     
			DatabaseMetaData odm = db_conn.getMetaData();
			rs = odm.getColumns(null, null, tbl, null);

			while (rs.next())
			{	
				if (rs.getString("COLUMN_NAME").toUpperCase().equals(clmn)){
					tp = rs.getString("TYPE_NAME");
					break;
				}
				//System.out.println(""+rs.getString(4));
			}
			AttrTypeString.put(key, tp);
		} catch(Exception e){
			//if (rs!=null) rs.close();
			e.printStackTrace();
			results = false;
		}
		finally {
			try {
				if (rs!=null) rs.close();
			} catch (Exception e) {
				System.out.println("Error in GetPrimaryKey method of OracleDBAdapter:  Failed to close the result set.");
				e.printStackTrace();
			}
		}
		return tp;
	}
	
	public static Vector GetColumnNames(Connection db_conn, String tablename)
	{

		String tbl = tablename.toUpperCase();
		//If the cache lookup succeeds then just return
		Vector lookup = null;
		if (TableAttrs.get(tbl) != null)
				lookup = (Vector) TableAttrs.get(tbl);
		if (lookup != null) {
			return lookup;
		}
		
		boolean results = true;
		ResultSet rs = null;
		Vector V = null;
		if (db_conn == null) {
			System.out.println("Error in GetPrimaryKey (OracleDBAdapter class):  connection is null");
			return V;
		}
		try {
//			String ctlname = db_conn.getCatalog();
//			System.out.println("\nctlname="+ctlname+"\n");
			if (verbose) System.out.println("Catalog name is "+db_conn.getCatalog());
		     
			DatabaseMetaData odm = db_conn.getMetaData();
			rs = odm.getColumns(null, null, tbl, null);
			V = new Vector();
			while (rs.next())
			{	
				V.addElement(rs.getString("COLUMN_NAME"));
				//System.out.println(""+rs.getString(4));
			}
			TableAttrs.put(tbl, V);
		} catch(Exception e){
			//if (rs!=null) rs.close();
			e.printStackTrace();
			results = false;
		}
		finally {
			try {
				if (rs!=null) rs.close();
			} catch (Exception e) {
				System.out.println("Error in GetPrimaryKey method of OracleDBAdapter:  Failed to close the result set.");
				e.printStackTrace();
			}
		}
		return V;
	}
	
	public static int GetPrimaryKey(Connection db_conn, String tablename, StringBuffer pk)
	{
		int clmn = 0;
		String tbl = tablename.toUpperCase();
		//If the cache lookup succeeds then just return
		String lookup = null;
		if (pkey.get(tbl) != null)
				lookup = pkey.get(tbl).toString();
		if (lookup != null && lookup.length() > 0) {
			pk.append(lookup);
			String[] terms = lookup.trim().split(",");
			return terms.length;
		}

		boolean results = true;
		ResultSet rs = null;
		if (db_conn == null) {
			System.out.println("Error in GetPrimaryKey (OracleDBAdapter class):  connection is null");
			return -1;
		}
		
		try {
			if (verbose) System.out.println("Catalog name is "+db_conn.getCatalog());
		     
			DatabaseMetaData odm = db_conn.getMetaData();
			rs = odm.getPrimaryKeys(db_conn.getCatalog(), null, tbl);
			HashMap<String,String> dups = new HashMap();
			String KeyColName;
			while (rs.next())
			{	
				KeyColName = rs.getString("COLUMN_NAME");
				if (!dups.containsKey(KeyColName)){
					if (pk.length() > 0) pk.append(",");
					pk.append(""+KeyColName);
					dups.put(KeyColName, "1");
					clmn ++;
				}
				//System.out.println(""+rs.getString(4));
			}
			pkey.put(tbl, pk.toString());
			//rs.close();
		} catch(Exception e){
			//if (rs!=null) rs.close();
			e.printStackTrace();
			results = false;
			clmn = -1;
		}
		finally {
			try {
				if (rs!=null) rs.close();
			} catch (Exception e) {
				System.out.println("Error in GetPrimaryKey method of OracleDBAdapter:  Failed to close the result set.");
				e.printStackTrace();
			}
		}
		
		return clmn;
	}

}
