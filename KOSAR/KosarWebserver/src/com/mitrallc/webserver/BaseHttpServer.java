package com.mitrallc.webserver;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.TimeZone;

import com.sun.net.httpserver.*;

public class BaseHttpServer {
	HttpServer server;
	public static Calendar startTime=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	public BaseHttpServer() {}
	public BaseHttpServer(int port, String name, BaseHttpHandler handler) {
		
		
		handler.setName(name);
		try {
			InetAddress ip = InetAddress.getLocalHost();
			server = HttpServer.create(new InetSocketAddress(ip, port), 1);
			server.createContext("/", handler);
			server.start();
		} catch (IOException e) {
			System.err.println("Problem in creating server at localhost:8080");
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BaseHttpServer myServer = new BaseHttpServer(8080, "BASE", new BaseHttpHandler());
	}

}
