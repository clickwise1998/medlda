package cn.clickwise.classify.svm_struct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import cn.clickwise.str.basic.SSO;

public class svm_struct_api {

	private static Logger logger = Logger.getLogger(svm_struct_api.class);

	public static void init_struct_model(SAMPLE sample, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm, LEARN_PARM lparm, KERNEL_PARM kparm) {
		int i, totwords = 0;
		WORD w;
		WORD[] temp_words;

		sparm.num_classes = 1;
		for (i = 0; i < sample.n; i++)
			/* find highest class label */
			if (sparm.num_classes < ((sample.examples[i].y.class_index) + 0.1))
				sparm.num_classes = (int) (sample.examples[i].y.class_index + 0.1);
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
					+ " features " + sparm.num_classes + " classes\n");
		}
		// logger.info("sparm.num_features:"+sparm.num_features);

		sm.sizePsi = sparm.num_features * sparm.num_classes;
		// logger.info("sm.sizePsi:"+sm.sizePsi);
		if (svm_struct_common.struct_verbosity >= 2) {
			System.out.println("Size of Phi: " + sm.sizePsi + "\n");
		}
	}

	/**
	 * Initializes the optimization problem. Typically, you do not need to
	 * change this function, since you want to start with an empty set of
	 * constraints. However, if for example you have constraints that certain
	 * weights need to be positive, you might put that in here. The constraints
	 * are represented as lhs[i]*w >= rhs[i]. lhs is an array of feature
	 * vectors, rhs is an array of doubles. m is the number of constraints. The
	 * function returns the initial set of constraints.
	 * 
	 * @param sample
	 * @param sm
	 * @param sparm
	 * @return
	 */
	public static CONSTSET init_struct_constraints(SAMPLE sample,
			STRUCTMODEL sm, STRUCT_LEARN_PARM sparm) {

		CONSTSET c = new CONSTSET();
		int sizePsi = sm.sizePsi;
		int i;
		WORD[] words = new WORD[2];

		if (true) { /* normal case: start with empty set of constraints */
			c.lhs = null;
			c.rhs = null;
			c.m = 0;
		}

		return (c);
	}

	/**
	 * Returns a feature vector describing the match between pattern x and label
	 * y. The feature vector is returned as an SVECTOR (i.e. pairs
	 * <featurenumber:featurevalue>), where the last pair has featurenumber 0 as
	 * a terminator. Featurenumbers start with 1 and end with sizePsi. This
	 * feature vector determines the linear evaluation function that is used to
	 * score labels. There will be one weight in sm.w for each feature. Note
	 * that psi has to match find_most_violated_constraint_???(x, y, sm) and
	 * vice versa. In particular, find_most_violated_constraint_???(x, y, sm)
	 * finds that ybar!=y that maximizes psi(x,ybar,sm)*sm.w (where * is the
	 * inner vector product) and the appropriate function of the loss.
	 * 
	 * @param x
	 * @param y
	 * @param sm
	 * @param sparm
	 * @return
	 */
	public static SVECTOR psi(PATTERN x, LABEL y, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) {
		SVECTOR fvec;
		/*
		 * String wwinfo=""; for(int k=0;k<x.doc.fvec.words.length;k++) {
		 * wwinfo=
		 * wwinfo+x.doc.fvec.words[k].wnum+":"+x.doc.fvec.words[k].weight+" "; }
		 * logger.info("wwwwinfo:"+wwinfo);
		 */
		fvec = svm_common.shift_s(x.doc.fvec, (y.class_index - 1)
				* sparm.num_features);
		// logger.info("fvec psi:"+fvec.toString());
		fvec.kernel_id = y.class_index;
		return fvec;
	}

	public static boolean finalize_iteration(double ceps,
			int cached_constraint, SAMPLE sample, STRUCTMODEL sm,
			CONSTSET cset, double[] alpha, STRUCT_LEARN_PARM sparm) {
		/*
		 * This function is called just before the end of each cutting plane
		 * iteration. ceps is the amount by which the most violated constraint
		 * found in the current iteration was violated. cached_constraint is
		 * true if the added constraint was constructed from the cache. If the
		 * return value is FALSE, then the algorithm is allowed to terminate. If
		 * it is TRUE, the algorithm will keep iterating even if the desired
		 * precision sparm->epsilon is already reached.
		 */
		return false;
	}

	public static LABEL find_most_violated_constraint_slackrescaling(PATTERN x,
			LABEL y, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm) {
		/*
		 * Finds the label ybar for pattern x that that is responsible for the
		 * most violated constraint for the slack rescaling formulation. It has
		 * to take into account the scoring function in sm, especially the
		 * weights sm.w, as well as the loss function. The weights in sm.w
		 * correspond to the features defined by psi() and range from index 1 to
		 * index sm->sizePsi. Most simple is the case of the zero/one loss
		 * function. For the zero/one loss, this function should return the
		 * highest scoring label ybar, if ybar is unequal y; if it is equal to
		 * the correct label y, then the function shall return the second
		 * highest scoring label. If the function cannot find a label, it shall
		 * return an empty label as recognized by the function empty_label(y).
		 */
		LABEL ybar = new LABEL();
		DOC doc;
		int ci;
		int bestclass = -1;
		int first = 1;
		double score, score_y, score_ybar, bestscore = -1;

		/*
		 * NOTE: This function could be made much more efficient by not always
		 * computing a new PSI vector.
		 */
		doc = (x.doc);
		doc.fvec = psi(x, y, sm, sparm);
		score_y = svm_common.classify_example(sm.svm_model, doc);

		ybar.scores = null;
		ybar.num_classes = sparm.num_classes;
		// logger.info("ybar.num_classes:"+ybar.num_classes);

		for (ci = 1; ci <= sparm.num_classes; ci++) {
			// logger.info("ci="+ci);
			ybar.class_index = ci;
			// logger.info("before psi");
			doc.fvec = psi(x, ybar, sm, sparm);
			// logger.info("after psi");
			score_ybar = svm_common.classify_example(sm.svm_model, doc);
			// logger.info("after classify_example");
			score = loss(y, ybar, sparm) * (1.0 - score_y + score_ybar);
			if ((bestscore < score) || (first != 0)) {
				bestscore = score;
				bestclass = ci;
				first = 0;
			}

		}
		if (bestclass == -1)
			logger.debug("ERROR: Only one class\n");
		ybar.class_index = bestclass;
		// logger.info("ybar.class_index:"+ybar.class_index);
		if (svm_struct_common.struct_verbosity >= 3)
			logger.debug("[" + bestclass + ":" + bestscore + "] ");
		return (ybar);
	}

	public static LABEL find_most_violated_constraint_marginrescaling(
			PATTERN x, LABEL y, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm) {

		/*
		 * Finds the label ybar for pattern x that that is responsible for the
		 * most violated constraint for the margin rescaling formulation. It has
		 * to take into account the scoring function in sm, especially the
		 * weights sm.w, as well as the loss function. The weights in sm.w
		 * correspond to the features defined by psi() and range from index 1 to
		 * index sm->sizePsi. Most simple is the case of the zero/one loss
		 * function. For the zero/one loss, this function should return the
		 * highest scoring label ybar, if ybar is unequal y; if it is equal to
		 * the correct label y, then the function shall return the second
		 * highest scoring label. If the function cannot find a label, it shall
		 * return an empty label as recognized by the function empty_label(y).
		 */
		LABEL ybar = new LABEL();
		DOC doc;
		int ci = 0;
		int bestclass = -1;
		int first = 1;
		double score, bestscore = -1;

		/*
		 * NOTE: This function could be made much more efficient by not always
		 * computing a new PSI vector.
		 */
		doc = x.doc.copyDoc();
        //logger.info("in find_most_violated_constraint_marginrescaling");
        //logger.info(doc.fvec.toString());
        
		ybar.scores = null;
		ybar.num_classes = sparm.num_classes;

		// logger.info("ybar_num_classes:"+ybar.num_classes);
		// String winfo="";
		for (ci = 1; ci <= sparm.num_classes; ci++) {
			// logger.info("ci="+ci);
			ybar.class_index = ci;
			
			// logger.info("before psi");
			doc.fvec = psi(x, ybar, sm, sparm);
			//logger.info("doc fvec:"+doc.fvec.toString());
			score = svm_common.classify_example(sm.svm_model, doc);
			// logger.info("ybar.class_index:"+ybar.class_index+"  score:"+score);
			score += loss(y, ybar, sparm);
			if ((bestscore < score) || (first != 0)) {
				bestscore = score;
				bestclass = ci;
				first = 0;
			}
		}
		if (bestclass == -1)
			logger.debug("ERROR: Only one class\n");
		ybar.class_index = bestclass;
		// logger.info("ybar_class_index:"+ybar.class_index);
		if (svm_struct_common.struct_verbosity >= 3) {
			logger.info("[%" + bestclass + ":" + bestscore + "] ");
		}
		//logger.info("bestscore:"+bestscore);
		return (ybar);
	}

	/**
	 * loss for correct label y and predicted label ybar. The loss for
	 * y==ybar has to be zero. sparm->loss_function is set with the -l
	 * option.
	 */
	public static double loss(LABEL y, LABEL ybar, STRUCT_LEARN_PARM sparm) {

		//logger.info("sparm.loss_function:"+sparm.loss_function);
		if (sparm.loss_function == 0) { /* type 0 loss: 0/1 loss */
			if (y.class_index == ybar.class_index)
				return (0);
			else
				return (100);
		}
		if (sparm.loss_function == 1) { /* type 1 loss: squared difference */
			return ((y.class_index - ybar.class_index) * (y.class_index - ybar.class_index));
		} else {
			/*
			 * Put your code for different loss functions here. But then
			 * find_most_violated_constraint_???(x, y, sm) has to return the
			 * highest scoring label with the largest loss.
			 */
			logger.debug("Unkown loss function\n");
			System.exit(1);
		}

		return 10000;
	}

	/**
	 * Returns true, if y is an empty label. An empty label might be returned by
	 * find_most_violated_constraint_???(x, y, sm) if there is no incorrect
	 * label that can be found for x, or if it is unable to label x at all
	 * 
	 * @param y
	 * @return
	 */
	public static boolean empty_label(LABEL y) {
		return (y.class_index < 0.9);
	}

	public static void realloc(CONSTSET cset) {
		DOC[] olhs = cset.lhs;
		cset.lhs = new DOC[cset.m];
		for (int i = 0; i < (cset.m - 1); i++) {
			cset.lhs[i] = olhs[i];
		}
		cset.lhs[cset.m - 1] = new DOC();

	}

	public static void realsmallloc_lhs(CONSTSET cset) {
		DOC[] olhs = cset.lhs;
		cset.lhs = new DOC[cset.m];
		for (int i = 0; i < (cset.m); i++) {
			cset.lhs[i] = olhs[i];
		}
	}

	public static void realsmallloc_rhs(CONSTSET cset) {
		double[] orhs = cset.rhs;
		cset.rhs = new double[cset.m];
		for (int i = 0; i < (cset.m); i++) {
			cset.rhs[i] = orhs[i];
		}
	}

	public static void realloc_rhs(CONSTSET cset) {
		double[] orhs = cset.rhs;
		cset.rhs = new double[cset.m];
		for (int i = 0; i < (cset.m - 1); i++) {
			cset.rhs[i] = orhs[i];
		}
		cset.rhs[cset.m - 1] = 0;
	}

	public static double[] realloc_alpha(double[] alpha, int m) {
		double[] oalpha = alpha;
		alpha = new double[m];
		for (int i = 0; i < (m - 1); i++) {
			alpha[i] = oalpha[i];
		}
		alpha[m - 1] = 0;

		return alpha;
	}

	public static int[] realloc_alpha_list(int[] alpha_list, int m) {
		int[] oalpha_list = alpha_list;
		alpha_list = new int[m];
		for (int i = 0; i < (m - 1); i++) {
			alpha_list[i] = oalpha_list[i];
		}
		alpha_list[m - 1] = 0;

		return alpha_list;
	}

	public static void print_struct_learning_stats(SAMPLE sample,
			STRUCTMODEL sm, CONSTSET cset, double[] alpha,
			STRUCT_LEARN_PARM sparm) {
		/*
		 * This function is called after training and allows final touches to
		 * the model sm. But primarly it allows computing and printing any kind
		 * of statistic (e.g. training error) you might want.
		 */

		/* Replace SV with single weight vector */
		/*******************
		 * MODEL model=sm.svm_model.copyMODEL();
		 ******************/

		MODEL model = sm.svm_model;
		if (model.kernel_parm.kernel_type == svm_common.LINEAR) {
			if (svm_struct_common.struct_verbosity >= 1) {
				logger.info("Compacting linear model...");
			}

			sm.svm_model = svm_common.compact_linear_model(model);
			sm.w = sm.svm_model.lin_weights; /* short cut to weight vector */

			if (svm_struct_common.struct_verbosity >= 1) {
				logger.info("done\n");
			}
		}
	}

	public static void svm_struct_learn_api_init(String[] args) {
		/*
		 * Called in learning part before anything else is done to allow any
		 * initializations that might be necessary.
		 */
	}

	public static void print_struct_help() {
		/*
		 * Prints a help text that is appended to the common help text of
		 * svm_struct_learn.
		 */

		System.out.print("none\n\n");
		System.out.print("Based on multi-class SVM formulation described in:\n");
		System.out.print("K. Crammer and Y. Singer. On the Algorithmic Implementation of\n");
		System.out.print("Multi-class SVMs, JMLR, 2001.\n");
	}

	public static void parse_struct_parameters_classify(STRUCT_LEARN_PARM sparm) {
		/*
		 * Parses the command line parameters that start with -- for the
		 * classification module
		 */
		int i;

		for (i = 0; (i < sparm.custom_argc)
				&& ((sparm.custom_argv[i]).charAt(0) == '-'); i++) {
			switch ((sparm.custom_argv[i]).charAt(2)) {
			/* case 'x': i++; strcpy(xvalue,sparm->custom_argv[i]); break; */
			default:
				System.out.print("\nUnrecognized option "
						+ sparm.custom_argv[i] + "!\n\n");
				System.exit(0);
			}
		}
	}

	public static void parse_struct_parameters(STRUCT_LEARN_PARM sparm) {
		/* Parses the command line parameters that start with -- */
		int i;

		for (i = 0; (i < sparm.custom_argc)
				&& ((sparm.custom_argv[i]).charAt(0) == '-'); i++) {
			switch ((sparm.custom_argv[i]).charAt(2)) {
			case 'a':
				i++; /* strcpy(learn_parm->alphafile,argv[i]); */
				break;
			case 'e':
				i++; /* sparm->epsilon=atof(sparm->custom_argv[i]); */
				break;
			case 'k':
				i++; /* sparm->newconstretrain=atol(sparm->custom_argv[i]); */
				break;
			}
		}
	}

	public static SAMPLE read_struct_examples(String file,
			STRUCT_LEARN_PARM sparm) {
		/*
		 * Reads training examples and returns them in sample. The number of
		 * examples must be written into sample.n
		 */
		SAMPLE sample = new SAMPLE(); /* sample */
		EXAMPLE[] examples;
		int n; /* number of examples */
		DOC[] docs; /* examples in original SVM-light format */
		double[] target = null;
		int totwords, i, num_classes = 0;

		/* Using the read_documents function from SVM-light */
		docs = svm_common.read_documents(file, target);
	
		// logger.info("in read struct examples: docs.length:"+docs.length);
		target = svm_common.read_target;
		logger.info("target:"+target);
		totwords = svm_common.read_totwords;
		logger.info("totwords:"+totwords);
		n = svm_common.read_totdocs;
         logger.info("n:"+n);
        //logger.info("totwords:"+totwords);
		for(int k=0;k<docs.length;k++)
		{
			if(docs[k]==null||docs[k].fvec==null)
			{
				continue;
			}
			logger.info("test["+k+"]="+docs[k].fvec.toString());
		}
		examples = new EXAMPLE[n];
		for (int k = 0; k < n; k++) {
			examples[k] = new EXAMPLE();
			examples[k].x = new PATTERN();
			examples[k].y = new LABEL();
		}

		for (i = 0; i < n; i++)
			/* find highest class label */
			if (num_classes < (target[i] + 0.1))
				num_classes = (int) (target[i] + 0.1);
		for (i = 0; i < n; i++)
			/* make sure all class labels are positive */
			if (target[i] < 1) {
				logger.info("\nERROR: The class label '" + target[i]
						+ "' of example number " + (i + 1)
						+ " is not greater than '1'!\n");
				System.exit(1);
			}

		for (i = 0; i < n; i++) { /* copy docs over into new datastructure */
			examples[i].x.doc = docs[i];
		   // logger.info("example "+i+"  "+examples[i].x.doc.fvec.toString());
			examples[i].y.class_index = (int) (target[i] + 0.1);
			// logger.info("the label is "+target[i]+" \n");
			examples[i].y.scores = null;
			examples[i].y.num_classes = num_classes;
		}

		sample.n = n;
		sample.examples = examples;

		if (svm_struct_common.struct_verbosity >= 0)
			logger.info(" (" + sample.n + " examples) ");

		return (sample);
	}

	public static void write_struct_model(String file, STRUCTMODEL sm,
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
			modelfl.print(sparm.num_classes + "# number of classes\n");
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

	public static void svm_struct_learn_api_exit() {
		/*
		 * Called in learning part at the very end to allow any clean-up that
		 * might be necessary.
		 */
	}

	public static DOC[] reallocDOCS(DOC[] ods, int n) {

		DOC[] ndoc = new DOC[n];
		if (ods == null) {
			for (int i = 0; i < n; i++) {
				ndoc[i] = new DOC();
			}
			return ndoc;
		}
		for (int i = 0; i < ods.length; i++) {
			ndoc[i] = ods[i].copyDoc();
		}
		for (int i = ods.length; i < n; i++) {
			ndoc[i] = new DOC();
		}
		
		return ndoc;
	}

	public static double[] reallocDoubleArr(double[] arr, int nsize) {
		if (arr != null) {
			logger.info("double arr length:" + arr.length + " nsize:" + nsize);
		} else {
			logger.info("double arr length: null nsize:" + nsize);
		}
		double[] narr = new double[nsize];
		if (arr == null) {
			for (int ni = 0; ni < nsize; ni++) {
				narr[ni] = 0;
			}
			return narr;
		}

		if (nsize <= arr.length) {
			narr = new double[arr.length];
			for (int ni = 0; ni < nsize; ni++) {
				narr[ni] = arr[ni];
			}

			for (int ni = nsize; ni < arr.length; ni++) {
				narr[ni] = 0;
			}

			return narr;
		}

		for (int ni = 0; ni < arr.length; ni++) {
			narr[ni] = arr[ni];
		}
		for (int ni = arr.length; ni < nsize; ni++) {
			narr[ni] = 0;
		}

		return narr;
	}

	public static int[] reallocIntArr(int[] arr, int nsize) {
		if (arr != null) {
			logger.info("int arr length:" + arr.length + " nsize:" + nsize);
		} else {
			logger.info("int arr length: null nsize:" + nsize);
		}
		int[] narr = new int[nsize];
		if (arr == null) {
			for (int ni = 0; ni < nsize; ni++) {
				narr[ni] = 0;
			}
			return narr;
		}
		if (nsize <= arr.length) {
			narr = new int[arr.length];
			for (int ni = 0; ni < nsize; ni++) {
				narr[ni] = arr[ni];
			}
			for (int ni = nsize; ni < arr.length; ni++) {
				narr[ni] = 0;
			}
			return narr;
		}
		for (int ni = 0; ni < arr.length; ni++) {
			narr[ni] = arr[ni];
		}
		for (int ni = arr.length; ni < nsize; ni++) {
			narr[ni] = 0;
		}

		return narr;
	}

	public static String douarr2str(double[] arr) {
		String str = "";
		if (arr == null) {
			return "";
		}

		for (int i = 0; i < arr.length; i++) {
			str += (i + ":" + arr[i] + " ");
		}

		str = str.trim();
		return str;
	}

	public static String intarr2str(int[] arr) {
		String str = "";
		if (arr == null) {
			return "";
		}

		for (int i = 0; i < arr.length; i++) {
			str += (i + ":" + arr[i] + " ");
		}

		str = str.trim();
		return str;
	}

	public static void svm_struct_classify_api_init(int argc, String[] args) {

	}

	public static void print_struct_help_classify() {
		/*
		 * Prints a help text that is appended to the common help text of
		 * svm_struct_classify.
		 */
	}

	public static STRUCTMODEL read_struct_model(String file,
			STRUCT_LEARN_PARM sparm) {
		logger.info("in read struct model");
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
			sparm.num_classes = Integer.parseInt(SSO.beforeStr(line, "#")
					.trim());
            logger.info("sparm.num_classes:"+sparm.num_classes);
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

	/**
	 * Finds the label yhat for pattern x that scores the highest according to
	 * the linear evaluation function in sm, especially the weights sm.w. The
	 * returned label is taken as the prediction of sm for the pattern x. The
	 * weights correspond to the features defined by psi() and range from index
	 * 1 to index sm->sizePsi. If the function cannot find a label, it shall
	 * return an empty label as recognized by the function empty_label(y).
	 */
	public static LABEL classify_struct_example(PATTERN x, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) {
		LABEL y = new LABEL();
		DOC doc;
		int class_index, bestclass = -1, j;
		boolean first = true;
		double score=0.0, bestscore = -1;
		WORD[] words;

		doc = x.doc.copyDoc();
		y.scores = new double[sparm.num_classes + 1];
		y.num_classes = sparm.num_classes;
		words = doc.fvec.words;

		
		for (j = 0; j <words.length; j++) {
			if (words[j].wnum > sparm.num_features) {
				//System.out.println(doc.fvec.words[j].wnum+" is set to 0");
				return null;
				//words[j].wnum = 0;
			}
		}
	
		for (class_index = 1; class_index <= sparm.num_classes; class_index++) {
			y.class_index = class_index;
			doc.fvec = psi(x, y, sm, sparm);
	
			score = svm_common.classify_example(sm.svm_model, doc);	
			y.scores[class_index] = score;
			if ((bestscore < score) || first) {
				bestscore = score;
				bestclass = class_index;
				first = false;
			}
		}
      
		y.class_index = bestclass;
		return y;
	}

	public static void write_label(PrintWriter fp, LABEL y) {
		int i;
		fp.print(y.class_index + " ");
		if (y.scores != null) {
			for (i = 1; i < y.num_classes; i++) {
				fp.print(y.scores[i] + " ");
			}
		}
		fp.println();

	}

	/**
	 * This function allows you to accumlate statistic for how well the
	 * predicition matches the labeled example. It is called from
	 * svm_struct_classify. See also the function print_struct_testing_stats.
	 */
	public static void eval_prediction(int exnum, EXAMPLE ex, LABEL ypred,
			STRUCTMODEL sm, STRUCT_LEARN_PARM sparm, STRUCT_TEST_STATS teststats) {
		if (exnum == 0) { /*
						 * this is the first time the function is called. So
						 * initialize the teststats
						 */
		}
	}

	/**
	 * This function is called after making all test predictions in
	 * svm_struct_classify and allows computing and printing any kind of
	 * evaluation (e.g. precision/recall) you might want. You can use the
	 * function eval_prediction to accumulate the necessary statistics for each
	 * prediction.
	 */
	public static void print_struct_testing_stats(SAMPLE sample, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm, STRUCT_TEST_STATS teststats) {

	}

}
