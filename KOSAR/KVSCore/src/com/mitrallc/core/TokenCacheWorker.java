package com.mitrallc.core;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import com.mitrallc.common.TokenObject;
import com.mitrallc.util.SocketIO;
import com.mitrallc.webserver.BaseSettings;
import com.mitrallc.webserver.EventMonitor;

/**
 * The TokenCacheWorker is a worker thread that acquires a semaphore
 * dictating that there is work to be done.  TokenCacheWorkers cache keys.
 * They manage two important data structures in the core:
 * 		internalTokenToQueriesMap
 * 		queryToClientsMap
 * 
 * The primary interaction is as follows:  A client will request to cache 
 * a list of internal tokens and a query string. The TokenCacheWorker does so
 * and, depending on the state of the above hashmaps, will tell the client to
 * 		1) steal the KV pair from another client
 * 		2) copy the KV pair from another client
 * 		3) consume the KV pair of another client (obtain but not cache)
 * 		4) go to the RDBMS.
 */
public class TokenCacheWorker extends Thread{
	int workerID = -1;
	Semaphore testSem = new Semaphore(0, true);
	boolean verbose = false;
	Random rand = new Random();
	final int DEFAULT_INIT_ARRAY_SIZE = 3;

	public TokenCacheWorker(int workerID) {
		this.workerID = workerID;
	}
	
	public void run() {
		while(KosarCore.coreWorking) {
			try {
				//Acquire the semaphore for work to be done.
				KosarCore.tokenCachedWorkToDo.get(workerID).acquire();
			} catch (InterruptedException e) {
				System.out.println("Error: TokenCacheWorker - Could not acquire Semaphore");
				e.printStackTrace();
			}

			//We put this condition boolean here so TokenCacheWorkers 
			//may close without performing these actions
			//if they are shut down by the CORE.
			if(KosarCore.coreWorking) {
				//Get the token object.  Since objects are put in this data structure
				//before the semaphore is released, there should never be a null pointer exception.
				TokenObject tokenObject = null;
				SocketIO socket = null;
				byte[] requestArray = null;
				boolean cache = false;
				synchronized(KosarCore.requestsToProcess) {
					tokenObject = (TokenObject) KosarCore.requestsToProcess.get(workerID).poll();
				}
				if(tokenObject != null) {
					socket = tokenObject.getSocket();
					requestArray = tokenObject.getRequestArray();
					cache = tokenObject.isCache();
					if(cache) {
						doCache(socket, requestArray);
					} else {
						doInvalidate(socket, requestArray);
					}
				}//end if tokenObject != null
			}//end if
		} //end while
		System.out.println("Worker " + workerID + " finished.");
	}

