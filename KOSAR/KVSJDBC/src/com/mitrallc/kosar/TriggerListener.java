package com.mitrallc.kosar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;

import com.mitrallc.common.Constants;
import com.mitrallc.common.KeyQueueDataItem;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.PreparedStatement;
import com.mitrallc.sqltrig.QueryToTrigger;

public class TriggerListener extends Thread {
	private static final short CMD_USECACHE = 1111;
	private static long invalidatecount=0;
	private int networkport;
	private String networkaddr;
	private kosar cachemgr = null;
	private ServerSocket s = null;
	private boolean verbose = false;

	public TriggerListener(kosar k1) {
		cachemgr = k1;
		setIpPortAddress();
	}

	public TriggerListener() {}

	void setIpPortAddress() {
		// Obtain the IP and port address for the triggerlistener
		try {
			StringBuilder builder = new StringBuilder();
			String ipport;
			if (KosarSoloDriver.core_ip != null)
				builder.append(KosarSoloDriver.core_ip).append(":").append(KosarSoloDriver.db_port);
			else
				builder.append(InetAddress.getLocalHost().getHostAddress());
			int maxports = 10000;
			for (int p = 4000; p < maxports; p++) {
				try {
					s = new ServerSocket(p);
					KosarSoloDriver.clientData.setInvalidationPort(p);
					networkport = p;
					
					p = maxports;
					
				} catch (IOException e) {
					if (verbose)
						System.out.println("Port " + p + " is allocated.");
				}
			}
			if (KosarSoloDriver.core_ip == null){
				builder.append(":").append(networkport);
			}

			ipport = builder.toString();
			if (kosar.qt != null && ipport.indexOf(":") > 0) {
				if (KosarSoloDriver.core_ip == null)
					networkaddr = ipport;
				System.out.println("ipport " + ipport);
				kosar.qt.SetIPport(ipport);
			} else
				System.out
				.println("Error, kosar failed to set IP address in QueryToTrigger:  Expect stale data.  Constructed "
						+ ipport);
		} catch (UnknownHostException e) {
			throw new RuntimeException("Can't obtain IP address");
		}
	}


	public void response(DataOutputStream outStream, int responseCode) throws IOException
	{
		outStream.writeInt(responseCode);
	}

	public void run() {

		Socket client = null;
		DataInputStream inStream = null;
		DataOutputStream outStream = null;

		byte[] byte_array;
		String datazone = null;
		String keylist;
		int datazoneSize = -1;
		int keylistSize = -1;
		int response = 0;

		kosar.StartTrigListeners.release(); //kosar cache manager is waiting for the listener to start.

		if (s == null){
			System.out.println("Failed to open a ServerSocket on port "+networkport);
			QueryToTrigger.delIPport(networkaddr);
			return;
		}
		boolean firstTime = true;
		for (;;) {
			try {
				response = 0;
				//If there is no core or it's the first time, then connect every iteration.
				//Otherwise, the loop should go right to command = inStream.readShort()
				if(KosarSoloDriver.core_ip == null || firstTime)
					client = s.accept();
				if (client == null) {
					System.out
					.println("Error in TriggerListener:  client is null.");
				} else {
					if(KosarSoloDriver.core_ip == null || firstTime) {
						inStream = new DataInputStream(new BufferedInputStream(
								client.getInputStream()));
						outStream = new DataOutputStream(new BufferedOutputStream(
								client.getOutputStream()));
					}

					firstTime = false;
					short command = inStream.readShort();
					if (verbose) System.out.println("Recived on "+networkport+": cmd = "+command);
					if (command == CMD_USECACHE) {
						System.out.println("flag change");
						datazoneSize = inStream.readInt();
						keylistSize = inStream.readInt();
						if (datazoneSize > 0) {
							byte_array = new byte[datazoneSize];
							inStream.read(byte_array);
							datazone = new String(byte_array);
						}
						if (keylistSize > 0) {
							if (verbose)
								System.out.println("" + networkaddr + ":"
										+ networkport);
							byte_array = new byte[keylistSize];
							inStream.read(byte_array);
							keylist = new String(byte_array);
							if (verbose)	System.out.println(keylist);
						}
					} else {
						if(verbose)System.out.println("noflag change");
						datazoneSize = inStream.readInt();
						keylistSize = inStream.readInt();
						if (verbose) System.out.println("datazoneSize="+datazoneSize+", keylistSize="+keylistSize);
						if (datazoneSize >= 1000 || keylistSize >= 100000){
							System.out.println("Error in TriggerListener:  Payload is corrupt.  Skipping the rest");
							System.out.println("datazoneSize="+datazoneSize+", keylistSize="+keylistSize);
							System.out.println("Limits are 1000 for datazoneSize and 100,000 for keylistSize (all numbers in bytes)");
							response = 1;
						} else {
							if (datazoneSize > 0) {
								byte_array = new byte[datazoneSize];
								inStream.read(byte_array);

								datazone = new String(byte_array);
							}
							if (verbose && datazone != null) System.out.println("datazone "+datazone);

							if (keylistSize > 0) {
								if (verbose)
									System.out.println("" + networkaddr + ":"
											+ networkport);
								byte_array = new byte[keylistSize];
								inStream.read(byte_array);
								/* When in single node mode,
								 * The following step adds this key set to the Client's KeyQueue.
								 * This will be used later to implement the double delete operation
								 * to eliminate staleness because of a R-W race condition.
								 * 
								 */
								if(KosarSoloDriver.kosarEnabled) {
									Constants.KEY_QUEUE_WRITE_LOCK.lock();
									KosarSoloDriver.keyQueue.add(Constants.AI.incrementAndGet(), byte_array);
									if (verbose){
										keylist = new String(byte_array);
										String[] its = keylist.trim().split(" ");
										System.out.print("Delete: ");
										for (int s1=0; s1<its.length; s1++)
											System.out.println(""+its[s1]+ " ");
										System.out.println("");
									}
									Constants.KEY_QUEUE_WRITE_LOCK.unlock();
								}

								keylist = new String(byte_array);
								String[] its = keylist.trim().split(" ");
								HashMap<String, String> H = new HashMap<String, String>();
								if (verbose)
									System.out
									.println("KosarSolo Driver TriggerListener:  keylist "
											+ keylist);
								for (int s1 = 0; s1 < its.length; s1++)
									if (cachemgr != null && its[s1].length() > 0) {
										if (H.get(its[s1])==null){
											cachemgr.DeleteIT(its[s1]);
											H.put(its[s1], "1");
										}
									}
							}
						}
					}
					if (verbose) System.out.println("TriggerListener, response is "+response);
					this.response(outStream, response);
					if(KosarSoloDriver.core_ip == null) {
						if (outStream != null) {
							outStream.flush();
							outStream.close();
							outStream = null;
						}
						if (inStream != null) {
							inStream.close();
							inStream = null;
						}
						if (client != null) {
							client.close();
							client = null;
						}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
