package cn.clickwise.str.basic;

import java.util.Scanner;

public class StdReader {

	private Scanner scanner=null;
	public StdReader()
	{
		scanner = new Scanner(System.in,"utf-8"); 
	}
	
	public String readLine()
	{           
		 String line = scanner.next();
		 return line;
	}
	
	public static void main(String[] args)
	{
		StdReader stdreader=new StdReader();
	    System.out.println(stdreader.readLine());
	}
}
