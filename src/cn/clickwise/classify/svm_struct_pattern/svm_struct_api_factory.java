package cn.clickwise.classify.svm_struct_pattern;

public class svm_struct_api_factory {

	public static svm_struct_api getSvmStructApi()
	{
		if(svmconfig.model_type==0||svmconfig.model_type==1)
		{
		  return new svm_multiclass();
		}
		else if(svmconfig.model_type==2)
		{
			return new svm_struct_tb();
		}
		
		return null;
		
	}
}
