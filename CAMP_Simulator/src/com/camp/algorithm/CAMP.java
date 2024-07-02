package com.camp.algorithm;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.System;

public class CAMP {

	public long cacheSize;
	public AtomicLong currentCacheUsage = new AtomicLong(0);
	public AtomicLong cacheThreshold;
	public int NumThreads;
	public long totalTime = 0;
	public boolean verbose = false;
	public final int NumConcurrentHT = 29;

	AtomicLong MinPriority = new AtomicLong(0);
	AtomicInteger MaxSize = new AtomicInteger(1);

	@SuppressWarnings("unchecked")
	Map<Integer, MultiModeLock> [] QueueLocks = new ConcurrentHashMap[NumConcurrentHT];
	MultiModeLock [] QueueSemaphores = new MultiModeLock[NumConcurrentHT];

	LRUQueue MinLRUQ = null;
	public Map<Integer,LRUQueue> LRUHT = new ConcurrentHashMap<Integer,LRUQueue>(); //HashTable of costsize to lruqueue;

	public BinaryHeap<HeapElt> PQ;

	KeyValueStore KVS = new KeyValueStore();
	Comparator<HeapElt> comparator = new HeapEltComparator();

	HashMap<Double, Integer> minCostMap = new HashMap<Double, Integer>();
	double currentMinCost = Double.MAX_VALUE;

	CampRounding campRounding;
	Random randomGenerator = new Random();
	private double InsertionProbability;

	boolean usingProbability = false;
	boolean usingMinCost = false;

	public CAMP(long cacheSize, int precision, double probability){		
		this.cacheSize = cacheSize;
		cacheThreshold = new AtomicLong(cacheSize);
		campRounding = new CampRounding(precision);
		InsertionProbability = probability;

		PQ = new BinaryHeap<HeapElt>();

		for(int i = 0; i < NumConcurrentHT; i++){
			QueueSemaphores[i] = new MultiModeLock();
			QueueLocks[i] = new ConcurrentHashMap<Integer, MultiModeLock>();
		}
	}

	private void AdjustMax(int NewSize){
		if (NewSize > MaxSize.get()) 
			MaxSize.set(NewSize);
		return;
	}

	private int ComputeCost(double MySize, int MyCost){
		int result = (int) ((MaxSize.get() / MySize) * MyCost);
		return campRounding.RoundCost(result);
	}

	public void calculateMinCost(){

		double minCost = Double.MAX_VALUE;

		for (Map.Entry<Double, Integer> entry : minCostMap.entrySet())
		{
			if(entry.getKey() < minCost)
				minCost = entry.getKey();
		}

		currentMinCost = minCost;

	}

	public void CAMP_Handle_Request(Request r, CAMPStats stats){

		if(r.repeat){
			stats.IncrementNumReqs();
			stats.IncrementTotalCost(r.cost);
		}

		if( r.size > cacheSize)
		{
			Print("Item too big for cache");
			return;	
		}

		AdjustMax( r.size );	

		dust elt = CAMP_Lookup(r);

		if(elt == null){ // not in cache
			elt = new dust();
			elt.key = r.key;
			elt.payload = null;
			elt.setSize( r.size );
			elt.SetInitialCost(r.cost);

			MakeRoom(r.size, stats);
			
			CAMP_Insert(elt, stats, false);

			if(r.repeat){
				stats.IncrementCostNotInCache(r.cost);
				stats.IncrementNumMisses();
			}
		}
		else{ // in cache	
			CAMP_Update(elt, stats);
			stats.IncrementNumHit();	
		} 
	}

	public dust CAMP_Lookup(Request r){

		dust elt = KVS.get(r.key);
		return elt;
	}

