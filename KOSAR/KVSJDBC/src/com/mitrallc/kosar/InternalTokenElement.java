package com.mitrallc.kosar;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mitrallc.common.DynamicArray;
import com.mitrallc.sql.ResultSet;

/*
 * 
 * This class contains a hashmap of query strings to a temporary placeholder,
 * so that there are no duplicate query strings in the key set.
 * 
 * It also contains the dust elements and the Gumball data.
 */
public class InternalTokenElement {
	private ConcurrentHashMap<String, String> queryStringMap = new ConcurrentHashMap<String, String>();
	private boolean gumball = false;
	private long gumballTS = 0;
	
	public InternalTokenElement() {
	}
	public void setGumball(){
		gumballTS = System.currentTimeMillis();
		gumball = true;
	}
	public void clearGumball(){
		gumball = false;
	}
	
	public boolean isGumball(){
		return gumball;
	}
	
	public long getGumballTS(){
		return gumballTS;
	}
	public ConcurrentHashMap<String, String> getQueryStringMap() {
		return queryStringMap;
	}
	public void setQueryStringMap(ConcurrentHashMap<String, String> map) {
		queryStringMap = map;
	}
	public Set<String> getQueryStringKeySet() {
		return queryStringMap.keySet();
	}
	public void putInMap(String qry, String tmp) {
		queryStringMap.put(qry, tmp);
	}
	
}
