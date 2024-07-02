package com.camp.simulator;

import java.util.Comparator;

import com.camp.algorithm.Request;

public class RequestScrambler implements Comparable<RequestScrambler> {

	public int NumAppearances;
	public Request r;
	

	@Override
	public int compareTo(RequestScrambler o) {
		if(NumAppearances < o.NumAppearances)
			return -1;
		else if ( NumAppearances == o.NumAppearances)
			return 0;
		else{
			return 1;
		}
	}
	
	
}
