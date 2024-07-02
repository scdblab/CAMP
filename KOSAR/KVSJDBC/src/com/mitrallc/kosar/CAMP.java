package com.mitrallc.kosar;

import java.util.Hashtable;

public class CAMP {
	int MinPriority = 0;
	int MaxSize = 1;
	LRUQueue MinLRUQ = null;
	static CampRounding CR = new CampRounding();
	Hashtable<Integer,LRUQueue> LRUHT = new Hashtable<Integer,LRUQueue>(); //HashTable of costsize to lruqueue;
	BinaryHeap<HeapElt> PQ = new BinaryHeap<HeapElt>();

	private void AdjustMax(int NewSize){
		if (NewSize > MaxSize) MaxSize = NewSize;
		return;
	}

	private int ComputeCost(int MySize, int MyCost){
		int result = (int) (MaxSize * MyCost) / MySize;
		result += 1;
		return CR.RoundCost(result);
	}

	public void CAMP_Insert(dust elt, int MySize, int MyCost){
		AdjustMax( MySize );
		elt.setSize( MySize );
		int roundedcost = ComputeCost(MySize, MyCost);
		elt.setCostSize( roundedcost );
		elt.setPriority( MinPriority );
		LRUQueue LQ = LRUHT.get( roundedcost ); //Lookup LRUQueue corresponding to elt.getCostSize() in LRUHT;

		if (LQ == null){
			LQ = new LRUQueue(); //Construct a new LRUQueue
			LRUHT.put(roundedcost, LQ); //Insert elt.getCostSize() mapping to the new LRUQueue in LRUHT
		}

		//Insert the dust element in LQ
		LQ.Append(elt);

		if (LQ.getHeapEntry() == null){
			//HN=Create a HeapNode for this LRU Queue with the computed costsize and insert it in the Heap
			HeapElt HN = new HeapElt(roundedcost);
			PQ.add(HN);
			LQ.setHeapEntry(HN);
		}
	}

	public boolean CAMP_delete(dust elt){
		boolean results = true;
		int roundedcost = elt.getCostSize();  //Lookup the cost of item
		LRUQueue LQ = LRUHT.get(roundedcost); //Identify the LRU queue containing this item
		if (LQ == null) {
			System.out.println("Error, CAMP_delete invoked with an element that does not exist.");
			return false;
		} 
		
		int OldPriority = LQ.getLowestCost();  //Lookup the priority of item at the head of the queue
		LQ.Delete(elt);  //Delete elt from the LRU queue
		
		if (LRU.getHead() == null){
			HeapElt elt = LQ.getHeapEntry();
			//We must remove this entry
			PQ.add(elt);
			PQ.ChangeEltValue( elt, elt.getCost() );
		}
					IF1. delete the corresponding element from the priority queue
					IF2. set the LRU to null for garbage collection
					else 
						1. NewPriority = lookup the priority of item at the head of the queue
						2. IF (NewPriority != OldPriority) proceed to change the value of the heap 
						element with NewPriority and adjust the index structure.

						Update the minimum priority.
		return results;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
