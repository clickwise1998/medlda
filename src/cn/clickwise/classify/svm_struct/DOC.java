package cn.clickwise.classify.svm_struct;

public class DOC {
	public int docnum;
	public int orgDocNum;
	public int queryid;
	public double costfactor;
	public int slackid;
	public int kernelid;
	public SVECTOR fvec;
	public int[] lvec;

	public DOC copyDoc() {
		DOC ndoc = new DOC();
		ndoc.docnum = docnum;
		ndoc.queryid = queryid;
		ndoc.costfactor = costfactor;
		ndoc.slackid = slackid;
		ndoc.kernelid = kernelid;
		if (fvec == null) {
			ndoc.fvec = null;
		} else {
			ndoc.fvec = fvec.copySVECTOR();
		}
		if (lvec == null) {
			ndoc.lvec = null;
		} else {

			ndoc.lvec = new int[lvec.length];
			for (int i = 0; i < lvec.length; i++) {
				ndoc.lvec[i] = lvec[i];
			}
		}
		return ndoc;
	}
	
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
