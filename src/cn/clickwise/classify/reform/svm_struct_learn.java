package cn.clickwise.classify.reform;

import org.apache.log4j.Logger;


/**
 * Basic algorithm for learning structured outputs (e.g. parses, sequences,
 * multi-label classification) with a Support Vector Machine.
 * 
 * @author lq
 */

public class svm_struct_learn {

	public static final short SLACK_RESCALING = 1;
	public static final short MARGIN_RESCALING = 2;
	public static final short NSLACK_ALG = 0;
	public static final short NSLACK_SHRINK_ALG = 1;
	public static final short ONESLACK_PRIMAL_ALG = 2;
	public static final short ONESLACK_DUAL_ALG = 3;
	//public static final short ONESLACK_DUAL_CACHE_ALG = 4;

	public double rhs_g = 0;
	public SVECTOR lhs_g = null;
	public double rt_cachesum_g = 0;
	public double rhs_i_g;
	public double rt_viol_g;
	public double rt_psi_g;
	public double[] lhs_n;
	public int argmax_count_g;
	
	public SVECTOR fydelta_g = null;
	public LABEL[] most_violated_g=null;

	public double[] alpha_g = null;
	public int[] alphahist_g = null;
	public int[] remove_g = null;
	
	public int[] subThreadsFinished;

	private static Logger logger = Logger.getLogger(svm_struct_learn.class);
	
	public svm_common sc=null;
	
	public svm_struct_api ssa=null;
	
	public svm_struct_learn()
	{
	  sc=new svm_common();	
	  ssa=svm_struct_api_factory.getSvmStructApi();
	}

