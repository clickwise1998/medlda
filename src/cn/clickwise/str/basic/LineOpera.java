package cn.clickwise.str.basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 扫描文件每一行，提取出匹配的字段，写入输出文件
 * @author zkyz
 *
 */
public class LineOpera {

	/**
	 * regex : 匹配的 规则
	 * N: 提取字段数
	 * @param input_file
	 * @param output_file
	 * @param regex
	 * @param N
	 * @throws Exception
	 */
	public static void fieldFind(String input_file,String output_file,String regex,int N) throws Exception 
	{
		 FileReader fr=new FileReader(new File(input_file));
		  BufferedReader br=new BufferedReader(fr);
		  String line="";
		  FileWriter fw=new FileWriter(new File(output_file));
		  PrintWriter pw=new PrintWriter(fw);

		  Pattern pat=Pattern.compile(regex);
		  Matcher mat=null;
		  String field_info="";
		  String field="";
		  
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }
			  
			  mat=pat.matcher(line);
			  while(mat.find())
			  {
				  field_info="";
				  for(int j=1;j<=N;j++)
				  {
			        field=mat.group(j);
			        if(!(SSO.tnoe(field)))
			        {
			        	field="";
			        }
			        field=field.trim();
			        field_info=field_info+field+"\001";
				  }
				  field_info=field_info.trim();
				  if(SSO.tnoe(field_info))
				  {
				    pw.println(field_info);
				  }
			  }			  
		  }
		  fw.close();
		  pw.close();
		  br.close();
		  fr.close();
		
	}
	
	/**
	 * regex : 匹配的 规则
	 * N: 提取字段数
	 * @param input_file
	 * @param output_file
	 * @param regex
	 * @param N
	 * @throws Exception
	 */
	public static void fieldFindUniq(String input_file,String output_file,String regex,int N) throws Exception 
	{
		 FileReader fr=new FileReader(new File(input_file));
		  BufferedReader br=new BufferedReader(fr);
		  String line="";
		  FileWriter fw=new FileWriter(new File(output_file));
		  PrintWriter pw=new PrintWriter(fw);

		  Pattern pat=Pattern.compile(regex);
		  Matcher mat=null;
		  String field_info="";
		  String field="";
		  HashMap<String,String> f_hash=new HashMap<String,String>();
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }
			  
			  mat=pat.matcher(line);
			  while(mat.find())
			  {
				  field_info="";
				  for(int j=1;j<=N;j++)
				  {
			        field=mat.group(j);
			        if(!(SSO.tnoe(field)))
			        {
			        	field="";
			        }
			        field=field.trim();
			        field_info=field_info+field+"\001";
				  }
				  field_info=field_info.trim();
				  if(SSO.tnoe(field_info))
				  {
					  if(!(f_hash.containsKey(field)))
					  {
				         pw.println(field_info);
				         f_hash.put(field, "1");
					  }
				  }
			  }			  
		  }
		  fw.close();
		  pw.close();
		  br.close();
		  fr.close();
		
	}
	
	/**
	 * 
	 * @param input_file
	 * @param output_file
	 * @param regex
	 * @param N
	 * @throws Exception
	 */
	public static void fieldAddTail(String input_file,String output_file,String[] add_info) throws Exception 
	{
		 FileReader fr=new FileReader(new File(input_file));
		  BufferedReader br=new BufferedReader(fr);
		  String line="";
		  FileWriter fw=new FileWriter(new File(output_file));
		  PrintWriter pw=new PrintWriter(fw);

		  String[] seg_arr=null;
		  String nLine="";
		  while((line=br.readLine())!=null)
		  {
			  line=line.trim();
			  if(!(SSO.tnoe(line)))
			  {
				  continue;
			  }		  
			  seg_arr=line.split("\001");
			  
			  nLine="";
			  nLine=nLine+(seg_arr[0].trim()).toLowerCase()+"\001";
			  for(int i=1;i<seg_arr.length;i++)
			  {
				  nLine=nLine+seg_arr[i].trim()+"\001";
			  }
			  for(int i=0;i<add_info.length;i++)
			  {
				  nLine=nLine+add_info[i].trim()+"\001";
			  }
			  nLine=nLine.trim();
			  pw.println(nLine);
		  }
		  fw.close();
		  pw.close();
		  br.close();
		  fr.close();
		
	}
	
	
}
