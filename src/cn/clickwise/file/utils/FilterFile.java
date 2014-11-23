package cn.clickwise.file.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import cn.clickwise.str.basic.SSO;

public class FilterFile {

	public static void fileter(File input_file, File output_file, String keyword) {
		try {
			FileWriter fw = new FileWriter(output_file);
			PrintWriter pw = new PrintWriter(fw);
			String[] samples = FileToArray.fileToDimArr(input_file);
			String line = "";
			for (int i = 0; i < samples.length; i++) {
				line = samples[i];
				if (SSO.tioe(line)) {
					continue;
				}
				if (line.indexOf(keyword) > -1) {
					continue;
				}

				pw.println(line.trim());
			}
			fw.close();
			pw.close();
		} catch (Exception e) {

		}
	}
	
	
	public static void main(String[] args)
	{
		File input_file=new File("temp/seg_test/five_dict_uniq.txt");
		File output_file=new File("temp/seg_test/five_dict_uniq_clean.txt");
		
		FilterFile.fileter(input_file, output_file, "的");
		
		
		
		
	}

}
