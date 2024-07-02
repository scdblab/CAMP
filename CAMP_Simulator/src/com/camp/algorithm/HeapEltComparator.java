package com.camp.algorithm;

import java.util.Comparator;

public class HeapEltComparator implements Comparator<HeapElt> {

	@Override
	public int compare(HeapElt x, HeapElt y){
		
		if(x.getPriority() > y.getPriority())
		{
			return 1;
		}
		else if(x.getPriority() == y.getPriority()){
			
			int xRoundedCost = x.getQueue().getRoundedCost();
			int yRoundedCost = y.getQueue().getRoundedCost();
			
			if(xRoundedCost > yRoundedCost)
				return 1;
			else if ( xRoundedCost == yRoundedCost ){
				
				System.out.println("IMPOSSIBLE SITUATION. HORRIBLY WRONG IF THIS OCCURS");
				return 0;
			}
			else
			{
				return -1;
			}	
		}
		else{
			return -1;
		}
	}
	
}
