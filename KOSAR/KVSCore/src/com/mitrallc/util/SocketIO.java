package com.mitrallc.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * SocketIO manages socket operations performed by the CORE.
 * This includes the read/write actions performed by Data Input/Output Streams.
 * 
 * Take care to understand the usage of each method.
 * For example, writeBytes always writes the length of the message (4 byte int) first
 * and readBytes always reads in the length of the message (4 byte int) first.
 */
public class SocketIO {

	Socket socket;
	
	DataInputStream in = null;
	DataOutputStream out = null;
	ByteArrayOutputStream baos = null;
	
	public SocketIO(Socket sock) throws IOException {
		this.socket = sock;
		this.in = new DataInputStream(sock.getInputStream());
		this.out = new DataOutputStream(sock.getOutputStream());
		this.baos = new ByteArrayOutputStream();
	}
	
	public void writeByte(byte val) throws IOException {
		if(socket == null || !socket.isConnected())
			throw new IOException("Error: Attempting to read from closed socket");
		
		out.write((byte)val);
		out.flush();
	}

	public void writeBytes(byte[] val) throws IOException {
		if(socket == null || !socket.isConnected())
			throw new IOException("Error: Attempting to read from closed socket");
		
		baos.reset();
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(val.length);
		baos.write(bb.array());
		baos.write(val);
		baos.flush();
		
		out.write(baos.toByteArray());
		out.flush();
		
		baos.reset();
	}
	
	public void writeInt(int returnVal) throws IOException{
		if(socket == null || !socket.isConnected())
			throw new IOException("Error: Attempting to read from closed socket");
		
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(returnVal);
		
		out.write(bb.array());
		out.flush();
		// This socket is shared, and should not be closed.
		// s.close();
	}
	
	public byte readByte() throws IOException {
		if(socket == null || !socket.isConnected())
			throw new IOException("Error: Attempting to read from closed socket");
		
		return in.readByte();
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
	
	public int read(byte[] b) throws IOException {
		if (socket == null || !socket.isConnected()) {
			throw new IOException(
					"++++ attempting to read from closed socket");
		}
		
		int count = 0;
		while (count < b.length) {
			int cnt = in.read(b, count, (b.length - count));
			count += cnt;
		}
		return count;
	}
	public short readShort() throws IOException {
		if (socket == null || !socket.isConnected()) {
			throw new IOException(
					"++++ attempting to read from closed socket");
		}
		return in.readShort();
	}
	
	public int readInt() throws EOFException, IOException {
		if(socket == null || !socket.isConnected())
			throw new IOException("Error: Attempting to read from closed socket");
		try {
			return in.readInt();
		} catch(EOFException eof) {
			System.out.println("End of File.");
		}
		return 0;
	}

	public void closeAll() throws IOException {
		if(socket == null || !socket.isConnected())
			throw new IOException("Error: Attempting to read from closed socket");
		
		socket.close();
		out.close();
		in.close();
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public DataOutputStream getOut() {
		return out;
	}
}
