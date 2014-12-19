package cn.clickwise.medlda;

/**
 * 一个子树，一个父节点，多个子节点的一层树
 * @author zkyz
 */
public class SubTree {

	private int fTreeIndex;
	
	private int sTreeIndex;
	
	private int tTreeIndex;

	public SubTree(int fTreeIndex,int sTreeIndex,int tTreeIndex)
	{
	  this.fTreeIndex=fTreeIndex;
	  this.sTreeIndex=sTreeIndex;
	  this.tTreeIndex=tTreeIndex;
	}
	
	public int getfTreeIndex() {
		return fTreeIndex;
	}

	public void setfTreeIndex(int fTreeIndex) {
		this.fTreeIndex = fTreeIndex;
	}

	public int getsTreeIndex() {
		return sTreeIndex;
	}

	public void setsTreeIndex(int sTreeIndex) {
		this.sTreeIndex = sTreeIndex;
	}

	public int gettTreeIndex() {
		return tTreeIndex;
	}

	public void settTreeIndex(int tTreeIndex) {
		this.tTreeIndex = tTreeIndex;
	}
	
}
