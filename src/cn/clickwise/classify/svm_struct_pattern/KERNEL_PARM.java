package cn.clickwise.classify.svm_struct_pattern;


public class KERNEL_PARM {
	  public short kernel_type;
	  public int poly_degree;
	  public double rbf_gamma;
	  public double coef_lin;
	  public double coef_const;
	  public String custom;
	  public MATRIX gram_matrix;
	  
	  public KERNEL_PARM copyKERNEL_PARM()
	  {
		  KERNEL_PARM nkp=new KERNEL_PARM();
		  nkp.kernel_type=kernel_type;
		  nkp.poly_degree=poly_degree;
		  nkp.rbf_gamma=rbf_gamma;
		  nkp.coef_lin=coef_lin;
		  nkp.coef_const=coef_const;
		  nkp.custom=custom;
		  
		  return nkp;
	  }
	  
}
