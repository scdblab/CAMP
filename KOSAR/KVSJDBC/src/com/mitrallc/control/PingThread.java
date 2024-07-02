package com.mitrallc.control;

/**
 * This thread is responsible for sending a ping message to the coordinator
 * (when in multi-node mode) at regular intervals, so that the coordinator 
 * knows it is alive and connected.
 * 
 * The interval duaration can be changed in com.mitrallc.common.Constants.java
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;

public class PingThread extends Thread {
	private SockIO socket = null;
	private volatile boolean stopThread = false;
	private volatile boolean running;

	public PingThread() {
		setDaemon(true);
	}

	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Sets stop variable
	 */
	public void stopThread() {
		this.stopThread = true;
		this.interrupt();
	}

	@Override
	public void run() {
		this.running = true;

		while (!this.stopThread) {
			try {
				// Create and send register message
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ByteBuffer bb = ByteBuffer.allocate(4);
				bb.putInt(Constants.CLIENT_PING);
				baos.write(bb.array());
				bb.clear();
				baos.write(KosarSoloDriver.clientData.getID());
				socket.write(baos.toByteArray());
				socket.flush();

				// Sleep
				Thread.sleep(Constants.delta - Constants.epsilon);
			} catch (InterruptedException ie) {
				// do nothing
			} catch (Exception e) {
				KosarSoloDriver
						.startReconnectThread(System.currentTimeMillis());
			}
		}
		this.running = false;
		this.stopThread = false;
	}

	public SockIO getSocket() {
		return socket;
	}

	public void setSocket(SockIO socket) {
		this.socket = socket;
	}

}
