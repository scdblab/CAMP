package com.mitrallc.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;

/**
 * This class initializes the connection with the coordinator when the client is
 * initially started up.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
public class CoordinatorConnector implements Runnable {

	public void run() {
		// Get the IP address of the client
		KosarSoloDriver.getConnectionPool().setServer(KosarSoloDriver.core_ip + ":" + KosarSoloDriver.server_port);
		KosarSoloDriver.getConnectionPool().setInitConn(KosarSoloDriver.init_connections);
		byte[] registerMessage;
		while (true) {
			try {
				// Creates the socket connections
				KosarSoloDriver.getConnectionPool().initialize();
				// Sends the handshake message letting the coordinator know
				// that it is alive
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				SockIO socket = KosarSoloDriver.getConnectionPool().getSock();
				if (socket == null) {
					introduceDelay();
					continue;
				}
				// append message code
				ByteBuffer bb = ByteBuffer.allocate(4); 
				bb.putInt(Constants.CLIENT_REGISTER);
				baos.write(bb.array());
				bb.clear();
				bb.putInt(socket.getLocalIPAddress().length);
				baos.write(bb.array());
				bb.clear();
				baos.write(socket.getLocalIPAddress());
				
				// append ports
				byte[] ports;
				if((ports = KosarSoloDriver.getConnectionPool().getPorts()) == null) {
					ports = new byte[1];
					ports[0] = 0;
				}
				baos.write(ports);
				
			    bb.putInt(KosarSoloDriver.clientData.getInvalidationPort());
				baos.write(bb.array());
				// convert to byte array
				registerMessage = baos.toByteArray();

				// send message
				socket.write(registerMessage);
				socket.flush();
				
				byte[] reply = socket.readBytes();
				// wait for reply in which coordinator sends id.
				// then store id and change port information that is stored.

				// Ignore the first byte - that indicates the message type -
				// which we do not need to check here since it will always be an
				// assign id message.
				// Store the remaining bytes as the id.
				
				KosarSoloDriver.clientData.setID(Arrays.copyOfRange(reply, 1,
						reply.length-4));
				KosarSoloDriver.clientData.setPorts(ports);
				KosarSoloDriver.setNumReplicas(ByteBuffer.wrap(
						Arrays.copyOfRange(reply, reply.length-4,  reply.length)).getInt());
				
				System.out.print("Initial Client ID: ");
				for(byte b:KosarSoloDriver.clientData.getID()) {
					System.out.print(b+" ");
				}
				System.out.println();
				System.out.println("Num Replicas: " + KosarSoloDriver.getNumReplicas());
				
				// enable query caching
				KosarSoloDriver.getFlags().setCoordinatorConnected(true);
				CacheModeController.enableQueryCaching();
				KosarSoloDriver.getLockManager().clearAll();

				KosarSoloDriver.pingThread.setSocket(socket);
				if (!KosarSoloDriver.pingThread.isRunning()) {
					KosarSoloDriver.pingThread.start();
					while(!KosarSoloDriver.pingThread.isRunning()){
						try{
							Thread.sleep(100);
						}catch (Exception e) {
						}
					}
				}
				break;
			} catch (ConnectException c) {
				System.out.println("connection exception");
				KosarSoloDriver.getConnectionPool().shutDown();
				introduceDelay();
			} catch (SocketTimeoutException e) {
				System.out.println("socket timeout exception");
				KosarSoloDriver.getConnectionPool().shutDown();
				introduceDelay();
			} catch (IOException e) {
				System.out.println("io exception");
				KosarSoloDriver.getConnectionPool().shutDown();
				introduceDelay();
			} 
		}
	}

	public void introduceDelay() {
		CacheModeController.disableQueryCaching();
		try {
			Thread.sleep(Constants.RECONNECT_TIME);
		} catch (InterruptedException e) {
		}
	}
}
