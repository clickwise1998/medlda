package cn.clickwise.tb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;

public class TBExample {
	
	public void filter(String tb_goods,String resFile){
		
		  HashMap<String,Integer> eff_cate_count=new HashMap<String,Integer>();
		  try{
			  BufferedReader br=new BufferedReader(new FileReader(tb_goods));
			  PrintWriter pw=new PrintWriter(new FileWriter(resFile));
			  String line="";
			  String[] fields=null;
			  String cate="";
			  String title="";
			  
			  while((line=br.readLine())!=null)
			  {
				  fields=line.split("\001");
				  if(fields.length!=2)
				  {
					  continue;
				  }
				  cate=fields[0];
				  title=fields[1];
				 // if((eff_cate_count.get(cate)!=null)&&(eff_cate_count.get(cate)>0))
				 // {
				//	  continue;
				 // }
				  if(!(eff_cate_count.containsKey(cate)))
				  {
					  eff_cate_count.put(cate, 1);
				  }
				  else 
				  {
					  eff_cate_count.put(cate, eff_cate_count.get(cate)+1);
				  }
				  
				  pw.println(line);
				  
			  }
			  
			  pw.close();
			  br.close();
			  
		  }
		  catch(Exception e)
		  {
			  e.printStackTrace();
		  }
		  
	}
	
	public static void main(String[] args)
	{
		TBExample tbe=new TBExample();
		tbe.filter("D:/projects/medlda_win_workplace/medlda/tb/tb_goods_short_mod_rearch1216.txt", "D:/projects/medlda_win_workplace/medlda/tb/tb_goods_short_exmple_all_1216.txt");
	}
}
