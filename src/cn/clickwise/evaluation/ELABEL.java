package cn.clickwise.evaluation;

public class ELABEL {

	private int label;
	
	public ELABEL(int label)
	{
		this.setLabel(label);
	}

	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}
	
	public String getKey()
	{
		return label+"";
	}
}
