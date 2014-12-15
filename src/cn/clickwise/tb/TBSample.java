package cn.clickwise.tb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import cn.clickwise.str.basic.SSO;

public class TBSample {

	
	public void filter(String eff_cates,String tb_goods,String resFile)
	{
	  HashMap<String,Integer> eff_cate_hash=new HashMap<String,Integer>();
	  HashMap<String,Boolean> title_hash=new HashMap<String,Boolean>();
	  HashMap<String,Integer> eff_cate_count=new HashMap<String,Integer>();
	  try{
		  BufferedReader br=new BufferedReader(new FileReader(eff_cates));
		  String line="";
		  String[] fields=null;
		  String cate="";
		  int count=0;
		  while((line=br.readLine())!=null)
		  {
			  if(SSO.tioe(line))
			  {
				  continue;
			  }
			  line=line.trim();
			  fields=line.split("\\s+");
			  if(fields.length!=2)
			  {
				  continue;
			  }
			  cate=fields[0].trim();
			  count=Integer.parseInt(fields[1]);
			  if(SSO.tioe(cate))
			  {
				  continue;
			  }
			  if(!(eff_cate_hash.containsKey(cate)))
			  {
				  eff_cate_hash.put(cate, count);
			  }
		  }
		  
		  br.close();
		  
		  BufferedReader tbbr=new BufferedReader(new FileReader(tb_goods));
		  PrintWriter pw=new PrintWriter(new FileWriter(resFile));
		  String title="";
		  while((line=tbbr.readLine())!=null)
		  {
			  if(SSO.tioe(line))
			  {
				  continue;
			  }
			  line=line.trim();
			  fields=line.split("\t");
			  if(fields.length<3)
			  {
				  continue;
			  }
			  cate=fields[1];
			  title="";
			  for(int j=2;j<fields.length;j++)
			  {
				  title=title+fields[j]+" ";
			  }
			  title=title.trim();
			  title=title.replaceAll("-tmall.com天猫", "");
			  if((eff_cate_hash.containsKey(cate))&&(!(title_hash.containsKey(title))))
			  {
				  title_hash.put(title, true);
				  if((eff_cate_count.get(cate)!=null)&&(eff_cate_count.get(cate)>1000))
				  {
					  continue;
				  }
				  pw.println(cate+"\001"+title);
				  if(!(eff_cate_count.containsKey(cate)))
				  {
					  eff_cate_count.put(cate, 1);
				  }
				  else
				  {
					  eff_cate_count.put(cate, eff_cate_count.get(cate)+1);
				  }
			  }
			  
		  }
		  tbbr.close();
		  pw.close();
		  for(Map.Entry<String, Integer> cc:eff_cate_count.entrySet())
		  {
			  System.out.println(cc.getKey()+":"+cc.getValue());
		  }
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
	  }
	}
	
	public static void main(String[] args)
	{
		TBSample tbs=new TBSample();
		tbs.filter("D:/projects/dataanalysis_win_workplace/DataAnalysis/training_set/taobaocate/tb_effective_cates.txt", "D:/projects/dataanalysis_win_workplace/DataAnalysis/training_set/taobaocate/tb_goods_sort.txt", "D:/projects/dataanalysis_win_workplace/DataAnalysis/training_set/taobaocate/tb_goods_selu.txt");
		
	}
}
