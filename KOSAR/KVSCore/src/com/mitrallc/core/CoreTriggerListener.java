package com.mitrallc.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import com.mitrallc.common.TokenObject;
import com.mitrallc.sqltrig.QueryToTrigger;
import com.mitrallc.util.SocketIO;

/**
 * 
 * So named to be consistent with the method of the same name in the KosarClient.
 * Listens to Trigger Fires from the RDBMS.
 *
 */
public class CoreTriggerListener extends Thread{
	ServerSocket server = null;
	static String ipPort = "";
	boolean continueListening = true;
	ByteArrayOutputStream baos = null;
	boolean verbose = false;
	
	public CoreTriggerListener() {
		//Finds an open port on which to listen for key invalidations.
		server = findAndSetListenerPort();
		baos = new ByteArrayOutputStream();
		System.out.println("Trigger Listener listening on: "+server.getLocalPort());
	}
	
	/**
	 * Searches for open ports and assigns an available one.
	 */
	public static ServerSocket findAndSetListenerPort() {
		// Obtain the IP and port address for the triggerlistener
		ServerSocket s = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			ipPort = addr.getHostAddress();
			int maxports = 10000;
			for (int p = 4000; p < maxports; p++) {
				try {
					s = new ServerSocket(p);

					ipPort += ":" + p;
					p = maxports;
				} catch (IOException e) {
					System.out.println("Port " + p + " is allocated.");
				}
			}
			QueryToTrigger.SetIPport(ipPort);
		} catch (UnknownHostException e) {
			throw new RuntimeException("Can't obtain IP address");
		}
		return s;
	}
	public void interrupt() {
		this.stop();
	}
	public void run() {
		SocketIO socket = null;
		while(continueListening){
			try{
				//wait for a connection from a client
				Socket s = server.accept();
		    	//database dll sends some command
				socket = new SocketIO(s);
				ByteBuffer bb = ByteBuffer.allocate(2);
				short command = socket.readShort();
				if(verbose) System.out.println("command: " + command);
		    	bb.putShort(command);
		    	baos.write(bb.array());
		    	bb.clear();
		    	
		    	bb = ByteBuffer.allocate(4);
		    	int datazoneSize = socket.readInt();
		    	bb.putInt(datazoneSize);
		    	baos.write(bb.array());
		    	bb.clear();
		    	
		    	int keyListSize = socket.readInt();
		    	bb.putInt(keyListSize);
		    	baos.write(bb.array());
		    	bb.clear();
		    	if(verbose)
		    		System.out.println("datazonesize="+datazoneSize + " keylistsize=" + keyListSize);
		    	byte[] keys = new byte[datazoneSize];
		    	socket.read(keys);
		    	baos.write(keys);
		    	
		    	//read the tokens
		    	keys = null;
		    	keys = new byte[keyListSize];
		    	socket.read(keys);
		    	baos.write(keys);
		    	baos.flush();
		    	
		    	if(verbose)
		    		System.out.println("RECEIVED KEYS: " + keys);
		    	int index = KosarCore.getIndexForWorkToDo();
		    	synchronized(KosarCore.requestsToProcess) {
					KosarCore.requestsToProcess.get(index).add(new TokenObject(socket, baos.toByteArray(), false));
					KosarCore.tokenCachedWorkToDo.get(index).release();
				}
		    	baos.reset();
		    	
	    	}catch (EOFException eof){
	    		System.out.println("Connection Lost with Database");
	    	}catch(SocketException se){
	    		se.printStackTrace();
	    	} catch(IOException i){
	    		i.printStackTrace();
	    	}
		}
	}
	
	public static void UnitTest1(){
		short s = 1111;
		byte[] keylist = {1,2,3,4,5};
		int datazoneSize = 0;
		int keylistSize = keylist.length;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.putShort(s);
			baos.write(bb.array());
			bb.clear();
			bb = ByteBuffer.allocate(4);
			bb.putInt(datazoneSize);
			baos.write(bb.array());
			bb.clear();
			bb.putInt(keylistSize);
			baos.write(bb.array());
			bb.clear();
			baos.write(keylist);
			baos.flush();
			Iterator<String> keys = KosarCore.invalidationPorts.keySet().iterator();
			while(keys.hasNext()) {
				String ip = keys.next();
				Socket sock = new Socket(ip, KosarCore.invalidationPorts.get(ip));
				DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(
						sock.getOutputStream()));
				outStream.write(baos.toByteArray());
				outStream.flush();
				baos.reset();
			}
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
