package com.mitrallc.kosar;
import com.mitrallc.kosar.*;

import com.mitrallc.sql.KosarSoloDriver;
import com.mitrallc.sqltrig.QTmeta;

public class LRUQueue {
	private dust tail=null;
	private dust head=null;
//	private HeapElt HeapEntry=null;
//	
//	public HeapElt getHeapEntry(){
//		return HeapEntry;
//	}
//	
//	public void setHeapEntry(HeapElt elt){
//		HeapEntry=elt;
//	}
	
	public int getLowestCost(){
		dust elt = this.head;
		return elt.getCostSize();
	}
	
	public synchronized boolean EvictHead(){
		if (this.head == null) return false;
		dust elt = this.head;
		if(KosarSoloDriver.webServer != null)
			KosarSoloDriver.KosarEvictedKeysEventMonitor.newEvent(1);
		this.Delete(this.head);
		//Remove the element from the RS
		com.mitrallc.kosar.kosar.DeleteCachedQry(elt.getKey());
		
		//Remove the element from an instance of the Query Template
		QTmeta qtelt = com.mitrallc.sqltrig.QueryToTrigger.TriggerCache.get(elt.getQueryTemplate());
		qtelt.deleteQInstance(elt.getKey());
		return true;
	}

	public synchronized void Append(dust elt){
		//Inserts dust element at the tail of the LRU queue
		elt.setNext(null);
		elt.setPrev(tail); //Set the prev to the tail of the lru queue
		if (head == null) head = elt; //Empty LRU queue
		else tail.setNext(elt);
		tail = elt;
	}
	
	public synchronized void Delete (dust elt){
		//Deletes dust element from the LRU queue
		if (elt.getNext() != null) elt.getNext().setPrev( elt.getPrev() );
		else tail = elt.getPrev();

		if (elt.getPrev() != null) elt.getPrev().setNext( elt.getNext() );
		else head = elt.getNext();
	}
}
