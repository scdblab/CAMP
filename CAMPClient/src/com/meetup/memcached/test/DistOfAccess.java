package com.meetup.memcached.test;

import java.util.Random;
import java.util.Vector;

public class DistOfAccess {
	Random m_randNumGenerator;
	String m_CurrentDist;
	double m_ZipfianMean;
	int m_numberOfItems;
	Vector<Double> m_DistLevels;
	Vector<Double> m_DistValues;
	Vector<Integer> m_SV;
	int m_SV_Length;
	boolean m_MakeRec;
	double m_nTime;
	boolean m_bBinarySearch; //  0 for linear, 1 for binary
	
	int LinearSearch(int nNum)
	{
		int randMovie = 0;
		for (int i=0; i<=m_numberOfItems; i++)
		{
			if (m_DistLevels.get(i) > nNum)
			{
				randMovie = i;
				break;
			}
		}
		return randMovie;
	}


	int BinarySearch(int nNum, int nStart, int nEnd)
	{
		int nIndex = (nEnd-nStart)/2;
		nIndex+=nStart;
		if(m_DistLevels.get(nIndex) <= nNum && m_DistLevels.get(nIndex+1) > nNum)
			return nIndex+1;
		else if(m_DistLevels.get(nIndex) >= nNum && m_DistLevels.get(nIndex+1) >= nNum)
			return BinarySearch(nNum, nStart, nIndex);
		else if(m_DistLevels.get(nIndex) <= nNum && m_DistLevels.get(nIndex+1) <= nNum)
			return BinarySearch(nNum, nIndex+1, nEnd);
		else
			return nEnd;
	}



	void InitZipfian(int numOfItems, double ZipfianMean)
	{
		m_numberOfItems = numOfItems;
		m_ZipfianMean = ZipfianMean;

		m_SV_Length = numOfItems+1;
		m_SV = new Vector<Integer>(m_SV_Length);

		for (int i = 0 ; i < numOfItems+1 ; i++) 
		{
			//m_SV.set(i, 0);
			m_SV.add(0);
		}

		m_DistLevels = new Vector<Double>();
		m_DistValues = new Vector<Double>();
		m_DistLevels.add(0.0);
		m_DistValues.add(0.0);
		for (int i=1; i<=m_numberOfItems; i++)
		{
			if ( m_CurrentDist.equals("Zipfian") )
				m_DistValues.add(100 * Math.pow(i, -(1-ZipfianMean))/Math.pow(m_numberOfItems, -(1-ZipfianMean)));
			else 
				m_DistValues.add(10.0);

			m_DistLevels.add(m_DistLevels.get(i-1) + m_DistValues.get(i));
		}
		
		
	}
	
	public DistOfAccess(int numOfItems, String distname, boolean bBinary)
	{
		if ( distname.equals("U")  || 
			distname.equals("u")  || 
			distname.equals("Uniform")  || 
			distname.equals("Unif")  || 
			distname.equals("uniform")  || 
			distname.equals("UNIFORM")  || 
			distname.equals("UNIF")  )
			m_CurrentDist="Uniform";
		else 
			m_CurrentDist = "Zipfian";
		m_bBinarySearch = bBinary;
		m_randNumGenerator = new Random();
		InitZipfian(numOfItems, 0.27);
	}

	public DistOfAccess(int numOfItems, String distname, boolean bBinary, double ZipfianMean)
	{
		if ( distname.equals("U")  || 
			distname.equals("u")  || 
			distname.equals("Uniform")  || 
			distname.equals("Unif")  || 
			distname.equals("uniform")  || 
			distname.equals("UNIFORM")  || 
			distname.equals("UNIF")  )
			m_CurrentDist="Uniform";
		else 
			m_CurrentDist = "Zipfian";
		m_bBinarySearch = bBinary;
		m_randNumGenerator = new Random();
		InitZipfian(numOfItems, ZipfianMean);

	}

	public DistOfAccess(int numOfItems, String distname, boolean bBinary, 
						double ZipfianMean, int randomSeed)
	{
		//cout<< "distname = " << distname << endl;
		//System.out.println("distname = " + distname);
		if ( distname.equals("U")  || 
			distname.equals("u")  || 
			distname.equals("Uniform")  || 
			distname.equals("Unif")  || 
			distname.equals("uniform")  || 
			distname.equals("UNIFORM")  || 
			distname.equals("UNIF")  )
			m_CurrentDist="Uniform";
		else 
			m_CurrentDist = "Zipfian";
		m_bBinarySearch = bBinary;

		//this.rClip = new Random(randomSeed);
		//srand( randomSeed );
		//m_randNumGenerator.setRandomSeed( randomSeed );
		m_randNumGenerator = new Random(randomSeed);
		//m_randNumGenerator.setSeed( randomSeed );

		InitZipfian(numOfItems, ZipfianMean);
	}

