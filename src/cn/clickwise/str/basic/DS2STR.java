package cn.clickwise.str.basic;


import java.util.ArrayList;

/**
 * 各种数据结构转换成str
 * @author zkyz
 *
 */
public class DS2STR {

	public static String arraylist2str(ArrayList list)
	{
		String str="";
		for(int i=0;i<list.size();i++)
		{
			str=str+list.get(i)+" ";
		}
		str=str.trim();
		return str;
	}
	
	public static String floatarr2str(float[] arr)
	{
		String str="";
		for(int i=0;i<arr.length;i++)
		{
			str=str+arr[i]+" ";
		}
		str=str.trim();
		return str;
	}
	
	public static String doublearr2str(double[] arr)
	{
		String str="";
		for(int i=0;i<arr.length;i++)
		{
			str=str+arr[i]+" ";
		}
		str=str.trim();
		return str;
	}
	
	public static String trimfield(String[] arr,String seprator,int field_num,int index)
	{
		String str="";
		String item="";
		String[] seg_arr=null;
		for(int i=0;i<arr.length;i++)
		{
			item=arr[i];
			if(SSO.tioe(item))
			{
				continue;
			}
			item=item.trim();
			seg_arr=item.split(seprator);
			if(seg_arr.length!=field_num)
			{
				continue;
			}
			str=str+seg_arr[index]+" ";
		}
		str=str.trim();
		return str;
	}
}
