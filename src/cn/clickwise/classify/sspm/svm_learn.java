package cn.clickwise.classify.sspm;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import cn.clickwise.time.utils.TimeOpera;

/**
 * Learning module of Support Vector Machine
 * 
 * @author lq
 * 
 */
public class svm_learn {

	private static Logger logger = Logger.getLogger(svm_learn.class);
	public int kernel_cache_statistic;
	public static final int MAXSHRINK = 50000;
	public double maxdiff;

	public int misclassified = 0;
	public int inconsistentnum = 0;
	public double maxsharedviol;
	//public double maxviol = 0;
	public double[] alpha_g;

	public svm_common sc=null;
	public svm_learn() {
		sc=new svm_common();
	}

	public void svm_learn_classification(DOC[] docs, double[] classc,
			int totdoc, int totwords, LEARN_PARM learn_parm,
			KERNEL_PARM kernel_parm, KERNEL_CACHE kernel_cache, MODEL model,
			double[] alpha) {
		/*
		 * pw.println("docs.length:"+docs.length); for(int
		 * k=0;k<docs.length;k++) { pw.print(k+"  "); if(docs[k].fvec!=null)
		 * for(int l=0;l<docs[k].fvec.words.length;l++) {
		 * pw.print(docs[k].fvec.words
		 * [l].wnum+":"+docs[k].fvec.words[l].weight+" "); pw.flush(); }
		 * pw.println(); }
		 */
		int[] inconsistent;
		int i;
		int[] label;
		int misclassified, upsupvecnum;
		double loss, model_length, example_length;
		double dualitygap, xisum, alphasum, xi;
		double[] lin, a, c;
		double runtime_start, runtime_end;
		int iterations;
		int[] unlabeled;
		int transduction;
		int heldout;
		int loo_count = 0, loo_count_pos = 0, loo_count_neg = 0, trainpos = 0, trainneg = 0;
		int loocomputed = 0;
		double runtime_start_loo = 0, runtime_start_xa = 0;
		double heldout_c = 0, r_delta_sq = 0, r_delta, r_delta_avg;
		int[] index;
		int[] index2dnum;
		double[] weights;
		double[] aicache;

		double[] xi_fullset;
		double[] a_fullset;
		TIMING timing_profile = new TIMING();
		SHRINK_STATE shrink_state = new SHRINK_STATE();

		runtime_start = svm_common.get_runtime();
		timing_profile.time_kernel = 0;
		timing_profile.time_opti = 0;
		timing_profile.time_shrink = 0;
		timing_profile.time_update = 0;
		timing_profile.time_model = 0;
		timing_profile.time_check = 0;
		timing_profile.time_select = 0;
		kernel_cache_statistic = 0;

		learn_parm.totwords = totwords;
		/**** add test ******/
		// learn_parm.sharedslack=1;

		/* make sure -n value is reasonable */
		if ((learn_parm.svm_newvarsinqp < 2)
				|| (learn_parm.svm_newvarsinqp > learn_parm.svm_maxqpsize)) {
			learn_parm.svm_newvarsinqp = learn_parm.svm_maxqpsize;
		}

		init_shrink_state(shrink_state, totdoc, MAXSHRINK);

		label = new int[totdoc];
		inconsistent = new int[totdoc];
		unlabeled = new int[totdoc];
		c = new double[totdoc];
		a = new double[totdoc];
		a_fullset = new double[totdoc];
		xi_fullset = new double[totdoc];
		lin = new double[totdoc];
		learn_parm.svm_cost = new double[totdoc];
		model.supvec = new DOC[totdoc + 2];
		model.alpha = new double[totdoc + 2];
		model.index = new int[totdoc + 2];

		model.at_upper_bound = 0;
		model.b = 0;
		model.supvec[0] = null; /* element 0 reserved and empty for now */
		model.alpha[0] = 0;
		model.lin_weights = null;
		model.totwords = totwords;
		model.totdoc = totdoc;
		model.kernel_parm = kernel_parm;
		model.sv_num = 1;
		model.loo_error = -1;
		model.loo_recall = -1;
		model.loo_precision = -1;
		model.xa_error = -1;
		model.xa_recall = -1;
		model.xa_precision = -1;
		inconsistentnum = 0;
		transduction = 0;

		r_delta = estimate_r_delta(docs, totdoc, kernel_parm);
		r_delta_sq = r_delta * r_delta;

		r_delta_avg = estimate_r_delta_average(docs, totdoc, kernel_parm);
		if (learn_parm.svm_c == 0.0) { /* default value for C */
			learn_parm.svm_c = 1.0 / (r_delta_avg * r_delta_avg);
			if (svm_common.verbosity >= 1)
				System.out.println("Setting default regularization parameter C="+ learn_parm.svm_c);
		}

		learn_parm.eps = -1.0; /* equivalent regression epsilon for classification*/

		for (i = 0; i < totdoc; i++) { /* various inits */
			docs[i].docnum = i;
			inconsistent[i] = 0;
			a[i] = 0;
			lin[i] = 0;
			c[i] = 0.0;
			unlabeled[i] = 0;
			if (classc[i] == 0) {
				unlabeled[i] = 1;
				label[i] = 0;
				transduction = 1;
			}
			if (classc[i] > 0) {
				learn_parm.svm_cost[i] = learn_parm.svm_c
						* learn_parm.svm_costratio * docs[i].costfactor;
				label[i] = 1;
				trainpos++;
			} else if (classc[i] < 0) {
				learn_parm.svm_cost[i] = learn_parm.svm_c * docs[i].costfactor;
				label[i] = -1;
				trainneg++;
			} else {
				learn_parm.svm_cost[i] = 0;
			}
		}

		if (svm_common.verbosity >= 2) {
			System.out.println(trainpos + " positive, " + trainneg
					+ " negative, and " + (totdoc - trainpos - trainneg)
					+ " unlabeled examples.\n");
		}

		/* caching makes no sense for linear kernel */
		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			/* kernel_cache = NULL; */
		}

		/* compute starting state for initial alpha values */
		if (alpha != null) {
			if (svm_common.verbosity >= 1) {
				System.out.println("Computing starting state...");
			}
			index = new int[totdoc];
			index2dnum = new int[totdoc + 11];
			weights = new double[totwords + 1];
			aicache = new double[totdoc];
			for (i = 0; i < totdoc; i++) { /* create full index and clip alphas */
				index[i] = 1;
				alpha[i] = Math.abs(alpha[i]);
				if (alpha[i] < 0)
					alpha[i] = 0;
				if (alpha[i] > learn_parm.svm_cost[i]) {
					alpha[i] = learn_parm.svm_cost[i];
				}
			}

			if ((kernel_cache != null)
					&& (kernel_parm.kernel_type != svm_common.LINEAR)) {
				for (i = 0; i < totdoc; i++)
					/* fill kernel cache with unbounded SV */
					if ((alpha[i] > 0) && (alpha[i] < learn_parm.svm_cost[i])
							&& (kernel_cache_space_available(kernel_cache)))
						cache_kernel_row(kernel_cache, docs, i, kernel_parm);
				for (i = 0; i < totdoc; i++)
					/* fill rest of kernel cache with bounded SV */
					if ((alpha[i] == learn_parm.svm_cost[i])
							&& (kernel_cache_space_available(kernel_cache)))
						cache_kernel_row(kernel_cache, docs, i, kernel_parm);
			}
			svm_common.clear_nvector(weights, totwords); /* set weights to zero */
			compute_index(index, totdoc, index2dnum);
			update_linear_component(docs, label, index2dnum, alpha, a,
					index2dnum, totdoc, totwords, kernel_parm, kernel_cache,
					lin, aicache, weights);
			calculate_svm_model(docs, label, unlabeled, lin, alpha, a, c,
					learn_parm, index2dnum, index2dnum, model);
			for (i = 0; i < totdoc; i++) { /* copy initial alphas */
				a[i] = alpha[i];
			}

			if (svm_common.verbosity >= 1) {

				// System.out.println("done.");
			}
		}

		if (transduction != 0) {
			learn_parm.svm_iter_to_shrink = 99999999;
			if (svm_common.verbosity >= 1)
				System.out
						.println("\nDeactivating Shrinking due to an incompatibility with the transductive \nlearner in the current version.");
		}

		if ((transduction != 0) && (learn_parm.compute_loo != 0)) {
			learn_parm.compute_loo = 0;
			if (svm_common.verbosity >= 1)
				System.out
						.println("\nCannot compute leave-one-out estimates for transductive learner.");
		}

		if ((learn_parm.remove_inconsistent != 0)
				&& (learn_parm.compute_loo != 0)) {
			learn_parm.compute_loo = 0;
			System.out
					.println("\nCannot compute leave-one-out estimates when removing inconsistent examples.");
		}

		if ((learn_parm.compute_loo != 0)
				&& ((trainpos == 1) || (trainneg == 1))) {
			learn_parm.compute_loo = 0;
			System.out
					.println("\nCannot compute leave-one-out with only one example in one class.");
		}

		if (svm_common.verbosity == 1) {
			System.out.println("Optimizing");
		}

		/* train the svm */
		iterations = optimize_to_convergence(docs, label, totdoc, totwords,
				learn_parm, kernel_parm, kernel_cache, shrink_state, model,
				inconsistent, unlabeled, a, lin, c, timing_profile, -1, 1);

		if (svm_common.verbosity >= 1) {
			if (svm_common.verbosity == 1) {
				System.out.println("done. (" + iterations + " iterations)");
			}

			misclassified = 0;
			for (i = 0; (i < totdoc); i++) { /* get final statistic */
				if ((lin[i] - model.b) * (double) label[i] <= 0.0)
					misclassified++;
			}

			System.out.println("Optimization finished (" + misclassified
					+ " misclassified, maxdiff=" + maxdiff + ").");

			runtime_end = svm_common.get_runtime();
			if (svm_common.verbosity >= 2) {
				System.out.println("Runtime in cpu-seconds:"
						+ (runtime_end - runtime_start) / 100.0 + " ("
						+ (100.0 * timing_profile.time_kernel)
						/ (runtime_end - runtime_start) + " for kernel/"
						+ (100.0 * timing_profile.time_opti)
						/ (runtime_end - runtime_start) + " for optimizer/"
						+ (100.0 * timing_profile.time_shrink)
						/ (runtime_end - runtime_start) + " for final/"
						+ (100.0 * timing_profile.time_update)
						/ (runtime_end - runtime_start) + " for update/"
						+ (100.0 * timing_profile.time_model)
						/ (runtime_end - runtime_start) + " for model/"
						+ (100.0 * timing_profile.time_check)
						/ (runtime_end - runtime_start) + " for check/"
						+ (100.0 * timing_profile.time_select)
						/ (runtime_end - runtime_start) + " for select)");
			} else {
				System.out.println("Runtime in cpu-seconds:"
						+ (runtime_end - runtime_start) / 100.0);
			}

			if (learn_parm.remove_inconsistent != 0) {
				inconsistentnum = 0;
				for (i = 0; i < totdoc; i++)
					if (inconsistent[i] != 0)
						inconsistentnum++;
				System.out.println("Number of SV: " + (model.sv_num - 1)
						+ " (plus " + inconsistentnum
						+ " inconsistent examples)");
			} else {
				upsupvecnum = 0;
				for (i = 1; i < model.sv_num; i++) {
					if (Math.abs(model.alpha[i]) >= (learn_parm.svm_cost[(model.supvec[i]).docnum] - learn_parm.epsilon_a))
						upsupvecnum++;
				}
				System.out.println("Number of SV: " + (model.sv_num - 1)
						+ " (including " + upsupvecnum + " at upper bound)");
			}

			if ((svm_common.verbosity >= 1)
					&& (learn_parm.skip_final_opt_check == 0)) {
				loss = 0;
				xisum = 0;
				alphasum = 0;
				model_length = 0;
				for (i = 0; i < totdoc; i++) {
					xi = Math.max(0.0, 1.0 - (lin[i] - model.b)
							* (double) label[i]);
					if (xi > learn_parm.epsilon_crit)
						loss += xi;
					xisum += xi * learn_parm.svm_cost[i];
					alphasum += a[i];
					model_length += a[i] * label[i] * lin[i];
				}
				model_length = Math.sqrt(model_length);
				dualitygap = (0.5 * model_length * model_length + xisum)
						- (alphasum - 0.5 * model_length * model_length);
				System.out.println("Upper bound on duality gap: gap="
						+ dualitygap);
				System.out.println("Dual objective value: dval="
						+ (alphasum - 0.5 * model_length * model_length));
				System.out.println("L1 loss: loss=" + loss);
				System.out
						.println("Norm of weight vector: |w|=" + model_length);
				example_length = estimate_sphere(model);
				System.out.println("Norm of longest example vector: |x|="
						+ length_of_longest_document_vector(docs, totdoc,
								kernel_parm));
				System.out.println("Estimated VCdim of classifier: VCdim<="
						+ estimate_margin_vcdim(model, model_length,
								example_length));

				if ((learn_parm.remove_inconsistent == 0)
						&& (transduction == 0)) {
					runtime_start_xa = svm_common.get_runtime();
					if (svm_common.verbosity >= 1) {
						System.out.println("Computing XiAlpha-estimates...");
					}
					compute_xa_estimates(model, label, unlabeled, totdoc, docs,
							lin, a, kernel_parm, learn_parm);
					if (svm_common.verbosity >= 1) {
						// System.out.println("done");
					}
					System.out
							.println("Runtime for XiAlpha-estimates in cpu-seconds: "
									+ (svm_common.get_runtime() - runtime_start_xa)
									/ 100.0);

					System.out.println("XiAlpha-estimate of the error: error<="
							+ model.xa_error + " (rho=" + learn_parm.rho
							+ ",depth=" + learn_parm.xa_depth + ")");
					System.out
							.println("XiAlpha-estimate of the recall: recall=>"
									+ model.xa_recall + " (rho="
									+ learn_parm.rho + ",depth="
									+ learn_parm.xa_depth + ")");
					System.out
							.println("XiAlpha-estimate of the precision: precision=>"
									+ model.xa_precision
									+ " (rho="
									+ learn_parm.rho
									+ ",depth="
									+ learn_parm.xa_depth + ")");
				} else if (learn_parm.remove_inconsistent == 0) {
					estimate_transduction_quality(model, label, unlabeled,
							totdoc, docs, lin);
				}
			}
			if (svm_common.verbosity >= 1) {
				System.out.println("Number of kernel evaluations:"
						+ kernel_cache_statistic);
			}
		}

		/* leave-one-out testing starts now */
		if (learn_parm.compute_loo != 0) {
			/* save results of training on full dataset for leave-one-out */
			runtime_start_loo = svm_common.get_runtime();
			for (i = 0; i < totdoc; i++) {
				xi_fullset[i] = 1.0 - ((lin[i] - model.b) * (double) label[i]);
				if (xi_fullset[i] < 0)
					xi_fullset[i] = 0;
				a_fullset[i] = a[i];
			}
			if (svm_common.verbosity >= 1) {
				System.out.println("Computing leave-one-out");
			}

			/* repeat this loop for every held-out example */
			for (heldout = 0; (heldout < totdoc); heldout++) {
				if (learn_parm.rho * a_fullset[heldout] * r_delta_sq
						+ xi_fullset[heldout] < 1.0) {
					/* guaranteed to not produce a leave-one-out error */
					if (svm_common.verbosity == 1) {
						System.out.print("+");
					}
				} else if (xi_fullset[heldout] > 1.0) {
					/* guaranteed to produce a leave-one-out error */
					loo_count++;
					if (label[heldout] > 0)
						loo_count_pos++;
					else
						loo_count_neg++;
					if (svm_common.verbosity == 1) {
						System.out.print("-");
					}
				} else {
					loocomputed++;
					heldout_c = learn_parm.svm_cost[heldout]; /*
															 * set upper bound
															 * to zero
															 */
					learn_parm.svm_cost[heldout] = 0;
					/* make sure heldout example is not currently */
					/* shrunk away. Assumes that lin is up to date! */
					shrink_state.active[heldout] = 1;
					if (svm_common.verbosity >= 2)
						System.out.println("\nLeave-One-Out test on example "
								+ heldout);
					if (svm_common.verbosity >= 1) {
						System.out.println("(?[" + heldout + "]");
					}

					optimize_to_convergence(docs, label, totdoc, totwords,
							learn_parm, kernel_parm, kernel_cache,
							shrink_state, model, inconsistent, unlabeled, a,
							lin, c, timing_profile, heldout, 2);

					if (((lin[heldout] - model.b) * (double) label[heldout]) <= 0.0) {
						loo_count++; /* there was a loo-error */
						if (label[heldout] > 0)
							loo_count_pos++;
						else
							loo_count_neg++;
						if (svm_common.verbosity >= 1) {
							System.out.print("-)");
						}
					} else {
						if (svm_common.verbosity >= 1) {
							System.out.print("+)");
						}
					}
					/* now we need to restore the original data set */
					learn_parm.svm_cost[heldout] = heldout_c; /*
															 * restore upper
															 * bound
															 */
				}
			} /* end of leave-one-out loop */

			if (svm_common.verbosity >= 1) {
				System.out.print("\nRetrain on full problem");
			}
			optimize_to_convergence(docs, label, totdoc, totwords, learn_parm,
					kernel_parm, kernel_cache, shrink_state, model,
					inconsistent, unlabeled, a, lin, c, timing_profile, -1, 1);
			// if(svm_common.verbosity >= 1)
			// System.out.println("done.");

			/* after all leave-one-out computed */
			model.loo_error = 100.0 * loo_count / (double) totdoc;
			model.loo_recall = (1.0 - (double) loo_count_pos
					/ (double) trainpos) * 100.0;
			model.loo_precision = (trainpos - loo_count_pos)
					/ (double) (trainpos - loo_count_pos + loo_count_neg)
					* 100.0;
			if (svm_common.verbosity >= 1) {
				System.out
						.println("Leave-one-out estimate of the error: error="
								+ model.loo_error);
				System.out
						.println("Leave-one-out estimate of the recall: recall="
								+ model.loo_recall);
				System.out
						.println("Leave-one-out estimate of the precision: precision="
								+ model.loo_precision);
				System.out.println("Actual leave-one-outs computed:  "
						+ loocomputed + " (rho=" + learn_parm.rho + ")");
				System.out.println("Runtime for leave-one-out in cpu-seconds: "
						+ (svm_common.get_runtime() - runtime_start_loo)
						/ 100.0);
			}
		}

