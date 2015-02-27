package cn.clickwise.classify.simplify;

public class MATRIX {
	  public int n;
	  public int m;
	  public double[][] element;
	  
	  public MATRIX copyMATRIX()
	  {
		  MATRIX mar=new MATRIX();
		  mar.n=n;
		  mar.m=m;
		  mar.element=new double[element.length][element[0].length];
		  for(int i=0;i<element.length;i++)
		  {
			  for(int j=0;j<element[0].length;j++)
			  {
				  mar.element[i][j]=element[i][j];
			  }
		  }
		  
		  return mar;
	  }
}
