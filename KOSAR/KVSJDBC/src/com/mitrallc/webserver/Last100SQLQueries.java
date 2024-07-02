package com.mitrallc.webserver;

public class Last100SQLQueries {
	final int SIZE = 100;
	String[] queries = new String[SIZE];
	int counter = 0;
	
	public void add(String newQuery) {
		int idx = counter%SIZE;
		if (idx >= 0 && idx < SIZE) queries[idx] = newQuery;
		counter++;
		if (counter < 0) counter=1;
	}
	
	public String getQueryList() {
		String message = "";
		message += "<ol>";
		for(String query : queries) {
			if(query != null)
				message += "<li><p class=\"align\">" + query + "</p></li>";
		}
		message += "</ol>";
		
		return message;
	}
}
