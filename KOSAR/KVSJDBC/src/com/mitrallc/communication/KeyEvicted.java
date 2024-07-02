package com.mitrallc.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.ResultSet;
import com.mitrallc.sql.SockIOPool.SockIO;

/**
 * This class sends the key evicted notification to the coordinator. When the
 * message goes through and the coordinator sends a positive ACK, the query
 * result is removed.
 * 
 * @author Neeraj Narang
 * @author Lakshmy Mohanan
 * 
 */
public class KeyEvicted implements Runnable {
	private String key;

	public KeyEvicted(String key, ResultSet result, long Tmiss) {
		this.key=key;
	}
	

	public void run() {
		// Get the IP address of the client
		byte[] keyEvictedMessage;
		while (true) {
			try {
				//Create Message
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				// append message code
				baos.write(Constants.CLIENT_KEY_EVICTED);
				// Append client id.
				baos.write(KosarSoloDriver.clientData.getID());
				// Append key value that needs to be evicted.
				baos.write(this.key.getBytes());
				// convert to byte array
				keyEvictedMessage = baos.toByteArray();
				// send message
				SockIO socket = KosarSoloDriver.getConnectionPool().getSock();
				if (socket == null) {
					introduceDelay();
					continue;
				}
				socket.write(keyEvictedMessage);
				socket.flush();

				// wait for reply in which coordinator sends a positive ACK.
				int reply=socket.read();

				if (reply == Constants.COORDINATOR_ALL_OK) {
					KosarSoloDriver.Kache.DeleteCachedQry(key);

				} else {
					/**
					 * TODO:This block will be reached if coordinator replies
					 * with a negative ack. This will happen when some other
					 * thread updated the data and invalidated this clients I
					 * lease,ie this client can use the data but cannot cache
					 * it.
					 * 
					 * To implement when caching is implemented.
					 * 
					 */
				}
				if (KosarSoloDriver.getFlags() // and it is connected
						.isCoordinatorConnected() && null != socket)
					socket.close();
				break;
			} catch (ConnectException c) {
				KosarSoloDriver.startReconnectThread(System.currentTimeMillis());
				introduceDelay();
			} catch (SocketTimeoutException e) {
				introduceDelay();
			} catch (IOException e) {
				KosarSoloDriver.startReconnectThread(System.currentTimeMillis());
				introduceDelay();
			}
		}
	}

	public void introduceDelay() {
		try {
			Thread.sleep(Constants.sleepTime);
		} catch (InterruptedException e) {
		}
	}
}
