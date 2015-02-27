package cn.clickwise.medldatb;

public class TBLabel {
	
	/********label for tb****/	
	private int fgndlabel;
	
	private int sgndlabel;
	
	private int tgndlabel;
	
	private int fgndsize;
	
	private int sgndsize;
	
	private int tgndsize;
	
	//always zero
	private int ftreeIndex;
	
	private int streeIndex;
	
	private int ttreeIndex;
	
	/*********************/
	
	public TBLabel(int fgndlabel,int sgndlabel,int tgndlabel,int fgndsize,int sgndsize,int tgndsize)
	{
		this.fgndlabel=fgndlabel;
		this.sgndlabel=sgndlabel;
		this.tgndlabel=tgndlabel;
		
		this.fgndsize=fgndsize;
		this.sgndsize=sgndsize;
		this.tgndsize=tgndsize;
	}

	public int getFgndlabel() {
		return fgndlabel;
	}

	public void setFgndlabel(int fgndlabel) {
		this.fgndlabel = fgndlabel;
	}

	public int getSgndlabel() {
		return sgndlabel;
	}

	public void setSgndlabel(int sgndlabel) {
		this.sgndlabel = sgndlabel;
	}

	public int getTgndlabel() {
		return tgndlabel;
	}

	public void setTgndlabel(int tgndlabel) {
		this.tgndlabel = tgndlabel;
	}

	public int getFgndsize() {
		return fgndsize;
	}

	public void setFgndsize(int fgndsize) {
		this.fgndsize = fgndsize;
	}

	public int getSgndsize() {
		return sgndsize;
	}

	public void setSgndsize(int sgndsize) {
		this.sgndsize = sgndsize;
	}

	public int getTgndsize() {
		return tgndsize;
	}

	public void setTgndsize(int tgndsize) {
		this.tgndsize = tgndsize;
	}

	public int getFtreeIndex() {
		return ftreeIndex;
	}

	public void setFtreeIndex(int ftreeIndex) {
		this.ftreeIndex = ftreeIndex;
	}

	public int getStreeIndex() {
		return streeIndex;
	}

	public void setStreeIndex(int streeIndex) {
		this.streeIndex = streeIndex;
	}

	public int getTtreeIndex() {
		return ttreeIndex;
	}

	public void setTtreeIndex(int ttreeIndex) {
		this.ttreeIndex = ttreeIndex;
	}
}
