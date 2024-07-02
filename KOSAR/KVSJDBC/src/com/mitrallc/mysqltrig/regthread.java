package com.mitrallc.mysqltrig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import com.mitrallc.common.Constants;
import com.mitrallc.kosar.kosar;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;
import com.mitrallc.sqltrig.*;

/**
 * @author Shahram Ghandeharizadeh, Sat, July 27, 2014 This class registers
 *         triggers with a target RDBMS A key data structure is a vector that
 *         contains the queries to be registered with the RDBMS.
 */

public class regthread extends Thread {
	int counter = 0;
	public static Connection db_conn = null;
	private static boolean verbose = false;
	private static MySQLQueryToTrigger MySQLqt = null;
	private static OracleQueryToTrigger ORCLqt = null;

	private static String BeginErrMsgInit = "Error in rgthread initialization (started by KosarSolo):";

	public static Vector workitems = new Vector();

	public static Semaphore DoRegTrigg = new Semaphore(0, true);
	public static Semaphore RDBMScmds = new Semaphore(1, true);

	public static ConcurrentHashMap<String, TableInfo> TableMetaData = new ConcurrentHashMap();
	public static ConcurrentHashMap<String, String> Procs = new ConcurrentHashMap();

	public static String CurrentSQLCmd = null;

	public static boolean AddQry(String qry) {
		try {
			regthread.RDBMScmds.acquire();
			regthread.workitems.addElement(qry);
			regthread.RDBMScmds.release();
			kosar.setIssuingTrigCMDS(true); // Stop query processing

			regthread.DoRegTrigg.release();


			// regthread to do its job
			return true;
		} catch (InterruptedException e) {
			e.printStackTrace();
			kosar.setIssuingTrigCMDS(false); // continue query processing
		}
		return false;
	}

	public static void getIT(String qry, StringBuffer COSARKey){
		Vector trgrVector = new Vector();
		switch (QueryToTrigger.getTargetSystem()) {
		case MySQL:
			MySQLqt.TQ(qry, trgrVector, COSARKey, db_conn, QueryToTrigger.OpType.GETKEY);
			break;
		case Oracle:
			ORCLqt.TQ(qry, trgrVector, COSARKey, db_conn, QueryToTrigger.OpType.GETKEY);
			break;
		default:
			System.out
			.println("Error, regthread:  target unknown system.  No trigger will be generated.");
			break;
		}
		return;
	}

