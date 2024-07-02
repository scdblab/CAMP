package com.mitrallc.UnitTest;
import com.mitrallc.kosar.*;


public class ReplacementTest {
	public static int NumDusts = 1000;
	public static int MemorySize = 100*1024*1024;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//ReplacementTechnique RP = new ReplacementTechnique(MemorySize);
		ReplacementTechnique RP = new ReplacementTechnique();
		Object[] b = new Object[NumDusts];
		for (int i = 0; i < NumDusts; i++){
			com.mitrallc.sql.ResultSet rs = new com.mitrallc.sql.ResultSet();
			dust ld = new dust();
			ld.setSize(i);
			ld.setPayLoad( new byte[1024*1024] );
			ld.setRS(rs);
			RP.InsertKV( ld, i % kosar.NumFragments );
		}
	}

}
