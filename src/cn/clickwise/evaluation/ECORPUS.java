package cn.clickwise.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.clickwise.file.utils.FileToArray;
import cn.clickwise.str.basic.SSO;

/**
 * 待评估的文档集
 * @author lq
 */
public class ECORPUS {

	private ArrayList<EDOC> corpus=new ArrayList<EDOC>();
	
	private int labelSize;
	
	//每个标记对应混淆矩阵行或列的索引
    private HashMap<String,Integer> labelIndex=new HashMap<String,Integer>();	
    
    //混淆矩阵，行是参考标记，列是预测标记
    private int[][] confus;
    
    private double[] precisions=null;	
    
    private double[] recalls=null;	
    
    private double precision;
    
    private double recall;
	
	public void add(EDOC doc)
	{
		corpus.add(doc);
	}
	
	public void analysis()
	{
		getCorpusInfo();
		getConfus();
		labelsPreRecFromConfus();
	}
	
	public double getAvgPrecision()
	{
		double sum=0;
		for(int i=0;i<precisions.length;i++)
		{
			sum+=precisions[i];
		}
		precision=sum/(double)labelSize;
		
	    return precision;
	    
	}
	
	public double getAvgRecall()
	{
		double sum=0;
		for(int i=0;i<recalls.length;i++)
		{
			sum+=recalls[i];
		}
		
		recall=sum/(double)labelSize;
	    return recall;
	}
	
	public double getF()
	{
		return 2*precision*recall/(precision+recall);
	}
	
	public double[] getPrecisions()
	{
		return precisions;
	}
	
	public double[] getRecalls()
	{
		return recalls;
	}
	
	public String labelOfIndex(int index)
	{
		String label="";
		int t;
		for(Map.Entry<String, Integer> m:labelIndex.entrySet())
		{
			t=m.getValue();
			if(t==index)
			{
				label=m.getKey();
			}
		}
		
		return label;
	}
	
	//获得文档集的labelSize,labelIndex等
	public void getCorpusInfo()
	{
	   	int index=0;
	   	EDOC edoc=null;
	   	ELABEL label=null;
	   	ELABEL predLabel=null;
	   	
	   	int maxlabel=-1;
	   	for(int i=0;i<corpus.size();i++)
	   	{
	   		edoc=corpus.get(i);
	   	    label=edoc.getLabel();
	   	    predLabel=edoc.getPredLabel();
	   	    /*
	   	    if(!(labelIndex.containsKey(label.getKey())))
	   	    {
	   	    	labelIndex.put(label.getKey(), index++);
	   	    }
	   	    
	   	    if(!(labelIndex.containsKey(predLabel.getKey())))
	   	    {
	   	    	labelIndex.put(predLabel.getKey(), index++);
	   	    } 
	   	    */
	   	    if(label.getLabel()>maxlabel)
	   	    {
	   	    	maxlabel=label.getLabel();
	   	    }
	   	   
		    if(predLabel.getLabel()>maxlabel)
	   	    {
	   	    	maxlabel=predLabel.getLabel();
	   	    }
	   	        
	   	}
	
	   	for(int i=1;i<=maxlabel;i++)
	   	{
	   		labelIndex.put(i+"", i-1);
	   	}
	   	
	   	labelSize=labelIndex.size();
	   	
	   	System.out.println("labelSize:"+labelSize);
	}
	
	//计算混淆矩阵
	public void getConfus()
	{
		confus=new int[labelSize][labelSize];
		for(int i=0;i<labelSize;i++)
		{
			for(int j=0;j<labelSize;j++)
			{
				confus[i][j]=0;
			}
		}
		
	 	EDOC edoc=null;
	   	ELABEL label=null;
	   	ELABEL predLabel=null;
	   	
	   	for(int i=0;i<corpus.size();i++)
	   	{
	 		edoc=corpus.get(i);
	   	    label=edoc.getLabel();
	   	    predLabel=edoc.getPredLabel();
	   	    confus[labelIndex.get(label.getKey())][labelIndex.get(predLabel.getKey())]++;
	   	}
	   	
	   	/*
	   	for(int i=0;i<labelSize;i++)
	   	{
	   		System.out.print("i="+i+" ");
	   		for(int j=0;j<labelSize;j++)
	   		{
	   			System.out.print(confus[i][j]+" ");
	   		}
	   		System.out.println();
	   	}
	   	*/
	   	
	}
	
	public void labelsPreRecFromConfus()
	{
		int[] rowsum=new int[labelSize];
		int[] colsum=new int[labelSize];
		
		for(int i=0;i<labelSize;i++)
		{
			rowsum[i]=0;
			colsum[i]=0;
		}
		
		for(int i=0;i<labelSize;i++)
		{
			for(int j=0;j<labelSize;j++)
			{
				rowsum[i]+=confus[i][j];
				colsum[j]+=confus[i][j];
			}
		}
		
		
		/*
		for(int i=0;i<labelSize;i++)
		{
			System.out.println("i="+i+": rowsum "+rowsum[i]+" colsum "+colsum[i]);
		}
		*/
		
		
		precisions=new double[labelSize];
		recalls=new double[labelSize];
		
		for(int i=0;i<labelSize;i++)
		{
		
			precisions[i]=((double)confus[i][i])/((double)rowsum[i]);
			recalls[i]=((double)confus[i][i])/((double)colsum[i]);
			if(rowsum[i]==0)
			{
			    	if(confus[i][i]==0)
			    	{
			    		precisions[i]=1;
			    	}
			}
			
			if(colsum[i]==0)
			{
			    	if(confus[i][i]==0)
			    	{
			    		recalls[i]=1;
			    	}
			}
			
		}
		
		/*
		for(int i=0;i<labelSize;i++)
		{
			System.out.println("i="+i+": pre "+precisions[i]+" rec "+recalls[i]);
		}
		*/
		getAvgPrecision();
		getAvgRecall();
		
	}
	
	
	
