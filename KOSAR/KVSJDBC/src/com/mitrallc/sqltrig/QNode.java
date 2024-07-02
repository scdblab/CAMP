package com.mitrallc.sqltrig;

/*
 * QNode represents either a selection or a join predicate of
 * the "WHERE" clause of a SQL SELECT statement (a query).
 */

public class QNode {
	private QCmpOp ParentOP = null;
	private String TableName = null;
	private String ParamAttr = null;
	
	public void SetTBLname (String tblname)
	{
		TableName = tblname.trim();
	}
	
	public String GetTBLname ()
	{
		return TableName;
	}
	
	public void SetParentOP(QCmpOp parent)
	{
		ParentOP = parent;
	}
	
	public QCmpOp GetParentOP()
	{
		return ParentOP;
	}
	
	public void SetParamAttr(String InParamAttr)
	{
		ParamAttr = InParamAttr.trim();
	}
	
	public String GetParamAttr()
	{
		return ParamAttr;
	}
}
