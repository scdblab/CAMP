package com.mitrallc.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;

/**
 * This class reestablishes the connection with the coordinator if it breaks
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 */
public class CoordinatorReconnector implements Callable<Boolean> {
	public Boolean call() {
		// Get the IP address of the client
		byte[] reconnectMessage;
		KosarSoloDriver.getConnectionPool().shutDown();
		int timeTryingToReconnect = 0;
		while (true) {
			try {
				System.out.println("Trying to reconnect...");
				// Creates the socket connections
				KosarSoloDriver.getConnectionPool().initialize();

				// Sends the handshake message letting the coordinator know
				// that it is alive
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				SockIO socket = KosarSoloDriver.getConnectionPool().getSock();
				if (socket == null) {
					timeTryingToReconnect = introduceDelay(timeTryingToReconnect);
					continue;
				}
				// append message code
				baos.write(Constants.CLIENT_RECONNECT);
				
				baos.write(KosarSoloDriver.clientData.getID());
				baos.write('|');
				
				baos.write(socket.getLocalIPAddress());
				baos.write('|');
				
				// append old port IDs
				baos.write(KosarSoloDriver.clientData.getPorts());
				baos.write('|');
				// append new port IDs
				byte[] ports = KosarSoloDriver.getConnectionPool().getPorts();
				baos.write(ports);
				baos.write('|');

				ByteBuffer bb = ByteBuffer.allocate(4); 
			    bb.putInt(KosarSoloDriver.clientData.getInvalidationPort());
				
				baos.write(bb.array());

				// convert to byte array
				reconnectMessage = baos.toByteArray();
				// send message
				socket.write(reconnectMessage);
				socket.flush();
				
				// Wait for reply
				// The reply can be Clean cacle, All ok, or clear cache.
				byte[] reply = socket.readBytes();
				
				if (reply[0] == Constants.COORDINATOR_CLEAN_CACHE) {
					KosarSoloDriver.clientData.setPorts(ports);
					int size=reply.length;
					int i=1;
					while(i<size) {
						StringBuilder sb = new StringBuilder();
						while(reply[i]!=','&&i<size) {
							sb.append(reply[i]);
							i++;
						}
						System.out.println(sb.toString());
						KosarSoloDriver.Kache.DeleteCachedQry(sb.toString());
						i++;
					}
				} else if (reply[0] == Constants.COORDINATOR_ALL_OK) {
					// Store the remaining bytes as the id.
					KosarSoloDriver.clientData.setPorts(ports);
				} else if (reply[0] == Constants.COORDINATOR_CLEAR_CACHE) {
					// This Message contains New ID.

					KosarSoloDriver.Kache.clearCache();

					// Store the remaining bytes as the id.
					KosarSoloDriver.clientData.setID(Arrays.copyOfRange(reply,
							1, reply.length));
					KosarSoloDriver.clientData.setPorts(ports);
				} else {
					//Handle invalid messages from coordinator
				}
				System.out.print("Client ID after reconnection: ");
				for(byte b:KosarSoloDriver.clientData.getID()) {
					System.out.print(b+" ");
				}
				System.out.println();
				// Coordinator does not expect a reply so continue as usual
				KosarSoloDriver.getFlags().setCoordinatorConnected(true);
				
				KosarSoloDriver.setLastReconnectTime(System.currentTimeMillis());
				CacheModeController.enableQueryCaching();
				KosarSoloDriver.getLockManager().clearAll();

				KosarSoloDriver.pingThread.setSocket(socket);
				
				if (!KosarSoloDriver.pingThread.isRunning()){
					KosarSoloDriver.pingThread.stopThread();
					while (KosarSoloDriver.pingThread.isRunning()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
					}
					try {
						KosarSoloDriver.pingThread.start();
						while(!KosarSoloDriver.pingThread.isRunning()){
							try{
								Thread.sleep(100);
							}catch (Exception e) {
							}
						}
					} catch (IllegalThreadStateException e) {
						//do nothing
					}
				}
				break;
			} catch (ConnectException c) {
				System.out.println("connect exception");
				KosarSoloDriver.getConnectionPool().shutDown();
				introduceDelay(timeTryingToReconnect);;
			} catch (SocketTimeoutException e) {
				introduceDelay(timeTryingToReconnect);;
			} catch (IOException e) {
				e.printStackTrace();
				KosarSoloDriver.getConnectionPool().shutDown();
				introduceDelay(timeTryingToReconnect);;
			} catch (Exception e) {
				e.printStackTrace();
				KosarSoloDriver.getConnectionPool().shutDown();
				introduceDelay(timeTryingToReconnect);
			}
		}
		return true;
	}

	public int introduceDelay(int timeTryingToReconnect) {
		try {
			Thread.sleep(Constants.RECONNECT_TIME);
			timeTryingToReconnect+=Constants.RECONNECT_TIME;
			if(timeTryingToReconnect>59)
				CacheModeController.disableQueryCaching();
		} catch (InterruptedException e) {
		}
		return timeTryingToReconnect;
	}
}