	public int size()
	{
		return corpus.size();
	}

	public int getLabelSize() {
		return labelSize;
	}

	public void setLabelSize(int labelSize) {
		this.labelSize = labelSize;
	}


	public HashMap<String,Integer> getLabelIndex() {
		return labelIndex;
	}

	public void setLabelIndex(HashMap<String,Integer> labelIndex) {
		this.labelIndex = labelIndex;
	}
    
	public static void main(String[] args)
	{
		ECORPUS ecorpus=new ECORPUS();
		/*
		//row 1
		for(int i=0;i<69;i++)
		{
			ecorpus.add(new EDOC(1,1));
		}
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(1,2));
		}
		for(int i=0;i<4;i++)
		{
			ecorpus.add(new EDOC(1,3));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(1,4));
		}
		for(int i=0;i<1;i++)
		{
			ecorpus.add(new EDOC(1,5));
		}
		for(int i=0;i<14;i++)
		{
			ecorpus.add(new EDOC(1,6));
		}
		for(int i=0;i<4;i++)
		{
			ecorpus.add(new EDOC(1,7));
		}
		
		
		//row 2
		for(int i=0;i<3;i++)
		{
			ecorpus.add(new EDOC(2,1));
		}
		for(int i=0;i<70;i++)
		{
			ecorpus.add(new EDOC(2,2));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(2,3));
		}
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(2,4));
		}
		for(int i=0;i<7;i++)
		{
			ecorpus.add(new EDOC(2,5));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(2,6));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(2,7));
		}
		
		
		//row 3
		for(int i=0;i<4;i++)
		{
			ecorpus.add(new EDOC(3,1));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(3,2));
		}
		for(int i=0;i<66;i++)
		{
			ecorpus.add(new EDOC(3,3));
		}
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(3,4));
		}
		for(int i=0;i<5;i++)
		{
			ecorpus.add(new EDOC(3,5));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(3,6));
		}
		for(int i=0;i<11;i++)
		{
			ecorpus.add(new EDOC(3,7));
		}
		
		
		//row 4
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(4,1));
		}
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(4,2));
		}
		for(int i=0;i<18;i++)
		{
			ecorpus.add(new EDOC(4,3));
		}
		for(int i=0;i<51;i++)
		{
			ecorpus.add(new EDOC(4,4));
		}
		for(int i=0;i<9;i++)
		{
			ecorpus.add(new EDOC(4,5));
		}
		for(int i=0;i<12;i++)
		{
			ecorpus.add(new EDOC(4,6));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(4,7));
		}
		
		
		//row 5
		for(int i=0;i<4;i++)
		{
			ecorpus.add(new EDOC(5,1));
		}
		for(int i=0;i<8;i++)
		{
			ecorpus.add(new EDOC(5,2));
		}
		for(int i=0;i<1;i++)
		{
			ecorpus.add(new EDOC(5,3));
		}
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(5,4));
		}
		for(int i=0;i<80;i++)
		{
			ecorpus.add(new EDOC(5,5));
		}
		for(int i=0;i<3;i++)
		{
			ecorpus.add(new EDOC(5,6));
		}
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(5,7));
		}
		
		
		//row 6
		for(int i=0;i<7;i++)
		{
			ecorpus.add(new EDOC(6,1));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(6,2));
		}
		for(int i=0;i<3;i++)
		{
			ecorpus.add(new EDOC(6,3));
		}
		for(int i=0;i<7;i++)
		{
			ecorpus.add(new EDOC(6,4));
		}
		for(int i=0;i<2;i++)
		{
			ecorpus.add(new EDOC(6,5));
		}
		for(int i=0;i<67;i++)
		{
			ecorpus.add(new EDOC(6,6));
		}
		for(int i=0;i<8;i++)
		{
			ecorpus.add(new EDOC(6,7));
		}
		
		
		//row 7
		for(int i=0;i<11;i++)
		{
			ecorpus.add(new EDOC(7,1));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(7,2));
		}
		for(int i=0;i<7;i++)
		{
			ecorpus.add(new EDOC(7,3));
		}
		for(int i=0;i<5;i++)
		{
			ecorpus.add(new EDOC(7,4));
		}
		for(int i=0;i<7;i++)
		{
			ecorpus.add(new EDOC(7,5));
		}
		for(int i=0;i<6;i++)
		{
			ecorpus.add(new EDOC(7,6));
		}
		for(int i=0;i<58;i++)
		{
			ecorpus.add(new EDOC(7,7));
		}
		*/
		try{
		  String[] lines=FileToArray.fileToDimArr("temp/info.log");
		  String line="";
		  String[] tokens;
		  for(int i=0;i<lines.length;i++)
		  {
			  line=lines[i];
			  if(SSO.tioe(line))
			  {
				  continue;
			  }
			  line=line.trim();
			  tokens=line.split("\\s+");
			  if(tokens.length!=6)
			  {
				  continue;
			  }
			  ecorpus.add(new EDOC(tokens[4],tokens[5]));
		  }
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
		ecorpus.analysis();
		 
        double[] precisions=ecorpus.getPrecisions();
        double[] recalls=ecorpus.getRecalls();
       
        /*
        for(int i=0;i<precisions.length;i++)
        {
        	System.out.println("i="+i+" pre:"+precisions[i]+" rec:"+recalls[i]);
        }
		*/
        System.out.println("avgPre:"+ecorpus.getAvgPrecision());
        System.out.println("avgRec:"+ecorpus.getAvgRecall());
        System.out.println("f:"+ecorpus.getF());
	}
		
	
}
