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
		
		/*******bat2********
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
		********/
		if(SSO.tioe(cate))
		{
			return "";
		}
		if(cate.equals("家居建材|家纺布艺|毛巾被/毛巾毯"))
		{
			mcate="床上用品/靠垫/毛巾/布艺|床褥/床垫/床单/毛毯";
		}
		else if(cate.equals("床上用品/靠垫/毛巾/布艺|被子/蚕丝被/纤维被"))
		{
			mcate="家居建材|家纺布艺|被子/蚕丝被/纤维被";
		}
		else if((cate.equals("家居建材|家纺布艺|布艺定制窗帘"))||(cate.equals("家居建材|家纺布艺|布艺成品窗帘")))
		{
			mcate="家居建材|家纺布艺|布艺窗帘";
		}
		else if((cate.equals("床上用品/靠垫/毛巾/布艺|毛巾"))||(cate.equals("床上用品/靠垫/毛巾/布艺|浴巾")))
		{
			mcate="家居建材|家纺布艺|毛巾/浴巾";
		}
		else if(cate.equals("家居建材|家居饰品|相片墙/画片"))
		{
			mcate="家居建材|家居饰品|相框/相架/画框";
		}
		else if(cate.equals("家居建材|家装主材|花洒单头/莲蓬头"))
		{
			mcate="家居建材|家装主材|淋浴花洒套装";
		}
		else if((cate.equals("家电办公|大家电|燃气热水器"))||(cate.equals("家电办公|大家电|电热水器")))
		{
			mcate="家居建材|家装主材|热水器";
		}
		else if((cate.equals("家电办公|影音电器|HIFI音箱"))||(cate.equals("家电办公|影音电器|扩音器")))
		{
			mcate="家电办公|影音电器|组合/迷你/插卡音响";
		}
		else if(cate.equals("家电办公|影音电器|移动/便携DVD"))
		{
			mcate="家电办公|影音电器|蓝光/DVD影碟机";
		}
		else if(cate.equals("家电办公|厨房电器|净水器"))
		{
			mcate="家电办公|生活电器|净水器";
		}
		else if((cate.equals("家电办公|生活电器|吊扇"))||(cate.equals("家电办公|生活电器|电风扇"))||(cate.equals("日用百货|居家日用|扇子/迷你风扇")))
		{
			mcate="家电办公|生活电器|风扇";
		}
		else if(cate.equals("数码|电脑周边|组装机"))
		{
			mcate="数码|电脑周边|一体机";
		}
		else if((cate.equals("数码|笔记本|Acer/宏碁"))||(cate.equals("数码|平板电脑|Acer/宏碁")))
		{
			mcate="数码|笔记本平板电脑|Acer/宏碁";
		}
		else if((cate.equals("数码|数码配件|笔记本电脑配件"))||(cate.equals("数码|数码配件|平板电脑配件")))
		{
			mcate="数码|数码配件|笔记本平板电脑配件";
		}
		else if(cate.equals("文化玩乐|书籍杂志|社会科学"))
		{
			mcate="文化玩乐|书籍杂志|考试/教材/论文";
		}
		else if((cate.equals("文化玩乐|爱好收藏|外国邮票"))||(cate.equals("文化玩乐|爱好收藏|外国邮票")))
		{
			mcate="文化玩乐|爱好收藏|中外邮票";
		}
		else if(cate.equals("文化玩乐|个性定制|照片冲印"))
		{
			mcate="文化玩乐|网络服务|照片冲印/图片处理";
		}
		else if(cate.equals("日用百货|成人用品|情趣套装/睡衣"))
		{
			mcate="日用百货|成人用品|情趣内衣";
		}
		else if((cate.equals("家居建材|住宅家具|搁板/置物架"))||(cate.equals("日用百货|收纳整理|整理架/置物架"))||(cate.equals("日用百货|收纳整理|浴室角架/置物架"))||(cate.equals("日用百货|餐饮用具|晾碗架/置物架")))
		{
			mcate="日用百货|收纳整理|整理架/置物架";
		}
		else if(cate.equals("文化玩乐|网络服务|礼品袋/塑料袋"))
		{
			mcate="日用百货|收纳整理|收纳袋";
		}
		else if((cate.equals("护肤彩妆|美发护发|洗发水"))||(cate.equals("护肤彩妆|美发护发|染发膏")))
		{
			mcate="日用百货|洗护清洁|洗发水";
		}
		else if(cate.equals("日用百货|居家日用|香薰器具"))
		{
			mcate="日用百货|洗护清洁|香薰用品";
		}
		else if(cate.equals("日用百货|餐饮用具|炒锅"))
		{
			mcate="日用百货|餐饮用具|烹饪锅具";
		}
		else if((cate.equals("本地生活|休闲娱乐|温泉/洗浴"))||(cate.equals("本地生活|休闲娱乐|游泳")))
		{
			mcate="本地生活|休闲娱乐|温泉/洗浴/游泳";
		}
		else if((cate.equals("美食特产|水果蔬菜|芒果"))||(cate.equals("美食特产|水果蔬菜|车厘子"))||(cate.equals("美食特产|水果蔬菜|桃"))||(cate.equals("美食特产|水果蔬菜|石榴")))
		{
			mcate="本地生活|生活超市|新鲜水果";
		}
		else if((cate.equals("美食特产|水果蔬菜|鸡蛋"))||(cate.equals("美食特产|水果蔬菜|鸭蛋")))
		{
			mcate="本地生活|生活超市|蛋类";
		}
		else if(cate.equals("美食特产|水果蔬菜|青蟹"))
		{
			mcate="本地生活|生活超市|水产鲜肉";
		}
		else if((cate.equals("本地生活|面包蛋糕|饮料/酒水提货券"))||(cate.equals("本地生活|餐饮美食|美食折扣券"))||(cate.equals("本地生活|面包蛋糕|商场购物卡"))||(cate.equals("本地生活|面包蛋糕|超市卡")))
		{
			mcate="本地生活|卡券消费|网上平台优惠劵";
		}
		else if((cate.equals("美食特产|水果蔬菜|火腿"))||(cate.equals("美食特产|水果蔬菜|腊/腌肉"))||(cate.equals("美食特产|粮油米面|香/腊肠")))
		{
			mcate="美食特产|粮油米面|即食肠类";
		}
		else if(cate.equals("美食特产|营养品|枸杞及其制品"))
		{
			mcate="美食特产|有机食品|有机滋补品";
		}
		else if(cate.equals("美食特产|营养品|山药"))
		{
			mcate="美食特产|水果蔬菜|山药";
		}
		else if((cate.equals("美食特产|营养品|植物精华/提取物"))||(cate.equals("美食特产|营养品|维生素/矿物质"))||(cate.equals("美食特产|营养品|蛋白质/氨基酸")))
		{
			mcate="美食特产|营养品|功能复合型膳食营养补充剂";
		}
		else if(cate.equals("美食特产|营养品|蜂蜜/蜂产品"))
		{
			mcate="美食特产|有机食品|有机酒水饮料";
		}
		else if(cate.equals("美食特产|酒水|洋酒"))
		{
			mcate="本地生活|生活超市|酒水饮料";
		}
		else if(cate.equals("运动户外|运动服|男装"))
		{
			if((title.indexOf("迷彩")>-1)||(title.indexOf("夹克")>-1))
			{
				return "";
			}
		}
		else if(cate.equals("运动户外|运动服|女装"))
		{
			if(title.indexOf("夹克")>-1)
			{
				return "";
			}
		}
		else if(cate.equals("运动户外|运动服|运动茄克"))
		{
			if((title.indexOf("夹克")==-1)&&(title.indexOf("皮衣")==-1))
			{
				return "";
			}
		}
		else if(cate.equals("运动户外|运动/瑜伽/健身/球迷用品|山地车"))
		{
			mcate="运动户外|运动/瑜伽/健身/球迷用品|自行车/单车";
		}
		else if((cate.equals("运动户外|运动/瑜伽/健身/球迷用品|踏步机/中小型健身器材"))||(cate.equals("运动户外|运动/瑜伽/健身/球迷用品|跑步机/大型健身器械")))
		{
			mcate="运动户外|运动/瑜伽/健身/球迷用品|健身器械";
		}
		else if((cate.equals("鞋类箱包|功能箱包|卡片包"))||(cate.equals("鞋类箱包|功能箱包|钥匙包"))||(cate.equals("鞋类箱包|功能箱包|腰包")))
		{
			mcate="运动户外|运动包|腰包/手包/配件包";
		}
		else if(cate.equals("运动户外|运动服|运动单/双肩背包"))
		{
			mcate="鞋类箱包|功能箱包|旅行包";
		}
		else if((cate.equals("鞋类箱包|春秋女鞋|帆布鞋"))||(cate.equals("鞋类箱包|春秋女鞋|松糕鞋"))||(cate.equals("鞋类箱包|春秋男鞋|板鞋")))
		{
			mcate="运动户外|运动鞋|板鞋/休闲鞋/滑板鞋";
		}
		else{
			mcate=cate;
		}
		mcate=mcate.trim();
		
		title=title.trim();
		dest=mcate+"\001"+title;
		return dest;
	}
	
	public static void main(String[] args)
	{
		String input="tb/tb_goods_short_exmple_all_1216.txt";
		String output="tb/tb_goods_short_mod_rearch1217.txt";
		
		TBArchitecture tbarch=new TBArchitecture();
		tbarch.reArch(input, output);
	}
	
}
