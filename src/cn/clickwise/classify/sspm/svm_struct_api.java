package cn.clickwise.classify.sspm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import cn.clickwise.str.basic.SSO;

public abstract class svm_struct_api {

	 static Logger logger = Logger.getLogger(svm_struct_api.class);

	public svm_common sc=null;
	
	public svm_struct_api()
	{
		sc=new svm_common();
	}
	
	public abstract void init_struct_model(SAMPLE sample, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm, LEARN_PARM lparm, KERNEL_PARM kparm);
	

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
	public abstract SVECTOR psi(PATTERN x, LABEL y, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm);
	

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

	public  abstract LABEL find_most_violated_constraint_slackrescaling(PATTERN x,
			LABEL y, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm);
	

	public abstract LABEL find_most_violated_constraint_marginrescaling(
			PATTERN x, LABEL y, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm);

	/**
	 * loss for correct label y and predicted label ybar. The loss for
	 * y==ybar has to be zero. sparm->loss_function is set with the -l
	 * option.
	 */
	public abstract double loss(LABEL y, LABEL ybar, STRUCT_LEARN_PARM sparm) ;
	

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

	public  abstract SAMPLE read_struct_examples(String file,
			STRUCT_LEARN_PARM sparm) ;
	

	public abstract void write_struct_model(String file, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) ;
	

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

	public abstract STRUCTMODEL read_struct_model(String file,
			STRUCT_LEARN_PARM sparm) ;

	/**
	 * Finds the label yhat for pattern x that scores the highest according to
	 * the linear evaluation function in sm, especially the weights sm.w. The
	 * returned label is taken as the prediction of sm for the pattern x. The
	 * weights correspond to the features defined by psi() and range from index
	 * 1 to index sm->sizePsi. If the function cannot find a label, it shall
	 * return an empty label as recognized by the function empty_label(y).
	 */
	public abstract LABEL classify_struct_example(PATTERN x, STRUCTMODEL sm,
			STRUCT_LEARN_PARM sparm) ;
	

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
