package com.camp.algorithm;

public class LRUQueue {
	private dust head = null;
	private dust tail = null;
	private HeapElt HeapEntry=null;
	private int roundedCost;
	public int count = 0;


	public int size(){
		return count;
	}
	
	public int getRoundedCost(){
		return roundedCost;
	}
	
	public HeapElt getHeapEntry(){
		return HeapEntry;
	}
	
	public dust peek(){
		return head;
	}
	
	public void setRoundedCost(int cost){
		roundedCost = cost;
	}
	
	public void setHeapEntry(HeapElt elt){
		HeapEntry=elt;
	}
	
	public boolean add(dust elt){
		count++;
		if (tail == null) tail = elt;
		else {
			tail.setNext(elt);
			elt.setPrev(tail);
			tail = elt;
			elt.setNext(null);
		}

		if (head == null) head = tail;
		return true;
	}
	
	public boolean remove(dust elt){
		count--;
		if (elt.getNext() != null) elt.getNext().setPrev(elt.getPrev());
		if (elt.getPrev() != null) elt.getPrev().setNext(elt.getNext());
		if (head == elt) head = elt.getNext();
		if (tail == elt) tail = elt.getPrev();
		elt.setNext(null);
		elt.setPrev(null);
		return true;
	}
	
	public boolean isEmpty(){
		return head == null;
	}
}