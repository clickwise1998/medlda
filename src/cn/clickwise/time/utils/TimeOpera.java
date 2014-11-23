package cn.clickwise.time.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.clickwise.str.basic.SSO;


/**
 * 时间处理的工具类
 * @author zkyz
 *
 */
public class TimeOpera {

	public static long str2long(String str)
	{
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dt =null;	
		try
		{
			dt = sdf.parse(str);	
		}
		catch(Exception e)
		{
			
		}
		if(dt==null)
		{
			return -1;
		}
				
		return dt.getTime();
	}
	
	public static String long2str(long tl)
	{
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dt = new Date(tl);	
		return sdf.format(dt);
	}
	
	public static String getDateFromStr(String str)
	{
		String dstr="";
		
		String date_regex="([\\d]+\\-[\\d]+\\-[\\d]+)";
		Pattern date_pat=Pattern.compile(date_regex);
		Matcher date_mat=date_pat.matcher(str);
		if(date_mat.find())
		{
		  dstr=date_mat.group(1);	
		}
	    if(!(SSO.tnoe(dstr)))
	    {
	    	dstr="";
	    }
	    dstr=dstr.replaceAll("\\-", "");
	    dstr=dstr.trim();
	    
		return dstr;
	}
	
	public static String getTimeFromStr(String str)
	{
		String tstr="";
		
		String time_regex="([\\d]+:[\\d]+:[\\d]+)";
		Pattern time_pat=Pattern.compile(time_regex);
		Matcher time_mat=time_pat.matcher(str);
		if(time_mat.find())
		{
		  tstr=time_mat.group(1);	
		}
	    if(!(SSO.tnoe(tstr)))
	    {
	    	tstr="";
	    }
	    tstr=tstr.trim();
	    
		return tstr;
	}
	
	public static String getCurrentTime()
	{
		long ctime=System.currentTimeMillis();
		String cstr=long2str(ctime);
		//System.out.println("cstr:"+cstr);
		return cstr;
	}
	
	public static long getCurrentTimeLong()
	{
		long ctime=System.currentTimeMillis();
		return ctime;
	}
	
	public static void main(String[] args)
	{
		String[] dstr={"2014-02-08 15:29:05","2014-02-08 19:29:20","2014-02-08 02:22:36","2014-02-08 15:04:25","2014-02-08 14:22:40","2014-02-08 16:17:22","2014-02-08 09:28:47"};
		for(int i=0;i<dstr.length;i++)
		{
			System.out.println(i+"   "+getTimeFromStr(dstr[i])+"    "+getDateFromStr(dstr[i]));
			
			System.out.println(i+":"+str2long(dstr[i])+"    "+long2str(str2long(dstr[i])-999));
		}
		
		getCurrentTime();
		
		System.out.println("test:"+long2str(1397401685));
		
	}
	
}
