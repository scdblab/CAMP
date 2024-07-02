package com.mitrallc.sql;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import com.mitrallc.kosar.exceptions.KosarSQLException;

public class ResultSet implements java.sql.ResultSet {
	private com.mitrallc.sql.ResultSetMetaData RSMD;
	private java.sql.ResultSet resultSet;
	private Hashtable<String, ColumnData> map;
	//private Vector<String> colIndex;

	private int current_row;
	private int num_columns;
	private int num_rows;
	
	private static final int deserialize_buffer_size = 65536;
	
	private static final String TYPE_VARCHAR = "VARCHAR";
	private static final String TYPE_VARCHAR2 = "VARCHAR2";
	private static final String TYPE_NUMBER = "NUMBER";
	private static final String TYPE_BLOB = "BLOB";
	private static final String TYPE_LONG_BLOB = "LONGBLOB";  //used by MySQL
	private static final String TYPE_DATETIME = "DATETIME";
	private static final String TYPE_DATE = "DATE";
	private static final String TYPE_FLOAT = "FLOAT";
	private static final String TYPE_INT = "INT";
	private static final String TYPE_INTEGER = "INTEGER";
	private static final String TYPE_BIGINT = "BIGINT";
	private static final boolean EXCEPTION_FOR_UNSUPPORTED = true;
	private static int NumRows = 200;
	
	private String[] colIndex;

	
	private static final boolean verbose = false;
	

	
	
	private class ColumnData
	{
		public String tableName;
		public String type;
		//public Object value;
		public Object[] values = new Object[NumRows];
		//public Vector<Object> values;
		
		private Object MakeCopy(Object cd){
			Object result = null;
			if( cd == null )
			{
				System.err.println("Error. Cannot make a copy of a null object.");
				return null;
			}

			if( cd.getClass().equals(TYPE_VARCHAR2) || cd.getClass().toString().contains("String") )
			{
				// TODO: figure out set. should that be used instead
				//cd.values.set(index, rs.getString(col_name));
				//cd.values.add(rs.getString(col_name));
				return (Object) new String(cd.toString());
			}
			else if( cd.getClass().equals(TYPE_NUMBER) )
			{		
				//cd.values.add(rs.getObject(col_name));
				return (Object) new Double(cd.toString());
			}
			else if( cd.getClass().equals(TYPE_INTEGER) || cd.getClass().toString().contains("java.lang.Integer"))
			{
				//cd.values.add(rs.getInt(col_name));
				return (Object) new Integer(cd.toString());
			}
			else if(cd.getClass().equals(TYPE_INT)) 
			{
				//cd.values.add(rs.getInt(col_name));
				return (Object) new Integer(cd.toString());
			}
			else if( cd.getClass().equals(TYPE_FLOAT) )
			{
				//cd.values.add(rs.getFloat(col_name));
				return (Object) new Float(cd.toString());
			}
			else if( cd.getClass().toString().contains("BLOB") )
			{
				//cd.values.add(rs.getBlob(col_name));
				Blob aBlob = (Blob) cd;
				byte[] allBytesInBlob;
				Blob bBlob = null;
				try {
					allBytesInBlob = aBlob.getBytes(1, (int) aBlob.length());
					bBlob = new BLOB();
					if (bBlob.setBytes(1, allBytesInBlob) != allBytesInBlob.length)
						System.out.println("Error in KosarSolo:ResultSet:  Failed to write "+allBytesInBlob.length+" bytes into the new BLOB.");
				} catch (SQLException e) {
					try {
						throw new KosarSQLException(e.getMessage());
					} catch (KosarSQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				return (Object) bBlob;
				//cd.values[index] = rs.getBlob(col_name);
			}
			else if( cd.getClass().equals(TYPE_DATETIME) || cd.getClass().equals(TYPE_DATE) )
			{
				//cd.values.add(rs.getDate(col_name));
				return cd;
			}
			else if ( cd.getClass().toString().contains("BigDecimal") ){
				return (Object) new BigDecimal(cd.toString());
			}
			else
			{
				System.out.println("Error in ColumnData:  Unsupported type: " + cd.getClass());
				//cd.values[index] = rs.getObject(col_name);
				//cd.values.add(rs.getObject(col_name));
				//cd.values.set(index, rs.getObject(col_name));
			}
			return result;
		}
		
		public ColumnData MakeCopy(){
			ColumnData cpy = new ColumnData();
			cpy.tableName = this.tableName;
			cpy.type = this.type;
			cpy.values = new Object[this.values.length];
			for (int i=0; i < this.values.length; i++) {
				if (this.values[i] != null)
					cpy.values[i] = MakeCopy(this.values[i]);   //this.values[i];
				else cpy.values[i] = null;
			}
			return cpy;
		}
	}
	
	public ResultSet()
	{
		this.init();
		this.map = new Hashtable<String, ColumnData>();
		//this.colIndex = new Vector<String>();
		this.colIndex = new String[this.num_columns];
	}
	
	public ResultSet( java.sql.ResultSet rs )
	{
		this.init();
		ColumnData cd = null;
		try {	
			this.resultSet = rs; 
			ResultSetMetaData meta = rs.getMetaData();
			int num_cols = meta.getColumnCount();
			this.num_columns = num_cols;
			
			this.map = new Hashtable<String, ColumnData>();
			//this.colIndex = new Vector<String>();
			this.colIndex = new String[num_cols];
			
			for( int i = 1; i < num_cols+1; i++ )
			{
				String col_name = meta.getColumnLabel(i).toUpperCase();
								
				cd = new ColumnData();
				cd.tableName = meta.getTableName(i);
				cd.type = meta.getColumnTypeName(i);
				//cd.values = new Object[NumRows];
				//cd.values = new Vector<Object>();

				this.map.put(col_name, cd);	
				this.colIndex[i-1] = col_name;
				//this.colIndex.add(col_name);
			}
			
			this.num_rows = 0;
			while( rs.next() )
			{
				for( String column : this.map.keySet() )
				{
					this.setColumnValue(column, this.num_rows, rs);
				}
				this.num_rows++;

				if (cd != null & this.num_rows >= cd.values.length){
					for (String column : this.map.keySet() ){
						cd = this.map.get(column);
						int myArraySize = cd.values.length;
						Object mynewArray[] = new Object[myArraySize*2];
						for (int i=0; i < cd.values.length; i++)
							mynewArray[i]=cd.values[i];
						cd.values = mynewArray;
					}
				}
			}
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace();
			}
		}
		if (verbose) System.out.println("num_rows="+num_rows+", current_row="+this.current_row);
	}
	