	public void doCache(SocketIO socket, byte[] requestArray) {
		/**
		 * Format of request message from client:
		 * clientID (4 bytes)
		 * qryLength (4 bytes)
		 * qry (qryLength bytes)
		 * it[0].length
		 * it[0]
		 * it[1].length
		 * it[1]
		 * ...
		 * it[n].length
		 * it[n]
		 */
		//Extract 4-byte client ID.
		byte[] clientID = Arrays.copyOfRange(requestArray, 0, 4);
		
		//Retrieve the sql query from the order queryLength query
		int qryLength = 0;
		int index = 4;
		byte[] query = null;
		qryLength = ByteBuffer.wrap(Arrays.copyOfRange(requestArray, index, index+4)).getInt();
		index += 4;
		query = Arrays.copyOfRange(requestArray, index, index+qryLength);
		try {
			KosarCore.last100readQueries.add(new String(query, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		index += qryLength;
		
		//Retrieve the internal tokens from the order ITlength IT ITlength IT ...
		byte[] internalToken = null;
		int itLength = 0;
		while(index < requestArray.length) {
			itLength = ByteBuffer.wrap(Arrays.copyOfRange(requestArray, index, index+4)).getInt();
			index += 4;
			internalToken = Arrays.copyOfRange(requestArray, index, index+itLength);
			index += itLength;

			//Insert Internal Token into the internalTokenToQueries HashMap.
			try {
				mapTokenToQueryArray(internalToken, query);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		//Map Query to Clients Map
		Object[] clients = mapQueryToClients(query, clientID);
		String command = "";

		/* Preceding Command:
		 * 		querydb: Go to DB
		 * 		steal: StealFromClient (Client B [the messaged Client] deletes)
		 * 		copy: CopyFromClient (Both Clients retain key-value pair)
		 */
		if(clients == null || KosarCore.getNumReplicas() == 0)
			command = "querydb";
		else if(KosarCore.getNumReplicas() == -1 ||
				clientsInArrayNotThisClient(clients, clientID) < KosarCore.getNumReplicas()) {
			command = "copy";
			KosarCore.copyCount++;
		}
		else{
			if(KosarCore.getClientReplicaCommand().equalsIgnoreCase("steal")) {
				command = "steal";
				KosarCore.stealCount++;
			} else if(KosarCore.getClientReplicaCommand().equalsIgnoreCase("consume")) {
				command = "consume";
				KosarCore.consumeCount++;
			}
		}
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(command.getBytes().length);
			baos.write(bb.array());
			baos.write(command.getBytes());
			bb.clear();

			//Determine what client ip's to send to the receiving client.
			if(clients != null) {
				//Pick an element in array to start with
				int startingPoint = Math.abs(rand.nextInt() % clients.length);
				int count = 0;
				for(int i = startingPoint; i < clients.length+startingPoint; i++) {
					//The pointing index of the for loop wraps around the array
					int j = i%clients.length;
					if(clients[j] != null && ((ByteBuffer)clients[j]).compareTo(ByteBuffer.wrap(clientID)) != 0) {
						byte[] id = ((ByteBuffer)clients[j]).array();
						byte[] ip = ((ByteBuffer)KosarCore.clientToIPMap.get((ByteBuffer)clients[j])).array();
						if(ip != null) {
							baos.write(id); //4 bytes
							bb.putInt(ip.length);
							baos.write(bb.array());
							baos.write(ip);
							bb.clear();
						}
					}
				}
				System.out.println();
			}
			baos.flush();
			socket.writeBytes(baos.toByteArray());
			baos.reset();

			baos = null;
			bb = null;
			command = null;
		} catch (IOException e) {
			System.out.println("Error: TokenCacheWorker - Socket Error; Could not connect to Client");
			e.printStackTrace();
			System.exit(-1);
		} catch(Exception e) {
			System.out.println("Unknown Exception.");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void doInvalidate(SocketIO dbSocket, byte[] requestArray) {
		System.out.println("Do invalidate");
		try {
			/*Iterator<String> keys = KosarCore.invalidationPorts.keySet().iterator();
			while(keys.hasNext()) {
				String ip = keys.next();*/
				int count = 0;
				for(SocketIO sock : KosarCore.invalidationSockets) {
					sock.writeBytes(requestArray);
					int response = sock.readInt();
					if(response==0)count++;
				}
				if(count == KosarCore.invalidationSockets.size()) {
					dbSocket.getOut().writeInt(0);
				} else {
					dbSocket.getOut().writeInt(1);
				}
				dbSocket.getOut().flush();
				dbSocket.closeAll();
				System.out.println("Invalidation Done.");
			//}
		} catch (IOException ioex){
			ioex.printStackTrace();
		}
	}
	/**
	 * Returns the number of clients in an array (first parameter) whose id is not equal
	 * to the clientID passed in the second parameter.
	 * 
	 * @param clients
	 * @param thisClient
	 * @return
	 */
	private int clientsInArrayNotThisClient(Object[] clients, byte[] thisClient) {
		int count = 0;
		for(Object o : clients)
			if(o != null && !((ByteBuffer)o).equals(ByteBuffer.wrap(thisClient))) count++;
		return count;
	}
	
	/**
	 * This method maps tokens to query arrays.
	 * @param internalToken
	 * @param query
	 * @throws UnsupportedEncodingException
	 */
	private void mapTokenToQueryArray(byte[] internalToken, byte[] query) throws UnsupportedEncodingException {
		//If the internal token is found, attempt to store it in existing array.
		//If it's full, double its size
		//If the query is already present in the array, don't insert.
		try {
			KosarCore.internalTokenSemaphores.get(KosarCore.computeHashCode(internalToken, internalToken.length)).acquire();
		} catch (InterruptedException e) {
			System.out.println("Error: TokenCacheWorker - Failed to obtain InternalTokenSemaphore.");
			e.printStackTrace();
		}
		Object[] queriesArray = null;
		queriesArray = (Object[])KosarCore.internalTokenToQueriesMap.get(ByteBuffer.wrap(internalToken));
		if(queriesArray != null) {
			//Returns -1 if full.
			int nextIndex;
			if((nextIndex = getNextEmpty(queriesArray)) != -1) {
				//Check to make sure that clientID is not present in map.
				//If found, don't put.
				if(indexOf((Object[]) queriesArray, query) < 0) {
					queriesArray[nextIndex] = ByteBuffer.wrap(query);
					if(verbose)
						System.out.println("Query not found in IT-query map. InternalToken " + 
								Arrays.toString(internalToken) + " entered with " + new String(query, "UTF-8"));
				}
				else {
					if(verbose)
						System.out.println("Query found in IT-query map. InternalToken " + 
								Arrays.toString(internalToken) + " entered with " + new String(query, "UTF-8"));
				}
			} else {
				//Expand client map to twice the length
				Object[] newArray = new Object[queriesArray.length*2];
				System.arraycopy(queriesArray,0,newArray,0,queriesArray.length);
				if(indexOf(newArray, query) < 0) {
					newArray[getNextEmpty(newArray)] = ByteBuffer.wrap(query);
					if(verbose)
						System.out.println("Putting Query Entry for " + Arrays.toString(query));
				} else {
					if(verbose)
						System.out.println("Query Entry found for " + Arrays.toString(internalToken)+", " + 
								new String(query,"UTF-8") + ". Not putting.");
				}
				queriesArray = newArray;
			}	
		} else {
			//No client has yet mapped this internal token.
			Object[] queryArray = new Object[DEFAULT_INIT_ARRAY_SIZE];
			queryArray[0] = ByteBuffer.wrap(query);
			KosarCore.internalTokenToQueriesMap.put(ByteBuffer.wrap(internalToken), queryArray);
			if(verbose)	
				System.out.println("Creating new pair in map: " + Arrays.toString(internalToken) +", "
						+ new String(query, "UTF-8"));
		}
		KosarCore.internalTokenSemaphores.get(KosarCore.computeHashCode(internalToken, internalToken.length)).release();
	}

	/**
	 * 
	 * @param query
	 * @param clientID
	 * @return 	
	 * 		If no clients have the sql query cached or the CORE sees that the requesting
	 * 			client already has the sql query cached, returns null. Put the client in the data structure
	 * 			and go to the RDBMS.
	 * 		If one or more clients have the sql cached, returns an array of client ids. Send list of id's
	 * 			to the requesting client.  The client will parse the message and determine how many client ids
	 * 			have been sent. If it is fewer than the max, then it will not request a client to delete the sql
	 * 			query.  However, if the number of client id's equals the max number of replicas specified,
	 * 			the client will choose a client and delete it from that cache.
	 */
	private Object[] mapQueryToClients(byte[] query, byte[] clientID) {
		
		boolean sendArray = false;
		Object[] clientArray;
	
		try {
			KosarCore.querySemaphores.get(KosarCore.computeHashCode(query, query.length)).acquire();
		} catch (InterruptedException e) {
			System.out.println("Error: TokenCacheWorker - Failed to acquire semaphore.  InterruptedException thrown");
			e.printStackTrace();
		}
		//Concurrent HashMap.  No need to further protect with other concurrent protection methods
		clientArray = (Object[])KosarCore.queryToClientsMap.get(ByteBuffer.wrap(query));
		
		if(clientArray != null) {
			if(indexOf((Object[]) clientArray, clientID) < 0) {
				//Check to make sure that clientID is not present in map.
				int nextIndex;
				if(KosarCore.getClientReplicaCommand().equalsIgnoreCase("steal")
						|| clientsInArrayNotThisClient(clientArray, clientID) < KosarCore.getNumReplicas()) {
					if((nextIndex = getNextEmpty(clientArray)) != -1) {
						clientArray[nextIndex] = ByteBuffer.wrap(clientID);
						if(verbose)
							System.out.println("Space found in client array.");
					} else {
						//Expand client map to twice the length
						Object[] newArray = new Object[clientArray.length*2];
						System.arraycopy(clientArray,0,newArray,0,clientArray.length);
						newArray[getNextEmpty(newArray)] = ByteBuffer.wrap(clientID);
						if(verbose)
							System.out.println("Doubled array size to " + clientArray.length*2);
						clientArray = newArray;
					}
					KosarCore.numQueryInstances.get(ByteBuffer.wrap(clientID)).incrementAndGet();
				}
				sendArray = true;
			} else {
				//Only send the array if there are clients other than the current one in the array.
				for(Object o : (Object[])clientArray) {
					if(o != null && ((ByteBuffer)o).compareTo(ByteBuffer.wrap(clientID)) != 0)
						sendArray = true;
				}
			}
		} else {
			//No client has yet mapped this query
			clientArray = new Object[KosarCore.getNumReplicas()== -1 ? DEFAULT_INIT_ARRAY_SIZE : KosarCore.getNumReplicas()];
			clientArray[0] = ByteBuffer.wrap(clientID);
			KosarCore.queryToClientsMap.put(ByteBuffer.wrap(query), clientArray);
			KosarCore.numQueryInstances.get(ByteBuffer.wrap(clientID)).incrementAndGet();
			if(verbose)
				System.out.println("No client has mapped yet. Creating new array");
		}
		Object[] returnArray = null;
		if(sendArray)
			returnArray = (Object[]) KosarCore.queryToClientsMap.get(ByteBuffer.wrap(query));
		
		KosarCore.querySemaphores.get(KosarCore.computeHashCode(query, query.length)).release();
		return returnArray;
	}
	
	/**
	 * Returns the index of the second parameter from the first parameter.
	 * If the second parameter (byteArray) is not found in arrayOfByteBuffers,
	 * return -1.
	 * 
	 * @param arrayOfByteBuffers
	 * @param byteArray
	 * @return
	 */
	public static int indexOf(Object[] arrayOfByteBuffers, byte[] byteArray) {
		for(int i = 0; i < arrayOfByteBuffers.length; i++) {
			if(ByteBuffer.wrap(byteArray).equals((ByteBuffer)arrayOfByteBuffers[i]))
				return i;
		}
		return -1;
	}
	
	/**
	 * Returns -1 if the array is full.
	 * @param array
	 * @return
	 */
	public int getNextEmpty(Object[] array) {
		for(int i = 0; i < array.length; i++) {
			if(array[i] == null) {
				return i;
			}
		}
		return -1;
	}
	public static void main(String args[]) {
		UnitTest1(1);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("-----------------------");
		UnitTest1(2);
	}
	public static void UnitTest1(int factor) {
		byte b[] = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] clientID = {(byte)(factor*2),14,25,36};
			baos.write(clientID);
			String sql = "SELECT COUNT(*) FROM friendship";
			byte[] sqlBytes = sql.getBytes();
			byte[] it1 = {3,3,3,3,3,3,3,3,3,3};
			byte[] it2 = {2,2,2,2,2,2,2,2,2,2,2,2,2};
			byte[] it3 = {5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5};

			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(sqlBytes.length);
			baos.write(bb.array());
			baos.write(sqlBytes);
			bb.clear();
			bb.putInt(it1.length);
			baos.write(bb.array());
			baos.write(it1);
			bb.clear();
			bb.putInt(it2.length);
			baos.write(bb.array());
			baos.write(it2);
			bb.clear();
			bb.putInt(it3.length);
			baos.write(bb.array());
			baos.write(it3);
			bb.clear();
			bb.putInt(it1.length);
			baos.write(bb.array());
			baos.write(it1);
			bb.clear();
			bb.putInt(it2.length);
			baos.write(bb.array());
			baos.write(it2);
			bb.clear();
			bb.putInt(it3.length);
			baos.write(bb.array());
			baos.write(it3);
			baos.flush();

			b = baos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		KosarCore.setNumReplicas(3);
		TokenCacheWorker tcw = new TokenCacheWorker(0);
		tcw.testSem.release();
		TokenObject to = new TokenObject(null, b, true);
		KosarCore.requestsToProcess.add(new ConcurrentLinkedQueue<Object>());
		KosarCore.requestsToProcess.get(0).add(to);
		//tcw.check();
	}
}
