package cn.clickwise.file.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

import cn.clickwise.str.basic.SSO;

public class FileReaderUtil {

	/**
	 * 获取hashmap,第一列为key,第二列为value
	 * 
	 * @return
	 */
	public static HashMap<String, String> getHashFromPlainFile(String input_file) {
		// TODO Auto-generated method stub

		HashMap<String, String> hm = new HashMap<String, String>();
		String item = "";
		String word = "";
		String index_str = "";
		int index = 0;
		FileReader fr = null;
		BufferedReader br = null;

		String[] seg_arr = null;

		try {
			fr = new FileReader(new File(input_file));
			br = new BufferedReader(fr);
			while ((item = br.readLine()) != null) {
				if (!(SSO.tnoe(item))) {
					continue;
				}
				seg_arr = item.split("\\s+");
				if (seg_arr.length != 2) {
					continue;
				}
				word = seg_arr[0].trim();
				index_str = seg_arr[1].trim();

				if (!(SSO.tnoe(word))) {
					continue;
				}

				if (!(SSO.tnoe(index_str))) {
					continue;
				}
				if (!(hm.containsKey(word))) {
					hm.put(word, index_str);
				}
			}
			fr.close();
			br.close();
		} catch (Exception e) {

		}

		return hm;
	}

	/**
	 * 读取模型的词典文件，建立单词索引 词典索引从1开始
	 * 
	 * @return
	 */
	public static HashMap getDictFromPlainFile(String input_file) {
		// TODO Auto-generated method stub

		HashMap hm = new HashMap();
		String item = "";
		String word = "";
		String index_str = "";
		int index = 0;
		FileReader fr = null;
		BufferedReader br = null;

		String[] seg_arr = null;

		try {
			fr = new FileReader(new File(input_file));
			br = new BufferedReader(fr);
			while ((item = br.readLine()) != null) {
				if (!(SSO.tnoe(item))) {
					continue;
				}
				seg_arr = item.split("\\s+");
				if (seg_arr.length != 2) {
					continue;
				}
				word = seg_arr[0].trim();
				index_str = seg_arr[1].trim();

				if (!(SSO.tnoe(word))) {
					continue;
				}

				if (!(SSO.tnoe(index_str))) {
					continue;
				}
				index = Integer.parseInt(index_str);
				// if(index%100==0)
				// {
				// System.out.println(word+" "+index_str);
				// }
				if (index < 1) {
					continue;
				}
				hm.put(word, index);
			}

		} catch (Exception e) {

		}
		return hm;
	}

	/**
	 * 类别索引和类别名的对应关系 类别索引从1开始
	 * 
	 * @return
	 */
	public static HashMap getIndexLabelFromPlainFile(String input_file) {
		// TODO Auto-generated method stub

		HashMap hm = new HashMap();
		String item = "";
		String label = "";
		String index_str = "";
		int index = 0;
		FileReader fr = null;
		BufferedReader br = null;

		String[] seg_arr = null;

		try {
			fr = new FileReader(new File(input_file));
			br = new BufferedReader(fr);
			while ((item = br.readLine()) != null) {
				if (!(SSO.tnoe(item))) {
					continue;
				}
				seg_arr = item.split("\\s+");
				if (seg_arr.length != 2) {
					continue;
				}
				label = seg_arr[0].trim();
				index_str = seg_arr[1].trim();

				if (!(SSO.tnoe(index_str))) {
					continue;
				}

				if (!(SSO.tnoe(label))) {
					continue;
				}
				index = Integer.parseInt(index_str);
				// if(index%100==0)
				// {
				// System.out.println(word+" "+index_str);
				// }
				if (index < 1) {
					continue;
				}
				hm.put(index_str, label);
			}

		} catch (Exception e) {

		}
		return hm;
	}

	/**
	 * jarFile 转换为hashMap 输入文件为两列或一列 如果为两列，第一列为key、第二列为value
	 * 如果为一列，该列为key、value是自动编号
	 * 
	 * @param fileName
	 * @return HashMap<String,String>
	 */
	public static HashMap<String, String> file2Hash(String fileName) {
		HashMap<String, String> ht = new HashMap<String, String>();
		try {
			FileReader fr = new FileReader(new File(fileName));
			BufferedReader fb = new BufferedReader(fr);
			String line = fb.readLine();
			line = line.trim();
			int field_num = 0;
			String[] seg_arr = line.split("\001");
			field_num = seg_arr.length;

			String key = "";
			String value = "";
			int index = 0;
			while (SSO.tnoe(line)) {
				if (field_num == 2) {
					key = line.substring(0, line.indexOf("\001")).trim();
					value = line.substring(line.indexOf("\001") + 1).trim();
					if (!(ht.containsKey(key))) {
						ht.put(key, value);
					}
				} else if (field_num == 1) {
					key = line;
					value = (++index) + "";
					if (!(ht.containsKey(key))) {
						ht.put(key, value);
					}
				}
				line = fb.readLine();
			}
			fb.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ht;
	}

	public static String readContent(File input_file) {
		String content = "";
		try {
			FileReader fr = new FileReader(input_file);
			BufferedReader br = new BufferedReader(fr);

			String line = "";
			while ((line = br.readLine()) != null) {
				if (SSO.tioe(line)) {
					continue;
				}
				content += line;
			}

			fr.close();
			br.close();
		} catch (Exception e) {

		}

		return content;
	}

}
