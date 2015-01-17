package cn.clickwise.classify.reform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cn.clickwise.str.basic.SSO;

public class svm_struct_tbs extends svm_struct_api{
	
	public  int read_totdocs;
	public  int read_totwords;
	public  int read_max_docs;
	public  int read_max_words_doc;

	public  double read_first_label;
	public  double read_second_label;

	
	public  int read_queryid;
	public  int read_slackid;
	public  double read_costfactor;
	public  int read_wpos;
	public  String read_comment;

	public  WORD[] read_words;
	public  LABEL[] read_target = null;
	public LABEL[] posslabels=null;
	public svm_struct_tbs()
	{
		super();
		readPossLabels();
	}
	@Override
	public void init_struct_model(SAMPLE sample, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm, LEARN_PARM lparm, KERNEL_PARM kparm) {
		int i, totwords = 0;
		WORD w;
		WORD[] temp_words;

		//sparm.num_classes = 1;
		sparm.first_size = 1;
		sparm.second_size = 1;
		logger.info("sample.nh:"+sample.n);
		logger.info("sample.examples.len:"+sample.examples.length);
		for (i = 0; i < sample.n; i++)
		{
			/* find highest class label */
			if (sparm.first_size < ((sample.examples[i].y.first_class) + 0.1))
			{
				sparm.first_size = (int) (sample.examples[i].y.first_class + 0.1);
			}
		
		    if (sparm.second_size < ((sample.examples[i].y.second_class) + 0.1))
		    {
			    sparm.second_size = (int) (sample.examples[i].y.second_class + 0.1);
		    }
		
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
					+ " features " + sparm.first_size + " first_classes "+sparm.second_size+" second_classes ");
		}
		// logger.info("sparm.num_features:"+sparm.num_features);

		//sm.sizePsi = sparm.num_features * sparm.num_classes;
		sm.sizePsi=0;
		sm.sizePsi+=sparm.num_features*sparm.first_size;
		sm.sizePsi+=sparm.num_features*sparm.second_size;

