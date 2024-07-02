package com.mitrallc.communication;

/**
 * This class listens for invalidation connections from the coordinator.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;

public class InvalidationClientServer implements Runnable {
	public static ServerSocket server = null;

	@Override
	public void run() {
		Socket socket = null;
		try {
			server = new ServerSocket(0);
			System.out.println("Invalidation active on port: "
					+ server.getLocalPort());
			KosarSoloDriver.clientData.setInvalidationPort(server
					.getLocalPort());
			while (KosarSoloDriver.getFlags().coordinatorExists()) {
				try {
					// wait for a connection from a client
					socket = server.accept();
					new InvalidationHandler(new SockIO(socket, 0)).start();

				} catch (Exception e) {
					System.out.println("Coordinator server closed");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != server && !server.isClosed())
					server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
