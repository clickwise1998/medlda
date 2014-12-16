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
		/******bat1****
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
		
		******/
		if((cate.equals("珠宝手表|其他配饰|偏光太阳镜"))||(cate.equals("珠宝手表|其他配饰|太阳眼镜")))
		{
			mcate="珠宝手表|其他配饰|功能眼镜";
		}
		else if(cate.equals("家电办公|生活电器|电熨斗"))
		{
			mcate="家电办公|生活电器|挂烫机";
		}
		else if(cate.equals("家居建材|住宅家具|简易衣柜"))
		{
			mcate="家居建材|住宅家具|衣柜";
		}
		else if((cate.equals("汽车摩托|汽车用品|汽车清洗用品"))||(cate.equals("汽车摩托|汽车用品|汽车美容用品")))
		{
			mcate="汽车摩托|汽车用品|保养用品";
		}
		else if((cate.equals("数码|办公设备|打印机"))||(cate.equals("数码|办公设备|复印机"))||(cate.equals("数码|办公设备|多功能一体机")))
		{
			mcate="数码|办公设备|打印复印机";
		}
		else if((cate.equals("数码|电脑周边|无线鼠标"))||(cate.equals("数码|电脑周边|有线鼠标")))
		{
			mcate="数码|电脑周边|鼠标";
		}
		else if((cate.equals("数码|相机/DV|胶卷相机"))||(cate.equals("数码|相机/DV|LOMO相机"))||(cate.equals("数码|相机/DV|单反相机"))||(cate.equals("数码|相机/DV|单电/微单"))||(cate.equals("数码|相机/DV|拍立得"))||(cate.equals("数码|相机/DV|数码相机")))
		{
			mcate="数码|相机/DV|相机";
		}
		else if(cate.equals("数码|网络存储|3G无线路由器"))
		{
			mcate="数码|网络存储|路由器";
		}
		else if(cate.equals("数码|电脑周边|电脑/网络工具"))
		{
			mcate="数码|网络存储|网络设备";
		}
		else if(cate.equals("数码|电脑周边|台式整机"))
		{
			mcate="数码|电脑周边|一体机";
		}
		else if((cate.equals("文化玩乐|书籍杂志|管理"))||(cate.equals("文化玩乐|书籍杂志|经济")))
		{
			mcate="文化玩乐|书籍杂志|经济管理";
		}
		else if((cate.equals("床上用品/靠垫/毛巾/布艺|床褥/床垫"))||(cate.equals("家居建材|家纺布艺|床单/床裙/床笠/床罩"))||(cate.equals("家居建材|家纺布艺|床品套件/四件套/多件套"))||(cate.equals("家居建材|住宅家具|床垫/席梦思"))||(cate.equals("床上用品/靠垫/毛巾/布艺|毛毯/绒毯")))
		{
			mcate="床上用品/靠垫/毛巾/布艺|床褥/床垫/床单/毛毯";
		}
		else if((cate.equals("床上用品/靠垫/毛巾/布艺|枕头/枕芯"))||(cate.equals("床上用品/靠垫/毛巾/布艺|枕套/枕巾")))
		{
			mcate="床上用品/靠垫/毛巾/布艺|枕套/枕巾/枕头/枕芯";
		}
		else if((cate.equals("日用百货|餐饮用具|杯子/水壶"))||(cate.equals("文化玩乐|个性定制|杯子定制"))||(cate.equals("日用百货|餐饮用具|玻璃杯"))||(cate.equals("运动户外|运动包|水壶")))
		{
			mcate="日用百货|餐饮用具|杯子/水壶";
		}
		else if(cate.equals("家居建材|五金电工|接线板"))
		{
			mcate="家居建材|五金电工|电源插座";
		}
		else if(cate.equals("家居建材|家居饰品|装饰架/装饰搁板"))
		{
			mcate="家居建材|住宅家具|搁板/置物架";
		}
		else if(cate.equals("虚拟|淘宝网厅|移动/联通/电信网上营业厅"))
		{
			mcate="虚拟|淘宝网厅|移动/联通/电信充值中心";
		}
		else if((cate.equals("家居建材|五金电工|成套监控系统"))||(cate.equals("家居建材|五金电工|监控摄像机"))||(cate.equals("数码|相机/DV|摄像机")))
		{
			mcate="家居建材|五金电工|监控摄像机";
		}
		else if((cate.equals("护肤彩妆|彩妆香水|眼影"))||(cate.equals("护肤彩妆|彩妆香水|眼线")))
		{
			mcate="护肤彩妆|彩妆香水|眼影眼线";
		}
		else if((cate.equals("护肤彩妆|彩妆香水|粉饼"))||(cate.equals("护肤彩妆|彩妆香水|蜜粉/散粉"))||(cate.equals("护肤彩妆|彩妆香水|腮红/胭脂")))
		{
			mcate="护肤彩妆|彩妆香水|粉饼";
		}
		else if(cate.equals("母婴用品|新生儿|新生儿内衣"))
		{
			mcate="母婴用品|童装|婴儿内衣";
		}
		else if(cate.equals("运动户外|户外运动用品|冲锋衣裤/套装"))
		{
			mcate="运动户外|户外运动用品|军迷用品";
		}
		else if(cate.equals("运动户外|户外运动用品|速干衣裤/套装"))
		{
			mcate="运动户外|户外运动用品|户外服装";
		}
		else if((cate.equals("运动户外|户外运动用品|垂钓/渔具包"))||(cate.equals("运动户外|户外运动用品|浮漂"))||(cate.equals("运动户外|户外运动用品|钓鱼用品"))||(cate.equals("运动户外|户外运动用品|鱼竿"))||(cate.equals("运动户外|户外运动用品|鱼饵")))
		{
			mcate="运动户外|户外运动用品|钓鱼用品";
		}
		else if((cate.equals("运动户外|运动服|运动单/双肩背包"))||(cate.equals("鞋类箱包|功能箱包|休闲型双肩包"))||(cate.equals("鞋类箱包|功能箱包|商务型双肩包"))||(cate.equals("鞋类箱包|功能箱包|甜美型双背包"))||(cate.equals("鞋类箱包|功能箱包|背包/双肩包"))||(cate.equals("鞋类箱包|功能箱包|书包")))
		{
			mcate="运动户外|运动服|运动单/双肩背包";
		}
		else
		{
			mcate=cate;
		}
		title=title.trim();
		dest=mcate+"\001"+title;
		return dest;
	}
	
	public static void main(String[] args)
	{
		String input="tb/tb_goods_short_exmple1000.txt";
		String output="tb/tb_goods_short_mod_rearch1216.txt";
		
		TBArchitecture tbarch=new TBArchitecture();
		tbarch.reArch(input, output);
	}
	
}
