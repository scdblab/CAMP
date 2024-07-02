package com.mitrallc.kosar;

public class HeapElt implements Comparable<HeapElt> {
	private int CostVal=0;
	private LRUQueue lruq;
	
	public boolean equals (HeapElt e){
		if (CostVal == e.getCost()) return true;
		return false;
	}
	public int compareTo(HeapElt e){
		return this.CostVal - e.CostVal;
	}
	public void setCost(int Cost){
		CostVal = Cost;
	}
	public int getCost() {
		return CostVal;
	}
	public void printCost() {
		System.out.println("Cost = "+getCost());
	}
	public HeapElt (int Cost) {
		// Java doesn't allow construction of arrays of placeholder data types 
		CostVal = Cost;
	}
}
