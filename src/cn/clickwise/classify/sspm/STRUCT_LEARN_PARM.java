package cn.clickwise.classify.sspm;

public class STRUCT_LEARN_PARM {

	/**
	 * precision for which to solve quadratic program 
	 */
	public double epsilon;
	
	/**
	 * number of new constraints to accumulate before recomputing the QP solution
	 */
	public double newconstretrain;
	
	/**
	 * maximum number of constraints to cache for each example (used in w=4 algorithm)
	 */
	public int ccache_size;
	
	/**
	 * size of the mini batches in percent of training set size (used in w=4 algorithm)
	 */
	public double batch_size;
	
	/**
	 * trade-off between margin and loss
	 */
	public double C;
	
	
	String[] custom_argv;
	
	public  int  custom_argc; 
	
	public int   slack_norm;;
	
	public int loss_type;  ;
	
	public int   loss_function; 
	
	public int num_classes;
	
	public int num_features;
	
	/**for medlda***/
	public double delta_ell;
	
	/***for tb********/
	public int first_size;
	
	public int second_size;
	
	public int third_size;
	
	/****************/
	
}
