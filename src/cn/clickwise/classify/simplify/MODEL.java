package cn.clickwise.classify.simplify;



public class MODEL {
	
	  public int sv_num;
	  public int at_upper_bound;
	  public double b;
	  public DOC[] supvec;
	  public double[] alpha;
	  public int[] index;
	  public int totwords;
	  public int totdoc;
	  public KERNELPARM kernel_parm;
	  
	  public double loo_error,loo_recall,loo_precision;
	  public double xa_error,xa_recall,xa_precision;
	  public double[] lin_weights;
	  public double maxdiff;
	  
	  
	  
	  public String topWeights()
	  {
		  String wi="";
		  if(lin_weights==null)
		  {
			  return "";
		  }
		  for(int i=0;(i<lin_weights.length&&i<100);i++)
		  {
			  wi+=(i+":"+lin_weights[i]);
		  }
		  return wi;
	  }
	  
}
