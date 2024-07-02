package com.mitrallc.common;

/**
 * KeyQueueData item stores the invalidation key sets sent by the database with
 * the timestamp at which it was received. These keys are removed once as soon
 * as they were received and again when the transaction that caused them to be
 * invalidated was completed (in case they were brought back into the cache)
 * 
 * @author lakshmy.mohanan
 * @author neeraj.narang
 * 
 */
public class KeyQueueDataItem {
	private byte[] keyList; /*comma separated key list in the form of a byte array*/
	private long TSofDeletion;
	
	public KeyQueueDataItem(byte[] keylist, long timestamp) {
		this.keyList=keylist;
		this.TSofDeletion=timestamp;
	}
	public long getTSofDeletion() {
		return TSofDeletion;
	}
	public void setTSofDeletion(long tSofDeletion) {
		TSofDeletion = tSofDeletion;
	}
	public byte[] getKeyList() {
		return keyList;
	}
	public void setKeyList(byte[] keyList) {
		this.keyList = keyList;
	}

}
