package cn.clickwise.math.random;

import java.util.Random;

/**
 * 生成各种随机数
 * @author zkyz
 *
 */
public class RandomGen {

	
	public static String getRandomStr()
	{
		String[] rstr={"pconline","miercn","fzyuechi","weight","thread"," bbsdf","sohuea","39yss","zdofad","rafdof"};
		int rani = -1;
		double ran = Math.random();
		rani = (int) (ran * (rstr.length));		
		return rstr[rani];
	}
	
	public static int getRandom(int top)
	{
		double ran=Math.random();
		int rt=(int)(ran*top);
		
        return rt;		
	}
	
	public static double randPosNeg()
	{
		double rand=Math.random();
		double rpn=rand*2-1;
		return rpn;
	}
	
	/** 产生一个随机的字符串*/  
	public static String RandomString(int length) {  
	    String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";  
	    Random random = new Random();  
	    StringBuffer buf = new StringBuffer();  
	    for (int i = 0; i < length; i++) {  
	        int num = random.nextInt(62);  
	        buf.append(str.charAt(num));  
	    }  
	    return buf.toString();  
	} 
	
	
	public static void main(String[] args)
	{
		for(int i=0;i<10;i++)
		{
		//System.out.println(randPosNeg());
			System.out.println(Math.random());
		}
	}
}
