package cn.clickwise.medlda;

public class Document {

	private int gndlabel;
	
	private int predlabel;
	
	private int lossAugLabel;
	
	private double lhood;
	
	private int[] words;
	
	private int[] counts;
	
	private int length;
	
	private int total;

	public int getGndlabel() {
		return gndlabel;
	}

	public void setGndlabel(int gndlabel) {
		this.gndlabel = gndlabel;
	}

	public int getPredlabel() {
		return predlabel;
	}

	public void setPredlabel(int predlabel) {
		this.predlabel = predlabel;
	}

	public int getLossAugLabel() {
		return lossAugLabel;
	}

	public void setLossAugLabel(int lossAugLabel) {
		this.lossAugLabel = lossAugLabel;
	}

	public double getLhood() {
		return lhood;
	}

	public void setLhood(double lhood) {
		this.lhood = lhood;
	}

	public int[] getWords() {
		return words;
	}

	public void setWords(int[] words) {
		this.words = words;
	}

	public int[] getCounts() {
		return counts;
	}

	public void setCounts(int[] counts) {
		this.counts = counts;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}
	
	
	
}
