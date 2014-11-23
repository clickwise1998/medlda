package cn.clickwise.math.random;

public class ArrayUtil {

	public static double[] initialRandom(int n)
	{
  
		double[] arr=new double[n];
		for(int i=0;i<arr.length;i++)
		{
			 arr[i] = (Math.random() - 0.5) / (double)n;
		}
        
        return arr;	
	}
	
	/**
	 * 加和两数组，普通方法
	 * @param w1
	 * @param w2
	 * @return
	 */
	public static double[] sum_weight(double[] w1,double[] w2)
	{
  
		double[] sumw=new double[w1.length];
		for(int i=0;i<w1.length;i++)
		{
			sumw[i]=w1[i]+w2[i];
		}
        
        return sumw;	
	}
	
	/**
	 * w1-w2
	 * @param w1
	 * @param w2
	 * @return
	 */
	public static double[] sub_weight(double[] w1,double[] w2)
	{
  
		double[] sumw=new double[w1.length];
		for(int i=0;i<w1.length;i++)
		{
			sumw[i]=w1[i]-w2[i];
		}
        
        return sumw;	
	}
	
	public static String arrayToSamStr(double[] arr)
	{
		String samstr="";
		
		for(int i=0;i<arr.length;i++)
		{
			samstr=samstr+(i+1)+":"+arr[i]+" ";
		}
		
		samstr=samstr.trim();
		return samstr;
	}
	
	public static double[] normalize(double[] arr)
	{
		double[] narr=new double[arr.length];
		double sum=0;
		for(int i=0;i<arr.length;i++)
		{
			sum+=Math.abs(arr[i]);
		}
		if(sum==0)
		{
			sum=1000000;
		}
		for(int i=0;i<arr.length;i++)
		{
			narr[i]=(arr[i]/sum);
		}
		
		return narr;
	}

        public static double[] normalize(double[] arr,double z)
        {
                double[] narr=new double[arr.length];
                double sum=0;
                for(int i=0;i<arr.length;i++)
                {
                        sum+=Math.abs(arr[i]);
                }
                if(sum==0)
                {
                        sum=1000000;
                }
                for(int i=0;i<arr.length;i++)
                {
                        narr[i]=(arr[i]/(sum*z));
                }

                return narr;
        }
	
	public static double dotProduct(double[] arr1,double[] arr2)
	{
	    if(arr1.length!=arr2.length)
	    {
	    	return 0;
	    }
	    
		double sum=0;
		for(int i=0;i<arr1.length;i++)
		{
			sum+=arr1[i]*arr2[i];
		}
		
		return sum;
	}
	
}
