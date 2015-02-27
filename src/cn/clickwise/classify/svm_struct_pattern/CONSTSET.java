package cn.clickwise.classify.svm_struct_pattern;

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
	
}
