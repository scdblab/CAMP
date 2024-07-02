package com.meetup.memcached.test;
/**                                                                                                                                                                                
 * Copyright (c) 2012 USC Database Laboratory All rights reserved. 
 *
 * Authors:  Sumita Barahmand and Shahram Ghandeharizadeh                                                                                                                            
 *                                                                                                                                                                                 
 * Licensed under the Apache License, Version 2.0 (the "License"); you                                                                                                             
 * may not use this file except in compliance with the License. You                                                                                                                
 * may obtain a copy of the License at                                                                                                                                             
 *                                                                                                                                                                                 
 * http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                      
 *                                                                                                                                                                                 
 * Unless required by applicable law or agreed to in writing, software                                                                                                             
 * distributed under the License is distributed on an "AS IS" BASIS,                                                                                                               
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or                                                                                                                 
 * implied. See the License for the specific language governing                                                                                                                    
 * permissions and limitations under the License. See accompanying                                                                                                                 
 * LICENSE file.                                                                                                                                                                   
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class CacheUtilities {
	public static enum ListenerCmdType {
		START, MONGO, MEMCACHED_START, MEMCACHED_STOP, MEMCACHED_START_CUSTOM
	}
	
	
	public static final boolean LOCK_TABLE_EXPLICIT 	= true;	// Manually lock tables to disable effect of MVCC not working with Gumball
	
	public static boolean USE_LISTENER_START_CACHE 		= true;
	private static final int COSAR_WAIT_TIME 			= 10000;
	
	
	public static void runListener(String listener_hostname, int listener_port, String command)
	{
		runListener(listener_hostname, listener_port, ListenerCmdType.START, command);
	}
	
	public static void startMemcached(String listener_hostname, int listener_port)
	{
		runListener(listener_hostname, listener_port, ListenerCmdType.MEMCACHED_START, "");
	}	
	
	public static void startMemcached(String listener_hostname, int listener_port, String command)
	{
		runListener(listener_hostname, listener_port, ListenerCmdType.MEMCACHED_START_CUSTOM, command);
	}
	
	public static void stopMemcached(String listener_hostname, int listener_port)
	{
		runListener(listener_hostname, listener_port, ListenerCmdType.MEMCACHED_STOP, "");
	}
	
	public static void startCOSAR(String cache_hostname, int listener_port)
	{
		String command = "C:\\cosar\\configurable\\TCache.NetworkInterface.exe C:\\cosar\\configurable\\V2gb.xml ";
		runListener(cache_hostname, listener_port, command);
		

		System.out.println("Wait for "+COSAR_WAIT_TIME/1000+" seconds to allow COSAR to startup.");
		try{
			Thread.sleep(COSAR_WAIT_TIME);
		}catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
		
	public static void runListener(String listener_hostname, int listener_port, ListenerCmdType type, String command)
	{
		DataInputStream in = null;
		DataOutputStream out = null;
		
		try {
			
			Socket conn = new Socket(listener_hostname, listener_port);
			in = new DataInputStream(conn.getInputStream());
			out = new DataOutputStream(conn.getOutputStream());
			
			switch(type)
			{
			case START:
				out.writeBytes("start ");
				break;
			case MONGO:
				out.writeBytes("mongo ");				
				break;
			case MEMCACHED_START:
				out.writeBytes("memch ");
				command = "start";
				break;
			case MEMCACHED_START_CUSTOM:
				out.writeBytes("memch ");
				command = "startcustom " + command;
				break;
			case MEMCACHED_STOP:
				out.writeBytes("memch ");
				command = "stop";
				break;				
			default:
				break;
			}
			
			out.writeInt(command.length());
			out.writeBytes(command);
			out.flush();
			
			
			int response = in.readInt();
			if(response != 0)
			{
				System.out.println("Error starting process");
			}
			
			out.close();
			in.close();
			conn.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
