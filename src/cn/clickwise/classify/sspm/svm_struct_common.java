package cn.clickwise.classify.sspm;

public class svm_struct_common {

	public static final String INST_NAME      =  "Multi-Class SVM";
	public static final String INST_VERSION    =   "V2.20";
	public static final String INST_VERSION_DATE = "14.08.08";
	
	public static final String STRUCT_VERSION     = "V3.10";
	public static final String STRUCT_VERSION_DATE = "14.08.08";
	
	public static int struct_verbosity=1;
	public static final int   NSLACK_ALG     =    0;
    public static final int   NSLACK_SHRINK_ALG    =    1;
    public static final int   ONESLACK_PRIMAL_ALG   =   2;
    public static final int   ONESLACK_DUAL_ALG      =  3;
    public static final int   ONESLACK_DUAL_CACHE_ALG  =4;
    public static final int   USE_FYCACHE=0;
  //  public static final double COMPACT_ROUNDING_THRESH= 10E-15;
    public static final double COMPACT_ROUNDING_THRESH= 10E-15;
    public static final int   DEFAULT_ALG_TYPE  = 2;
    
    
    /* default precision for solving the optimization problem */
    public static final double DEFAULT_EPS  =   0.1 ;
    
    public static final int DEFAULT_LOSS_FCT  = 0;
    
    /* default loss rescaling method: 1=slack_rescaling, 2=margin_rescaling */
    public static final int DEFAULT_RESCALING = 2;
    
    /* decide whether to evaluate sum before storing vectors in constraint
    cache: 
    0 = NO, 
    1 = YES (best, if sparse vectors and long vector lists), 
    2 = YES (best, if short vector lists),
    3 = YES (best, if dense vectors and long vector lists) */
    public static final int COMPACT_CACHED_VECTORS=2;
    
    public static String printW(double[] w, int sizePhi, int n,double C)
    {
    	 String log_info="";
    	  int i;
    	  log_info="---- w ----\n";
    	  for(i=0;i<sizePhi;i++)
    	    {
    	      log_info=log_info+w[i]+" ";
    	    }
    	  log_info=log_info+"\n----- xi ----\n";
    	  for(;i<sizePhi+2*n;i++)
    	    {
    	      log_info=log_info+1/Math.sqrt(2*C)*w[i]+" ";
    	    }
    	  log_info=log_info+"\n";
    	  
    	  return log_info;
    }
    
}
