package com.mitrallc.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;


import com.mitrallc.kosar.kosar;
import com.mitrallc.sql.KosarSoloDriver;
import com.sun.net.httpserver.*;

public class MyHttpHandler extends BaseHttpHandler {
	boolean verbose = false;

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

	/**
	 * Overrides BaseHttpHandler's getContent() method, with two new pages:
	 * 		LastIDU and CachedQueries
	 * @param uri
	 * @return
	 */
	private String getContent(URI uri) {
		String output;
		switch (uri.getPath()){
		case "/":
			ClientMainpage main=new ClientMainpage();
			main.setName(name);
			main.setMenuString(getMenuString());
			output = main.content();
			break;
		case "/Triggers":
			BaseTriggers triggers=new ClientTriggersPage();
			triggers.setName(name);
			triggers.setMenuString(getMenuString());
			triggers.getRegisteredTriggers();
			output = triggers.content();
			break;
		case "/SQL":
			BaseSQL sql=new ClientSQLPage();
			sql.setName(name);
			sql.setMenuString(getMenuString());
			sql.getSQLStats();
			output = sql.content();
			break;
		case "/Cached":
			ClientCachedPage cached=new ClientCachedPage();
			cached.setName(name);
			cached.setMenuString(getMenuString());
			adjustCachedStatic(PageNameValue);
			output = cached.content();
			break;
		case "/Setting":
			setting = new ClientSettingsPage();
			setting.setName(name);
			setting.setMenuString(getMenuString());
			adjustRefreshedSettings(PageNameValue);
			output = setting.content();

			break;
		case "/LastQueries":
			BaseLast100Queries lastQ=new ClientLast100QueriesPage();
			lastQ.setName(name);
			lastQ.setMenuString(getMenuString());
			lastQ.getLast100QueryList();
			output = lastQ.content();

			break;
		case "/LastIDU":
			ClientLastIDUPage lastidu=new ClientLastIDUPage();
			lastidu.setName(name);
			lastidu.setMenuString(getMenuString());
			output = lastidu.content();
			break;
		default:
			ClientMainpage emp=new ClientMainpage();
			emp.setName(name);
			emp.setMenuString(getMenuString());
			output = emp.content();
			break;
		}
		return output;
	}

	/**
	 * This method is not an inheritance extension of the Webpage.java
	 * instance of getMenuString().  However, this method is overridden
	 * with options for two pages: Cached and LastIDU.  This method is passed
	 * to the necessary classes in the handle() method above.
	 * @return
	 */
	public static String getMenuString() {
		return  "<li><a href=\"mainpage\">Performance Metrics</a></li>"+
                "<li><a href=\"Cached\">Cached Queries</a></li>"+
                "<li><a href=\"Triggers\">Triggers</a></li>"+
                "<li><a href=\"SQL\">SQL Stats</a></li>"+
                "<li><a href=\"Setting\">Settings</a></li>"+
                "<li><a href=\"/LastQueries\">Last 100 Queries</a></li>"+
				"<li><a href=\"/LastIDU\">Last 100 Insert/Delete/Update</a></li>";
	}

	private void adjustCachedStatic(HashMap<String,String> PageNameValue) {
		//Nothing to process
		if (PageNameValue == null) return;
		com.mitrallc.sqltrig.QueryToTrigger.UpdateTriggerCache(PageNameValue);

	}
	
}
