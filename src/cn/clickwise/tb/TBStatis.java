package cn.clickwise.tb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cn.clickwise.sort.utils.SortStrArray;
import cn.clickwise.str.basic.SSO;

public class TBStatis {

	public void statis(String samples,String countFile)
	{
		try{
			PrintWriter pw=new PrintWriter(new FileWriter(countFile));
			BufferedReader br=new BufferedReader(new FileReader(samples));
			String line="";
			String[] fields=null;
			
			String cate="";
			String title="";
	        HashMap<String,Integer> cateCount=new HashMap<String,Integer>();
			
			while((line=br.readLine())!=null)
			{
				fields=line.split("\001");
				if(fields.length!=2)
				{
					continue;
				}
				cate=fields[0];
				title=fields[1];
				if(SSO.tioe(cate))
				{
					continue;
				}
				cate=cate.trim();
				if(!(cateCount.containsKey(cate)))
				{
					cateCount.put(cate, 1);
				}
				else
				{
					cateCount.put(cate, cateCount.get(cate)+1);
				}	
			}
			
			ArrayList<String> list=new ArrayList<String>();
			for(Map.Entry<String, Integer> m:cateCount.entrySet())
			{
				list.add(m.getKey()+"\001"+m.getValue());
			}
			
			String[] sort=SortStrArray.sort_List(list, 1, "dou", 2, "\001");
			
			for(int i=0;i<sort.length;i++)
			{
				System.out.println(sort[i]);
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		TBStatis tbs=new TBStatis();
		tbs.statis("tb/tb_goods_short_mod_rearch1224.txt", "tb/tb_goods_short_mod_rearch1217_2_count.txt");
	}
}
