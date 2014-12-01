package cn.clickwise.math.random;

/**
 * 简单的数学函数
 * @author lq
 *
 */
public class SimFunc {



	public static long maxl(long  a, long  b)
	{
	  if(a>b)
	    return(a);
	  else
	    return(b);
	}
	
	public static int maxi(int  a, int  b)
	{
	  if(a>b)
	    return(a);
	  else
	    return(b);
	}
	
	public static double entropy(double x)
	{
		double y=0;
		y=-x*Math.log(x)-(1-x)*Math.log(1-x);
		return y;
	}
	
	
	
}
