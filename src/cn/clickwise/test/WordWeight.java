package cn.clickwise.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

public class WordWeight {

	private HashMap<Integer,String> wordIndex;
	
	public WordWeight()
	{
		setWordIndex(new HashMap<Integer,String>());
	}
	
	public void wordWeight(String wei_file,String dict_file)
	{
	   	BufferedReader br=null;
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
	
	
	
}
