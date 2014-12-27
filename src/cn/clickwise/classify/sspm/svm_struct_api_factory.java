package cn.clickwise.classify.sspm;

public class svm_struct_api_factory {

	public static svm_struct_api getSvmStructApi()
	{
		//System.out.println("svmconfig.model_type:"+svmconfig.model_type);
		if((svmconfig.model_type==0)||(svmconfig.model_type==1))
		{
		  return new svm_multiclass();
		}
		else if(svmconfig.model_type==2)
		{
			return new svm_struct_tb();
		}
		else if(svmconfig.model_type==3)
		{
			return new svm_struct_tbs();
		}
		return null;
		
	}
}
