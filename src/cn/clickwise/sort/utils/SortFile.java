package cn.clickwise.sort.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import cn.clickwise.str.basic.SSO;

public class SortFile {
	
    public static void sortFile(String input,String output,int fieldNum,int keyIndex,String keyType,String separator)
    {
    	ArrayList<String> list=new ArrayList<String>();
    	try{
    		BufferedReader br=new BufferedReader(new FileReader(input));
    		String line="";
    		String[] tokens=null;
    		String index="";
    		double weight=0;
    		while((line=br.readLine())!=null)
    		{
    			if(SSO.tioe(line))
    			{
    				continue;
    			}
    			tokens=line.split(":");
    			if(tokens.length!=2)
    			{
    				continue;
    			}
    			index=tokens[0];
    			weight=Double.parseDouble(tokens[1]);
    			list.add(index+":"+Math.abs(weight));
    		}
    		br.close();
    		
    		String[] sorted=SortStrArray.sort_List(list, keyIndex, keyType, fieldNum, separator);
    		PrintWriter pw=new PrintWriter(new FileWriter(output));
    		for(int i=0;i<sorted.length;i++)
    		{
    			pw.println(sorted[i]);
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
		if(args.length!=6)
		{
			System.err.println("Usage:<input> <output> <fieldNum> <keyIndex> <keyType> <separator>\n" +
					                  "input: input file\n" +
					                  "output: output file\n" +
					                  "fieldNum: 字段个数\n" +
					                  "keyIndex: 排序的字段编号，从0开始\n" +
					                  "keyType:  排序的字段类型，dou 浮点数,int 整数， str 字符串\n" +
					                  "separator:分隔符 001 表示 \001 tab表示 \t blank 表示\\s+ \n");
			System.exit(1);
		}
		
		String input="";
		String output="";
		int fieldNum=0;
		int keyIndex=0;
		String keyType="";
		String inseparator="";
		String separator="";
		input=args[0];
		output=args[1];
		fieldNum=Integer.parseInt(args[2]);
		keyIndex=Integer.parseInt(args[3]);
		keyType=args[4];
		inseparator=args[5];
		inseparator=inseparator.trim();
		
		if(inseparator.equals("001"))
		{
			separator="\001";
		}
		else if(inseparator.equals("tab"))
		{
			separator="\t";
		}
		else if(inseparator.equals("blank"))
		{
			separator="\\s+";
		}
		else
		{
			separator=inseparator;
		}
		System.out.println("input:"+input);
		System.out.println("output:"+output);
		System.out.println("fieldNum:"+fieldNum);
		System.out.println("keyIndex:"+keyIndex);
		System.out.println("keyType:"+keyType);
		System.out.println("separator:"+separator);
		SortFile.sortFile(input, output, fieldNum, keyIndex, keyType,separator);
		
	}
}
