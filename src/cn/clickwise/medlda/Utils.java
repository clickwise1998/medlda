package cn.clickwise.medlda;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import cn.clickwise.sort.utils.SortStrArray;

public class Utils {
	
	private static Logger logger = Logger.getLogger(Utils.class);
	
	public static double log_sum(double log_a, double log_b)
	{
		double v;

		if (log_a < log_b) {
			v = log_b+Math.log(1 + Math.exp(log_a-log_b));
		} else {
			v = log_a+Math.log(1 + Math.exp(log_b-log_a));
		}
		return(v);
	}
	
	public static double trigamma(double x)
	{
		double p;
		int i;

		x = x+6;
		p = 1/(x*x);
		p = (((((0.075757575757576*p-0.033333333333333)*p+0.0238095238095238)
			*p-0.033333333333333)*p+0.166666666666667)*p+1)/x+0.5*p;
		for (i=0; i<6 ;i++) {
			x = x - 1;
			p = 1/(x*x)+p;
		}
		return(p);
	}
	
	public static double digamma(double x)
	{
		double p;
		x = x+6;
		p = 1/(x*x);
		p = (((0.004166666666667*p-0.003968253986254)*p+
			0.008333333333333)*p-0.083333333333333)*p;
		p = p + Math.log(x) - 0.5/x-1/(x-1)-1/(x-2)-1/(x-3)-1/(x-4)-1/(x-5)-1/(x-6);
		return p;
	}
	
	public static double log_gamma(double x)
	{
		double z=1/(x*x);

		x=x+6;
		z=(((-0.000595238095238*z+0.000793650793651)
			*z-0.002777777777778)*z+0.083333333333333)/x;
		z=(x-0.5)*Math.log(x)-x+0.918938533204673+z-Math.log(x-1)-
				Math.log(x-2)-Math.log(x-3)-Math.log(x-4)-Math.log(x-5)-Math.log(x-6);
		return z;
	}
	
	public static double lgamma(double x)
	{
		double x0,x2,xp,gl,gl0;
		int n=0,k;
		double a[] = {
			8.333333333333333e-02,
			-2.777777777777778e-03,
			7.936507936507937e-04,
			-5.952380952380952e-04,
			8.417508417508418e-04,
			-1.917526917526918e-03,
			6.410256410256410e-03,
			-2.955065359477124e-02,
			1.796443723688307e-01,
			-1.39243221690590
		};

		x0 = x;
		if (x <= 0.0) return 1e308;
		else if ((x == 1.0) || (x == 2.0)) return 0.0;
		else if (x <= 7.0) {
			n = (int)(7-x);
			x0 = x+n;
		}
		x2 = 1.0/(x0*x0);
		xp = 2.0*Math.PI;
		gl0 = a[9];
		for (k=8;k>=0;k--) {
			gl0 = gl0*x2 + a[k];
		}
		gl = gl0/x0+0.5*Math.log(xp)+(x0-0.5)*Math.log(x0)-x0;
		if (x <= 7.0) {
			for (k=1;k<=n;k++) {
				gl -= Math.log(x0-1.0);
				x0 -= 1.0;
			}
		}
		return gl;
	}
	
	public static int argmax(double[] x, int n)
	{
		int i;
		double max = x[0];
		int argmax = 0;
		for (i = 1; i < n; i++)
		{
			if (x[i] > max)
			{
				max = x[i];
				argmax = i;
			}
		}
		return(argmax);
	}
	
	public static double  dotprod(double[] a, double[] b, int n)
	{
		double res = 0;
		for ( int i=0; i<n; i++ ) {
			res += a[i] * b[i];
		}
		return res;
	}
	

	
	public static void printmatrix(double[][] A, double n)
	{
		for ( int i=0; i<n; i++ ) {
			for ( int j=0; j<n; j++ ) {
				System.err.printf("\t%f", A[i][j]);
			}
			System.err.println();
		}
	}
	
	public static void printinqmatrix(double[][] A,double m ,double n)
	{
		String line="";
		for ( int i=0; i<1; i++ ) {
			line="";
			for ( int j=0; j<n; j++ ) {
				line+=(j+":"+A[i][j]+" ");
				//System.err.printf("\t%f", A[i][j]);
			}
			logger.info("line:"+i+" "+line);
			//System.err.println();
		}
	}
	public static void printvector(double[] A, double n)
	{
		String line="";
		for ( int j=0; j<n; j++ ) {
			line+=(j+":"+A[j]+" ");
			//System.err.printf("\t%f", A[j]);
		}
		logger.info(line);
		
	}
	
	public static double[] potential2probs(double[] potential)
	{
		double[] probs=new double[2];
		double logsum=log_sum(potential[0],potential[1]);
		
		probs[0]=Math.exp(potential[0]-logsum);
		probs[1]=Math.exp(potential[1]-logsum);
		
		return probs;
	}
	
	public static double[] optWeight(double[] gradient,double[] oldpoint)
	{
		double steplength=0.01;
		double[] newpoint=new double[2];
		for(int l=0;l<5;l++)
		{
		 for(int i=0;i<newpoint.length;i++)
		 {
			newpoint[i]=oldpoint[i]+steplength*gradient[i];
			oldpoint[i]=newpoint[i];
		 }
		
		}
		return newpoint;
	}
	
	public static double optWeight(double gradient,double oldpoint)
	{
		double steplength=1;
		double newpoint=0;

		newpoint=oldpoint+steplength*gradient;
		return newpoint;
	}
	
	public static boolean[] selectState(double[][] probs)
	{
		boolean[] selstat=new boolean[probs.length];
		ArrayList<String> scores=new ArrayList<String>();
		for(int i=0;i<probs.length;i++)
		{
			scores.add(i+"\001"+Math.abs(probs[i][1]));
		}
		
		String[] sorted=SortStrArray.sort_List(scores, 1, "dou", 2, "\001");
		
		//for(int i=0;i<10;i++)
		//{
		//	System.out.println("i="+i+" "+sorted[i]);
		//}
		
		/*
		int lowthreshodIndex=(int)(sorted.length*(0.9));
		double lowthreshold=Double.parseDouble((sorted[lowthreshodIndex].split("\001"))[1]);
       	System.out.println("lowthreshold:"+lowthreshold);
		*/
		int upthreshodIndex=(int)(sorted.length*(0.3));		
		double upthreshold=Double.parseDouble((sorted[upthreshodIndex].split("\001"))[1]);
		System.out.println("upthreshold:"+upthreshold);
		
		
		for(int i=0;i<probs.length;i++)
		{
			if((Math.abs(probs[i][1])>upthreshold))
			{
				selstat[i]=true;
			}
			else
			{
				selstat[i]=false;
			}
		}
		
		/*
		for(int i=0;i<probs.length;i++)
		{
			double rand=Math.random();
			if(rand>0.5)
			{
				selstat[i]=true;
			}
			else
			{
				selstat[i]=false;
			}
		}
		*/
		return selstat;
	}
}
