package com.mitrallc.kosar;

public class ReplacementTechnique {
	int memsize = -1;
	LRU selectedTechnique = null;
	boolean caching = false;
	boolean verbose = false;
	private static int MinMemory = 1024;
	
	public boolean InsertKV(dust elt, int fragid){
		if (elt == null){
			System.out.println("Error in ReplacementTechnique.InsertKV:  input key-value pair is null.");
			return false;
		}
		if (fragid < 0 || fragid >= kosar.NumFragments){
			System.out.println("Error in ReplacementTechnique.InsertKV:  The input fragid "+fragid+" is not valid.  It must be a value between 0 and "+kosar.NumFragments);
			return false;
		}
		if (!caching){
			if (verbose) System.out.println("ReplacementTechnique.InsertKV:  caching is disabled due to a cache size smaller than the specified Min "+MinMemory);
			elt.setRS(null);
			return false;
		}
		return selectedTechnique.InsertKV(elt, fragid);
	}
	
	public void DeleteKV(dust elt, int fragid){
		if (elt == null){
			System.out.println("Error in ReplacementTechnique.DeleteKV:  input key-value pair is null.");
			return;
		}
		if (fragid < 0 || fragid >= kosar.NumFragments){
			System.out.println("Error in ReplacementTechnique.DeleteKV:  The input fragid "+fragid+" is not valid.  It must be a value between 0 and "+kosar.NumFragments);
			return;
		}
		selectedTechnique.DeleteKV(elt, fragid);
		return;
	}
	
	public void RegisterHit(dust elt, int fragid){
		if (elt == null){
			System.out.println("Error in ReplacementTechnique.RegisterHit:  input key-value pair is null.");
			return;
		}
		if (fragid < 0 || fragid >= kosar.NumFragments){
			System.out.println("Error in ReplacementTechnique.RegisterHit:  The input fragid "+fragid+" is not valid.  It must be a value between 0 and "+kosar.NumFragments);
			return;
		}
		selectedTechnique.RegisterHit(elt, fragid);
		return;
	}
	
	public void Reset(){
		selectedTechnique = new LRU(memsize);
		return;
	}
	
	public ReplacementTechnique(){
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();

        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        
        if ( (heapMaxSize - heapSize) > (MinMemory) ){
        	caching = true;
        	selectedTechnique = new LRU();
        }
	}
	
	public ReplacementTechnique(
			int cachesize //in bytes
			){
		if (cachesize >= MinMemory ) 
			caching = true;  //If available cachesize is less than the minimum specified amount of memory then there is no caching
		memsize = cachesize;
		
		selectedTechnique = new LRU(memsize);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}
