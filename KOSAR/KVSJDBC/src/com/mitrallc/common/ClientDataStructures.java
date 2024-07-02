package com.mitrallc.common;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class contains the data structures used to hold
 * 
 * Client ID - which is assigned by the coordinator
 * 
 * Port List - used to communicate with the coordinator
 * 
 * Invalidation Port Number - Port on which the client listens for invalidation
 * messages sent by the coordinator.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
public class ClientDataStructures {
	private byte[] ID;
	private byte[] ports;
	private int invalidationPort;
	public HashMap<String, Long> queryLocks = new HashMap<String, Long>();

	public byte[] getID() {
		return ID;
	}

	public void setID(byte[] iD) {
		ID = iD;
	}

	public byte[] getPorts() {
		return ports;
	}

	public void setPorts(byte[] ports) {
		this.ports = ports;
	}

	public int getInvalidationPort() {
		return invalidationPort;
	}

	public void setInvalidationPort(int invalidationPort) {
		this.invalidationPort = invalidationPort;
	}
}
