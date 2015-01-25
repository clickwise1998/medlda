package cn.clickwise.classify.reform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


import cn.clickwise.math.random.SeedRandom;
import cn.clickwise.str.basic.SSO;
import cn.clickwise.time.utils.TimeOpera;

public class svm_common {

	
	public static final String VERSION = "V6.20";
	public static final String VERSION_DATE = "14.08.08";

	public static final short LINEAR = 0;
	public static final short POLY = 1;
	public static final short RBF = 2;
	public static final short SIGMOID = 3;
	public static final short CUSTOM = 4;
	public static final short GRAM = 5;

	public static final short CLASSIFICATION = 1;
	public static final short REGRESSION = 2;
	public static final short RANKING = 3;
	public static final short OPTIMIZATION = 4;

	public static final int MAXSHRINK = 50000;

	public static final String INST_NAME = "Multi-Class SVM";

	public static final String INST_VERSION = "V2.20";

	public static final String INST_VERSION_DATE = "14.08.08";

	/**
	 * default precision for solving the optimization problem
	 */
	public static final double c = 0.1;

	/**
	 * default loss rescaling method: 1=slack_rescaling, 2=margin_rescaling
	 */
	public static final short DEFAULT_RESCALING = 2;

	/**
	 * default loss function:
	 */
	public static final int DEFAULT_LOSS_FCT = 0;

	/**
	 * default optimization algorithm to use:
	 */
	//public static final int DEFAULT_ALG_TYPE = 4;

	/**
	 * store Psi(x,y) once instead of recomputing it every time:
	 */
	public static final int USE_FYCACHE = 0;

	/**
	 * decide whether to evaluate sum before storing vectors in constraint
	 * cache: 0 = NO, 1 = YES (best, if sparse vectors and long vector lists), 2
	 * = YES (best, if short vector lists), 3 = YES (best, if dense vectors and
	 * long vector lists)
	 */
	public static final short COMPACT_CACHED_VECTORS = 2;

	/**
	 * minimum absolute value below which values in sparse vectors are rounded
	 * to zero. Values are stored in the FVAL type defined in svm_common.h
	 * RECOMMENDATION: assuming you use FVAL=float, use 10E-15 if
	 * COMPACT_CACHED_VECTORS is 1 10E-10 if COMPACT_CACHED_VECTORS is 2 or 3
	 */
	public static final double COMPACT_ROUNDING_THRESH = 10E-15;

	public  int kernel_cache_statistic = 0;
	public static int verbosity = 0;

	public  int read_totdocs;
	public  int read_totwords;
	public  int read_max_docs;
	public  int read_max_words_doc;

	public  double read_doc_label;
	public  int read_queryid;
	public  int read_slackid;
	public  double read_costfactor;
	public  int read_wpos;
	public  String read_comment;

	public  WORD[] read_words;
	public  double[] read_target = null;

	public  int progress_n;
	
	
	private static Logger logger = Logger.getLogger(svm_common.class);

	public static SVECTOR create_svector(WORD[] words, String userdefined,
			double factor) {
		SVECTOR vec;
		int fnum, i;

		fnum = 0;

		vec = new SVECTOR();
		vec.words = new WORD[words.length];

		for (i = 0; i < words.length; i++) {
			vec.words[i] = words[i];
		}

		vec.twonorm_sq = -1;

		if (userdefined != null) {
			vec.userdefined = userdefined;
		} else {
			vec.userdefined = null;
		}

		vec.kernel_id = 0;
		vec.next = null;
		vec.factor = factor;
		return vec;
	}

	public static DOC create_example(int docnum, int queryid, int slackid,
			double costfactor, SVECTOR fvec) {

		DOC example = new DOC();

		example.docnum = docnum;
		example.kernelid = docnum;
		example.queryid = queryid;
		example.slackid = slackid;
		example.costfactor = costfactor;

		example.fvec = fvec;

		return example;
	}

	public  double kernel(KERNEL_PARM kernel_parm, DOC a, DOC b) {
		// System.out.println("in kernel");
		double sum = 0;
		SVECTOR fa, fb;
		if (kernel_parm.kernel_type == GRAM) {
			// System.out.println("kernel_type:" + GRAM);
			if ((a.kernelid >= 0) && (b.kernelid >= 0)) {

				return kernel_parm.gram_matrix.element[Math.max(a.kernelid,
						b.kernelid)][Math.min(a.kernelid, b.kernelid)];
			} else {
				return 0;
			}
		}
		// System.out.println("fa pro");
		for (fa = a.fvec; fa != null; fa = fa.next) {
			for (fb = b.fvec; fb != null; fb = fb.next) {

				if (fa.kernel_id == fb.kernel_id) {
					// if (sum > 0)
					// System.out.println("sum:" + sum);
					sum += fa.factor * fb.factor
							* single_kernel(kernel_parm, fa, fb);
				}
			}
		}

		return sum;
	}

	public  double single_kernel(KERNEL_PARM kernel_parm, SVECTOR a,
			SVECTOR b) {
		kernel_cache_statistic++;

		switch (kernel_parm.kernel_type) {
		case LINEAR:
			// System.out.println("liner kernel y");
			return sprod_ss(a, b);
		case POLY:
			return Math.pow(kernel_parm.coef_lin * sprod_ss(a, b)
					+ kernel_parm.coef_const, kernel_parm.poly_degree);
		case RBF:
			if (a.twonorm_sq < 0) {
				a.twonorm_sq = sprod_ss(a, a);
			} else if (b.twonorm_sq < 0) {
				b.twonorm_sq = sprod_ss(b, b);
			}
			return Math.exp(-kernel_parm.rbf_gamma
					* (a.twonorm_sq - 2 * sprod_ss(a, b) + b.twonorm_sq));
		case SIGMOID:
			return Math.tanh(kernel_parm.coef_lin * sprod_ss(a, b)
					+ kernel_parm.coef_const);
		case CUSTOM:
			return kernel.custom_kernel(kernel_parm, a, b);
		default:
			System.out.println("Error: Unknown kernel function");
			System.exit(1);

		}

		return 0;
	}