	public Hashtable<String, ColumnData> CopyHashTable(Hashtable<String, ColumnData> H){
		Hashtable<String, ColumnData> mp = new Hashtable<String, ColumnData>();
		ColumnData C = null;
		String T = null;
		Enumeration tokens;
		tokens = H.keys();
		while (tokens.hasMoreElements()){
			T = (String) tokens.nextElement(); 
			C = H.get(T);
			if (C != null)
				mp.put(T, C.MakeCopy());
		}
		return mp;
	}
	
	/***
	 * Deep copy of another com.rays.sql.ResultSet object
	 * @param rs
	 */
	public ResultSet( com.mitrallc.sql.ResultSet rs )
	{
		this.init();
		this.num_columns = rs.num_columns;
		this.num_rows = rs.num_rows;
		this.map = CopyHashTable(rs.map);
		//this.map = (Hashtable<String, ColumnData>) rs.map.clone();
		this.colIndex = new String[rs.colIndex.length];
		for (int i=0; i < rs.colIndex.length; i++)
			colIndex[i]=rs.colIndex[i];
		//this.colIndex = (Vector<String>) rs.colIndex.clone();
	}
	
	public Object clone()
	{
		return new ResultSet(this);
	}
	
	private void init()
	{
		this.current_row = -1;
		this.num_columns = 0;
		this.num_rows = 0;
		this.resultSet = null;
		
		this.map = null;
		this.colIndex = null;
	}
	