		if (learn_parm.alphafile != null) {
			write_alphas(learn_parm.alphafile, a, label, totdoc);
		}

	}

	/* The following solves a freely defined and given set of
	   inequalities. The optimization problem is of the following form:

	   min 0.5 w*w + C sum_i C_i \xi_i
	   s.t. x_i * w > rhs_i - \xi_i

	   This corresponds to the -z o option. */
	public void svm_learn_optimization(DOC[] docs, double[] rhs, int totdoc,
			int totwords, LEARN_PARM learn_parm, KERNEL_PARM kernel_parm,
			KERNEL_CACHE kernel_cache, MODEL model, double[] alpha) {

		// logger.info("in svm_learn_optimization");
		/*
		 * if(docs!=null) { logger.info("docs.size:"+docs.length); for(int
		 * i=0;i<docs.length;i++) {
		 * logger.info(i+" doc fvec:"+docs[i].fvec.toString());
		 * logger.info(" rhs:"+rhs[i]); } }
		 */

		int i;
		int[] label;
		int misclassified, upsupvecnum;
		double loss = 0, model_length, alphasum, example_length;
		double[] lin;
		double[] a;
		double[] c;
		double runtime_start, runtime_end;
		int iterations, maxslackid, svsetnum = 0;
		int[] unlabeled;
		int[] inconsistent;
		double r_delta_avg;
		int[] index;
		int[] index2dnum;
		double[] weights;
		double[] slack;
		double[] alphaslack;

		double[] aicache;

		TIMING timing_profile = new TIMING();
		SHRINK_STATE shrink_state = new SHRINK_STATE();

		runtime_start = TimeOpera.getCurrentTimeLong();
		timing_profile.time_kernel = 0;
		timing_profile.time_opti = 0;
		timing_profile.time_shrink = 0;
		timing_profile.time_update = 0;
		timing_profile.time_model = 0;
		timing_profile.time_check = 0;
		timing_profile.time_select = 0;

		kernel_cache_statistic = 0;
		// logger.info("learn_parm.svm_maxqpsize before:"+learn_parm.svm_maxqpsize+" learn_parm.svm_newvarsinqp:"+learn_parm.svm_newvarsinqp);
		learn_parm.totwords = totwords;
		if ((learn_parm.svm_newvarsinqp < 2)
				|| (learn_parm.svm_newvarsinqp > learn_parm.svm_maxqpsize)) {
			learn_parm.svm_newvarsinqp = learn_parm.svm_maxqpsize;
		}
		// logger.info("learn_parm.svm_maxqpsize after:"+learn_parm.svm_maxqpsize+" learn_parm.svm_newvarsinqp:"+learn_parm.svm_newvarsinqp);
		init_shrink_state(shrink_state, totdoc, MAXSHRINK);

		label = new int[totdoc];
		unlabeled = new int[totdoc];
		inconsistent = new int[totdoc];
		c = new double[totdoc];
		a = new double[totdoc];
		lin = new double[totdoc];

		learn_parm.svm_cost = new double[totdoc];
		model.supvec = new DOC[totdoc + 2];
		model.alpha = new double[totdoc + 2];
		//for (int k = 0; k < model.alpha.length; k++) {

		//}
		
		model.index = new int[totdoc + 2];

		model.at_upper_bound = 0;
		model.b = 0;
		model.supvec[0] = null;
		model.alpha[0] = 0;
		model.lin_weights = null;
		model.totwords = totwords;
		model.totdoc = totdoc;
		model.kernel_parm = kernel_parm;
		model.sv_num = 1;
		model.loo_error = -1;
		model.loo_recall = -1;
		model.loo_precision = -1;
		model.xa_error = -1;
		model.xa_recall = -1;
		model.xa_precision = -1;

		// logger.info("learn_parm.shared:" + learn_parm.sharedslack);
		r_delta_avg = estimate_r_delta_average(docs, totdoc, kernel_parm);

		if (learn_parm.svm_c == 0) {

			learn_parm.svm_c = 1.0 / (r_delta_avg * r_delta_avg);
			if (svm_common.verbosity >= 1) {
				System.out.println("Setting default regularization parameter C="+ learn_parm.svm_c);
			}
		}

		learn_parm.biased_hyperplane = 0;

		learn_parm.eps = 0.0;

		for (i = 0; i < totdoc; i++) {
			docs[i].docnum = i;
			a[i] = 0;
			lin[i] = 0;
			c[i] = rhs[i];
			// System.out.println("the c[i] is "+c[i]);
			unlabeled[i] = 0;
			inconsistent[i] = 0;
			learn_parm.svm_cost[i] = learn_parm.svm_c* learn_parm.svm_costratio * docs[i].costfactor;
			label[i] = 1;
		}

		if (learn_parm.sharedslack != 0) {
			for (i = 0; i < totdoc; i++) {
				if (docs[i].slackid == 0) {
					System.err.println("Error: Missing shared slacks definitions in some of the examples.");
					System.exit(0);
				}
			}
		}

		if (alpha != null) {
			/*
			 * if(svm_common.verbosity>=1) {
			 * System.out.println("Computing starting state..."); }
			 */

			index = new int[totdoc];
			index2dnum = new int[totdoc + 11];
			if (kernel_parm.kernel_type == svm_common.LINEAR) {
				weights = new double[totwords + 1];
				svm_common.clear_nvector(weights, totwords);
				aicache = null;
			} else {
				weights = null;
				aicache = new double[totdoc];
			}

			for (i = 0; i < totdoc; i++) {
				index[i] = 1;
				alpha[i] = Math.abs(alpha[i]);
				if (alpha[i] < 0) {
					alpha[i] = 0;
				}

				if (alpha[i] > learn_parm.svm_cost[i]) {
					alpha[i] = learn_parm.svm_cost[i];
				}
			}

			if ((kernel_cache != null)
					&& (kernel_parm.kernel_type != svm_common.LINEAR)) {

				/*** fill kernel cache with unbounded SV ****/
				for (i = 0; i < totdoc; i++) {
					if ((alpha[i] > 0) && (alpha[i] < learn_parm.svm_cost[i])
							&& (kernel_cache_space_available(kernel_cache))) {
						cache_kernel_row(kernel_cache, docs, i, kernel_parm);
					}
				}

				for (i = 0; i < totdoc; i++) {
					if ((alpha[i] == learn_parm.svm_cost[i])
							&& (kernel_cache_space_available(kernel_cache))) {
						cache_kernel_row(kernel_cache, docs, i, kernel_parm);
					}
				}
			}

			compute_index(index, totdoc, index2dnum);
			update_linear_component(docs, label, index2dnum, alpha, a,
					index2dnum, totdoc, totwords, kernel_parm, kernel_cache,
					lin, aicache, weights);
			/*
			 * String a_str=""; String c_str=""; for(int t=0;t<totdoc;t++) {
			 * a_str+=t+":"+a[t]+" "; c_str+=t+":"+c[t]+" "; }
			 * 
			 * logger.info("a_str:"+a_str); logger.info("c_str:"+c_str);
			 */
			// for(int t=0;t<totdoc;t++)
			// {
			// logger.info("doc:"+t+"  "+docs[t].fvec.toString());
			// }
			// logger.info("learn_parm.svm_maxqpsize 11:"+learn_parm.svm_maxqpsize+" learn_parm.svm_newvarsinqp:"+learn_parm.svm_newvarsinqp);

			calculate_svm_model(docs, label, unlabeled, lin, alpha, a, c,
					learn_parm, index2dnum, index2dnum, model);

			// logger.info("learn_parm.svm_maxqpsize 12:"+learn_parm.svm_maxqpsize+" learn_parm.svm_newvarsinqp:"+learn_parm.svm_newvarsinqp);
			for (i = 0; i < totdoc; i++) {
				a[i] = alpha[i];
			}
			/*
			 * String astr=""; logger.info("astr:"); for(i=0;i<totdoc;i++) {
			 * astr+=(i+":"+a[i]+" "); } logger.info(astr);
			 */
		}

		if (learn_parm.remove_inconsistent != 0) {
			learn_parm.remove_inconsistent = 0;
			System.out
					.println("'remove inconsistent' not available in this mode. Switching option off!");
		}

		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			/** kernel_cache=NULL; **/
		}

		if (svm_common.verbosity == 1) {
			System.out.println("Optimizing");
		}
		// // learn_parm.sharedslack=0;
		if (learn_parm.sharedslack != 0) {
			 //logger.info("learn  sharedslack");
			iterations = optimize_to_convergence_sharedslack(docs, label,
					totdoc, totwords, learn_parm, kernel_parm, kernel_cache,
					shrink_state, model, a, lin, c, timing_profile);

		} else {
			 //logger.info("learn not sharedslack");
			// logger.info("learn_parm.svm_maxqpsize2:"+learn_parm.svm_maxqpsize+" learn_parm.svm_newvarsinqp:"+learn_parm.svm_newvarsinqp);
			iterations = optimize_to_convergence(docs, label, totdoc, totwords,
					learn_parm, kernel_parm, kernel_cache, shrink_state, model,
					inconsistent, unlabeled, a, lin, c, timing_profile, -1, 1);

		}

		if (svm_common.verbosity >= 1) {
			if (svm_common.verbosity == 1) {
				System.out.println("done. (" + iterations + " iterations)\n");
			}
			misclassified = 0;
			for (i = 0; (i < totdoc); i++) { /* get final statistic */
				if ((lin[i] - model.b) * (double) label[i] <= 0.0)
					misclassified++;
			}

			System.out.println("Optimization finished (maxdiff=" + maxdiff
					+ ").");

			runtime_end = svm_common.get_runtime();
			if (svm_common.verbosity >= 2) {
				System.out.println("Runtime in cpu-seconds:"
						+ (runtime_end - runtime_start) / 100.0 + " ("
						+ (100.0 * timing_profile.time_kernel)
						/ (runtime_end - runtime_start) + " for kernel/"
						+ (100.0 * timing_profile.time_opti)
						/ (runtime_end - runtime_start) + " for optimizer/"
						+ (100.0 * timing_profile.time_shrink)
						/ (runtime_end - runtime_start) + " for final/"
						+ (100.0 * timing_profile.time_update)
						/ (runtime_end - runtime_start) + " for update/"
						+ (100.0 * timing_profile.time_model)
						/ (runtime_end - runtime_start) + " for model/"
						+ (100.0 * timing_profile.time_check)
						/ (runtime_end - runtime_start) + " for check/"
						+ (100.0 * timing_profile.time_select)
						/ (runtime_end - runtime_start) + " for select)\n");
			} else {
				System.out.println("Runtime in cpu-seconds: "
						+ (runtime_end - runtime_start) / 100.0);
			}
		}

		if ((svm_common.verbosity >= 1)
				&& (learn_parm.skip_final_opt_check == 0)) {
			loss = 0;
			model_length = 0;
			alphasum = 0;
			for (i = 0; i < totdoc; i++) {
				if ((lin[i] - model.b) * (double) label[i] < c[i]
						- learn_parm.epsilon_crit) {
					loss += c[i] - (lin[i] - model.b) * (double) label[i];
				}
				model_length += a[i] * label[i] * lin[i];
				alphasum += rhs[i] * a[i];
			}
			model_length = Math.sqrt(model_length);
			System.out.println("Dual objective value: dval="
					+ (alphasum - 0.5 * model_length * model_length));
			System.out.println("Norm of weight vector: |w|=" + model_length);
		}

		if (learn_parm.sharedslack != 0) {
			index = new int[totdoc];
			index2dnum = new int[totdoc + 11];
			maxslackid = 0;
			for (i = 0; i < totdoc; i++) { /* create full index */
				index[i] = 1;
				if (maxslackid < docs[i].slackid) {
					maxslackid = docs[i].slackid;
				}
			}
			compute_index(index, totdoc, index2dnum);
			slack = new double[maxslackid + 1];
			alphaslack = new double[maxslackid + 1];
			for (i = 0; i <= maxslackid; i++) { /* init shared slacks */
				slack[i] = 0;
				alphaslack[i] = 0;
			}
			for (i = 0; i < totdoc; i++) { /* compute alpha aggregated by slack */
				alphaslack[docs[i].slackid] += a[i];
			}
			compute_shared_slacks(docs, label, a, lin, c, index2dnum,
					learn_parm, slack, alphaslack);
			loss = 0;
			model.at_upper_bound = 0;
			svsetnum = 0;
			for (i = 0; i <= maxslackid; i++) { /* create full index */
				loss += slack[i];
				if (alphaslack[i] > (learn_parm.svm_c - learn_parm.epsilon_a)) {
					model.at_upper_bound++;
				}
				if (alphaslack[i] > learn_parm.epsilon_a)
					svsetnum++;
			}
		}

		if ((svm_common.verbosity >= 1)
				&& (learn_parm.skip_final_opt_check == 0)) {
			if (learn_parm.sharedslack != 0) {
				System.out.println("Number of SV: " + (model.sv_num - 1));
				System.out.println("Number of non-zero slack variables: "
						+ model.at_upper_bound + " (" + svsetnum
						+ " slacks have non-zero alpha)\n");
				System.out.println("L1 loss: loss=" + loss);
			} else {
				upsupvecnum = 0;
				for (i = 1; i < model.sv_num; i++) {
					if (Math.abs(model.alpha[i]) >= (learn_parm.svm_cost[(model.supvec[i]).docnum] - learn_parm.epsilon_a)) {
						upsupvecnum++;
					}
				}
				System.out.println("Number of SV: " + (model.sv_num - 1)
						+ " (including " + upsupvecnum + " at upper bound)");
			}
			example_length = estimate_sphere(model);
			System.out.println("Norm of longest example vector: |x|="
					+ length_of_longest_document_vector(docs, totdoc,
							kernel_parm));
		}

		if (svm_common.verbosity >= 1) {
			System.out.println("Number of kernel evaluations: "
					+ kernel_cache_statistic);
		}

		if (alpha != null) {
			for (i = 0; i < totdoc; i++) { /* copy final alphas */
				alpha[i] = a[i];
			}
		}

		if (learn_parm.alphafile != null) {
			write_alphas(learn_parm.alphafile, a, label, totdoc);
		}
		/*
		 * String model_alpha_str = ""; for (i = 0; i < totdoc; i++) {
		 * model_alpha_str += (i + ":" + model.alpha[i] + " "); }
		 * logger.info("model alpha_trace:" + model_alpha_str);
		 * 
		 * String alpha_str = ""; for (i = 0; i < totdoc; i++) { alpha_str += (i
		 * + ":" + alpha[i] + " "); } logger.info("alpha_trace:" + alpha_str);
		 */
		if (alpha == null) {
			alpha_g = alpha;
		} else {
			alpha_g = new double[alpha.length];
			for (int j = 0; j < alpha_g.length; j++) {
				alpha_g[j] = alpha[j];
			}
		}
		
		if(shrink_state!=null)
		{
		  shrink_state_cleanup(shrink_state);
		}
		label=null;
		unlabeled=null;
		inconsistent=null;
		c=null;
		a=null;
		lin=null;
		if(learn_parm!=null)
		{
		  learn_parm.svm_cost=null;
		}

	}

	public void shrink_state_cleanup(SHRINK_STATE shrink_state)
	{
		shrink_state.active=null;
		shrink_state.inactive_since=null;
		
		if(shrink_state.deactnum > 0) 
			shrink_state.a_history[shrink_state.deactnum-1]=null;
		shrink_state.a_history=null;
		shrink_state.last_a=null;
		shrink_state.last_lin=null;
	}
	
	public void init_shrink_state(SHRINK_STATE shrink_state, int totdoc,
			int maxhistory) {
		int i;

		shrink_state.deactnum = 0;
		shrink_state.active = new int[totdoc];
		shrink_state.inactive_since = new int[totdoc];
		shrink_state.a_history = new double[maxhistory][];
		shrink_state.maxhistory = maxhistory;
		shrink_state.last_lin = new double[totdoc];
		shrink_state.last_a = new double[totdoc];

		for (i = 0; i < totdoc; i++) {
			shrink_state.active[i] = 1;
			shrink_state.inactive_since[i] = 0;
			shrink_state.last_a[i] = 0;
			shrink_state.last_lin[i] = 0;
		}

	}

	public void update_linear_component(DOC[] docs, int[] label,
			int[] active2dnum, double[] a, double[] a_old, int[] working2dnum,
			int totdoc, int totwords, KERNEL_PARM kernel_parm,
			KERNEL_CACHE kernel_cache, double[] lin, double[] aicache,
			double[] weights) {
		int i, ii, j, jj;
		double tec;
		SVECTOR f;

		if (kernel_parm.kernel_type == 0) {
			// System.out.println("working2dnum length:"+working2dnum.length);
			for (ii = 0; (i = working2dnum[ii]) >= 0; ii++) {
				// System.out.println("working i:"+i);
				if (a[i] != a_old[i]) {
					for (f = docs[i].fvec; f != null; f = f.next) {
						// System.out.println("docs["+i+"] fvec:"+docs[i].fvec.words.length);
						svm_common.add_vector_ns(weights, f, f.factor
								* ((a[i] - a_old[i]) * label[i]));
						// for(int wi=0;wi<weights.length;wi++)
						// {
						// System.out.println("weights["+wi+"]="+weights[wi]);
						// }
					}
				}
			}

			for (jj = 0; (j = active2dnum[jj]) >= 0; jj++) {
				for (f = docs[j].fvec; f != null; f = f.next) {
					lin[j] += f.factor * svm_common.sprod_ns(weights, f);
					// lin[j] =
					// WeightsUpdate.sum(lin[j],WeightsUpdate.mul(f.factor,
					// svm_common.sprod_ns(weights, f)));
				}
			}

			for (ii = 0; (i = working2dnum[ii]) >= 0; ii++) {
				if (a[i] != a_old[i]) {
					for (f = docs[i].fvec; f != null; f = f.next) {
						svm_common.mult_vector_ns(weights, f, 0.0);
					}
				}
			}
		} else {
			for (jj = 0; (i = working2dnum[jj]) >= 0; jj++) {
				if (a[i] != a_old[i]) {
					get_kernel_row(kernel_cache, docs, i, totdoc, active2dnum,
							aicache, kernel_parm);
					for (ii = 0; (j = active2dnum[ii]) >= 0; ii++) {
						tec = aicache[j];
						lin[j] += (((a[i] * tec) - (a_old[i] * tec)) * (double) label[i]);
						// lin[j] =
						// WeightsUpdate.sum(lin[j],WeightsUpdate.mul(WeightsUpdate.sub(WeightsUpdate.mul(a[i],
						// tec), WeightsUpdate.mul(a_old[i], tec)), (double)
						// label[i]));
					}
				}
			}
		}
		
		/*
		String lin_str = "";
		for (int li = 0; li < lin.length; li++) {
			lin_str += li + ":" + lin[li] + " ";
		}
        */
		// logger.info("lin_str:"+lin_str);
	}

	public double estimate_r_delta_average(DOC[] docs, int totdoc,
			KERNEL_PARM kernel_parm) {
		int i;
		double avgxlen;
		DOC nulldoc;
		WORD[] nullword = new WORD[1];
		nullword[0] = new WORD();
		nullword[0].wnum = 0;
		nulldoc = svm_common.create_example(-2, 0, 0, 0.0,
				svm_common.create_svector(nullword, "", 1.0));
		avgxlen = 0;

		for (i = 0; i < totdoc; i++) {
			avgxlen += Math.sqrt(sc.kernel(kernel_parm, docs[i],
					docs[i])
					- 2
					* sc.kernel(kernel_parm, docs[i], nulldoc)
					+ sc.kernel(kernel_parm, nulldoc, nulldoc));
		}

		return (avgxlen / totdoc);
	}

	public boolean kernel_cache_space_available(KERNEL_CACHE kernel_cache) {
		if (kernel_cache.elems < kernel_cache.max_elems) {
			return true;
		} else {
			return false;
		}
	}

	public void cache_kernel_row(KERNEL_CACHE kernel_cache, DOC[] docs, int m,
			KERNEL_PARM kernel_parm) {
		DOC ex;
		int j, k, l;
		double[] cache;

		if (!kernel_cache_check(kernel_cache, m)) {
			cache = kernel_cache_clean_and_malloc(kernel_cache, m);
			if (cache != null) {
				l = kernel_cache.totdoc2active[m];
				ex = docs[m];
				for (j = 0; j < kernel_cache.activenum; j++) {
					k = kernel_cache.active2totdoc[j];
					if ((kernel_cache.index[k] != -1) && (l != -1) && (k != m)) {
						cache[j] = kernel_cache.buffer[kernel_cache.activenum
								* kernel_cache.index[k] + l];
					} else {
						cache[j] = sc.kernel(kernel_parm, ex, docs[k]);
					}

				}

			} else {
				System.err
						.println("Error: Kernel cache full! => increase cache size");
			}
		}

	}

	public boolean kernel_cache_check(KERNEL_CACHE kernel_cache, int docnum) {
		return (kernel_cache.index[docnum] != -1);
	}

	public double[] kernel_cache_clean_and_malloc(KERNEL_CACHE kernel_cache,
			int docnum) {
		int result;
		if ((result = kernel_cache_malloc(kernel_cache)) == -1) {
			if (kernel_cache_free_lru(kernel_cache)) {
				result = kernel_cache_malloc(kernel_cache);
			}
		}

		kernel_cache.index[docnum] = result;
		if (result == -1) {
			return null;
		}

		kernel_cache.invindex[result] = docnum;
		kernel_cache.lru[kernel_cache.index[docnum]] = kernel_cache.time;

		return new double[kernel_cache.buffer.length
				+ (kernel_cache.activenum * kernel_cache.index[docnum])];
	}

	public int kernel_cache_malloc(KERNEL_CACHE kernel_cache) {
		int i;
		if (kernel_cache_space_available(kernel_cache)) {
			for (i = 0; i < kernel_cache.max_elems; i++) {
				if (kernel_cache.occu[i] == 0) {
					kernel_cache.occu[i] = 1;
					kernel_cache.elems++;
					return i;
				}
			}
		}

		return -1;
	}

	public boolean kernel_cache_free_lru(KERNEL_CACHE kernel_cache) {
		int k, least_elem = -1, least_time;
		least_time = kernel_cache.time + 1;

		for (k = 0; k < kernel_cache.max_elems; k++) {
			if (kernel_cache.invindex[k] != -1) {
				if (kernel_cache.lru[k] < least_time) {
					least_time = kernel_cache.lru[k];
					least_elem = k;
				}
			}
		}

		if (least_elem != -1) {
			kernel_cache_free(kernel_cache, least_elem);
			kernel_cache.index[kernel_cache.invindex[least_elem]] = -1;
			kernel_cache.invindex[least_elem] = -1;
			return true;
		}

		return false;
	}

	public void kernel_cache_free(KERNEL_CACHE kernel_cache, int i) {
		kernel_cache.occu[i] = 0;
		kernel_cache.elems--;
	}

	public int compute_index(int[] binfeature, int range, int[] index) {
		int i, ii;
		ii = 0;

		for (i = 0; i < range; i++) {
			if (binfeature[i] != 0) {
				index[ii] = i;
				ii++;
			}
		}

		for (i = 0; i < 4; i++) {
			index[ii + i] = -1;
		}

		return ii;
	}

	public void get_kernel_row(KERNEL_CACHE kernel_cache, DOC[] docs,
			int docnum, int totdoc, int[] active2dnum, double[] buffer,
			KERNEL_PARM kernel_parm) {
		int i, j, start;
		DOC ex;
		ex = docs[docnum];

		if ((kernel_cache != null) && (kernel_cache.index[docnum] != -1)) {
			kernel_cache.lru[kernel_cache.index[docnum]] = kernel_cache.time;
			start = kernel_cache.activenum * kernel_cache.index[docnum];
			for (i = 0; (j = active2dnum[i]) >= 0; i++) {
				if (kernel_cache.totdoc2active[j] >= 0) {
					buffer[j] = kernel_cache.buffer[start
							+ kernel_cache.totdoc2active[j]];
				} else {
					buffer[j] = sc.kernel(kernel_parm, ex, docs[j]);
				}
			}
		} else {
			for (i = 0; (j = active2dnum[i]) >= 0; i++) {
				buffer[j] = sc.kernel(kernel_parm, ex, docs[j]);
			}
		}

	}

	public int calculate_svm_model(DOC[] docs, int[] label, int[] unlabeled,
			double[] lin, double[] a, double[] a_old, double[] c,
			LEARN_PARM learn_parm, int[] working2dnum, int[] active2dnum,
			MODEL model) {
		int i, ii, pos, b_calculated = 0, first_low, first_high;
		double ex_c, b_temp, b_low, b_high;
		/*
		 * if(svm_common.verbosity>=3){
		 * System.out.println("Calculating model..."); }
		 */

		if (learn_parm.biased_hyperplane == 0) {
			model.b = 0;
			b_calculated = 1;
		}
		String supvec_str = "";
		String alpha_str = "";
		String index_str = "";
		String svm_cost_str = "";
		// logger.info("learn_parm.sharedslack is "+learn_parm.sharedslack);
		// logger.info("learn_parm.epsilon_a "+learn_parm.epsilon_a);
		// logger.info("learn_parm.eps "+ learn_parm.eps);
		/*
		 * for(int k=0;k<100;k++) {
		 * 
		 * alpha_str+=(k+":"+model.alpha[k]+" ");
		 * index_str+=(k+":"+model.index[k]+" "); //
		 * svm_cost_str+=(k+":"+learn_parm.svm_cost[k]+" "); }
		 */
		// logger.info("alpha_str_before:"+alpha_str);
		// logger.info("index_str_before:"+index_str);
		// logger.info("svm_cost_str_before:"+svm_cost_str);

		supvec_str = "";
		alpha_str = "";
		index_str = "";
		svm_cost_str = "";

		for (ii = 0; (i = working2dnum[ii]) >= 0; ii++) {
			if ((a_old[i] > 0) && (a[i] == 0)) {
				/** remove from model **/
				pos = model.index[i];
				model.index[i] = -1;
				(model.sv_num)--;

				model.supvec[pos] = model.supvec[model.sv_num];
				model.alpha[pos] = model.alpha[model.sv_num];
				// logger.info("pos="+pos+"  "+ model.alpha[pos]);
				model.index[model.supvec[pos].docnum] = pos;
			} else if ((a_old[i] == 0) && (a[i] > 0)) {
				/** add to model **/
				model.supvec[model.sv_num] = docs[i];
				//logger.info("lvec i:"+i+" "+docs[i].lvecString());
				model.alpha[model.sv_num] = a[i] * ((double) label[i]);
				// logger.info("model.alpha["+model.sv_num+"]="+
				// model.alpha[model.sv_num]);
				model.index[i] = model.sv_num;
				(model.sv_num)++;
			} else if (a_old[i] == a[i]) {

			} else {
				model.alpha[model.index[i]] = a[i] * ((double) label[i]);
			}

			ex_c = learn_parm.svm_cost[i] - learn_parm.epsilon_a;

			if (learn_parm.sharedslack == 0) {
				if ((a_old[i] >= ex_c) && (a[i] < ex_c)) {
					(model.at_upper_bound)--;
				} else if ((a_old[i] < ex_c) && (a[i] >= ex_c)) {
					(model.at_upper_bound)++;
				}
			}

			if ((b_calculated == 0) && (a[i] > learn_parm.epsilon_a)
					&& (a[i] < ex_c)) {
				model.b = ((double) label[i]) * learn_parm.eps - c[i] + lin[i];
				b_calculated = 1;
			}

			/*
			if(model.supvec[i]!=null)
			{
			 supvec_str += (ii + ":" + i + ":" + model.supvec[i].lvecString() + " ");
			}
			
			alpha_str += (ii + ":" + i + ":" + model.alpha[i] + " ");
			index_str += (ii + ":" + i + ":" + model.index[i] + " ");
			svm_cost_str += (ii + ":" + i + ":" + learn_parm.svm_cost[i] + " ");
            */
		}

		// logger.info("supvec_str:"+supvec_str);
		// logger.info("alpha_str:"+alpha_str);
		// logger.info("index_str:"+index_str);
		// logger.info("svm_cost_str:"+svm_cost_str);
		// logger.info("model.sv_num:"+model.sv_num);
		// logger.info("model.at_upper_bound:"+model.at_upper_bound);

		/**
		 * No alpha in the working set not at bounds, so b was not calculated in
		 * the usual way. The following handles this special case.
		 */
		if ((learn_parm.biased_hyperplane != 0) && (b_calculated == 0)
				&& ((model.sv_num - 1) == model.at_upper_bound)) {
			first_low = 1;
			first_high = 1;
			b_low = 0;
			b_high = 0;

			for (ii = 0; (i = active2dnum[ii]) >= 0; ii++) {
				ex_c = learn_parm.svm_cost[i] - learn_parm.epsilon_a;
				if (a[i] < ex_c) {
					if (label[i] > 0) {
						b_temp = -(learn_parm.eps - c[i] + lin[i]);
						if ((b_temp > b_low) || (first_low != 0)) {
							b_low = b_temp;
							first_low = 0;
						}
					} else {
						b_temp = -(-learn_parm.eps - c[i] + lin[i]);
						if ((b_temp < b_high) || (first_high != 0)) {
							b_high = b_temp;
							first_high = 0;
						}
					}
				} else {
					if (label[i] < 0) {
						b_temp = -(-learn_parm.eps - c[i] + lin[i]);
						if ((b_temp > b_low) || (first_low != 0)) {
							b_low = b_temp;
							first_low = 0;
						}
					} else {
						b_temp = -(learn_parm.eps - c[i] + lin[i]);
						if ((b_temp < b_high) || (first_high != 0)) {
							b_high = b_temp;
							first_high = 0;
						}
					}
				}
			}// for

			if (first_high != 0) {
				model.b = -b_low;
			} else if (first_low != 0) {
				model.b = -b_high;
			} else {
				model.b = -(b_high + b_low) / 2.0;
			}

			// logger.info("b_low="+b_low+", b_high="+b_high+",b="+model.b);
		}
		/*
		 * if(svm_common.verbosity>=3){ System.out.println("done\n"); }
		 */

		return (model.sv_num - 1);
	}

	public int optimize_to_convergence(DOC[] docs, int[] label, int totdoc,
			int totwords, LEARN_PARM learn_parm, KERNEL_PARM kernel_parm,
			KERNEL_CACHE kernel_cache, SHRINK_STATE shrink_state, MODEL model,
			int[] inconsistent, int[] unlabeled, double[] a, double[] lin,
			double[] c, TIMING timing_profile, int heldout, int retrain) {

		 logger.info("in optimize_to_convergence");
		/*
		 * if(docs!=null) { logger.info("docs.size:"+docs.length); for(int
		 * i=0;i<docs.length;i++) {
		 * logger.info(i+" doc fvec:"+docs[i].fvec.toString());
		 * logger.info(" label:"+label[i]); } }
		 */
		int[] chosen;
		int[] key;
		int i, j, jj;
		int[] last_suboptimal_at;
		double noshrink;
		svm_common.verbosity = 0;
		int inconsistentnum, choosenum, already_chosen = 0, iteration;
		int misclassified, supvecnum = 0;
		int[] active2dnum;
		int inactivenum;
		int[] working2dnum;
		int[] selexam;
		int activenum;
		double criterion;
		double eq;
		double[] a_old;
		double t0 = 0, t1 = 0, t2 = 0, t3 = 0, t4 = 0, t5 = 0, t6 = 0;
		int transductcycle;
		int transduction;
		double epsilon_crit_org;
		double bestmaxdiff;
		int bestmaxdiffiter, terminate;

		double[] selcrit;
		double[] aicache;
		double[] weights;
		QP qp = null;

		qp = new QP();

		epsilon_crit_org = learn_parm.epsilon_crit;
		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			learn_parm.epsilon_crit = 2.0;
		}
		learn_parm.epsilon_shrink = 2;
		maxdiff = 1;

		learn_parm.totwords = totwords;
		// System.out.println("totdoc otc:"+totdoc);
		chosen = new int[totdoc];
		last_suboptimal_at = new int[totdoc];
		key = new int[totdoc + 11];
		selcrit = new double[totdoc];
		selexam = new int[totdoc];
		a_old = new double[totdoc];
		aicache = new double[totdoc];
		working2dnum = new int[totdoc + 11];
		active2dnum = new int[totdoc + 11];

		qp.opt_ce = new double[learn_parm.svm_maxqpsize];
		qp.opt_ce0 = new double[1];
		qp.opt_g = new double[(learn_parm.svm_maxqpsize)
				* (learn_parm.svm_maxqpsize)];
		qp.opt_g0 = new double[learn_parm.svm_maxqpsize];
		qp.opt_xinit = new double[learn_parm.svm_maxqpsize];
		qp.opt_low = new double[learn_parm.svm_maxqpsize];
		qp.opt_up = new double[learn_parm.svm_maxqpsize];

		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			weights = svm_common.create_nvector(totwords);
			svm_common.clear_nvector(weights, totwords);
		} else {
			weights = null;
		}

		choosenum = 0;
		inconsistentnum = 0;
		transductcycle = 0;
		transduction = 0;

		if (retrain == 0) {
			retrain = 1;
		}

		iteration = 1;
		bestmaxdiffiter = 1;
		bestmaxdiff = 999999999;
		terminate = 0;

		if (kernel_cache != null) {
			kernel_cache.time = iteration;
			kernel_cache_reset_lru(kernel_cache);
		}

		for (i = 0; i < totdoc; i++) {
			chosen[i] = 0;
			a_old[i] = a[i];
			last_suboptimal_at[i] = 1;
			// logger.info("inconsistent["+i+"]="+inconsistent[i]);
			if (inconsistent[i] != 0) {
				inconsistentnum++;
			}

			if (unlabeled[i] != 0) {
				transduction = 1;
			}
		}

		activenum = compute_index(shrink_state.active, totdoc, active2dnum);
		inactivenum = totdoc - activenum;
		clear_index(working2dnum);

		/** main loop ***/
		for (; (retrain != 0) && (terminate == 0); iteration++) {
			if (kernel_cache != null) {
				kernel_cache.time = iteration;
			}

			if (svm_common.verbosity >= 2) {
				logger.info("Iteration " + iteration);
			} else if (svm_common.verbosity == 1) {
				logger.info(".");
			}

			if (svm_common.verbosity >= 2) {
				t0 = svm_common.get_runtime();
			}

			if (svm_common.verbosity >= 3) {
				logger.info("Selecting working set... ");
			}

			if (learn_parm.svm_newvarsinqp > learn_parm.svm_maxqpsize) {
				learn_parm.svm_newvarsinqp = learn_parm.svm_maxqpsize;
			}

			i = 0;

			for (jj = 0; (j = working2dnum[jj]) >= 0; jj++) {
				// logger.info("j="+j);
				// logger.info("learn_parm.svm_maxqpsize/Math.min(learn_parm.svm_maxqpsize, learn_parm.svm_newvarsinqp))):"+(chosen[j]>=(learn_parm.svm_maxqpsize/Math.min(learn_parm.svm_maxqpsize,
				// learn_parm.svm_newvarsinqp))));
				// logger.info("j==heldout:"+(j==heldout));
				if ((chosen[j] >= (learn_parm.svm_maxqpsize / Math.min(
						learn_parm.svm_maxqpsize, learn_parm.svm_newvarsinqp)))
						|| (inconsistent[j] != 0) || (j == heldout)) {
					chosen[j] = 0;
					choosenum--;
				} else {
					chosen[j]++;
					working2dnum[i++] = j;
					// logger.info("working2dum11:" + (i - 1) + " j:" + j);
				}
			}

			working2dnum[i] = -1;
			// logger.info("working2dum[" + i + "] is -1");
			if (retrain == 2) {
				choosenum = 0;
				for (jj = 0; (j = working2dnum[jj]) >= 0; jj++) {
					chosen[j] = 0;
				}

				clear_index(working2dnum);

				for (i = 0; i < totdoc; i++) {
					if (((inconsistent[i] > 0) || (heldout == i))
							&& (a[i] != 0)) {
						chosen[i] = 99999;
						choosenum++;
						a[i] = 0;
					}
				}

				if (learn_parm.biased_hyperplane != 0) {
					eq = 0;
					for (i = 0; i < totdoc; i++) {
						eq += a[i] * label[i];
					}

					for (i = 0; (i < totdoc)
							&& (Math.abs(eq) > learn_parm.epsilon_a); i++) {
						if ((eq * label[i] > 0) && (a[i] > 0)) {
							chosen[i] = 88888;
							choosenum++;
							if ((eq * label[i]) > a[i]) {
								eq -= (a[i] * label[i]);
								a[i] = 0;
							} else {
								a[i] -= (eq * label[i]);
								eq = 0;
							}
						}
					}
				}
				// logger.info("begin compute index");
				compute_index(chosen, totdoc, working2dnum);

			}// retrain==2
			else {
				if ((iteration % 101) != 0) {
					// logger.info("in iteration 101");
					already_chosen = 0;
					// System.out.println("already_chosen begin");
					// System.out.println("learn_parm.svm_newvarsinqp:"+learn_parm.svm_newvarsinqp);
					// System.out.println("learn_parm.svm_maxqpsize:"+learn_parm.svm_maxqpsize);
					// System.out.println("kernel_parm.kernel_type:"+kernel_parm.kernel_type);
					if ((Math.min(learn_parm.svm_newvarsinqp,
							learn_parm.svm_maxqpsize - choosenum) >= 4)
							&& (kernel_parm.kernel_type != svm_common.LINEAR)) {
						// logger.info("minl");
						already_chosen = select_next_qp_subproblem_grad(label,
								unlabeled, a, lin, c, totdoc, (Math.min(
										learn_parm.svm_maxqpsize - choosenum,
										learn_parm.svm_newvarsinqp) / 2),
								learn_parm, inconsistent, active2dnum,
								working2dnum, selcrit, selexam, kernel_cache,
								1, key, chosen);

						// logger.info("choose num:" + choosenum);
						choosenum += already_chosen;
					}

					choosenum += select_next_qp_subproblem_grad(
							label,
							unlabeled,
							a,
							lin,
							c,
							totdoc,
							Math.min(learn_parm.svm_maxqpsize - choosenum,
									learn_parm.svm_newvarsinqp - already_chosen),
							learn_parm, inconsistent, active2dnum,
							working2dnum, selcrit, selexam, kernel_cache, 0,
							key, chosen);
				
					for (int wi = 0; wi < working2dnum.length; wi++) {
						if (working2dnum[wi] > -1) {
							// System.out.println("select working2dnum:"+wi+"  "+working2dnum[wi]);
						}
					}
					// System.out.println("choose num linear:"+choosenum);
				} else {
					// logger.info("in else");
					choosenum += select_next_qp_subproblem_rand(label,
							unlabeled, a, lin, c, totdoc, Math.min(
									learn_parm.svm_maxqpsize - choosenum,
									learn_parm.svm_newvarsinqp), learn_parm,
							inconsistent, active2dnum, working2dnum, selcrit,
							selexam, kernel_cache, key, chosen, iteration);
				}
			}

			if (svm_common.verbosity >= 2) {
				// System.out.println(choosenum+" vectors chosen");
				t1 = svm_common.get_runtime();
			}

			if (kernel_cache != null) {
				cache_multiple_kernel_rows(kernel_cache, docs, working2dnum,
						choosenum, kernel_parm);
			}

			if (svm_common.verbosity >= 2) {
				t2 = svm_common.get_runtime();
			}

			if (retrain != 2) {
			
				optimize_svm(docs, label, unlabeled, inconsistent, 0.0, chosen,
						active2dnum, model, totdoc, working2dnum, choosenum, a,
						lin, c, learn_parm, aicache, kernel_parm, qp,
						epsilon_crit_org);
		
			}

			if (svm_common.verbosity >= 2) {
				t3 = svm_common.get_runtime();
			}

			// for(int di=0;di<docs.length;di++)
			// {
			// if(docs[di]!=null)
			// System.out.println("docs di["+di+"]="+docs[di].fvec.words.length);
			// }
			update_linear_component(docs, label, active2dnum, a, a_old,
					working2dnum, totdoc, totwords, kernel_parm, kernel_cache,
					lin, aicache, weights);

			if (svm_common.verbosity >= 2) {
				t4 = svm_common.get_runtime();
			}

			supvecnum = calculate_svm_model(docs, label, unlabeled, lin, a,
					a_old, c, learn_parm, working2dnum, active2dnum, model);

			if (svm_common.verbosity >= 2) {
				t5 = svm_common.get_runtime();
			}

			if (svm_common.verbosity >= 3) {

				criterion = compute_objective_function(a, lin, c,
						learn_parm.eps, label, active2dnum);
			}

			for (jj = 0; (i = working2dnum[jj]) >= 0; jj++) {
				a_old[i] = a[i];
			}

			if (retrain == 2) { /* reset inconsistent unlabeled examples */
				for (i = 0; (i < totdoc); i++) {
					if ((inconsistent[i] != 0) && (unlabeled[i] != 0)) {
						inconsistent[i] = 0;
						label[i] = 0;
					}
				}
			}
		
			retrain = check_optimality(model, label, unlabeled, a, lin, c,
					totdoc, learn_parm, epsilon_crit_org, inconsistent,
					active2dnum, last_suboptimal_at, iteration, kernel_parm);
		
			if (svm_common.verbosity >= 2) {
				t6 = svm_common.get_runtime();
				timing_profile.time_select += t1 - t0;
				timing_profile.time_kernel += t2 - t1;
				timing_profile.time_opti += t3 - t2;
				timing_profile.time_update += t4 - t3;
				timing_profile.time_model += t5 - t4;
				timing_profile.time_check += t6 - t5;
			}

			/* checking whether optimizer got stuck */
			if (maxdiff < bestmaxdiff) {
				bestmaxdiff = maxdiff;
				bestmaxdiffiter = iteration;
			}

			if (iteration > (bestmaxdiffiter + learn_parm.maxiter)) {
				/* long time no progress? */
				terminate = 1;
				retrain = 0;
				if (svm_common.verbosity >= 1) {
					logger.info("\nWARNING: Relaxing KT-Conditions due to slow progress! Terminating!");
				}
			}

			noshrink = 0;

			if ((retrain == 0)
					&& (inactivenum > 0)
					&& ((learn_parm.skip_final_opt_check == 0) || (kernel_parm.kernel_type == svm_common.LINEAR))) {

				if (((svm_common.verbosity >= 1) && (kernel_parm.kernel_type != svm_common.LINEAR))
						|| (svm_common.verbosity >= 2)) {
					if (svm_common.verbosity == 1) {
						logger.info("");
					}
					logger.info(" Checking optimality of inactive variables...");
				}

				t1 = svm_common.get_runtime();

		
				reactivate_inactive_examples(label, unlabeled, a, shrink_state,
						lin, c, totdoc, totwords, iteration, learn_parm,
						inconsistent, docs, kernel_parm, kernel_cache, model,
						aicache, weights);
			
				activenum = compute_index(shrink_state.active, totdoc,
						active2dnum);
				inactivenum = totdoc - activenum;
				/* reset watchdog */
				bestmaxdiff = maxdiff;
				bestmaxdiffiter = iteration;

				/* termination criterion */
				noshrink = 1;
				retrain = 0;
				if (maxdiff > learn_parm.epsilon_crit) {
					retrain = 1;
				}
				timing_profile.time_shrink += svm_common.get_runtime() - t1;
				if (((svm_common.verbosity >= 1) && (kernel_parm.kernel_type != svm_common.LINEAR))
						|| (svm_common.verbosity >= 2)) {
					logger.info("done.");
					logger.info(" Number of inactive variables = "
							+ inactivenum);
				}
			}

			if ((retrain == 0) && (learn_parm.epsilon_crit > maxdiff)) {
				learn_parm.epsilon_crit = maxdiff;
			}

			if ((retrain == 0) && (learn_parm.epsilon_crit > epsilon_crit_org)) {
				learn_parm.epsilon_crit /= 2.0;
				retrain = 1;
				noshrink = 1;
			}
			if (learn_parm.epsilon_crit < epsilon_crit_org) {
				learn_parm.epsilon_crit = epsilon_crit_org;
			}

			if (svm_common.verbosity >= 2) {
				logger.info(" => (" + supvecnum + model.at_upper_bound
						+ "SV (incl. " + maxdiff
						+ " SV at u-bound), max violation=%.5f)");

			}
			if (svm_common.verbosity >= 3) {
				logger.info("");
			}

			if ((retrain == 0) && (transduction != 0)) {
				for (i = 0; (i < totdoc); i++) {
					shrink_state.active[i] = 1;
				}
				activenum = compute_index(shrink_state.active, totdoc,
						active2dnum);
				inactivenum = 0;
				if (svm_common.verbosity == 1) {
					// logger.info("done");
				}
			
				retrain = incorporate_unlabeled_examples(model, label,
						inconsistent, unlabeled, a, lin, totdoc, selcrit,
						selexam, key, transductcycle, kernel_parm, learn_parm);
			
				epsilon_crit_org = learn_parm.epsilon_crit;
				if (kernel_parm.kernel_type == svm_common.LINEAR) {
					learn_parm.epsilon_crit = 1;
				}
				transductcycle++;
				/* reset watchdog */
				bestmaxdiff = maxdiff;
				bestmaxdiffiter = iteration;
			} else if (((iteration % 10) == 0) && (noshrink == 0)) {
		
				activenum = shrink_problem(
						docs,
						learn_parm,
						shrink_state,
						kernel_parm,
						active2dnum,
						last_suboptimal_at,
						iteration,
						totdoc,
						Math.max((activenum / 10),
								Math.max((totdoc / 500), 100)), a, inconsistent);
			
				inactivenum = totdoc - activenum;
				if ((kernel_cache != null)
						&& (supvecnum > kernel_cache.max_elems)
						&& ((kernel_cache.activenum - activenum) > Math.max(
								(activenum / 10), 500))) {
					kernel_cache_shrink(kernel_cache, totdoc, Math.min(
							(kernel_cache.activenum - activenum),
							(kernel_cache.activenum - supvecnum)),
							shrink_state.active);
				}
			}

			if ((retrain == 0) && (learn_parm.remove_inconsistent != 0)) {
				if (svm_common.verbosity >= 1) {
					logger.info(" Moving training errors to inconsistent examples...");
				}
				if (learn_parm.remove_inconsistent == 1) {
			
					retrain = identify_inconsistent(a, label, unlabeled,
							totdoc, learn_parm, inconsistent);
				

				} else if (learn_parm.remove_inconsistent == 2) {
					retrain = identify_misclassified(lin, label, unlabeled,
							totdoc, model, inconsistent);
				} else if (learn_parm.remove_inconsistent == 3) {
					retrain = identify_one_misclassified(lin, label, unlabeled,
							totdoc, model, inconsistent);
				}
				if (retrain != 0) {
					if (kernel_parm.kernel_type == svm_common.LINEAR) { /*
																		 * reinit
																		 * shrinking
																		 */
						learn_parm.epsilon_crit = 2.0;
					}
				}
				if (svm_common.verbosity >= 1) {
					logger.info("done.");
					if (retrain != 0) {
						logger.info(" Now " + inconsistentnum
								+ " inconsistent examples.");
					}
				}
			}
		}// end of loop

		learn_parm.epsilon_crit = epsilon_crit_org;
		model.maxdiff = maxdiff;

		

		return iteration;
	}

	public void kernel_cache_reset_lru(KERNEL_CACHE kernel_cache) {
		int maxlru = 0, k;
		for (k = 0; k < kernel_cache.max_elems; k++) {
			if (maxlru < kernel_cache.lru[k]) {
				maxlru = kernel_cache.lru[k];
			}
		}

		for (k = 0; k < kernel_cache.max_elems; k++) {
			kernel_cache.lru[k] -= maxlru;
		}

	}

	public void clear_index(int[] index) {
		for (int i = 0; i < index.length; i++) {
			index[i] = -1;
		}
	}

	/**
	 * Working set selection
	 * 
	 * @param label
	 * @param unlabeled
	 * @param a
	 * @param lin
	 * @param c
	 * @param totdoc
	 * @param qp_size
	 * @param learn_parm
	 * @param inconsistent
	 * @param active2dnum
	 * @param working2dnum
	 * @param selcrit
	 * @param select
	 * @param kernel_cache
	 * @param cache_only
	 * @param key
	 * @param chosen
	 * @return
	 */
	public int select_next_qp_subproblem_grad(int[] label, int[] unlabeled,
			double[] a, double[] lin, double[] c, int totdoc, int qp_size,
			LEARN_PARM learn_parm, int[] inconsistent, int[] active2dnum,
			int[] working2dnum, double[] selcrit, int[] select,
			KERNEL_CACHE kernel_cache, int cache_only, int[] key, int[] chosen) {

		int choosenum, i, j, k, activedoc, inum, valid;
		double s;

		for (inum = 0; working2dnum[inum] >= 0; inum++)
			;
		// System.out.println("inum:"+inum);
		choosenum = 0;
		activedoc = 0;

		for (i = 0; (j = active2dnum[i]) >= 0; i++) {
			//logger.info("i=" + i + " j=" + j);
			s = -label[j];
			if ((kernel_cache != null) && (cache_only != 0)) {
				if (kernel_cache.index[j] >= 0) {
					valid = 1;
				} else {
					valid = 0;
				}
			} else {
				valid = 1;
			}

			if ((valid != 0)
					&& (!((a[j] <= (0 + learn_parm.epsilon_a)) && (s < 0)))
					&& (!((a[j] >= (learn_parm.svm_cost[j] - learn_parm.epsilon_a)) && (s > 0)))
					&& (chosen[j] == 0) && (label[j] != 0)
					&& (inconsistent[j] == 0)) {

				selcrit[activedoc] = (double) label[j]
						* (learn_parm.eps - (double) label[j] * c[j] + (double) label[j]
								* lin[j]);

				// selcrit[activedoc]=WeightsUpdate.mul((double)label[j],
				// WeightsUpdate.sum(WeightsUpdate.sub(learn_parm.eps,
				// WeightsUpdate.mul((double)label[j],
				// c[j])),WeightsUpdate.mul((double)label[j], lin[j]) ));

				//logger.info("j=" + j + " label=" + (double) label[j] + " eps="
				//		+ learn_parm.eps + " c=" + c[j] + " lin=" + lin[j]);
				//logger.info("activedoc=" + activedoc + " selcrit="
				//		+ selcrit[activedoc] + " key=" + key[activedoc]);
				//if (Math.abs(selcrit[activedoc]) > (double) (0.5)) {//0.5 set may be bug
					key[activedoc] = j;
					activedoc++;
				//}
			}
		}

		//logger.info("qp_size1:" + qp_size + " activedoc:" + activedoc);
		select_top_n(selcrit, activedoc, select, qp_size / 2);

		for (k = 0; (choosenum < ((qp_size) / 2)) && (k < (qp_size / 2))
				&& (k < activedoc); k++) {

			i = key[select[k]];
			chosen[i] = 1;
			working2dnum[inum + choosenum] = i;
			//logger.info("inum:" + inum + " choosenum:" + choosenum + " i:" + i
			//		+ " k=" + k + " select[k]=" + select[k]);
		
			choosenum += 1;
			if (kernel_cache != null) {
				kernel_cache_touch(kernel_cache, i);
			}
		}
		// System.out.println("half choosenum="+choosenum);
		activedoc = 0;
		for (i = 0; (j = active2dnum[i]) >= 0; i++) {
			s = label[j];
			if ((kernel_cache != null) && (cache_only != 0)) {
				if (kernel_cache.index[j] >= 0) {
					valid = 1;
				} else {
					valid = 0;
				}
			} else {
				valid = 1;
			}

			if (valid != 0
					&& (!((a[j] <= (0 + learn_parm.epsilon_a)) && (s < 0)))
					&& (!((a[j] >= (learn_parm.svm_cost[j] - learn_parm.epsilon_a)) && (s > 0)))
					&& (chosen[j] == 0) && (label[j] != 0)
					&& (inconsistent[j] == 0)) {
				selcrit[activedoc] = -(double) label[j]* (learn_parm.eps - (double) label[j] * c[j] + (double) label[j]* lin[j]);

				/*
				 * selcrit[activedoc]=-(double)(label[j]*(-1.0+(double)label[j]*lin
				 * [j]));
				 */
				//logger.info("activedoc=" + activedoc + "key=" + key[activedoc]);
				//if (Math.abs(selcrit[activedoc]) > (double)( 0.5)) {//0.5 set may be bug
					key[activedoc] = j;
					// logger.info("key2["+activedoc+"]="+j);
					activedoc++;
				//}
			}
		}

		/*
		 * if((sumselcrit<1E-5)&&(wloop>10)&&activedoc>2) { isTooSamll=true; }
		 */
		// logger.info("qp_size:"+qp_size);
		select_top_n(selcrit, activedoc, select, qp_size / 2);

		// for(k=0;(choosenum<qp_size) && (k<((qp_size+0.1)/2)) &&
		// (k<activedoc);k++) {
		for (k = 0; (choosenum < qp_size) && (k < ((qp_size) / 2))
				&& (k < activedoc); k++) {
			/* if(learn_parm->biased_hyperplane || (selcrit[select[k]] > 0)) { */
			i = key[select[k]];
			// logger.info("select["+k+"]="+select[k]);
			// logger.info("key[select["+k+"]]="+i);
			chosen[i] = 1;
			working2dnum[inum + choosenum] = i;
			//logger.info("tt inum:" + inum + " choosenum:" + choosenum + " i:"
			//		+ i);
			// System.out.println("inum:"+inum+" choosenum:"+choosenum);
			// logger.info("inum+choosenum2["+(inum+choosenum)+"]="+i);
			choosenum += 1;
			if (kernel_cache != null) {
				kernel_cache_touch(kernel_cache, i);
			}
			/* } */
		}

		// System.out.println("choosenum:"+choosenum);
		working2dnum[inum + choosenum] = -1; /* complete index */
		return (choosenum);
	}

	void select_top_n(double[] selcrit, int range, int[] select, int n) {

		//for (int k = 0; k < n && k < range; k++) {
			// //logger.info("selcrit[" + k + "]=" + selcrit[k] + "  select[" +
			// k
			// // + "]=" + select[k]);
		//}

		// logger.info("n=" + n + " range=" + range);

		int i, j;

		for (i = 0; (i < n) && (i < range); i++) {
			for (j = i; j >= 0; j--) {
				if ((j > 0) && (selcrit[select[j - 1]] < selcrit[i])) {
					select[j] = select[j - 1];
					// logger.info("j=" + j + " i=" + i + " select="
					// + select[j - 1] + " selcritj="
					// + selcrit[select[j - 1]] + " selcrit=" + selcrit[i]);
				} else {
					select[j] = i;
					// logger.info("j=" + j + " i=" + i);
					j = -1;
				}
			}
		}

		if (n > 0) {
			for (i = n; i < range; i++) {
				// logger.info("i=" + i + " n-1=" + (n - 1) + " select: "
				// + select[n - 1] + " s1:" + selcrit[i] + " s2:"
				// + selcrit[select[n - 1]]);
				// logger.info("sub:"+(selcrit[i]-selcrit[select[n - 1]]));

				if (selcrit[i] > selcrit[select[n - 1]]) {
					for (j = n - 1; j >= 0; j--) {
						if ((j > 0) && (selcrit[select[j - 1]] < selcrit[i])) {
							select[j] = select[j - 1];
							// logger.info("n>0 j=" + j + " i=" + i + " select="
							// + select[j - 1] + " selcritj="
							// + selcrit[select[j - 1]] + " selcrit="
							// + selcrit[i]);
						} else {
							select[j] = i;
							// logger.info("n>0 j=" + j + " i=" + i);
							j = -1;
						}
					}
				}

			}
		}
	}

	public int kernel_cache_touch(KERNEL_CACHE kernel_cache, int docnum) {
		if ((kernel_cache != null) && (kernel_cache.index[docnum] != -1)) {
			kernel_cache.lru[kernel_cache.index[docnum]] = kernel_cache.time;
			return 1;
		}

		return 0;
	}

	public int select_next_qp_subproblem_rand(int[] label, int[] unlabeled,
			double[] a, double[] lin, double[] c, int totdoc, int qp_size,
			LEARN_PARM learn_parm, int[] inconsistent, int[] active2dnum,
			int[] working2dnum, double[] selcrit, int[] select,
			KERNEL_CACHE kernel_cache, int[] key, int[] chosen, int iteration) {
		int choosenum, i, j, k, activedoc, inum;
		double s = 0;

		for (inum = 0; working2dnum[inum] >= 0; inum++)
			;
		choosenum = 0;
		activedoc = 0;

		for (i = 0; (j = active2dnum[i]) >= 0; i++) {
			s -= label[j];

			if ((!((a[j] <= (0 + learn_parm.epsilon_a)) && (s < 0)))
					&& (!((a[j] >= (learn_parm.svm_cost[j] - learn_parm.epsilon_a)) && (s > 0)))
					&& (inconsistent[j] == 0) && (label[j] != 0)
					&& (chosen[j] == 0)) {
				selcrit[activedoc] = (j + iteration) % totdoc;
				key[activedoc] = j;
				activedoc++;
			}
		}

		select_top_n(selcrit, activedoc, select, (qp_size / 2));

		for (k = 0; (choosenum < (qp_size / 2)) && (k < (qp_size / 2))
				&& (k < activedoc); k++) {
			i = key[select[k]];
			chosen[i] = 1;
			working2dnum[inum + choosenum] = i;
			choosenum += 1;
			kernel_cache_touch(kernel_cache, i); /*
												 * make sure it does not get
												 * kicked
												 */
			/* out of cache */
		}

		activedoc = 0;
		for (i = 0; (j = active2dnum[i]) >= 0; i++) {
			s = label[j];
			if ((!((a[j] <= (0 + learn_parm.epsilon_a)) && (s < 0)))
					&& (!((a[j] >= (learn_parm.svm_cost[j] - learn_parm.epsilon_a)) && (s > 0)))
					&& (inconsistent[j] == 0) && (label[j] != 0)
					&& (chosen[j] == 0)) {
				selcrit[activedoc] = (j + iteration) % totdoc;
				key[activedoc] = j;
				activedoc++;
			}
		}

		select_top_n(selcrit, activedoc, select, (qp_size / 2));
		for (k = 0; (choosenum < qp_size) && (k < (qp_size / 2))
				&& (k < activedoc); k++) {
			i = key[select[k]];
			chosen[i] = 1;
			working2dnum[inum + choosenum] = i;
			choosenum += 1;
			kernel_cache_touch(kernel_cache, i); /*
												 * make sure it does not get
												 * kicked
												 */
			/* out of cache */
		}
		working2dnum[inum + choosenum] = -1; /* complete index */
		return (choosenum);

	}

	public void cache_multiple_kernel_rows(KERNEL_CACHE kernel_cache,
			DOC[] docs, int[] key, int varnum, KERNEL_PARM kernel_parm) {
		int i;
		for (i = 0; i < varnum; i++) { /* fill up kernel cache */
			cache_kernel_row(kernel_cache, docs, key[i], kernel_parm);
		}
	}

	public void optimize_svm(DOC[] docs, int[] label, int[] unlabeled,
			int[] exclude_from_eq_const, double eq_target, int[] chosen,
			int[] active2dnum, MODEL model, int totdoc, int[] working2dnum,
			int varnum, double[] a, double[] lin, double[] c,
			LEARN_PARM learn_parm, double[] aicache, KERNEL_PARM kernel_parm,
			QP qp, double epsilon_crit_target) {
		int i;
		double[] a_v;
		
		compute_matrices_for_optimization(docs, label, unlabeled,
				exclude_from_eq_const, eq_target, chosen, active2dnum,
				working2dnum, model, a, lin, c, varnum, totdoc, learn_parm,
				aicache, kernel_parm, qp);
	
		//if (svm_common.verbosity >= 3) {

		//}
		// logger.info("Running optimizer...");

		/* call the qp-subsolver */
		svm_hideo shid = new svm_hideo();

		a_v = shid.optimize_qp(qp, epsilon_crit_target,
				learn_parm.svm_maxqpsize, (model.b), learn_parm);
		

		for (i = 0; i < varnum; i++) {
			a[working2dnum[i]] = a_v[i];
		}

	}

	public void compute_matrices_for_optimization(DOC[] docs, int[] label,
			int[] unlabeled, int[] exclude_from_eq_const, double eq_target,
			int[] chosen, int[] active2dnum, int[] key, MODEL model,
			double[] a, double[] lin, double[] c, int varnum, int totdoc,
			LEARN_PARM learn_parm, double[] aicache, KERNEL_PARM kernel_parm,
			QP qp) {

		/*
		 * logger.info("in compute_matrices_for_optimization"); if(docs!=null) {
		 * logger.info("docs.size:"+docs.length); for(int i=0;i<docs.length;i++)
		 * { logger.info(i+" doc fvec:"+docs[i].fvec.toString());
		 * logger.info(" label:"+label[i]); } }
		 */
		int ki, kj, i, j;
		double kernel_temp;

		if (svm_common.verbosity >= 3) {
			logger.info("Computing qp-matrices (type "
					+ kernel_parm.kernel_type + " kernel [degree "
					+ kernel_parm.poly_degree + ", rbf_gamma "
					+ kernel_parm.rbf_gamma + ", coef_lin "
					+ kernel_parm.coef_lin + ",coef_const "
					+ kernel_parm.coef_const + "]...");
		}

		qp.opt_n = varnum;
		qp.opt_ce0[0] = -eq_target;
		// System.out.print("label");
		for (int li = 0; li < label.length; li++) {
			// System.out.print("label["+li+"]="+label[li]+" ");
		}
		// //logger.info("varnum:" + varnum);
		// System.out.println();
		for (j = 1; j < model.sv_num; j++) {
			// //logger.info("j=" + j + " chosen[" + model.supvec[j].docnum +
			// "]="
			// // + chosen[model.supvec[j].docnum]
			// // + " exclude_from_eq_const="
			// // + exclude_from_eq_const[model.supvec[j].docnum] + " alpha:"
			// // + model.alpha[j]);
			if ((chosen[model.supvec[j].docnum] == 0)
					&& (exclude_from_eq_const[model.supvec[j].docnum] == 0)) {
				qp.opt_ce0[0] += model.alpha[j];
			}
		}

		if (learn_parm.biased_hyperplane != 0) {
			qp.opt_m = 1;
		} else {
			qp.opt_m = 0;
		}

		for (i = 0; i < varnum; i++) {
			// //logger.info("i=" + i + " keyi:" + key[i] + " lin:" +
			// lin[key[i]]);
			qp.opt_g0[i] = lin[key[i]];
		}

		for (i = 0; i < varnum; i++) {
			ki = key[i];
			// logger.info("ki:"+ki);
			// System.out.print("  i="+i+" ki= "+ki);
			qp.opt_ce[i] = label[ki];
			qp.opt_low[i] = 0;
			qp.opt_up[i] = learn_parm.svm_cost[ki];
			// System.out.println("docs[ki]:"+ki);
			kernel_temp = sc.kernel(kernel_parm, docs[ki], docs[ki]);
			// //logger.info("kernel_temp:" + kernel_temp);
			qp.opt_g0[i] -= (kernel_temp * a[ki] * (double) label[ki]);
			qp.opt_g[varnum * i + i] = kernel_temp;

			for (j = i + 1; j < varnum; j++) {
				kj = key[j];
				kernel_temp = sc
						.kernel(kernel_parm, docs[ki], docs[kj]);

				qp.opt_g0[i] -= (kernel_temp * a[kj] * (double) label[kj]);
				qp.opt_g0[j] -= (kernel_temp * a[ki] * (double) label[ki]);

				/* compute quadratic part of objective function */
				qp.opt_g[varnum * i + j] = (double) label[ki]
						* (double) label[kj] * kernel_temp;
				qp.opt_g[varnum * j + i] = (double) label[ki]
						* (double) label[kj] * kernel_temp;
				// //logger.info("i=" + i + " j=" + j + " qpij="
				// // + qp.opt_g[varnum * i + j] + " qpji="
				// // + qp.opt_g[varnum * j + i]);
				// logger.info("varnum*j+i:"+(varnum*j+i)+" "+
				// qp.opt_g[varnum*j+i]);

			}
			/*
			 * if(svm_common.verbosity>=3) {
			 * 
			 * if(i % 20 == 0) { logger.info(i+".."); } }
			 */

		}

		for (i = 0; i < varnum; i++) {
			/* assure starting at feasible point */
			qp.opt_xinit[i] = a[key[i]];
			/* set linear part of objective function */
			qp.opt_g0[i] = (learn_parm.eps - (double) label[key[i]] * c[key[i]])
					+ qp.opt_g0[i] * (double) label[key[i]];
		}

		if (svm_common.verbosity >= 3) {
			logger.info("done");
		}

	}

	public double compute_objective_function(double[] a, double[] lin,
			double[] c, double eps, int[] label, int[] active2dnum)
	/* Return value of objective function. */
	/* Works only relative to the active variables! */
	{
		int i, ii;
		double criterion;
		/* calculate value of objective function */
		criterion = 0;
		for (ii = 0; active2dnum[ii] >= 0; ii++) {
			i = active2dnum[ii];
			criterion = criterion + (eps - (double) label[i] * c[i]) * a[i]
					+ 0.5 * a[i] * label[i] * lin[i];
		}
		return (criterion);
	}

	public int check_optimality(MODEL model, int[] label, int[] unlabeled,
			double[] a, double[] lin, double[] c, int totdoc,
			LEARN_PARM learn_parm, double epsilon_crit_org, int[] inconsistent,
			int[] active2dnum, int[] last_suboptimal_at, int iteration,
			KERNEL_PARM kernel_parm) {
		int i, ii, retrain;
		double dist = 0, ex_c, target;
		// logger.info("in check_optimality");
		if (kernel_parm.kernel_type == svm_common.LINEAR) { /* be optimistic */
			learn_parm.epsilon_shrink = -learn_parm.epsilon_crit
					+ epsilon_crit_org;
		} else { /* be conservative */
			learn_parm.epsilon_shrink = learn_parm.epsilon_shrink * 0.7
					+ (maxdiff) * 0.3;
		}

		retrain = 0;
		maxdiff = 0;
		misclassified = 0;

		for (ii = 0; (i = active2dnum[ii]) >= 0; ii++) {
			if ((inconsistent[i] == 0) && (label[i] != 0)) {
				// System.out.println("dist:"+dist);
				// logger.info("i="+i+" a["+i+"]="+a[i]+"  c["+i+"]="+c[i]+"  label["+i+"]="+label[i]+"  lin["+i+"]="+lin[i]);
				dist = (lin[i] - model.b) * (double) label[i];
				target = -(learn_parm.eps - (double) label[i] * c[i]);
				ex_c = learn_parm.svm_cost[i] - learn_parm.epsilon_a;

				if (dist <= 0) {
					misclassified++;
				}

				if ((a[i] > learn_parm.epsilon_a) && (dist > target)) {
					if ((dist - target) > maxdiff) {
						maxdiff = dist - target;
					}
				} else if ((a[i] < ex_c) && (dist < target)) {
					if ((target - dist) > maxdiff) /* largest violation */
					{
						maxdiff = target - dist;
					}
				}

				if ((a[i] > (learn_parm.epsilon_a)) && (a[i] < ex_c)) {
					last_suboptimal_at[i] = iteration; /* not at bound */
				} else if ((a[i] <= (learn_parm.epsilon_a))
						&& (dist < (target + learn_parm.epsilon_shrink))) {
					last_suboptimal_at[i] = iteration; /* not likely optimal */
				} else if ((a[i] >= ex_c)
						&& (dist > (target - learn_parm.epsilon_shrink))) {
					last_suboptimal_at[i] = iteration; /* not likely optimal */
				}
			}
		}
		// System.out.println("maxdiff is :"+maxdiff);
		/* termination criterion */
		if ((retrain == 0) && (maxdiff > (learn_parm.epsilon_crit))) {
			retrain = 1;
		}
		return (retrain);

	}

	public void reactivate_inactive_examples(int[] label, int[] unlabeled,
			double[] a, SHRINK_STATE shrink_state, double[] lin, double[] c,
			int totdoc, int totwords, int iteration, LEARN_PARM learn_parm,
			int[] inconsistent, DOC[] docs, KERNEL_PARM kernel_parm,
			KERNEL_CACHE kernel_cache, MODEL model, double[] aicache,
			double[] weights) {
		int i, j, ii, jj, t;
		int[] changed2dnum, inactive2dnum;
		int[] changed;
		int[] inactive;

		double kernel_val;
		double[] a_old;
		double dist;

		double ex_c, target;
		SVECTOR f;

		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			a_old = shrink_state.last_a;

			for (i = 0; i < totdoc; i++) {
				if (a[i] != a_old[i]) {
					for (f = docs[i].fvec; (f != null); f = f.next) {
						svm_common.add_vector_ns(weights, f, f.factor
								* ((a[i] - a_old[i]) * (double) label[i]));
					}
					a_old[i] = a[i];
				}
			}
			for (i = 0; i < totdoc; i++) {
				if (shrink_state.active[i] == 0) {
					for (f = docs[i].fvec; f != null; f = f.next) {
						lin[i] = shrink_state.last_lin[i] + f.factor
								* svm_common.sprod_ns(weights, f);
					}
				}
				shrink_state.last_lin[i] = lin[i];
			}
			for (i = 0; i < totdoc; i++) {
				for (f = docs[i].fvec; f != null; f = f.next) {
					svm_common.mult_vector_ns(weights, f, 0.0); /*
																 * set weights
																 * back to zero
																 */
				}
			}
		} else {
			changed = new int[totdoc];
			changed2dnum = new int[totdoc];
			inactive = new int[totdoc];
			inactive2dnum = new int[totdoc + 11];

			for (t = shrink_state.deactnum - 1; (t >= 0)
					&& ((shrink_state.a_history[t] != null)); t--) {
				if (svm_common.verbosity >= 2) {
					System.out.println(t + "..");
				}
				a_old = shrink_state.a_history[t];
				for (i = 0; i < totdoc; i++) {

					if ((shrink_state.active[i] == 0)
							&& (shrink_state.inactive_since[i] == t)) {
						inactive[i] = 1;
					} else {
						inactive[i] = 0;
					}

					if (a[i] != a_old[i]) {
						changed[i] = 1;
					} else {
						changed[i] = 0;
					}
				}
				compute_index(inactive, totdoc, inactive2dnum);
				compute_index(changed, totdoc, changed2dnum);

				for (ii = 0; (i = changed2dnum[ii]) >= 0; ii++) {
					get_kernel_row(kernel_cache, docs, i, totdoc,
							inactive2dnum, aicache, kernel_parm);
					for (jj = 0; (j = inactive2dnum[jj]) >= 0; jj++) {
						kernel_val = aicache[j];
						lin[j] += (((a[i] * kernel_val) - (a_old[i] * kernel_val)) * (double) label[i]);
					}
				}
			}
		}

		maxdiff = 0;

		for (i = 0; i < totdoc; i++) {
			shrink_state.inactive_since[i] = shrink_state.deactnum - 1;
			if (inconsistent[i] == 0) {
				dist = (lin[i] - model.b) * (double) label[i];
				target = -(learn_parm.eps - (double) label[i] * c[i]);
				ex_c = learn_parm.svm_cost[i] - learn_parm.epsilon_a;
				if ((a[i] > learn_parm.epsilon_a) && (dist > target)) {
					if ((dist - target) > maxdiff) /* largest violation */
					{
						maxdiff = dist - target;
					}
				} else if ((a[i] < ex_c) && (dist < target)) {
					if ((target - dist) > maxdiff) /* largest violation */
					{
						maxdiff = target - dist;
					}
				}
				if ((a[i] > (0 + learn_parm.epsilon_a)) && (a[i] < ex_c)) {
					shrink_state.active[i] = 1; /* not at bound */
				} else if ((a[i] <= (0 + learn_parm.epsilon_a))
						&& (dist < (target + learn_parm.epsilon_shrink))) {
					shrink_state.active[i] = 1;
				} else if ((a[i] >= ex_c)
						&& (dist > (target - learn_parm.epsilon_shrink))) {
					shrink_state.active[i] = 1;
				} else if (learn_parm.sharedslack != 0) { /*
														 * make all active when
														 * sharedslack
														 */
					shrink_state.active[i] = 1;
				}
			}
		}

		if (kernel_parm.kernel_type != svm_common.LINEAR) { /*
															 * update history
															 * for non-linear
															 */
			for (i = 0; i < totdoc; i++) {
				shrink_state.a_history[shrink_state.deactnum - 1][i] = a[i];
			}
			for (t = shrink_state.deactnum - 2; (t >= 0)
					&& (shrink_state.a_history[t] != null); t--) {
				shrink_state.a_history[t] = null;
			}
		}

	}

	int incorporate_unlabeled_examples(MODEL model, int[] label,
			int[] inconsistent, int[] unlabeled, double[] a, double[] lin,
			int totdoc, double[] selcrit, int[] select, int[] key,
			int transductcycle, KERNEL_PARM kernel_parm, LEARN_PARM learn_parm) {
		int i, j, k, j1, j2, j3, j4, unsupaddnum1 = 0, unsupaddnum2 = 0;
		int pos, neg, upos, uneg, orgpos, orgneg, nolabel, newpos, newneg, allunlab;
		double dist, model_length, posratio, negratio;
		int check_every = 2;
		double loss;
		double switchsens = 0.0, switchsensorg = 0.0;
		double umin, umax, sumalpha;
		int imin = 0, imax = 0;
		int switchnum = 0;

		switchsens /= 1.2;

		/* assumes that lin[] is up to date -> no inactive vars */

		orgpos = 0;
		orgneg = 0;
		newpos = 0;
		newneg = 0;
		nolabel = 0;
		allunlab = 0;
		for (i = 0; i < totdoc; i++) {
			if (unlabeled[i] == 0) {
				if (label[i] > 0) {
					orgpos++;
				} else {
					orgneg++;
				}
			} else {
				allunlab++;
				if (unlabeled[i] != 0) {
					if (label[i] > 0) {
						newpos++;
					} else if (label[i] < 0) {
						newneg++;
					}
				}
			}
			if (label[i] == 0) {
				nolabel++;
			}
		}

		if (learn_parm.transduction_posratio >= 0) {
			posratio = learn_parm.transduction_posratio;
		} else {
			posratio = (double) orgpos / (double) (orgpos + orgneg); /*
																	 * use ratio
																	 * of
																	 * pos/neg
																	 */
		} /* in training data */
		negratio = 1.0 - posratio;

		learn_parm.svm_costratio = 1.0; /* global */
		if (posratio > 0) {
			learn_parm.svm_costratio_unlab = negratio / posratio;
		} else {
			learn_parm.svm_costratio_unlab = 1.0;
		}

		pos = 0;
		neg = 0;
		upos = 0;
		uneg = 0;
		for (i = 0; i < totdoc; i++) {
			dist = (lin[i] - model.b); /* 'distance' from hyperplane */
			if (dist > 0) {
				pos++;
			} else {
				neg++;
			}
			if (unlabeled[i] != 0) {
				if (dist > 0) {
					upos++;
				} else {
					uneg++;
				}
			}
			if ((unlabeled[i] == 0)
					&& (a[i] > (learn_parm.svm_cost[i] - learn_parm.epsilon_a))) {
				/*
				 * printf("Ubounded %ld (class %ld, unlabeled %ld)\n",i,label[i],
				 * unlabeled[i]);
				 */
			}
		}
		if (svm_common.verbosity >= 2) {
			System.out.println("POS=" + pos + ", ORGPOS=" + orgpos
					+ ", ORGNEG=" + orgneg);
			System.out.println("POS=" + pos + ", NEWPOS=" + newpos
					+ ", NEWNEG=" + newneg);
			System.out.println("pos ratio = " + (double) (upos)
					/ (double) (allunlab) + " (" + posratio + ")");
		}

		if (transductcycle == 0) {
			j1 = 0;
			j2 = 0;
			j4 = 0;
			for (i = 0; i < totdoc; i++) {
				dist = (lin[i] - model.b); /* 'distance' from hyperplane */
				if ((label[i] == 0) && (unlabeled[i] != 0)) {
					selcrit[j4] = dist;
					key[j4] = i;
					j4++;
				}
			}

			unsupaddnum1 = 0;
			unsupaddnum2 = 0;
			select_top_n(selcrit, j4, select, (int) (allunlab * posratio + 0.5));
			for (k = 0; (k < (int) (allunlab * posratio + 0.5)); k++) {
				i = key[select[k]];
				label[i] = 1;
				unsupaddnum1++;
				j1++;
			}

			for (i = 0; i < totdoc; i++) {
				if ((label[i] == 0) && (unlabeled[i] != 0)) {
					label[i] = -1;
					j2++;
					unsupaddnum2++;
				}
			}
			for (i = 0; i < totdoc; i++) { /* set upper bounds on vars */
				if (unlabeled[i] != 0) {
					if (label[i] == 1) {
						learn_parm.svm_cost[i] = learn_parm.svm_c
								* learn_parm.svm_costratio_unlab
								* learn_parm.svm_unlabbound;
					} else if (label[i] == -1) {
						learn_parm.svm_cost[i] = learn_parm.svm_c
								* learn_parm.svm_unlabbound;
					}
				}
			}
			if (svm_common.verbosity >= 1) {
				System.out.println("Classifying unlabeled data as "
						+ unsupaddnum1 + " POS / " + unsupaddnum2 + " NEG.");

			}
			if (svm_common.verbosity >= 1) {
				System.out.println("Retraining.");
			}
			if (svm_common.verbosity >= 2) {
				// System.out.println();
			}
			return 3;
		}

		if ((transductcycle % check_every) == 0) {
			if (svm_common.verbosity >= 1) {
				System.out.println("Retraining.");
			}
			if (svm_common.verbosity >= 2) {
				// System.out.println();
			}

			j1 = 0;
			j2 = 0;
			unsupaddnum1 = 0;
			unsupaddnum2 = 0;
			for (i = 0; i < totdoc; i++) {
				if ((unlabeled[i] == 2)) {
					unlabeled[i] = 1;
					label[i] = 1;
					j1++;
					unsupaddnum1++;
				} else if ((unlabeled[i] == 3)) {
					unlabeled[i] = 1;
					label[i] = -1;
					j2++;
					unsupaddnum2++;
				}
			}
			for (i = 0; i < totdoc; i++) { /* set upper bounds on vars */
				if (unlabeled[i] != 0) {
					if (label[i] == 1) {
						learn_parm.svm_cost[i] = learn_parm.svm_c
								* learn_parm.svm_costratio_unlab
								* learn_parm.svm_unlabbound;
					} else if (label[i] == -1) {
						learn_parm.svm_cost[i] = learn_parm.svm_c
								* learn_parm.svm_unlabbound;
					}
				}
			}

			if (svm_common.verbosity >= 2) {
				System.out.println(upos + " positive -> Added " + unsupaddnum1
						+ " POS / " + unsupaddnum2
						+ " NEG unlabeled examples.\n");
			}

			if (learn_parm.svm_unlabbound == 1) {
				learn_parm.epsilon_crit = 0.001; /* do the last run right */
			} else {
				learn_parm.epsilon_crit = 0.01; /*
												 * otherwise, no need to be so
												 * picky
												 */
			}

			return 3;
		} else if (((transductcycle % check_every) < check_every)) {
			model_length = 0;
			sumalpha = 0;
			loss = 0;
			for (i = 0; i < totdoc; i++) {
				model_length += a[i] * label[i] * lin[i];
				sumalpha += a[i];
				dist = (lin[i] - model.b); /* 'distance' from hyperplane */
				if ((label[i] * dist) < (1.0 - learn_parm.epsilon_crit)) {
					loss += (1.0 - (label[i] * dist)) * learn_parm.svm_cost[i];
				}
			}
			model_length = Math.sqrt(model_length);
			if (svm_common.verbosity >= 2) {
				System.out.println("Model-length = " + model_length + " ("
						+ sumalpha + "), loss = " + loss + ", objective = "
						+ loss + 0.5 * model_length * model_length);
			}
			j1 = 0;
			j2 = 0;
			j3 = 0;
			j4 = 0;
			unsupaddnum1 = 0;
			unsupaddnum2 = 0;
			umin = 99999;
			umax = -99999;
			j4 = 1;
			while (j4 != 0) {
				umin = 99999;
				umax = -99999;
				for (i = 0; (i < totdoc); i++) {
					dist = (lin[i] - model.b);
					if ((label[i] > 0) && (unlabeled[i] != 0)
							&& (inconsistent[i] == 0) && (dist < umin)) {
						umin = dist;
						imin = i;
					}
					if ((label[i] < 0) && (unlabeled[i] != 0)
							&& (inconsistent[i] == 0) && (dist > umax)) {
						umax = dist;
						imax = i;
					}
				}
				if ((umin < (umax + switchsens - 1E-4))) {
					j1++;
					j2++;
					unsupaddnum1++;
					unlabeled[imin] = 3;
					inconsistent[imin] = 1;
					unsupaddnum2++;
					unlabeled[imax] = 2;
					inconsistent[imax] = 1;
				} else
					j4 = 0;
				j4 = 0;
			}
			for (j = 0; (j < totdoc); j++) {
				if ((unlabeled[j] != 0) && (inconsistent[j] == 0)) {
					if (label[j] > 0) {
						unlabeled[j] = 2;
					} else if (label[j] < 0) {
						unlabeled[j] = 3;
					}
					/* inconsistent[j]=1; */
					j3++;
				}
			}
			switchnum += unsupaddnum1 + unsupaddnum2;

			/*
			 * stop and print out current margin
			 * 
			 * if(switchnum == 2*kernel_parm.poly_degree) {
			 * learn_parm.svm_unlabbound=1; }
			 */

			if ((unsupaddnum1 == 0) && (unsupaddnum2 == 0)) {
				if ((learn_parm.svm_unlabbound >= 1)
						&& ((newpos + newneg) == allunlab)) {
					for (j = 0; (j < totdoc); j++) {
						inconsistent[j] = 0;
						if (unlabeled[j] != 0) {
							unlabeled[j] = 1;
						}
					}
					write_prediction(learn_parm.predfile, model, lin, a,
							unlabeled, label, totdoc, learn_parm);
					if (svm_common.verbosity >= 1) {
						System.out.println("Number of switches:" + switchnum);
					}
					return 0;
				}
				switchsens = switchsensorg;
				learn_parm.svm_unlabbound *= 1.5;
				if (learn_parm.svm_unlabbound > 1) {
					learn_parm.svm_unlabbound = 1;
				}
				model.at_upper_bound = 0; /* since upper bound increased */
				if (svm_common.verbosity >= 1)
					System.out
							.println("Increasing influence of unlabeled examples to "
									+ learn_parm.svm_unlabbound * 100.0 + " .");
			} else if (svm_common.verbosity >= 1) {
				System.out.println(upos + " positive -> Switching labels of "
						+ unsupaddnum1 + " POS / " + unsupaddnum2
						+ " NEG unlabeled examples.");
			}

			if (svm_common.verbosity >= 2) {
				// System.out.println();
			}
			learn_parm.epsilon_crit = 0.5; /* don't need to be so picky */

			for (i = 0; i < totdoc; i++) { /* set upper bounds on vars */
				if (unlabeled[i] != 0) {
					if (label[i] == 1) {
						learn_parm.svm_cost[i] = learn_parm.svm_c
								* learn_parm.svm_costratio_unlab
								* learn_parm.svm_unlabbound;
					} else if (label[i] == -1) {
						learn_parm.svm_cost[i] = learn_parm.svm_c
								* learn_parm.svm_unlabbound;
					}
				}
			}

			return 2;
		}

		return 0;
	}

	public void write_prediction(String predfile, MODEL model, double[] lin,
			double[] a, int[] unlabeled, int[] label, int totdoc,
			LEARN_PARM learn_parm) {
		FileWriter fw = null;
		PrintWriter pw = null;
		try {
			fw = new FileWriter(new File(predfile));
			pw = new PrintWriter(fw);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		int i;
		double dist, a_max;

		if (svm_common.verbosity >= 1) {
			System.out.println("Writing prediction file...");
		}

		a_max = learn_parm.epsilon_a;
		for (i = 0; i < totdoc; i++) {
			if ((unlabeled[i] != 0) && (a[i] > a_max)) {
				a_max = a[i];
			}
		}
		for (i = 0; i < totdoc; i++) {
			if (unlabeled[i] != 0) {
				if ((a[i] > (learn_parm.epsilon_a))) {
					dist = (double) label[i]
							* (1.0 - learn_parm.epsilon_crit - a[i]
									/ (a_max * 2.0));
				} else {
					dist = (lin[i] - model.b);
				}
				if (dist > 0) {
					System.out.println(dist + ":+1 " + (-dist) + ":-1");
				} else {
					System.out.println((-dist) + ":-1 " + dist + ":1");
				}
			}
		}
		try {
			fw.close();
			pw.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if (svm_common.verbosity >= 1) {
			// System.out.println("done");
		}

	}

	public int shrink_problem(DOC[] docs, LEARN_PARM learn_parm,
			SHRINK_STATE shrink_state, KERNEL_PARM kernel_parm,
			int[] active2dnum, int[] last_suboptimal_at, int iteration,
			int totdoc, int minshrink, double[] a, int[] inconsistent)
	/*
	 * Shrink some variables away. Do the shrinking only if at least minshrink
	 * variables can be removed.
	 */
	{
		int i, ii, change, activenum, lastiter;
		double[] a_old;

		activenum = 0;
		change = 0;
		for (ii = 0; active2dnum[ii] >= 0; ii++) {
			i = active2dnum[ii];
			activenum++;
			if (false && (learn_parm.sharedslack != 0)) {
				lastiter = last_suboptimal_at[docs[i].slackid];
			} else {
				lastiter = last_suboptimal_at[i];
			}
			if (((iteration - lastiter) > learn_parm.svm_iter_to_shrink)
					|| (inconsistent[i] != 0)) {
				change++;
			}
		}
		if ((change >= minshrink) /* shrink only if sufficiently many candidates */
				&& (shrink_state.deactnum < shrink_state.maxhistory)) { /*
																		 * and
																		 * enough
																		 * memory
																		 */
			/* Shrink problem by removing those variables which are */
			/* optimal at a bound for a minimum number of iterations */
			if (svm_common.verbosity >= 2) {
				System.out.println(" Shrinking...");
			}
			if (kernel_parm.kernel_type != svm_common.LINEAR) { /*
																 * non-linear
																 * case save
																 * alphas
																 */
				a_old = new double[totdoc];
				shrink_state.a_history[shrink_state.deactnum] = a_old;
				for (i = 0; i < totdoc; i++) {
					a_old[i] = a[i];
				}
			}
			for (ii = 0; active2dnum[ii] >= 0; ii++) {
				i = active2dnum[ii];
				if (false && (learn_parm.sharedslack != 0)) {
					lastiter = last_suboptimal_at[docs[i].slackid];
				} else {
					lastiter = last_suboptimal_at[i];
				}
				if (((iteration - lastiter) > learn_parm.svm_iter_to_shrink)
						|| (inconsistent[i] != 0)) {
					shrink_state.active[i] = 0;
					shrink_state.inactive_since[i] = shrink_state.deactnum;
				}
			}
			activenum = compute_index(shrink_state.active, totdoc, active2dnum);
			shrink_state.deactnum++;
			if (kernel_parm.kernel_type == svm_common.LINEAR) {
				shrink_state.deactnum = 0;
			}
			if (svm_common.verbosity >= 2) {
				System.out.println("done.\n");
				System.out.println(" Number of inactive variables = "
						+ (totdoc - activenum));
			}
		}
		return (activenum);
	}

	public void kernel_cache_shrink(KERNEL_CACHE kernel_cache, int totdoc,
			int numshrink, int[] after)
	/*
	 * Remove numshrink columns in the cache which correspond to examples marked
	 * 0 in after.
	 */
	{
		int i, j, jj, from = 0, to = 0, scount;
		int[] keep;

		if (svm_common.verbosity >= 2) {
			System.out.println(" Reorganizing cache...");
		}

		keep = new int[totdoc];
		for (j = 0; j < totdoc; j++) {
			keep[j] = 1;
		}
		scount = 0;
		for (jj = 0; (jj < kernel_cache.activenum) && (scount < numshrink); jj++) {
			j = kernel_cache.active2totdoc[jj];
			if (after[j] == 0) {
				scount++;
				keep[j] = 0;
			}
		}

		for (i = 0; i < kernel_cache.max_elems; i++) {
			for (jj = 0; jj < kernel_cache.activenum; jj++) {
				j = kernel_cache.active2totdoc[jj];
				if (keep[j] == 0) {
					from++;
				} else {
					kernel_cache.buffer[to] = kernel_cache.buffer[from];
					to++;
					from++;
				}
			}
		}

		kernel_cache.activenum = 0;
		for (j = 0; j < totdoc; j++) {
			if ((keep[j] != 0) && (kernel_cache.totdoc2active[j] != -1)) {
				kernel_cache.active2totdoc[kernel_cache.activenum] = j;
				kernel_cache.totdoc2active[j] = kernel_cache.activenum;
				kernel_cache.activenum++;
			} else {
				kernel_cache.totdoc2active[j] = -1;
			}
		}

		kernel_cache.max_elems = (kernel_cache.buffsize / kernel_cache.activenum);
		if (kernel_cache.max_elems > totdoc) {
			kernel_cache.max_elems = totdoc;
		}

		if (svm_common.verbosity >= 2) {
			System.out.println("done.");
			System.out.println(" Cache-size in rows = "
					+ kernel_cache.max_elems);
		}
	}

	int identify_inconsistent(double[] a, int[] label, int[] unlabeled,
			int totdoc, LEARN_PARM learn_parm, int[] inconsistent) {
		int i, retrain;

		/* Throw out examples with multipliers at upper bound. This */
		/* corresponds to the -i 1 option. */
		/* ATTENTION: this is just a heuristic for finding a close */
		/* to minimum number of examples to exclude to */
		/* make the problem separable with desired margin */
		retrain = 0;
		for (i = 0; i < totdoc; i++) {
			if ((inconsistent[i] == 0)
					&& (unlabeled[i] == 0)
					&& (a[i] >= (learn_parm.svm_cost[i] - learn_parm.epsilon_a))) {
				inconsistentnum++;
				inconsistent[i] = 1; /* never choose again */
				retrain = 2; /* start over */
				if (svm_common.verbosity >= 3) {
					System.out.println("inconsistent(" + i + ")..");
				}
			}
		}
		return (retrain);
	}

	public int identify_misclassified(double[] lin, int[] label,
			int[] unlabeled, int totdoc, MODEL model, int[] inconsistent) {
		int i, retrain;
		double dist;

		/* Throw out misclassified examples. This */
		/* corresponds to the -i 2 option. */
		/* ATTENTION: this is just a heuristic for finding a close */
		/* to minimum number of examples to exclude to */
		/* make the problem separable with desired margin */
		retrain = 0;
		for (i = 0; i < totdoc; i++) {
			dist = (lin[i] - model.b) * (double) label[i]; /*
															 * 'distance' from
															 * hyperplane
															 */
			if ((inconsistent[i] == 0) && (unlabeled[i] == 0) && (dist <= 0)) {
				inconsistentnum++;
				inconsistent[i] = 1; /* never choose again */
				retrain = 2; /* start over */
				if (svm_common.verbosity >= 3) {
					System.out.println("inconsistent(" + i + ")..");
				}
			}
		}
		return (retrain);
	}

	public int identify_one_misclassified(double[] lin, int[] label,
			int[] unlabeled, int totdoc, MODEL model, int[] inconsistent) {
		int i, retrain, maxex = -1;
		double dist, maxdist = 0;

		/* Throw out the 'most misclassified' example. This */
		/* corresponds to the -i 3 option. */
		/* ATTENTION: this is just a heuristic for finding a close */
		/* to minimum number of examples to exclude to */
		/* make the problem separable with desired margin */
		retrain = 0;
		for (i = 0; i < totdoc; i++) {
			if ((inconsistent[i] == 0) && (unlabeled[i] == 0)) {
				dist = (lin[i] - model.b) * (double) label[i];/*
															 * 'distance' from
															 * hyperplane
															 */
				if (dist < maxdist) {
					maxdist = dist;
					maxex = i;
				}
			}
		}
		if (maxex >= 0) {
			inconsistentnum++;
			inconsistent[maxex] = 1; /* never choose again */
			retrain = 2; /* start over */
			if (svm_common.verbosity >= 3) {
				System.out.println("inconsistent(" + i + ")..");
			}
		}
		return (retrain);
	}

	/***
	 * 此方法有问题
	 * 
	 * @param docs
	 * @param label
	 * @param totdoc
	 * @param totwords
	 * @param learn_parm
	 * @param kernel_parm
	 * @param kernel_cache
	 * @param shrink_state
	 * @param model
	 * @param a
	 * @param lin
	 * @param c
	 * @param timing_profile
	 * @return
	 */
	public int optimize_to_convergence_sharedslack(DOC[] docs, int[] label,
			int totdoc, int totwords, LEARN_PARM learn_parm,
			KERNEL_PARM kernel_parm, KERNEL_CACHE kernel_cache,
			SHRINK_STATE shrink_state, MODEL model, double[] a, double[] lin,
			double[] c, TIMING timing_profile) {
		logger.info("in optimize_to_convergence_sharedslack");
		int[] chosen;
		int[] key;
		int i, j, jj;
		int[] last_suboptimal_at;
		int noshrink;
		int[] unlabeled;

		int[] inconsistent;
		int choosenum, already_chosen = 0, iteration;
		int misclassified, supvecnum = 0;
		int[] active2dnum;
		int inactivenum;
		int[] working2dnum;
		int[] selexam;
		int[] ignore;
		int activenum, retrain, maxslackid, slackset, jointstep;
		double criterion, eq_target;
		double[] a_old;
		double[] alphaslack;
		double t0 = 0, t1 = 0, t2 = 0, t3 = 0, t4 = 0, t5 = 0, t6 = 0; /* timing */
		double epsilon_crit_org;
		double bestmaxdiff;
		int bestmaxdiffiter, terminate;

		double[] selcrit; /* buffer for sorting */
		double[] aicache; /* buffer to keep one row of hessian */
		double[] weights; /* buffer for weight vector in linear case */
		QP qp = new QP(); /* buffer for one quadratic program */
		double[] slack; /*
						 * vector of slack variables for optimization with
						 * shared slacks
						 */
		svm_common.verbosity = 0;
		epsilon_crit_org = learn_parm.epsilon_crit; /* save org */
		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			learn_parm.epsilon_crit = 2.0;
			/* kernel_cache=NULL; *//* caching makes no sense for linear kernel */
		}
		learn_parm.epsilon_shrink = 2;
		maxdiff = 1;
		learn_parm.totwords = totwords;

		chosen = new int[totdoc];
		unlabeled = new int[totdoc];
		inconsistent = new int[totdoc];
		ignore = new int[totdoc];
		last_suboptimal_at = new int[totdoc];
		key = new int[totdoc + 11];
		selcrit = new double[totdoc];
		selexam = new int[totdoc];
		a_old = new double[totdoc];
		aicache = new double[totdoc];
		working2dnum = new int[totdoc + 11];
		active2dnum = new int[totdoc + 11];
		qp.opt_ce = new double[learn_parm.svm_maxqpsize];
		qp.opt_ce0 = new double[1];
		qp.opt_g = new double[learn_parm.svm_maxqpsize
				* learn_parm.svm_maxqpsize];

		qp.opt_g0 = new double[learn_parm.svm_maxqpsize];
		qp.opt_xinit = new double[learn_parm.svm_maxqpsize];
		qp.opt_low = new double[learn_parm.svm_maxqpsize];
		qp.opt_up = new double[learn_parm.svm_maxqpsize];

		if (kernel_parm.kernel_type == svm_common.LINEAR) {
			weights = svm_common.create_nvector(totwords);
			svm_common.clear_nvector(weights, totwords);
		} else {
			weights = null;
		}

		maxslackid = 0;

		for (i = 0; i < totdoc; i++) { /* determine size of slack array */
			if (maxslackid < docs[i].slackid) {
				maxslackid = docs[i].slackid;
			}
		}

		slack = new double[maxslackid + 1];
		alphaslack = new double[maxslackid + 1];

		for (i = 0; i < maxslackid; i++) {
			slack[i] = 0;
			alphaslack[i] = 0;
		}

		choosenum = 0;
		retrain = 1;
		iteration = 1;
		bestmaxdiffiter = 1;
		bestmaxdiff = 999999999;
		terminate = 0;

		if (kernel_cache != null) {
			kernel_cache.time = iteration; /* for lru cache */
			kernel_cache_reset_lru(kernel_cache);
		}

		for (i = 0; i < totdoc; i++) { /* various inits */
			chosen[i] = 0;
			unlabeled[i] = 0;
			inconsistent[i] = 0;
			ignore[i] = 0;
			alphaslack[docs[i].slackid] += a[i];
			a_old[i] = a[i];
			last_suboptimal_at[i] = 1;
		}
		activenum = compute_index(shrink_state.active, totdoc, active2dnum);
		inactivenum = totdoc - activenum;
		clear_index(working2dnum);

		/* call to init slack and alphaslack */
		compute_shared_slacks(docs, label, a, lin, c, active2dnum, learn_parm,
				slack, alphaslack);

		for (; (retrain != 0) && (terminate == 0); iteration++) {

			svmconfig.iterations++;

			 for(int ci=0;ci<totdoc;ci++)
             {
                selcrit[ci]=0.0;
             }
			 
			if (kernel_cache != null) {
				kernel_cache.time = iteration; /* for lru cache */
			}

			if (svm_common.verbosity >= 0) {
				//if (iteration % 51 == 0) {
					//System.out.println("Iteration " + iteration + ": ");
				//	logger.info("Iteration " + iteration + ": ");
				//}
			} else if (svm_common.verbosity == 1) {
				System.out.println(".");
			}

			if (svm_common.verbosity >= 2) {
				t0 = svm_common.get_runtime();
			}

			if (svm_common.verbosity >= 3) {
				System.out.println("\nSelecting working set... ");
			}

			if (learn_parm.svm_newvarsinqp > learn_parm.svm_maxqpsize) {
				learn_parm.svm_newvarsinqp = learn_parm.svm_maxqpsize;
			}

			/* select working set according to steepest gradient */
			jointstep = 0;
			eq_target = 0;

			if ((iteration % 101) != 0) {
				slackset = select_next_qp_slackset(docs, label, a, lin, slack,
						alphaslack, c, learn_parm, active2dnum);
				//logger.info("slackset="+slackset+" maxsharedviol:"+maxsharedviol+" learn_parm.epsilon_crit:"+learn_parm.epsilon_crit);
				if (((iteration % 100) == 0) || (slackset == 0)
						|| (maxsharedviol < learn_parm.epsilon_crit)) {
					/* do a step with examples from different slack sets */
					if (svm_common.verbosity >= 2) {
						System.out.println("(i-step)");
					}
					i = 0;
					for (jj = 0; (j = working2dnum[jj]) >= 0; jj++) {
						if ((chosen[j] >= (learn_parm.svm_maxqpsize / Math.min(
								learn_parm.svm_maxqpsize,
								learn_parm.svm_newvarsinqp)))) {
							chosen[j] = 0;
							choosenum--;
						} else {
							chosen[j]++;
							working2dnum[i++] = j;
							//logger.info("working2dum " + i + " " + j);
						}
					}
					working2dnum[i] = -1;
					//logger.info("working2dum[" + i + "] is -1");
					already_chosen = 0;
					if ((Math.min(learn_parm.svm_newvarsinqp,
							learn_parm.svm_maxqpsize - choosenum) >= 4)
							&& (kernel_parm.kernel_type != svm_common.LINEAR)) {
						/* select part of the working set from cache */
						//logger.info("minl select");
                        /*
						String sc_str = "";
						for (int ci = 0; ci < totdoc; ci++) {
							sc_str += (ci + ":" + selcrit[ci] + " ");
						}

						logger.info("sc_str:" + sc_str);
                        */
						
						already_chosen = select_next_qp_subproblem_grad(label,
								unlabeled, a, lin, c, totdoc, (int) (Math.min(
										learn_parm.svm_maxqpsize - choosenum,
										learn_parm.svm_newvarsinqp) / 2),
								learn_parm, inconsistent, active2dnum,
								working2dnum, selcrit, selexam, kernel_cache,
								1, key, chosen);

						choosenum += already_chosen;
					}
					//logger.info("or select \n");
					choosenum += select_next_qp_subproblem_grad(
							label,
							unlabeled,
							a,
							lin,
							c,
							totdoc,
							Math.min(learn_parm.svm_maxqpsize - choosenum,
									learn_parm.svm_newvarsinqp - already_chosen),
							learn_parm, inconsistent, active2dnum,
							working2dnum, selcrit, selexam, kernel_cache, 0,
							key, chosen);
				} else { /* do a step with all examples from same slack set */
					// logger.info("do a step with all examples from same slack set");
					if (svm_common.verbosity >= 2) {
						System.out.println("(j-step on " + slackset + ")");
					}
					jointstep = 1;
					
					
					//  clear working set					 
					for (jj = 0; (j = working2dnum[jj]) >= 0; jj++) { 
						chosen[j] = 0;
					}
					working2dnum[0] = -1;
					eq_target = alphaslack[slackset];
					for (j = 0; j < totdoc; j++) { /* mask all but slackset */
						/* for(jj=0;(j=active2dnum[jj])>=0;jj++) { */
						if (docs[j].slackid != slackset) {
							ignore[j] = 1;
						} else {
							ignore[j] = 0;
							learn_parm.svm_cost[j] = learn_parm.svm_c;
						}
					}
					learn_parm.biased_hyperplane = 1;
					choosenum = select_next_qp_subproblem_grad(label,
							unlabeled, a, lin, c, totdoc,
							learn_parm.svm_maxqpsize, learn_parm, ignore,
							active2dnum, working2dnum, selcrit, selexam,
							kernel_cache, 0, key, chosen);
					learn_parm.biased_hyperplane = 0;
				}
			} else { /*
					 * once in a while, select a somewhat random working set to
					 * get unlocked of infinite loops due to numerical
					 * inaccuracies in the core qp-solver
					 */
				//logger.info("in else yr");
				choosenum += select_next_qp_subproblem_rand(label, unlabeled,
						a, lin, c, totdoc, Math.min(learn_parm.svm_maxqpsize
								- choosenum, learn_parm.svm_newvarsinqp),
						learn_parm, inconsistent, active2dnum, working2dnum,
						selcrit, selexam, kernel_cache, key, chosen, iteration);
			}

			if (svm_common.verbosity >= 2) {
				System.out.println(choosenum + " vectors chosen");
			}

			if (svm_common.verbosity >= 2) {
				t1 = svm_common.get_runtime();
			}

			if (kernel_cache != null) {
				cache_multiple_kernel_rows(kernel_cache, docs, working2dnum,
						choosenum, kernel_parm);
			}

			if (svm_common.verbosity >= 2) {
				t2 = svm_common.get_runtime();
			}

			if (jointstep != 0) {
				learn_parm.biased_hyperplane = 1;
			}

			
			optimize_svm(docs, label, unlabeled, ignore, eq_target, chosen,
					active2dnum, model, totdoc, working2dnum, choosenum, a,
					lin, c, learn_parm, aicache, kernel_parm, qp,
					epsilon_crit_org);

			learn_parm.biased_hyperplane = 0;

			
			//  recompute sums of alphas
			for (jj = 0; (i = working2dnum[jj]) >= 0; jj++) 
			{
				alphaslack[docs[i].slackid] += (a[i] - a_old[i]);
			}

			 
		    //reduce alpha to fulfill constraints		 
			for (jj = 0; (i = working2dnum[jj]) >= 0; jj++) {
				if (alphaslack[docs[i].slackid] > learn_parm.svm_c) {
					if (a[i] < (alphaslack[docs[i].slackid] - learn_parm.svm_c)) {
						alphaslack[docs[i].slackid] -= a[i];
						a[i] = 0;
					} else {
						a[i] -= (alphaslack[docs[i].slackid] - learn_parm.svm_c);
						alphaslack[docs[i].slackid] = learn_parm.svm_c;
					}
				}
			}

			for (jj = 0; (i = active2dnum[jj]) >= 0; jj++) {
				learn_parm.svm_cost[i] = a[i]
						+ (learn_parm.svm_c - alphaslack[docs[i].slackid]);
			}

			model.at_upper_bound = 0;
			for (jj = 0; jj <= maxslackid; jj++) {
				if (alphaslack[jj] > (learn_parm.svm_c - learn_parm.epsilon_a)) {
					model.at_upper_bound++;
				}
			}

			if (svm_common.verbosity >= 2) {
				t3 = svm_common.get_runtime();
			}

			update_linear_component(docs, label, active2dnum, a, a_old,
					working2dnum, totdoc, totwords, kernel_parm, kernel_cache,
					lin, aicache, weights);
			compute_shared_slacks(docs, label, a, lin, c, active2dnum,
					learn_parm, slack, alphaslack);

			if (svm_common.verbosity >= 2) {
				t4 = svm_common.get_runtime();
			}

			supvecnum = calculate_svm_model(docs, label, unlabeled, lin, a,
					a_old, c, learn_parm, working2dnum, active2dnum, model);

			if (svm_common.verbosity >= 2) {
				t5 = svm_common.get_runtime();
			}

			/* The following computation of the objective function works only */
			/* relative to the active variables */
			if (svm_common.verbosity >= 3) {
				criterion = compute_objective_function(a, lin, c,
						learn_parm.eps, label, active2dnum);
				System.out
						.println("Objective function (over active variables):"
								+ criterion);

			}

			for (jj = 0; (i = working2dnum[jj]) >= 0; jj++) {
				a_old[i] = a[i];
			}

			
			retrain = check_optimality_sharedslack(docs, model, label, a, lin,
					c, slack, alphaslack, totdoc, learn_parm, epsilon_crit_org,
					active2dnum, last_suboptimal_at, iteration, kernel_parm);
			// maxdiff?传值 or 传地址?

			if (svm_common.verbosity >= 2) {
				t6 = svm_common.get_runtime();
				timing_profile.time_select += t1 - t0;
				timing_profile.time_kernel += t2 - t1;
				timing_profile.time_opti += t3 - t2;
				timing_profile.time_update += t4 - t3;
				timing_profile.time_model += t5 - t4;
				timing_profile.time_check += t6 - t5;
			}

			/* checking whether optimizer got stuck */
			if (maxdiff < bestmaxdiff) {
				bestmaxdiff = maxdiff;
				bestmaxdiffiter = iteration;
			}

			if (iteration > (bestmaxdiffiter + learn_parm.maxiter)) {
				/* long time no progress? */
				terminate = 1;
				retrain = 0;
				if (svm_common.verbosity >= 1) {
					System.out
							.println("\nWARNING: Relaxing KT-Conditions due to slow progress! Terminating!");
				}
			}
			noshrink = 0;

			if ((retrain == 0)
					&& (inactivenum > 0)
					&& ((learn_parm.skip_final_opt_check == 0) || (kernel_parm.kernel_type == svm_common.LINEAR))) {
				if (((svm_common.verbosity >= 1) && (kernel_parm.kernel_type != svm_common.LINEAR))
						|| (svm_common.verbosity >= 2)) {
					if (svm_common.verbosity == 1) {
						// System.out.println();
					}
					System.out
							.println(" Checking optimality of inactive variables...");

				}

				t1 = svm_common.get_runtime();
				reactivate_inactive_examples(label, unlabeled, a, shrink_state,
						lin, c, totdoc, totwords, iteration, learn_parm,
						inconsistent, docs, kernel_parm, kernel_cache, model,
						aicache, weights);
				/* Update to new active variables. */
				activenum = compute_index(shrink_state.active, totdoc,
						active2dnum);
				inactivenum = totdoc - activenum;
				/*
				 * check optimality, since check in reactivate does not work for
				 * sharedslacks
				 */
				compute_shared_slacks(docs, label, a, lin, c, active2dnum,
						learn_parm, slack, alphaslack);

				retrain = check_optimality_sharedslack(docs, model, label, a,
						lin, c, slack, alphaslack, totdoc, learn_parm,
						epsilon_crit_org, active2dnum, last_suboptimal_at,
						iteration, kernel_parm);


				/* reset watchdog */
				bestmaxdiff = maxdiff;
				bestmaxdiffiter = iteration;
				/* termination criterion */
				noshrink = 1;
				retrain = 0;
				if (maxdiff > learn_parm.epsilon_crit) {
					retrain = 1;
				}
				timing_profile.time_shrink += svm_common.get_runtime() - t1;
				if (((svm_common.verbosity >= 1) && (kernel_parm.kernel_type != svm_common.LINEAR))
						|| (svm_common.verbosity >= 2)) {
					// System.out.println("done.");
					System.out.println(" Number of inactive variables = "
							+ inactivenum);
				}
			}

			if ((retrain == 0) && (learn_parm.epsilon_crit > maxdiff)) {
				learn_parm.epsilon_crit = maxdiff;
			}
			if ((retrain == 0) && (learn_parm.epsilon_crit > epsilon_crit_org)) {
				learn_parm.epsilon_crit /= 2.0;
				retrain = 1;
				noshrink = 1;
			}
			if (learn_parm.epsilon_crit < epsilon_crit_org) {
				learn_parm.epsilon_crit = epsilon_crit_org;
			}
			if (svm_common.verbosity >= 2) {
				System.out.println(" => (" + supvecnum + " SV (incl. "
						+ model.at_upper_bound
						+ " SV at u-bound), max violation=" + maxdiff + ")");
			}
			if (svm_common.verbosity >= 3) {
				//System.out.println();
			}

			if (((iteration % 10) == 0) && (noshrink == 0)) {
				activenum = shrink_problem(
						docs,
						learn_parm,
						shrink_state,
						kernel_parm,
						active2dnum,
						last_suboptimal_at,
						iteration,
						totdoc,
						Math.max((int) (activenum / 10),
								Math.max((int) (totdoc / 500), 100)), a,
						inconsistent);
				inactivenum = totdoc - activenum;
				if ((kernel_cache != null)
						&& (supvecnum > kernel_cache.max_elems)
						&& ((kernel_cache.activenum - activenum) > Math.max(
								(int) (activenum / 10), 500))) {
					kernel_cache_shrink(kernel_cache, totdoc, Math.min(
							(kernel_cache.activenum - activenum),
							(kernel_cache.activenum - supvecnum)),
							shrink_state.active);
				}
			}

		}

		alphaslack=null;
		slack=null;
		chosen=null;
		unlabeled=null;
		inconsistent=null;
		ignore=null;
		last_suboptimal_at=null;
		key=null;
		selcrit=null;
		selexam=null;
		a_old=null;
		aicache=null;
		working2dnum=null;
		active2dnum=null;
		if(qp!=null)
		{
		  qp.opt_ce=null;
		  qp.opt_ce0=null;
		  qp.opt_g=null;
		  qp.opt_g0=null;
		  qp.opt_xinit=null;
		  qp.opt_low=null;
		  qp.opt_up=null;
		}
		
		if(weights!=null)
		{
			weights=null;
		}
		learn_parm.epsilon_crit = epsilon_crit_org; /* restore org */
		model.maxdiff = maxdiff;

		return (iteration);

	}

	public void compute_shared_slacks(DOC[] docs, int[] label, double[] a,
			double[] lin, double[] c, int[] active2dnum, LEARN_PARM learn_parm,
			double[] slack, double[] alphaslack)
	/* compute the value of shared slacks and the joint alphas */
	{
		int jj, i;
		double dist, target;

		for (jj = 0; (i = active2dnum[jj]) >= 0; jj++) { /* clear slack variables */
			slack[docs[i].slackid] = 0.0;
			//logger.info("jj=" + jj + " i=" + i + " slackid=" + docs[i].slackid
			//		+ " is zero");
			/* alphaslack[docs[i].slackid]=0.0; */
		}
		for (jj = 0; (i = active2dnum[jj]) >= 0; jj++) {
			/*
			 * dist = (lin[i]) * (double) label[i]; target = -(learn_parm.eps -
			 * (double) label[i] * c[i]); if ((target - dist) >
			 * slack[docs[i].slackid]) { slack[docs[i].slackid] = target - dist;
			 * //alphaslack[docs[i].slackid]+=a[i]; }
			 */
			dist = (lin[i]) * (double) label[i];

			//target = -WU.sub(learn_parm.eps, WU.mul((double) label[i], c[i]));
			target=-(learn_parm.eps-(double)label[i]*c[i]);
			//logger.info("jj=" + jj + " i=" + i + " lin=" + lin[i] + " dist="
			//		+ dist + " c=" + c[i] + " eps=" + learn_parm.eps
			//		+ " target:" + target + " slack=" + slack[docs[i].slackid]);
			//if (((target - dist) > slack[docs[i].slackid])
				//	&& Math.abs(target - dist) > (double) (0.5)) 
			if ((target - dist) > slack[docs[i].slackid])//0.5 set may be bug 
			{

				//slack[docs[i].slackid] = WU.sub(target, dist);
				slack[docs[i].slackid]=target-dist;
				//logger.info("jj=" + jj + " i=" + i + " slackid="
				//		+ docs[i].slackid + "  slack=" + slack[docs[i].slackid]);
				// alphaslack[docs[i].slackid]+=a[i];
			}
		}
	}

	public int select_next_qp_slackset(DOC[] docs, int[] label, double[] a,
			double[] lin, double[] slack, double[] alphaslack, double[] c,
			LEARN_PARM learn_parm, int[] active2dnum)
	/* returns the slackset with the largest internal violation */
	{
		int i, ii, maxdiffid;
		double dist, target, maxdiff, ex_c;

		maxdiff = 0;
		maxdiffid = 0;
		for (ii = 0; (i = active2dnum[ii]) >= 0; ii++) {
			ex_c = learn_parm.svm_c - learn_parm.epsilon_a;
			if (alphaslack[docs[i].slackid] >= ex_c) {
				dist = (lin[i]) * (double) label[i] + slack[docs[i].slackid]; /* distance */
				target = -(learn_parm.eps - (double) label[i] * c[i]); /*
																		 * rhs
																		 * of
																		 * constraint
																		 */
				if ((a[i] > learn_parm.epsilon_a) && (dist > target)) {
					if ((dist - target) > maxdiff) { /* largest violation */
						maxdiff = dist - target;
						maxdiffid = docs[i].slackid;
					}
				}
			}
		}
		maxsharedviol = maxdiff;
		return (maxdiffid);
	}

	public int check_optimality_sharedslack(DOC[] docs, MODEL model,
			int[] label, double[] a, double[] lin, double[] c, double[] slack,
			double[] alphaslack, int totdoc, LEARN_PARM learn_parm,
			double epsilon_crit_org, int[] active2dnum,
			int[] last_suboptimal_at, int iteration, KERNEL_PARM kernel_parm)
	/* Check KT-conditions */
	{
		int i, ii, retrain;
		double dist, dist_noslack, ex_c = 0, target;

		// logger.info("in check_optimality_sharedslack");
		if (kernel_parm.kernel_type == svm_common.LINEAR) { /* be optimistic */
			learn_parm.epsilon_shrink = -learn_parm.epsilon_crit / 2.0;
		} else { /* be conservative */
			learn_parm.epsilon_shrink = learn_parm.epsilon_shrink * 0.7
					+ maxdiff * 0.3;
		}

		retrain = 0;
		maxdiff = 0;
		misclassified = 0;
		for (ii = 0; (i = active2dnum[ii]) >= 0; ii++) {
			
			/* 'distance' from hyperplane */
			dist_noslack = (lin[i] - model.b) * (double) label[i];
			dist = dist_noslack + slack[docs[i].slackid];
			target = -(learn_parm.eps - (double) label[i] * c[i]);
			ex_c = learn_parm.svm_c - learn_parm.epsilon_a;
			 //logger.info("ii="+ii+" i="+i+" a="+a[i]+"  c="+c[i]+"  label="+label[i]+"  lin="+lin[i]+" epsilon_a="+learn_parm.epsilon_a+" dist="+dist+" target="+target+" maxdiff="+maxdiff+" ex_c="+ex_c);
			if ((a[i] > learn_parm.epsilon_a) && (dist > target)) {
				if ((dist - target) > maxdiff) { /* largest violation */
					maxdiff = dist - target;
					//if (svm_common.verbosity >= 5) {
					//logger.info("sid " + docs[i].slackid + ": dist="
					//					+ dist + ", target=" + target
					//					+ ", slack=" + slack[docs[i].slackid]
					//					+ ", a=" + a[i] + ", alphaslack="
					//					+ alphaslack[docs[i].slackid]);
					//}

					//if (svm_common.verbosity >= 5) {
					//logger.info(" (single " + maxdiff + ")");
					//}
				}
			}
			if ((alphaslack[docs[i].slackid] < ex_c)
					&& (slack[docs[i].slackid] > 0)) {
				if ((slack[docs[i].slackid]) > (maxdiff)) { /* largest violation */
					maxdiff = slack[docs[i].slackid];
					//if (svm_common.verbosity >= 5) {
					//	logger.info("sid " + docs[i].slackid + ": dist="
					//					+ dist + ", target=" + target
					//					+ ", slack=" + slack[docs[i].slackid]
					//					+ ", a=" + a[i] + ", alphaslack="
					//					+ alphaslack[docs[i].slackid]);
					//}
					//if (svm_common.verbosity >= 5) {
					//	logger.info(" (joint " + maxdiff + ")");
					//}
				}
			}

			/* Count how long a variable was at lower/upper bound (and optimal). */
			/* Variables, which were at the bound and optimal for a long */
			/* time are unlikely to become support vectors. In case our */
			/* cache is filled up, those variables are excluded to save */
			/* kernel evaluations. (See chapter 'Shrinking'). */
			if ((a[i] <= learn_parm.epsilon_a)
					&& (dist < (target + learn_parm.epsilon_shrink))) {
				last_suboptimal_at[i] = iteration; /* not likely optimal */
			} else if ((alphaslack[docs[i].slackid] < ex_c)
					&& (a[i] > learn_parm.epsilon_a)
					&& (Math.abs(dist_noslack - target) > -learn_parm.epsilon_shrink)) {
				last_suboptimal_at[i] = iteration; /* not at lower bound */
			} else if ((alphaslack[docs[i].slackid] >= ex_c)
					&& (a[i] > learn_parm.epsilon_a)
					&& (Math.abs(target - dist) > -learn_parm.epsilon_shrink)) {
				last_suboptimal_at[i] = iteration; /* not likely optimal */
			}
		}
		/* termination criterion */
		if ((retrain == 0) && ((maxdiff) > learn_parm.epsilon_crit)) {
			retrain = 1;
		}
		return (retrain);
	}

	public double estimate_sphere(MODEL model)
	/* Approximates the radius of the ball containing */
	/* the support vectors by bounding it with the */
	{ /* length of the longest support vector. This is */
		int j; /* pretty good for text categorization, since all */
		double xlen, maxxlen = 0; /*
								 * documents have feature vectors of length 1.
								 * It
								 */
		DOC nulldoc; /* assumes that the center of the ball is at the */
		WORD[] nullword = new WORD[1]; /* origin of the space. */
		KERNEL_PARM kernel_parm = model.kernel_parm;
		nullword[0] = new WORD();
		nullword[0].wnum = 0;
		nulldoc = svm_common.create_example(-2, 0, 0, 0.0,
				svm_common.create_svector(nullword, "", 1.0));

		for (j = 1; j < model.sv_num; j++) {
			xlen = Math.sqrt(sc.kernel(kernel_parm, model.supvec[j],
					model.supvec[j])
					- 2
					* sc.kernel(kernel_parm, model.supvec[j], nulldoc)
					+ sc.kernel(kernel_parm, nulldoc, nulldoc));
			if (xlen > maxxlen) {
				maxxlen = xlen;
			}
		}

		return (maxxlen);
	}

	public double length_of_longest_document_vector(DOC[] docs, int totdoc,
			KERNEL_PARM kernel_parm) {
		int i;
		double maxxlen, xlen;

		maxxlen = 0;
		for (i = 0; i < totdoc; i++) {
			xlen = Math.sqrt(sc.kernel(kernel_parm, docs[i], docs[i]));
			if (xlen > maxxlen) {
				maxxlen = xlen;
			}
		}

		return (maxxlen);
	}

	public void write_alphas(String alphafile, double[] a, int[] label,
			int totdoc) {
		// System.out.println("alpha file:"+alphafile);
		alphafile = "alpha.txt";
		FileWriter fw = null;
		PrintWriter pw = null;
		try {
			fw = new FileWriter(new File(alphafile));
			pw = new PrintWriter(fw);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		int i;

		// if(svm_common.verbosity>=1) {
		// System.out.println("Writing alpha file...");
		// }
		// System.out.println("totdoc.size:"+totdoc);
		for (i = 0; i < totdoc; i++) {
			pw.println(a[i] * (double) label[i]);
		}
		try {
			pw.close();
			fw.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if (svm_common.verbosity >= 1) {
			// System.out.println("done");
		}
	}

	public double estimate_r_delta(DOC[] docs, int totdoc,
			KERNEL_PARM kernel_parm) {
		int i;
		double maxxlen, xlen;
		DOC nulldoc; /* assumes that the center of the ball is at the */
		WORD[] nullword = new WORD[1]; /* origin of the space. */

		nullword[0] = new WORD();
		nullword[0].wnum = 0;
		nulldoc = svm_common.create_example(-2, 0, 0, 0.0,
				svm_common.create_svector(nullword, "", 1.0));

		maxxlen = 0;
		for (i = 0; i < totdoc; i++) {
			/*
			 * pw.println("doc i:"+i);
			 * pw.println("docs i label "+docs[i].docnum); for(int
			 * k=0;k<docs[i].fvec.words.length;k++) {
			 * System.out.print(k+":"+docs
			 * [i].fvec.words[k].wnum+":"+docs[i].fvec.words[k].weight+" ");
			 * pw.print
			 * (k+":"+docs[i].fvec.words[k].wnum+":"+docs[i].fvec.words[k
			 * ].weight+" "); } System.out.println(); pw.flush();
			 */
			xlen = Math.sqrt(sc.kernel(kernel_parm, docs[i], docs[i])
					- 2 * sc.kernel(kernel_parm, docs[i], nulldoc)
					+ sc.kernel(kernel_parm, nulldoc, nulldoc));
			if (xlen > maxxlen) {
				maxxlen = xlen;
			}
		}

		return (maxxlen);
	}

	public double estimate_margin_vcdim(MODEL model, double w, double R)
	/* optional: length of model vector in feature space */
	/* optional: radius of ball containing the data */
	{
		double h;

		/* follows chapter 5.6.4 in [Vapnik/95] */

		if (w < 0) {
			w = sc.model_length_s(model);
		}
		if (R < 0) {
			R = estimate_sphere(model);
		}
		h = w * w * R * R + 1;
		return (h);
	}

	/************************** Compute estimates ******************************/

	public void compute_xa_estimates(MODEL model, int[] label, int[] unlabeled,
			int totdoc, DOC[] docs, double[] lin, double[] a,
			KERNEL_PARM kernel_parm, LEARN_PARM learn_parm)
	/*
	 * Computes xa-estimate of error rate, recall, and precision. See T.
	 * Joachims, Estimating the Generalization Performance of an SVM
	 * Efficiently, IMCL, 2000.
	 */
	{
		int i, looerror, looposerror, loonegerror;
		int totex, totposex;
		double xi, r_delta, r_delta_sq, sim = 0;
		int[] sv2dnum = null, sv = null;
		int svnum;

		r_delta = estimate_r_delta(docs, totdoc, kernel_parm);
		r_delta_sq = r_delta * r_delta;

		looerror = 0;
		looposerror = 0;
		loonegerror = 0;
		totex = 0;
		totposex = 0;
		svnum = 0;

		if (learn_parm.xa_depth > 0) {
			sv = new int[totdoc + 11];
			for (i = 0; i < totdoc; i++)
				sv[i] = 0;
			for (i = 1; i < model.sv_num; i++)
				if (a[model.supvec[i].docnum] < (learn_parm.svm_cost[model.supvec[i].docnum] - learn_parm.epsilon_a)) {
					sv[model.supvec[i].docnum] = 1;
					svnum++;
				}
			sv2dnum = new int[totdoc + 11];
			clear_index(sv2dnum);
			compute_index(sv, totdoc, sv2dnum);
		}

		for (i = 0; i < totdoc; i++) {
			if (unlabeled[i] != 0) {
				/* ignore it */
			} else {
				xi = 1.0 - ((lin[i] - model.b) * ((double) label[i]));
				if (xi < 0)
					xi = 0;
				if (label[i] > 0) {
					totposex++;
				}
				if ((learn_parm.rho * a[i] * r_delta_sq + xi) >= 1.0) {
					if (learn_parm.xa_depth > 0) { /* makes assumptions */
						sim = distribute_alpha_t_greedily(
								sv2dnum,
								svnum,
								docs,
								a,
								i,
								label,
								kernel_parm,
								learn_parm,
								(double) ((1.0 - xi - a[i] * r_delta_sq) / (2.0 * a[i])));
					}
					if ((learn_parm.xa_depth == 0)
							|| ((a[i]
									* sc.kernel(kernel_parm, docs[i],
											docs[i]) + a[i] * 2.0 * sim + xi) >= 1.0)) {
						looerror++;
						if (label[i] > 0) {
							looposerror++;
						} else {
							loonegerror++;
						}
					}
				}
				totex++;
			}
		}

		model.xa_error = ((double) looerror / (double) totex) * 100.0;
		model.xa_recall = (1.0 - (double) looposerror / (double) totposex) * 100.0;
		model.xa_precision = (((double) totposex - (double) looposerror) / ((double) totposex
				- (double) looposerror + (double) loonegerror)) * 100.0;

	}

	public void clear_index(double[] index) {
		index[0] = -1;
	}

	public double distribute_alpha_t_greedily(int[] sv2dnum, int svnum,
			DOC[] docs, double[] a, int docnum, int[] label,
			KERNEL_PARM kernel_parm, LEARN_PARM learn_parm, double thresh)
	/*
	 * Experimental Code improving plain XiAlpha Estimates by computing a better
	 * bound using a greedy optimzation strategy.
	 */
	{
		int best_depth = 0;
		int i, j, k, d, skip, allskip;
		double best, val, init_val_sq, init_val_lin;
		double[] best_val = new double[101];
		int[] best_ex = new int[101];
		double[] cache, trow;

		cache = new double[learn_parm.xa_depth * svnum];
		trow = new double[svnum];

		for (k = 0; k < svnum; k++) {
			trow[k] = sc.kernel(kernel_parm, docs[docnum],
					docs[sv2dnum[k]]);
		}

		init_val_sq = 0;
		init_val_lin = 0;
		best = 0;

		for (d = 0; d < learn_parm.xa_depth; d++) {
			allskip = 1;
			if (d >= 1) {
				init_val_sq += cache[best_ex[d - 1] + svnum * (d - 1)];
				for (k = 0; k < d - 1; k++) {
					init_val_sq += 2.0 * cache[best_ex[k] + svnum * (d - 1)];
				}
				init_val_lin += trow[best_ex[d - 1]];
			}
			for (i = 0; i < svnum; i++) {
				skip = 0;
				if (sv2dnum[i] == docnum)
					skip = 1;
				for (j = 0; j < d; j++) {
					if (i == best_ex[j])
						skip = 1;
				}

				if (skip == 0) {
					val = init_val_sq;
					val += sc.kernel(kernel_parm, docs[sv2dnum[i]],
							docs[sv2dnum[i]]);
					for (j = 0; j < d; j++) {
						val += 2.0 * cache[i + j * svnum];
					}
					val *= (1.0 / (2.0 * (d + 1.0) * (d + 1.0)));
					val -= ((init_val_lin + trow[i]) / (d + 1.0));

					if ((allskip != 0) || (val < best_val[d])) {
						best_val[d] = val;
						best_ex[d] = i;
					}
					allskip = 0;
					if (val < thresh) {
						i = svnum;

					}
				}
			}
			if (allskip == 0) {
				for (k = 0; k < svnum; k++) {
					cache[d * svnum + k] = sc.kernel(kernel_parm,
							docs[sv2dnum[best_ex[d]]], docs[sv2dnum[k]]);
				}
			}
			if ((allskip == 0) && ((best_val[d] < best) || (d == 0))) {
				best = best_val[d];
				best_depth = d;
			}
			if ((allskip != 0) || (best < thresh)) {
				d = learn_parm.xa_depth;
			}
		}

		return (best);
	}

	public void estimate_transduction_quality(MODEL model, int[] label,
			int[] unlabeled, int totdoc, DOC[] docs, double[] lin)
	/*
	 * Loo-bound based on observation that loo-errors must have an equal
	 * distribution in both training and test examples, given that the test
	 * examples are classified correctly. Compare chapter
	 * "Constraints on the Transductive Hyperplane" in my Dissertation.
	 */
	{
		int i, j, l = 0, ulab = 0, lab = 0, labpos = 0, labneg = 0, ulabpos = 0, ulabneg = 0, totulab = 0;
		double totlab = 0, totlabpos = 0, totlabneg = 0, labsum = 0, ulabsum = 0;
		double r_delta, r_delta_sq, xi, xisum = 0, asum = 0;

		r_delta = estimate_r_delta(docs, totdoc, model.kernel_parm);
		r_delta_sq = r_delta * r_delta;

		for (j = 0; j < totdoc; j++) {
			if (unlabeled[j] != 0) {
				totulab++;
			} else {
				totlab++;
				if (label[j] > 0)
					totlabpos++;
				else
					totlabneg++;
			}
		}

		for (j = 1; j < model.sv_num; j++) {
			i = model.supvec[j].docnum;
			xi = 1.0 - ((lin[i] - model.b) * (double) label[i]);
			if (xi < 0)
				xi = 0;

			xisum += xi;
			asum += Math.abs(model.alpha[j]);
			if (unlabeled[i] != 0) {
				ulabsum += (Math.abs(model.alpha[j]) * r_delta_sq + xi);
			} else {
				labsum += (Math.abs(model.alpha[j]) * r_delta_sq + xi);
			}
			if ((Math.abs(model.alpha[j]) * r_delta_sq + xi) >= 1) {
				l++;
				if (unlabeled[model.supvec[j].docnum] != 0) {
					ulab++;
					if (model.alpha[j] > 0)
						ulabpos++;
					else
						ulabneg++;
				} else {
					lab++;
					if (model.alpha[j] > 0)
						labpos++;
					else
						labneg++;
				}
			}
		}

		System.out.println("xacrit>=1: labeledpos=" + (double) labpos
				/ (double) totlab * 100.0 + " labeledneg=" + (double) labneg
				/ (double) totlab * 100.0 + " default=" + (double) totlabpos
				/ (double) (totlab) * 100.0);
		System.out.println("xacrit>=1: unlabelpos=" + (double) ulabpos
				/ (double) totulab * 100.0 + " unlabelneg=" + (double) ulabneg
				/ (double) totulab * 100.0);
		System.out.println("xacrit>=1: labeled=" + (double) lab
				/ (double) totlab * 100.0 + " unlabled=" + (double) ulab
				/ (double) totulab * 100.0 + " all=" + (double) l
				/ (double) (totdoc) * 100.0);
		System.out.println("xacritsum: labeled=" + (double) labsum
				/ (double) totlab * 100.0 + " unlabled=" + (double) ulabsum
				/ (double) totulab * 100.0 + " all="
				+ (double) (labsum + ulabsum) / (double) (totdoc) * 100.0);
		System.out.println("r_delta_sq=" + r_delta_sq + " xisum=" + xisum
				+ " asum=" + asum);
	}

	public static KERNEL_CACHE kernel_cache_init(int totdoc, int buffsize) {
		int i;
		KERNEL_CACHE kernel_cache = new KERNEL_CACHE();

		kernel_cache.index = new int[totdoc];
		kernel_cache.occu = new int[totdoc];
		kernel_cache.lru = new int[totdoc];
		kernel_cache.invindex = new int[totdoc];
		kernel_cache.active2totdoc = new int[totdoc];
		kernel_cache.totdoc2active = new int[totdoc];
		kernel_cache.buffer = new double[buffsize * 1024 * 1024];

		kernel_cache.buffsize = buffsize * 1024 * 1024;

		kernel_cache.max_elems = kernel_cache.buffsize / totdoc;
		if (kernel_cache.max_elems > totdoc) {
			kernel_cache.max_elems = totdoc;
		}

		if (svm_common.verbosity >= 2) {
			System.out.println(" Cache-size in rows = "
					+ kernel_cache.max_elems);
			//System.out.println(" Kernel evals so far: "
				//	+ svm_common.kernel_cache_statistic);
		}

		kernel_cache.elems = 0; /* initialize cache */
		for (i = 0; i < totdoc; i++) {
			kernel_cache.index[i] = -1;
			kernel_cache.lru[i] = 0;
		}
		for (i = 0; i < totdoc; i++) {
			kernel_cache.occu[i] = 0;
			kernel_cache.invindex[i] = -1;
		}

		kernel_cache.activenum = totdoc;
		;
		for (i = 0; i < totdoc; i++) {
			kernel_cache.active2totdoc[i] = i;
			kernel_cache.totdoc2active[i] = i;
		}

		kernel_cache.time = 0;

		return (kernel_cache);
	}

	public static void main(String[] args) {

	}

}
