package cn.clickwise.tb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import cn.clickwise.str.basic.SSO;

/**
 * 修改淘宝的分类体系
 * @author zkyz
 */
public class TBArchitecture {

	public void reArch(String input,String output)
	{
		BufferedReader br=null;
		PrintWriter pw=null;
		String src="";
		String dest="";
		
		try{
			br=new BufferedReader(new FileReader(input));
			pw=new PrintWriter(new FileWriter(output));
		    
			while((src=br.readLine())!=null)
			{
				if(SSO.tioe(src))
				{
					continue;
				}
				
				dest=check(src);
				
				if(SSO.tioe(dest))
				{
					continue;
				}
				pw.println(dest);
			}
			
			br.close();
			pw.close();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String check(String src)
	{
		String dest="";
		
		String[] fields=null;
		fields=src.split("\001");
		if(fields.length!=2)
		{
			return "";
		}
		
		String cate="";
		String title="";
		cate=fields[0];
		title=fields[1];
		
		String mcate="";
		if(cate.equals("护肤彩妆|彩妆香水|唇膏/口红"))
		{
			mcate="护肤彩妆|彩妆香水|唇彩/唇蜜";
		}
		else if(cate.equals("本地生活|卡券消费|淘系优惠券"))
		{
			mcate="本地生活|卡券消费|网上平台优惠劵";
		}
		else if(cate.equals("本地生活|面包蛋糕|蔬菜/年夜饭/熟食/半成品券")||cate.equals("本地生活|面包蛋糕|85度C"))
		{
			mcate="本地生活|餐饮美食|美食折扣券";
		}
		else if(cate.equals("内衣配饰|服装配饰|婚纱礼服配件"))
		{
			mcate="女装男装|其他女装|婚纱/礼服/旗袍";
		}
		else if(cate.equals("内衣配饰|内衣分类|文胸"))
		{
			mcate="内衣配饰|内衣分类|内衣名店街";
		}
		else if(cate.equals("女装男装|男式裤子|羽绒裤"))
		{
			mcate="女装男装|男式裤子|棉裤";
		}
		else if(cate.equals("家居建材|住宅家具|鞋架"))
		{
			mcate="家居建材|住宅家具|鞋柜";
		}
		else if(cate.equals("家居建材|家纺布艺|泡沫地垫"))
		{
			mcate="家居建材|家纺布艺|地毯";
		}
		else if(cate.equals("女装男装|女式上装|针织衫"))
		{
			mcate="女装男装|其他女装|毛衣";
		}
		else if(cate.equals("母婴用品|儿童玩具|电动/遥控玩具")||cate.equals("母婴用品|儿童玩具|拼装玩具")||cate.equals("母婴用品|儿童玩具|积木类玩具"))
		{
			mcate="母婴用品|儿童玩具|早教/智能玩具";
		}
		else if(cate.equals("家居建材|五金电工|电钻"))
		{
			mcate="家居建材|五金电工|电动工具";
		}
		else 
		{
			mcate=cate;
		}
		
		if((cate.indexOf("女"))>-1)
		{
			if((title.indexOf("男"))>-1)
			{
				return "";
			}
		}
		
		if((cate.indexOf("男"))>-1)
		{
			if((title.indexOf("女"))>-1)
			{
				return "";
			}
		}
		if((SSO.tioe(title))||(title.length()<10)||(title.indexOf(";&#")>-1))
		{
			return "";
		}
		title=title.trim();
		dest=mcate+"\001"+title;
		return dest;
	}
	
	public static void main(String[] args)
	{
		String input="tb/tb_goods_short_mod.txt";
		String output="tb/tb_goods_short_mod_rearch.txt";
		
		TBArchitecture tbarch=new TBArchitecture();
		tbarch.reArch(input, output);
	}
	
}
