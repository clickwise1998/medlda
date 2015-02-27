package cn.clickwise.medldatb;

/**
 * 一个子树，一个父节点，多个子节点的一层树
 * @author zkyz
 */
public class SubTree {

	/***第一层的索引 always zero*****/
	private int fTreeIndex;
	
	/***第二层的索引********/
	private int sTreeIndex;
	
	/***第三层的索引********/
	private int tTreeIndex;
	
	/***该树的索引******/
	private int treeIndex;

    /**
     * 该树所在的层次 0 1 2 
     */
	private int level;
	
	/***子节点的个数****/
	private int size;
	
	public SubTree(int fTreeIndex,int sTreeIndex,int tTreeIndex,int level,int size)
	{
	  this.fTreeIndex=fTreeIndex;
	  this.sTreeIndex=sTreeIndex;
	  this.tTreeIndex=tTreeIndex;
	  this.level=level;
	  this.size=size;
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

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getTreeIndex() {
		return treeIndex;
	}

	public void setTreeIndex(int treeIndex) {
		this.treeIndex = treeIndex;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
}
