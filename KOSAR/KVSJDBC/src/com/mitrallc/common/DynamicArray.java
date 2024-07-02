package com.mitrallc.common;

import java.util.Arrays;

import com.mitrallc.kosar.dust;

/**
 * The Dynamic Array is written for the pendingTransactionList
 * and the keyQueue for DBMS updates. Some methods are common
 * to both, but some are specific to the keyQueue.
 * Array is not actually initialized until it is populated using 'add'
 * 
 * Edit: 6-10: extended use for dust
 * 
 */
public class DynamicArray {

	Integer maxSize = 16;
	float loadFactor = 0.75f;
	Integer threshold;
	Integer size = 0;
	ArrayObject[] data  = new ArrayObject[maxSize];
	boolean DEBUG = true;
	
	public DynamicArray() {
		threshold = (int)(maxSize * loadFactor);
	}

	private void checkDouble() {
		if ( size+1 >= threshold ) {
			maxSize = maxSize*2;
			data = Arrays.copyOf(data, maxSize);
			threshold = (int)(maxSize * loadFactor);
		}
	}
	/*
	 * When the size of the used array exceeds 75%, 
	 * the size doubles and copies the array.
	 * This method used for KosarSoloDriver.pendingTransactionList
	 */
	public void add(long counter) {
		
		checkDouble();
		data[size++] = new ArrayObject(counter);
		
		if(DEBUG) sanityCheck();
	}

	/*
	 * When the size of the used array exceeds 75%, 
	 * the size doubles and copies the array.
	 * This method used for KosarSoloDriver.keyQueue
	 */
	public void add(long counter, byte[] keyList) {
		checkDouble();
		data[size++] = new ArrayObject(counter, keyList);
		if(DEBUG) sanityCheck();
	}

	/*
	 * When the size of the used array exceeds 75%, 
	 * the size doubles and copies the array.
	 * This method used for KosarSoloDriver.keyQueue
	 */
	public void add(dust ds) {
		checkDouble();
		data[size++] = new ArrayObject(ds);
		if(DEBUG) sanityCheck();
	}
	
	/*
	 * Remove object from array; search by timestamp (AtomicInteger)
	 */
	public void remove(long counter) {
		int index = 0;

		//copy first part of the array
		while(index < size) {
			if ( data[index].getCounter() == counter) {
				data[index++] = null;
				break;
			}
			index++;
		}
		//copy second part of the array;
		while(index < size) {
			data[index-1] = data[index++];	
		}
		size--;
		if(DEBUG) sanityCheck();
	}
	public void removeUpTo(long counter) {
		if(data == null || size == 0) return;
		
		int i = 0;
		for(i = 0; i < size; i++) {
			if(data[i].getCounter() >= counter) 
				break;
			data[i] = null;
		}
		size -= i;
		
		if(i == 0) return;
		int index = 0;

		//iterate once;
		while(index < size) {
			data[index] = data[i];
			i++;
			index++;
		}

		if(DEBUG) sanityCheck();
	}
	/*
	 * Returns timestamp (AtomicInteger)
	 */
	public long getCounter(int index) {
		if(index >= size) return 0;
		return data[index].getCounter();
	}

	/*
	 * Returns keyList in the case of keyQueue
	 */
	public byte[] getKeyList(int index) {
		if(index >= size) return null;
		return data[index].getKeyList();
	}

	/*
	 * Returns dust in the case of dust
	 */
	public dust getDust(int index) {
		if(index >= size) return null;
		return data[index].getDust();
	}
	
	public int getIndexOf(long counter) {

		for(int i = 0; i < size; i++)
			if(counter == data[i].getCounter())
				return i;		
		return -1;
	}

	public int size() { return size; }

	public boolean sanityCheck() {
		int index = 0;
		while(index < size-1) {
			if(data[index].getCounter() >= data[index+1].getCounter()) {
				System.out.println("Bad sanity check. " + data[index].getCounter() +
						" > " + data[index+1].getCounter());
				return false;
			}
			index++;
		}
		return true;
	}

	public class ArrayObject {
		long counter;
		byte[] keyList;
		dust ds;

		public ArrayObject(long counter) {
			this.counter = counter;
		}
		public ArrayObject(long counter, byte[] keyList) {
			this.counter= counter;
			this.keyList = keyList;
		}
		public ArrayObject(dust ds) {
			this.ds = ds;
		}
		public long getCounter() { return counter; }
		public byte[] getKeyList() { return keyList; }
		public dust getDust() { return ds; }
	}
}
