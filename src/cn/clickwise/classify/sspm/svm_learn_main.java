package cn.clickwise.classify.sspm;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

public class svm_learn_main {

	public static String docfile;
	public static String modelfile;
	public static String restartfile;

	private static Logger logger = Logger.getLogger(svm_learn_main.class);

	public static void read_input_parameters(int argc, String argv[],
			short verbosity, LEARN_PARM learn_parm, KERNEL_PARM kernel_parm) {
		int i;
		String type;

		/* set default */
		svm_common.set_learning_defaults(learn_parm, kernel_parm);
		modelfile = "svm_model";
		restartfile = "";
		//svm_common.verbosity = 5;
		type = "c";

		for (i = 1; (i < argc) && ((argv[i].charAt(0)) == '-'); i++) {

			switch ((argv[i].charAt(1))) {
			case '?':
				print_help();
				System.exit(0);
			case 'z':
				i++;
				type = argv[i];
				break;
			case 'v':
				i++;
				svm_common.verbosity = Integer.parseInt(argv[i]);
				break;
			case 'b':
				i++;
				learn_parm.biased_hyperplane = Short.parseShort(argv[i]);
				break;
			case 'i':
				i++;
				learn_parm.remove_inconsistent = Integer.parseInt(argv[i]);
				break;
			case 'f':
				i++;
				learn_parm.skip_final_opt_check = 1 - Integer.parseInt(argv[i]);
				break;
			case 'q':
				i++;
				learn_parm.svm_maxqpsize = Integer.parseInt(argv[i]);
				break;
			case 'n':
				i++;
				learn_parm.svm_newvarsinqp = Integer.parseInt(argv[i]);
				break;
			case '#':
				i++;
				learn_parm.maxiter = Integer.parseInt(argv[i]);
				break;
			case 'h':
				i++;
				learn_parm.svm_iter_to_shrink = Integer.parseInt(argv[i]);
				break;
			case 'm':
				i++;
				learn_parm.kernel_cache_size = Integer.parseInt(argv[i]);
				break;
			case 'c':
				i++;
				learn_parm.svm_c = Integer.parseInt(argv[i]);
				break;
			case 'w':
				i++;
				learn_parm.eps = Integer.parseInt(argv[i]);
				break;
			case 'p':
				i++;
				learn_parm.transduction_posratio = Integer.parseInt(argv[i]);
				break;
			case 'j':
				i++;
				learn_parm.svm_costratio = Double.parseDouble(argv[i]);
				break;
			case 'e':
				i++;
				learn_parm.epsilon_crit = Double.parseDouble(argv[i]);
				break;
			case 'o':
				i++;
				learn_parm.rho = Double.parseDouble(argv[i]);
				break;
			case 'k':
				i++;
				learn_parm.xa_depth = Integer.parseInt(argv[i]);
				break;
			case 'x':
				i++;
				learn_parm.compute_loo = Integer.parseInt(argv[i]);
				break;
			case 't':
				i++;
				kernel_parm.kernel_type = Short.parseShort(argv[i]);
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
			case 'l':
				i++;
				learn_parm.predfile = argv[i];
				break;
			case 'a':
				i++;
				learn_parm.alphafile = argv[i];
				break;
			case 'y':
				i++;
				restartfile = argv[i];
				break;
			default:
				System.out.println("\nUnrecognized option " + argv[i]);
				print_help();
				System.exit(0);
			}
		}
		if (i >= argc) {
			System.out.println("\nNot enough input parameters!\n\n");
			// wait_any_key();
			// print_help();
			System.exit(0);
		}
		docfile = argv[i];
		System.out.println("docfile:" + docfile);
		if ((i + 1) < argc) {
			modelfile = argv[i + 1];
		}
		System.out.println("modelfile:" + modelfile);
		if (learn_parm.svm_iter_to_shrink == -9999) {
			if (kernel_parm.kernel_type == svm_common.LINEAR)
				learn_parm.svm_iter_to_shrink = 2;
			else
				learn_parm.svm_iter_to_shrink = 100;
		}
		if (type.equals("c")) {
			learn_parm.type = svm_common.CLASSIFICATION;
		} else if (type.equals("r")) {
			learn_parm.type = svm_common.REGRESSION;
		} else if (type.equals("p")) {
			learn_parm.type = svm_common.RANKING;
		} else if (type.equals("o")) {
			learn_parm.type = svm_common.OPTIMIZATION;
		} else if (type.equals("s")) {
			learn_parm.type = svm_common.OPTIMIZATION;
			learn_parm.sharedslack = 1;
		} else {
			System.out
					.println("\nUnknown type '"
							+ type
							+ "': Valid types are 'c' (classification), 'r' regession, and 'p' preference ranking.\n");
			wait_any_key();
			print_help();
			System.exit(0);
		}
		if (!svm_common.check_learning_parms(learn_parm, kernel_parm)) {
			wait_any_key();
			print_help();
			System.exit(0);
		}
	}