	public static void BusyWaitForRegThread(String cmd) {
		while (kosar.isIssuingTrigCMDS() == true) {
			if (CurrentSQLCmd != null && cmd.equals(CurrentSQLCmd))
				return;
			else
				try {
					//System.out.print("Sleep for 100 msec");
					sleep(100);
					//System.out.println("... Awakened!");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	public void UpdateWSmonitor(String cmd){
		if(KosarSoloDriver.webServer != null){
			if (cmd.contains("TRIGGER"))
				kosar.KosarMonitor.IncrementNumTriggers();
		}
	}

	public int ExecuteCommand(Connection db_conn, String updateQ) {
		Statement st;
		SockIO socket = null;
		int retval = -1;

		if (verbose)
			System.out.println("Execute Command "+ updateQ);

		try {
			if(KosarSoloDriver.getFlags().coordinatorExists()) {
				if(verbose)
					System.out.println("core exists." +
							"\nWriting to CORE " + updateQ);
				//No need to transmit DROP Trigger commands to the KVSCORE
				//The KVSCORE will generate these on its own.
				if(updateQ.contains("DROP TRIGGER")) {
					return 0;
				}

				while(true) {
					socket = KosarSoloDriver.getConnectionPool().getSock();
					if(socket == null) {
						introduceDelay();
						continue;
					}
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ByteBuffer bb = ByteBuffer.allocate(4);
					bb.putInt(7);
					baos.write(bb.array());
					bb.clear();
					baos.write(updateQ.getBytes());
					baos.flush();
					socket.write(baos.toByteArray());
					socket.flush();

					retval = socket.readByte();
					baos.reset();
					break;
				}

			}
			else {
				st = db_conn.createStatement();
				CurrentSQLCmd = updateQ; // Must specify in order to avoid a
				// deadlock with execute query
				// Open the gates temporarily to register the trigger
				st.executeUpdate(updateQ);
				CurrentSQLCmd = null;

				st.close();
				retval = 0;
			}
		} catch (SQLException e) {
			System.out
			.println("MySQLQueryToTrigger.ExecuteCommand Error: Failed to process: \n\t"
					+ updateQ + "\n");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (KosarSoloDriver.getFlags() // and it is connected
					.isCoordinatorConnected() && null != socket) {
				//This method doesn't actually close the socket.
				//It adds it back to the available sockets pool.
				socket.close();
			}
		}

		UpdateWSmonitor(updateQ);

		return retval;

	}

	public static void introduceDelay() {
		try {
			Thread.sleep(Constants.sleepTime);
		} catch (InterruptedException e) {
		}
	}

	public void MySQLRegisterExistingTriggers(Connection db_conn) {
		String triggername, tablename, triggertype, triggertiming, triggerstmt;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out
				.println("regthread, querying mysql for the triggers.");
			st = db_conn.createStatement();

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("show triggers");
			while (rs.next()) {
				triggername = rs.getString("Trigger");
				tablename = rs.getString("Table").toUpperCase();
				triggertype = rs.getString("Timing");
				triggerstmt = rs.getString("Statement");
				triggertiming = rs.getString("Event");

				if (verbose) {
					System.out.println("Trig Name=" + triggername);
					System.out.println("Table=" + tablename);
					System.out.println("Statement=" + triggerstmt);
					System.out.println("Timing=" + triggertype);
					System.out.println("Event=" + triggertiming);
				}
				String trigger = mysqlTrigGenerator.AssembleFromDBMS(
						triggername, tablename, triggertype, triggertiming,
						triggerstmt);

				if (verbose)
					System.out.println("Trigger " + trigger);
				TableInfo tbl = TableMetaData.get(tablename);
				if (tbl == null) {
					if (verbose)
						System.out
						.println("tbl entry does not exist; creating!");
					tbl = new TableInfo();
					tbl.setTableName(tablename); // add the table name for this
					// entry

					// Now, insert the tbl entry for future lookup
					TableMetaData.put(tablename, tbl);
				}
				tbl.addVRegTrig(trigger);
			}
		} catch (SQLException e) {
			System.out
			.println("MySQLQueryToTrigger.MySQLPopulate Error: Failed to process: show triggers\n");
			e.printStackTrace();
		} finally {
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in RegisterExistingTriggers method:  Failed to close Statement/ResultSet");
					e.printStackTrace();
				}

		}
	}

