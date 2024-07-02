package com.camp.algorithm;

import java.util.Comparator;


public class HeapElt implements Comparable<HeapElt> {
	private long Priority = 0;
	private LRUQueue lruq;
	
	public boolean equals (HeapElt e){
		if (Priority == e.getPriority()) return true;
		return false;
	}
	/* public int compareTo(HeapElt e){
		return this.Priority - e.Priority;
	}*/
	public void setPriority(long Priority){
		this.Priority = Priority;
	}
	
	public int compareTo(HeapElt e){
		
		if(this.Priority > e.Priority)
			return 1;
		else if ( this.Priority < e.Priority)
			return -1;
		else
			return 0;
		
	}
	
	/*
	@Override
	public int compare(HeapElt o1, HeapElt o2) {
		if(o1.Priority > o2.Priority)
			return 1;
		else if ( o1.Priority < o2.Priority)
			return -1;
		else
			return 0;
	}
	*/
	
	public long getPriority() {
		return Priority;
	}
	public void printCost() {
		System.out.println("Cost = "+ getPriority());
	}
	public HeapElt (long Priority) {
		// Java doesn't allow construction of arrays of placeholder data types 
		this.Priority = Priority;
	}
	
	public void setQueue(LRUQueue queue){
		lruq = queue;
	}
	
	public LRUQueue getQueue(){
		return lruq;
	}
}
