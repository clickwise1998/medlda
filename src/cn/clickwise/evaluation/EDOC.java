package cn.clickwise.evaluation;


public class EDOC {

	//参考标记
	private ELABEL label;
	
	//预测标记
	private ELABEL predLabel;
	
	public EDOC(ELABEL label,ELABEL predLabel)
	{
		this.label=label;
		this.predLabel=predLabel;
	}
	
	public EDOC(int label,int predLabel)
	{
		this.label=new ELABEL(label);
		this.predLabel=new ELABEL(predLabel);
	}

	public ELABEL getLabel() {
		return label;
	}

	public void setLabel(ELABEL label) {
		this.label = label;
	}

	public ELABEL getPredLabel() {
		return predLabel;
	}

	public void setPredLabel(ELABEL predLabel) {
		this.predLabel = predLabel;
	}
	
}