	private String getColumnName( int columnIndex )
	{
		return this.colIndex[columnIndex-1];
	}
	
//	private void setColumnValue( int col_id, int index, java.sql.ResultSet rs ) throws SQLException
//	{
//		ColumnData cd = this.map.get(col_name);
//		if( cd == null )
//		{
//			System.err.println("Error. Column not registered with Hashtable");
//			return;
//		}
//		
//		if( cd.type.equals(TYPE_VARCHAR2) )
//		{
//			// TODO: figure out set. should that be used instead
//			//cd.values.set(index, rs.getString(col_name));
//			//cd.values.add(rs.getString(col_name));
//			cd.values[index] = rs.getString(col_id);
//		}
//		else if( cd.type.equals(TYPE_NUMBER) )
//		{		
//			//cd.values.add(rs.getObject(col_name));
//			String val = rs.getObject(col_id).toString();
//			cd.values[index] = rs.getObject(col_id);
//		}
//		else if( cd.type.equals(TYPE_INTEGER) )
//		{
//			//cd.values.add(rs.getInt(col_name));
//			cd.values[index] = rs.getInt(col_id);
//		}
//		else if( cd.type.equals(TYPE_FLOAT) )
//		{
//			//cd.values.add(rs.getFloat(col_name));
//			cd.values[index] = rs.getFloat(col_id);
//		}
//		else if( cd.type.equals(TYPE_BLOB) )
//		{
//			//cd.values.add(rs.getBlob(col_name));
//			cd.values[index] = rs.getBlob(col_id);
//		}
//		else if( cd.type.equals(TYPE_DATETIME) || cd.type.equals(TYPE_DATE) )
//		{
//			//cd.values.add(rs.getDate(col_name));
//			cd.values[index] = rs.getDate(col_id);
//		}
//		else
//		{
//			System.out.println("Unsupported type: " + cd.type);
//			cd.values[index] = rs.getObject(col_id);
//			//cd.values.add(rs.getObject(col_name));
//			//cd.values.set(index, rs.getObject(col_name));
//		}
//	}
	
	private void setColumnValue( String col_name, int index, java.sql.ResultSet rs ) throws SQLException
	{
		ColumnData cd = this.map.get(col_name);
		if( cd == null )
		{
			System.err.println("Error. Column not registered with Hashtable");
			return;
		}
		
		if( cd.type.equals(TYPE_VARCHAR2) )
		{
			// TODO: figure out set. should that be used instead
			//cd.values.set(index, rs.getString(col_name));
			//cd.values.add(rs.getString(col_name));
			cd.values[index] = rs.getString(col_name);
		}
		else if( cd.type.equals(TYPE_VARCHAR) )
		{
			// TODO: figure out set. should that be used instead
			//cd.values.set(index, rs.getString(col_name));
			//cd.values.add(rs.getString(col_name));
			cd.values[index] = rs.getString(col_name);
		}
		else if( cd.type.equals(TYPE_NUMBER) )
		{		
			//cd.values.add(rs.getObject(col_name));
			Object ob = rs.getObject(col_name);
			//String val = rs.getObject(col_name).toString();
			cd.values[index] = rs.getObject(col_name);
		}
		else if( cd.type.equals(TYPE_INTEGER) )
		{
			//cd.values.add(rs.getInt(col_name));
			cd.values[index] = rs.getInt(col_name);
		}
		else if(cd.type.equals(TYPE_INT)) 
		{
			//cd.values.add(rs.getInt(col_name));
			cd.values[index] = rs.getInt(col_name);
		}
		else if( cd.type.equals(TYPE_BIGINT) )
		{
			//cd.values.add(rs.getInt(col_name));
			cd.values[index] = rs.getInt(col_name);
		}
		else if( cd.type.equals(TYPE_FLOAT) )
		{
			//cd.values.add(rs.getFloat(col_name));
			cd.values[index] = rs.getFloat(col_name);
		}
		else if( cd.type.equals(TYPE_BLOB) || cd.type.equals(TYPE_LONG_BLOB))
		{
			//cd.values.add(rs.getBlob(col_name));
			cd.values[index] = rs.getBlob(col_name);
		}
		else if( cd.type.equals(TYPE_DATETIME) || cd.type.equals(TYPE_DATE) )
		{
			//cd.values.add(rs.getDate(col_name));
			cd.values[index] = rs.getDate(col_name);
		}
		else
		{
			System.out.println("Unsupported type: " + cd.type);
			cd.values[index] = rs.getObject(col_name);
			//cd.values.add(rs.getObject(col_name));
			//cd.values.set(index, rs.getObject(col_name));
		}
	}
	
