package cn.clickwise.classify.simplify;

/**
 * minimize g0 * x + 1/2 x' * G * x 
 * s.t.
 *  ce*x - ce0 = 0 
 *  l <= x <= u  
 *  note: restricted to one  equality constraint 
 * @author lq
 */
public class QP {
	
	 /**
	  * opt_n is the number of variables, that is the dimensions of vector x 
	  */
	 public int opt_n;
	 
     /**
      * the number of equality constraints
      * value can only be 0 or 1
      */
	 public int opt_m;
	 
	 /**
	  * store the vector ce
	  */
	 public double[] opt_ce;
	 
	 /**
	  * store ce0
	  */
	 public double opt_ce0;
	 
	 /**
	  * store matrix G, transverse conversion to vector
	  */
	 public double[] opt_g;
	 
	 /**
	  * store vector g0
	  */
	 public double[] opt_g0;
	 
	 /**
	  * store the initial values of vector x
	  */
	 public double[] opt_xinit;
	 
	 /**
	  * store lower bound of vector x
	  */
	 public double[] opt_low;
	 
	 /**
	  * store upper bound of vector x
	  */ 
	 public double[] opt_up;
}
