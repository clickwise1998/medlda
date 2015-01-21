package cn.clickwise.test;

public class MemoryTest {

	public static void main(String[] args)
	{
		
		if(args.length!=2)
		{
			System.err.println("Usage:<word num> <test type>" +
					"word num:声明的单词个数" +
					"test type: 0不new 1 new");
			
			System.exit(1);
		}
		
		long wordNum=Long.parseLong(args[0]);
		int type=Integer.parseInt(args[1]);
		int i=0;
		while(true)
		{
			if(i<wordNum)
			{
				i++;
				WORD[] test;
				if(type==1)
				{
					test=new WORD[1];
				}
			}
		}
	}
}
