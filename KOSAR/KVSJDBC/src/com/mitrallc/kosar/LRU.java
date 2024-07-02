package com.mitrallc.kosar;

public class LRU {
	private static LRUQueue[] mylru = new LRUQueue[ kosar.NumFragments ];
	private double myfrac=0.9;
	private long memsize = 0;
	private long lowWaterMark=0;
	private long highWaterMark=0;
	private boolean verbose = false;
	
	public boolean InsertKV(dust elt, int fragid){
		//If the highWaterMark is larger than available memory then evict an element
		//If no eviction candidate exists then evict from another LRU Queue
		//If no item is evicted then ignore the insert operation
		long heapSize = Runtime.getRuntime().totalMemory();
		if (verbose) System.out.println("Current memory usage "+heapSize/(1024*1024)+" MB.");
		boolean res=true;
		if (heapSize > highWaterMark || heapSize-lowWaterMark > memsize ){
			if (verbose) System.out.println("LRU.InsertKV:  Evicting a victim.");
			res = mylru[fragid].EvictHead();
			if (!res){
				for (int i=0; i < kosar.NumFragments; i++){
					if ( mylru[ (fragid + i) % kosar.NumFragments ].EvictHead() ){
						i = kosar.NumFragments;
						res = true;
					}
				}
			}
		}
		if (res) mylru[fragid].Append(elt);
		return res;
	}
	
	public void DeleteKV(dust elt, int fragid){
		mylru[fragid].Delete(elt);
		return;
	}
	
	public void RegisterHit(dust elt, int fragid){
		mylru[fragid].Delete(elt);  //Remove the element from the queue
		mylru[fragid].Append(elt);  //And insert it at the queue of the queue
		return;
	}
	
	public void Reset(){
		mylru = new LRUQueue[ kosar.NumFragments ];
		return;
	}
	
	public LRU(){
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        
        memsize = heapMaxSize-heapSize;
        memsize = (long)(((double) memsize) * myfrac); //Take over the specified fraction of memory
        
        lowWaterMark = heapSize;
        highWaterMark = heapSize+memsize;
        
        //Initialize array of LRU queues
        mylru = new LRUQueue[ kosar.NumFragments ];
        for (int i=0; i < kosar.NumFragments; i++)
        	mylru[i] = new LRUQueue();
	}
	
	public LRU (
			int cachesize  //in bytes
			){
		long memsize = cachesize;
		System.out.println("LRU:  Memory is initialized to "+memsize+" bytes.");
		
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        
        if (heapMaxSize < (heapSize+memsize)){
        	System.out.println("LRU:  The specified memsize "+memsize+" PLUS the current heapsize "+heapSize+" exceeds the JVM size "+heapMaxSize);
        	memsize = (int) (heapMaxSize - heapSize);
        	System.out.println("LRU:  Resetting the KVS size to "+memsize);
        }
        
        lowWaterMark = heapSize;
        highWaterMark = heapSize+memsize;
        
        //Initialize array of LRU queues
        mylru = new LRUQueue[ kosar.NumFragments ];
        for (int i=0; i < kosar.NumFragments; i++)
        	mylru[i] = new LRUQueue();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("LRU Unit Test");

	}
}
