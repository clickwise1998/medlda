package cn.clickwise.classify.simplify;



public class MODEL {
	
	 /**
	  * number of support vectors
	  */
	  public int sv_num;
	  public int at_upper_bound;
	  public double b;
	  public DOC[] supvec;
	  public double[] alpha;
	  
	  /**
	   * index from docnum(index) to position(value) in model
	   */
	  public int[] index;
	  
	  /**
	   * number of features
	   */
	  public int totwords;
	  
	  /**
	   * number of training documents
	   */
	  public int totdoc;
	  
	  /**
	   * kernel
	   */
	  public KERNELPARM kernel_parm;
	  
	  /**
	   * leave-one-out estimates
	   */
	  public double loo_error,loo_recall,loo_precision;
	  
	  /**
	   * xi/alpha estimates
	   */
	  public double xa_error,xa_recall,xa_precision;
	  
	  /**
	   * weights for linear case using folding
	   */
	  public double[] lin_weights;
	  
	  /**
	   * precision ,up to which this model is accurate
	   */
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
