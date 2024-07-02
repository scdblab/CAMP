package com.mitrallc.kosar;
import java.util.Arrays;

public class BinaryHeap<T extends Comparable<T>> implements PriorityQueue<T>{

	private static final int DEFAULT_CAPACITY = 10;
	protected HeapElt[] array;
	protected int size;

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
	public void ChangeEltValue(HeapElt elt, int NewCostVal){
		int oldval = elt.getCost();
		if (oldval == NewCostVal) return; //No change
		elt.setCost(NewCostVal);
		
		if (oldval > NewCostVal) bubbleUp();
		else bubbleDown();
	}


	/**
	 * Adds a value to the min-heap.
	 */
	public void add(HeapElt value) {
		// grow array if needed
		if (size >= array.length - 1) {
			array = this.resize();
		}        

		// place element into heap at bottom
		size++;
		int index = size;
		array[index] = value;

		bubbleUp();
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
	
	/**
	 * Removes a specific element from the heap.
	 */
	public void RemoveElt(HeapElt elt){
		int eltidx = -1;
		for (int i=1; i < size; i++){
			if (array[i]==elt) eltidx = i;
		}
		if (eltidx == -1){
				System.out.println("Error in RemoveElt method of BinaryHeap, failed to find index of the requested element.");
		}
	}


	/**
	 * Removes and returns the minimum element in the heap.
	 */
	public HeapElt remove() {
		// what do want return?
		HeapElt result = peek();

		// get rid of the last leaf/decrement
		array[1] = array[size];
		array[size] = null;
		size--;

		bubbleDown();

		return result;
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
	protected void bubbleDown() {
		int index = 1;

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
	protected void bubbleUp() {
		int index = this.size;

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
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BinaryHeap<HeapElt> BH;
		BH = new BinaryHeap<HeapElt>();

		//Insert elements in reverse order
		for (int i = 110; i > 100; i--) {
			HeapElt e = new HeapElt(i);
			BH.add(e);
		}
		
		//This should print elements in the right order
		for (int i = 0; i < 10; i++){
			HeapElt e = BH.remove();
			e.printCost();
		}
	}
}
