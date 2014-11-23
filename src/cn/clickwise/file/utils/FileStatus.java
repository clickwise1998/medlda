package cn.clickwise.file.utils;

import java.io.File;

/**
 * 判断文件的具体信息
 * @author lq
 *
 */
public class FileStatus {

	
	public static boolean isRegularFile(File file)
	{
		String file_name=file.getName();
		if(file_name.charAt(0)=='.')
		{
			return false;
		}
		if(file_name.indexOf("crc")>-1)
		{
			return false;
		}
		return true;
	}
	
}
