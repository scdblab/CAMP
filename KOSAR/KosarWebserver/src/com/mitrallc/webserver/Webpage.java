package com.mitrallc.webserver;

/*
 * This is the most basic class for the Webpage.
 * Contains a name describing that webpage, which is
 * set in the banner of that webpage.
 * 
 */
public class Webpage {
	String name;
	String menuString;
	public Webpage() {
		menuString = "<li><a href=\"mainpage\">Performance Metrics</a></li>"+
                "<li><a href=\"Triggers\">Triggers</a></li>"+
                "<li><a href=\"SQL\">SQL Stats</a></li>"+
                "<li><a href=\"Setting\">Settings</a></li>"+
                "<li><a href=\"/LastQueries\">Last 100 Queries</a></li>";
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
		
	}
	public String getMenuString() {
		return menuString;
	}
	public void setMenuString(String menu) {
		this.menuString = menu;
	}
}
