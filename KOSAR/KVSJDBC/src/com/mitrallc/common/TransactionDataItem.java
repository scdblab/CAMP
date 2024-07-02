package com.mitrallc.common;

/**
 * Holds transaction meta data: a unique ID TID and start and end times. This is
 * used to match the invalidated key sets to the transactions that may have
 * caused them to become invalid.
 * 
 * @author Lakshmy Mohanan
 * @author Neeraj Narang
 * 
 */
public class TransactionDataItem {
	private long TID;
	private long StartTime;
	private long EndTime;

	public TransactionDataItem(long TID, long ST, long ET) {
		this.TID = TID;
		this.StartTime = ST;
		this.EndTime = ET;
	}

	public long getTID() {
		return TID;
	}

	public void setTID(long tID) {
		TID = tID;
	}

	public long getEndTime() {
		return EndTime;
	}

	public void setEndTime(long endTime) {
		EndTime = endTime;
	}

	public long getStartTime() {
		return StartTime;
	}

	public void setStartTime(long startTime) {
		StartTime = startTime;
	}

}
