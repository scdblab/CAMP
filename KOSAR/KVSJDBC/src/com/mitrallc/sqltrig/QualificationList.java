package com.mitrallc.sqltrig;

//import com.rays.core.QBooleanOp.BoolOpType;

public class QualificationList {
	int NumTables=0;
	QBooleanOp Head=null;
	String[] TblList = null;
	
	public void SetTblList(String[] tl)
	{
		TblList = tl;
	}
	public String[] GetTblList()
	{
		return TblList;
	}
	
	public void SetBoolOp(QBooleanOp qb)
	{
		Head = qb;
	}
	
	public QBooleanOp GetBoolOp()
	{
		return Head;
	}
	
	public void SetNumTables(int nt)
	{
		NumTables = nt;
	}
	
	public int GetNumTables()
	{
		return NumTables;
	}
}