	int getRandomNum( int max )
	{
		//double r = ( (double)rand() / ((double)(RAND_MAX)+1) );
		//return int(r * max);

		//return m_randNumGenerator.uniform( (unsigned int)max );
		return m_randNumGenerator.nextInt(max);
	}

	int GenerateOneItem()
	{
		int randMovie = 0;
		m_DistLevels.get(m_numberOfItems);
		int max = (int)(double)m_DistLevels.get(m_numberOfItems);
		//int movieIndex = rClip.Next(max);
		int movieIndex = getRandomNum( max );
		Integer temp_val;

		if(!m_bBinarySearch)
			randMovie = LinearSearch(movieIndex);
		else
			randMovie = BinarySearch(movieIndex, 0 , m_numberOfItems);

		if (m_MakeRec)
		{
			// Console.WriteLine("item is "+randMovie);
			if (randMovie >= 0 && randMovie <= m_SV_Length)
			{
				temp_val = m_SV.get(randMovie);
				m_SV.set(randMovie, temp_val + 1);
			}
			else 
			{
				//cout<< "Error in DistOfAccess.cs, indexing item " << randMovie << " which is out of range.\n";
				System.out.println("Error in DistOfAccess.cs, indexing item " + randMovie + " which is out of range.");
			}
		}
		return randMovie;
	}

	//index starts from one
	double GetFrequency(int index)
	{
		if (index < 1 || index > m_numberOfItems) 
			return -1;
		return (double)m_DistValues.get(index) / (double)m_DistLevels.get(m_numberOfItems);
	}

	void PrintAccurracy ()
	{
		if (m_MakeRec)
		{
			//cout<< "Item \t Obs Freq \t Exp Freq \t Freq Err\n";
			System.out.println("Item \t Obs Freq \t Exp Freq \t Freq Err");
			int TotalSamples = 0;
			for (int i = 0; i < m_numberOfItems+1 ; i++)
				TotalSamples += m_SV.get(i);
			if (TotalSamples > 0)
			{
				double ObsFreq = 0.0;
				for (int i = 1; i < m_numberOfItems+1 ; i++)
				{
					ObsFreq = (double) m_SV.get(i) / TotalSamples;
					// Console.WriteLine("Elt "+ i + ", number of occ is "+SV[i]+", freq is "+ObsFreq);
					//cout<< "" << i << " \t " << ObsFreq << " \t " << GetFrequency(i) << " \t " << (double) 100 * (GetFrequency(i) - ObsFreq ) / GetFrequency(i) << endl;
					System.out.println( i + " \t " + ObsFreq + " \t " + GetFrequency(i) + " \t " + 
							((double) 100 * (GetFrequency(i) - ObsFreq ) / GetFrequency(i)));
					
				}
			} 
			else 
				//cout<< "Error, total samples is " << TotalSamples << endl;
				System.out.println("Error, total samples is " + TotalSamples);
		} 
		else 
		{
			
			//cout<< "Error, MakeRecording was not enabled.\n";
			//cout<< "Enable MakeRecording must be enabled to gather statistics.\n";
			//cout<< "Usage:  DistOfAccess.MakeRecording = true\n";
			System.out.println("Error, MakeRecording was not enabled.\n" +
				"Enable MakeRecording must be enabled to gather statistics.\n" +
				"Usage:  DistOfAccess.MakeRecording = true");
		}
	}
	
	public static void main( String[] args )
	{
		int num_items = 100;
		int num_runs = 10000;
		String distrib_name = "Z";		// "U" (uniform) or "Z" (zipfian)
		
		DistOfAccess dist = new DistOfAccess(num_items,distrib_name,true,0.27);
		int [] count_array = new int[num_items];
		
		// Keep track of the distribution of generated items
		for( int i = 0; i < num_runs; i++ )
		{
			// minus 1 because dist generates from 1 to num_items
			count_array[dist.GenerateOneItem() - 1]++;
		}
		
		// Display final distribution
		for( int i = 0; i < num_items; i++ )
		{
			System.out.println(i+1 + ": " + count_array[i] + " times");
		}
	}
}
