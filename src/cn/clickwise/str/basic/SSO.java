package cn.clickwise.str.basic;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple Str Operation
 * 简单的字符串处理类
 * @author lq
 *
 */
public class SSO {

	
	/**
	 * 测试字符串是否为null或空,,若不为null且不为空返回true
	 * @param str
	 * @return istnoe
	 */
	public static boolean tnoe(String str)
	{
		boolean istnoe=false;
		if((str!=null)&&(!(str.trim().equals(""))))
	    {
			istnoe=true;
		}
		
		return istnoe;
	}
	
	/**
	 * 测试字符串是否为null或空,,若为null或为空返回true
	 * @param str
	 * @return istnoe
	 */
	public static boolean tioe(String str)
	{
		boolean istioe=false;
		if((str==null)||(str.trim().equals("")))
	    {
			istioe=true;
		}
		
		return istioe;
	}
	
	public static String implode(String[] seg_arr,String separator)
	{
		String ns="";
		if(seg_arr.length<1)
		{
			return "";
		}
		for(int i=0;i<(seg_arr.length-1);i++)
		{
			ns=ns+seg_arr[i]+separator;
		}
		ns=ns+seg_arr[seg_arr.length-1];
		
		return ns;
	}
	
	public static String implode(ArrayList arr_list,String separator)
	{
		String ns="";
		if((arr_list.size())<1)
		{
			return "";
		}
		String lit="";
		for(int i=0;i<((arr_list.size())-1);i++)
		{
			lit=arr_list.get(i)+"";
			if(!(SSO.tnoe(lit)))
			{
				continue;
			}
			
			ns=ns+lit+separator;
		}
		ns=ns+arr_list.get(arr_list.size()-1);
		
		return ns;
	}
	
	/**
	 * 取str之前的字符串,不包括str
	 * @param str
	 * @return
	 */
	public static String beforeStr(String source,String str)
	{
		String ns="";
		if(!(SSO.tnoe(source)))
		{
			return "";
		}
		if(source.indexOf(str)<0)
		{
			return source;
		}
		ns=source.substring(0, source.indexOf(str));
		ns=ns.trim();
		return ns;
	}
	
	/**
	 * 取str之后的字符串,不包括str
	 * @param str
	 * @return
	 */
	public static String afterStr(String source,String str)
	{
		String ns="";
		if(!(SSO.tnoe(source)))
		{
			return "";
		}
		if(source.indexOf(str)<0)
		{
			return source;
		}
		ns=source.substring(source.indexOf(str)+str.length(),source.length());
		ns=ns.trim();
		return ns;
	}
	
	/**
	 * 截断str之后的字符串，包括str
	 * @param source
	 * @param str
	 * @return
	 */
	public static String truncAfterStr(String source,String str)
	{
		String ts="";
		if(source.indexOf(str)<0)
		{
			return source;
		}
		ts=source.substring(0,source.lastIndexOf(str));
		ts=ts.trim();
		return ts;
	}
	
	/**
	 * 截断str之前的字符串，包括str
	 * @param source
	 * @param str
	 * @return
	 */
	public static String truncBeforeStr(String source,String str)
	{
		String ts="";
		if(source.indexOf(str)<0)
		{
			return source;
		}
		ts=source.substring(source.indexOf(str)+str.length(),source.length());
		ts=ts.trim();
		return ts;
	}
	
	/**
	 * 返回str1,str2中间的字符串，不包括str1,str2
	 * @param source
	 * @param str1
	 * @param str2
	 * @return
	 */
	public  static String midstrs(String source,String str1,String str2)
	{
		String ms="";
		if(!(SSO.tnoe(source)))
		{
			return "";
		}
		int firstIndex=0;
		int lastIndex=0;
		
		if((source.indexOf(str1))==(source.lastIndexOf(str2)))
		{
			return source;
		}
		if(source.indexOf(str1)<0)
		{
			firstIndex=0;
		}
		else
		{
			firstIndex=source.indexOf(str1)+str1.length();
		}
		
		if(source.lastIndexOf(str2)<0)
		{
			lastIndex=source.length();
		}
		else
		{
			lastIndex=source.lastIndexOf(str2);
		}
		
		System.out.println("source:"+source+" fi:"+firstIndex+" li:"+lastIndex);
		ms=source.substring(firstIndex,lastIndex);
		ms=ms.trim();
		return ms;
	}
	
	/**
	 * 将source最后出现的find 替换为replace
	 * @param source
	 * @param find
	 * @param replace
	 * @return
	 */
	public static String replaceLast(String source,String find,String replace)
	{
		find=find.trim();
		if(!(SSO.tnoe(find)))
		{
			return source;
		}
		String prefix=source.substring(0,source.lastIndexOf(find));
		String suffix=source.substring(source.lastIndexOf(find)+find.length(),source.length());
		String ns=prefix+replace+suffix;
		return ns;
	}
	
