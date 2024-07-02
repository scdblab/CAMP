
package com.camp.algorithm;


public class Request {

	public String key;
	public int index;
	public int size;
	public int cost;
	public boolean repeat;
	public boolean orig_repeat;
	
	public Request(){}
	
	public Request(int index, String key, int size, int cost, boolean repeat){	
		this.key = key;
		this.index = index;
		this.size = size;
		this.cost = cost;
		this.repeat = repeat;	
		this.orig_repeat = repeat;
	}
	
	public Request clone(){
		Request r = new Request();
		r.key = key;
		r.index = index;
		r.size = size;
		r.cost = cost;
		r.repeat = repeat;
		r.orig_repeat = repeat;
		return r;
	}
	
	public boolean isEqual(Request r){
		return r.key.compareTo(key) == 0;
	}
	
	public void setRepeat(boolean repeat){
		this.repeat = repeat;
	}
	
}
