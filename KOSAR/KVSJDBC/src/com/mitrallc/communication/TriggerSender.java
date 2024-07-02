package com.mitrallc.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;

import com.mitrallc.common.Constants;
import com.mitrallc.kosar.TriggerRegThread;
import com.mitrallc.kosar.kosar;
import com.mitrallc.kosar.query;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;

/**
 * This class sends the trigger to the coordinator, so that the coordinator can
 * register it with the database. When the message goes through and the
 * coordinator sends a positive ACK, the query result is cached.
 * 
 * @author Neeraj Narang
 * @author Lakshmy Mohanan
 * 
 */
public class TriggerSender implements Runnable {
	private query q;

	public TriggerSender(query q) {
		this.q = q;
	}

	public void run() {
		// Format for message sent
		// 7#paramq#comma-separated-token list#triggers which are hash separated
		byte[] keyCachedMessage = null;
		// Create Message
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(Constants.CLIENT_REG_TRIGGER);
			baos.write('#');
			baos.write(q.getParamQuery().getBytes());
			baos.write('#');
			for (int i = 0; i < q.getInternalKeys().size(); i++) {
				baos.write(q.getInternalKeys().get(i).getBytes());
				if (i < q.getInternalKeys().size() - 1) {
					baos.write(',');
				}
			}
			baos.write('#');
			for (int j = 0; j < q.getTriggers().size(); j++) {
				baos.write(q.getTriggers().get(j).getBytes());
				if (j < q.getTriggers().size() - 1) {
					baos.write('#');
				}
			}
			keyCachedMessage = baos.toByteArray();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		while(true) {
			SockIO socket = null;
			try {
				
				// send message
				if (null == socket)
					socket = KosarSoloDriver.getConnectionPool().getSock();
				if (socket == null) {
					//System.out.println("trigger sender:no socket available");
					introduceDelay();
					continue;
				}
				//System.out.println("trigger sender: socket acquired");
				socket.write(keyCachedMessage);
				socket.flush();
			
				int response = socket.read();
				if(response == Constants.CLIENT_ALL_OK) {
					((ConcurrentHashMap<String, String>) TriggerRegThread.RegQueries[kosar
							.getFragment(q.getParamQuery())]).put(
							q.getParamQuery(), "exists");
					System.out.println("trigger registered");
				} else {
					System.out.println("trigger registration failed");
				}
				break;
			} catch (ConnectException c) {
				System.out.println("cache: connect exception");
				KosarSoloDriver
						.startReconnectThread(System.currentTimeMillis());
				break;
			} catch (SocketTimeoutException e) {
				System.out.println("trigger:socket time out");
				introduceDelay();
			} catch (IOException e) {
				System.out.println("cache: io exception");
				KosarSoloDriver
						.startReconnectThread(System.currentTimeMillis());
				break;
			} finally {
				if (KosarSoloDriver.getFlags() // and it is connected
						.isCoordinatorConnected() && null != socket) {
					socket.close();
				}
			}
		}
		//KosarSoloDriver.getLockManager().unlock(q.getQuery());
	}

	public void introduceDelay() {
		try {
			Thread.sleep(Constants.sleepTime);
		} catch (InterruptedException e) {
		}
	}
}
