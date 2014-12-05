package cn.clickwise.math.random;

import java.util.Random;

import cn.clickwise.time.utils.TimeOpera;

public class SeedRandom {

	private static Random r = new Random(TimeOpera.getCurrentTimeLong());
	
    public static double getRandom()
    {
    	return r.nextDouble();
    }
    
    public static void main(String[] args)
    {
    	for(int i=0;i<10;i++)
    	{
    		System.out.println("i="+i+" "+getRandom());
    	}
    }
}
