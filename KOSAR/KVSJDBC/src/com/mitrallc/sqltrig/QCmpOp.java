package com.mitrallc.sqltrig;

public class QCmpOp {
//	public enum CmpOpType {
//	    EQ, LESS, GREATER, EQLESS, EQGREATER, NOTEQ 
//	}
	
	public enum PredType {
		SELECT, JOIN, NOT
	}
	String predicate=null;
	String TblName=null;
	String CmpOp;
	public PredType PredKind;
	QNode LeftNode=null;
	QNode RightNode=null;
	QCmpOp next = null;
	
	public boolean GiveJoinPredicate(StringBuffer res){
		if (res == null) return false;
		if (PredKind != PredType.JOIN){
			System.out.println("Error in QCmpOp:  Cannot invoke this method on a non JOIN operator.");
			return false;
		}
		QNode left = this.LeftNode;
		QNode right = this.RightNode;
		res.append(" "+left.GetTBLname()+"."+left.GetParamAttr()+""+CmpOp+""+right.GetTBLname()+"."+right.GetParamAttr()+" ");
		return true;
	}
	
	public QNode WhichNodeRefsTableAttr(String TBLname, String Attr){
		if (this.LeftNode.GetTBLname().equals(TBLname) && this.LeftNode.GetParamAttr().equals(Attr)) return this.LeftNode;
		if (this.RightNode.GetTBLname().equals(TBLname) && this.RightNode.GetParamAttr().equals(Attr)) return this.RightNode;
		return null;
	}
	
	public QNode WhichNodeRefsTable(String TBLname){
		if (this.LeftNode.GetTBLname().equals(TBLname)) return this.LeftNode;
		if (this.RightNode.GetTBLname().equals(TBLname)) return this.RightNode;
		return null;
	}
	
	public String GetSelectAttrName(){
		String results = null;
		if (PredKind != PredType.SELECT){
			System.out.println("Error in QCmpOp:  Cannot invoke this method on a non SELECT operator.");
			return null;
		}
		if (this.GetLeftNode().GetTBLname() != null) return this.GetLeftNode().GetParamAttr();
		if (this.GetRightNode().GetTBLname() != null) return this.GetRightNode().GetParamAttr();
		return results;
	}
	
	public String GetSelectTblName(){
		String results = null;
		if (PredKind != PredType.SELECT){
			System.out.println("Error in QCmpOp:  Cannot invoke this method on a non SELECT operator.");
			return null;
		}
		if (this.GetLeftNode().GetTBLname() != null) return this.GetLeftNode().GetTBLname();
		if (this.GetRightNode().GetTBLname() != null) return this.GetRightNode().GetTBLname();
		return results;
	}
	
	public String GetSelectLiteral()
	{
		String results = null;
		if (PredKind != PredType.SELECT){
			System.out.println("Error in QCmpOp:  Cannot invoke this method on a non SELECT operator.");
			return null;
		}
		if (LeftNode != null){
			if (this.LeftNode.GetTBLname() == null) return this.LeftNode.GetParamAttr();
		}
		if (RightNode != null)
			if (this.RightNode.GetTBLname() == null) return this.RightNode.GetParamAttr();
		return results;
	}
	
	public void SetPredicate(String p)
	{
		predicate = p;
	}
	public String GetPredicate() 
	{
		return predicate;
	}
	
	public void SetTblName(String tbl)
	{
		TblName = tbl;
	}
	public String GetTblName()
	{
		return TblName;
	}
	
	public void SetCmpOpType(String inCmp)
	{
		CmpOp = inCmp.trim();
	}
	public String GetCmpOpType()
	{
		return CmpOp;
	}
	
	public void SetPred(PredType inP)
	{
		PredKind = inP;
	}
	
	public PredType GetPredType()
	{
		return PredKind;
	}
	
	public void SetLeftNode (QNode lefty)
	{
		LeftNode = lefty;
	}
	
	public QNode GetLeftNode()
	{
		return LeftNode;
	}
	
	public void SetRightNode(QNode righty)
	{
		RightNode = righty;
	}
	
	public QNode GetRightNode()
	{
		return RightNode;
	}
	public void SetNext(QCmpOp elt)
	{
		next = elt;
	}
	
	public QCmpOp GetNext()
	{
		return next;
	}
}
