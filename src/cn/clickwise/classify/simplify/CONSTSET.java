package cn.clickwise.classify.simplify;

/**
 * a set of linear inequality constrains of
			     for lhs[i]*w >= rhs[i]
 * @author lq
 *
 */
public class CONSTSET {
	 /* m is the total number of constrains */
	public int m;
	public DOC[] lhs;
	public double[] rhs;
	
	public void free()
	{
		if(lhs!=null)
		{
			for(int i=0;i<lhs.length;i++)
			{
				if(lhs[i]!=null)
				{
					lhs[i].free();
					lhs[i]=null;
				}
			}
			
		}
		lhs=null;
		
		rhs=null;
	}
	
}
