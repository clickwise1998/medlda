package cn.clickwise.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.clickwise.sort.utils.SortStrArray;
import cn.clickwise.str.basic.SSO;

public class TBErrorAnalysis {

	public void errorAnalysis(String log,String genLabelDict,String report)
	{
		try{
		   BufferedReader genbr=new BufferedReader(new FileReader(genLabelDict));
		   String genLine="";
		   HashMap<String,String> labelDict=new HashMap<String,String>();
		   HashMap<String,Integer> errorCount=new HashMap<String,Integer>();
		   HashMap<String,Integer> goldenCount=new HashMap<String,Integer>();
		   
		   String[] fields=null;
		   String labelStr="";
		   String labelName="";
		   
		   while((genLine=genbr.readLine())!=null)
		   {
			   if(SSO.tioe(genLine))
			   {
				   continue;
			   }
			   genLine=genLine.trim();
			   fields=genLine.split("\001");
			   if(fields.length!=2)
			   {
				   continue;
			   }
			   
			   labelStr=fields[0].trim();
			   if(SSO.tioe(labelStr))
			   {
				   continue;
			   }
			   labelStr=labelStr.replaceAll("\\|", "_");
			   labelName=fields[1].trim();
			   if(!(labelDict.containsKey(labelStr)))
			   {
				   labelDict.put(labelStr, labelName);
			   }
		   }
		   genbr.close();
		   
		   
		   BufferedReader logbr=new BufferedReader(new FileReader(log));
		   String logLine="";
		   Pattern predictPat=Pattern.compile("y:(\\d+_\\d+_\\d+)");
		   Pattern goldenPat=Pattern.compile("testsample.examples\\[\\d+\\]\\.y:(\\d+_\\d+_\\d+)");
		   String predict="";
		   String golden="";
		   String predictName="";
		   String goldenName="";
		   Matcher m=null;
		   
		   PrintWriter pw=new PrintWriter(new FileWriter(report));
		   String estr="";
		   while((logLine=logbr.readLine())!=null)
		   {
			   
			   if(SSO.tioe(logLine))
			   {
				   continue;
			   }
			   if((logLine.indexOf("testsample.examples"))<0)
			   {
				   continue;
			   }
			  // System.out.println("logLine:"+logLine);
			   m=predictPat.matcher(logLine);
			   if(m.find())
			   {
				   predict=m.group(1).trim();  
			   }
			   m=goldenPat.matcher(logLine);
			   if(m.find())
			   {
				   golden=m.group(1).trim();  
			   }
			  // System.out.println("predict:"+predict+" golden:"+golden);
			   if(!(predict.equals(golden)))
			   {
				   predictName=labelDict.get(predict);
				   goldenName=labelDict.get(golden);
				  // System.out.println("predictName:"+predictName+" goldenName:"+goldenName);
				   pw.println("golden:"+goldenName+" pred:"+predictName);
				   estr="golden:"+goldenName+" pred:"+predictName;
				   if(!(errorCount.containsKey( estr)))
				   {
					   errorCount.put( estr, 1);
				   }
				   else
				   {
					   errorCount.put( estr, errorCount.get( estr)+1);
				   }
				   
				   if(!(goldenCount.containsKey(  goldenName)))
				   {
					   goldenCount.put( goldenName, 1);
				   }
				   else
				   {
					   goldenCount.put(  goldenName, goldenCount.get(  goldenName)+1);
				   }
			   }
		   }
		   
		   ArrayList<String> errorList=new ArrayList<String>();
		   for(Map.Entry<String, Integer> el:errorCount.entrySet())
		   {
			  // System.out.println(el.getKey()+":"+el.getValue());
			   errorList.add(el.getKey()+"\001"+el.getValue());
		   }
		   
		   String[] errorArr=SortStrArray.sort_List(errorList, 1, "dou", 2, "\001");
		   
		   
		   
		   for(int i=0;i<errorArr.length;i++)
		   {
			   pw.println(errorArr[i]);
		   }
		   
		   ArrayList<String> goldenList=new ArrayList<String>();
		   for(Map.Entry<String, Integer> el:goldenCount.entrySet())
		   {
			  // System.out.println(el.getKey()+":"+el.getValue());
			   goldenList.add(el.getKey()+"\001"+el.getValue());
		   }
		   
		   String[] goldenArr=SortStrArray.sort_List(goldenList, 1, "dou", 2, "\001");
		   
	   
		   for(int i=0;i<goldenArr.length;i++)
		   {
			   pw.println(goldenArr[i]);
		   }
		   
		   pw.close();

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args)
	{
		if(args.length!=3)
		{
			System.err.println("Usage:<log> <genLabelDict> <report>");
			System.exit(1);
		}
		
		String log="";
		String genLabelDict="";
		String report="";
		
		log=args[0];
		genLabelDict=args[1];
		report=args[2];
		
		TBErrorAnalysis tba=new TBErrorAnalysis();
		tba.errorAnalysis(log, genLabelDict, report);
		
	}
}