	public void svm_learn_struct(SAMPLE sample, STRUCT_LEARN_PARM sparm,
			LEARN_PARM lparm, KERNEL_PARM kparm, STRUCTMODEL sm, int alg_type) {
		int i, j;
		int numIt = 0;
		alpha_g = null;
		alphahist_g = null;
		int argmax_count = 0;
		int newconstraints = 0, totconstraints = 0, activenum = 0;
		int opti_round;
		int[] opti;
		int  use_shrinking;
		int old_totconstraints = 0;
		double epsilon, svmCnorm;
		int tolerance, new_precision = 1, dont_stop = 0;
		double lossval, factor, dist;
		double margin = 0;
		double slack;
		double[] slacks;
		double slacksum, ceps = 0;
		double dualitygap, modellength, alphasum;
		int sizePsi = 0;
		// double[] alpha = null;
		// int[] alphahist = null;
		int optcount = 0, lastoptcount = 0;
		CONSTSET cset;
		SVECTOR diff = null;
		SVECTOR fy, fybar, f;
		SVECTOR[] fycache = null;
		SVECTOR slackvec;
		WORD[] slackv = new WORD[2];
		MODEL svmModel = null;
		KERNEL_CACHE kcache = null;
		LABEL ybar;
		DOC doc;

		int n = sample.n;
		EXAMPLE[] ex = sample.examples;
		double rt_total = 0, rt_opt = 0, rt_init = 0, rt_psi = 0, rt_viol = 0;
		double rt1, rt2;
		rt1 = svm_common.get_runtime();

		ssa.init_struct_model(sample, sm, sparm, lparm, kparm);
		sizePsi = sm.sizePsi + 1;
		logger.info("the sizePsi2 is " + sizePsi);
		if (alg_type == svm_struct_common.NSLACK_SHRINK_ALG) {
			use_shrinking = 1;
		} else {
			use_shrinking = 0;
		}

		opti = new int[n];
		for (i = 0; i < n; i++) {
			opti[i] = 0;
		}

		opti_round = 0;
		svmCnorm = sparm.C / n;

		if (sparm.slack_norm == 1) {
			lparm.svm_c = svmCnorm;
			lparm.sharedslack = 1;
		} else if (sparm.slack_norm == 2) {
			lparm.svm_c = 999999999999999.0; /*upper bound C must never be reached*/
			lparm.sharedslack = 0;
			if (kparm.kernel_type != svm_common.LINEAR) {
				logger.error("ERROR: Kernels are not implemented for L2 slack norm!");
				System.exit(0);
			}
		} else {
			logger.error("ERROR: Slack norm must be L1 or L2!");
		}

		epsilon = 100.0;
		tolerance = Math.min(n / 3, Math.max(n / 100, 5));
		lparm.biased_hyperplane = 0;

		cset = svm_struct_api.init_struct_constraints(sample, sm, sparm);

		if (cset.m > 0) {
			alpha_g = new double[cset.m];
			alphahist_g = new int[cset.m];
			for (i = 0; i < cset.m; i++) {
				alpha_g[i] = 0;
				alphahist_g[i] = -1; /*-1 makes sure these constraints are never removed*/ 									  																		
			}
		}

		/* set initial model and slack variables */
		svmModel = new MODEL();
		lparm.epsilon_crit = epsilon;
		if (kparm.kernel_type != svm_common.LINEAR) {
			kcache = svm_learn.kernel_cache_init(Math.max(cset.m, 1),
					lparm.kernel_cache_size);
		}
		svm_learn sl = new svm_learn();
		logger.info("sizePsi:" + sizePsi + "   n:" + n);
		sl.svm_learn_optimization(cset.lhs, cset.rhs, cset.m, sizePsi + n,
				lparm, kparm, kcache, svmModel, alpha_g);
		svm_common.add_weight_vector_to_linear_model(svmModel);
		sm.svm_model = svmModel;
		sm.w = svmModel.lin_weights;


		rt_init += Math.max(svm_common.get_runtime() - rt1, 0);
		rt_total += Math.max(svm_common.get_runtime() - rt1, 0);

		/*****************/
		/*** main loop ***/
		/*****************/
		do { /* iteratively increase precision */

			epsilon = Math.max(epsilon * 0.49999999999, sparm.epsilon);
			new_precision = 1;

			if (epsilon == sparm.epsilon) /* for final precision, find all SV */
			{
				tolerance = 0;
			}

			lparm.epsilon_crit = epsilon / 2; /*svm precision must be higher than eps */
											
			if (svm_struct_common.struct_verbosity >= 1) {
				logger.info("Setting current working precision to " + epsilon);
			}

			do {/*iteration until (approx) all SV are found for current precision and tolerance*/

				opti_round++;
				activenum = n;
				dont_stop = 0;
				old_totconstraints = totconstraints;

				do { /*
					 * with shrinking turned on, go through examples that keep
					 * producing new constraints
					 */

					if (svm_struct_common.struct_verbosity >= 1) {
						logger.info("Iter " + (++numIt) + " (" + activenum+ " active):");
					}

					ceps = 0;

					for (i = 0; i < n; i++) {
						/*** example loop ***/

						rt1 = svm_common.get_runtime();

						if ((use_shrinking == 0) || (opti[i] != opti_round)) {
							/*
							 * if the example is not shrunk away, then see if it
							 * is necessary to add a new constraint
							 */
							rt2 = svm_common.get_runtime();
							argmax_count++;
							if (sparm.loss_type == SLACK_RESCALING) {
								ybar = ssa.find_most_violated_constraint_slackrescaling(ex[i].x, ex[i].y, sm, sparm);
							} else {
								ybar = ssa.find_most_violated_constraint_marginrescaling(ex[i].x, ex[i].y, sm, sparm);
							}
							rt_viol += Math.max(svm_common.get_runtime() - rt2,0);

							if (svm_struct_api.empty_label(ybar)) {
								if (opti[i] != opti_round) {
									activenum--;
									opti[i] = opti_round;
								}
								if (svm_struct_common.struct_verbosity >= 2)
									logger.info("no-incorrect-found(" + i+ ") ");
								continue;
							}

							/**** get psi(y)-psi(ybar) ****/
							rt2 = svm_common.get_runtime();
							if (fycache != null) {
								fy = svm_common.copy_svector(fycache[i]);
							} else {
								fy = ssa.psi(ex[i].x, ex[i].y, sm,sparm);
							}

							fybar = ssa.psi(ex[i].x, ybar, sm, sparm);
							rt_psi += Math.max(svm_common.get_runtime() - rt2,0);

							/**** scale feature vector and margin by loss ****/
							lossval = ssa.loss(ex[i].y, ybar, sparm);
							if (sparm.slack_norm == 2)
								lossval = Math.sqrt(lossval);
							if (sparm.loss_type == SLACK_RESCALING)
								factor = lossval;
							else
								/* do not rescale vector for */
								factor = 1.0; /* margin rescaling loss type */
							for (f = fy; f != null; f = f.next) {
								f.factor *= factor;
							}

							for (f = fybar; f != null; f = f.next)
								f.factor *= -factor;
							margin = lossval;

							/**** create constraint for current ybar ****/
							svm_common.append_svector_list(fy, fybar);
							doc = svm_common.create_example(cset.m, 0, i + 1,1, fy);

							/**** compute slack for this example ****/
							slack = 0;
							for (j = 0; j < cset.m; j++)
								if (cset.lhs[j].slackid == i + 1) {
									if (sparm.slack_norm == 2)
										slack = Math.max(slack,cset.rhs[j]- (sc.classify_example(svmModel,cset.lhs[j]) - sm.w[sizePsi+ i]/ (Math.sqrt(2 * svmCnorm))));
									else
										slack = Math.max(slack,cset.rhs[j]- sc.classify_example(svmModel,cset.lhs[j]));
								}

							/**** if `error' add constraint and recompute ****/
							dist = sc.classify_example(svmModel, doc);
							ceps = Math.max(ceps, margin - dist - slack);
							if (slack > (margin - dist + 0.0001)) {
								logger.debug("\nWARNING: Slack of most violated constraint is smaller than slack of working\n");
								logger.debug("         set! There is probably a bug in 'find_most_violated_constraint_*'.\n");
								logger.debug("Ex " + i + ": slack=" + slack+ ", newslack=" + (margin - dist)+ "\n");
								/* exit(1); */
							}
							if ((dist + slack) < (margin - epsilon)) {
								if (svm_struct_common.struct_verbosity >= 2) {
									logger.info("(" + i + ",eps="+ (margin - dist - slack) + ") ");
								}
								if (svm_struct_common.struct_verbosity == 1) {
									// logger.info(".");
									System.out.print(".");
								}

								/**** resize constraint matrix and add new constraint****/
								cset.m++;
								svm_struct_api.realloc(cset);

								if (kparm.kernel_type == svm_common.LINEAR) {
									diff = svm_common.add_list_ss(fy);
									if (sparm.slack_norm == 1)
										cset.lhs[cset.m - 1] = svm_common.create_example(cset.m - 1,0,i + 1,1,svm_common.copy_svector(diff));
									else if (sparm.slack_norm == 2) {
										/**** add squared slack variable to feature vector****/
										slackv[0].wnum = sizePsi + i;
										slackv[0].weight = 1 / (Math.sqrt(2 * svmCnorm));
										slackv[1].wnum = 0; /* terminator */
										slackvec = svm_common.create_svector(slackv, null, 1.0);
										cset.lhs[cset.m - 1] = svm_common.create_example(cset.m - 1, 0,i + 1, 1,svm_common.add_ss(diff,slackvec));
									}
								} else { /* kernel is used */
									if (sparm.slack_norm == 1)
										cset.lhs[cset.m - 1] = svm_common.create_example(cset.m - 1,0,i + 1,1,svm_common.copy_svector(fy));
									else if (sparm.slack_norm == 2)
										System.exit(1);
								}
								svm_struct_api.realloc_rhs(cset);
								cset.rhs[cset.m - 1] = margin;

								alpha_g = svm_struct_api.realloc_alpha(alpha_g,cset.m);
								alpha_g[cset.m - 1] = 0;
								alphahist_g = svm_struct_api.realloc_alpha_list(alphahist_g, cset.m);
								alphahist_g[cset.m - 1] = optcount;
								newconstraints++;
								totconstraints++;
								
								/********medlda**********/
								/* record the index to the original constraints. */
								cset.lhs[cset.m-1].orgDocNum = ex[i].x.doc.docnum * ybar.num_classes + ybar.class_index - 1;

								
								/************************/
								
							} else {
								// logger.info("+");
								if (opti[i] != opti_round) {
									activenum--;
									opti[i] = opti_round;
								}
							}
						}// if use shrinking

						/**** get new QP solution ****/
						if ((newconstraints >= sparm.newconstretrain)
								|| ((newconstraints > 0) && (i == n - 1))
								|| ((new_precision != 0) && (i == n - 1))) {
							if (svm_struct_common.struct_verbosity >= 1) {
								// logger.info("*");
							}
							rt2 = svm_common.get_runtime();

							svmModel = new MODEL();
							/*
							 * Always get a new kernel cache. It is not possible to use the 
							 * same cache for two different training runs
							 */

							if (kparm.kernel_type != svm_common.LINEAR)
								kcache = sl.kernel_cache_init(Math.max(cset.m, 1),lparm.kernel_cache_size);
							
							/* Run the QP solver on cset. */
							sl.svm_learn_optimization(cset.lhs, cset.rhs,cset.m, sizePsi + n, lparm, kparm, kcache,svmModel, alpha_g);
							/*
							 * Always add weight vector, in case part of the
							 * kernel is linear. If not, ignore the weight
							 * vector since its content is bogus.
							 */
							svm_common.add_weight_vector_to_linear_model(svmModel);
							sm.svm_model = svmModel.copyMODEL();
							sm.w = new double[svmModel.lin_weights.length];
							for (int k = 0; k < svmModel.lin_weights.length; k++) {
								sm.w[k] = svmModel.lin_weights[k];
							}
							// sm.w = svmModel.lin_weights;
							optcount++;
							/*
							 * keep track of when each constraint was last
							 * active. constraints marked with -1 are not
							 * updated
							 */
							for (j = 0; j < cset.m; j++) {
								if ((alphahist_g[j] > -1) && (alpha_g[j] != 0)) {
									alphahist_g[j] = optcount;
								}
								// logger.info("j="+j+"");
							}
							rt_opt += Math.max(svm_common.get_runtime() - rt2,0);

							if ((new_precision != 0)&& (epsilon <= sparm.epsilon))
								dont_stop = 1; /*make sure we take one final pass */
												 
							new_precision = 0;
							newconstraints = 0;
						}

						rt_total += Math.max(svm_common.get_runtime() - rt1, 0);
					}// exmample loop

					rt1 = svm_common.get_runtime();

					//if (svm_struct_common.struct_verbosity >= 1)
						logger.info("(NumConst=" + cset.m + ", SV="+ (svmModel.sv_num - 1) + ", CEps=" + ceps+ ", QPEps=" + svmModel.maxdiff + ")\n");
						System.out.println("(NumConst=" + cset.m + ", SV="+ (svmModel.sv_num - 1) + ", CEps=" + ceps+ ", QPEps=" + svmModel.maxdiff + ")\n");

					if (svm_struct_common.struct_verbosity >= 2)
						logger.info("Reducing working set...");

					remove_inactive_constraints(cset, optcount,
							Math.max(50, optcount - lastoptcount));

					lastoptcount = optcount;
					if (svm_struct_common.struct_verbosity >= 2)
						logger.info("done. (NumConst=" + cset.m + ")\n");

					rt_total += Math.max(svm_common.get_runtime() - rt1, 0);

				} while ((use_shrinking != 0) && (activenum > 0));
			} while (((totconstraints - old_totconstraints) > tolerance)|| (dont_stop != 0));
		} while ((epsilon > sparm.epsilon)|| svm_struct_api.finalize_iteration(ceps, 0, sample, sm, cset,alpha_g, sparm)); // main_loop

		if (svm_struct_common.struct_verbosity >= 1) {
			/**** compute sum of slacks ****/
			/****
			 * WARNING: If positivity constraints are used, then the maximum
			 * slack id is larger than what is allocated below
			 ****/
			slacks = new double[n + 1];
			for (i = 0; i <= n; i++) {
				slacks[i] = 0;
			}

			if (sparm.slack_norm == 1) {
				for (j = 0; j < cset.m; j++)
					slacks[cset.lhs[j].slackid] = Math.max(slacks[cset.lhs[j].slackid],cset.rhs[j]- sc.classify_example(svmModel,cset.lhs[j]));
			} else if (sparm.slack_norm == 2) {
				for (j = 0; j < cset.m; j++)
					slacks[cset.lhs[j].slackid] = Math.max(slacks[cset.lhs[j].slackid],cset.rhs[j]
									- (sc.classify_example(svmModel,cset.lhs[j]) - sm.w[sizePsi+ cset.lhs[j].slackid - 1]/ (Math.sqrt(2 * svmCnorm))));
			}
			slacksum = 0;
			for (i = 1; i <= n; i++)
				slacksum += slacks[i];

			alphasum = 0;
			for (i = 0; i < cset.m; i++)
				alphasum += alpha_g[i] * cset.rhs[i];
			modellength = sc.model_length_s(svmModel);
			dualitygap = (0.5 * modellength * modellength + svmCnorm* (slacksum + n * ceps))
					- (alphasum - 0.5 * modellength * modellength);

			/*********medlda**************/
			sm.primalobj = 0.5*modellength*modellength + svmCnorm * (slacksum + n*ceps);
			/*****************************/
			logger.info("Final epsilon on KKT-Conditions: "+ Math.max(svmModel.maxdiff, epsilon) + "\n");
			logger.info("Upper bound on duality gap: " + dualitygap + "\n");
			logger.info("Dual objective value: dval="+ (alphasum - 0.5 * modellength * modellength) + "\n");
			logger.info("Total number of constraints in final working set: "+ (int) cset.m + " (of " + (int) totconstraints + ")\n");
			logger.info("Number of iterations:" + numIt + "\n");
			logger.info("Number of calls to 'find_most_violated_constraint': "+ argmax_count + "\n");
			if (sparm.slack_norm == 1) {
				logger.info("Number of SV: " + (svmModel.sv_num - 1) + " \n");
				logger.info("Number of non-zero slack variables: "+ svmModel.at_upper_bound + " (out of " + n + ")\n");
				logger.info("Norm of weight vector: |w|=" + modellength + "\n");
			} else if (sparm.slack_norm == 2) {
				logger.info("Number of SV: " + (svmModel.sv_num - 1)+ " (including " + svmModel.at_upper_bound+ " at upper bound)\n");
				logger.info("Norm of weight vector (including L2-loss): |w|="+ modellength + "\n");
			}

			logger.info("Norm. sum of slack variables (on working set): sum(xi_i)/n="+ slacksum / n + "\n");
			logger.info("Norm of longest difference vector: ||Psi(x,y)-Psi(x,ybar)||="
					+ sl.length_of_longest_document_vector(cset.lhs, cset.m,kparm) + "\n");
			logger.info("Runtime in cpu-seconds: " + rt_total / 100.0 + " ("
					+ (100.0 * rt_opt) / rt_total + " for QP, "
					+ (100.0 * rt_viol) / rt_total + " for Argmax, "
					+ (100.0 * rt_psi) / rt_total + " for Psi, "
					+ (100.0 * rt_init) / rt_total + " for init)\n");
		}

		if (svm_struct_common.struct_verbosity >= 4)
			logger.info(svm_struct_common.printW(sm.w, sizePsi, n, lparm.svm_c));

		if (svmModel != null) {
			// sm.svm_model = svm_common.copy_model(svmModel);
			sm.svm_model = svmModel;
			sm.w = sm.svm_model.lin_weights; /* short cut to weight vector */
			//String wstr = "";
			//for (int wi = 0; wi < sm.w.length; wi++) {
			//	wstr += (wi + ":" + sm.w[wi] + " ");
			//}
			//logger.info("wstr:" + wstr);
		}

		/******************medlda******************/
		for ( i=1; i<sm.svm_model.sv_num; i++ ) {
			int docnum = sm.svm_model.supvec[i].docnum;
			sm.svm_model.supvec[i].orgDocNum = cset.lhs[docnum].orgDocNum;

		}
		/*****************************************/
		//svm_struct_api.print_struct_learning_stats(sample, sm, cset, alpha_g,sparm);//edit 20141201

		/*
		 * svm_struct_api.write_struct_model("temp/med/testmodel",sm,sparm);
		 * String[] argt=new String[3];
		 * 
		 * argt[0]="temp/med/test.txt"; argt[1]="temp/med/model";
		 * argt[2]="temp/med/pred"; svm_struct_classify ssc=new
		 * svm_struct_classify(); ssc.classfiy(argt, sm,sparm,svmModel,sample);
		 */
	}

