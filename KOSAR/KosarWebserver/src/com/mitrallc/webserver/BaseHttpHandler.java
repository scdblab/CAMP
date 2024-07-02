package com.mitrallc.webserver;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.net.URLDecoder;

import com.sun.net.httpserver.*;

public class BaseHttpHandler implements HttpHandler {
	boolean verbose = false;
	String name = "";
	BaseSettings setting=new BaseSettings();
	HashMap<String,String> PageNameValue = new HashMap<String,String>();
	int refreshRate=-1;
	int swhitRate=-1;
	int rphitrate=-1;
	String boxValue="";
	String KosarValue="";
	String optionVal="";
	
	protected HashMap<String,String> ProcessRequestHdrBody(HttpExchange exch) {
		Vector queryselected = new Vector( );
		byte buf[] = null;
		String requestMethod = exch.getRequestMethod();
		HashMap<String, String> disabledqts = null;

		if (requestMethod.equalsIgnoreCase("POST")) {//if2
			Headers requestHeaders = exch.getRequestHeaders();		
			Set<String> keySet = requestHeaders.keySet();
			Iterator<String> iter = keySet.iterator();

			if (verbose){
				//Print the content of the request header
				while (iter.hasNext()){
					String key = iter.next();
					List values = requestHeaders.get(key);
					System.out.println(""+key+"="+values.toString());
				}
			}

			String qry=null;
			InputStream in = exch.getRequestBody();
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				buf = new byte[4096];
				for (int n = in.read(buf); n > 0; n = in.read(buf)) {
					out.write(buf, 0, n);
				}
				qry = new String(out.toByteArray()); 
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (verbose) System.out.println(qry);

			disabledqts = new HashMap<String,String>();
			String defs[] = qry.split("[&]");
			for (String def: defs) {
				def = DecodeQueries(def);
				int ix = def.lastIndexOf("=");
				String name;
				String value;
				if (ix < 0) {
					name = def;
					value = "";
				} else {
					name = def.substring(0, ix);
					value = def.substring(ix+1);
				}
				//Identify the disabled query templates in a hash table
				disabledqts.put(name, value);

				if (verbose) System.out.println(""+name+" = "+value);
			}
		}
		return disabledqts;
	}
	
	private String DecodeQueries(String inval){
		String result = inval.replace("%28", "(");
		result = result.replace("%29", ")");
		result = result.replace("+", " ");
		result = result.replace("%3D", "=");
		result = result.replace("%2C", ",");
		result = result.replace("%3F", "?");
		return result.trim();
	}
	
	public void handle(HttpExchange exch) throws IOException {

		String output;
		URI uri = exch.getRequestURI();
		if(verbose) {
			System.out.println("Got connection.");
			System.out.println(uri.toString());
		}
		String response = "Path: " + uri.getPath() + "\n";
		PageNameValue = ProcessRequestHdrBody(exch);
		output = this.getContent(uri);
		Headers responseHeaders = exch.getResponseHeaders();
		responseHeaders.set("Content-Type", "text/html");	
		exch.sendResponseHeaders(200, output.length());
		OutputStream os = exch.getResponseBody();
		byte[] buffer = output.getBytes();
		if (buffer.length > 0) {
			os.write(buffer);
		}
		os.close();
	}

	private String getContent(URI uri) {
		String output = null;
		switch (uri.getPath()){
		case "/":
			BaseMainpage main=new BaseMainpage();
			main.setName(name);
			output = main.content();
			break;
		case "/Setting":
			adjustRefreshedSettings(PageNameValue);
			setting.setName(name);
			output = setting.content();
			break;
		default:
			BaseMainpage emp=new BaseMainpage();
			emp.setName(name);
			output = emp.content();
			break;
		}
		return output;
	}
	
	protected void adjustRefreshedSettings(HashMap PageNameValue) {
		//Nothing to process
		if (PageNameValue == null) return;

		//Iterate over the PageNameValue HashMap to process its elements
		Iterator<String> keys = PageNameValue.keySet().iterator();

		while(keys.hasNext()) { 
			String name = keys.next(); 
			doSetting(name, setting);
		}
	}
	private void doSetting(String name2, BaseSettings setting) {
		// TODO Auto-generated method stub
		String value = PageNameValue.get(name2).toString();
		switch (name2) {
		case "kosarOnOff":
			setting.setKosarOnOff(value);
			break;
		case "kvsFlush":
			//Turn KOSAR off and reset the data structures
			setting.doResetKosar();
			break;
		case "refresh":
			refreshRate = Integer.parseInt(value);
			setting.setRefreshVal(refreshRate);
			break;
		case "sw-hitrate":
			swhitRate = Integer.parseInt(value);
			setting.setSlidingWindowVal(swhitRate);
			break;
		case "rp-hitrate":
			rphitrate = Integer.parseInt(value);
			setting.setGranularity(rphitrate);
			break;
		case "boxBytes":
			boxValue="Bytes";
			setting.setBox(boxValue);
			break;
		case "boxKBytes":
			boxValue="KBytes";
			setting.setBox(boxValue);
			break;
		case "boxMBytes":
			boxValue="MBytes";
			setting.setBox(boxValue);
			break;
		case "boxGBytes":
			boxValue="GBytes";
			setting.setBox(boxValue);
			break;
		case "cooperation":
			optionVal=value;
			setting.setNumReplicas(Integer.parseInt(value));
			setting.setoptionVal(optionVal);
			break;
		default:
			System.out.println("Error, case is unknown.");
			break;
		}
		if(verbose) System.out.println(""+name2+" = "+value);
	}


	protected void setName(String name) {
		this.name = name;
	}
}
