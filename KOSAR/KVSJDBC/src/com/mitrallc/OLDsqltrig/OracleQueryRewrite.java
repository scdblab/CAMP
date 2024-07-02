package com.mitrallc.sqltrig;

public class OracleQueryRewrite {
	public static String rownum = "ROWNUM";
	public static String orderby = "ORDER";
	public static String and = "AND";
	public static String or = "OR";
	public static String paranthesis = "(";
	
	static String[] sampleqrys = {
		"SELECT * FROM resources WHERE walluserid = 123 AND rownum <5",
		"SELECT * FROM resources WHERE walluserid = 123 AND rownum <5 ORDER BY rid desc",
		"SELECT * FROM resources WHERE walluserid = 123 AND rownum <5 and salary > 23000 ORDER BY rid desc",
		"SELECT * FROM resources WHERE rownum <5 AND walluserid = 123 ORDER BY rid desc"
	};
	
	private static int RemoveBooleanAtTheBeginning(String qry){
		String capqry = qry.toUpperCase().trim();
		if (capqry.startsWith(and)){
			return and.length();
		}
		if (capqry.endsWith(or)){
			return or.length();
		}
		return 0;
	}
	
	private static int RemoveBooleanAtTheEnd(String qry){
		String capqry = qry.toUpperCase().trim();
		if (capqry.endsWith(and)){
			return (qry.length() - and.length());
		}
		if (capqry.endsWith(or)){
			return (qry.length() - or.length());
		}
		return qry.length();
	}
	
	public static String ReWriteQry(String qry){
		String capqry = qry.toUpperCase();
		int idx = capqry.indexOf(rownum);
		if (idx > 0){
			int idx2 = capqry.substring(idx).indexOf(and);
			int idx3 = capqry.substring(idx).indexOf(or);
			int idx4 = capqry.substring(idx).indexOf(paranthesis);

			if (idx2 < 0 && idx3 < 0 && idx4 < 0){
				int charsTOdrop = RemoveBooleanAtTheEnd(qry.substring(0,idx).trim());
				return qry.trim().substring(0, charsTOdrop); //return the string without rownum
			}
			
			idx2 += idx;
			idx3 += idx;
			idx4 += idx;
			
			
			if (idx2 < idx) idx2 = qry.length();
			if (idx3 < idx) idx3 = qry.length();
			if (idx4 < idx) idx4 = qry.length();
			
			if (idx2 < idx3 && idx2 < idx4){
				int charsTOdrop = RemoveBooleanAtTheEnd(qry.substring(0,idx).trim());
				int beginoffset = 0;
				if ( charsTOdrop == qry.substring(0,idx).trim().length() ){
					beginoffset = RemoveBooleanAtTheBeginning(qry.substring(idx2).trim());
				}
				return qry.trim().substring(0,charsTOdrop)+" "+qry.trim().substring(idx2+beginoffset);
			} else if (idx3 < idx2 && idx3 < idx4){
				int charsTOdrop = RemoveBooleanAtTheEnd(qry.substring(0,idx).trim());
				return qry.trim().substring(0,charsTOdrop)+" "+qry.substring(idx3);
			} else if (idx4 < idx2 && idx4 < idx3){
				int charsTOdrop = RemoveBooleanAtTheEnd(qry.substring(0,idx).trim());
				return qry.trim().substring(0,charsTOdrop)+" "+ qry.substring(idx4);
			}
		}
		return qry;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		for (int i = 0; i < sampleqrys.length; i++)
			System.out.println(""+ReWriteQry(sampleqrys[i]));
	}

}
