package cn.clickwise.file.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.clickwise.str.basic.SSO;

public class FileWriterUtil {

	public static void writeHashMap(HashMap hm,String output_file) 
	{
		Iterator iter = hm.entrySet().iterator();
		
		FileWriter fw=null;
		PrintWriter pw=null;
		try{
			
		fw=new FileWriter(new File(output_file));
		pw=new PrintWriter(fw);
		
		while (iter.hasNext()) { 
		    Map.Entry entry = (Map.Entry) iter.next(); 
		    Object key = entry.getKey(); 
		    Object val = entry.getValue(); 
		    pw.println(key+" "+val);    		    
		} 

		fw.close();
		pw.close();
		}
		catch(Exception e){
			
		}

	}
	
	public static void writeHashMapUniq(HashMap hm,String output_file) 
	{
		Iterator iter = hm.entrySet().iterator();
		HashMap<String,String> chm=new HashMap<String,String>();
		FileWriter fw=null;
		PrintWriter pw=null;
		try{
			
		fw=new FileWriter(new File(output_file));
		pw=new PrintWriter(fw);
		String key_str="";
		while (iter.hasNext()) { 
		    Map.Entry entry = (Map.Entry) iter.next(); 
		    Object key = entry.getKey(); 
		    Object val = entry.getValue(); 
		    key_str=key+"";
		    key_str=key_str.trim();
		    if(!(SSO.tnoe(key_str)))
		    {
		    	continue;
		    }
		    if(!(chm.containsKey(key_str)))
		    {
		       pw.println(key+" "+val);
		       chm.put(key_str, "1");
		    }
		} 

		fw.close();
		pw.close();
		}
		catch(Exception e){
			
		}

	}
	
	public static void writeContent(String content,String output_file) 
	{
		
		FileWriter fw=null;
		PrintWriter pw=null;
		try{
			
		fw=new FileWriter(new File(output_file));
		pw=new PrintWriter(fw);
		
        pw.println(content);
		fw.close();
		pw.close();
		}
		catch(Exception e){
			
		}

	}
	
	public static void writeArrayList(ArrayList<String> arrlist,String output_file,boolean append) 
	{
		
		FileWriter fw=null;
		PrintWriter pw=null;
		try{
			
		fw=new FileWriter(new File(output_file),append);
		pw=new PrintWriter(fw,append);
		
		for(int i=0;i<arrlist.size();i++)
		{
			if((arrlist.get(i))!=null)
			{
			 pw.println(arrlist.get(i).trim());
			}
		}
       
		fw.close();
		pw.close();
		}
		catch(Exception e){
			
		}

	}
	
	public static PrintWriter getPW(String output_file)
	{
		FileWriter fw=null;
		PrintWriter pw=null;
		
		try{
			fw=new FileWriter(output_file);
			pw=new PrintWriter(fw);
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return pw;
	}
	
}