	public void svm_learn_struct_joint(SAMPLE sample, STRUCT_LEARN_PARM sparm,
			LEARN_PARM lparm, KERNEL_PARM kparm, STRUCTMODEL sm, int alg_type) {
		int i, j;
		int numIt = 0;
		// int argmax_count=0;
		argmax_count_g = 0;
		int totconstraints = 0;
		int kernel_type_org;
		double epsilon, epsilon_cached;
		double lhsXw;
		// double rhs_i;
		rhs_i_g = 0;
		rhs_g = 0;
		// double rhs=0;
		double slack, ceps;
		double dualitygap, modellength, alphasum;
		int sizePsi;
		// double[] alpha = null;
		// int[] alphahist = null;
		int optcount = 0;

		CONSTSET cset;
		
		// double[] lhs_n=null;
		// SVECTOR fydelta=null;
		SVECTOR[] fycache = null;
		// SVECTOR lhs;
		MODEL svmModel = null;
		DOC doc;

		int n = sample.n;
		System.out.println("sample.n:"+sample.n);
		EXAMPLE[] ex = sample.examples;
		double rt_total = 0;
		double rt_opt = 0;
		double rt_init = 0;
		// double rt_psi=0;
		// double rt_viol=0;
		rt_psi_g = 0;
		rt_viol_g = 0;
		double rt_kernel = 0;

		double rt_cacheupdate = 0, rt_cacheconst = 0, rt_cacheadd = 0;
		// double rt_cachesum=0;
		rt_cachesum_g = 0;
		double rt1 = 0, rt2 = 0;
	
		int cached_constraint;
		double viol;
	
		svm_learn sl = new svm_learn();
		rt1 = svm_common.get_runtime();
	
		ssa.init_struct_model(sample, sm, sparm, lparm, kparm);
		sizePsi = sm.sizePsi + 1; /* sm must contain size of psi on return */
		// logger.info("the sizePsi is "+sizePsi+" \n");

		if (sparm.slack_norm == 1) {
			lparm.svm_c = sparm.C; /* set upper bound C */	
			 lparm.sharedslack = 1;
		} else if (sparm.slack_norm == 2) {
			logger.info("ERROR: The joint algorithm does not apply to L2 slack norm!");
			System.exit(0);
		} else {
			logger.info("ERROR: Slack norm must be L1 or L2!");
			System.exit(0);
		}

		lparm.biased_hyperplane = 0; /* set threshold to zero */
		epsilon = 100.0; /* start with low precision and increase later*/
		epsilon_cached = epsilon; /* epsilon to use for iterations using constraints constructed from the constraint cache*/

		cset = svm_struct_api.init_struct_constraints(sample, sm, sparm);

		if (cset.m > 0) {
			alpha_g = new double[cset.m];
			alphahist_g = new int[cset.m];
			for (i = 0; i < cset.m; i++) {
				alpha_g[i] = 0;
				alphahist_g[i] = -1;
			}
		}

		kparm.gram_matrix = null;
		if ((alg_type == svm_struct_common.ONESLACK_DUAL_ALG)|| (alg_type == svm_struct_common.ONESLACK_DUAL_CACHE_ALG))
			kparm.gram_matrix = init_kernel_matrix(cset, kparm);

		/* set initial model and slack variables */
		svmModel = new MODEL();
		lparm.epsilon_crit = epsilon;

		
		sl.svm_learn_optimization(cset.lhs, cset.rhs, cset.m, sizePsi, lparm,kparm, null, svmModel, alpha_g);

		// logger.info("sl totwords:"+svmModel.totwords);
		svm_common.add_weight_vector_to_linear_model(svmModel);
		sm.svm_model = svmModel;
		sm.w = svmModel.lin_weights; /* short cut to weight vector */

		/* create a cache of the feature vectors for the correct labels */
		/*
		fycache = new SVECTOR[n];
		for (i = 0; i < n; i++) {
				fy = null;
			    fycache[i] = fy;
		}
        */
		

		if (kparm.kernel_type == svm_common.LINEAR) {
			// logger.info("kernel type is LINEAR \n");
			lhs_n = svm_common.create_nvector(sm.sizePsi);
		}


		rt_init += Math.max(svm_common.get_runtime() - rt1, 0);
		rt_total += rt_init;

		/*****************/
		/*** main loop ***/
		/*****************/
		
		/******medlda**********/
		int[] vecLabel = new int[n];
		/**********************/
		do { /* iteratively find and add constraints to working set */

			if (svm_struct_common.struct_verbosity >= 1) {
				logger.info("in loop Iter " + (++numIt) + ": ");
				System.out.println("in loop Iter " + (numIt) + ": ");
			}

			rt1 = svm_common.get_runtime();

			/**** compute current slack ****/
			alphasum = 0;
			for (j = 0; (j < cset.m); j++)
			{
				alphasum += alpha_g[j];
			}
			for (j = 0, slack = -1; (j < cset.m) && (slack == -1); j++) {
				if (alpha_g[j] > alphasum / cset.m) {
					slack = Math.max(0,cset.rhs[j]- sc.classify_example(svmModel,cset.lhs[j]));
				}
			}
			slack = Math.max(0, slack);

			rt_total += Math.max(svm_common.get_runtime() - rt1, 0);

			/**** find a violated joint constraint ****/
			lhs_g = null;
			rhs_g = 0;
			
				/* do not use constraint from cache */
				rt1 = svm_common.get_runtime();
				cached_constraint = 0;
				if (kparm.kernel_type == svm_common.LINEAR)
					svm_common.clear_nvector(lhs_n, sm.sizePsi);
				sc.progress_n = 0;
				rt_total += Math.max(svm_common.get_runtime() - rt1, 0);

				double rtPrepareStart=svm_common.get_runtime();
				most_violated_g=new LABEL[n];
				
				/*
				for (i = 0; i < n; i++) {
					rt1 = svm_common.get_runtime();

					if (svm_struct_common.struct_verbosity >= 1)
						sc.print_percent_progress(n, 10, ".");
				

					////****medlda add ybar return value************
					LABEL ybar=find_most_violated_constraint(ex[i], fycache[i], n, sm,sparm);
					
					//***************medlda***********************
					vecLabel[i] = ybar.class_index - 1;
					//********************************************					
					// add current fy-fybar to lhs of constraint 
					if (kparm.kernel_type == svm_common.LINEAR) {
						logger.info("kernel type: is linear 1226");
						svm_common.add_list_n_ns(lhs_n, fydelta_g, 1.0);
					} else {
						svm_common.append_svector_list(fydelta_g, lhs_g);
						lhs_g = fydelta_g;
					}
					rhs_g += rhs_i_g; 

					rt_total += Math.max(svm_common.get_runtime() - rt1, 0);

				} 
                
				*/
				
				int threadNum=7;
				int batNum=(int)((double)n/(double)threadNum);
				int currentStartIndex=0;
				int currentEndIndex=0;
				subThreadsFinished=new int[threadNum];
				for(int th=0;th<threadNum;th++)
				{
					subThreadsFinished[th]=0;
				}
				MostViolated[] mvs=new MostViolated[threadNum];
				for(int th=0;th<threadNum;th++)
				{
					mvs[th]=new MostViolated();
					mvs[th].setEx(ex);
					mvs[th].setFycache(fycache);
					mvs[th].setN(n);
					mvs[th].setSm(sm);
					mvs[th].setSparm(sparm);
					mvs[th].setStartIndex(currentStartIndex);
					mvs[th].setLocalnumIt(numIt);
					currentEndIndex=currentStartIndex+batNum;
					mvs[th].setEndIndex(currentEndIndex);
					mvs[th].setThreadIndex(th);
					currentStartIndex=currentEndIndex;
					Thread t=new Thread(mvs[th]);
					t.start();
				}
				
				boolean allSubFinish=false;
				while(allSubFinish==false)
				{
					//System.out.println("waiting all subthreas stop");
					try{
					Thread.sleep(1000);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					allSubFinish=true;
					for(int th=0;th<threadNum;th++)
					{
						if(subThreadsFinished[th]==0)
						{
							allSubFinish=false;
						}
					}
				}
				
				for(int tk=0;tk<most_violated_g.length;tk++)
				{
					most_violated_g[tk]=new LABEL();
				}
				//update 
				for(int th=0;th<threadNum;th++)
				{
				 	for(int tk=0;tk<lhs_n.length;tk++)
				  	{
				  		lhs_n[tk]+=(mvs[th].getLocal_lhs_n()[tk]);
				  	}
	
				 	rhs_g+=mvs[th].getLocal_rhs_g();
				 	for(int tk=0;tk<most_violated_g.length;tk++)
					{
				 		if((mvs[th].getViolatedValid()[tk])==true)
				 		{
						   most_violated_g[tk]=mvs[th].getMostViolatedLabels()[tk];
				 		}
					}
				 	argmax_count_g+=(mvs[th].getLocal_argmax_count_g());
				 	mvs[th].des();
				 	mvs[th]=null;
				}
				
				for(int ti=0;ti<n;ti++)
				{
					vecLabel[ti] = most_violated_g[ti].class_index - 1;
				}
				System.out.println("all has finished find most violated");
				
				
				double rtPrepareEnd=svm_common.get_runtime();
				logger.info("numIt:"+numIt+" prepareTime:"+((rtPrepareEnd-rtPrepareStart)/100));
				
				rt1 = svm_common.get_runtime();

				/* create sparse vector from dense sum */
				// logger.info("kernel type is "+kparm.kernel_type);
				//System.out.println("kernel type is " + kparm.kernel_type);
				if (kparm.kernel_type == svm_common.LINEAR) {
					// logger.info("kernel type is linear and lhs+n="+lhs_n.toString());
					lhs_g = svm_common.create_svector_n_r(lhs_n, sm.sizePsi,null, 1.0,svm_struct_common.COMPACT_ROUNDING_THRESH);
					// logger.info("kernel type is linear and lhs="+lhs_g.toString());
				}
				doc = svm_common.create_example(cset.m, 0, 1, 1, lhs_g);
				lhsXw = sc.classify_example(svmModel, doc);

				viol = rhs_g - lhsXw;

				rt_total += Math.max(svm_common.get_runtime() - rt1, 0);

		

			rt1 = svm_common.get_runtime();


			ceps = Math.max(0, rhs_g - lhsXw - slack);
			
			double rtOptimizeStart=svm_common.get_runtime();
			if ((ceps > sparm.epsilon) || cached_constraint != 0) {
				/**** resize constraint matrix and add new constraint ****/
				// cset.lhs=new DOC[cset.m+1];
				int ti = 0;
				ti = cset.m + 1;
				cset.lhs = svm_struct_api.reallocDOCS(cset.lhs, ti);			
				cset.lhs[cset.m] = svm_common.create_example(cset.m, 0, 1, 1,lhs_g);

				/**************medlda**********************/
				cset.lhs[cset.m].lvec = new int[n];
				for ( i=0; i<n; i++ ) {
					cset.lhs[cset.m].lvec[i] = vecLabel[i];
				}
				
				/******************************************/
				// cset.rhs = new double[cset.m + 1];
				cset.rhs = svm_struct_api.reallocDoubleArr(cset.rhs, cset.m + 1);
				cset.rhs[cset.m] = rhs_g;
				// alpha = new double[cset.m + 1];
				// logger.info("alpha_g:"+svm_struct_api.douarr2str(alpha_g));
				alpha_g = svm_struct_api.reallocDoubleArr(alpha_g, cset.m + 1);
				alpha_g[cset.m] = 0;
				// alphahist = new int[cset.m + 1];
				// logger.info("alphahist_g:"+svm_struct_api.intarr2str(alphahist_g));
				alphahist_g = svm_struct_api.reallocIntArr(alphahist_g,cset.m + 1);
				alphahist_g[cset.m] = optcount;
				// logger.info("optcount here:"+optcount);
				cset.m++;
				totconstraints++;
				if (alg_type == ONESLACK_DUAL_ALG) {
					if (svm_struct_common.struct_verbosity >= 2)
						rt2 = svm_common.get_runtime();
					kparm.gram_matrix = update_kernel_matrix(kparm.gram_matrix,cset.m - 1, cset, kparm);
					if (svm_struct_common.struct_verbosity >= 2)
						rt_kernel += Math.max(svm_common.get_runtime() - rt2, 0);
				}

				/**** get new QP solution ****/
				if (svm_struct_common.struct_verbosity >= 1) {
					// logger.info("*");
				}
				if (svm_struct_common.struct_verbosity >= 2)
					rt2 = svm_common.get_runtime();
				/*
				 * set svm precision so that higher than eps of most violated
				 * constr
				 */
				if (cached_constraint != 0) {
					epsilon_cached = Math.min(epsilon_cached, ceps);
					lparm.epsilon_crit = epsilon_cached / 2;
				} else {
					epsilon = Math.min(epsilon, ceps); /* best eps so far */
					lparm.epsilon_crit = epsilon / 2;
					epsilon_cached = epsilon;
				}

				svmModel = new MODEL();
				/* Run the QP solver on cset. */
				kernel_type_org = kparm.kernel_type;
				if ((alg_type == svm_struct_common.ONESLACK_DUAL_ALG)|| (alg_type == svm_struct_common.ONESLACK_DUAL_CACHE_ALG))
					kparm.kernel_type = svm_common.GRAM; /*use kernel stored in kparm */
														  														  														
				// logger.info("svm_learn_struct_joint call svm_learn_optimization in loop");
				sl.svm_learn_optimization(cset.lhs, cset.rhs, cset.m, sizePsi,lparm, kparm, null, svmModel, alpha_g);

				kparm.kernel_type = (short) kernel_type_org;
                svmModel.kernel_parm.kernel_type = (short) kernel_type_org;
				
                /** Always add weight vector, in case part of the kernel is
				 * linear. If not, ignore the weight vector since its content is bogus.*/
				svm_common.add_weight_vector_to_linear_model(svmModel);
				/***
				 * sm.svm_model = svmModel.copyMODEL();
				 ***/
				sm.svm_model = svmModel;
				/*************************
				 * sm.svm_model = svmModel; sm.w = svmModel.lin_weights;
				 **************************/

				sm.w = new double[svmModel.lin_weights.length];
				for (int iw = 0; iw < svmModel.lin_weights.length; iw++) {
					sm.w[iw] = svmModel.lin_weights[iw];
				}

				// logger.info("learned weights:"+svmModel.topWeights());
				/*
				 * for(int kk=0;kk<svmModel.lin_weights.length;kk++) {
				 * if(svmModel.lin_weights[kk]>0) {
				 * logger.info(kk+":"+svmModel.lin_weights[kk]+" "); } }
				 */
				optcount++;
				/*
				 * keep track of when each constraint was last active.
				 * constraints marked with -1 are not updated
				 */
				for (j = 0; j < cset.m; j++) {
					// logger.info("op j="+j+":");
					if ((alphahist_g[j] > -1) && (alpha_g[j] != 0)) {
						alphahist_g[j] = optcount;
						// logger.info("alphahist_g j="+j+":"+alphahist_g[j]);
					}
				}
				if (svm_struct_common.struct_verbosity >= 2)
					rt_opt += Math.max(svm_common.get_runtime() - rt2, 0);

				/*
				 * Check if some of the linear constraints have not been active
				 * in a while. Those constraints are then removed to avoid
				 * bloating the working set beyond necessity.
				 */
				if (svm_struct_common.struct_verbosity >= 3) {
					logger.info("Reducing working set...");
				}

				/************ 在这里要将某些限制去掉 **********/
				// logger.info("cset.m before:"+cset.m);
				remove_inactive_constraints(cset, optcount, 50);
				// logger.info("cset.m after:"+cset.m);
				if (svm_struct_common.struct_verbosity >= 3)
					logger.info("done. ");

			} else {

			}

			double rtOptimizeEnd=svm_common.get_runtime();
			logger.info("numIt:"+numIt+" optimize time:"+((rtOptimizeEnd-rtOptimizeStart)/100));
			//if (svm_struct_common.struct_verbosity >= 1) {
				logger.info("(NumConst=" + cset.m + ", SV="+ (svmModel.sv_num - 1) + ", CEps=" + ceps + ", QPEps="+ svmModel.maxdiff + ")\n");
				System.out.println("(NumConst=" + cset.m + ", SV="+ (svmModel.sv_num - 1) + ", CEps=" + ceps + ", QPEps="+ svmModel.maxdiff + ")\n");
			//}

			rt_total += Math.max(svm_common.get_runtime() - rt1, 0);
            
			//if (ceps < 0.5) {
			//	break;
			//}
			System.gc();

			
		} while (cached_constraint != 0|| (ceps > sparm.epsilon)
				|| svm_struct_api.finalize_iteration(ceps, cached_constraint,sample, sm, cset, alpha_g, sparm));


		/************medlda***********************/
		if(kparm.kernel_type == svm_common.LINEAR) modellength = sc.model_length_n(svmModel);
		else modellength = sc.model_length_s(svmModel);
		sm.primalobj = 0.5*modellength*modellength + sparm.C*viol;
		/****************************************/
		
		if (svm_struct_common.struct_verbosity >= 1) {
			logger.info("Final epsilon on KKT-Conditions: "+ (Math.max(svmModel.maxdiff, ceps)) + "\n");

			slack = 0;
			for (j = 0; j < cset.m; j++)
				slack = Math.max(slack,cset.rhs[j]- sc.classify_example(svmModel,cset.lhs[j]));
			alphasum = 0;
			for (i = 0; i < cset.m; i++)
				alphasum += alpha_g[i] * cset.rhs[i];
			if (kparm.kernel_type == svm_common.LINEAR)
				modellength = sc.model_length_n(svmModel);
			else
				modellength = sc.model_length_s(svmModel);
			dualitygap = (0.5 * modellength * modellength + sparm.C * viol)- (alphasum - 0.5 * modellength * modellength);

			logger.info("Upper bound on duality gap: " + dualitygap + "\n");
			logger.info("Dual objective value: dval="+ (alphasum - 0.5 * modellength * modellength) + "\n");
			logger.info("Primal objective value: pval="+ (0.5 * modellength * modellength + sparm.C * viol) + "\n");
			logger.info("Total number of constraints in final working set: "+ ((int) cset.m) + " (of " + ((int) totconstraints) + ")\n");
			logger.info("Number of iterations: " + numIt + "\n");
			logger.info("Number of calls to 'find_most_violated_constraint': "+ argmax_count_g + "\n");
			logger.info("Number of SV: " + (svmModel.sv_num - 1) + " \n");
			logger.info("Norm of weight vector: |w|=" + modellength + "\n");
			logger.info("Value of slack variable (on working set): xi=" + slack+ "\n");
			logger.info("Value of slack variable (global): xi=" + viol + "\n");
			logger.info("Norm of longest difference vector: ||Psi(x,y)-Psi(x,ybar)||="+ (sl.length_of_longest_document_vector(cset.lhs, cset.m,kparm)) + "\n");
			if (svm_struct_common.struct_verbosity >= 2)
				logger.info("Runtime in cpu-seconds: " + (rt_total / 100.0)
						+ " (" + ((100.0 * rt_opt) / rt_total) + " for QP, "
						+ ((100.0 * rt_kernel) / rt_total) + " for kernel, "
						+ ((100.0 * rt_viol_g) / rt_total) + "for Argmax, "
						+ ((100.0 * rt_psi_g) / rt_total) + " for Psi, "
						+ ((100.0 * rt_init) / rt_total) + " for init, "
						+ ((100.0 * rt_cacheupdate) / rt_total)
						+ " for cache update, "
						+ ((100.0 * rt_cacheconst) / rt_total)
						+ " for cache const, "
						+ ((100.0 * rt_cacheadd) / rt_total)
						+ " for cache add (incl. "
						+ ((100.0 * rt_cachesum_g) / rt_total) + " for sum))\n");
			else if (svm_struct_common.struct_verbosity == 1)
				logger.info("Runtime in cpu-seconds: " + (rt_total / 100.0)
						+ "\n");
		}



		if (svm_struct_common.struct_verbosity >= 4)
			svm_struct_common.printW(sm.w, sizePsi, n, lparm.svm_c);

		if (svmModel != null) {
			if (svmModel.kernel_parm == null) {
				logger.info("svmModel kernel_parm is null");
			}

			//logger.info("sv num there:" + svmModel.sv_num);
			// sm.svm_model = svm_common.copy_model(svmModel);
			sm.svm_model = svmModel;
			sm.w = sm.svm_model.lin_weights; /* short cut to weight vector */

			/**********medlda***************/
			/* record the loss-augmented prediction in support vectors. */

			for ( i=1; i<svmModel.sv_num; i++ ) {
				//sm.svm_model.supvec[i].lvec = new int[n];
				//logger.info("sl vec i="+i+":"+svmModel.supvec[i].lvecString());
				sm.svm_model.supvec[i].lvec=svmModel.supvec[i].lvec;
				//for ( int d=0; d<n; d++ ) {
				//	sm.svm_model.supvec[i].lvec[d] = svmModel.supvec[i].lvec[d];
				//}
				
			}	
		
		}
		/*
		 * if(sm.svm_model==null) { logger.info("the svm model is null"); }
		 * if(sm.svm_model.kernel_parm==null) {
		 * logger.info("the svm model kernel_parm is null"); }
		 */

		logger.info("model type in slsj:"+svmconfig.model_type);
		if(svmconfig.model_type!=1)//medlda不要把特征向量加和
		{
		  svm_struct_api.print_struct_learning_stats(sample, sm, cset, alpha_g,sparm);
		}
		
		/**clear****/
		//sl=null;
		lhs_n=null;
	
	    rhs_g = 0;
		lhs_g = null;
		rt_cachesum_g = 0;
		rhs_i_g=0;
		rt_viol_g=0;
		rt_psi_g=0;	
		argmax_count_g=0;
	    fydelta_g = null;
		alpha_g = null;
		alphahist_g = null;
		remove_g = null;
		if(cset!=null)
		{
		for(int cl=0;cl<cset.lhs.length;cl++)
		{
			cset.lhs[cl]=null;
		}
		cset.rhs=null;
		cset=null;
		}
		if(kparm!=null)
		{
			kparm.gram_matrix=null;
		}
		
		System.gc();
		Runtime.getRuntime().gc() ;
		try{
		Thread.sleep(5000);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void remove_inactive_constraints(CONSTSET cset, int currentiter,
			int mininactive)
	/*
	 * removes the constraints from cset (and alpha) for which alphahist
	 * indicates that they have not been active for at least mininactive
	 * iterations
	 */

	{
		int i, m;
		/*
		 * double[] local_alpha=new double[alpha_g.length]; for(int
		 * lai=0;lai<local_alpha.length;lai++) { local_alpha[lai]=alpha_g[lai];
		 * }
		 * 
		 * int[] local_alphahist=new int[alphahist_g.length]; for(int
		 * lai=0;lai<local_alphahist.length;lai++) {
		 * local_alphahist[lai]=alphahist_g[lai]; }
		 */
		m = 0;
		for (i = 0; i < cset.m; i++) {
			if ((alphahist_g[i] < 0)
					|| ((currentiter - alphahist_g[i]) < mininactive)) {
				/*
				 * keep constraints that are marked as -1 or which have recently
				 * been active
				 */
				cset.lhs[m] = cset.lhs[i];
				cset.lhs[m].docnum = m;
				cset.rhs[m] = cset.rhs[i];
				alpha_g[m] = alpha_g[i];
				alphahist_g[m] = alphahist_g[i];
				// logger.info("m="+m+" i="+i+"  alphahist_g"+alphahist_g[m]+" \n");
				m++;
			} else {
			}
		}

		if (cset.m != m) {
			cset.m = m;
			svm_struct_api.realsmallloc_lhs(cset);
			svm_struct_api.realsmallloc_rhs(cset);

			alpha_g = svm_struct_api.reallocDoubleArr(alpha_g, cset.m);
			alphahist_g = svm_struct_api.reallocIntArr(alphahist_g, cset.m);

		}
	}

	public MATRIX init_kernel_matrix(CONSTSET cset, KERNEL_PARM kparm)
	/*
	 * assigns a kernelid to each constraint in cset and creates the
	 * corresponding kernel matrix.
	 */
	{
		int i, j;
		double kval;
		MATRIX matrix;

		/* assign kernel id to each new constraint */
		for (i = 0; i < cset.m; i++)
			cset.lhs[i].kernelid = i;

		/* allocate kernel matrix as necessary */
		matrix = svm_common.create_matrix(i + 50, i + 50);

		for (j = 0; j < cset.m; j++) {
			for (i = j; i < cset.m; i++) {
				kval = sc.kernel(kparm, cset.lhs[j], cset.lhs[i]);
				matrix.element[j][i] = kval;
				matrix.element[i][j] = kval;
			}
		}
		return (matrix);
	}







	/**
	 * find_most_violated_constraint(&fydelta,&rhs_i,&ex[i],
	 * fycache[i],n,sm,sparm, &rt_viol,&rt_psi,&argmax_count);
	 * 
	 * @param fydelta
	 * @param rhs
	 * @param ex
	 * @param fycached
	 * @param n
	 * @param sm
	 * @param sparm
	 * @param rt_viol
	 * @param rt_psi
	 * @param argmax_count
	 */
	public LABEL find_most_violated_constraint(EXAMPLE ex, SVECTOR fycached,
			int n, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm)
	/*
	 * returns fydelta=fy-fybar and rhs scalar value that correspond to the most
	 * violated constraint for example ex
	 */
	{
		// logger.info("begin find_most_violated_constraint");
		double rt2 = 0;
		LABEL ybar;
		SVECTOR fybar, fy;
		double factor, lossval;

		// logger.info("in the find_most_violated_constraint");

		if (svm_struct_common.struct_verbosity >= 2)
			rt2 = svm_common.get_runtime();
		argmax_count_g++;
		if (sparm.loss_type == SLACK_RESCALING) {
			// logger.info("call method find_most_violated_constraint_slackrescaling");
			ybar = ssa.find_most_violated_constraint_slackrescaling(ex.x, ex.y, sm, sparm);
		} else {
			// logger.info("call method find_most_violated_constraint_marginrescaling");
			ybar = ssa.find_most_violated_constraint_marginrescaling(ex.x, ex.y,sm, sparm);
		}
		if (svm_struct_common.struct_verbosity >= 2)
			rt_viol_g += Math.max(svm_common.get_runtime() - rt2, 0);

		//if (svm_struct_api.empty_label(ybar)) {
		//	logger.info("ERROR: empty label was returned for example\n");
		//}

		/**** get psi(x,y) and psi(x,ybar) ****/
		if (svm_struct_common.struct_verbosity >= 2)
			rt2 = svm_common.get_runtime();
		if (fycached != null)
			fy = svm_common.copy_svector(fycached);
		else
			fy = ssa.psi(ex.x, ex.y, sm, sparm);
		// logger.info("ybar label info:"+ybar.class_index);
		fybar = ssa.psi(ex.x, ybar, sm, sparm);
		// logger.info("fydelta find yeah:"+fybar.toString());
		if (svm_struct_common.struct_verbosity >= 2)
			rt_psi_g += Math.max(svm_common.get_runtime() - rt2, 0);
		lossval = ssa.loss(ex.y, ybar, sparm);

		/**** scale feature vector and margin by loss ****/
		if (sparm.loss_type == SLACK_RESCALING)
			factor = lossval / n;
		else
			/* do not rescale vector for */
			factor = 1.0 / n; /* margin rescaling loss type */
		svm_common.mult_svector_list(fy, factor);
		svm_common.mult_svector_list(fybar, -factor);
		svm_common.append_svector_list(fybar, fy); /* compute fy-fybar */
		/*
		 * String winfo=""; if(fybar!=null) { winfo=""; for(int
		 * l=0;l<fybar.words.length;l++) {
		 * winfo=winfo+fybar.words[l].wnum+":"+fybar.words[l].weight+" "; }
		 * logger.info("ybar info:"+winfo);
		 * 
		 * }
		 */
		fydelta_g = fybar;

		// logger.info("fydelta find year:" + fydelta_g.toString());
		//rhs_i_g = lossval / n;
		//rhs_i_g = lossval / n;
		//rhs_i_g=WU.div(lossval, n, 20);
		rhs_i_g = lossval / (double)n;
		// logger.info("rhs_i_g:" + rhs_i_g);
		// logger.info("end find_most_violated_constraint");
		return ybar;
	}

	public MATRIX update_kernel_matrix(MATRIX matrix, int newpos,
			CONSTSET cset, KERNEL_PARM kparm)
	/*
	 * assigns new kernelid to constraint in position newpos and fills the
	 * corresponding part of the kernel matrix
	 */
	{
		int i, maxkernelid = 0, newid;
		double kval;
		double[] used;

		/* find free kernelid to assign to new constraint */
		for (i = 0; i < cset.m; i++) {
			// logger.info("i="+i+" newpos:"+newpos);
			/*
			 * if(cset.lhs!=null) {
			 * logger.info("cset.lhs.length:"+cset.lhs.length);
			 * logger.info("cset.lhs[i].kernelid all:"+cset.lhs[i].kernelid); }
			 * else { logger.info("cset.lhs is null"); }
			 */
			if (i != newpos) {
				// logger.info("cset.lhs[i].kernelid:"+cset.lhs[i].kernelid);
				maxkernelid = Math.max(maxkernelid, cset.lhs[i].kernelid);
			}
		}
		used = svm_common.create_nvector(maxkernelid + 2);
		svm_common.clear_nvector(used, maxkernelid + 2);
		for (i = 0; i < cset.m; i++)
			if (i != newpos)
				used[cset.lhs[i].kernelid] = 1;
		for (newid = 0; used[newid] != 0; newid++)
			;
		cset.lhs[newpos].kernelid = newid;

		/* extend kernel matrix if necessary */
		maxkernelid = Math.max(maxkernelid, newid);
		if ((matrix == null) || (maxkernelid >= matrix.m))
			matrix = svm_common.realloc_matrix(matrix, maxkernelid + 50,
					maxkernelid + 50);

		for (i = 0; i < cset.m; i++) {
			kval = sc.kernel(kparm, cset.lhs[newpos], cset.lhs[i]);
			matrix.element[newid][cset.lhs[i].kernelid] = kval;
			matrix.element[cset.lhs[i].kernelid][newid] = kval;
		}
		return (matrix);
	}

	public void svm_learn_struct_joint_custom(SAMPLE sample,
			STRUCT_LEARN_PARM sparm, LEARN_PARM lparm, KERNEL_PARM kparm,
			STRUCTMODEL sm) {

	}

	
	//更新 lhs_n
	public synchronized void add_local_lhs(double[] local_lhs)
	{
		System.out.println("in add_local_lhs");
	  	for(int i=0;i<lhs_n.length;i++)
	  	{
	  		lhs_n[i]+=local_lhs[i];
	  	}
	}
	
	//更新rhs_g
	public synchronized void add_local_rhs(double local_rhs)
	{
		System.out.println("in add_local_rhs");
	  	rhs_g+=local_rhs;
	}
	
	public synchronized void update_local_mostVoilated(LABEL[] local_mostviolated)
	{
		System.out.println("in update_local_mostVoilated");
		for(int i=0;i<most_violated_g.length;i++)
		{
			most_violated_g[i]=local_mostviolated[i];
		}
	}
	
	public synchronized void add_local_argmax_count(double argmax_count)
	{
		System.out.println("in add_local_argmax_count");
		argmax_count_g+=argmax_count;
	}
	
	public synchronized void updateSubThreadsFinished(int threadIndex,int status)
	{
		System.out.println("thread "+threadIndex+" is setting status");
		subThreadsFinished[threadIndex]=status;
	}
	
	private class MostViolated implements Runnable{

		/****input parameters****/
		private EXAMPLE[] ex;
		private SVECTOR[] fycache;
		private int n;
		private STRUCTMODEL sm;
		private STRUCT_LEARN_PARM sparm;
		private int startIndex;
		private int endIndex;
		private int threadIndex=0;
		private int localnumIt;
		
		/*****output parameters***/
		private double[] local_lhs_n;
		private double local_rhs_g=0;
		//private double local_rt_psi_g;
		//private double local_rt_viol_g;
		private int local_argmax_count_g=0;
		
		/*****local parameters************/
		private double local_rhs_i_g=0;
		private SVECTOR local_fydelta_g=null;
		private svm_struct_api local_ssa=null;
		private LABEL[] mostViolatedLabels=null;
		private boolean[] violatedValid=null;
		
		
		@Override
		public void run() {
			init();
		   	
			for(int i=startIndex;i<endIndex;i++)
			{
				LABEL ybar=find_most_violated_constraint_local(ex[i], fycache[i], n, sm,sparm);
				add_list_n_ns(local_lhs_n, local_fydelta_g, 1.0);
				local_rhs_g+=local_rhs_i_g;	
				mostViolatedLabels[i]=ybar;
				violatedValid[i]=true;
			}
			
			//System.out.println("numIt:"+localnumIt+" thread:"+threadIndex+" has finished find most violated");
			//add_local_lhs(local_lhs_n);
			//add_local_rhs(local_rhs_g);
			//update_local_mostVoilated(mostViolatedLabels);
			//add_local_argmax_count(local_argmax_count_g);
			//subThreadsFinished[threadIndex]=1;
			updateSubThreadsFinished(threadIndex,1);
			//des();
			
		}
		
		private void init()
		{
			local_lhs_n = new double[sm.sizePsi+1];
			for(int i=0;i<local_lhs_n.length;i++)
			{
				local_lhs_n[i]=0;
			}
			mostViolatedLabels=new LABEL[n];
			violatedValid=new boolean[n];
			for(int i=0;i<n;i++)
			{
				violatedValid[i]=false;
			}
			local_ssa=svm_struct_api_factory.getSvmStructApi();
		}
		
		public void des()
		{
			local_lhs_n=null;
			mostViolatedLabels=null;
			local_ssa=null;
			violatedValid=null;
			/**********free memory******/
			local_fydelta_g=null;
			local_lhs_n=null;
			/**************************/
		}
		
		private void add_list_n_ns(double[] vec_n, SVECTOR vec_s,
				double faktor) {
			SVECTOR f;
			for (f = vec_s; f != null; f = f.next)
				add_vector_ns(vec_n, f, f.factor * faktor);
			/**********free memory******/
			f=null;
			/**************************/
		}
		
		private  void add_vector_ns(double[] vec_n, SVECTOR vec_s,
				double faktor) {
			
			WORD[] ai;
			ai = vec_s.words;
			for (int i = 0; i < ai.length; i++) {
				if (ai[i] != null) {
					vec_n[ai[i].wnum] += (faktor * ai[i].weight);
					//vec_n[ai[i].wnum]=WeightsUpdate.sum(vec_n[ai[i].wnum], WeightsUpdate.mul(faktor, ai[i].weight));
				} else {
					continue;
				}
			}
			
			/**********free memory******/
			if(ai!=null)
			{
				for(int i=0;i<ai.length;i++)
				{
					ai[i]=null;
				}
			}
			ai=null;
			/**************************/
		}
		/*
		 * returns fydelta=fy-fybar and rhs scalar value that correspond to the most
		 * violated constraint for example ex
		 */
		private LABEL find_most_violated_constraint_local(EXAMPLE ex, SVECTOR fycached,
				int n, STRUCTMODEL sm, STRUCT_LEARN_PARM sparm)
		{
			
			LABEL ybar;
			SVECTOR fybar, fy;
			double factor, lossval;
			
			setLocal_argmax_count_g(getLocal_argmax_count_g() + 1);
			if (sparm.loss_type == SLACK_RESCALING) {
				ybar = local_ssa.find_most_violated_constraint_slackrescaling(ex.x, ex.y, sm, sparm);
			} else {
				ybar = local_ssa.find_most_violated_constraint_marginrescaling(ex.x, ex.y,sm, sparm);
			}
			


			/**** get psi(x,y) and psi(x,ybar) ****/

			if (fycached != null)
				fy = copy_svector(fycached);
			else
				fy = ssa.psi(ex.x, ex.y, sm, sparm);
			// logger.info("ybar label info:"+ybar.class_index);
			fybar = ssa.psi(ex.x, ybar, sm, sparm);
	
			lossval = ssa.loss(ex.y, ybar, sparm);

			/**** scale feature vector and margin by loss ****/
			if (sparm.loss_type == SLACK_RESCALING)
				factor = lossval / n;
			else
				/* do not rescale vector for */
				factor = 1.0 / n; /* margin rescaling loss type */
			mult_svector_list(fy, factor);
			mult_svector_list(fybar, -factor);
			append_svector_list(fybar, fy); /* compute fy-fybar */

			setLocal_fydelta_g(fybar);
			setLocal_rhs_i_g(lossval / (double)n);

			fy=null;
			fybar=null;
			
			return ybar;
		}
		
		private  void mult_svector_list(SVECTOR a, double factor)
		/* multiplies the factor of each element in vector list with factor */
		{
			SVECTOR f;

			for (f = a; f != null; f = f.next)
				f.factor *= factor;
		}
		
		private  void append_svector_list(SVECTOR a, SVECTOR b)
		/* appends SVECTOR b to the end of SVECTOR a. */
		{
			SVECTOR f;

			for (f = a; f.next != null; f = f.next)
				; /* find end of first vector list */
			f.next = b; /* append the two vector lists */
		}
		
		private  SVECTOR copy_svector(SVECTOR vec) {
			SVECTOR newvec = null;
			if (vec != null) {
				newvec = create_svector(vec.words, vec.userdefined, vec.factor);
				newvec.kernel_id = vec.kernel_id;
				newvec.next = copy_svector(vec.next);
			}
			return (newvec);
		}
		
		private  SVECTOR create_svector(WORD[] words, String userdefined,
				double factor) {
			SVECTOR vec;
			int  i;
			vec = new SVECTOR();
			vec.words = new WORD[words.length];

			for (i = 0; i < words.length; i++) {
				//before reform
				//vec.words[i] = words[i];
				vec.words[i] = words[i].copy_word();
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
		

		public void setEx(EXAMPLE[] ex) {
			this.ex = ex;
		}


		public void setFycache(SVECTOR[] fycache) {
			this.fycache = fycache;
		}


		public void setN(int n) {
			this.n = n;
		}

		public void setSm(STRUCTMODEL sm) {
			this.sm = sm;
		}


		public void setSparm(STRUCT_LEARN_PARM sparm) {
			this.sparm = sparm;
		}

		public double[] getLocal_lhs_n() {
			return local_lhs_n;
		}


		public double getLocal_rhs_g() {
			return local_rhs_g;
		}





		public int getLocal_argmax_count_g() {
			return local_argmax_count_g;
		}

		public void setLocal_argmax_count_g(int local_argmax_count_g) {
			this.local_argmax_count_g = local_argmax_count_g;
		}

	

		public void setLocal_rhs_i_g(double local_rhs_i_g) {
			this.local_rhs_i_g = local_rhs_i_g;
		}



		public void setLocal_fydelta_g(SVECTOR local_fydelta_g) {
			this.local_fydelta_g = local_fydelta_g;
		}

		public LABEL[] getMostViolatedLabels() {
			return mostViolatedLabels;
		}


		
		public boolean[] getViolatedValid()
		{
			return violatedValid;
		}

	

		public void setStartIndex(int startIndex) {
			this.startIndex = startIndex;
		}


		public void setEndIndex(int endIndex) {
			this.endIndex = endIndex;
		}
		

		public void setThreadIndex(int threadIndex) {
			this.threadIndex = threadIndex;
		}


		public void setLocalnumIt(int localnumIt) {
			this.localnumIt = localnumIt;
		}
	}

}
