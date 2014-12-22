package cn.clickwise.classify.svm_struct_pattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Pattern;

import cn.clickwise.str.basic.SSO;

public class svm_struct_tb extends svm_struct_api {
	
	public  int read_totdocs;
	public  int read_totwords;
	public  int read_max_docs;
	public  int read_max_words_doc;

	public  double read_first_label;
	public  double read_second_label;
	public  double read_third_label;
	
	public  int read_queryid;
	public  int read_slackid;
	public  double read_costfactor;
	public  int read_wpos;
	public  String read_comment;

	public  WORD[] read_words;
	public  LABEL[] read_target = null;
	public LABEL[] posslabes=null;
	
	@Override
	public void init_struct_model(SAMPLE sample, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm, LEARN_PARM lparm, KERNEL_PARM kparm) {
		int i, totwords = 0;
		WORD w;
		WORD[] temp_words;

		//sparm.num_classes = 1;
		sparm.first_size = 1;
		sparm.second_size = 1;
		sparm.third_size = 1;
		
		for (i = 0; i < sample.n; i++)
			/* find highest class label */
			if (sparm.first_size < ((sample.examples[i].y.first_class) + 0.1))
			{
				sparm.first_size = (int) (sample.examples[i].y.first_class + 0.1);
			}
		
		    if (sparm.second_size < ((sample.examples[i].y.second_class) + 0.1))
		    {
			    sparm.second_size = (int) (sample.examples[i].y.second_class + 0.1);
		    }
		
		    if (sparm.third_size < ((sample.examples[i].y.third_class) + 0.1))
		    {
			    sparm.third_size = (int) (sample.examples[i].y.third_class + 0.1);
		    }
		
		for (i = 0; i < sample.n; i++) /* find highest feature number */
		{
			temp_words = sample.examples[i].x.doc.fvec.words;
			for (int j = 0; j < temp_words.length; j++) {
				w = temp_words[j];
				if (totwords < w.wnum) {
					totwords = w.wnum;
				}
			}

		}

		sparm.num_features = totwords;
		if (svm_struct_common.struct_verbosity >= 0) {
			System.out.println("Training set properties: " + sparm.num_features
					+ " features " + sparm.first_size + " first_classes "+sparm.second_size+" second_classes "+sparm.third_size+" third_classes");
		}
		// logger.info("sparm.num_features:"+sparm.num_features);

		//sm.sizePsi = sparm.num_features * sparm.num_classes;
		sm.sizePsi=0;
		sm.sizePsi+=sparm.num_features*sparm.first_size;
		sm.sizePsi+=sparm.num_features*sparm.second_size;
		sm.sizePsi+=sparm.num_features*sparm.third_size;
		// logger.info("sm.sizePsi:"+sm.sizePsi);
		if (svm_struct_common.struct_verbosity >= 2) {
			System.out.println("Size of Phi: " + sm.sizePsi + "\n");
		}
		
	}

