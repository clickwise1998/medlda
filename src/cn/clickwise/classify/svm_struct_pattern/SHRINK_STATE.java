package cn.clickwise.classify.svm_struct_pattern;

public class SHRINK_STATE {
	  int[] active;
	  int[] inactive_since;
	  int deactnum;
	  double[][] a_history;
	  int maxhistory;
	  double[] last_a;
	  double[] last_lin;
}
