package com.mitrallc.core;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.mitrallc.util.SocketIO;

/**
 * 
 * So named to be consistent with the method of the same name in the KosarClient.
 * Listens to Trigger Fires from the RDBMS.
 *
 */
public class TriggerListener extends Thread{
	ServerSocket server = null;
	static String ipPort = "";
	boolean continueListening = true;
	
	public TriggerListener() {
		//Finds an open port on which to listen for key invalidations.
		server = findAndSetListenerPort();
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
		    	socket.readShort();
		    	//the dll then sends datazone size
		    	int datazoneSize = socket.readInt();
		    	int keyListSize = socket.readInt();
		    	//read the datazone
		    	byte[] keys = new byte[datazoneSize];
		    	socket.read(keys);
		    	//read the tokens
		    	keys = null;
		    	keys = new byte[keyListSize];
		    	socket.read(keys);
		    	System.out.println("RECEIVED KEYS: " + keys);
		    	//put keys in KeyQueue
		    	KosarCore.KEY_QUEUE_WRITE_LOCK.lock();
			    	KosarCore.keyQueue.add(KosarCore.AL.incrementAndGet(),keys);
			    KosarCore.KEY_QUEUE_WRITE_LOCK.unlock();
			    //System.out.println("key to invalidate: "+new String(keys));
			    //invalidationHandlerService.execute(new InvalidateKeyCommand(keys, socket,false));
	    	}catch (EOFException eof){
	    		System.out.println("Connection Lost with Database");
	    	}catch(SocketException se){
	    		se.printStackTrace();
	    	} catch(IOException i){
	    		i.printStackTrace();
	    	}
		}
	}
}
