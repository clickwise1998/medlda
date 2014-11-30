package cn.clickwise.classify.svm_struct;




public class SVECTOR {
	  public WORD[] words;
	  public double twonorm_sq;
	  public String userdefined;
	  public int kernel_id;
	  public SVECTOR next;
	  public double factor;
	 
	  
	  public SVECTOR copySVECTOR()
	  {
		  SVECTOR sv=new SVECTOR();
		  sv.twonorm_sq=twonorm_sq;
		  sv.userdefined=userdefined;
		  sv.kernel_id=kernel_id;
		  sv.factor=factor;
		  sv.words=new WORD[words.length];
		  for(int i=0;i<words.length;i++)
		  {
			  if(words[i]!=null)
			  {
			    sv.words[i]=words[i].copy_word();
			  }
			  else
			  {
				  sv.words[i]=null;
			  }
		  }
		  if(next!=null)
		  {
		  sv.next=next.copySVECTOR();	  
		  }
		  else
		  {
			 next=null;
		  }
		  return sv;
	  }
	  
	  public String toString()
	  {
		  String wi="";
		  for(int i=0;(i<words.length);i++)
		  {
			  if(words[i]==null)
			  {
				  continue;
			  }
			  if(words[i].weight!=0)
			  {
			    wi=wi+words[i].wnum+":"+words[i].weight+" ";
			  }
		  }
		  wi=wi.trim();
		  
		  return wi;
	  }
	  
	  public String toLongString()
	  {
		  String wi="";
		  for(int i=0;(i<words.length);i++)
		  {
			  if(words[i]==null)
			  {
				  continue;
			  }
			  if(words[i].weight!=0)
			  {
			    wi=wi+words[i].wnum+":"+words[i].weight+" ";
			  }
		  }
		  wi=wi.trim();
		  
		  return wi;
	  }	  
}