	public MultiModeLock GetLockForRoundedCost(int roundedcost){

		int n = roundedcost % NumConcurrentHT;

		MultiModeLock queueLock = QueueLocks[n].get(roundedcost);

		try{
			if(queueLock == null){
				MultiModeLock ql = new MultiModeLock();

				QueueSemaphores[n].acquire();
				
				if( (queueLock = QueueLocks[n].get(roundedcost)) == null){
					QueueLocks[n].put(roundedcost, ql);
					queueLock = ql;
				}
				QueueSemaphores[n].release();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		return queueLock;
	}

	public void CAMP_Insert( dust elt, CAMPStats stats, boolean update ){

		 int roundedcost = ComputeCost(elt.getSize(), elt.GetInitialCost());
		elt.setCostSize( roundedcost );
		elt.setPriority( MinPriority.get() + roundedcost );

		MultiModeLock queueLock = this.GetLockForRoundedCost(elt.getCostSize());

		queueLock.acquire();
		
		KVS.put(elt.key, elt);		

		LRUQueue LQ;
		LQ = LRUHT.get( elt.getCostSize() );
		if(LQ == null){
			LQ = new LRUQueue();	
			LQ.setRoundedCost( elt.getCostSize() );
			LRUHT.put(elt.getCostSize(), LQ);			
		}

		LQ.add(elt);

		if ( LQ.getHeapEntry() == null )
		{
			HeapElt HN = new HeapElt(elt.getPriority());
			HN.setQueue(LQ);
			LQ.setHeapEntry(HN);

			stats.IncrementHeapAccesses();
			PQ.add(HN);
		}
		
		queueLock.release();
		
		if(!update)
			currentCacheUsage.getAndAdd( elt.getSize() ); 
	}

	public void CAMP_Update(dust elt, CAMPStats stats){
		CAMP_Maint_Delete(elt, stats);
		CAMP_Insert(elt, stats, true);
	}

	public void CAMP_Maint_Delete(dust deleteElt, CAMPStats stats){

		MultiModeLock queueLock = this.GetLockForRoundedCost(deleteElt.getCostSize());
		queueLock.acquire();

		LRUQueue queue = LRUHT.get(deleteElt.getCostSize());
		if(queue == null){
			queueLock.release();
			return;
		}
		
		HeapElt heapElt = queue.getHeapEntry();
		if(heapElt == null)
			System.out.println("Error: Heap Element in deletion is null");
		

		boolean removed = queue.remove(deleteElt);
		if(!removed){
			queueLock.release();
			return;
		}

		if(queue.isEmpty()){
			stats.IncrementHeapAccesses();
			boolean removedFirstElt = PQ.remove(heapElt);
			heapElt.setQueue(null);

			LRUQueue removedQueue = LRUHT.remove(queue.getRoundedCost());

			if(removedQueue == null){
				System.out.println("Error: Queue could not be removed from hashtable");
			}
			removedQueue.setHeapEntry(null);

			if(!removedFirstElt)
				System.out.println("Error: First Heap Element not properly removed");
		}
		else{

			dust firstEltInQueue = queue.peek();
			if(deleteElt.getPriority() != firstEltInQueue.getPriority()){
				PQ.ChangeEltValue(heapElt, firstEltInQueue.getPriority());
				stats.IncrementHeapAccesses();
			}
		}

		queueLock.release();
	}
	public boolean CAMP_Delete( CAMPStats stats){
		MultiModeLock queueLock = null;
		LRUQueue queue;

		while(true){
			try{
				queue = PQ.peek().getQueue();
				queueLock = this.GetLockForRoundedCost(queue.getRoundedCost());

				if(queueLock.tryAcquire())
					break;
			}
			catch(Exception e){ // could be caught because queue could be changed
			}
		}

		dust deleteElt = queue.peek();
		if(deleteElt == null){
			queueLock.release();
			return false;
		}

		HeapElt heapElt = queue.getHeapEntry();

		if(heapElt == null){
			System.out.println("Error: Heap Element in deletion is null");
			queueLock.release();
			return false;
		}

		dust kvsRemovalElt = KVS.remove(deleteElt.key);
		boolean wasRemoved = queue.remove(deleteElt);

		if(kvsRemovalElt == null && wasRemoved){
			System.out.println("Removed from Queue but not from KVS");
		}

		if(kvsRemovalElt == null){
			System.out.println("Error in Delete - Thread " + Thread.currentThread().getId() +" - KVS could not remove element: " + deleteElt.key );
			queueLock.release();
			return false;
		}

		stats.IncrementEvictions();

		if(usingMinCost){
			double removedEltCost = ((double) deleteElt.GetInitialCost()) / deleteElt.getSize();
			Integer currentNum = this.minCostMap.get(removedEltCost);

			if(currentNum == 0)
				System.out.println("Error");

			currentNum--;
			if(currentNum == 0){
				minCostMap.remove(removedEltCost);
				if(removedEltCost == currentMinCost){
					calculateMinCost();
				}
			} 
		}
		if(queue.isEmpty()){

			LRUQueue removedQueue = LRUHT.remove(queue.getRoundedCost());

			if(removedQueue == null){
				System.out.println("Error: Queue could not be removed from hashtable");
				queueLock.release();
				return false;
			}

			stats.IncrementHeapAccesses();
			boolean removedFirstElt = PQ.remove(heapElt);
			heapElt.setQueue(null);
			removedQueue.setHeapEntry(null);

			if(!removedFirstElt){
				System.out.println("Error: First Heap Element not properly removed");
				queueLock.release();
				return false;
			}
		}
		else{

			dust firstEltInQueue = queue.peek();

			if(kvsRemovalElt.getPriority() != firstEltInQueue.getPriority()){
				stats.IncrementHeapAccesses();				
				PQ.ChangeEltValue(heapElt, firstEltInQueue.getPriority());
				stats.IncrementHeapAccesses();
			}
		}

		HeapElt front = PQ.peek();
		if(front != null){
			MinPriority.set( front.getPriority() );
		}

		currentCacheUsage.addAndGet(-kvsRemovalElt.getSize());

		queueLock.release();
		return true;
	}

	public void CAMP_RemoveLowestPriority(CAMPStats stats){

		boolean successful = false;

		while(!successful){
			try{ // Due to race conditions, must use try/catch if pointers become null
				successful = CAMP_Delete(stats);
			} catch(Exception e){
				System.out.println("Caught error in RemoveLowest Priority");
			}
		}
	}

	public void MakeRoom( int newElementSize, CAMPStats stats){

		while(currentCacheUsage.get() + newElementSize > cacheThreshold.get()){
			CAMP_RemoveLowestPriority(stats);
		}	
	}

	public void Print(String str){
		if(verbose){
			System.out.println(str);
		}
	}

	public int GetNumberOfKeyItems(){

		int sum = 0;

		Object [] heapElts = PQ.array;

		for(Object elt : heapElts){
			if(elt == null)
				continue;
			HeapElt heapElt = (HeapElt) elt;
			LRUQueue queue = heapElt.getQueue();
			if(queue == null)
				continue;

			sum += queue.size();
		}

		return sum;
	}


	public void PrintWaited(long startTime, long endTime, String place){

		long threshold = 5000;
		long duration = endTime - startTime;
		if(duration > threshold)
			System.out.println("Waited for " + place + " for " + duration);

	}
}