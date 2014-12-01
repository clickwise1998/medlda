package cn.clickwise.file.utils;

import java.io.File;

public class FileCreateUtil {

	public static void make_directory(String dir)
	{
		File dirHandle=new File(dir);
		if(!(dirHandle.exists()))
		{
			dirHandle.mkdirs();
		}
	}
}
