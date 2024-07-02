package com.mitrallc.core;

import java.nio.ByteBuffer;

/**
 * This class listens for Ping messages from the clients.
 * 
 * @author Neeraj Narang
 * @author Lakshmy Mohanan
 */
public class KVSClientPingHandler implements Runnable {
	private byte[] clientID = null;

	public KVSClientPingHandler(byte[] clientID){
		this.clientID = clientID;
	}

	@Override
	public void run() {
		try {
			//update timestamp in the PingTimeStamp Map
			if(KosarCore.clientToPortsMap.containsKey(ByteBuffer.wrap(clientID))){
				//if it's a valid client id
				KosarCore.pingClientsMap.put(ByteBuffer.wrap(clientID), System.currentTimeMillis());
			} else {
				System.out.println("Error: KosarClientPingHandler - No Valid Client found in ClientMap");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
