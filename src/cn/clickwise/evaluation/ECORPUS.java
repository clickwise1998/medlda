package cn.clickwise.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	
	public double getAvgPrecsion()
	{
		double sum=0;
		for(int i=0;i<precisions.length;i++)
		{
			sum+=precisions[i];
		}
		
	    return sum/(double)labelSize;
	    
	}
	
	public double getAvgRecall()
	{
		double sum=0;
		for(int i=0;i<recalls.length;i++)
		{
			sum+=recalls[i];
		}
		
	    return sum/(double)labelSize;
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
	   	
	   	for(int i=0;i<corpus.size();i++)
	   	{
	   		edoc=corpus.get(i);
	   	    label=edoc.getLabel();
	   	    predLabel=edoc.getPredLabel();
	   	    if(!(labelIndex.containsKey(label.getKey())))
	   	    {
	   	    	labelIndex.put(label.getKey(), index++);
	   	    }
	   	    
	   	    if(!(labelIndex.containsKey(predLabel.getKey())))
	   	    {
	   	    	labelIndex.put(predLabel.getKey(), index++);
	   	    } 
	   	    
	   	}
	
	   	labelSize=labelIndex.size();
	   	
	   	
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
		
		precisions=new double[labelSize];
		recalls=new double[labelSize];
		
		for(int i=0;i<labelSize;i++)
		{
			precisions[i]=((double)confus[i][i])/((double)rowsum[i]);
			recalls[i]=((double)confus[i][i])/((double)colsum[i]);
		}
		
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
    

		
	
}
