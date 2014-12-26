package cn.clickwise.classify.sspm;

public class KERNEL_CACHE {
	  public int[] index;
	  public double[] buffer;
	  public int[] invindex;
	  public int[] active2totdoc;
	  public int[] totdoc2active;
	  public int[] lru; 
	  public int[] occu;
	  public int elems;
	  public int max_elems;
	  public int time;
	  public int activenum;
	  public int buffsize;
}
