package cn.clickwise.medlda;

public class SuffStats {

	//主题、单词频率统计，行是主题，列是单词
	public double[][] class_word;
	
	//主题里的词数统计，行是主题
	public double[] class_total;
	
	//alpha的充分统计量
	public double[] alpha_suffstats;
	
	
	public int num_docs;
	
	/**
	 * exp应该是sum{z1,...,zn}
	 * 二维向量，行是文档、列是主题
	 */
	public double[][] exp;
	
	public int[] y;
	
	public String dir;

	public double[][] getClass_word() {
		return class_word;
	}

	public void setClass_word(double[][] class_word) {
		this.class_word = class_word;
	}

	public double[] getClass_total() {
		return class_total;
	}

	public void setClass_total(double[] class_total) {
		this.class_total = class_total;
	}

	public double[] getAlpha_suffstats() {
		return alpha_suffstats;
	}

	public void setAlpha_suffstats(double[] alpha_suffstats) {
		this.alpha_suffstats = alpha_suffstats;
	}

	public int getNum_docs() {
		return num_docs;
	}

	public void setNum_docs(int num_docs) {
		this.num_docs = num_docs;
	}

	public double[][] getExp() {
		return exp;
	}

	public void setExp(double[][] exp) {
		this.exp = exp;
	}

	public int[] getY() {
		return y;
	}

	public void setY(int[] y) {
		this.y = y;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}
	
	
	
	
	
}
