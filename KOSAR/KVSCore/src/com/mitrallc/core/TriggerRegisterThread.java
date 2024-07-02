package com.mitrallc.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import com.mitrallc.kosar.kosar;
import com.mitrallc.mysqltrig.MySQLQueryToTrigger;
import com.mitrallc.mysqltrig.mysqlTrigGenerator;
import com.mitrallc.mysqltrig.regthread;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;
import com.mitrallc.sqltrig.OracleTrigGenerator;
import com.mitrallc.sqltrig.QueryToTrigger;
import com.mitrallc.util.SocketIO;

public class TriggerRegisterThread extends regthread {

	boolean verbose = true;
	
	public TriggerRegisterThread(Connection conn) {
		super(conn);
	}
	
	@Override
	public int ExecuteCommand(Connection db_conn, String updateQ) {
		Statement st;
		int retval = -1;
		try {
			if(verbose)System.out.println("EXECUTING COMMAND " + updateQ);
			st = db_conn.createStatement();
			CurrentSQLCmd = updateQ; // Must specify in order to avoid a
			// deadlock with execute query
			// Open the gates temporarily to register the trigger
			st.executeUpdate(updateQ);
			CurrentSQLCmd = null;

			st.close();
			retval = 0;
		
		} catch (SQLException e) {
			System.out
					.println("MySQLQueryToTrigger.ExecuteCommand Error: Failed to process: \n\t"
							+ updateQ + "\n");
			e.printStackTrace();
		} 
		return retval;

	}
	
	public static boolean AddTrig(SocketIO socket, byte[] request, byte[] clientID) {
		String trig = "";
		
		try {
			trig = new String(request, "UTF-8");
			
			regthread.RDBMScmds.acquire();
			
			/*if ((item = freeTriggerItemQueue.poll()) != null) {
				item.setSocket(socket);
				item.setTrigger(cmd);
			} else*/
			
			TriggerItem item = new TriggerItem(socket, trig, clientID);
			regthread.workitems.addElement(item);
			regthread.RDBMScmds.release();
			regthread.DoRegTrigg.release();

			kosar.setIssuingTrigCMDS(true); // Stop query processing for the
											// regthread to do its job
			return true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}
	public void interrupt() {
		this.stop();
	}
	public void run() {
		boolean retval = false;
		MySQLQueryToTrigger.TriggerType triggertype;
		String TblName;
		String TriggerName;
		TriggerItem triggerItem = null;

		while (KosarCore.coreWorking) {
			try {
				DoRegTrigg.acquire();
			} catch (InterruptedException e1) {
				System.out.println("Concurrency error: TriggerRegisterThread - Unable to acquire work semaphore");
				e1.printStackTrace();
			}
			if(KosarCore.coreWorking) {
				try {
					RDBMScmds.acquire();
					triggerItem = (TriggerItem)workitems.remove(0);
					RDBMScmds.release();
					switch (QueryToTrigger.getTargetSystem()) {
					case MySQL:
						if (triggerItem.getTrigger().indexOf(mysqlTrigGenerator.StartProc) >= 0)
							MySQLRegProc(triggerItem.getTrigger());
						else retval = MySQLRegTrig(triggerItem.getTrigger());
						break;
					case Oracle:
						if (triggerItem.getTrigger().indexOf(OracleTrigGenerator.StartProc) >= 0)
							retval = OracleRegProc(triggerItem.getTrigger());
						else retval = OracleRegTrig(triggerItem.getTrigger());
						break;
					default:
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//0 = successful registration
				//-1 = failed registration
				//1 = already registered
				messageClient(retval, triggerItem);
			}
		}
		
	}
	private void messageClient(boolean success, TriggerItem triggerItem) {
		int retval = -1;
		
		if(success) {
			retval = 0;
			if(verbose)
				System.out.println("Registered or Exists: " + triggerItem.getTrigger());
			
			ByteBuffer client = ByteBuffer.wrap(triggerItem.getClientID());
			KosarCore.triggersRegPerClient.get(client).incrementAndGet();
			KosarCore.triggerMap.put(triggerItem.getTrigger(), "1");
		}
		else {
			if(verbose)
				System.out.println("Failed: " + triggerItem.getTrigger());
		}
		
		// write the return value back to the client.
		try {
			triggerItem.getSocket().writeByte((byte)retval);
		} catch (IOException e) {
			System.out.println("Error: TriggerRegisterThread - Failed to send acknowledgement");
			e.printStackTrace();
		}

		
	}
	/*
	 * TriggerItem class deals with a trigger and a socket for the necessary
	 * client registering it.
	 */
	public static class TriggerItem {
		private String trigger;
		private SocketIO socket;
		private byte[] clientID;
		
		TriggerItem(SocketIO s, String trigger, byte[] clientID) {
			this.socket = s;
			this.trigger = trigger;
			this.clientID = clientID;
		}

		public byte[] getClientID() {
			return clientID;
		}

		public void setClientID(byte[] clientID) {
			this.clientID = clientID;
		}

		public String getTrigger() {
			return trigger;
		}

		public void setTrigger(String trigger) {
			this.trigger = trigger;
		}

		public SocketIO getSocket() {
			return socket;
		}

		public void setSocket(SocketIO socket) {
			this.socket = socket;
		}
	}
}
