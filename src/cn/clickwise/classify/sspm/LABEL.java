package cn.clickwise.classify.sspm;
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
	
}