		// logger.info("sm.sizePsi:"+sm.sizePsi);
		if (svm_struct_common.struct_verbosity >= 2) {
			System.out.println("Size of Phi: " + sm.sizePsi + "\n");
		}
		
	}

	@Override
	public SVECTOR psi(PATTERN x, LABEL y, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) {
		SVECTOR fvec=new SVECTOR();
		/*
		 * String wwinfo=""; for(int k=0;k<x.doc.fvec.words.length;k++) {
		 * wwinfo=
		 * wwinfo+x.doc.fvec.words[k].wnum+":"+x.doc.fvec.words[k].weight+" "; }
		 * logger.info("wwwwinfo:"+wwinfo);
		 */
		int xlen=x.doc.fvec.words.length;
		fvec.words=new WORD[xlen*2];

		for(int i=0;i<x.doc.fvec.words.length;i++)
		{
		  fvec.words[i]=x.doc.fvec.words[i].copy_word();
		  fvec.words[i+xlen]=x.doc.fvec.words[i].copy_word();
		  
		  fvec.words[i].wnum+=(y.first_class-1)*sparm.num_features;
		  fvec.words[i+xlen].wnum+=((y.second_class-1)*sparm.num_features+y.first_size*sparm.num_features);

		}
		
		String userdefined = "";
		SVECTOR vec = svm_common.create_svector_shallow(fvec.words, userdefined, x.doc.fvec.factor);
		// logger.info("fvec psi:"+fvec.toString());
		//vec.kernel_id = y.class_index;
		return vec;
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
		
		// logger.info("ybar_num_classes:"+ybar.num_classes);
		// String winfo="";
		for (ci = 0; ci < posslabels.length; ci++) {
			// logger.info("ci="+ci);
			ybar.first_class= posslabels[ci].first_class;
			ybar.second_class= posslabels[ci].second_class;
			
			// logger.info("before psi");
			doc.fvec = psi(x, ybar, sm, sparm);
			//logger.info("doc fvec:"+doc.fvec.toString());
			score = sc.classify_example(sm.svm_model, doc);
			// logger.info("ybar.class_index:"+ybar.class_index+"  score:"+score);
			score += loss(y, ybar, sparm);
			if ((bestscore < score) || (first != 0)) {
				bestscore = score;
				bestfirst = posslabels[ci].first_class;
				bestsecond=posslabels[ci].second_class;
				first = 0;
			}
		}
		if (bestfirst == -1|bestsecond == -1)
			logger.debug("ERROR: Only one class\n");
		//logger.info("bestclass is "+bestclass);
		//ybar.class_index = bestclass;
		ybar.first_class = bestfirst;
		ybar.second_class = bestsecond;

		
		// logger.info("ybar_class_index:"+ybar.class_index);
		if (svm_struct_common.struct_verbosity >= 3) {
			logger.info("[%" + bestfirst +"_"+bestsecond+":" + bestscore + "] ");
		}
		//logger.info("bestscore:"+bestscore);
		return (ybar);
	}

	@Override
	public double loss(LABEL y, LABEL ybar, STRUCT_LEARN_PARM sparm) {
		
		 if(sparm.loss_function == 0) { /* type 0 loss: 0/1 loss */

             if((y.first_class==ybar.first_class)&&(y.second_class==ybar.second_class))
             {
                     return(0);
             }
             else
             {
                     return(100);
             }
         }
		 
         if(sparm.loss_function == 1) { /* type 1 loss: squared difference */

             if((y.first_class==ybar.first_class)&&(y.second_class==ybar.second_class))
             {
                     return(0);
             }
             else
             {
                     return(100);
             }
         }
         else {
             /* Put your code for different loss functions here. But then
                find_most_violated_constraint_???(x, y, sm) has to return the
                highest scoring label with the largest loss. */
             System.out.println("Unkown loss function");
             System.exit(1);
         }
         
         return 100;

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
		int totwords, i, num_first_classes = 0,num_second_classes = 0;

		/* Using the read_documents function from SVM-light */
		docs = read_documents(file, target);
	
		// logger.info("in read struct examples: docs.length:"+docs.length);
		target =read_target;
		logger.info("target:"+target);
		totwords = read_totwords;
		logger.info("totwords:"+totwords);
		n = read_totdocs;
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
		logger.info("nh:"+n);
		examples = new EXAMPLE[n];
		for (int k = 0; k < n; k++) {
			examples[k] = new EXAMPLE();
			examples[k].x = new PATTERN();
			examples[k].y = new LABEL();
		}

		for (i = 0; i < n; i++)
		{
			/* find highest class label */
			if (num_first_classes < (target[i].first_class + 0.1))
			{
				num_first_classes = (int) (target[i].first_class + 0.1);
			}
		
		    if (num_second_classes < (target[i].second_class + 0.1))
		    {
			    num_second_classes = (int) (target[i].second_class + 0.1);
		    }
		}



		for (i = 0; i < n; i++) { /* copy docs over into new datastructure */
			examples[i].x.doc = docs[i];
		   // logger.info("example "+i+"  "+examples[i].x.doc.fvec.toString());
			//examples[i].y.class_index = (int) (target[i] + 0.1);
			examples[i].y.first_class = (int) (target[i].first_class + 0.1);
			examples[i].y.second_class = (int) (target[i].second_class + 0.1);
			// logger.info("the label is "+target[i]+" \n");
			examples[i].y.scores = null;
			//examples[i].y.num_classes = num_classes;
			examples[i].y.first_size = num_first_classes;
			examples[i].y.second_size = num_second_classes;

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
		try {
			/* Writes structural model sm to file file. */
			FileWriter fw = new FileWriter(new File(file));
			PrintWriter modelfl = new PrintWriter(fw);
			int j, i, sv_num;
			MODEL model = sm.svm_model.copyMODEL();
			SVECTOR v;

			modelfl.print("SVM-multiclass Version "
					+ svm_struct_common.INST_VERSION + "\n");
			//modelfl.print(sparm.num_classes + "# number of classes\n");
			modelfl.print(sparm.first_size + "# number of first classes\n");
			modelfl.print(sparm.second_size + "# number of second classes\n");
			modelfl.print(sparm.num_features + "# number of base features\n");
			modelfl.print(sparm.loss_function + " # loss function\n");
			modelfl.print(model.kernel_parm.kernel_type + " # kernel type\n");
			modelfl.print(model.kernel_parm.poly_degree
					+ " # kernel parameter -d \n");
			modelfl.print(model.kernel_parm.rbf_gamma
					+ " # kernel parameter -g \n");
			modelfl.print(model.kernel_parm.coef_lin
					+ " # kernel parameter -s \n");
			modelfl.print(model.kernel_parm.coef_const
					+ " # kernel parameter -r \n");
			modelfl.print(model.kernel_parm.custom
					+ " # kernel parameter -u \n");
			modelfl.print(model.totwords + " # highest feature index \n");
			modelfl.print(model.totdoc + " # number of training documents \n");
            logger.info("sv_num here: "+model.sv_num);
			sv_num = 1;
			for (i = 1; i < model.sv_num; i++) {
				for (v = model.supvec[i].fvec; v != null; v = v.next)
					sv_num++;
			}
			modelfl.print(sv_num + " # number of support vectors plus 1 \n");
			modelfl.print(model.b
					+ " # threshold b, each following line is a SV (starting with alpha*y)\n");
			logger.info("model.sv_num"+model.sv_num);
			for (i = 1; i < model.sv_num; i++) {
				for (v = model.supvec[i].fvec; v != null; v = v.next) {
					logger.info("model.alpha:"+model.alpha[i]);
					logger.info("v.factor:"+v.factor);
					modelfl.print((model.alpha[i] * v.factor) + " ");
					modelfl.print("qid:" + v.kernel_id + " ");
                  //  logger.info("i="+i+" v.length:"+v.words.length);
					for (j = 0; j < v.words.length; j++) {
						modelfl.print((v.words[j]).wnum + ":"
								+ (double) (v.words[j]).weight + " ");
					}
					if (v.userdefined != null)
						modelfl.print("#" + v.userdefined + "\n");
					else
						modelfl.print("#\n");
					/*
					 * NOTE: this could be made more efficient by summing the
					 * alpha's of identical vectors before writing them to the
					 * file.
					 */
				}
			}
			modelfl.close();

		} catch (Exception e) {
		}
		
	}

	@Override
	public STRUCTMODEL read_struct_model(String file, STRUCT_LEARN_PARM sparm) {
		/*
		 * Reads structural model sm from file file. This function is used only
		 * in the prediction module, not in the learning module.
		 */
		File modelfl;
		STRUCTMODEL sm = new STRUCTMODEL();
		int i, queryid, slackid;
		double costfactor;
		int max_sv, max_words, ll, wpos;
		String line, comment;
		WORD[] words;
		String version_buffer;
		MODEL model;
		svm_common sc=new svm_common();
		
		sc.nol_ll(file); /* scan size of model file */
		max_sv = sc.read_max_docs;
		max_words = sc.read_max_words_doc;
		//logger.info("max_sv:"+max_sv);
		//logger.info("max_words:"+max_words);
		max_words += 2;

		words = new WORD[max_words + 10];
		line = "";
		model = new MODEL();
		model.kernel_parm=new KERNEL_PARM();
		modelfl = new File(file);
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(modelfl);
			br = new BufferedReader(fr);
			line = br.readLine();
			//logger.info("line:"+line);
			version_buffer = SSO.afterStr(line, "SVM-multiclass Version")
					.trim();

			if (!(version_buffer.equals(svm_struct_common.INST_VERSION))) {
				System.err
						.println("Version of model-file does not match version of svm_struct_classify!");

			}
			line = br.readLine();
			//logger.info("line:"+line);
			sparm.first_size = Integer.parseInt(SSO.beforeStr(line, "#")
					.trim());
            logger.info("sparm.first_size:"+sparm.first_size);
			line = br.readLine();
			//logger.info("line:"+line);
			sparm.second_size = Integer.parseInt(SSO.beforeStr(line, "#")
					.trim());
            logger.info("sparm.second_size:"+sparm.second_size);
      
			line = br.readLine();
			//logger.info("line:"+line);
			sparm.num_features = Integer.parseInt(SSO.beforeStr(line, "#")
					.trim());
            logger.info("sparm.num_features:"+sparm.num_features);
			line = br.readLine();
			//logger.info("line:"+line);
			//System.out.println("line:"+line);
			sparm.loss_function = Integer.parseInt(SSO.beforeStr(line, "#")
					.trim());
            logger.info("sparm.loss_function:"+sparm.loss_function);
			
			line = br.readLine();
			//logger.info("line:"+line);
			//System.out.println("line:"+line);
			model.kernel_parm.kernel_type = Short.parseShort(SSO.beforeStr(
					line, "#").trim());
            logger.info("model.kernel_parm.kernel_type:"+model.kernel_parm.kernel_type);
			
			line = br.readLine();
			//logger.info("line:"+line);
			//System.out.println("line:"+line);
			model.kernel_parm.poly_degree = Integer.parseInt(SSO.beforeStr(
					line, "#").trim());
            logger.info("model.kernel_parm.poly_degree:"+model.kernel_parm.poly_degree);
			
			line = br.readLine();
			//logger.info("line:"+line);
			model.kernel_parm.rbf_gamma = Double.parseDouble(SSO.beforeStr(
					line, "#").trim());
            logger.info("model.kernel_parm.rbf_gammma:"+model.kernel_parm.rbf_gamma);
			
			
			line = br.readLine();
			//logger.info("line:"+line);
			model.kernel_parm.coef_lin = Double.parseDouble(SSO.beforeStr(line,
					"#").trim());
            logger.info("model.kernel_parm.coef_lin:"+model.kernel_parm.coef_lin);
            
			line = br.readLine();
			//logger.info("line:"+line);
			model.kernel_parm.coef_const = Double.parseDouble(SSO.beforeStr(
					line, "#").trim());
			logger.info("model.kernel_parm.coef_const:"+model.kernel_parm.coef_const);
			

			line = br.readLine();
			//logger.info("line:"+line);
			model.kernel_parm.custom = SSO.beforeStr(line, "#");
            logger.info("model.kernel_parm.custom:"+model.kernel_parm.custom);
			
			line = br.readLine();
			//logger.info("line:"+line);
			model.totwords = Integer.parseInt(SSO.beforeStr(line, "#").trim());
            logger.info("model.totwords:"+model.totwords);
			
			
			line = br.readLine();
			//logger.info("line:"+line);
			model.totdoc = Integer.parseInt(SSO.beforeStr(line, "#").trim());
            logger.info("model.totdoc:"+model.totdoc);
			
			line = br.readLine();
			//logger.info("line:"+line);
			model.sv_num = Integer.parseInt(SSO.beforeStr(line, "#").trim());
            logger.info("model.sv_num:"+model.sv_num);
			
			
			line = br.readLine();
			//logger.info("line:"+line);
			model.b = Double.parseDouble(SSO.beforeStr(line, "#").trim());
            logger.info("model.b:"+model.b);
			
			
			model.supvec = new DOC[model.sv_num];
			model.alpha = new double[model.sv_num];
			model.index = null;
			model.lin_weights = null;
			
			for (i = 1; i < model.sv_num; i++) {
				line = br.readLine();
				//logger.info("i="+i+" line:"+line);
				sc.parse_document(line, max_words);
				model.alpha[i] = sc.read_doc_label;
				queryid = sc.read_queryid;
				slackid = sc.read_slackid;
				costfactor = sc.read_costfactor;
				wpos = sc.read_wpos;
				comment = sc.read_comment;
                words=sc.read_words;
                System.out.println("words:"+words.length);
                System.out.println("queryid:"+queryid);
				model.supvec[i] = sc.create_example(-1, 0, 0, 0.0,
						sc.create_svector(words, comment, 1.0));
				model.supvec[i].fvec.kernel_id = queryid;
				//logger.info("read supvec["+i+"]:"+model.supvec[i].fvec.toString());
			}
			fr.close();
			br.close();

			if (svm_common.verbosity >= 1) {
				System.out.println(" (" + (model.sv_num - 1)
						+ " support vectors read) ");
			}
			//logger.info("kernel type here:"+model.kernel_parm.kernel_type);
			sm.svm_model = model;
			sm.sizePsi = model.totwords;
			sm.w = null;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		System.out.println("reading done");
		return (sm);
	}

	@Override
	public LABEL classify_struct_example(PATTERN x, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) {
		LABEL y = new LABEL();
		DOC doc;
		int first_class, bestfirst = -1;
		int second_class, bestsecond = -1;

		int  j;
		boolean first = true;
		double score=0.0, bestscore = -1;
		WORD[] words;

		int ci=0;
		doc = x.doc.copyDoc();
		y.scores = new double[posslabels.length ];
		y.first_size=sparm.first_size;
		y.second_size=sparm.second_size;
		//y.num_classes = sparm.num_classes;
		words = doc.fvec.words;

		
		for (j = 0; j <words.length; j++) {
			if (words[j].wnum > sparm.num_features) {
				//System.out.println(doc.fvec.words[j].wnum+" is set to 0");
				return null;
				//words[j].wnum = 0;
			}
		}
	
		for (ci = 0; ci < posslabels.length; ci++) {
			y.first_class = posslabels[ci].first_class;
			y.second_class = posslabels[ci].second_class;
	
			
			doc.fvec = psi(x, y, sm, sparm);
	
			score = sc.classify_example(sm.svm_model, doc);	
			y.scores[ci] = score;
			if ((bestscore < score) || first) {
				bestscore = score;
				bestfirst = y.first_class;
				bestsecond = y.second_class;
				first = false;
			}
		}
      
		y.first_class = bestfirst;
		y.second_class = bestsecond;

		
		return y;
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
		for(int i=0;i<label.length;i++)
		{
			label[i]=new LABEL();
		}
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
				//System.out.println(line);
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
		logger.info("dnum:"+dnum);
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
		if ((seg_arr.length < 2) || (seg_arr[0].indexOf("#") > -1)) {
			return null;
		}
		read_first_label = Double.parseDouble(seg_arr[0]);
		read_second_label = Double.parseDouble(seg_arr[1]);
		
		String wstr = "";
		String pstr = "";
		String sstr = "";
		for (int i = 2; i < seg_arr.length; i++) {
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
				//if(svmconfig.model_type==0)
				//{
					read_words[wpos].wnum= read_words[wpos].wnum;
				//}
				//else{
				//	read_words[wpos].wnum= read_words[wpos].wnum+1;
				//}
				
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
	public void readPossLabels()
	{
		try{
		BufferedReader br=new BufferedReader(new FileReader("genlabeldict_mul.txt"));
		ArrayList<String> pls=new ArrayList<String>();
		String line="";
		while((line=br.readLine())!=null)
		{
			if(SSO.tioe(line))
			{
				continue;
			}
			line=line.trim();
			pls.add(line);
		}
		logger.info("pls.size:"+pls.size());
		posslabels=new LABEL[pls.size()];
		
		LABEL tlabel=new LABEL();
		String[] fields=null;
		String labelStr="";
		String[] labels=null;
		for(int i=0;i<pls.size();i++)
		{
			line=pls.get(i);
			fields=line.split("\001");
			if(fields.length!=2)
			{
				continue;
			}
			
			labelStr=fields[0];
			if(SSO.tioe(labelStr))
			{
				continue;
			}
			labelStr=labelStr.trim();
			labels=labelStr.split("\\|");
			if(labels.length!=2)
			{
				continue;
			}
			tlabel=new LABEL();
			tlabel.first_class=Integer.parseInt(labels[0]);
			tlabel.second_class=Integer.parseInt(labels[1]);
			posslabels[i]=tlabel;
		}
		
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
