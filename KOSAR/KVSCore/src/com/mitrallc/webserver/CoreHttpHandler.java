package com.mitrallc.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class CoreHttpHandler extends BaseHttpHandler{
	
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
			BaseMainpage main=new CoreMainpage();
			main.setName(name);
			output = main.content();
			break;
		case "/Setting":
			setting=new CoreSettingsPage();
			adjustRefreshedSettings(PageNameValue);
			setting.setName(name);
			setting.setReplicationString();
			output = setting.content();
			break;
		case "/Triggers":
			BaseTriggers triggers = new CoreTriggersPage();
			triggers.setName(name);
			triggers.getRegisteredTriggers();
			output = triggers.content();
			break;
		case "/SQL":
			BaseSQL sql = new CoreSQLPage();
			sql.setName(name);
			sql.getSQLStats();
			output = sql.content();
			break;
		case "/LastQueries":
			BaseLast100Queries last100Queries = new CoreLast100QueriesPage();
			last100Queries.setName(name);
			last100Queries.getLast100QueryList();
			output = last100Queries.content();
			break;
		default:
			BaseMainpage emp=new CoreMainpage();
			emp.setName(name);
			output = emp.content();
			break;
		}
		return output;
	}
}
