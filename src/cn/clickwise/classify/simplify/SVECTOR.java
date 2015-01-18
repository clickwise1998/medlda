package cn.clickwise.classify.simplify;



public class SVECTOR {
	  public WORD[] words;
	  public double twonorm_sq;
	  public String userdefined;
	  public int kernel_id;
	  public SVECTOR next;
	  public double factor;
	 
	  
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
	  
	  public void destroy()
	  {
		  if(words!=null)
		  {
			  for(int i=0;i<words.length;i++)
			  {
				  words[i]=null;
			  }
		  }
		  
		  words=null;
		  
		  SVECTOR cur;
		  
		  cur=next;
		  
		  while(cur!=null)
		  {
			 cur.destroy();
			 cur=cur.next;
		  }
		  
		  next=null;
	  }
}
