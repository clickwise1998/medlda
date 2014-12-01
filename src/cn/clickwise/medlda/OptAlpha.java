package cn.clickwise.medlda;

public class OptAlpha {

	private static final double NEWTON_THRESH=1e-5;
	private static final int MAX_ALPHA_ITER= 1000;
	
	//for estimating alpha that is shared by all topics
	public static double alhood(double a, double ss, int D, int K)
	{
		return(D * (Utils.lgamma(K * a) - K * Utils.lgamma(a)) + (a - 1) * ss);
	}
	
	public static double d_alhood(double a, double ss, int D, int K)
	{
		return(D * (K * Utils.digamma(K * a) - K * Utils.digamma(a)) + ss);
	}
	
	public static double d2_alhood(double a, int D, int K)
	{	
		return(D * (K * K * Utils.trigamma(K * a) - K * Utils.trigamma(a)));
	}
	
	public static double opt_alpha(double ss, int D, int K)
	{
		  double a, log_a, init_a = 100;
		    double f, df, d2f;
		    int iter = 0;

		    log_a = Math.log(init_a);
		    do
		    {
		        iter++;
		        a = Math.exp(log_a);
		        if (/*isnan(a)*/a>1e300 || a<1e-300)
		        {
		            init_a = init_a * 10;
		            System.err.printf("warning : alpha is nan; new init = %5.5f\n", init_a);
		            a = init_a;
		            log_a = Math.log(a);
		        }
		        f = alhood(a, ss, D, K);
		        df = d_alhood(a, ss, D, K);
		        d2f = d2_alhood(a, D, K);
		        log_a = log_a - df/(d2f * a + df);
		        System.err.printf("alpha maximization : %5.5f   %5.5f\n", f, df);
		    }
		    while ((Math.abs(df) > NEWTON_THRESH) && (iter < MAX_ALPHA_ITER));
		    return(Math.exp(log_a));
	}
	
	
	//for estimating alphas that are different for different topics
	public static double alhood(double a, double alpha_sum, double ss, int D, int K)
	{
		return(D * (Utils.lgamma(alpha_sum) - Utils.lgamma(a)) + (a - 1) * ss);
	}
	
	
	public static double d_alhood(double a, double alpha_sum, double ss, int D, int K)
	{
		return(D * (Utils.digamma(alpha_sum) - Utils.digamma(a)) + ss);
	}
	
	public static double d2_alhood(double a, double alpha_sum, int D, int K)
	{
		
		return(D * (Utils.trigamma(alpha_sum) - Utils.trigamma(a))); 
	}
	
	public static double opt_alpha(double ss, double alpha_sum, int D, int K)
	{
		 double a, log_a, init_a = 100, old_a = 0;
		    double f, df, d2f;
		    int iter = 0;

		    log_a = Math.log(init_a);
		    do
		    {
		        iter++;
		        a = Math.exp(log_a);
				alpha_sum += a - old_a;
				old_a = a;
		        if (/*isnan(a)*/a>1e300 || a<1e-300)
		        {
		            init_a = init_a * 10;
		            System.err.printf("warning : alpha is nan; new init = %5.5f\n", init_a);
		            a = init_a;
		            log_a = Math.log(a);
		        }
		        f = alhood(a, alpha_sum, ss, D, K);
		        df = d_alhood(a, alpha_sum, ss, D, K);
		        d2f = d2_alhood(a, alpha_sum, D, K);
		        log_a = log_a - df/(d2f * a + df);
		        //printf("alpha maximization : %5.5f   %5.5f\n", f, df);
		    }
		    while ((Math.abs(df) > NEWTON_THRESH) && (iter < MAX_ALPHA_ITER));
		    return(Math.exp(log_a));
	}
}
