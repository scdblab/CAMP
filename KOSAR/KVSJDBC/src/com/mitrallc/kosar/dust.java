package com.mitrallc.kosar;

import com.mitrallc.sql.KosarSoloDriver;


public class dust {
	String key=null;
	com.mitrallc.sql.ResultSet cached_rowset = null;
	Object payload=null;
	private long lastWrite = 0;
	long gumballTS = 0;
	boolean gumball = false;

	dust prev = null;  //implements the LRU cache replacement policy
	dust next = null;  //implements the LRU cache replacement policy

	private int MySize=0;
	private int CostSize=0;
	private int Priority=0;

	private String QueryTemplate=null;

	public String getQueryTemplate() {
		return QueryTemplate;
	}

	public void setQueryTemplate(String queryTemplate) {
		QueryTemplate = queryTemplate;
	}

	/*	
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
	}*/
	public void setPriority(int prio){
		Priority = prio;
		return;
	}

	public int getPriority(){
		return Priority;
	}

	public void setCostSize(int CsSz){
		CostSize = CsSz;
		return;
	}

	public int getCostSize(){
		return CostSize;
	}

	public void setSize(int sz){
		MySize = sz;
		return;
	}

	public int getSize(){
		return MySize;
	}

	public void setNext(dust p){
		this.next = p;
	}

	public dust getNext(){
		return this.next;
	}

	public void setPrev(dust p){
		this.prev = p;
	}

	public dust getPrev(){
		return this.prev;
	}

	public void setKey(String k){
		key = k;
	}

	public String getKey(){
		return key;
	}

	public void setPayLoad (Object pl){
		payload = pl;
	}

	public void setRS (com.mitrallc.sql.ResultSet cr){
		//KosarSoloDriver.getLockManager().lockKey(key);
		lastWrite = System.currentTimeMillis();
		cached_rowset = cr;
		//KosarSoloDriver.getLockManager().unlockKey(key);
	}

	public com.mitrallc.sql.ResultSet getRS() {
		com.mitrallc.sql.ResultSet localRS = null;
		//KosarSoloDriver.getLockManager().lockKey(key);
		localRS = cached_rowset;
		//KosarSoloDriver.getLockManager().unlockKey(key);
		return localRS;
	}

	public void setLastWrite(long Tmiss){
		lastWrite=Tmiss;
	}

	public long getLastWrite(){
		return this.lastWrite;
	}
}
