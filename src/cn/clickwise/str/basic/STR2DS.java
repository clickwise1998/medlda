package cn.clickwise.str.basic;

public class STR2DS {

	public static double[] str2douarr(String str)
	{
		String[] seg_arr=str.split("\\s+");
		double[] arr=new double[seg_arr.length];
		for(int j=0;j<arr.length;j++)
		{
			arr[j]=Double.parseDouble(seg_arr[j]);
		}
		return arr;
	}
	
	
}
