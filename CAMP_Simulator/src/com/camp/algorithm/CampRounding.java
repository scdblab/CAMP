package com.camp.algorithm;

import java.util.Date;

public class CampRounding {

	public static final int NumBits = 32;
	public static int Precision = 4;
	public static int bitmask = 0x0000000F;
	public static int[] MyMaskedBits = new int[NumBits];

	public static int MostSignificantBit(int i){
		int result=0;
		return 32-Integer.numberOfLeadingZeros(i);
	}
	
	public void InitializeMaskedBits()
	{
		int k=1;
		for (int i = Precision+1; i < NumBits-1; i++){
			MyMaskedBits[i] = bitmask  << k;
			k++;
		}
	}

	public int RoundCost (int i)
	{
		if (i <= bitmask) return i;
		int sigbit = MostSignificantBit(i);
		if (sigbit == NumBits) return 0;
		return (i & MyMaskedBits[sigbit]);
	}

	public CampRounding(int precision){
		
		Precision = precision;
		
		bitmask = (int) Math.pow(2, precision) - 1;
		
		InitializeMaskedBits();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		long NISTmegabyte = 1024*1024;
		long NISTgigabyte = 1024*1024*1024;
		CampRounding CR = new CampRounding(4);

		//InitializeMaskedBits();

		int bitmask = 0xF0000000;
		System.out.println("bitmask = "+bitmask);
		System.out.println(String.format("%x",bitmask));

		// Get current size of heap in bytes
		long heapSize = Runtime.getRuntime().totalMemory();

		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.
		// Any attempt will result in an OutOfMemoryException.
		long heapMaxSize = Runtime.getRuntime().maxMemory();

		// Get amount of free memory within the heap in bytes. This size will increase
		// after garbage collection and decrease as new objects are created.
		long heapFreeSize = Runtime.getRuntime().freeMemory();

		System.out.println("Heap size (Megabyte) = "+heapSize/NISTmegabyte);
		System.out.println("Heap max size (Megabytes) = "+heapMaxSize/NISTmegabyte);
		System.out.println("Heap free size (Megabytes) = "+heapFreeSize/NISTmegabyte);

		for (int i=0; i < 18; i++)
			System.out.println("i="+i+", most significant bit is "+CR.RoundCost (i));
		System.out.println("0xB0AF,"+CR.RoundCost (0xB0AF));

		long lStartTime = new Date().getTime();

		for (int i =0 ; i <900*NISTmegabyte;i++)
			CR.RoundCost(i);

		long lEndTime = new Date().getTime();

		long difference = lEndTime - lStartTime;

		System.out.println("Elapsed milliseconds for 900 million camp rounding is: " + difference);
	}

}
