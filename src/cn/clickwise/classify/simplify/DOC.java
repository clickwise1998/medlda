package cn.clickwise.classify.simplify;

public class DOC {
	
	public int docnum;
	public int orgDocNum;
	public int queryid;
	public double costfactor;
	public int slackid;
	public int kernelid;
	public SVECTOR fvec;
	public int[] lvec;

	
	
	public String lvecString()
	{
		String str="";
		if(lvec==null)
		{
			return "";
		}
		for(int i=0;i<100;i++)
		{
			str+=(i+":"+lvec[i]+" ");
		}
		
		return str;
	}

}
