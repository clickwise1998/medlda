package cn.clickwise.classify.svm_struct;

import java.util.ArrayList;
import java.util.Collections;


import cn.clickwise.sort.utils.SortStrArray.SortElement;
import cn.clickwise.str.basic.SSO;



public class SortWordArr {

		
	public static class SortElement implements Comparable{
		
       public WORD w;
		
		public SortElement(WORD w)
		{
		 this.w=w;
		}
			
		public int compareTo(Object o) {
			SortElement s = (SortElement)o;
		    return w.wnum> s.w.wnum ? 1 : (w.wnum ==s.w.wnum  ? 0 : -1);		
		};
		
		public String toString(){
			String info="";	
		    return  info ;
		}
	}
	
	public static class DoubleElement implements Comparable{
		
	        public RANDPAIR rp;
			
			public DoubleElement(RANDPAIR rp)
			{
			     this.rp=rp;
			}
				
			public int compareTo(Object o) {
				DoubleElement s = (DoubleElement)o;
			    return rp.sort> s.rp.sort ? 1 : (rp.sort ==s.rp.sort  ? 0 : -1);		
			};
			
			public String toString(){
				String info="";	
			    return  info ;
			}
	}
	
	
   /**
   * 排序words
   * @param arr
   */
	public static WORD[] sort_array(WORD[] arr)
	{
		ArrayList<SortElement> al = new ArrayList<SortElement>();		
	 	SortElement sorele=null;	 	
	 	WORD row=null;
	 	
		for(int i=0;i<arr.length;i++)
		{
			row=arr[i];
			sorele=new SortElement(row);
			al.add(sorele);
		}
		Collections.sort(al);
		
		WORD[] narr=new WORD[al.size()];
		for(int i=0;i<al.size();i++)
		{
			narr[i]=al.get(i).w;
		}
		
		return narr;	
	}
	
	
	/**
	 * 
	 * @param arr
	 */
	public static RANDPAIR[] sort_double_array(RANDPAIR[] arr)
	{
		ArrayList<DoubleElement> al = new ArrayList<DoubleElement>();
		DoubleElement dele=null;
		RANDPAIR rp=null;
		for(int i=0;i<arr.length;i++)
		{
			rp=arr[i];
			dele=new DoubleElement(rp);
			al.add(dele);
		}
		Collections.sort(al);
		
		RANDPAIR[] narr=new RANDPAIR[al.size()];
		for(int i=0;i<al.size();i++)
		{
			narr[i]=al.get(i).rp;
		}
		
		return narr;		
	}

	
}