	public static void print_help() {
		System.out.println("\nSVM-light " + svm_common.VERSION
				+ ": Support Vector Machine, learning module  "
				+ svm_common.VERSION_DATE);
		svm_common.copyright_notice();
		System.out
				.println("   usage: svm_learn [options] example_file model_file\n\n");
		System.out.println("Arguments:\n");
		System.out.println("         example_file-> file with training data\n");
		System.out
				.println("         model_file  -> file to store learned decision rule in\n");

		System.out.println("General options:\n");
		System.out.println("         -?          -> this help\n");
		System.out
				.println("         -v [0..3]   -> verbosity level (default 1)\n");
		System.out.println("Learning options:\n");
		System.out
				.println("         -z {c,r,p}  -> select between classification (c), regression (r),\n");
		System.out
				.println("                        and preference ranking (p) (default classification)\n");
		System.out
				.println("         -c float    -> C: trade-off between training error\n");
		System.out
				.println("                        and margin (default [avg. x*x]^-1)\n");
		System.out
				.println("         -w [0..]    -> epsilon width of tube for regression\n");
		System.out.println("                        (default 0.1)\n");
		System.out
				.println("         -j float    -> Cost: cost-factor, by which training errors on\n");
		System.out
				.println("                        positive examples outweight errors on negative\n");
		System.out
				.println("                        examples (default 1) (see [4])\n");
		System.out
				.println("         -b [0,1]    -> use biased hyperplane (i.e. x*w+b>0) instead\n");
		System.out
				.println("                        of unbiased hyperplane (i.e. x*w>0) (default 1)\n");
		System.out
				.println("         -i [0,1]    -> remove inconsistent training examples\n");
		System.out.println("                        and retrain (default 0)\n");
		System.out.println("Performance estimation options:\n");
		System.out
				.println("         -x [0,1]    -> compute leave-one-out estimates (default 0)\n");
		System.out.println("                        (see [5])\n");
		System.out
				.println("         -o ]0..2]   -> value of rho for XiAlpha-estimator and for pruning\n");
		System.out
				.println("                        leave-one-out computation (default 1.0) (see [2])\n");
		System.out
				.println("         -k [0..100] -> search depth for extended XiAlpha-estimator \n");
		System.out.println("                        (default 0)\n");
		System.out.println("Transduction options (see [3]):\n");
		System.out
				.println("         -p [0..1]   -> fraction of unlabeled examples to be classified\n");
		System.out
				.println("                        into the positive class (default is the ratio of\n");
		System.out
				.println("                        positive and negative examples in the training data)\n");
		System.out.println("Kernel options:\n");
		System.out
				.println("         -t int      -> type of kernel function:\n");
		System.out.println("                        0: linear (default)\n");
		System.out
				.println("                        1: polynomial (s a*b+c)^d\n");
		System.out
				.println("                        2: radial basis function exp(-gamma ||a-b||^2)\n");
		System.out
				.println("                        3: sigmoid tanh(s a*b + c)\n");
		System.out
				.println("                        4: user defined kernel from kernel.h\n");
		System.out
				.println("         -d int      -> parameter d in polynomial kernel\n");
		System.out
				.println("         -g float    -> parameter gamma in rbf kernel\n");
		System.out
				.println("         -s float    -> parameter s in sigmoid/poly kernel\n");
		System.out
				.println("         -r float    -> parameter c in sigmoid/poly kernel\n");
		System.out
				.println("         -u string   -> parameter of user defined kernel\n");
		System.out.println("Optimization options (see [1]):\n");
		System.out
				.println("         -q [2..]    -> maximum size of QP-subproblems (default 10)\n");
		System.out
				.println("         -n [2..q]   -> number of new variables entering the working set\n");
		System.out
				.println("                        in each iteration (default n = q). Set n < q to \n");
		System.out.println("                        prevent zig-zagging.\n");
		System.out
				.println("         -m [5..]    -> size of cache for kernel evaluations in MB (default 40)\n");
		System.out
				.println("                        The larger the faster...\n");
		System.out
				.println("         -e float    -> eps: Allow that error for termination criterion\n");
		System.out
				.println("                        [y [w*x+b] - 1] >= eps (default 0.001)\n");
		System.out
				.println("         -y [0,1]    -> restart the optimization from alpha values in file\n");
		System.out
				.println("                        specified by -a option. (default 0)\n");
		System.out
				.println("         -h [5..]    -> number of iterations a variable needs to be\n");
		System.out
				.println("                        optimal before considered for shrinking (default 100)\n");
		System.out
				.println("         -f [0,1]    -> do final optimality check for variables removed\n");
		System.out
				.println("                        by shrinking. Although this test is usually \n");
		System.out
				.println("                        positive, there is no guarantee that the optimum\n");
		System.out
				.println("                        was found if the test is omitted. (default 1)\n");
		System.out
				.println("         -y string   -> if option is given, reads alphas from file with given\n");
		System.out
				.println("                        and uses them as starting point. (default 'disabled')\n");
		System.out
				.println("         -# int      -> terminate optimization, if no progress after this\n");
		System.out
				.println("                        number of iterations. (default 100000)\n");
		System.out.println("Output options:\n");
		System.out
				.println("         -l string   -> file to write predicted labels of unlabeled\n");
		System.out
				.println("                        examples into after transductive learning\n");
		System.out
				.println("         -a string   -> write all alphas to this file after learning\n");
		System.out
				.println("                        (in the same order as in the training set)\n");
		wait_any_key();
		System.out.println("\nMore details in:\n");
		System.out
				.println("[1] T. Joachims, Making Large-Scale SVM Learning Practical. Advances in\n");
		System.out
				.println("    Kernel Methods - Support Vector Learning, B. Sch鰈kopf and C. Burges and\n");
		System.out.println("    A. Smola (ed.), MIT Press, 1999.\n");
		System.out
				.println("[2] T. Joachims, Estimating the Generalization performance of an SVM\n");
		System.out
				.println("    Efficiently. International Conference on Machine Learning (ICML), 2000.\n");
		System.out
				.println("[3] T. Joachims, Transductive Inference for Text Classification using Support\n");
		System.out
				.println("    Vector Machines. International Conference on Machine Learning (ICML),\n");
		System.out.println("    1999.\n");
		System.out
				.println("[4] K. Morik, P. Brockhausen, and T. Joachims, Combining statistical learning\n");
		System.out
				.println("    with a knowledge-based approach - A case study in intensive care  \n");
		System.out
				.println("    monitoring. International Conference on Machine Learning (ICML), 1999.\n");
		System.out
				.println("[5] T. Joachims, Learning to Classify Text Using Support Vector\n");
		System.out
				.println("    Machines: Methods, Theory, and Algorithms. Dissertation, Kluwer,\n");
		System.out.println("    2002.\n\n");
	}