	public void MySQLRegisterExistingProcs(Connection db_conn) {
		String procname, procbody, dbname;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out
				.println("regthread, querying mysql for the procedures.");
			st = db_conn.createStatement();
			//dbname = db_conn.getCatalog();
			//System.out.println("dbname = "+dbname);

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("select db, name, body from mysql.proc where Db = DATABASE();");
			while (rs.next()) {
				procname = rs.getString("Name").trim();
				procbody = rs.getString("body").trim();

				if (verbose) {
					System.out.println("Proc Name=" + procname);
				}

				Procs.put(procname, procbody);
			}
		} catch (SQLException e) {
			System.out
			.println("MySQLRegisterExistingProcs Error: Failed to process: show procedure status\n");
			e.printStackTrace();
		} finally 
		{
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in MySQLRegisterExistingProcs method:  Failed to close Statement/ResultSet");
					e.printStackTrace();
				}
		}
	}

	public void MySQLPopulate(Connection db_conn) {
		MySQLRegisterExistingProcs(db_conn);
		MySQLRegisterExistingTriggers(db_conn);
	}

	public void OracleRegisterExistingTriggers(Connection db_conn) {
		String triggername, tablename, triggertype, triggertiming, triggerstmt;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out
				.println("regthread, querying mysql for the triggers.");
			st = db_conn.createStatement();

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("select * from USER_TRIGGERS");
			while (rs.next()) {
				triggername = rs.getString("TRIGGER_NAME");
				tablename = rs.getString("TABLE_NAME").toUpperCase();
				triggertype = rs.getString("TRIGGER_TYPE");
				triggerstmt = rs.getString("TRIGGER_BODY");
				triggertiming = rs.getString("TRIGGERING_EVENT");

				if (verbose) {
					System.out.println("Trig Name=" + triggername);
					System.out.println("Table=" + tablename);
					System.out.println("Statement=" + triggerstmt);
					System.out.println("Timing=" + triggertype);
					System.out.println("Event=" + triggertiming);
				}
				//Kosar Triggers do not have a SELECT statement
				//Do not incorporate these as they are specific to an application
				if ( !triggerstmt.toUpperCase().contains("SELECT") ) {
					String trigger = OracleTrigGenerator.AssembleFromDBMS(
							triggername, tablename, triggertype, triggertiming,
							triggerstmt);

					if (verbose)
						System.out.println("Trigger " + trigger);
					TableInfo tbl = TableMetaData.get(tablename);
					if (tbl == null) {
						if (verbose)
							System.out
							.println("tbl entry does not exist; creating!");
						tbl = new TableInfo();
						tbl.setTableName(tablename); // add the table name for this
						// entry

						// Now, insert the tbl entry for future lookup
						TableMetaData.put(tablename, tbl);
					}
					tbl.addVRegTrig(trigger);
				}
			}
		} catch (SQLException e) {
			System.out
			.println("MySQLQueryToTrigger.MySQLPopulate Error: Failed to process: show triggers\n");
			e.printStackTrace();
		} finally {
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in RegisterExistingTriggers method:  Failed to close Statement/ResultSet");
					e.printStackTrace();
				}

		}
	}

	public void OracleRegisterExistingProcs(Connection db_conn) {
		String procname;
		Statement st = null;
		ResultSet rs = null;
		try {
			if (verbose)
				System.out
				.println("regthread, querying mysql for the procedures.");
			st = db_conn.createStatement();

			// Open the gates temporarily to register the trigger
			rs = st.executeQuery("SELECT * FROM ALL_OBJECTS WHERE OBJECT_TYPE IN ('PROCEDURE')");
			while (rs.next()) {
				procname = rs.getString("OBJECT_NAME").trim();

				if (verbose) 
					System.out.println("Proc Name=" + procname);

				Procs.put(procname, "Exists");
			}
		} catch (SQLException e) {
			System.out
			.println("OracleQueryToTrigger.OraclePopulate Error: Failed to process SQL command to display existing procedures\n");
			e.printStackTrace();
		} finally 
		{
			// Close the connections
			if (rs != null)
				try {
					rs.close();
					if (st != null)
						st.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out
					.println("regthread, during initialization in RegisterExistingTriggers method:  Failed to close Statement/ResultSet");
					e.printStackTrace();
				}
		}
	}

	public void OraclePopulate(Connection db_conn) {
		OracleRegisterExistingProcs(db_conn);
		OracleRegisterExistingTriggers(db_conn);
	}

	public boolean MySQLRegProc(String cmdToProcess) {
		if (verbose)
			System.out.println("Register Procedure"+ cmdToProcess);
		String procname = mysqlTrigGenerator.GetProcName(cmdToProcess).trim();
		// If the proc exists then report error
		if (Procs.get(procname) != null){
			System.out.println("\nregthread, error, procedure "+procname+" already exists");
			System.out.println("\t Existing procedure: "+Procs.get(procname));
			System.out.println("\t New procedure being authored: "+cmdToProcess);
		}
		else {
			ExecuteCommand(db_conn, cmdToProcess);
			Procs.put(procname, "Exists");
		}
		return true;
	}

	public boolean MySQLRegTrig(String cmd) {
		//System.out.println("------------------------------");
		//System.out.println("STARTING REGTRIG FOR " + cmd);
		MySQLQueryToTrigger.TriggerType triggertype;
		String TblName, TriggerName, ExistTrigger;
		TableInfo tbl;

		// 1. If the trigger exists then skip registering
		// 2. Otherwise, if the same type of trigger exists then
		// merge it with the existing trigger, drop the existing trigger and
		// register the new one

		triggertype = mysqlTrigGenerator.WhatIsTriggerType(cmd);
		TblName = mysqlTrigGenerator.TableName(cmd, triggertype);

		// Invalid trigger - it must have a tablename.
		if (TblName == null)
			return false;

		// If so, create a “DROP IF EXISTS” command with the name of the
		// registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// see if the trigger type for the command exists
		// if it exists then create a “DROP IF EXISTS” command with the
		// name of the registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// if it does not exists proceed as before.
		tbl = TableMetaData.get(TblName);
		if (tbl == null) {
			if(verbose)
				System.out.println("tbl entry does not exist; creating!");
			tbl = new TableInfo();
			tbl.setTableName(TblName); // add the table name for this
			// entry

			// Now, insert the tbl entry for future lookup
			TableMetaData.put(TblName, tbl);
		}

		ExistTrigger = tbl.triggerTypeExists(cmd);

		//If the existing trigger is null then register command and return success
		if (ExistTrigger == null){
			if (ExecuteCommand(db_conn, cmd) == 0)
				tbl.addVRegTrig(cmd); // add the trigger only after it is registered with the DBMS
		}

		//If the existing trigger is the same as the incoming cmd then do nothing
		if (ExistTrigger != null
				&& mysqlTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)) {
			// No need to do anything; we are done with this incoming
			// trigger
		} else {
			if (ExistTrigger != null) {
				if(verbose) {
					System.out.println("ExistTriger "+ExistTrigger);
					System.out.println("cmd "+cmd);
				}
				// Now, merge the new trigger with the existing one
				Vector<String> inV = new Vector<String>();
				inV.add(ExistTrigger);
				inV.add(cmd);
				Vector<String> outV = new Vector<String>();
				MySQLOptimizeTriggers.Optimize(inV, outV, 2);
				cmd = outV.elementAt(0);
				//If the resulting trigger is the same as existing then there is nothing to be done
				if ( mysqlTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)){
					//Do nothing, the existing trigger is a superset
				} else {
					// Generate the trigger drop command and merge with the
					// incoming trigger
					TriggerName = mysqlTrigGenerator.GetTriggerName(ExistTrigger);
					String dropcommand = ("DROP TRIGGER IF EXISTS " + TriggerName);
					ExecuteCommand(db_conn, dropcommand);
					tbl.trigremove(ExistTrigger);

					if (ExecuteCommand(db_conn, cmd) == 0)
						tbl.addVRegTrig(cmd); // add the trigger only after it is
					// registered with the DBMS
				}
			}
		}
		return true;
	}

	// This should change to process an object of type query
	public boolean MySQLRegQry(String qry) {
		boolean res = true;
		Vector<String> key = new Vector<String>();
		Vector<String> ProcsTrigs = new Vector<String>();
		Vector<String> trgs = new Vector<String>();
		String cmd;

		// Generate triggers
		String QueryTemplate = MySQLqt.TransformQuery(qry, ProcsTrigs, key,
				db_conn);

		if (QueryTemplate == null) {
			// Query template has been deactivated. Do not register its
			// triggers.
			// This should not happen
			System.out.println("WARNING regthread::MySQLRegQry;  A null query tamplate indicating it has been deactived.  This should not happen.");
			System.out.println("Qry "+qry);
			return false;
		}

		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache
				.get(QueryTemplate);
		if (qtelt.isTriggersRegistered()) {
			if (verbose)
				System.out
				.println("regthread:  query template already registered.");
			if (verbose)
				System.out.println("\t" + QueryTemplate);
			return true;
		}

		// Prior to registering triggers, stop both queries and DML commands
		// from being issued to the server
		// because triggers are being dropped and added. No update should sneak
		// in between.
		// This is realized by stopping query/DML processing.
		kosar.setIssuingTrigCMDS(true);

		// Process procedures first and construct a vector of triggers
		for (int i = 0; i < ProcsTrigs.size(); i++) {
			String cmdToProcess = ProcsTrigs.elementAt(i).toString();
			if (cmdToProcess.indexOf(mysqlTrigGenerator.StartProc) >= 0) {
				MySQLRegProc(cmdToProcess);
			} else
				trgs.addElement(cmdToProcess); // Must be a trigger, save for
			// processing
		}

		// Iterate triggers and register them
		for (int i = 0; i < trgs.size(); i++) {
			cmd = trgs.elementAt(i).toString();
			MySQLRegTrig(cmd);
		}

		// Enable query processing asap
		kosar.setIssuingTrigCMDS(false);

		// Set the query template to indicate all its triggers have been
		// registered
		qtelt.setTg(trgs);
		qtelt.setTriggersRegistered(true);

		return res;
	}

	public boolean OracleRegProc(String cmdToProcess){
		String procname = mysqlTrigGenerator.GetProcName(cmdToProcess)
				.trim();
		// If the proc exists then report error
		if (Procs.get(procname) != null)
			System.out
			.println("regthread, error, procedure already exists");
		else {
			ExecuteCommand(db_conn, cmdToProcess);
			Procs.put(procname, "Exists");
		}
		return true;
	}

	public boolean OracleRegTrig(String cmd){
		QueryToTrigger.TriggerType triggertype;
		String TblName, ExistTrigger;
		TableInfo tbl;

		// 1. If the trigger exists then skip registering
		// 2. Otherwise, if the same type of trigger exists then
		// merge it with the existing trigger, drop the existing trigger and
		// register the new one

		triggertype = OracleTrigGenerator.WhatIsTriggerType(cmd);
		TblName = OracleTrigGenerator.TableName(cmd, triggertype);

		//Invalid trigger - it must have a tablename.
		if (TblName == null) return false;

		// If so, create a “DROP IF EXISTS” command with the name of the
		// registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// see if the trigger type for the command exists
		// if it exists then create a “DROP IF EXISTS” command with the
		// name of the registered trigger and
		// execute it against the RDBMS. Next, remove this trigger from
		// VRegTrig.
		// if it does not exists proceed as before.
		tbl = TableMetaData.get(TblName);
		if (tbl == null) {
			System.out.println("tbl entry does not exist; creating!");
			tbl = new TableInfo();
			tbl.setTableName(TblName); // add the table name for this
			// entry

			// Now, insert the tbl entry for future lookup
			TableMetaData.put(TblName, tbl);
		}

		ExistTrigger = tbl.triggerTypeExists(cmd);


		//If the existing trigger is null then register command and return success
		if (ExistTrigger == null){
			if (ExecuteCommand(db_conn, cmd) == 0)
				tbl.addVRegTrig(cmd); // add the trigger only after it is registered with the DBMS
		}

		//If the existing trigger is the same as the incoming cmd then do nothing
		if (ExistTrigger != null
				&& OracleTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)) {
			// No need to do anything; we are done with this incoming
			// trigger
		} else {
			if (ExistTrigger != null) {
				// Now, merge the new trigger with the existing one
				Vector<String> inV = new Vector<String>();
				inV.add(ExistTrigger);
				inV.add(cmd);
				Vector<String> outV = new Vector<String>();
				System.out.println(""+cmd);
				System.out.println(""+ExistTrigger);
				OracleOptimizeTriggers.Optimize(inV, outV, 2);
				cmd = outV.elementAt(0);
				//If the resulting trigger is the same as existing then there is nothing to be done
				if ( OracleTrigGenerator.AreTriggersEqual(ExistTrigger, cmd)){
					//Do nothing, the existing trigger is a superset
				} else {
					if (ExecuteCommand(db_conn, cmd) == 0){
						tbl.trigremove(ExistTrigger);
						tbl.addVRegTrig(cmd); // add the trigger only after it is
						// registered with the DBMS
					}
				}
			}
		}
		return true;
	}

	public boolean OracleRegQry(String qry) {
		boolean res = true;
		Vector<String> key = new Vector<String>();
		Vector<String> ProcsTrigs = new Vector<String>();
		Vector<String> trgs = new Vector<String>();
		String cmd;

		// Generate triggers
		String QueryTemplate = ORCLqt.TransformQuery(qry, ProcsTrigs, key, db_conn);

		if (QueryTemplate == null) {
			// Query template has been deactivated. Do not register its
			// triggers.
			// This should not happen
			System.out.println("WARNINING regthread::OracleRegQry;  A null query tamplate indicating it has been deactived.  This should not happen.");
			System.out.println("Qry "+qry);
			return false;
		}

		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache
				.get(QueryTemplate);
		if (qtelt.isTriggersRegistered()) {
			if (verbose)
				System.out
				.println("regthread:  query template already registered.");
			if (verbose)
				System.out.println("\t" + QueryTemplate);
			return true;
		}
		// Prior to registering triggers, stop both queries and DML commands
		// from being issued to the server
		// because triggers are being dropped and added. No update should sneak
		// in between.
		// This is realized by stopping query/DML processing.
		kosar.setIssuingTrigCMDS(true);

		// Process procedures first and construct a vector of triggers
		for (int i = 0; i < ProcsTrigs.size(); i++) {
			String cmdToProcess = ProcsTrigs.elementAt(i).toString();
			if (cmdToProcess.indexOf(OracleTrigGenerator.StartProc) >= 0) {
				OracleRegProc(cmdToProcess);
			} else
				trgs.addElement(cmdToProcess); // Must be a trigger, save for
			// processing
		}

		// Iterate triggers and register
		for (int i = 0; i < trgs.size(); i++) {
			cmd = trgs.elementAt(i).toString();
			OracleRegTrig(cmd);
		}

		// Enable query processing asap
		kosar.setIssuingTrigCMDS(false);

		// Set the query template to indicate all its triggers have been
		// registered
		qtelt.setTg(trgs);
		qtelt.setTriggersRegistered(true);

		return res;
	}

	public void run() {

		MySQLQueryToTrigger.TriggerType triggertype;
		String TblName;
		String TriggerName;
		String qry = "";

		while (true) {
			try {
				DoRegTrigg.acquire();
				RDBMScmds.acquire();
				qry = workitems.remove(0).toString();
				RDBMScmds.release();

				if (verbose)
					System.out.println("Register qry "+qry);

				switch (QueryToTrigger.getTargetSystem()) {
				case MySQL:
					MySQLRegQry(qry);
					break;
				case Oracle:
					OracleRegQry(qry);
					break;
				default:
					break;
				}
			} catch (InterruptedException e) {
				System.out.println("Got an exception when registering the query");
				e.printStackTrace();
			}
			if (verbose)
				System.out.println(counter++);

			// Enable query processing
			kosar.setIssuingTrigCMDS(false);
			if (verbose) System.out.println("After Exception, flag is "+kosar.isIssuingTrigCMDS());
		}
	}

	public regthread(Connection con1) {
		if (verbose)
			System.out.println("Starting the Trigger Registeration Thread.");
		if (con1 == null) {
			System.out.println("" + BeginErrMsgInit
					+ " Input JDBC connection is null.");
			System.out
			.println("Continuing execution without trigger registeration:  Expect stale data.");
			return;
		}

		// Get a connection
		db_conn = con1;

		// Instantiate the appropriate translator for a target RDBMS
		switch (QueryToTrigger.getTargetSystem()) {
		case MySQL:
			MySQLqt = new MySQLQueryToTrigger();
			MySQLPopulate(db_conn);
			break;
		case Oracle:
			ORCLqt = new OracleQueryToTrigger();
			OraclePopulate(db_conn);
			break;
		default:
			System.out
			.println("Error, regthread:  target unknown system.  No trigger will be generated.");
			break;
		}
	}
}
