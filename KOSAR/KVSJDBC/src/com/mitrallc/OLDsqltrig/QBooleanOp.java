package com.mitrallc.sqltrig;

public class QBooleanOp {

	public enum BoolOpType {
		AND, OR, NOT
	}
	
	BoolOpType BoolOp;
	QCmpOp LeftCmpOp=null;
	QCmpOp RightCmpOp=null;
	QBooleanOp Parent=null;
	QBooleanOp Child=null;
	
	public void SetBoolOp(BoolOpType op)
	{
		BoolOp = op;
	}
	
	public BoolOpType GetBoolOp()
	{
		return BoolOp;
	}
	
	public void SetLeftCmpOp(QCmpOp CmpOp)
	{
		LeftCmpOp = CmpOp;
	}
	
	public QCmpOp GetLeftCmpOp()
	{
		return LeftCmpOp;
	}
	
	public void SetRightCmpOp(QCmpOp CmpOp)
	{
		RightCmpOp = CmpOp;
	}
	
	public QCmpOp GetRightCmpOp()
	{
		return RightCmpOp;
	}
	
	public void SetParent(QBooleanOp BoolOp)
	{
		Parent = BoolOp;
	}
	
	public QBooleanOp GetParent()
	{
		return Parent;
	}
	
	public void SetChild (QBooleanOp BoolOp)
	{
		Child = BoolOp;
	}
	
	public QBooleanOp GetChild ()
	{
		return Child;
	}
		
}
