package cn.clickwise.medldatb;

/**
 * MedLDA 模型运行配置
 * @author lq
 *
 */
public class MedLDAConfig {

	public static boolean isWordSelection=false;
	
	//0 linear
	//1 softmax
	public static int weighttype=0;
	
	//0 host
	//1 tb
	public static int sampleType=0;
	
}
