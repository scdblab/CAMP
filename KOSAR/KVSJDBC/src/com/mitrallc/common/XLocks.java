package com.mitrallc.common;
/**
 * This class holds the lock tables to make sure the key queue is accessed properly.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class XLocks {
	boolean verbose = false;
	ConcurrentHashMap<String, Semaphore> semaphoreMap = new ConcurrentHashMap<String, Semaphore>();
	ConcurrentHashMap<String, Boolean> isLocked = new ConcurrentHashMap<String, Boolean>();

	public boolean isLocked(String key) {
		if(semaphoreMap.containsKey(key) && 
				semaphoreMap.get(key).availablePermits() == 0) 
			return true;
		
		return false;
	}
	
	public void lock(String key) {
		if(!semaphoreMap.containsKey(key))
			semaphoreMap.put(key, new Semaphore(1, true));
		
		try {
			if(verbose)
				System.out.println("Locking " + key);
			semaphoreMap.get(key).acquire();
		} catch (InterruptedException e) {
			System.out.println("Could not obtain Semaphore for " + key);
			e.printStackTrace();
		}
	}

	public void unlock(String key) {
		if(verbose)
			System.out.println("Unlocking key");
		semaphoreMap.get(key).release();
	}
	
	public void clearAll() {
		semaphoreMap.clear();
	}

}

/*public class XLocks {
	ConcurrentHashMap<String, AtomicBoolean> lockTable = new ConcurrentHashMap<String, AtomicBoolean>();

	public boolean islocked(String key) {
		if (lockTable.containsKey(key)) {
			return true;
		}
		return false;
	}

	public boolean lockKey(String key) {
		if (lockTable.containsKey(key)) {
			return false;
		}
		AtomicBoolean val = new AtomicBoolean(true);

		val = lockTable.put(key, val);
		if (val == null) {
			//System.out.println("locked: "+key);
			return true;
		} else {
			return false;
		}
	}

	public void unlockKey(String key) {
		//System.out.println("unlocked: "+key);
		lockTable.remove(key);
	}
	
	public void clearAll() {
		lockTable.clear();
	}
}
*/