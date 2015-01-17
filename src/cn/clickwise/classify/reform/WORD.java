package cn.clickwise.classify.reform;

public class WORD {
	  public int wnum=0;
	  public double weight=0;
	  
	  public WORD copy_word()
	  {
		  WORD nw=new WORD();
		  nw.weight=weight;
		  nw.wnum=wnum;
		  return nw;
	  }
	  
}
