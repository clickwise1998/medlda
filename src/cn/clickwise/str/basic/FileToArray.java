package cn.clickwise.str.basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	

}
