package cn.clickwise.classify.reform;

public class CCACHEELEM {
	SVECTOR fydelta;
	double rhs;
	double viol;
	CCACHEELEM next;
	
	public CCACHEELEM copyCCACHEELEM()
	{
		CCACHEELEM nc=new CCACHEELEM();
		nc.fydelta=fydelta.copySVECTOR();
		nc.rhs=rhs;
		nc.viol=viol;
		if(next!=null)
		{
		 nc.next=next.copyCCACHEELEM();
		}
		else
		{
		 nc.next=null;
		}
		return nc;
	}
	

}
