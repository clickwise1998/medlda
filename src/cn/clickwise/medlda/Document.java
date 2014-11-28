package cn.clickwise.medlda;

public class Document {

	public int gndlabel;
	
	public int predlabel;
	
	public int lossAugLabel;
	
	public double lhood;
	
	public int[] words;
	
	public int[] counts;
	
	public int length;
	
	public int total;

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
