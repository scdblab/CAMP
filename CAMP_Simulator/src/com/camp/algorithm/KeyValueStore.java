package com.camp.algorithm;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStore {

	public static int NumBuckets = 101;
	
	@SuppressWarnings("unchecked")
	ConcurrentHashMap<String, dust> [] KVS = new ConcurrentHashMap[NumBuckets];
	
	public KeyValueStore(){
		for(int i = 0; i < NumBuckets; i++){
			KVS[i] = new ConcurrentHashMap<String, dust>();
		}
	}
	
	private ConcurrentHashMap<String, dust> getHashMapForKey(String key){
		
		int hash = key.hashCode() < 0 ? (~key.hashCode() + 1) : key.hashCode();
		return KVS[hash % NumBuckets];
	}
	
	public dust get(String key){
		ConcurrentHashMap<String, dust> map = getHashMapForKey(key);
		return map.get(key);
	}
	
	public dust remove(String key){
		ConcurrentHashMap<String, dust> map = getHashMapForKey(key);
		return map.remove(key);
	}
	public dust put(String key, dust d){
		ConcurrentHashMap<String, dust> map = getHashMapForKey(d.key);
		return map.put(key, d);
	}
	
}