	@Override
	public SVECTOR psi(PATTERN x, LABEL y, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LABEL find_most_violated_constraint_slackrescaling(PATTERN x,
			LABEL y, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LABEL find_most_violated_constraint_marginrescaling(PATTERN x,
			LABEL y, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm) {
		LABEL ybar = new LABEL();
		DOC doc;
		int ci = 0;
		int bestfirst = -1,first=1;
		int bestsecond = -1;
		int bestthird = -1;
		double score, bestscore = -1;

		/*
		 * NOTE: This function could be made much more efficient by not always
		 * computing a new PSI vector.
		 */
		doc = x.doc.copyDoc();
        //logger.info("in find_most_violated_constraint_marginrescaling");
        //logger.info(doc.fvec.toString());
        
		ybar.scores = null;
		//ybar.num_classes = sparm.num_classes;
		ybar.first_size = sparm.first_size;
		ybar.second_size = sparm.second_size;
		ybar.third_size = sparm.third_size;
		
		// logger.info("ybar_num_classes:"+ybar.num_classes);
		// String winfo="";
		for (ci = 1; ci <= posslabes.length; ci++) {
			// logger.info("ci="+ci);
			ybar.first_class= posslabes[ci].first_class;
			ybar.second_class= posslabes[ci].second_class;
			ybar.third_class= posslabes[ci].third_class;
			
			// logger.info("before psi");
			doc.fvec = psi(x, ybar, sm, sparm);
			//logger.info("doc fvec:"+doc.fvec.toString());
			score = sc.classify_example(sm.svm_model, doc);
			// logger.info("ybar.class_index:"+ybar.class_index+"  score:"+score);
			score += loss(y, ybar, sparm);
			if ((bestscore < score) || (first != 0)) {
				bestscore = score;
				bestfirst = posslabes[ci].first_class;
				bestsecond=posslabes[ci].second_class;
				bestthird=posslabes[ci].third_class;
				first = 0;
			}
		}
		if (bestfirst == -1|bestsecond == -1||bestthird == -1)
			logger.debug("ERROR: Only one class\n");
		//logger.info("bestclass is "+bestclass);
		//ybar.class_index = bestclass;
		ybar.first_class = bestfirst;
		ybar.second_class = bestsecond;
		ybar.third_class = bestthird;
		
		// logger.info("ybar_class_index:"+ybar.class_index);
		if (svm_struct_common.struct_verbosity >= 3) {
			logger.info("[%" + bestfirst +"_"+bestsecond+"_"+bestthird+ ":" + bestscore + "] ");
		}
		//logger.info("bestscore:"+bestscore);
		return (ybar);
	}

	@Override
	public double loss(LABEL y, LABEL ybar, STRUCT_LEARN_PARM sparm) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SAMPLE read_struct_examples(String file, STRUCT_LEARN_PARM sparm) {
		/*
		 * Reads training examples and returns them in sample. The number of
		 * examples must be written into sample.n
		 */
		SAMPLE sample = new SAMPLE(); /* sample */
		EXAMPLE[] examples;
		int n; /* number of examples */
		DOC[] docs; /* examples in original SVM-light format */
		LABEL[] target = null;
		int totwords, i, num_first_classes = 0,num_second_classes = 0,num_third_classes = 0;

		/* Using the read_documents function from SVM-light */
		docs = read_documents(file, target);
	
		// logger.info("in read struct examples: docs.length:"+docs.length);
		target =read_target;
		logger.info("target:"+target);
		totwords = sc.read_totwords;
		logger.info("totwords:"+totwords);
		n = sc.read_totdocs;
         logger.info("n:"+n);
        //logger.info("totwords:"+totwords);
		for(int k=0;k<docs.length;k++)
		{
			if(docs[k]==null||docs[k].fvec==null)
			{
				continue;
			}
			//logger.info("test["+k+"]="+docs[k].fvec.toString());
		}
		examples = new EXAMPLE[n];
		for (int k = 0; k < n; k++) {
			examples[k] = new EXAMPLE();
			examples[k].x = new PATTERN();
			examples[k].y = new LABEL();
		}

		for (i = 0; i < n; i++)
			/* find highest class label */
			if (num_first_classes < (target[i].first_class + 0.1))
			{
				num_first_classes = (int) (target[i].first_class + 0.1);
			}
		
		    if (num_second_classes < (target[i].second_class + 0.1))
		    {
			    num_second_classes = (int) (target[i].second_class + 0.1);
		    }
		
		    if (num_third_classes < (target[i].third_class + 0.1))
		    {
			    num_third_classes = (int) (target[i].third_class + 0.1);
		    }
		for (i = 0; i < n; i++)
			/* make sure all class labels are positive */

		for (i = 0; i < n; i++) { /* copy docs over into new datastructure */
			examples[i].x.doc = docs[i];
		   // logger.info("example "+i+"  "+examples[i].x.doc.fvec.toString());
			//examples[i].y.class_index = (int) (target[i] + 0.1);
			examples[i].y.first_class = (int) (target[i].first_class + 0.1);
			examples[i].y.second_class = (int) (target[i].second_class + 0.1);
			examples[i].y.third_class = (int) (target[i].third_class + 0.1);
			// logger.info("the label is "+target[i]+" \n");
			examples[i].y.scores = null;
			//examples[i].y.num_classes = num_classes;
			examples[i].y.first_size = num_first_classes;
			examples[i].y.second_size = num_second_classes;
			examples[i].y.third_size = num_third_classes;
		}

		sample.n = n;
		sample.examples = examples;

		if (svm_struct_common.struct_verbosity >= 0)
			logger.info(" (" + sample.n + " examples) ");

		return (sample);
	}

	@Override
	public void write_struct_model(String file, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public STRUCTMODEL read_struct_model(String file, STRUCT_LEARN_PARM sparm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LABEL classify_struct_example(PATTERN x, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) {
		// TODO Auto-generated method stub
		return null;
	}

	public  DOC[] read_documents(String docfile, LABEL[] label) {
		String line, comment;
		//PrintWriter pw = null;
		//FileWriter fw = null;
		DOC[] docs;
		/*
		try {
			fw = new FileWriter(new File("log2.txt"));
			pw = new PrintWriter(fw);
		} catch (Exception e) {
		}
		*/
		int dnum = 0, wpos, dpos = 0, dneg = 0, dunlab = 0, queryid, slackid, max_docs;
		int max_words_doc, ll;
		double doc_label, costfactor;
		FileReader fr = null;
		BufferedReader br = null;

		if (svm_common.verbosity >= 1) {
			System.out.println("Scanning examples...");
		}

		nol_ll(docfile); /* scan size of input file */
		read_max_words_doc += 2;
		read_max_docs += 2;
		if (svm_common.verbosity >= 1) {
			System.out.println("done\n");
		}
		try {
			fr = new FileReader(new File(docfile));
			br = new BufferedReader(fr);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		docs = new DOC[read_max_docs]; /* feature vectors */
		// for(int k=0;k<read_docs.length;k++)
		// {
		// read_docs[k]=ps.getDOC();
		// }
		WORD[] words;
		label = new LABEL[read_max_docs]; /* target values */
		// System.out.println("docs length:"+docs.length);
		words = new WORD[read_max_words_doc + 10];
		for (int j = 0; j < words.length; j++) {
			words[j] = new WORD();
			words[j].wnum = 0;
			words[j].weight = 0;
		}
		if (svm_common.verbosity >= 1) {
			System.out.println("Reading examples into memory...");
		}
		dnum = 0;
		read_totwords = 0;
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.charAt(0) == '#')
					continue; /* line contains comments */
				// System.out.println(line);

				if ((words = parse_document(line, read_max_words_doc)) == null) {
					System.out.println("\nParsing error in line " + dnum
							+ "!\n" + line);
					// System.exit(1);
					continue;
				}
				//label[dnum] = read_doc_label;
				/*********medlda +1*************/

				label[dnum].first_class = (int)read_first_label;
				label[dnum].second_class = (int)read_second_label;
				label[dnum].third_class = (int)read_third_label;

				
				if ((read_wpos > 1)
						&& ((words[read_wpos - 2]).wnum > read_totwords))
					read_totwords = words[read_wpos - 2].wnum;

				docs[dnum] = svm_common.create_example(dnum, read_queryid, read_slackid,
						read_costfactor,
						svm_common.create_svector(words, read_comment, 1.0));
				//pw.println("docs dnum[" + dnum + "]"
				//		+ docs[dnum].fvec.words.length);
				/*
				for (int k = 0; k < 100; k++) {
					pw.print(" " + docs[dnum].fvec.words[k].wnum + ":"
							+ docs[dnum].fvec.words[k].weight);
				}
				*/
				// System.out.println();
				/* printf("\nNorm=%f\n",((*docs)[dnum]->fvec)->twonorm_sq); */
				dnum++;
				//if (verbosity >= 1) {
				//	if ((dnum % 100) == 0) {
						// System.out.println(dnum+"..");
				//	}
				//}
			}

			fr.close();
			br.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if (svm_common.verbosity >= 1) {
			System.out.println("OK. (" + dnum + " examples read)\n");
		}
		read_totdocs = dnum;
		read_target = label;
		return docs;
	}

	public  WORD[] parse_document(String line, int max_words_doc) {
		int wpos = 0, pos;
		int wnum;
		double weight;
		String featurepair, junk;
		if (SSO.tioe(line)) {
			return null;
		}

		read_words = new WORD[max_words_doc];
		for (int k = 0; k < read_words.length; k++) {
			read_words[k] = new WORD();
			read_words[k].wnum = 0;
			read_words[k].weight = 0;
		}
		read_queryid = 0;
		read_slackid = 0;
		read_costfactor = 1;

		pos = 0;
		read_comment = "";
		String dline = "";
		/* printf("Comment: '%s'\n",(*comment)); */
		// logger.info("lline:"+line);
		if (line.indexOf("#") > 0) {
			read_comment = line.substring(line.indexOf("#") + 1, line.length());
			dline = line.substring(0, line.indexOf("#"));
		} else {
			dline = line;
		}

		dline = dline.trim();
		wpos = 0;
		String[] seg_arr = dline.split(" ");
		if ((seg_arr.length < 3) || (seg_arr[0].indexOf("#") > -1)) {
			return null;
		}
		read_first_label = Double.parseDouble(seg_arr[0]);
		read_second_label = Double.parseDouble(seg_arr[1]);
		read_third_label = Double.parseDouble(seg_arr[2]);
		
		String wstr = "";
		String pstr = "";
		String sstr = "";
		for (int i = 3; i < seg_arr.length; i++) {
			wstr = seg_arr[i].trim();
			if (wstr.indexOf(":") < 0) {
				continue;
			}
			pstr = wstr.substring(0, wstr.indexOf(":"));
			sstr = wstr.substring(wstr.indexOf(":") + 1, wstr.length());
			pstr = pstr.trim();
			sstr = sstr.trim();
			if (pstr.equals("qid")) {
				read_queryid = Integer.parseInt(sstr);
			} else if (pstr.equals("sid")) {
				read_slackid = Integer.parseInt(sstr);
			} else if (pstr.equals("cost")) {
				read_costfactor = Double.parseDouble(sstr);
			} else if (Pattern.matches("[\\d]+", pstr)) {
				read_words[wpos].wnum = Integer.parseInt(pstr);
				
				/***********medlda+1*************/
				if(svmconfig.model_type==0)
				{
					read_words[wpos].wnum= read_words[wpos].wnum;
				}
				else{
					read_words[wpos].wnum= read_words[wpos].wnum+1;
				}
				
				/********************************/
				
				read_words[wpos].weight = Double.parseDouble(sstr);
				wpos++;
			}
		}

		read_words[wpos].wnum = 0;
		read_wpos = wpos + 1;
		return read_words;
	}
	
	public  void nol_ll(String input_file) {

		// logger.info("input_file:"+input_file);
		// logger.info("in nol ll");
		FileReader fr = null;
		BufferedReader br = null;

		try {
			fr = new FileReader(new File(input_file));
			br = new BufferedReader(fr);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error:" + e.getMessage());
		}
		// logger.info("in nol ll2");
		String line = "";
		int temp_docs = 0;
		int temp_words = 0;
		String[] seg_arr = null;
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				// logger.info("line:"+line);
				temp_docs++;
				seg_arr = line.split("\\s+");
				if (seg_arr.length > temp_words) {
					temp_words = seg_arr.length;
				}
			}

			read_max_docs = temp_docs;
			read_max_words_doc = temp_words;
			// System.out.println("read_max_docs:" + read_max_docs);
			// System.out.println("read_max_words_doc:" + read_max_words_doc);

			fr.close();
			br.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}
}
