package cn.clickwise.file.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.clickwise.str.basic.SSO;

/**
 * 文件数据转换成数组
 * @author zkyz
 *
 */
public class FileToArray {
	
	
	/**
	 * 文件转换成二维数组
	 * @return
	 */
	public static String[][] fileToDoubleDimArr(String input_file,String seprator) throws Exception
	{
		  FileReader fr=new FileReader(new File(input_file));
		  BufferedReader br=new BufferedReader(fr);
		  String line="";


		  String field_info="";
		  String field="";
		  
		  line=br.readLine();
		  if(!(SSO.tnoe(line)))
		  {
			  return null;
		  }
		  line=line.trim();
		  String[] seg_arr=null;
		  seg_arr=line.split(seprator);
		  
		  ArrayList<String[]> dataList=new ArrayList<String[]>();
          int field_num=seg_arr.length;
          dataList.add(seg_arr);
          
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			 // System.out.println("line:"+line);
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }
			  seg_arr=line.split(seprator);
			//  System.out.println("seg_arr.length:"+seg_arr.length);
			  if(seg_arr.length!=field_num)
			  {
				  continue;
			  }
			  dataList.add(seg_arr);			  			  
		  }
		  br.close();
		  fr.close();
		
		  String[][] ddda=new String[dataList.size()][field_num];
		  for(int i=0;i<ddda.length;i++)
		  {
			  ddda[i]=dataList.get(i);
		  }
		  
		  return ddda;
	}
	
	
	/**
	 * 文件转换成一维数组
	 * @return
	 */
	public static String[] fileToDimArr(String input_file) throws Exception
	{
		  FileReader fr=new FileReader(new File(input_file));
		  BufferedReader br=new BufferedReader(fr);
		  String line="";
	  	  
		  ArrayList<String> dataList=new ArrayList<String>();
     
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			 // System.out.println("line:"+line);
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }
			  
			  dataList.add(line);	  			  
		  }
		  
		  br.close();
		  fr.close();
		
		  String[] dda=new String[dataList.size()];
		  for(int i=0;i<dda.length;i++)
		  {
			  dda[i]=dataList.get(i);
		  }
		  
		  return dda;
	}
	
	
	/**
	 * 文件夹转换成一维数组
	 * @return
	 */
	public static String[] dirToDimArr(String input_dir) throws Exception
	{
		 File indir=new File(input_dir);	
		 File[] infiles=indir.listFiles();
		 ArrayList<String> dataList=new ArrayList<String>();
		 String[] sing_arr=null;
		 String file_name="";
		 for(int i=0;i<infiles.length;i++)
		 {
			
			if(!(FileStatus.isRegularFile(infiles[i])))
			{
				continue;
			}
			System.out.println(infiles[i].toString());
			sing_arr=fileToDimArr(infiles[i]);
			if(sing_arr==null)
			{
				continue;
			}
			System.out.println("sing_arr.length:"+sing_arr.length);
			for(int j=0;j<sing_arr.length;j++)
			{
				dataList.add(sing_arr[j]);
			}
		 }
		 
		  String[] dda=new String[dataList.size()];
		  for(int i=0;i<dda.length;i++)
		  {
			  dda[i]=dataList.get(i);
		  }
		  
		  return dda;
	}
	
	/**
	 * 文件转换成一维数组
	 * @return
	 */
	public static String[] fileToDimArr(File input_file) throws Exception
	{

		  FileReader fr=new FileReader(input_file);
		  BufferedReader br=new BufferedReader(fr);
		  String line="";
	  	  
		  ArrayList<String> dataList=new ArrayList<String>();
     
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			  
			 // System.out.println("line:"+line);
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }
			  line=line.replaceAll("\n", "");
			  dataList.add(line);	  			  
		  }
		  
		  br.close();
		  fr.close();
		
		  String[] dda=new String[dataList.size()];
		  for(int i=0;i<dda.length;i++)
		  {
			  dda[i]=dataList.get(i);
		  }
		  
		  return dda;
	}
	
	/**
	 * 文件转换成一维数组
	 * @return
	 */
	public static String[] fileToDimArrPrefix(String prefix,File input_file) throws Exception
	{
		  FileReader fr=new FileReader(input_file);
		  BufferedReader br=new BufferedReader(fr);
		  String line="";
	  	  
		  ArrayList<String> dataList=new ArrayList<String>();
     
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			 // System.out.println("line:"+line);
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }
			  
			  dataList.add(prefix+line);	  			  
		  }
		  
		  br.close();
		  fr.close();
		
		  String[] dda=new String[dataList.size()];
		  for(int i=0;i<dda.length;i++)
		  {
			  dda[i]=dataList.get(i);
		  }
		  
		  return dda;
	}
	
	
	/**
	 * 文件各行存入arraylist中
	 * @param input_file
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> fileToArrayList(String input_file) throws Exception
	{
		  FileReader fr=new FileReader(new File(input_file));
		  BufferedReader br=new BufferedReader(fr);
		  String line="";
	  	  
		  ArrayList<String> dataList=new ArrayList<String>();
     
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			 // System.out.println("line:"+line);
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }
			  
			  dataList.add(line);	  			  
		  }
		  
		  br.close();
		  fr.close();
		
		  return dataList;
	}
	
}
