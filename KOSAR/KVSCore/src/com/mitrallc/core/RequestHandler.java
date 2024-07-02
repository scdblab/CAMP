package com.mitrallc.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mitrallc.common.TokenObject;
import com.mitrallc.util.ShutdownHook;
import com.mitrallc.util.SocketIO;
import com.mitrallc.webserver.BaseSettings;
import com.mitrallc.webserver.EventMonitor;

/**
 * Each Client has one RequestHandler, which manages its requests.
 * Though each command may be a multi-threaded request, the request handler
 * is single-threaded. Requests may be processed on a different thread.
 * This design is in place because the CORE itself should do minimal input work.
 * 
 * The CORE may send many messages to the Clients for key invalidation.
 */
public class RequestHandler extends Thread {

	int handlerID;
	byte[] clientID = null;
	boolean continueHandling = true;
	SocketIO socket = null;
	Scanner scanner = null;
	String trigger = null;
	boolean verbose = false;
	int index;

	RequestHandler(Socket sock, int handlerID) {
		try {
			this.socket = new SocketIO(sock);
		} catch (IOException e) {
			System.out.println("Error: RequestHandler - Unable to establish SocketIO from socket");
			e.printStackTrace();
		} 
		this.handlerID = handlerID;
		Runtime.getRuntime().addShutdownHook(
				new ShutdownHook(this, this.getClass().toString()));
	}

