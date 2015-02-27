package cn.clickwise.classify.reform;

import org.apache.log4j.Logger;

import cn.clickwise.time.utils.TimeOpera;

public class svm_struct_main {

	public static String trainfile; /* file with training examples */
	public static String modelfile; /* file for resulting classifier */
	public static int verbosity;
	public static int struct_verbosity;
	public static int alg_type;

	private static Logger logger = Logger.getLogger(svm_struct_main.class);

	public static void read_input_parameters(int argc, String argv[],
			STRUCT_LEARN_PARM struct_parm, LEARN_PARM learn_parm,
			KERNEL_PARM kernel_parm) {
		int i;
		String type;

		/* set default */
		alg_type = svm_struct_common.DEFAULT_ALG_TYPE;
		struct_parm.C = -0.01;
		struct_parm.slack_norm = 1;
		struct_parm.epsilon = svm_struct_common.DEFAULT_EPS;
		struct_parm.custom_argc = 0;
		struct_parm.loss_function = svm_struct_common.DEFAULT_LOSS_FCT;
		struct_parm.loss_type = svm_struct_common.DEFAULT_RESCALING;
		struct_parm.newconstretrain = 100;
		struct_parm.ccache_size = 5;
		struct_parm.batch_size = 100;

		modelfile = "svm_struct_model";
		learn_parm.predfile = "trans_predictions";
		learn_parm.alphafile = "";
		verbosity = 0;/* verbosity for svm_light */
		struct_verbosity = 1; /* verbosity for struct learning portion */
		learn_parm.biased_hyperplane = 1;
		learn_parm.remove_inconsistent = 0;
		learn_parm.skip_final_opt_check = 0;
		learn_parm.svm_maxqpsize = 10;
		learn_parm.svm_newvarsinqp = 0;
		learn_parm.svm_iter_to_shrink = -9999;
		learn_parm.maxiter = 100000;
		learn_parm.kernel_cache_size = 40;
		learn_parm.svm_c = 99999999; /* overridden by struct_parm->C */
		learn_parm.eps = 0.001; /* overridden by struct_parm->epsilon */
		learn_parm.transduction_posratio = -1.0;
		learn_parm.svm_costratio = 1.0;
		learn_parm.svm_costratio_unlab = 1.0;
		learn_parm.svm_unlabbound = 1E-5;
		learn_parm.epsilon_crit = 0.001;
		learn_parm.epsilon_a = 1E-10; /* changed from 1e-15 */
		learn_parm.compute_loo = 0;
		learn_parm.rho = 1.0;
		learn_parm.xa_depth = 0;
		kernel_parm.kernel_type = 0;
		kernel_parm.poly_degree = 3;
		kernel_parm.rbf_gamma = 1.0;
		kernel_parm.coef_lin = 1;
		kernel_parm.coef_const = 1;
		kernel_parm.custom = "empty";
		type = "c";

		
		for (i = 0; (i < argc) && ((argv[i].charAt(0)) == '-'); i++) {
			System.out.println("i:" + i + " " + argv[i]);
			switch ((argv[i].charAt(1))) {
			case '?':
				print_help();
				System.exit(0);
			case 'a':
				i++;
				learn_parm.alphafile = argv[i];
				break;
			case 'c':
				i++;
				struct_parm.C = Double.parseDouble(argv[i]);
				break;
			case 'p':
				i++;
				struct_parm.slack_norm = Integer.parseInt(argv[i]);
				break;
			case 'e':
				i++;
				struct_parm.epsilon = Double.parseDouble(argv[i]);
				break;
			case 'k':
				i++;
				struct_parm.newconstretrain = Integer.parseInt(argv[i]);
				break;
			case 'h':
				i++;
				learn_parm.svm_iter_to_shrink = Integer.parseInt(argv[i]);
				break;
			case '#':
				i++;
				learn_parm.maxiter = Integer.parseInt(argv[i]);
				break;
			case 'm':
				i++;
				learn_parm.kernel_cache_size = Integer.parseInt(argv[i]);
				break;
			case 'w':
				i++;
				alg_type = Integer.parseInt(argv[i]);
				break;
			case 'o':
				i++;
				struct_parm.loss_type = Integer.parseInt(argv[i]);
				break;
			case 'n':
				i++;
				learn_parm.svm_newvarsinqp = Integer.parseInt(argv[i]);
				break;
			case 'q':
				i++;
				learn_parm.svm_maxqpsize = Integer.parseInt(argv[i]);
				break;
			case 'l':
				i++;
				struct_parm.loss_function = Integer.parseInt(argv[i]);
				break;
			case 'f':
				i++;
				struct_parm.ccache_size = Integer.parseInt(argv[i]);
				break;
			case 'b':
				i++;
				struct_parm.batch_size = Double.parseDouble(argv[i]);
				break;
			case 't':
				i++;
				kernel_parm.kernel_type = (short) Integer.parseInt(argv[i]);
				break;
			case 'd':
				i++;
				kernel_parm.poly_degree = Integer.parseInt(argv[i]);
				break;
			case 'g':
				i++;
				kernel_parm.rbf_gamma = Double.parseDouble(argv[i]);
				break;
			case 's':
				i++;
				kernel_parm.coef_lin = Double.parseDouble(argv[i]);
				break;
			case 'r':
				i++;
				kernel_parm.coef_const = Double.parseDouble(argv[i]);
				break;
			case 'u':
				i++;
				kernel_parm.custom = argv[i];
				break;
			case '-':
				struct_parm.custom_argv[struct_parm.custom_argc++] = argv[i];
				i++;
				struct_parm.custom_argv[struct_parm.custom_argc++] = argv[i];
				break;
			case 'v':
				i++;
				struct_verbosity = Integer.parseInt(argv[i]);
				break;
			case 'y':
				i++;
				verbosity = Integer.parseInt(argv[i]);
				break;
			default:
				System.out.print("\nUnrecognized option " + argv[i] + "!\n\n");
				print_help();
				System.exit(0);
			}
		}

		System.out.println("c is:" + struct_parm.C);
		if (i >= argc) {
			System.out.print("\nNot enough input parameters!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		
		trainfile = argv[i];
		if ((i + 1) < argc) {
			modelfile = argv[i + 1];
		}

		System.out.println("trainfile:" + trainfile);
		System.out.println("modelfile:" + modelfile);
		System.out.println("struct_parm.C:" + struct_parm.C);

		if (learn_parm.svm_iter_to_shrink == -9999) {
			learn_parm.svm_iter_to_shrink = 100;
		}

		if ((learn_parm.skip_final_opt_check != 0)
				&& (kernel_parm.kernel_type == svm_common.LINEAR)) {
			System.out
					.print("\nIt does not make sense to skip the final optimality check for linear kernels.\n\n");
			learn_parm.skip_final_opt_check = 0;
		}
		
		if ((learn_parm.skip_final_opt_check != 0)
				&& (learn_parm.remove_inconsistent != 0)) {
			System.out
					.print("\nIt is necessary to do the final optimality check when removing inconsistent \nexamples.\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		
		if ((learn_parm.svm_maxqpsize < 2)) {
			System.out
					.print("\nMaximum size of QP-subproblems not in valid range: "
							+ learn_parm.svm_maxqpsize + " [2..]\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		
		if ((learn_parm.svm_maxqpsize < learn_parm.svm_newvarsinqp)) {
			System.out.print("\nMaximum size of QP-subproblems ["
					+ learn_parm.svm_maxqpsize
					+ "] must be larger than the number of\n");
			System.out.print("new variables [" + learn_parm.svm_newvarsinqp
					+ "] entering the working set in each iteration.\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		
		if (learn_parm.svm_iter_to_shrink < 1) {
			System.out
					.print("\nMaximum number of iterations for shrinking not in valid range: "
							+ learn_parm.svm_iter_to_shrink + " [1,..]\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		
		if (struct_parm.C < 0) {
			System.out
					.print("\nYou have to specify a value for the parameter '-c' (C>0)!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		
		if (((alg_type) < 0) || (((alg_type) > 5) && ((alg_type) != 9))) {
			System.out
					.print("\nAlgorithm type must be either '0', '1', '2', '3', '4', or '9'!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		
		if (learn_parm.transduction_posratio > 1) {
			System.out
					.print("\nThe fraction of unlabeled examples to classify as positives must\n");
			System.out.print("be less than 1.0 !!!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}

		if (learn_parm.svm_costratio <= 0) {
			System.out
					.print("\nThe COSTRATIO parameter must be greater than zero!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if (struct_parm.epsilon <= 0) {
			System.out
					.print("\nThe epsilon parameter must be greater than zero!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if ((struct_parm.ccache_size <= 0) && ((alg_type) == 4)) {
			System.out.print("\nThe cache size must be at least 1!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if (((struct_parm.batch_size <= 0) || (struct_parm.batch_size > 100))
				&& ((alg_type) == 4)) {
			System.out
					.print("\nThe batch size must be in the interval ]0,100]!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if ((struct_parm.slack_norm < 1) || (struct_parm.slack_norm > 2)) {
			System.out
					.print("\nThe norm of the slacks must be either 1 (L1-norm) or 2 (L2-norm)!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if ((struct_parm.loss_type != svm_struct_learn.SLACK_RESCALING)
				&& (struct_parm.loss_type != svm_struct_learn.MARGIN_RESCALING)) {
			System.out
					.print("\nThe loss type must be either 1 (slack rescaling) or 2 (margin rescaling)!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if (learn_parm.rho < 0) {
			System.out
					.print("\nThe parameter rho for xi/alpha-estimates and leave-one-out pruning must\n");
			System.out
					.print("be greater than zero (typically 1.0 or 2.0, see T. Joachims, Estimating the\n");
			System.out
					.print("Generalization Performance of an SVM Efficiently, ICML, 2000.)!\n\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if ((learn_parm.xa_depth < 0) || (learn_parm.xa_depth > 100)) {
			System.out
					.print("\nThe parameter depth for ext. xi/alpha-estimates must be in [0..100] (zero\n");
			System.out
					.print("for switching to the conventional xa/estimates described in T. Joachims,\n");
			System.out
					.print("Estimating the Generalization Performance of an SVM Efficiently, ICML, 2000.)\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}

		svm_struct_api.parse_struct_parameters(struct_parm);
	}

	public static void main(String[] args) {
		
		if(args.length<2)
		{
			print_help();
			System.exit(1);
		}
		
		//svmconfig.model_type=1;
		SAMPLE sample; /* training sample */
		LEARN_PARM learn_parm = new LEARN_PARM();
		KERNEL_PARM kernel_parm = new KERNEL_PARM();
		STRUCT_LEARN_PARM struct_parm = new STRUCT_LEARN_PARM();
		STRUCTMODEL structmodel = new STRUCTMODEL();
		
		svm_struct_api.svm_struct_learn_api_init(args);

		
		long start_time = TimeOpera.getCurrentTimeLong();
		
		read_input_parameters(args.length + 1, args, struct_parm, learn_parm,
				kernel_parm);

		if (struct_verbosity >= 1) {
			System.out.println("Reading training examples...");
			logger.info("Reading training examples...");
		}
		svm_struct_api ssa=svm_struct_api_factory.getSvmStructApi();
		/* read the training examples */
		sample = ssa.read_struct_examples(trainfile, struct_parm);
		System.out.println("sample.size:"+sample.examples.length);
		if (struct_verbosity >= 1) {
			logger.info("done\n");
		}
		logger.info("alg_tye is " + alg_type + " \n");

		EXAMPLE tempex = null;
		// String winfo="";
		/*
		 * for(int k=0;k<sample.examples.length;k++) {
		 * tempex=sample.examples[k];
		 * logger.info("tempex k="+k+" "+tempex.x.doc.fvec.toString()); }
		 */
		/* Do the learning and return structmodel. */
		svm_struct_learn ssl = new svm_struct_learn();
		if (alg_type == 0) {
			ssl.svm_learn_struct(sample, struct_parm, learn_parm, kernel_parm,
					structmodel, svm_struct_common.NSLACK_ALG);
		} else if (alg_type == 1) {
			ssl.svm_learn_struct(sample, struct_parm, learn_parm, kernel_parm,
					structmodel, svm_struct_common.NSLACK_SHRINK_ALG);
		} else if (alg_type == 2) {
			ssl.svm_learn_struct_joint(sample, struct_parm, learn_parm,
					kernel_parm, structmodel,
					svm_struct_common.ONESLACK_PRIMAL_ALG);
		} else if (alg_type == 3) {
			ssl.svm_learn_struct_joint(sample, struct_parm, learn_parm,
					kernel_parm, structmodel,
					svm_struct_common.ONESLACK_DUAL_ALG);
		} else if (alg_type == 4) {
			logger.info("learn_parm.sharedslack:" + learn_parm.sharedslack);
			ssl.svm_learn_struct_joint(sample, struct_parm, learn_parm,
					kernel_parm, structmodel,
					svm_struct_common.ONESLACK_DUAL_CACHE_ALG);
		} else if (alg_type == 9) {
			ssl.svm_learn_struct_joint_custom(sample, struct_parm, learn_parm,
					kernel_parm, structmodel);
		} else {
			System.exit(1);
		}
		
		/*
		 * Warning: The model contains references to the original data 'docs'.
		 * If you want to free the original data, and only keep the model, you
		 * have to make a deep copy of 'model'.
		 */
		if (struct_verbosity >= 1) {
			logger.info("Writing learned model...");
		}
		ssa.write_struct_model(modelfile, structmodel, struct_parm);
		if (struct_verbosity >= 1) {
			logger.info("done\n");
		}

		long end_time = TimeOpera.getCurrentTimeLong();
		double tot_time = (double) (end_time - start_time) / (double) 1000;

		logger.info("tot_time:" + tot_time);
		System.out.println("tot_time:" + tot_time);

		svm_struct_api.svm_struct_learn_api_exit();
	}

	public static void print_help() {
		System.out.print("\nSVM-struct learning module: "
				+ svm_struct_common.INST_NAME + ", "
				+ svm_struct_common.INST_VERSION + ", "
				+ svm_struct_common.INST_VERSION_DATE + "\n");
		System.out.print("   includes SVM-struct "
				+ svm_struct_common.STRUCT_VERSION
				+ " for learning complex outputs, "
				+ svm_struct_common.STRUCT_VERSION_DATE + "\n");
		System.out.print("   includes SVM-light " + svm_common.VERSION
				+ " quadratic optimizer, " + svm_common.VERSION_DATE + "\n");
		svm_common.copyright_notice();
		System.out
				.print("   usage: svm_struct_learn [options] example_file model_file\n\n");
		System.out.print("Arguments:\n");
		System.out.print("         example_file-> file with training data\n");
		System.out
				.print("         model_file  -> file to store learned decision rule in\n");

		System.out.print("General Options:\n");
		System.out.print("         -?          -> this help\n");
		System.out
				.print("         -v [0..3]   -> verbosity level (default 1)\n");
		System.out
				.print("         -y [0..3]   -> verbosity level for svm_light (default 0)\n");
		System.out.print("Learning Options:\n");
		System.out
				.print("         -c float    -> C: trade-off between training error\n");
		System.out.print("                        and margin (default 0.01)\n");
		System.out
				.print("         -p [1,2]    -> L-norm to use for slack variables. Use 1 for L1-norm,\n");
		System.out
				.print("                        use 2 for squared slacks. (default 1)\n");
		System.out
				.print("         -o [1,2]    -> Rescaling method to use for loss.\n");
		System.out.print("                        1: slack rescaling\n");
		System.out.print("                        2: margin rescaling\n");
		System.out.print("                        (default "
				+ svm_struct_common.DEFAULT_RESCALING + ")\n");
		System.out.print("         -l [0..]    -> Loss function to use.\n");
		System.out.print("                        0: zero/one loss\n");
		System.out
				.print("                        ?: see below in application specific options\n");
		System.out.print("                        (default "
				+ svm_struct_common.DEFAULT_LOSS_FCT + ")\n");
		System.out.print("Optimization Options (see [2][5]):\n");
		System.out
				.print("         -w [0,..,9] -> choice of structural learning algorithm (default "
						+ ((int) svm_struct_common.DEFAULT_ALG_TYPE) + "):\n");
		System.out
				.print("                        0: n-slack algorithm described in [2]\n");
		System.out
				.print("                        1: n-slack algorithm with shrinking heuristic\n");
		System.out
				.print("                        2: 1-slack algorithm (primal) described in [5]\n");
		System.out
				.print("                        3: 1-slack algorithm (dual) described in [5]\n");
		System.out
				.print("                        4: 1-slack algorithm (dual) with constraint cache [5]\n");
		System.out
				.print("                        9: custom algorithm in svm_struct_learn_custom.c\n");
		System.out
				.print("         -e float    -> epsilon: allow that tolerance for termination\n");
		System.out.print("                        criterion (default "
				+ svm_struct_common.DEFAULT_EPS + ")\n");
		System.out
				.print("         -k [1..]    -> number of new constraints to accumulate before\n");
		System.out
				.print("                        recomputing the QP solution (default 100) (-w 0 and 1 only)\n");
		System.out
				.print("         -f [5..]    -> number of constraints to cache for each example\n");
		System.out
				.print("                        (default 5) (used with -w 4)\n");
		System.out
				.print("         -b [1..100] -> percentage of training set for which to refresh cache\n");
		System.out
				.print("                        when no epsilon violated constraint can be constructed\n");
		System.out
				.print("                        from current cache (default 100%%) (used with -w 4)\n");
		System.out
				.print("SVM-light Options for Solving QP Subproblems (see [3]):\n");
		System.out
				.print("         -n [2..q]   -> number of new variables entering the working set\n");
		System.out
				.print("                        in each svm-light iteration (default n = q). \n");
		System.out
				.print("                        Set n < q to prevent zig-zagging.\n");
		System.out
				.print("         -m [5..]    -> size of svm-light cache for kernel evaluations in MB\n");
		System.out
				.print("                        (default 40) (used only for -w 1 with kernels)\n");
		System.out
				.print("         -h [5..]    -> number of svm-light iterations a variable needs to be\n");
		System.out
				.print("                        optimal before considered for shrinking (default 100)\n");
		System.out
				.print("         -# int      -> terminate svm-light QP subproblem optimization, if no\n");
		System.out
				.print("                        progress after this number of iterations.\n");
		System.out.print("                        (default 100000)\n");
		System.out.print("Kernel Options:\n");
		System.out.print("         -t int      -> type of kernel function:\n");
		System.out.print("                        0: linear (default)\n");
		System.out.print("                        1: polynomial (s a*b+c)^d\n");
		System.out
				.print("                        2: radial basis function exp(-gamma ||a-b||^2)\n");
		System.out
				.print("                        3: sigmoid tanh(s a*b + c)\n");
		System.out
				.print("                        4: user defined kernel from kernel.h\n");
		System.out
				.print("         -d int      -> parameter d in polynomial kernel\n");
		System.out
				.print("         -g float    -> parameter gamma in rbf kernel\n");
		System.out
				.print("         -s float    -> parameter s in sigmoid/poly kernel\n");
		System.out
				.print("         -r float    -> parameter c in sigmoid/poly kernel\n");
		System.out
				.print("         -u string   -> parameter of user defined kernel\n");
		System.out.print("Output Options:\n");
		System.out
				.print("         -a string   -> write all alphas to this file after learning\n");
		System.out
				.print("                        (in the same order as in the training set)\n");
		System.out.print("Application-Specific Options:\n");
		svm_struct_api.print_struct_help();
		wait_any_key();
		System.out.print("\nMore details in:\n");
		System.out
				.print("[1] T. Joachims, Learning to Align Sequences: A Maximum Margin Aproach.\n");
		System.out.print("    Technical Report, September, 2003.\n");
		System.out
				.print("[2] I. Tsochantaridis, T. Joachims, T. Hofmann, and Y. Altun, Large Margin\n");
		System.out
				.print("    Methods for Structured and Interdependent Output Variables, Journal\n");
		System.out
				.print("    of Machine Learning Research (JMLR), Vol. 6(Sep):1453-1484, 2005.\n");
		System.out
				.print("[3] T. Joachims, Making Large-Scale SVM Learning Practical. Advances in\n");
		System.out
				.print("    Kernel Methods - Support Vector Learning, B. Sch鰈kopf and C. Burges and\n");
		System.out.print("    A. Smola (ed.), MIT Press, 1999.\n");
		System.out
				.print("[4] T. Joachims, Learning to Classify Text Using Support Vector\n");
		System.out
				.print("    Machines: Methods, Theory, and Algorithms. Dissertation, Kluwer,\n");
		System.out.print("    2002.\n");
		System.out
				.print("[5] T. Joachims, T. Finley, Chun-Nam Yu, Cutting-Plane Training of Structural\n");
		System.out.print("    SVMs, Machine Learning Journal, to appear.\n");
	}

	public static void wait_any_key() {
		System.out.println("\n(more)\n");
	}

}
