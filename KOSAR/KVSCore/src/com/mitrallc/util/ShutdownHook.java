package com.mitrallc.util;

import java.io.IOException;
import java.sql.SQLException;

public class ShutdownHook extends Thread {
	String type;
	Object classObject;

	public ShutdownHook(Object classObject, String type) {
		this.type = type;
		this.classObject = classObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run() Shutdown Hook properly handles streams in the
	 * event of an improper ending of the jar process.
	 */
	public void run() {
		if(type.contains("KosarCore")) {
			com.mitrallc.core.KosarCore core = ((com.mitrallc.core.KosarCore) classObject);
			try {
				core.getConn().close();
				core.getServer().close();
				core.getTriggerRegThread().interrupt();
			} catch (Exception e) {
				System.out
						.println("Error: Shutdown Hook - Failed to shut down");
			}
			System.out.println("KosarCore Thread Ended.");
		}
		else if(type.contains("RequestHandler")) {
			com.mitrallc.core.RequestHandler rh = ((com.mitrallc.core.RequestHandler) classObject);
			try {
				String ip = rh.getSocket().getSocket().getInetAddress().getHostAddress();
				com.mitrallc.core.KosarCore.clientToIPMap.remove(rh.getClientID());
				rh.getSocket().closeAll();
			} catch (IOException e1) {
				System.out.println("Error: ShutdownHook - Failed to close socket streams for RequestHandler");
				e1.printStackTrace();
			}
			System.out.println("RequestHandler for Client " + rh.getClientID() + " Ended.");
		}
		else if(type.contains("TriggerRegisterThread")) {
			System.out.println("Trigger Register Thread Ended.");
		}
		else {
			System.out
					.println("Error: Unknown class: " + classObject.toString() + " trying to use shutdown hook.");
		}
	}
}
