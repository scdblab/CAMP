package com.mitrallc.sqltrig;

public class QTablePredList {
	String TableName;
	int NumPredicates=0;
	int NumSelects=0;
	int NumJoins=0;
	QCmpOp Selects=null;
	QCmpOp Joins=null;
	
	public void SetTableName(String tbl) {
		TableName = tbl;
	}
	public String GetTableName() {
		return TableName;
	}
	
//	public void SetNumPredicates(int preds) {
//		NumPredicates = preds;
//	}
	public int GetNumPredicates() {
		return NumPredicates;
	}
	
//	public void SetNumSelects(int cnt){
//		NumSelects = cnt;
//	}
	public int GetNumSelects(){
		return NumSelects;
	}
	
//	public void SetNumJoins(int cnt){
//		NumJoins = cnt;
//	}
	public int GetNumJoins(){
		return NumJoins;
	}
	
	public void SetSelect(QCmpOp elt){
		NumSelects++;
		NumPredicates++;
		if (Selects != null) elt.SetNext(Selects);
		Selects = elt;
	}
	public QCmpOp GetSelect() {
		return Selects;
	}
	
	public void SetJoins(QCmpOp elt){
		NumJoins++;
		NumPredicates++;
		if (Joins != null) elt.SetNext(Joins);
		Joins = elt;

	}
}
