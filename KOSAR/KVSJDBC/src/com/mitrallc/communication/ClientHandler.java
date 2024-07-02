package com.mitrallc.communication;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;

public class ClientHandler extends Thread{

	SocketIO socket = null;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	boolean verbose = false;
	byte[] clientID = null;
	
	public ClientHandler(Socket s) {
		try {
			this.socket = new SocketIO(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void setClientID(byte[] clientID) {
		this.clientID = clientID;
	}
	public void run() {
		byte[] request = null;
		String command;
		while(true) {
			try {
				/*
				 * Format from the requesting client:
				 * command length
				 * command
				 * sql
				 */
				request = socket.readBytes();
				
				//Parse Command
				int commandLength = ByteBuffer.wrap(Arrays.copyOfRange(request, 0, 4)).getInt();
				
				//if the command length is 0, then close socket;
				if(commandLength == 0)
					break;
				
				command = new String((Arrays.copyOfRange(request, 4, 4+commandLength)),"UTF-8");
				
				request = Arrays.copyOfRange(request, 4+commandLength, request.length);
				
				//Get sql query
				String sql = new String(request, "UTF-8");
				
				//Look for sql in the cache
				int rsFound = -1;
				com.mitrallc.sql.ResultSet rs = KosarSoloDriver.Kache.GetQueryResult(sql);
				
				//If the asking client sends the command to steal the 
				if(command.equalsIgnoreCase("steal")) {
					KosarSoloDriver.Kache.DeleteDust(sql);
					new NotifyCore(request).start();
					if(verbose)
						KosarSoloDriver.stealCount++;
					
				}
				else if(command.equalsIgnoreCase("copy")) {
					if(verbose)
						KosarSoloDriver.copyCount++;
				}
				
				//If resultset is not null, 
				if(rs != null) {
					//success message is 0
					rsFound = 0;
				}
				
				/*
				 * Message to Client:
				 * msg (4 bytes as int)
				 */
				ByteBuffer bb = ByteBuffer.allocate(4);
				bb.putInt(rsFound);
				baos.write(bb.array());
				bb.clear();
				if(rs != null)
					baos.write(rs.serialize());
				baos.flush();
				socket.writeBytes(baos.toByteArray());
				socket.flush();
				baos.reset();
				if(verbose) {
					System.out.println("CopyCount = " + KosarSoloDriver.copyCount);
					System.out.println("StealCount = " + KosarSoloDriver.stealCount);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	class NotifyCore extends Thread {
		byte[] sql = null;
		SockIO sock = null;
		NotifyCore(byte[] sql) {
			this.sql = sql;
		}
		public void run() {
			try {
				while(true) {
					sock = KosarSoloDriver.getConnectionPool().getSock();
					if(sock == null) {
						introduceDelay();
						continue;
					}
					else {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ByteBuffer bb = ByteBuffer.allocate(4);
						bb.putInt(5);
						baos.write(bb.array());
						bb.clear();
						bb.putInt(sql.length);
						baos.write(bb.array());
						bb.clear();
						baos.write(sql);
						baos.write(KosarSoloDriver.clientData.getID());
						baos.flush();
						sock.write(baos.toByteArray());
						sock.flush();
						break;
					}
				}
			} catch (ConnectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if(sock != null) {
					sock.close();
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
	class SocketIO {
		Socket socket;
		DataInputStream in = null;
		DataOutputStream out = null;
		ByteArrayOutputStream baos = null;
		SocketIO(Socket sock) throws IOException {
			this.socket = sock;
			this.in = new DataInputStream(sock.getInputStream());
			this.out = new DataOutputStream(sock.getOutputStream());
			this.baos = new ByteArrayOutputStream();
		}
		public byte[] readBytes() throws IOException {
			if(socket == null || !socket.isConnected())
				throw new IOException("Error: Attempting to read from closed socket");
			
			byte[] input = null;
			int length = 0;
			int bytesRead = 0;
			try {
				length = readInt();
				input = new byte[length];
				if (in != null && length > 0) {
					while (bytesRead < length) {
						input[bytesRead++] = in.readByte();
					}
				}
			} catch (EOFException eof) {
				System.out.println("EOF");
			} catch (IndexOutOfBoundsException i) {
				System.out.println("Error: SocketIO - Out of bounds.");
				System.out.println("Length: "+length+" bytesRead: "+bytesRead);
				i.printStackTrace();
			}
			return input;
		}
		public int readInt() throws EOFException, IOException {
			if(socket == null || !socket.isConnected())
				throw new IOException("Error: Attempting to read from closed socket");
			try {
				return in.readInt();
			} catch(EOFException eof) {
				System.out.println("End of File.");
			} catch(IOException io) {
				System.out.println("I/O Disconnection from Client.");
				if(socket != null) {
					socket.close();
					in.close();
					out.close();
				}
			}
			return 0;
		}
		public void writeBytes(byte[] val) throws IOException {
			if(socket == null || !socket.isConnected())
				throw new IOException("Error: Attempting to read from closed socket");
			
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(val.length);
			baos.write(bb.array());
			baos.write(val);
			baos.flush();
			
			out.write(baos.toByteArray());
			out.flush();
			
			baos.reset();
		}
		public void flush() throws IOException {
			if (socket == null || !socket.isConnected()) {
				throw new IOException(
						"++++ attempting to write to closed socket");
			}
			out.flush();
		}
	}
	
}
