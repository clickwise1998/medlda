package cn.clickwise.classify.simplify;

public class LEARNPARM {
	 /**
	  * 选择是回归还是 分类
	  */
	 public short type;
	 
	 /**
	  * alphas的上界C
	  */
	 public double svm_c;
	 
	 /**
	  * 回归 epsilon (对于分类是1)
	  */
	 public double eps;
	 
	 /**
	  * C对于正样本的乘因子
	  */
	 public double svm_costratio;
	
	 
	 /**
	  * unlabeled example 的比例
	  */
	 public double transduction_posratio;
	 
	 
	 /**
	  * 如果非0，则使用超平面 w*x+b=0,否则为w*x=0
	  */
	 public short biased_hyperplane;
	 
	 
	 /**
	  * 如果非0，则使用shared slack variable,要求每个训练样本都设置slackid
	  */
	 public short sharedslack;
	 
	 /**
	  * working set 的大小
	  */
	 public int svm_maxqpsize;
	 
	 
	 /**
	  * 
	  */
	 public int svm_newvarsinqp;
	 
	 
	 public int kernel_cache_size;
	 
	 /**
	  * tolerable error for distances used  in stopping criterion
	  */
	 public double epsilon_crit;         
	 
	 /** how much a multiplier should be above zero for shrinking */
	 public double epsilon_shrink;       
    
	 /** iterations h after which an example can be removed by shrinking */
	 public long   svm_iter_to_shrink;   
    
	 /** number of iterations after which the optimizer terminates, if there was
	  no progress in maxdiff */
	 public long   maxiter;             
    
	 /** exclude examples with alpha at C and retrain */
	 public long   remove_inconsistent;  
    
	 /** do not check KT-Conditions at the end of optimization for examples removed by 
	  shrinking. WARNING: This might lead to  sub-optimal solutions! */
	 public  long   skip_final_opt_check; 
    
	 /** if nonzero, computes leave-one-out estimates */
	 public  long   compute_loo;          
    
	 /** parameter in xi/alpha-estimates and for pruning leave-one-out range [1..2] */
	 public double rho;                 
    
	 /** parameter in xi/alpha-estimates upper bounding the number of SV the current
	   alpha_t is distributed over */
	 public  int   xa_depth;             
    
	 /** file for predicitions on unlabeled examples in transduction */
	 public  String predfile;          
    
	 /** file to store optimal alphas in. use empty string if alphas should not be output */
	 public   String alphafile;        

    /** you probably do not want to touch the following tolerable error on eq-constraint */
	 public double epsilon_const;        

	 /** tolerable error on alphas at bounds */
	 public double epsilon_a;           
    
	 /** precision of solver, set to e.g. 1e-21 if you get convergence problems */
	 public  double opt_precision;        

     /** the following are only for internal use do so many steps for finding optimal C*/
	 public  long   svm_c_steps;         
   
	 /** increase C by this factor every step */
	 public  double svm_c_factor;         
   
	 public  double svm_costratio_unlab;
   
	 public  double svm_unlabbound;
   
	 /** individual upper bounds for each var , that the dual variable for each constraint in P1 */
	 public  double[] svm_cost;            
    
	 /** number of features */
	 public  int   totwords;
	 
}
