package cn.clickwise.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import cn.clickwise.sort.utils.SortStrArray;

public class WordWeight {

        //this is a test line
	private HashMap<Integer,String> wordIndex;
	
	public WordWeight()
	{
		setWordIndex(new HashMap<Integer,String>());
	}
	
	public void wordWeight(String wei_file,String dict_file,String output_file)
	{
	   	BufferedReader br=null;
	   	PrintWriter pw=null;
	   	try{
	   		br=new BufferedReader(new FileReader(dict_file));
	   		String line="";
	   		String[] tokens=null;
	   		String word="";
	   		int index=0;
	   		while((line=br.readLine())!=null)
	   		{
	   			tokens=line.split("\001");
	   			if(tokens.length!=2)
	   			{
	   				continue;
	   			}
	   			word=tokens[0].trim();
	   			index=Integer.parseInt(tokens[1]);
	   			if(!(wordIndex.containsKey(index)))
	   			{
	   				wordIndex.put(index, word);
	   			}
	   		}
	   		br.close();
	   		
	   		br=new BufferedReader(new FileReader(wei_file));
	   		pw=new PrintWriter(new FileWriter(output_file));
	   		double weight=0;
	   		ArrayList<String> wlist=new ArrayList<String>();
	   		while((line=br.readLine())!=null)
	   		{
	   			tokens=line.split(":");
	   			if(tokens.length!=2)
	   			{
	   				continue;
	   			}
	   			
	   			index=Integer.parseInt(tokens[0]);
	   			weight=Double.parseDouble(tokens[1]);
	   			word=wordIndex.get(index);
	   			if(word==null)
	   			{
	   				continue;
	   			}
	   			wlist.add(word+"\001"+Math.abs(weight));
	   		}
	   		String[] sorted=SortStrArray.sort_List(wlist, 1, "dou", 2, "\001");
	   		for(int i=0;i<sorted.length;i++)
	   		{
	   			pw.println(sorted[i]);
	   		}
	   		br.close();
	   		pw.close();

	   	}
	   	catch(Exception e)
	   	{
	   		e.printStackTrace();
	   	}
	
	   	
	}

	public HashMap<Integer,String> getWordIndex() {
		return wordIndex;
	}

	public void setWordIndex(HashMap<Integer,String> wordIndex) {
		this.wordIndex = wordIndex;
	}
	
	public static void main(String[] args)
	{
		String wei_file="temp120_c16_f10/final.wwei";
		String dict_file="example/gendictansj12.txt";
		String output_file="example/wordweingram.txt";
		WordWeight ww=new WordWeight();
		ww.wordWeight(wei_file, dict_file, output_file);
	}
	
}