	public static void wait_any_key() {
		System.out.println("\n(more)\n");
	}

	public static void main(String[] args) {
		svm_learn sl = new svm_learn();
		DOC[] docs = null; /* training examples */
		int totwords = 0, totdoc = 0, i = 0;
		double[] target = null;
		double[] alpha_in = null;
		KERNEL_CACHE kernel_cache;
		LEARN_PARM learn_parm;
		KERNEL_PARM kernel_parm;

		MODEL model = new MODEL();
		learn_parm = new LEARN_PARM();
		kernel_parm = new KERNEL_PARM();
		read_input_parameters(args.length + 1, args,
				(short) svm_common.verbosity, learn_parm, kernel_parm);
		PrintWriter pw = null;
		FileWriter fw = null;

		try {
			fw = new FileWriter(new File("log3.txt"));
			pw = new PrintWriter(fw);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		svm_common sc=new svm_common();
		docs = sc.read_documents(docfile, target);

		target = sc.read_target;
		System.out.println("docs length in main:" + docs.length);
		totwords = sc.read_totwords;
		totdoc = sc.read_totdocs;
		/*
		 * pw.println("docs length:"+docs.length); for(int
		 * k=0;k<docs.length;k++) { pw.print(k+"  "); if(docs[k]!=null) for(int
		 * l=0;l<docs[k].fvec.words.length;l++) {
		 * pw.print(docs[k].fvec.words[l].
		 * wnum+":"+docs[k].fvec.words[l].weight+" "); } pw.println(); }
		 */
		// for(int di=0;di<docs.length;di++)
		// {
		// if(docs[di]!=null)
		// System.out.println("docs di["+di+"]="+docs[di].fvec.words.length);
		// }
		if (restartfile != null) {
			alpha_in = svm_common.read_alphas(restartfile,
					sc.read_totdocs);
		}
		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			kernel_cache = null;
		} else {
			/*
			 * Always get a new kernel cache. It is not possible to use the same
			 * cache for two different training runs
			 */
			kernel_cache = svm_learn.kernel_cache_init(totdoc,
					learn_parm.kernel_cache_size);
		}

		if (learn_parm.type == svm_common.CLASSIFICATION) {
			logger.info("call   sl.svm_learn_classification");
			sl.svm_learn_classification(docs, target, totdoc, totwords,
					learn_parm, kernel_parm, kernel_cache, model, alpha_in);
		} else if (learn_parm.type == svm_common.OPTIMIZATION) {
			logger.info("call   sl.svm_learn_optimization");
			sl.svm_learn_optimization(docs, target, totdoc, totwords,
					learn_parm, kernel_parm, kernel_cache, model, alpha_in);
		}
		sc.write_model(modelfile, model);
	}

}
