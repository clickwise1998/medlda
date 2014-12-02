package cn.clickwise.str.basic;

import java.util.Scanner;

public class ScanTest {

	public static void main(String[] args)
	{
		Scanner scan=new Scanner("5 1 1:1 10:1 30:2 42222:3 224244:5");
		System.out.println(scan.nextInt());
		System.out.println(scan.nextInt());
		System.out.println(scan.next());
		System.out.println(scan.next());
		System.out.println(scan.next());
		double a=0;
		double c=Math.random();
		a+=10+Math.random();
		System.out.println("a="+a);
	}
}