	public static double sprod_ss(SVECTOR a, SVECTOR b) {
		double sum = 0;
		WORD[] ai, bj;
		ai = a.words;
		bj = b.words;

		int i = 0;
		int j = 0;
		// logger.info("a:"+a.toString());
		// logger.info("b:"+b.toString());
		// logger.info("ai:"+ai.length);
		// logger.info("bj:"+bj.length);
		while ((i < ai.length) && (j < bj.length)) {
			if (ai[i] == null || bj[j] == null) {
				break;
			}
			// logger.info("i:"+i+"  j:"+j);
			if (ai[i].wnum > bj[j].wnum) {
				j++;
			} else if (ai[i].wnum < bj[j].wnum) {
				i++;
			} else {

				sum += ai[i].weight * bj[j].weight;
				i++;
				j++;
			}
		}

		return sum;
	}

	public static void clear_nvector(double[] vec, int n) {
		int i;
		for (i = 0; i <= n; i++) {
			vec[i] = 0;
		}
	}

	public static double[] create_nvector(int n) {
		double[] vector;
		vector = new double[n + 1];
		return vector;
	}

	public static void add_vector_ns(double[] vec_n, SVECTOR vec_s,
			double faktor) {
		WORD[] ai;
			
		if(vec_s==null)
		{
		  return;	
		}
		
		ai = vec_s.words;
		
		if(ai==null)
		{
		  return;	
		}
		
		
		for (int i = 0; i < ai.length; i++) {
			if (ai[i] != null) {
				vec_n[ai[i].wnum] += (faktor * ai[i].weight);
				//vec_n[ai[i].wnum]=WeightsUpdate.sum(vec_n[ai[i].wnum], WeightsUpdate.mul(faktor, ai[i].weight));
			} else {
				continue;
			}
		}
	}

	public static double sprod_ns(double[] vec_n, SVECTOR vec_s) {
		double sum = 0;
		WORD[] ai;
		// logger.info("vec_s:"+vec_s.toString());
		ai = vec_s.words;
		// logger.info("ai.length:"+ai.length);

		for (int i = 0; i < ai.length; i++) {
			// logger.info("i:"+i+" ai[i].wnum:"+ai[i].wnum);
			if (ai[i] != null) {
				
				sum += (vec_n[ai[i].wnum] * ai[i].weight);
				//sum = WeightsUpdate.sum(sum, WeightsUpdate.mul(vec_n[ai[i].wnum], ai[i].weight));
			} else {
				continue;
			}
		}

		return sum;
	}

	public static void mult_vector_ns(double[] vec_n, SVECTOR vec_s,
			double faktor) {
		WORD[] ai;
		ai = vec_s.words;
		for (int i = 0; i < ai.length; i++) {
			if (ai[i] == null) {
				continue;
			}
			vec_n[ai[i].wnum] *= (faktor * ai[i].weight);
		}

	}

	public static double get_runtime() {
		int c = (int) TimeOpera.getCurrentTimeLong();
		double hc = 0;
		hc = ((double) c) * 10;
		return hc;
	}

	public  double model_length_s(MODEL model)
	/* compute length of weight vector */
	{
		int i, j;
		double sum = 0, alphai;
		DOC supveci;
		KERNEL_PARM kernel_parm = model.kernel_parm;

		for (i = 1; i < model.sv_num; i++) {
			alphai = model.alpha[i];
			supveci = model.supvec[i];
			for (j = 1; j < model.sv_num; j++) {
				sum += alphai * model.alpha[j]
						* kernel(kernel_parm, supveci, model.supvec[j]);
			}
		}
		return (Math.sqrt(sum));
	}

	public static void set_learning_defaults(LEARN_PARM learn_parm,
			KERNEL_PARM kernel_parm) {
		learn_parm.type = CLASSIFICATION;
		learn_parm.predfile = "trans_predictions";
		learn_parm.alphafile = "";
		learn_parm.biased_hyperplane = 1;
		learn_parm.sharedslack = 0;
		learn_parm.remove_inconsistent = 0;
		learn_parm.skip_final_opt_check = 0;
		learn_parm.svm_maxqpsize = 10;
		//learn_parm.svm_maxqpsize = 100;
		learn_parm.svm_newvarsinqp = 0;
		learn_parm.svm_iter_to_shrink = -9999;
		//learn_parm.maxiter = 100000;
		learn_parm.maxiter = 100000;
		
		learn_parm.kernel_cache_size = 40;
		//learn_parm.kernel_cache_size = 400;
		learn_parm.svm_c = 0.0;
		learn_parm.eps = 0.1;
		learn_parm.transduction_posratio = -1.0;
		learn_parm.svm_costratio = 1.0;
		learn_parm.svm_costratio_unlab = 1.0;
		learn_parm.svm_unlabbound = 1E-5;
		learn_parm.epsilon_crit = 0.001;
		learn_parm.epsilon_a = 1E-15;
		//learn_parm.epsilon_a = 1E-5;
		learn_parm.compute_loo = 0;
		learn_parm.rho = 1.0;
		learn_parm.xa_depth = 0;
		kernel_parm.kernel_type = LINEAR;
		kernel_parm.poly_degree = 3;
		kernel_parm.rbf_gamma = 1.0;
		kernel_parm.coef_lin = 1;
		kernel_parm.coef_const = 1;
		kernel_parm.custom = "empty";
	}

