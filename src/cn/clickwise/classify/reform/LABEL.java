package cn.clickwise.classify.reform;
/**
 * this defines the y-part (the label) of a training example,
     e.g. the parse tree of the corresponding sentence.
 * @author lq
 *
 */
public class LABEL {

	public int class_index;
	public int num_classes;
	
	/***for tb*******/
	public int first_class;
	
	public int second_class;
	
	public int third_class;
	
	public int first_size;
	
	public int second_size;
	
	public int third_size;
	
	/**************/
	
	public double[] scores;
	
	public void free()
	{
		scores=null;
	}
	
	public void initValue()
	{
		first_class=0;
		second_class=0;
		third_class=0;
		class_index=0;
	}
	
}