	public void run() {
		byte request[] = null;
		int command = -1;
		int length = -1;
		if(verbose)
			System.out.println("Client " + handlerID + " Listening on " + socket.getSocket().getInetAddress()
					+":"+socket.getSocket().getPort());
		try {
			while (continueHandling) {
				try {
					if (socket != null || !socket.getSocket().isConnected()
							|| !socket.getSocket().isClosed()) {
						//Reads in the request from the Client.
						//Format is always 4-byte int command followed by byte array
						//whose format is determined by the command.
						request = socket.readBytes();
					} else {
						System.out
						.println("Error: RequestHandler - Socket is null.");
					}
				} catch (EOFException eof) {
					System.out.println("End of Stream. Good Bye.");
				} catch (Exception e) {
					System.out.println("Client Stream Shutdown.");
					break;
				}
				command = ByteBuffer.wrap(Arrays.copyOfRange(request, 0, 4)).getInt();
				//pulls the command from the request byte array
				request = Arrays.copyOfRange(request, 4, request.length);
				
				/**
				 * Commands:
				 * 1) Register Client
				 * 3) Register Ping Handler
				 * 4) Cache Keys
				 * 5) Delete Key (from Client when KV pair is being stolen by another client)
				 * 7) Register Trigger
				 * 99) Shutdown
				 */
				switch (command) {
				case 1:
					if(verbose) 
						System.out.println("Received command 1.  Registering Client.");
					clientID = KVSClientRegistrar.register(socket, request, true);
					for(RequestHandler rh : KosarCore.handlers) {
						if(rh.getSocket().getSocket().getInetAddress().getHostAddress().equals(
								socket.getSocket().getInetAddress().getHostAddress()))
							rh.setClientID(clientID);
					}
					break;
				case 3:
					if(verbose)
						System.out.println("Received command 3.  Registering Ping Thread.");
					//Fix so that it doesn't keep creating a new thread.
					new KVSClientPingHandler(request).run();
					break;
				case 4:
					//if(verbose)
					//	System.out.println("Received command 4.  Registering Key Cached.");
					index = KosarCore.getIndexForWorkToDo();
					synchronized(KosarCore.requestsToProcess) {
						KosarCore.requestsToProcess.get(index).add(new TokenObject(socket, request, true));
						KosarCore.tokenCachedWorkToDo.get(index).release();
					}
					KosarCore.requestRateEventMonitor.get(ByteBuffer.wrap(clientID)).newEvent(1);
					break;
				case 5:
					new QuickDeleteQueryIDPair(request).start();
					break;
				case 7:
					if(verbose)
						System.out.println("Received command 7.  Registering Trigger(s).");
					KosarCore.triggerRegThread.AddTrig(socket, request, clientID);
					break;		
				case 99:
					System.out
					.println("Received command 99.  Client " + handlerID + " shutting down.");
					socket.writeByte((byte)0);
					continueHandling = false;
					break;
				default:
					System.out
					.println("Error: RequestHandler - Could not recognize command "
							+ command);
					break;
				}

			} // end while
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				socket.closeAll();
			} catch (IOException e) {
				System.out.println("Error: RequestHandler - Failed to close streams");
				e.printStackTrace();
			}
			//doPrintout();
			System.out.println("RequestHandler " + handlerID
					+ " shut down. I/O cleanup complete.");
		}
	}

	/**
	 * This class, called by a client when removing a KV pair from its cached,
	 * removes the <clientid,querystring> pair from queryToClientsMap
	 *
	 */
	class QuickDeleteQueryIDPair extends Thread{
		byte[] request = null;
		QuickDeleteQueryIDPair(byte[] request) {
			this.request = request;
		}
		public void run() {
			try {
				//Parse message
				int index = 0;
				int sqlLength = ByteBuffer.wrap(Arrays.copyOfRange(request, 0, 4)).getInt();
				index += 4;
				byte[] sql = Arrays.copyOfRange(request, index, index+sqlLength);
				index += sqlLength;
				byte[] clientIDToDelete = Arrays.copyOfRange(request, index, index+4);
				
				//Acquire semaphore and perform delete
				KosarCore.querySemaphores.get(KosarCore.computeHashCode(sql, sql.length)).acquire();
				Object[] clientList = (Object[]) KosarCore.queryToClientsMap.get(ByteBuffer.wrap(sql));
				if(clientList != null) {
					int i;
					if((i = TokenCacheWorker.indexOf(clientList, clientIDToDelete)) >= 0)
						clientList[i] = null;
				}
				//Release semaphore.
				KosarCore.querySemaphores.get(KosarCore.computeHashCode(sql, sql.length)).release();
			} catch (InterruptedException e) {
				System.out.println("Error: RequestHandler - Failed to acquire querySemaphore. InterruptedException thrown");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Optional printout of query strings and their constituent clients to a text file.
	 */
	public static void doPrintout() {
		int index = 0;
		while(true) {
			HashMap<ByteBuffer, Integer> map = new HashMap<ByteBuffer, Integer>();
			File f = null;
			PrintWriter pw = null;
			try {
				f = new File("test"+index+".txt");
				if(f.createNewFile()) {
					pw = new PrintWriter(f);
					pw.println("Printing results");
					synchronized(KosarCore.queryToClientsMap) {
						Iterator iterator = KosarCore.queryToClientsMap.keySet().iterator();
						int count = 0;
						int runningNoClients = 0;
						while (iterator.hasNext()) {  
							count++;
							ByteBuffer key = (ByteBuffer) iterator.next();  
							Object[] value = (Object[]) KosarCore.queryToClientsMap.get(key);  

							pw.print(new String(key.array(), "UTF-8") + " = ");
							for(Object o : value) {
								if(o != null) {
									if(map.containsKey((ByteBuffer)o)) {
										map.put((ByteBuffer)o, map.get((ByteBuffer)o) +1);
									} else
										map.put((ByteBuffer)o, 1);
									runningNoClients++;
									pw.print(Arrays.toString(((ByteBuffer)o).array()) + "  ,  ");
								}
							}
							pw.println();
						}
						pw.println();
						Iterator i = map.keySet().iterator();
						while(i.hasNext()) {
							ByteBuffer next = (ByteBuffer)i.next();
							pw.println(Arrays.toString(next.array()) + " : " + map.get(next));
						}
						pw.println("AVG Client Count per query: " + (double)runningNoClients/(double)count);
					}
					pw.flush();
					
					break;
				} else
					index++;
			} catch(IOException ioe) {
				ioe.printStackTrace();
			} finally {
				if(f != null && pw != null)
					pw.close();
			}
		}

	}

	/****Getters and Setters****/
	public SocketIO getSocket() {
		return socket;
	}

	public void setSocket(SocketIO socket) {
		this.socket = socket;
	}

	public byte[] getClientID() {
		return clientID;
	}

	public void setClientID(byte[] clientID) {
		this.clientID = clientID;
	}

	public int getHandlerID() {
		return handlerID;
	}

}