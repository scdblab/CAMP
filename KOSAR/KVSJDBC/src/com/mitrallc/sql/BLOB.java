package com.mitrallc.sql;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

public class BLOB implements Blob {
	private byte[] payload=null;
	private long length;

	@Override
	public long length() throws SQLException {
		// TODO Auto-generated method stub
		return length;
	}

	@Override
	public byte[] getBytes(long paramLong, int paramInt) throws SQLException {
		paramLong = paramLong - 1; //the ordinal position of the first byte in the BLOB value to be extracted; the first byte is at position 1
		if (paramLong < 0) paramLong = 0;
		if (paramLong != 0)
			System.out.println("Error in KosarSolo:  Blob is returning the wrong array at offset 0 instead of "+paramLong);
		return payload;
	}

	@Override
	public InputStream getBinaryStream() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long position(byte[] paramArrayOfByte, long paramLong)
			throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long position(Blob paramBlob, long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int setBytes(long paramLong, byte[] paramArrayOfByte)
			throws SQLException {
		// Make a copy of the provided content
		paramLong = paramLong - 1; //the ordinal position of the first byte in the BLOB value to be extracted; the first byte is at position 1
		if (paramLong < 0) paramLong = 0;
		int size = paramArrayOfByte.length;
		if (payload == null){
			payload = new byte[(int)paramLong+(int)size];
			length = paramLong + size;
		}
		else {
			if (size + paramLong > length){
				byte[] p = new byte[(int)paramLong+(int)size];
				for (int i=0; i < length; i++)
					p[i]=payload[i];
				payload = p;
				length = (int)paramLong+size;
			}
		}
		
		for (int i=0; i < size; i++)
			payload[(int)paramLong+i] = paramArrayOfByte[i];
		
		return (int) size;
	}

	@Override
	public int setBytes(long paramLong, byte[] paramArrayOfByte, int paramInt1,
			int paramInt2) throws SQLException {
		// Make a copy of the provided content
		int offset = paramInt1; //the offset into the array bytes at which to start reading the bytes to be set
		int size = paramInt2;  //the number of bytes to be written to the BLOB value from array of bytes
		if (payload == null){
			payload = new byte[(int)paramLong+(int)size];
			length = paramLong + size;
		} else {
			if (size + paramLong > length){
				byte[] p = new byte[(int)paramLong+(int)size];
				for (int i=0; i < length; i++)
					p[i]=payload[i];
				payload = p;
				length = (int)paramLong+size;
			}
		}
		
		for (int i=0; i < size; i++)
			payload[(int)paramLong+i] = paramArrayOfByte[offset+i];
		return size;
	}

	@Override
	public OutputStream setBinaryStream(long paramLong) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void truncate(long paramLong) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void free() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public InputStream getBinaryStream(long paramLong1, long paramLong2)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
