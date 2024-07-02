package com.mitrallc.communication;
/**
 * This class takes care of removing keys value pairs that are no longer valid.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import com.mitrallc.common.Constants;
import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sql.SockIOPool.SockIO;

public class InvalidationHandler extends Thread {
	SockIO socket = null;
	public InvalidationHandler(SockIO sockIO) {
		this.socket = sockIO;
	}

	@Override
	public void run() {
		byte request[] = null;
		try {
			while (true) {
				try {
		    		request = socket.readBytes();
					if (null == request) {
						// failure
						request = new byte[1];
						request[0] = -1;
						socket.write(request);
						socket.flush();
						continue;
					}
					int i=0;
					
					try{
						while(i<request.length) {
							ByteArrayOutputStream bao = new ByteArrayOutputStream();
							while (i < request.length && request[i] != '#') {
								bao.write(request[i]);
								i++;
							}
							String key = new String(bao.toByteArray());
							//writeToFile(key);
							//get a lock on the key
							//delete the key from cache
							KosarSoloDriver.Kache.DeleteCachedQry(key);
							
							//unlock the key
							i++;
						}
						
					} catch(Exception e){
						System.out.println("Exception in the client while processing invalidate requests: "+e.getMessage());
						e.printStackTrace();
					} finally {
						socket.write(1);
						socket.flush();

					}
				} catch (EOFException eof) {
					System.out.println("client closed the socket");
					break;
				} catch (SocketException se) {
					se.printStackTrace();
					break;
				} catch (SocketTimeoutException se) {
					se.printStackTrace();
					continue;
				} catch(IOException i) {
					i.printStackTrace();
					break;
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private synchronized void writeToFile(String key) {
		
		File file = new File("InvalidateKeysReceived.csv");
		
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter fw;
		try {
			fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.append(key+"\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}			
		
	}

}
