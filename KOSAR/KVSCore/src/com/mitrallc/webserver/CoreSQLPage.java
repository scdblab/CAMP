package com.mitrallc.webserver;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

import com.mitrallc.core.KosarCore;

public class CoreSQLPage extends BaseSQL {
	public void getSQLStats() {
		sqlStats += "<p>"+KosarCore.queryToClientsMap.size() + " queries tracked. </p>";
		Iterator iterator = KosarCore.queryToClientsMap.keySet().iterator();
		int count = 0;
		int runningNoClients = 0;
		
		try {
			while (iterator.hasNext()) {  
				count++;
				//System.out.println(count);
				ByteBuffer key = (ByteBuffer) iterator.next();  
				Object[] value = (Object[]) KosarCore.queryToClientsMap.get(key);  
				sqlStats += "<dl>"+
						new String(key.array(), "UTF-8") +
						"</dt>";
				if(value != null) {
					for(Object o : value) {
						if(o != null) {
							byte[] id = ((ByteBuffer)o).array();
							sqlStats += "<dd>    " + Arrays.toString(id) + " : ";
							Object ip;
							if((ip = KosarCore.clientToIPMap.get(ByteBuffer.wrap(id))) != null) {
								sqlStats += Arrays.toString(((ByteBuffer)ip).array());
							}
							sqlStats += "</dd>";
						}
					}	
				}
				sqlStats+="</dl>";
			}
		} catch(UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}
	}
}
