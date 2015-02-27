package cn.clickwise.classify.reform;



public class MODEL {
	  public int sv_num;
	  public int at_upper_bound;
	  public double b;
	  public DOC[] supvec;
	  public double[] alpha;
	  public int[] index;
	  public int totwords;
	  public int totdoc;
	  public KERNEL_PARM kernel_parm;
	  
	  public double loo_error,loo_recall,loo_precision;
	  public double xa_error,xa_recall,xa_precision;
	  public double[] lin_weights;
	  public double maxdiff;
	  
	  public MODEL copyMODEL()
	  {
		MODEL model=new MODEL();
		model.sv_num=sv_num;
		model.at_upper_bound=at_upper_bound;
		model.b=b;
		if(supvec!=null)
		{
		  model.supvec=new DOC[supvec.length];
		  for(int i=0;i<supvec.length;i++)
		  {
			  if(supvec[i]!=null)
			  {
			    model.supvec[i]=supvec[i].copyDoc();
			  }
			  else
			  {
				  model.supvec[i]=null;
			  }
		  }
		}
		else
		{
			model.supvec=null;
		}
		model.alpha=new double[alpha.length];
		for(int i=0;i<alpha.length;i++)
		{
			model.alpha[i]=alpha[i];
		}
		if(index!=null)
		{
		  model.index=new int[index.length];
		  for(int i=0;i<index.length;i++)
		  {
			model.index[i]=index[i];
		  }
		}
		else
		{
			model.index=null;
		}
		
		model.totwords=totwords;
		model.totdoc=totdoc;
		model.kernel_parm=kernel_parm.copyKERNEL_PARM();
		model.loo_error=loo_error;
		model.loo_recall=loo_recall;
		model.loo_precision=loo_precision;
		model.xa_error=xa_error;
		model.xa_recall=xa_recall;
		model.xa_precision=xa_precision;
		if(lin_weights!=null)
		{
		model.lin_weights=new double[lin_weights.length];
		for(int i=0;i<lin_weights.length;i++)
		{
			model.lin_weights[i]=lin_weights[i];
		}
		}
		else
		{
			model.lin_weights=null;
		}
		model.maxdiff=maxdiff;
		
		return model;
	  }
	  
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