	public static String[] getResponseType(String response_value)
	{
		String[] responseArr=new String[2];
		responseArr[0]="";
		responseArr[1]="";
		String responseType="";
		String responseSplitVal="";
		
		String rt_regex="((?:(?:A)|(?:MX)|(?:AAAA)|(?:TXT)|(?:CNAME)|(?:SOA)|(?:NS))\\s*[a-zA-Z0-9_\\-\\.\\s\\,:]*?)\\s*(?:(?:A)|(?:MX)|(?:AAAA)|(?:TXT)|(?:CNAME)|(?:SOA)|(?:NS))";
		ArrayList rt_list=new ArrayList();
		Pattern rt_pat=Pattern.compile(rt_regex);
		Matcher rt_mat=rt_pat.matcher(response_value);
		String sin_type="";
		String left_value="";
		boolean isFind=false;
		while(rt_mat.find())
		{
			isFind=true;
			sin_type=rt_mat.group(1);
			rt_list.add(sin_type);
			response_value=response_value.replaceFirst(sin_type,"");
			//System.out.println("response_value:"+response_value);
			left_value=response_value;
			rt_mat=rt_pat.matcher(response_value);
		}
        if(isFind==false)
        {
        	left_value=response_value;
        }
	    left_value=left_value.replaceAll("\\([\\d]+\\)", "");
	    left_value=left_value.trim();
	    //System.out.println("left_value: "+left_value.trim());
	    rt_list.add(left_value);
	    
	    String[] seg_arr=null;
	    
	    for(int i=0;i<(rt_list.size());i++)
	    {
	    	sin_type=rt_list.get(i)+"";
	    	responseSplitVal=responseSplitVal+sin_type+"|";
	    	sin_type=sin_type.trim();
	    	if(!(SSO.tnoe(sin_type)))
	    	{
	    		continue;
	    	}
	    	seg_arr=sin_type.split("\\s+");
	    	if((seg_arr==null)||(seg_arr.length<1))
	    	{
	    		continue;
	    	}
	    	
	    	//System.out.println(i+" "+seg_arr[0]);
	    	responseType=responseType+seg_arr[0]+"|";   	
	    }
	    if(SSO.tnoe(responseType))
	    {
	       responseType=responseType.substring(0,responseType.lastIndexOf("|"));
	    }
	    if(SSO.tnoe(responseSplitVal))
	    {
	    	responseSplitVal=responseSplitVal.substring(0,responseSplitVal.lastIndexOf("|"));
	    }
	    //System.out.println("responseType: "+responseType);
	    //responseType=responseType+"|"+rt_list.get((rt_list.size()-1)); 
	    responseArr[0]=responseType;
	    responseArr[1]=responseSplitVal;	
		return responseArr;
	}
	
	public static void main(String[] args)
	{
		/*
		String date_str="11:30:43";
		date_str=date_str.substring(0,date_str.lastIndexOf(":"));
		date_str=date_str.replaceAll(":", "");
		System.out.println("date_str:"+date_str);	
		String s="";
		
		System.out.println("s:"+s.trim());
		s=null;
		System.out.println("s2:"+s);
		
		String s2="CNAME us.sina.com.cn., us.sina.com.cn. CNAME news.sina.com.cn., news.sina.com.cn. CNAME jupiter.sina.com.cn., jupiter.sina.com.cn. CNAME taurus.sina.com.cn., taurus.sina.com.cn. A 61.172.201.20, taurus.sina.com.cn. A 61.172.201.21, taurus.sina.com.cn. A 61.172.201.24, taurus.sina.com.cn. A 61.172.201.25, taurus.sina.com.cn. A 61.172.201.36, taurus.sina.com.cn. A 61.172.201.9, taurus.sina.com.cn. A 61.172.201.10, taurus.sina.com.cn. A 61.172.201.11, taurus.sina.com.cn. A 61.172.201.12, taurus.sina.com.cn. A 61.172.201.13, taurus.sina.com.cn. A 61.172.201.14, taurus.sina.com.cn. A 61.172.201.15, taurus.sina.com.cn. A 61.172.201.16, taurus.sina.com.cn. A 61.172.201.17, taurus.sina.com.cn. A 61.172.201.18, taurus.sina.com.cn. A 61.172.201.19 ns: sina.com.cn. NS ns2.sina.com.cn., sina.com.cn. NS ns3.sina.com.cn., sina.com.cn. NS ns1.sina.com.cn., sina.com.cn. NS ns4.sina.com.cn. ar: ns1.sina.com.cn. A 202.106.184.166, ns2.sina.com.cn. A 61.172.201.254, ns3.sina.com.cn. A 123.125.29.99, ns4.sina.com.cn. A 121.14.1.22 (512)";
		String[] resarr=SSO.getResponseType(s2);
		System.out.println("0:"+resarr[0]);
		System.out.println("1:"+resarr[1]);
				
		
		String s3="2014-03-02 07:00:00 : {\"locid\":31,\"guestid\":\"e37d710cbf13b1081c849cc0cb56a9b7\",\"user_agent\":\"Mozilla\\\\/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident\\\\/4.0; .NET CLR 2.0.50727)\",\"host\":\"www.189so.cn\",\"refer\":\"\",\"title\":\"\u5bfc\u822a\",\"ip\":\"115.203.69.65\",\"cm\":1,\"num\":1,\"ids\":[\"730096d3a1f29d549c90efca81129950\"]}";
		String time=SSO.beforeStr(s3, "{");
		System.out.println("time:"+time);
		String time2=SSO.truncAfterStr(time, ":");
		System.out.println("time2:"+time2);
	
		String info=SSO.midstrs(s3, "{", "}");
		System.out.println("info:"+info);
		
		String[] seg_arr=info.split(",");
		
		String key="";
		String val="";
		
		for(int i=0;i<seg_arr.length;i++)
		{
		  key=SSO.beforeStr(seg_arr[i], ":");	
		  key=SSO.midstrs(key, "\"", "\"");
		  val=SSO.afterStr(seg_arr[i], ":");
		  val=SSO.midstrs(val, "\"", "\"");
		  
		  System.out.println(i+" "+seg_arr[i]+"   key="+key+"  val="+val);	
		}
		*/
		/*
		String source="乡村爱情圆舞曲48";
		String find="48";
		String replace="";
		System.out.println(replaceLast(source,find,replace));
		*/
		String source="铜仁市";
		System.out.println(source.charAt(source.length()-1));
		
		
		
		
	}
	
}
