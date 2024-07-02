package com.mitrallc.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientConnector extends Thread{

	int serverPort;
	ServerSocket ss;
	public ClientConnector(int port) {
		this.serverPort = port;
	}
	public void run() {
		
		Socket s = null;
		try {
			ss = new ServerSocket(serverPort);
			while(true) {
				s = ss.accept();
				ClientHandler ch = new ClientHandler(s);
				ch.start();
			}
		} catch (IOException ioex) {
			ioex.printStackTrace();
		} finally {
			try {
				if (null != ss && !ss.isClosed())
					ss.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