	private void outputValue( DataOutputStream out, String type, Object value ) throws IOException, SQLException
	{
		if( type.equals(TYPE_VARCHAR2) )
		{
			if( value == null )
			{
				out.writeInt(-1);
			}
			else
			{
				out.writeInt(((String)value).length());
				out.writeBytes((String)value);
			}
		}
		else if( type.equals(TYPE_NUMBER) )
		{
			if(value.getClass().toString().contains("java.lang.Integer")) {
				out.writeInt( ((Integer)value).intValue() );
			}
			else {
				out.writeInt( ((BigDecimal)value).intValue() );
			}
			
		}
		else if( type.equals(TYPE_BLOB) )
		{			
			long size = ((Blob)value).length();
			out.writeLong(size);
			
			//TODO: currently only handles BLOBs up to 2GB in size (max signed int)
			if( size > Integer.MAX_VALUE )
			{
				System.err.println("Error: BLOB size too large");
			}
			out.write( ((Blob)value).getBytes(1, (int)size) );
			
		}
		else
		{
			System.err.println("Error. Value type '" + type + "' not supported");
		}
	}
	
	private Object inputValue( DataInputStream in, String type, byte[] buffer ) throws IOException, SerialException, SQLException
	{
		int field_size = 0;
		
		if( type.equals(TYPE_VARCHAR2) )
		{
			field_size = in.readInt();
			if( field_size == -1 )
			{
				return null;
			}
			
			this.readObject(in, field_size, buffer);
			return new String(buffer, 0, field_size);
		}
		else if( type.equals(TYPE_NUMBER) )
		{
			return in.readInt();
		}
		else if( type.equals(TYPE_BLOB) )
		{
			long size = in.readLong();
			if( size > buffer.length )
			{
				System.err.println("Error: buffer size too small");
			}
			this.readObject(in, (int)size, buffer);
			return new SerialBlob(buffer);
		}
		else
		{
			System.err.println("Error. Value type '" + type + "' not supported");
		}
		
		return null;
	}
	
	public byte[] serialize()
	{
		try
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bos);
			out.writeInt(this.num_columns);
			out.writeInt(this.num_rows);
			
			int col_counter = 0;
			//for( String column_name : this.map.keySet() )
			String column_name;
			for (int k=0; k < this.colIndex.length; k++)
			//for( String column_name : this.colIndex )
			{
				column_name = this.colIndex[k];
				out.writeInt(column_name.length());
				out.writeBytes(column_name);
				
				ColumnData cd = this.map.get(column_name);
				out.writeInt(cd.tableName.length());
				out.writeBytes(cd.tableName);
				out.writeInt(cd.type.length());
				out.writeBytes(cd.type);
				
				if( cd.values.length != this.num_rows )
				{
					//System.out.println("Warning: num_rows does not match the hashtable contents (" 
					//		+ this.num_rows + " vs " + cd.values.length + ")");
				}
				
				for( int i = 0; i < cd.values.length; i++ )
				{
					if(cd.values[i]==null) 
						break;
					
					this.outputValue(out, cd.type, cd.values[i] );
				}
				col_counter++;
			}
			
			if( col_counter != this.num_columns )
			{
				System.out.println("Warning: num_columns does not match the hashtable (" 
						+ num_columns + " vs " + col_counter + ")");
			}
			
			out.flush();
			out.close();
			
