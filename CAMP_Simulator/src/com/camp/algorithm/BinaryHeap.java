package com.camp.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class BinaryHeap<T extends Comparable<T>> implements PriorityQueue<T>{

	private static final int DEFAULT_CAPACITY = 10;
	public HeapElt[] array;
	protected int size;

	public HashMap<HeapElt, Integer> indexMap = new HashMap<HeapElt, Integer>();
	
	MultiModeLock heapLock = new MultiModeLock();
	
	/**
	 * Constructs a new BinaryHeap.
	 */
	@SuppressWarnings("unchecked")
	public BinaryHeap () {
		// Java doesn't allow construction of arrays of placeholder data types 
		//array = (HeapElt[])new Comparable[DEFAULT_CAPACITY];  
		array = new HeapElt[DEFAULT_CAPACITY];
		size = 0;
	}
	
	/**
	 * Changes the value of an existing heap element
	 */
	public void ChangeEltValue(HeapElt elt, long NewCostVal){
		
//		try {
//			//System.out.println("Try acquire: change elt");
//		//	HeapSema.acquire();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		heapLock.acquire();
		
		long oldval = elt.getPriority();
		if (oldval == NewCostVal){
			heapLock.release();
			return; //No change
		}
		elt.setPriority(NewCostVal);
		
		Integer index = indexMap.get(elt);
		
		if(index == null){
			System.out.println("Item does not exist in hashmap");
			return;
		}
		
		//int index = this.findHeapIndexOfElement(elt);
		
		if (oldval > NewCostVal) 
			bubbleUp(index);
		else bubbleDown(index);
		
		heapLock.release();

	}


	/**
	 * Adds a value to the min-heap.
	 */
	public void add(HeapElt value) {
		
//		try {
//		//	System.out.println("Try acquire: add elt");
//			HeapSema.acquire();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		heapLock.acquire();
		
		// grow array if needed
		if (size >= array.length - 1) {
			array = this.resize();
		}        

		// place element into heap at bottom
		size++;
		int index = size;
		array[index] = value;
		
		indexMap.put(value, index);

		bubbleUp(this.size);
		
		heapLock.release();
		//System.out.println(" released: add elt");
	}


	/**
	 * Returns true if the heap has no elements; false otherwise.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	

	/**
	 * Returns (but does not remove) the minimum element in the heap.
	 */
	public HeapElt peek() {
		if (this.isEmpty()) {
			throw new IllegalStateException();
		}
		return array[1];
	}
	
	public int findHeapIndexOfElement(HeapElt elt){
		int eltidx = -1;
		
		for (int i=1; i <= size; i++){
			if (array[i]==elt) eltidx = i;
		}
		if (eltidx == -1){
				System.out.println("Error in RemoveElt method of BinaryHeap, failed to find index of the requested element.");
		}
		
		return eltidx;
	}
	
	/**
	 * Removes a specific element from the heap.
	 */
	public boolean remove(HeapElt elt){
		//System.out.println("Try acquire: remove elt");
//		try {
//			
//				HeapSema.acquire();
//				
//				
//				
//			
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		heapLock.acquire();
		
		Integer eltidx = indexMap.get(elt);
		
		if(eltidx == null){
			System.out.println("Item does not exist in hashmap");
			return false;
		}
				
		//int eltidx = this.findHeapIndexOfElement(elt);		
		if(eltidx == -1)
			return false;
		
		if(eltidx == size)
		{
			indexMap.remove(array[size]);
			array[size] = null;
			size--;	
			heapLock.release();
			return true;
			
		}
			
			
		swap(eltidx, size);
		
		indexMap.remove(array[size]);
		array[size] = null;
		size--;		
		
		this.bubbleUp(eltidx);
		this.bubbleDown(eltidx);
		
		
		heapLock.release();
	//	System.out.println("Try release: remove elt");
		
		return true;
	}

	/**
	 * Removes and returns the minimum element in the heap.
	 */
	public HeapElt remove() {
		
//		try {
//		//	System.out.println("Try acquire: remove");
//			HeapSema.acquire();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		heapLock.acquire();
		
		// what do want return?
		HeapElt result = peek();
		
		// get rid of the last leaf/decrement
		array[1] = array[size];
		array[size] = null;
		size--;

		bubbleDown(1);

		
		heapLock.release();
	//	System.out.println("Try acquire: release");

		return result;
	}

	public int size(){
		return size;
	}

	/**
	 * Returns a String representation of BinaryHeap with values stored with 
	 * heap structure and order properties.
	 */
	public String toString() {
		return Arrays.toString(array);
	}


	/**
	 * Performs the "bubble down" operation to place the element that is at the 
	 * root of the heap in its correct place so that the heap maintains the 
	 * min-heap order property.
	 */
	protected void bubbleDown(int index) {

		// bubble down
		while (hasLeftChild(index)) {
			// which of my children is smaller?
			int smallerChild = leftIndex(index);

			// bubble with the smaller child, if I have a smaller child
			if (hasRightChild(index)
					&& array[leftIndex(index)].compareTo(array[rightIndex(index)]) > 0) {
				smallerChild = rightIndex(index);
			} 

			if (array[index].compareTo(array[smallerChild]) > 0) {
				swap(index, smallerChild);
			} else {
				// otherwise, get outta here!
				break;
			}

			// make sure to update loop counter/index of where last el is put
			index = smallerChild;
		}        
	}
	
	protected int BruteForceFindIndexOfElt(HeapElt elt){
		int index = 1;
		for (int i=1; i < size; i++){
			if (array[i] == elt) return i;
		}
		System.out.println("Error in BruteForceFindIndexOfElt in BinaryHeap:  Should not have reached this point.");
		return index;
	}


	/**
	 * Performs the "bubble up" operation to place a newly inserted element 
	 * (i.e. the element that is at the size index) in its correct place so 
	 * that the heap maintains the min-heap order property.
	 */
	protected void bubbleUp(int index) {

		while (hasParent(index)
				&& (parent(index).compareTo(array[index]) > 0)) {
			// parent/child are out of order; swap them
			swap(index, parentIndex(index));
			index = parentIndex(index);
		}        
	}


	protected boolean hasParent(int i) {
		return i > 1;
	}


	protected int leftIndex(int i) {
		return i * 2;
	}

	protected int rightIndex(int i) {
		return i * 2 + 1;
	}

	protected boolean hasLeftChild(int i) {
		return leftIndex(i) <= size;
	}

	protected boolean hasRightChild(int i) {
		return rightIndex(i) <= size;
	}

	protected HeapElt parent(int i) {
		return array[parentIndex(i)];
	}


	protected int parentIndex(int i) {
		return i / 2;
	}


	protected HeapElt[] resize() {
		return Arrays.copyOf(array, array.length * 2);
	}


	protected void swap(int index1, int index2) {
		HeapElt tmp = array[index1];
		array[index1] = array[index2];
		array[index2] = tmp;      
		
		indexMap.put(array[index1], index1);
		indexMap.put(array[index2], index2);
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BinaryHeap<HeapElt> heap;
		heap = new BinaryHeap<HeapElt>();

		//Heap heap = new Heap();
	    
    	ArrayList<HeapElt> elts = new ArrayList<HeapElt>();

    	Random r = new Random();
    	for(int i = 100000; i > 0; i--){
    		int priority = r.nextInt(Integer.MAX_VALUE);
    		//int priority = i;
    		HeapElt temp = new HeapElt(priority);
    		elts.add(temp);
    		heap.add(temp);
    		
    	}
    	
    	long startTime = System.nanoTime();
    
    	System.out.println("Before delete");
    	
    	int size = elts.size();
    	
    	for(int i = 0; i < 50000; i++){

    		int rand = r.nextInt(size - i);
    		HeapElt removed = elts.get(rand);
    		heap.remove(elts.get(rand));
    		HeapElt elt = elts.remove(rand);

    		assert(elt == removed);
    		
    	}
    	
   	
    for(Map.Entry<HeapElt,Integer> he: heap.indexMap.entrySet()){
		HeapElt elt = he.getKey();
		Integer index = he.getValue();
	
		if(elt != heap.array[index])
			System.out.println("Wrong");
		
    }
    
	for(int i = 100000; i > 0; i--){
		int priority = r.nextInt(Integer.MAX_VALUE);
		//int priority = i;
		HeapElt temp = new HeapElt(priority);
		elts.add(temp);
		heap.add(temp);   		
	}
    
    for(Map.Entry<HeapElt,Integer> he: heap.indexMap.entrySet()){
		HeapElt elt = he.getKey();
		Integer index = he.getValue();
	
		if(elt != heap.array[index])
			System.out.println("Wrong");
		
    }
	
    	long endTime = System.nanoTime();
    	
    	long duration = endTime - startTime;
    	
    	System.out.println("Time: " + duration);
    	
    	HeapElt prev = new HeapElt(Integer.MIN_VALUE);
    	
    	while(heap.size() != 0){

    		HeapElt elt = heap.remove();
    			
    		if(elt.getPriority() < prev.getPriority()){
    			System.out.println("Prev: " + prev.getPriority() + "   " + elt.getPriority());
    		}
    		prev = elt;
    	}

    	System.out.println("DONE");
    	
    	
		/*
		ArrayList<HeapElt> elts = new ArrayList<HeapElt>();
		
		//Insert elements in reverse order
		for (int i = 110; i > 100; i--) {
			HeapElt e = new HeapElt(i);
			elts.add(e);
			BH.add(e);
		}
		
		BH.remove(elts.get(3));
		
		int size = BH.size;
		//This should print elements in the right order
		for (int i = 0; i < size; i++){
			HeapElt e = BH.remove();
			e.printCost();
		} */
	}
}