	public  DOC[] read_documents(String docfile, double[] label) {
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

		if (verbosity >= 1) {
			System.out.println("Scanning examples...");
		}

		nol_ll(docfile); /* scan size of input file */
		read_max_words_doc += 2;
		read_max_docs += 2;
		if (verbosity >= 1) {
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
		label = new double[read_max_docs]; /* target values */
		// System.out.println("docs length:"+docs.length);
		words = new WORD[read_max_words_doc + 10];
		for (int j = 0; j < words.length; j++) {
			words[j] = new WORD();
			words[j].wnum = 0;
			words[j].weight = 0;
		}
		if (verbosity >= 1) {
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
				//logger.info("model type in rd:"+svmconfig.model_type);
				if(svmconfig.model_type!=1)
				{
					label[dnum] = read_doc_label;
				}
				else
				{
					label[dnum] = read_doc_label+1;
				}
				
				/* printf("docnum=%ld: Class=%f ",dnum,doc_label); */
				if (read_doc_label > 0)
					dpos++;
				if (read_doc_label < 0)
					dneg++;
				if (read_doc_label == 0)
					dunlab++;
				if ((read_wpos > 1)
						&& ((words[read_wpos - 2]).wnum > read_totwords))
					read_totwords = words[read_wpos - 2].wnum;

				docs[dnum] = create_example(dnum, read_queryid, read_slackid,
						read_costfactor,
						create_svector(words, read_comment, 1.0));
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
		if (verbosity >= 1) {
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
		if ((seg_arr.length < 1) || (seg_arr[0].indexOf("#") > -1)) {
			return null;
		}
		read_doc_label = Double.parseDouble(seg_arr[0]);

		String wstr = "";
		String pstr = "";
		String sstr = "";
		for (int i = 1; i < seg_arr.length; i++) {
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
				//logger.info("model type in pd:"+svmconfig.model_type);
				if(svmconfig.model_type!=1)
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

	public WORD[] parse_big_document(String line, int max_words_doc) {
		int wpos = 0, pos = 0;
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

		Scanner sc = new Scanner(line);
		String token = "";
		String pstr = "";
		String sstr = "";
		read_doc_label = Double.parseDouble(sc.next());

		while ((token = sc.next()) != null) {
			if (token.indexOf(":") < 0) {
				continue;
			}
			pstr = token.substring(0, token.indexOf(":"));
			sstr = token.substring(token.indexOf(":") + 1, token.length());
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
				read_words[wpos].weight = Double.parseDouble(sstr);
				wpos++;
			}
		}

		read_words[wpos].wnum = 0;
		read_wpos = wpos + 1;

		/*
		 * read_queryid = 0; read_slackid = 0; read_costfactor = 1;
		 * 
		 * pos = 0; read_comment = ""; String dline = ""; //
		 * logger.info("lline:"+line); if (line.indexOf("#") > 0) { read_comment
		 * = line.substring(line.indexOf("#") + 1, line.length()); dline =
		 * line.substring(0, line.indexOf("#")); } else { dline = line; }
		 * 
		 * dline = dline.trim(); wpos = 0; logger.info("dline:"+dline); String[]
		 * seg_arr = dline.split(" ");
		 * System.out.println("seg_arr.length:"+seg_arr.length); if
		 * ((seg_arr.length < 1)||(seg_arr[0].indexOf("#")>-1)) { return null; }
		 * read_doc_label = Double.parseDouble(seg_arr[0]);
		 * 
		 * String wstr = ""; String pstr = ""; String sstr = ""; for (int i = 1;
		 * i < seg_arr.length; i++) { wstr = seg_arr[i].trim(); if
		 * (wstr.indexOf(":") < 0) { continue; } pstr = wstr.substring(0,
		 * wstr.indexOf(":")); sstr = wstr.substring(wstr.indexOf(":") + 1,
		 * wstr.length()); pstr = pstr.trim(); sstr = sstr.trim(); if
		 * (pstr.equals("qid")) { read_queryid = Integer.parseInt(sstr); } else
		 * if (pstr.equals("sid")) { read_slackid = Integer.parseInt(sstr); }
		 * else if (pstr.equals("cost")) { read_costfactor =
		 * Double.parseDouble(sstr); } else if (Pattern.matches("[\\d]+", pstr))
		 * { read_words[wpos].wnum = Integer.parseInt(pstr);
		 * read_words[wpos].weight = Double.parseDouble(sstr); wpos++; } }
		 * 
		 * read_words[wpos].wnum = 0; read_wpos = wpos + 1; //
		 * System.out.println("wpos:"+read_wpos);
		 */

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

	public void nol_big_ll(String input_file) {

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
		int wcount = 0;
		String token = "";
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				//System.out.println("line:" + line);
				temp_docs++;

				Scanner scan = new Scanner(line);
				wcount = 0;
				try {
					while ((token = scan.next()) != null) {
						System.out.println(token);
						wcount++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (wcount > temp_words) {
					temp_words = wcount;
				}
				System.out.println("wcount:" + wcount);
			}

			read_max_docs = temp_docs;
			read_max_words_doc = temp_words;
			// System.out.println("read_max_docs:" + read_max_docs);
			// System.out.println("read_max_words_doc:" + read_max_words_doc);

			fr.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

	}

	public static double[] read_alphas(String alphafile, int totdoc)
	/*
	 * reads the alpha vector from a file as written by the write_alphas
	 * function
	 */
	{
		FileReader fr = null;
		BufferedReader br = null;
		double[] alpha = null;
		try {
			fr = new FileReader(new File(alphafile));
			br = new BufferedReader(fr);

			alpha = new double[totdoc];
			int dnum = 0;
			String line = "";

			while ((line = br.readLine()) != null) {
				alpha[dnum] = Double.parseDouble(line);
				dnum++;
			}
			fr.close();
			br.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return (alpha);
	}

	/***************************** IO routines ***************************/

	public  void write_model(String modelfile, MODEL model) {
		FileWriter fw = null;
		PrintWriter pw = null;

		try {
			fw = new FileWriter(new File(modelfile));
			pw = new PrintWriter(fw);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		int j, i, sv_num;
		SVECTOR v;
		MODEL compact_model = null;

		if (verbosity >= 1) {
			System.out.println("Writing model file...");
		}

		/* Replace SV with single weight vector */
		if (false && (model.kernel_parm.kernel_type == LINEAR)) {
			if (verbosity >= 1) {
				System.out.println("(compacting...");
			}
			compact_model = compact_linear_model(model);
			model = compact_model;
			if (verbosity >= 1) {
				System.out.println("done)");
			}
		}

		pw.println("SVM-light Version " + VERSION);
		pw.println(model.kernel_parm.kernel_type + " # kernel type");
		pw.println(model.kernel_parm.poly_degree + " # kernel parameter -d ");
		pw.println(model.kernel_parm.rbf_gamma + " # kernel parameter -g ");
		pw.println(model.kernel_parm.coef_lin + " # kernel parameter -s ");
		pw.println(model.kernel_parm.coef_const + " # kernel parameter -r ");
		pw.println(model.kernel_parm.custom + "# kernel parameter -u ");
		pw.println(model.totwords + " # highest feature index ");
		pw.println(model.totdoc + " # number of training documents ");

		sv_num = 1;
		for (i = 1; i < model.sv_num; i++) {
			for (v = model.supvec[i].fvec; v != null; v = v.next)
				sv_num++;
		}
		pw.println(sv_num + " # number of support vectors plus 1 \n");
		pw.println(model.b
				+ " # threshold b, each following line is a SV (starting with alpha*y)\n");

		for (i = 1; i < model.sv_num; i++) {
			for (v = model.supvec[i].fvec; v != null; v = v.next) {
				pw.print(model.alpha[i] * v.factor + " ");
				for (j = 0; (v.words[j]).wnum != 0; j++) {
					pw.print((int) (v.words[j]).wnum + ":"
							+ (double) (v.words[j]).weight + " ");
				}
				if (v.userdefined != null)
					pw.print("#" + v.userdefined + "\n");
				else
					pw.print("#\n");
				/*
				 * NOTE: this could be made more efficient by summing the
				 * alpha's of identical vectors before writing them to the file.
				 */
			}
		}

		if (verbosity >= 1) {
			System.out.println("done\n");
		}
	}

	public static MODEL compact_linear_model(MODEL model)
	/*
	 * Makes a copy of model where the support vectors are replaced with a
	 * single linear weight vector.
	 */
	/* NOTE: It adds the linear weight vector also to newmodel->lin_weights */
	/* WARNING: This is correct only for linear models! */
	{
		MODEL newmodel;
		newmodel = new MODEL();
		newmodel = model.copyMODEL();
		add_weight_vector_to_linear_model(newmodel);
		newmodel.supvec = new DOC[2];
		newmodel.alpha = new double[2];
		newmodel.index = null; /* index is not copied */
		newmodel.supvec[0] = null;
		newmodel.alpha[0] = 0.0;
		newmodel.supvec[1] = create_example(
				-1,
				0,
				0,
				0,
				create_svector_n(newmodel.lin_weights, newmodel.totwords, null,
						1.0));
		newmodel.alpha[1] = 1.0;
		newmodel.sv_num = 2;

		return (newmodel);
	}

	public static void add_weight_vector_to_linear_model(MODEL model)
	/* compute weight vector in linear case and add to model */
	{
		int i;
		SVECTOR f;
		//logger.info("model.totwords:" + model.totwords);
		model.lin_weights = create_nvector(model.totwords);
		clear_nvector(model.lin_weights, model.totwords);
		for (i = 1; i < model.sv_num; i++) {
			for (f = (model.supvec[i]).fvec; f != null; f = f.next)
				add_vector_ns(model.lin_weights, f, f.factor * model.alpha[i]);
		}
	}

	public static SVECTOR create_svector_n(double[] nonsparsevec,
			int maxfeatnum, String userdefined, double factor) {
		return (create_svector_n_r(nonsparsevec, maxfeatnum, userdefined,
				factor, 0));
	}

	public static SVECTOR create_svector_n_r(double[] nonsparsevec,
			int maxfeatnum, String userdefined, double factor,
			double min_non_zero) {
		// logger.info("begin create_svector_n_r");
		SVECTOR vec;
		int fnum, i;

		fnum = 0;
		for (i = 1; i <= maxfeatnum; i++)
			if ((nonsparsevec[i] < -min_non_zero)
					|| (nonsparsevec[i] > min_non_zero))
				fnum++;

		vec = new SVECTOR();
		vec.words = new WORD[fnum + 1];
		for (int vi = 0; vi < vec.words.length; vi++) {
			vec.words[vi] = new WORD();
		}

		fnum = 0;
		for (i = 1; i <= maxfeatnum; i++) {
			if ((nonsparsevec[i] < -min_non_zero)
					|| (nonsparsevec[i] > min_non_zero)) {
				vec.words[fnum].wnum = i;
				vec.words[fnum].weight = nonsparsevec[i];
				fnum++;
			}
		}
		vec.words[fnum].wnum = 0;
		vec.twonorm_sq = -1;

		if (userdefined != null) {
			vec.userdefined = userdefined;
		} else
			vec.userdefined = null;

		vec.kernel_id = 0;
		vec.next = null;
		vec.factor = factor;
		// logger.info("end create_svector_n_r");
		return (vec);
	}

	public static void copyright_notice() {
		System.out
				.println("\nCopyright: Thorsten Joachims, thorsten@joachims.org");
		System.out
				.println("This software is available for non-commercial use only. It must not");
		System.out
				.println("be modified and distributed without prior permission of the author.");
		System.out
				.println("The author is not responsible for implications from the use of this");
		System.out.println("software.\n\n");
	}

	public static boolean check_learning_parms(LEARN_PARM learn_parm,
			KERNEL_PARM kernel_parm) {
		System.out.println("check_learning_parms");
		if ((learn_parm.skip_final_opt_check != 0)
				&& (kernel_parm.kernel_type == LINEAR)) {
			System.out
					.println("\nIt does not make sense to skip the final optimality check for linear kernels.\n\n");
			learn_parm.skip_final_opt_check = 0;
		}
		if ((learn_parm.skip_final_opt_check != 0)
				&& (learn_parm.remove_inconsistent != 0)) {
			System.out
					.println("\nIt is necessary to do the final optimality check when removing inconsistent \nexamples.\n");
			return false;
		}
		if ((learn_parm.svm_maxqpsize < 2)) {
			System.out
					.println("\nMaximum size of QP-subproblems not in valid range: "
							+ learn_parm.svm_maxqpsize + " [2..]\n");
			return false;
		}
		if ((learn_parm.svm_maxqpsize < learn_parm.svm_newvarsinqp)) {
			System.out.println("\nMaximum size of QP-subproblems ["
					+ learn_parm.svm_maxqpsize
					+ "] must be larger than the number of\n");
			System.out.println("new variables [" + learn_parm.svm_newvarsinqp
					+ "] entering the working set in each iteration.\n");
			return false;
		}
		if (learn_parm.svm_iter_to_shrink < 1) {
			System.out
					.println("\nMaximum number of iterations for shrinking not in valid range: "
							+ learn_parm.svm_iter_to_shrink + " [1,..]\n");
			return false;
		}
		if (learn_parm.svm_c < 0) {
			System.out
					.println("\nThe C parameter must be greater than zero!\n\n");
			return false;
		}
		if (learn_parm.transduction_posratio > 1) {
			System.out
					.println("\nThe fraction of unlabeled examples to classify as positives must\n");
			System.out.println("be less than 1.0 !!!\n\n");
			return false;
		}
		if (learn_parm.svm_costratio <= 0) {
			System.out
					.println("\nThe COSTRATIO parameter must be greater than zero!\n\n");
			return false;
		}
		if (learn_parm.epsilon_crit <= 0) {
			System.out
					.println("\nThe epsilon parameter must be greater than zero!\n\n");
			return false;
		}
		if (learn_parm.rho < 0) {
			System.out
					.println("\nThe parameter rho for xi/alpha-estimates and leave-one-out pruning must\n");
			System.out
					.println("be greater than zero (typically 1.0 or 2.0, see T. Joachims, Estimating the\n");
			System.out
					.println("Generalization Performance of an SVM Efficiently, ICML, 2000.)!\n\n");
			return false;
		}
		if ((learn_parm.xa_depth < 0) || (learn_parm.xa_depth > 100)) {
			System.out
					.println("\nThe parameter depth for ext. xi/alpha-estimates must be in [0..100] (zero\n");
			System.out
					.println("for switching to the conventional xa/estimates described in T. Joachims,\n");
			System.out
					.println("Estimating the Generalization Performance of an SVM Efficiently, ICML, 2000.)\n");
		}
		System.out.println("true");
		return true;
	}

	public static SVECTOR shift_s(SVECTOR a, int shift) {
		SVECTOR vec;
		//WORD[] sum;
		WORD[] sumi;
		WORD[] ai;
		int veclength;
		String userdefined = "";
		// logger.info("shift:"+shift);
		ai = new WORD[a.words.length];
		for (int k = 0; k < ai.length; k++) {
			ai[k] = svm_common.copy_word(a.words[k]);
		}
		// ai = a.words;

		/*
		 * String wwinfo=""; for(int k=0;k<a.words.length;k++) {
		 * wwinfo=wwinfo+a.words[k].wnum+":"+a.words[k].weight+" "; }
		 * logger.info("wwwinfo:"+wwinfo);
		 */
		veclength = ai.length;
		sumi = new WORD[veclength];
		for (int i = 0; i < ai.length; i++) {
			sumi[i] = svm_common.copy_word(ai[i]);
			sumi[i].wnum = ai[i].wnum + shift;
			// logger.info("ai.wnum:"+ai[i].wnum+" sumi wnum:"+sumi[i].wnum);
		}

		if (a.userdefined != null) {

		}

		/*
		 * String wwinfo=""; for(int k=0;k<sumi.length;k++) {
		 * wwinfo=wwinfo+sumi[k].wnum+":"+sumi[k].weight+" "; }
		 * logger.info("wwwinfo:"+wwinfo);
		 */

		vec = svm_common.create_svector_shallow(sumi, userdefined, a.factor);
		if(ai!=null)
		{
			for(int i=0;i<ai.length;i++)
			{
				ai[i]=null;
			}
			ai=null;
		}
		
		if(sumi!=null)
		{
			for(int i=0;i<sumi.length;i++)
			{
				sumi[i]=null;
			}
			sumi=null;
		}
		// logger.info("vec in sv:"+vec.toString());
		return vec;
	}

	public static SVECTOR create_svector_shallow(WORD[] words,
			String userdefined, double factor) {
		SVECTOR vec;
		vec = new SVECTOR();
		// logger.info("words.length:"+words.length);
		vec.words = new WORD[words.length];
		// logger.info("words.length:"+words.length);
		for (int i = 0; i < words.length; i++) {
			if (words[i] != null) {
				vec.words[i] = svm_common.copy_word(words[i]);
			} else {
				vec.words[i] = null;
			}
		}
		// logger.info("vec in create:"+vec.toString());
		vec.twonorm_sq = -1;
		vec.userdefined = userdefined;
		vec.kernel_id = 0;
		vec.next = null;
		vec.factor = factor;

		return vec;
	}

	public static SVECTOR add_list_ss(SVECTOR a) {
		return (add_list_ss_r(a, 0));
	}

	public static SVECTOR add_list_ss_r(SVECTOR a, double min_non_zero)
	/*
	 * computes the linear combination of the SVECTOR list weighted by the
	 * factor of each SVECTOR
	 */
	{
		SVECTOR oldsum, sum, f;
		WORD[] empty = new WORD[2];
		for (int k = 0; k < 2; k++) {
			empty[k] = new WORD();
		}

		if (a == null) {
			empty[0].wnum = 0;
			sum = create_svector(empty, null, 1.0);
			// logger.info("sum a:"+sum.toString());
		} else if ((a != null) && (a.next == null)) {
			sum = smult_s(a, a.factor);
			// logger.info("sum b:"+sum.toString());
		} else {
			// logger.info("a.factor:"+a.factor+"a.next.factor:"+a.next.factor);
			// logger.info("a words:"+a.toString());
			// logger.info("b words:"+a.next.toString());
			sum = multadd_ss_r(a, a.next, a.factor, a.next.factor, min_non_zero);
			// logger.info("sum c:"+sum.toLongString());
			int ii = 0;
			for (f = a.next.next; f != null; f = f.next) {
				oldsum = sum;
				sum = multadd_ss_r(oldsum, f, 1.0, f.factor, min_non_zero);
				// logger.info("ii="+ii+"sum d:"+sum.toString());
				ii++;
				oldsum=null;
			}
		}
		return (sum);
	}

	public static SVECTOR smult_s(SVECTOR a, double factor)
	/* scale sparse vector a by factor */
	{
		SVECTOR vec;
		WORD[] sum, sumi;
		WORD[] ai;
		int veclength;
		String userdefined = null;

		ai = a.words;
		veclength = ai.length;

		sum = new WORD[veclength];
		sumi = new WORD[veclength];
		ArrayList<WORD> wordlist = new ArrayList<WORD>();
		ai = a.words;
		WORD temp_word = null;
		for (int i = 0; i < veclength; i++) {
			temp_word = ai[i];
			temp_word.weight *= factor;
			if (temp_word.weight != 0) {
				wordlist.add(temp_word);
			}
		}

		sumi = new WORD[wordlist.size()];

		for (int i = 0; i < wordlist.size(); i++) {
			sumi[i] = wordlist.get(i);
		}

		if (a.userdefined != null) {
			userdefined = a.userdefined;
		}

		vec = create_svector_shallow(sumi, userdefined, 1.0);
		return (vec);
	}

	/**
	 * compute fa*a+fb*b of two sparse vectors Note: SVECTOR lists are not
	 * followed, but only the first SVECTOR is used 这个方法有错
	 * 
	 * @param a
	 * @param b
	 * @param fa
	 * @param fb
	 * @param min_non_zero
	 * @return
	 */
	public static SVECTOR multadd_ss_r(SVECTOR a, SVECTOR b, double fa,
			double fb, double min_non_zero) {
		SVECTOR vec;
		WORD[] sum, sumi;
		WORD[] ai, bj;
		int veclength;
		double weight;

		ai = a.words;
		bj = b.words;
		veclength = 0;
		int i = 0;
		int j = 0;

		while (i < ai.length && j < bj.length) {
			if (ai[i].wnum > bj[j].wnum) {
				veclength++;
				j++;
			} else if (ai[i].wnum < bj[j].wnum) {
				veclength++;
				i++;
			} else {
				veclength++;
				i++;
				j++;
			}
		}

		while (j < bj.length) {
			veclength++;
			j++;
		}

		while (i < ai.length) {
			veclength++;
			i++;
		}
		veclength++;

		sumi = new WORD[veclength];
		// sumi = sum;
		ai = a.words;
		bj = b.words;
		i = 0;
		j = 0;
		int s = 0;
		while (i < ai.length && j < bj.length) {
			if (ai[i].wnum > bj[j].wnum) {
				sumi[s] = bj[j];
				sumi[s].weight *= fb;
				if (sumi[s].weight != 0)
					// logger.info(" add 1 s="+s+" "+sumi[s].wnum+":"+sumi[s].weight);
					s++;
				j++;
			} else if (ai[i].wnum < bj[j].wnum) {
				sumi[s] = ai[i];
				sumi[s].weight *= fa;
				if (sumi[s].weight != 0)
					// logger.info(" add 2 s="+s+" "+sumi[s].wnum+":"+sumi[s].weight);
					s++;
				i++;
			} else {
				weight = fa * (double) ai[i].weight + fb
						* (double) bj[j].weight;
				if ((weight < -min_non_zero) || (weight > min_non_zero)) {
					/////******medlda*****************
					if(sumi[s]==null||ai[i]==null)
					{
						continue;
					}
				/////*********************************
					sumi[s].wnum = ai[i].wnum;
					sumi[s].weight = weight;
					if (sumi[s].weight != 0)
						// logger.info("add 3 s="+s+" "+sumi[s].wnum+":"+sumi[s].weight);
						s++;
				}
				i++;
				j++;
			}
		}
		while (j < bj.length) {
			sumi[s] = bj[j];
			sumi[s].weight *= fb;
			if (sumi[s].weight != 0)
				// logger.info("add 4 s="+s+" "+sumi[s].wnum+":"+sumi[s].weight);
				s++;
			j++;
		}
		while (i < ai.length) {
			sumi[s] = ai[i];
			sumi[s].weight *= fa;
			if (sumi[s].weight != 0)
				// logger.info("add 5 s="+s+" "+sumi[s].wnum+":"+sumi[s].weight);
				s++;
			i++;
		}

		if (true) { /* potentially this wastes some memory, but saves malloc'ing */
			/*
			 * String winfo=""; for(int wi=0;wi<sumi.length;wi++) {
			 * if(sumi[wi]==null) { continue; } if(sumi[wi].weight!=0) {
			 * winfo+=(sumi[wi].wnum+":"+sumi[wi].weight+" "); } }
			 */
			// logger.info("sumi dd:"+winfo);
			vec = create_svector_shallow(sumi, null, 1.0);
			// logger.info("vec ee:"+vec.toLongString());
		} else { /* this is more memory efficient */
			vec = create_svector(sumi, null, 1.0);

		}
		return (vec);
	}

	public  double classify_example(MODEL model, DOC ex)
	/* classifies one example */
	{
		int i;
		double dist;

		if ((model.kernel_parm.kernel_type == LINEAR)
				&& (model.lin_weights != null)) {
			// printf("model kernel type is LINEAR and lin_weights \n");
			return (classify_example_linear(model, ex));
		}
		dist = 0;
		for (i = 1; i < model.sv_num; i++) {
			dist += kernel(model.kernel_parm, model.supvec[i], ex)
					* model.alpha[i];
		}
		return (dist - model.b);
	}

	public static double classify_example_linear(MODEL model, DOC ex)
	/* classifies example for linear kernel */

	/* important: the model must have the linear weight vector computed */
	/* use: add_weight_vector_to_linear_model(&model); */

	/* important: the feature numbers in the example to classify must */
	/* not be larger than the weight vector! */
	{
		double sum = 0;
		SVECTOR f;

		for (f = ex.fvec; f != null; f = f.next) {
			// logger.info("lin_weights.length:"+size_arr(model.lin_weights)+" f:"+size_svector(f)+" "+f.toString());
			sum += f.factor * sprod_ns(model.lin_weights, f);
		}
		return (sum - model.b);
	}

	public static SVECTOR copy_svector(SVECTOR vec) {
		SVECTOR newvec = null;
		if (vec != null) {
			newvec = create_svector(vec.words, vec.userdefined, vec.factor);
			newvec.kernel_id = vec.kernel_id;
			newvec.next = copy_svector(vec.next);
		}
		return (newvec);
	}

	public static void append_svector_list(SVECTOR a, SVECTOR b)
	/* appends SVECTOR b to the end of SVECTOR a. */
	{
		SVECTOR f;

		for (f = a; f.next != null; f = f.next)
			; /* find end of first vector list */
		f.next = b; /* append the two vector lists */
	}

	public static SVECTOR add_ss(SVECTOR a, SVECTOR b)
	/* compute the sum a+b of two sparse vectors */
	/*
	 * Note: SVECTOR lists are not followed, but only the first SVECTOR is used
	 */
	{
		return (multadd_ss_r(a, b, 1.0, 1.0, 0));
	}

	public static MODEL copy_model(MODEL model) {
		MODEL newmodel;
		int i;

		newmodel = new MODEL();
		newmodel.supvec = new DOC[model.sv_num];
		newmodel.alpha = new double[model.sv_num];

		newmodel.index = null; /* index is not copied */
		newmodel.supvec[0] = null;// 为什么第0个设置为 null?
		newmodel.alpha[0] = 0;

		// logger.info("model.sv_num:"+model.sv_num);
		for (i = 1; i < model.sv_num; i++) {

			newmodel.alpha[i] = model.alpha[i];
			newmodel.supvec[i] = svm_common.create_example(
					model.supvec[i].docnum, model.supvec[i].queryid, 0,
					model.supvec[i].costfactor,
					svm_common.copy_svector(model.supvec[i].fvec));
			// logger.info("newmodel.supvec:"+i+" "+newmodel.supvec[i].fvec.words.length);
		}
		if (model.lin_weights != null) {
			newmodel.lin_weights = new double[model.totwords + 1];
			for (i = 0; i < model.totwords + 1; i++)
				newmodel.lin_weights[i] = model.lin_weights[i];
		}

		newmodel.kernel_parm = model.kernel_parm.copyKERNEL_PARM();

		return (newmodel);
	}

	public static MATRIX create_matrix(int n, int m)
	/* create matrix with n rows and m colums */
	{
		int i;
		MATRIX matrix;

		matrix = new MATRIX();
		matrix.n = n;
		matrix.m = m;
		matrix.element = new double[n][m];

		return (matrix);
	}

	public static SVECTOR add_list_sort_ss_r(SVECTOR a, double min_non_zero)
	/*
	 * Like add_list_sort_ss(SVECTOR *a), but rounds values smaller than
	 * min_non_zero to zero.
	 */
	{
		SVECTOR sum, f;
		WORD[] empty = new WORD[2];
		WORD[] ai, concat, concati, concat_read, concat_write;
		int length, i;
		double weight;

		int cwi = 0;
		int cri = 0;

		if (a != null) {
			/* count number or entries over all vectors */
			length = 0;
			for (f = a; f != null; f = f.next) {
				ai = f.words;
				for (int k = 0; k > ai.length; k++) {
					length++;
				}
			}

			/* write all entries into one long array and sort by feature number */
			concat = new WORD[length + 1];
			int s = 0;
			for (f = a; f != null; f = f.next) {
				ai = f.words;
				for (int k = 0; k < ai.length; k++) {
					concat[s] = ai[k];
					concat[s].weight *= f.factor;
					s++;
				}
			}

			concat = SortWordArr.sort_array(concat);

			concat_read = copy_word_arr(1, concat);
			concat_write = copy_word_arr(0, concat);

			for (i = 0; (i < length - 1)
					&& (concat_write[cwi].wnum != concat_read[cri].wnum); i++) {
				cwi++;
				cri++;
			}

			weight = concat_write[cwi].weight;
			for (i = i; (i < length - 1); i++) {
				if (concat_write[cwi].wnum == concat_read[cri].wnum) {
					weight += (double) concat_read[cri].weight;
					cri++;
				} else {
					if ((weight > min_non_zero) || (weight < -min_non_zero)) {
						concat_write[cwi].weight = weight;
						cwi++;
					}
					concat_write[cwi] = svm_common.copy_word(concat_read[cri]);// ?是否正确
					weight = concat_write[cwi].weight;
					cri++;
				}
			}

			if ((length > 0)
					&& ((weight > min_non_zero) || (weight < -min_non_zero))) {
				concat_write[cwi].weight = weight;
				cwi++;
			}

			if (true) { /* this wastes some memory, but saves malloc'ing */
				sum = create_svector_shallow(concat, null, 1.0);
			} else { /* this is more memory efficient */
				sum = create_svector(concat, null, 1.0);
				concat=null;
			}
		} else {
			empty[0].wnum = 0;
			sum = create_svector(empty, null, 1.0);
		}
		return (sum);
	}

	public static WORD[] copy_word_arr(int start_index, WORD[] oarr) {
		WORD[] warr = new WORD[oarr.length - start_index];
		if (start_index > (oarr.length - 1)) {
			return null;
		}

		for (int i = start_index; i < oarr.length; i++) {
			warr[i - start_index] = svm_common.copy_word(oarr[i]);
		}

		return warr;
	}

	public static int[] random_order(int n)
	/* creates an array of the integers [0..n-1] in random order */
	{
		int[] randarray = new int[n];
		RANDPAIR[] randpair = new RANDPAIR[n];
		int i;

		for (i = 0; i < n; i++) {
			randpair[i].val = i;
			randpair[i].sort = SeedRandom.getRandom();
		}

		SortWordArr.sort_double_array(randpair);
		for (i = 0; i < n; i++) {
			randarray[i] = randpair[i].val;
		}

		randpair=null;
		return (randarray);
	}

	public static void add_list_n_ns(double[] vec_n, SVECTOR vec_s,
			double faktor) {
		SVECTOR f;
		for (f = vec_s; f != null; f = f.next)
			add_vector_ns(vec_n, f, f.factor * faktor);
	}

	public  void print_percent_progress(int maximum, int percentperdot,
			String symbol)
	/*
	 * every time this function gets called, progress is incremented. It prints
	 * symbol every percentperdot calls, assuming that maximum is the max number
	 * of calls
	 */
	{
		if ((percentperdot * ((double) progress_n - 1) / maximum) != (percentperdot
				* ((double) progress_n) / maximum)) {
			// logger.info(symbol);
		}
		progress_n++;
	}

	public static void mult_svector_list(SVECTOR a, double factor)
	/* multiplies the factor of each element in vector list with factor */
	{
		SVECTOR f;

		for (f = a; f != null; f = f.next)
			f.factor *= factor;
	}

	public static SVECTOR add_list_ns_r(SVECTOR a, double min_non_zero)
	/*
	 * computes the linear combination of the SVECTOR list weighted by the
	 * factor of each SVECTOR. assumes that the number of features is small
	 * compared to the number of elements in the list
	 */
	{
		SVECTOR vec, f;
		WORD[] ai;
		int totwords;
		double[] sum;

		/* find max feature number */
		totwords = 0;
		for (f = a; f != null; f = f.next) {
			ai = f.words;
			for (int k = 0; k < ai.length; k++) {
				if (totwords < ai[k].wnum)
					totwords = ai[k].wnum;
			}
		}
		sum = create_nvector(totwords);

		clear_nvector(sum, totwords);
		for (f = a; f != null; f = f.next)
			add_vector_ns(sum, f, f.factor);

		vec = create_svector_n_r(sum, totwords, null, 1.0, min_non_zero);
        sum=null;
        
		return (vec);
	}

	public static MATRIX realloc_matrix(MATRIX matrix, int n, int m)
	/*
	 * extends/shrinks matrix to n rows and m colums. Not that added elements
	 * are not initialized.
	 */
	{
		int i;

		if (matrix == null)
			return (create_matrix(n, m));

		for(i=n;i<matrix.n;i++)
		{
			matrix.element[i]=null;
		}
		matrix.element = realloc_matrix_row(matrix.element, n, matrix.m, m);

		matrix.n = n;
		matrix.m = m;
		return (matrix);
	}

	public static double[][] realloc_matrix_row(double[][] ddarr, int n,
			int old_m, int m) {
		double[][] ndarr = new double[n][m];
		for (int i = 0; i < ddarr.length; i++) {
			for (int j = 0; j < old_m; j++) {
				ndarr[i][j] = ddarr[i][j];
			}
			for (int j = old_m; j < m; j++) {
				ndarr[i][j] = 0;
			}
		}

		for (int i = ddarr.length; i < n; i++) {
			for (int j = 0; j < m; j++) {
				ndarr[i][j] = 0;
			}
		}

		//free memory 
		ddarr=null;
		
		return ndarr;

	}

	public  double model_length_n(MODEL model)
	/* compute length of weight vector */
	{
		int i, totwords = model.totwords + 1;
		double sum;
		double[] weight_n;
		SVECTOR weight;

		if (model.kernel_parm.kernel_type != svm_common.LINEAR) {
			logger.info("ERROR: model_length_n applies only to linear kernel!\n");
		}
		weight_n = create_nvector(totwords);
		clear_nvector(weight_n, totwords);
		for (i = 1; i < model.sv_num; i++)
			add_list_n_ns(weight_n, model.supvec[i].fvec, model.alpha[i]);
		weight = create_svector_n(weight_n, totwords, null, 1.0);
		sum = sprod_ss(weight, weight);
        weight_n=null;
        
        //reform free weight
        if(weight!=null)
        {
        	weight.destroy();
        	weight=null;
        }
        //free_svector(weight);
		return (Math.sqrt(sum));
	}

	public void ffree_svector(SVECTOR vec)
	{
	  SVECTOR next;
	  while(vec!=null) {
	    if(vec.words!=null)
	      vec.words=null;
	    if(vec.userdefined!=null)
	      vec.userdefined=null;
	    next=vec.next;
	    vec=null;
	    vec=next;
	  }
	}
	public  MODEL read_model(String modelfile) {

		MODEL model = new MODEL();
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(modelfile);
			br = new BufferedReader(fr);

			int i, queryid, slackid;
			double costfactor;
			int max_sv, max_words, wpos;
			String line, comment;
			WORD[] words;
			String version_buffer;

			if (verbosity >= 1) {
				logger.info("Reading model...");
			}

			nol_ll(modelfile);
			max_words = read_max_words_doc;
			max_words += 2;
			line = br.readLine();
			version_buffer = SSO.afterStr(line, "SVM-multiclass Version")
					.trim();
			model.kernel_parm = new KERNEL_PARM();

			line = br.readLine();
			model.kernel_parm.kernel_type = Short.parseShort(SSO.beforeStr(
					line, "#"));

			line = br.readLine();
			model.kernel_parm.poly_degree = Integer.parseInt(SSO.beforeStr(
					line, "#"));

			line = br.readLine();
			model.kernel_parm.rbf_gamma = Double.parseDouble(SSO.beforeStr(
					line, "#"));

			line = br.readLine();
			model.kernel_parm.coef_lin = Double.parseDouble(SSO.beforeStr(line,
					"#"));

			line = br.readLine();
			model.kernel_parm.coef_const = Double.parseDouble(SSO.beforeStr(
					line, "#"));

			line = br.readLine();
			model.kernel_parm.custom = line;

			line = br.readLine();
			model.totwords = Integer.parseInt(SSO.beforeStr(line, "#"));

			line = br.readLine();
			model.totdoc = Integer.parseInt(SSO.beforeStr(line, "#"));

			line = br.readLine();
			model.sv_num = Integer.parseInt(SSO.beforeStr(line, "#"));
			line = br.readLine();
			line = br.readLine();
			model.b = Double.parseDouble(SSO.beforeStr(line, "#"));
			line = br.readLine();
			System.out.println("b:" + model.b);
			model.supvec = new DOC[model.sv_num];
			model.alpha = new double[model.sv_num];
			model.index = null;
			model.lin_weights = null;

			for (i = 1; i < model.sv_num; i++) {
				line = br.readLine();
				// logger.info("i:"+i+" "+line);
				parse_document(line, max_words);
				model.alpha[i] = read_doc_label;
				queryid = read_queryid;
				slackid = read_slackid;
				costfactor = read_costfactor;
				wpos = read_wpos;
				comment = read_comment;
				words = read_words;
				model.supvec[i] = svm_common.create_example(-1, 0, 0, 0.0,
						svm_common.create_svector(words, comment, 1.0));
				model.supvec[i].fvec.kernel_id = queryid;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return model;
	}

	public static int size_svector(SVECTOR fvec) {
		int len = 0;
		WORD[] words = fvec.words;
		int i = 0;
		return words.length;

	}

	public static int size_arr(double[] arr) {
		int len = 0;
		len = arr.length;
		return len;
	}
	
	public static void pprintLvec(DOC[] supvec){
		String str="supvec abstract:\n";
		if(supvec==null)
		{
			return;
		}
		for(int i=0;i<supvec.length;i++)
		{
			if(supvec[i]==null)
			{
				continue;
			}
		   str+=("i="+i+" "+supvec[i].lvecString()+"\n");	
		}
		logger.info(str);
	}
	
	  public static WORD copy_word(WORD w)
	  {
		  WORD nw=new WORD();
		  nw.weight=w.weight;
		  nw.wnum=w.wnum;
		  return nw;
	  }


		protected void finalize_n() throws Throwable
		{
			try{
				/*
				if(read_words!=null)
				{
					for(int i=0;i<read_words.length;i++)
					{
						read_words[i]=null;
					}
					read_words=null;
					
				}
				
				if(read_target!=null)
				{
					read_target=null;
				}
				*/
				
			}
			finally
			{
				super.finalize();
			}
		}
}