			return bos.toByteArray();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace();
			}
		}
		
		return null;
	}
	
	private void readObject( DataInputStream in, int num_bytes, byte[] byte_array ) throws IOException
	{
		int total_bytes_read = 0;
		int bytes_read = 0;
		
		while( total_bytes_read < num_bytes )
		{
			bytes_read = in.read(byte_array, total_bytes_read, num_bytes - total_bytes_read);
			total_bytes_read += bytes_read;
		}		
	}
	
	public int deserialize(byte[] byte_array)
	{
		try
		{
			// Reset fields
			this.init();
			this.map = new Hashtable<String, ColumnData>();
			//this.colIndex = new Vector<String>();

			
			// Read from byte_array
			ByteArrayInputStream bis = new ByteArrayInputStream(byte_array);
			DataInputStream in = new DataInputStream(bis);
			this.num_columns = in.readInt();
			this.num_rows = in.readInt();
			
			this.colIndex = new String[num_columns];
			
			byte[] buffer = new byte[deserialize_buffer_size];
			String temp_str;
			int field_size = 0;		
			String column_name = null;
			for( int col = 0; col < this.num_columns; col++ )
			{
				ColumnData cd = new ColumnData();
				
				field_size = in.readInt();
				this.readObject(in, field_size, buffer);
				column_name = new String(buffer, 0, field_size);
				
				field_size = in.readInt();
				this.readObject(in, field_size, buffer);
				cd.tableName = new String(buffer, 0, field_size);
				
				field_size = in.readInt();
				this.readObject(in, field_size, buffer);
				cd.type = new String(buffer, 0, field_size);
				
				//cd.values = new Vector<Object>();
				cd.values = new Object[this.num_rows];
				
				for( int row = 0; row < this.num_rows; row++ )
				{
					cd.values[row]=this.inputValue(in, cd.type, buffer );
				}
				
				this.map.put(column_name, cd);
				this.colIndex[col]=column_name;
			}
			in.close();
			bis.close();
			
			return 0;
		}
		catch( IOException e )
		{
			e.printStackTrace();
		} catch (SerialException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			try {
				throw new KosarSQLException(e.getMessage());
			} catch (KosarSQLException e1) {
				e1.printStackTrace();
			}
		}
		return -1;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public boolean next() throws SQLException {
		// TODO Auto-generated method stub
		// this.num_rows - 1 because current_row starts from -1 instead of 0
		if( this.num_rows > 0 && this.current_row < this.num_rows - 1 )
		{
			this.current_row++;
			return true;
		}
		return false;
	}

	@Override
	public void close() throws SQLException {
		// TODO Auto-generated method stub
		if( this.resultSet != null )
		{
			this.resultSet.close();
		}
	}

	@Override
	public boolean wasNull() throws SQLException {
		// TODO Auto-generated method stub
		return map.keySet().size()<=0;
	}

	@Override
	public String getString(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		String col_name = this.getColumnName(paramInt);
		return this.getString(col_name);
	}

	@Override
	public boolean getBoolean(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public byte getByte(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public short getShort(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public int getInt(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		String col_name = this.getColumnName(paramInt);		
		return this.getInt(col_name);
	}

	@Override
	public long getLong(int paramInt) throws SQLException {
		String col_name = this.getColumnName(paramInt);		
		return this.getLong(col_name);
	}

	@Override
	public float getFloat(int paramInt) throws SQLException {
		String col_name = this.getColumnName(paramInt);		
		return this.getFloat(col_name);
	}

	@Override
	public double getDouble(int paramInt) throws SQLException {
		String col_name = this.getColumnName(paramInt);		
		return this.getDouble(col_name);
	}

	@Override
	public BigDecimal getBigDecimal(int paramInt1, int paramInt2)
			throws SQLException {
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public byte[] getBytes(int paramInt) throws SQLException {
		String col_name = this.getColumnName(paramInt);		
		return this.getBytes(col_name);
	}

	@Override
	public Date getDate(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		String col_name = this.getColumnName(paramInt);		
		return this.getDate(col_name);
	}

	@Override
	public Time getTime(int paramInt) throws SQLException {
		String col_name = this.getColumnName(paramInt);		
		return this.getTime(col_name);
	}

	@Override
	public Timestamp getTimestamp(int paramInt) throws SQLException {
		String col_name = this.getColumnName(paramInt);		
		return this.getTimestamp(col_name);
	}

	@Override
	public InputStream getAsciiStream(int paramInt) throws SQLException {
		return resultSet.getAsciiStream(paramInt);
	}

	@Override
	public InputStream getUnicodeStream(int paramInt) throws SQLException {
		return resultSet.getUnicodeStream(paramInt);
	}

	@Override
	public InputStream getBinaryStream(int paramInt) throws SQLException {
		return resultSet.getBinaryStream(paramInt);
	}

	@Override
	public String getString(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		ColumnData cd = this.map.get(paramString.toUpperCase());
		if( cd != null )
		{
			if( cd.type == TYPE_VARCHAR2)
			{
				return (String)cd.values[this.current_row];
			}
			else
			{
				if (cd.values[this.current_row]==null) return null;
				return cd.values[this.current_row].toString();
			}
		}
		
		// if( this.resultSet != null ) return this.resultSet.getString(columnLabel);
		
		return null;
	}

	@Override
	public boolean getBoolean(String paramString) throws SQLException {
		return resultSet.getBoolean(paramString);
	}

	@Override
	public byte getByte(String paramString) throws SQLException {
		return resultSet.getByte(paramString);
	}

	@Override
	public short getShort(String paramString) throws SQLException {
		return resultSet.getShort(paramString);
	}

	@Override
	public int getInt(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		Object obj = this.getObject(paramString);
		if( obj == null )
		{
			// TODO: what to return for NULL column value?
			return 0;
		}
			
		
		if( obj.getClass().toString().equals("class java.lang.String") )
		{
			return Integer.parseInt((String)obj);
		} else if( obj.getClass().toString().equals("class java.math.BigDecimal") )
		{
			return ((BigDecimal)obj).intValue();
		}
		return (Integer)obj;
	}

	@Override
	public long getLong(String paramString) throws SQLException {
		Object obj = this.getObject(paramString);
		if( obj == null )
		{
			// TODO: what to return for NULL column value?
			return 0;
		}
			
		System.out.println("Class is "+obj.getClass().toString());
		if( obj.getClass().toString().equals("class java.lang.String") )
		{
			return Long.parseLong((String)obj);
		} else if( obj.getClass().toString().equals("class java.math.BigDecimal") )
		{
			return ((BigDecimal)obj).longValue();
		} else if (obj.getClass().toString().equals("class java.lang.Integer"))
		{
			return ((Integer)obj).intValue();
		}
		return (Long)obj;
	}

	@Override
	public float getFloat(String paramString) throws SQLException {
		Object obj = this.getObject(paramString);
		if( obj == null )
		{
			// TODO: what to return for NULL column value?
			return 0;
		}
			
		
		if( obj.getClass().toString().equals("class java.lang.String") )
		{
			return Float.parseFloat((String)obj);
		} else if( obj.getClass().toString().equals("class java.math.BigDecimal") )
		{
			return ((BigDecimal)obj).floatValue();
		}
		return (Float)obj;
	}

	@Override
	public double getDouble(String paramString) throws SQLException {
		Object obj = this.getObject(paramString);
		if( obj == null )
		{
			// TODO: what to return for NULL column value?
			return 0;
		}
			
		
		if( obj.getClass().toString().equals("class java.lang.String") )
		{
			return Double.parseDouble((String)obj);
		} else if( obj.getClass().toString().equals("class java.math.BigDecimal") )
		{
			return ((BigDecimal)obj).doubleValue();
		}
		return (Double)obj;
	}

	@Override
	public BigDecimal getBigDecimal(String paramString, int paramInt)
			throws SQLException {
		Object obj = this.getObject(paramString);
		if( obj == null )
		{
			// TODO: what to return for NULL column value?
			return null;
		}
			
		
		if( obj.getClass().toString().equals("class java.lang.String") )
		{
			return new BigDecimal((String)obj);
		} else if( obj.getClass().toString().equals("class java.math.BigDecimal") )
		{
			return ((BigDecimal)obj);
		}
		return (BigDecimal)obj;
	}

	@Override
	public byte[] getBytes(String paramString) throws SQLException {
		ColumnData cd = this.map.get(paramString.toUpperCase());
		if (cd != null) {
			if (cd.values[this.current_row]==null) return null;
			return cd.values[this.current_row].toString().getBytes();
		}
		return null;
	}

	@Override
	public Date getDate(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		Object obj = this.getObject(paramString);
		if( obj == null )
		{
			// TODO: what to return for NULL column value?
			return null;
		}
			
		
		if( obj.getClass().toString().equals("class java.lang.String") )
		{
			// TODO: handle this?
			//return new Date((String)obj);
		}
		return (Date)obj;
	}

	@Override
	public Time getTime(String paramString) throws SQLException {
		ColumnData cd = this.map.get(paramString.toUpperCase());
		if (cd != null) {
			if (cd.values[this.current_row]==null) return null;
			return (Time)cd.values[this.current_row];
		}
		return null;
	}

	@Override
	public Timestamp getTimestamp(String paramString) throws SQLException {
		ColumnData cd = this.map.get(paramString.toUpperCase());
		if (cd != null) {
			if (cd.values[this.current_row]==null) return null;
			return (Timestamp)cd.values[this.current_row];
		}
		return null;
	}

	@Override
	public InputStream getAsciiStream(String paramString) throws SQLException {
		return resultSet.getAsciiStream(paramString);
	}

	@Override
	public InputStream getUnicodeStream(String paramString) throws SQLException {
		return resultSet.getUnicodeStream(paramString);
	}

	@Override
	public InputStream getBinaryStream(String paramString) throws SQLException {
		return resultSet.getBinaryStream(paramString);
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return resultSet.getWarnings();

	}

	@Override
	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub
		this.clearWarnings();
	}

	@Override
	public String getCursorName() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		// TODO Auto-generated method stub
		//if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		//return null;
		if (this.resultSet == null)
		{
			if (this.RSMD != null) return (java.sql.ResultSetMetaData) this.RSMD;
			else {
				this.RSMD = new com.mitrallc.sql.ResultSetMetaData();
				this.RSMD.setColumnCount(this.num_columns);
				this.RSMD.setColumnNames(colIndex);
				return (java.sql.ResultSetMetaData) this.RSMD;
			}
		}
		return this.resultSet.getMetaData();
	}

	@Override
	public Object getObject(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		String columnLabel = this.getColumnName(paramInt);
		if( columnLabel != null )
		{
			return this.getObject(columnLabel);
		}
		return null;
	}

	@Override
	public Object getObject(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( paramString == null )
		{
			return null;
		}
	
		ColumnData cd = this.map.get(paramString.toUpperCase());
		if( cd != null )
		{
			return cd.values[this.current_row];
		}
		return null;
	}

	@Override
	public int findColumn(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public Reader getCharacterStream(int paramInt) throws SQLException {
		return resultSet.getCharacterStream(paramInt);
	}

	@Override
	public Reader getCharacterStream(String paramString) throws SQLException {
		return resultSet.getCharacterStream(paramString);
	}

	@Override
	public BigDecimal getBigDecimal(int paramInt) throws SQLException {
		String col_name = this.getColumnName(paramInt);		
		return this.getBigDecimal(col_name);
	}

	@Override
	public BigDecimal getBigDecimal(String paramString) throws SQLException {
		return this.getBigDecimal(paramString);

	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean isFirst() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean isLast() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public void beforeFirst() throws SQLException {
		// TODO Auto-generated method stub
		this.beforeFirst();
	}

	@Override
	public void afterLast() throws SQLException {
		// TODO Auto-generated method stub
		this.afterLast();
	}

	@Override
	public boolean first() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean last() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public int getRow() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public boolean absolute(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean relative(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean previous() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public void setFetchDirection(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public void setFetchSize(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public int getType() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public int getConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean rowInserted() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public void updateNull(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBoolean(int paramInt, boolean paramBoolean)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateByte(int paramInt, byte paramByte) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateShort(int paramInt, short paramShort) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateInt(int paramInt1, int paramInt2) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateLong(int paramInt, long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateFloat(int paramInt, float paramFloat) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateDouble(int paramInt, double paramDouble)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBigDecimal(int paramInt, BigDecimal paramBigDecimal)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateString(int paramInt, String paramString)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBytes(int paramInt, byte[] paramArrayOfByte)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateDate(int paramInt, Date paramDate) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateTime(int paramInt, Time paramTime) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateTimestamp(int paramInt, Timestamp paramTimestamp)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateAsciiStream(int paramInt1, InputStream paramInputStream,
			int paramInt2) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBinaryStream(int paramInt1, InputStream paramInputStream,
			int paramInt2) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateCharacterStream(int paramInt1, Reader paramReader,
			int paramInt2) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateObject(int paramInt1, Object paramObject, int paramInt2)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateObject(int paramInt, Object paramObject)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNull(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBoolean(String paramString, boolean paramBoolean)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateByte(String paramString, byte paramByte)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateShort(String paramString, short paramShort)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateInt(String paramString, int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateLong(String paramString, long paramLong)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateFloat(String paramString, float paramFloat)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateDouble(String paramString, double paramDouble)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBigDecimal(String paramString, BigDecimal paramBigDecimal)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateString(String paramString1, String paramString2)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBytes(String paramString, byte[] paramArrayOfByte)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateDate(String paramString, Date paramDate)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateTime(String paramString, Time paramTime)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateTimestamp(String paramString, Timestamp paramTimestamp)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateAsciiStream(String paramString,
			InputStream paramInputStream, int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBinaryStream(String paramString,
			InputStream paramInputStream, int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateCharacterStream(String paramString, Reader paramReader,
			int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateObject(String paramString, Object paramObject,
			int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateObject(String paramString, Object paramObject)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void insertRow() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateRow() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void deleteRow() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void refreshRow() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void moveToInsertRow() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public Statement getStatement() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Object getObject(int paramInt, Map<String, Class<?>> paramMap)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Ref getRef(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Blob getBlob(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		String col_name = this.getColumnName(paramInt);
		return (Blob) this.getBlob(col_name);
	}

	@Override
	public Clob getClob(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		return (Clob) this.getClob(paramInt);
	}

	@Override
	public Array getArray(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		return (Array) this.getArray(paramInt);
	}

	@Override
	public Object getObject(String paramString, Map<String, Class<?>> paramMap)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Ref getRef(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Blob getBlob(String paramString) throws SQLException {
		ColumnData cd = this.map.get(paramString.toUpperCase());
		if( cd != null )
		{
			if( cd.type == TYPE_BLOB )
			{
				return (Blob)cd.values[this.current_row];
			}
		}
		
		// TODO Auto-generated method stub
		return (Blob) this.getObject(paramString);
	}

	@Override
	public Clob getClob(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		return (Clob) this.getObject(paramString);
	}

	@Override
	public Array getArray(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		return (Array) this.getObject(paramString);
	}

	@Override
	public Date getDate(int paramInt, Calendar paramCalendar)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Date getDate(String paramString, Calendar paramCalendar)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Time getTime(int paramInt, Calendar paramCalendar)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Time getTime(String paramString, Calendar paramCalendar)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Timestamp getTimestamp(int paramInt, Calendar paramCalendar)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Timestamp getTimestamp(String paramString, Calendar paramCalendar)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public URL getURL(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public URL getURL(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public void updateRef(int paramInt, Ref paramRef) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateRef(String paramString, Ref paramRef) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBlob(int paramInt, Blob paramBlob) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBlob(String paramString, Blob paramBlob)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateClob(int paramInt, Clob paramClob) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateClob(String paramString, Clob paramClob)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateArray(int paramInt, Array paramArray) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateArray(String paramString, Array paramArray)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public RowId getRowId(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public RowId getRowId(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public void updateRowId(int paramInt, RowId paramRowId) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateRowId(String paramString, RowId paramRowId)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return false;
	}

	@Override
	public void updateNString(int paramInt, String paramString)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNString(String paramString1, String paramString2)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNClob(int paramInt, NClob paramNClob) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNClob(String paramString, NClob paramNClob)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public NClob getNClob(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public NClob getNClob(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public SQLXML getSQLXML(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public SQLXML getSQLXML(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public void updateSQLXML(int paramInt, SQLXML paramSQLXML)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateSQLXML(String paramString, SQLXML paramSQLXML)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public String getNString(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public String getNString(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Reader getNCharacterStream(int paramInt) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public Reader getNCharacterStream(String paramString) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		return null;
	}

	@Override
	public void updateNCharacterStream(int paramInt, Reader paramReader,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNCharacterStream(String paramString, Reader paramReader,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateAsciiStream(int paramInt, InputStream paramInputStream,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBinaryStream(int paramInt, InputStream paramInputStream,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateCharacterStream(int paramInt, Reader paramReader,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateAsciiStream(String paramString,
			InputStream paramInputStream, long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBinaryStream(String paramString,
			InputStream paramInputStream, long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateCharacterStream(String paramString, Reader paramReader,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBlob(int paramInt, InputStream paramInputStream,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBlob(String paramString, InputStream paramInputStream,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateClob(int paramInt, Reader paramReader, long paramLong)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateClob(String paramString, Reader paramReader,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNClob(int paramInt, Reader paramReader, long paramLong)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNClob(String paramString, Reader paramReader,
			long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNCharacterStream(int paramInt, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNCharacterStream(String paramString, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateAsciiStream(int paramInt, InputStream paramInputStream)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinaryStream(int paramInt, InputStream paramInputStream)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateCharacterStream(int paramInt, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateAsciiStream(String paramString,
			InputStream paramInputStream) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBinaryStream(String paramString,
			InputStream paramInputStream) throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateCharacterStream(String paramString, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBlob(int paramInt, InputStream paramInputStream)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateBlob(String paramString, InputStream paramInputStream)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateClob(int paramInt, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateClob(String paramString, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNClob(int paramInt, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public void updateNClob(String paramString, Reader paramReader)
			throws SQLException {
		// TODO Auto-generated method stub
		if( EXCEPTION_FOR_UNSUPPORTED ) throw new KosarSQLException("Error function not supported");
		
	}

	@Override
	public <T> T getObject(int arg0, Class<T> arg1) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getObject(String arg0, Class<T> arg1) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
