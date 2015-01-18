package cn.clickwise.classify.simplify;

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
	//////public int kernel_cache_statistic;
	public static final int MAXSHRINK = 50000;
	public double maxdiff;

	public int misclassified = 0;
	public int inconsistentnum = 0;
	public double maxsharedviol;
	//public double maxviol = 0;
	//////public double[] alpha_g;

	public svm_common sc=null;
	public svm_learn() {
		sc=new svm_common();
	}


	/**
	   The following solves a freely defined and given set of   
	   inequalities. The optimization problem is of the following form:

	   min 0.5 w*w + C sum_i C_i \xi_i
	   s.t. x_i * w > rhs_i - \xi_i

	   This corresponds to the -z o option.
	   includes: model.alpha[i]
	             model.sv_num
	             model.supvec[i]
	   the x_is are not the origin doc in the sample,but the constraints building on all samples          
	   return model 
    */
	public void svm_learn_optimization(DOC[] docs, double[] rhs, int totdoc,
			int totwords, LEARNPARM learn_parm, KERNELPARM kernel_parm, MODEL model, double[] alpha) {

		//free memory at the end of the methods
		int[] label;
		
		//lin 的作用是什么？ store  x_i * w,在check_optimality用到
		double[] lin; 
		double[] a;
		double[] c;
		int[] unlabeled;
		int[] inconsistent;
		int[] index;
		int[] index2dnum;
		double[] weights;
		double[] slack;
		double[] alphaslack;
		double[] aicache;
		
		int i;
		int misclassified, upsupvecnum;
		double loss = 0, model_length, alphasum, example_length;
	      
		//Setting default regularization parameter C if C is not setted
		double r_delta_avg;
		double runtime_start, runtime_end;
		int iterations, maxslackid, svsetnum = 0;
	

		SHRINKSTATE shrink_state = new SHRINKSTATE();

		runtime_start = TimeOpera.getCurrentTimeLong();

		//////kernel_cache_statistic = 0;
		learn_parm.totwords = totwords;
		if ((learn_parm.svm_newvarsinqp < 2)
				|| (learn_parm.svm_newvarsinqp > learn_parm.svm_maxqpsize)) {
			learn_parm.svm_newvarsinqp = learn_parm.svm_maxqpsize;
		}
		
		init_shrink_state(shrink_state, totdoc, MAXSHRINK);

		label = new int[totdoc];
		unlabeled = new int[totdoc];
		inconsistent = new int[totdoc];
		c = new double[totdoc];
		a = new double[totdoc];
		lin = new double[totdoc];

		learn_parm.svm_cost = new double[totdoc];
		
		//initialize the model
		model.supvec = new DOC[totdoc + 2];
		model.alpha = new double[totdoc + 2];	
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

	
		r_delta_avg = estimate_r_delta_average(docs, totdoc, kernel_parm);

		if (learn_parm.svm_c == 0) {
			learn_parm.svm_c = 1.0 / (r_delta_avg * r_delta_avg);
			if (SVMCommon.verbosity >= 1) {
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

			index = new int[totdoc];
			index2dnum = new int[totdoc + 11];
			if (kernel_parm.kernel_type == SVMCommon.LINEAR) {
				weights = new double[totwords + 1];
				//svm_common.clear_nvector(weights, totwords);
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


			compute_index(index, totdoc, index2dnum);
			update_linear_component(docs, label, index2dnum, alpha, a,
					index2dnum, totdoc, totwords, kernel_parm, 
					lin, aicache, weights);

			calculate_svm_model(docs, label, unlabeled, lin, alpha, a, c,
					learn_parm, index2dnum, index2dnum, model);

			for (i = 0; i < totdoc; i++) {
				a[i] = alpha[i];
			}
	
		}

		if (learn_parm.remove_inconsistent != 0) {
			learn_parm.remove_inconsistent = 0;
			System.err.println("'remove inconsistent' not available in this mode. Switching option off!");
		}

	
		if (learn_parm.sharedslack != 0) {
		
			iterations = optimize_to_convergence_sharedslack(docs, label,
					totdoc, totwords, learn_parm, kernel_parm, 
					shrink_state, model, a, lin, c);

		}

		if (SVMCommon.verbosity >= 1) {
			if (SVMCommon.verbosity == 1) {
				System.out.println("done. (" + iterations + " iterations)\n");
			}
			misclassified = 0;
			for (i = 0; (i < totdoc); i++) { /* get final statistic */
				if ((lin[i] - model.b) * (double) label[i] <= 0.0)
					misclassified++;
			}

			System.out.println("Optimization finished (maxdiff=" + maxdiff+ ").");

			runtime_end = SVMCommon.get_runtime();

			System.out.println("Runtime in cpu-seconds: "+ (runtime_end - runtime_start) / 100.0);
			
		}

		if ((SVMCommon.verbosity >= 1)
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
			System.err.println("Dual objective value: dval="
					+ (alphasum - 0.5 * model_length * model_length));
			System.err.println("Norm of weight vector: |w|=" + model_length);
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

		if ((SVMCommon.verbosity >= 1)
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

		/****simplify*******
		if (SVMCommon.verbosity >= 1) {
			System.out.println("Number of kernel evaluations: "
					+ kernel_cache_statistic);
		}
		*/

		if (alpha != null) {
			for (i = 0; i < totdoc; i++) { /* copy final alphas */
				alpha[i] = a[i];
			}
		}

		if (learn_parm.alphafile != null) {
			write_alphas(learn_parm.alphafile, a, label, totdoc);
		}
	
		/****simplify******
		if (alpha == null) {
			alpha_g = alpha;
		} else {
			alpha_g = new double[alpha.length];
			for (int j = 0; j < alpha_g.length; j++) {
				alpha_g[j] = alpha[j];
			}
		}
		*/
		
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

	public void shrink_state_cleanup(SHRINKSTATE shrink_state)
	{
		shrink_state.active=null;
		shrink_state.inactive_since=null;
		
		if(shrink_state.deactnum > 0) 
			shrink_state.a_history[shrink_state.deactnum-1]=null;
		shrink_state.a_history=null;
		shrink_state.last_a=null;
		shrink_state.last_lin=null;
	}
	
	public void init_shrink_state(SHRINKSTATE shrink_state, int totdoc,
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
			int totdoc, int totwords, KERNELPARM kernel_parm, double[] lin, double[] aicache,
			double[] weights) {
		int i, ii, j, jj;
		double tec;
		SVECTOR f;

		//linear kernel
		if (kernel_parm.kernel_type == 0) {
			for (ii = 0; (i = working2dnum[ii]) >= 0; ii++) {
				if (a[i] != a_old[i]) {
					for (f = docs[i].fvec; f != null; f = f.next) {	
						svm_common.add_vector_ns(weights, f, f.factor
								* ((a[i] - a_old[i]) * label[i]));
					}
				}
			}

			for (jj = 0; (j = active2dnum[jj]) >= 0; jj++) {
				for (f = docs[j].fvec; f != null; f = f.next) {
					lin[j] += f.factor * svm_common.sprod_ns(weights, f);
				}
			}

			for (ii = 0; (i = working2dnum[ii]) >= 0; ii++) {
				if (a[i] != a_old[i]) {
					for (f = docs[i].fvec; f != null; f = f.next) {
						svm_common.mult_vector_ns(weights, f, 0.0);
					}
				}
			}
		} 
		
		/********free memory**********/
		f=null;
		/****************************/
	}

	public double estimate_r_delta_average(DOC[] docs, int totdoc,
			KERNELPARM kernel_parm) {
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

		/********free memory**********/
		nulldoc=null;
		nullword[0]=null;
		nullword=null;
		/****************************/
		
		return (avgxlen / totdoc);
	}

	public boolean kernel_cache_space_available(KERNELCACHE kernel_cache) {
		if (kernel_cache.elems < kernel_cache.max_elems) {
			return true;
		} else {
			return false;
		}
	}


	public boolean kernel_cache_check(KERNELCACHE kernel_cache, int docnum) {
		return (kernel_cache.index[docnum] != -1);
	}

	public double[] kernel_cache_clean_and_malloc(KERNELCACHE kernel_cache,
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

	public int kernel_cache_malloc(KERNELCACHE kernel_cache) {
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

	public boolean kernel_cache_free_lru(KERNELCACHE kernel_cache) {
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

	public void kernel_cache_free(KERNELCACHE kernel_cache, int i) {
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

	public void get_kernel_row(KERNELCACHE kernel_cache, DOC[] docs,
			int docnum, int totdoc, int[] active2dnum, double[] buffer,
			KERNELPARM kernel_parm) {
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

	/**
	 * update the svm model parmaters ,that is
	 *      model.alpha[i]
	 *      model.sv_num
	 *      model.supvec[i] 
	 *      et al.
	 * @param docs
	 * @param label
	 * @param unlabeled
	 * @param lin
	 * @param a
	 * @param a_old
	 * @param c
	 * @param learn_parm
	 * @param working2dnum
	 * @param active2dnum
	 * @param model
	 * @return
	 */
	public int calculate_svm_model(DOC[] docs, int[] label, int[] unlabeled,
			double[] lin, double[] a, double[] a_old, double[] c,
			LEARNPARM learn_parm, int[] working2dnum, int[] active2dnum,
			MODEL model) {
		int i, ii, pos, b_calculated = 0, first_low, first_high;
		double ex_c, b_temp, b_low, b_high;
	

		if (learn_parm.biased_hyperplane == 0) {
			model.b = 0;
			b_calculated = 1;
		}



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


		}



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

			
		}
		

		return (model.sv_num - 1);
	}



	public void kernel_cache_reset_lru(KERNELCACHE kernel_cache) {
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
			LEARNPARM learn_parm, int[] inconsistent, int[] active2dnum,
			int[] working2dnum, double[] selcrit, int[] select, int cache_only, int[] key, int[] chosen) {

		int choosenum, i, j, k, activedoc, inum, valid;
		double s;

		for (inum = 0; working2dnum[inum] >= 0; inum++)
			;
	
		choosenum = 0;
		activedoc = 0;

		for (i = 0; (j = active2dnum[i]) >= 0; i++) {
			
			s = -label[j];
		    valid = 1;

			if ((valid != 0)
					&& (!((a[j] <= (0 + learn_parm.epsilon_a)) && (s < 0)))
					&& (!((a[j] >= (learn_parm.svm_cost[j] - learn_parm.epsilon_a)) && (s > 0)))
					&& (chosen[j] == 0) && (label[j] != 0)
					&& (inconsistent[j] == 0)) {

				selcrit[activedoc] = (double) label[j]
						* (learn_parm.eps - (double) label[j] * c[j] + (double) label[j]
								* lin[j]);

					key[activedoc] = j;
					activedoc++;
				
			}
		}

	
		select_top_n(selcrit, activedoc, select, qp_size / 2);

		for (k = 0; (choosenum < ((qp_size) / 2)) && (k < (qp_size / 2))
				&& (k < activedoc); k++) {

			i = key[select[k]];
			chosen[i] = 1;
			working2dnum[inum + choosenum] = i;

		
			choosenum += 1;
		}
	
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
					key[activedoc] = j;				
					activedoc++;
				
			}
		}


		select_top_n(selcrit, activedoc, select, qp_size / 2);

		for (k = 0; (choosenum < qp_size) && (k < ((qp_size) / 2))
				&& (k < activedoc); k++) {
			i = key[select[k]];
			chosen[i] = 1;
			working2dnum[inum + choosenum] = i;
			choosenum += 1;
			
		}

		working2dnum[inum + choosenum] = -1; /* complete index */
		return (choosenum);
	}

	void select_top_n(double[] selcrit, int range, int[] select, int n) {

		int i, j;

		for (i = 0; (i < n) && (i < range); i++) {
			for (j = i; j >= 0; j--) {
				if ((j > 0) && (selcrit[select[j - 1]] < selcrit[i])) {
					select[j] = select[j - 1];
				} else {
					select[j] = i;
					j = -1;
				}
			}
		}

		if (n > 0) {
			for (i = n; i < range; i++) {

				if (selcrit[i] > selcrit[select[n - 1]]) {
					for (j = n - 1; j >= 0; j--) {
						if ((j > 0) && (selcrit[select[j - 1]] < selcrit[i])) {
							select[j] = select[j - 1];
						} else {
							select[j] = i;
							j = -1;
						}
					}
				}

			}
		}
	}


	public int select_next_qp_subproblem_rand(int[] label, int[] unlabeled,
			double[] a, double[] lin, double[] c, int totdoc, int qp_size,
			LEARNPARM learn_parm, int[] inconsistent, int[] active2dnum,
			int[] working2dnum, double[] selcrit, int[] select, int[] key, int[] chosen, int iteration) {
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
		}
		working2dnum[inum + choosenum] = -1; /* complete index */
		return (choosenum);

	}

	public void cache_multiple_kernel_rows(KERNELCACHE kernel_cache,
			DOC[] docs, int[] key, int varnum, KERNELPARM kernel_parm) {
		int i;
		for (i = 0; i < varnum; i++) { /* fill up kernel cache */
			cache_kernel_row(kernel_cache, docs, key[i], kernel_parm);
		}
	}

	/**
	 * solve the dual problem
	 * @param docs
	 * @param label
	 * @param unlabeled
	 * @param exclude_from_eq_const
	 * @param eq_target
	 * @param chosen
	 * @param active2dnum
	 * @param model
	 * @param totdoc
	 * @param working2dnum
	 * @param varnum
	 * @param a
	 * @param lin
	 * @param c
	 * @param learn_parm
	 * @param aicache
	 * @param kernel_parm
	 * @param qp
	 * @param epsilon_crit_target
	 */
	public void optimize_svm(DOC[] docs, int[] label, int[] unlabeled,
			int[] exclude_from_eq_const, double eq_target, int[] chosen,
			int[] active2dnum, MODEL model, int totdoc, int[] working2dnum,
			int varnum, double[] a, double[] lin, double[] c,
			LEARNPARM learn_parm, double[] aicache, KERNELPARM kernel_parm,
			QP qp, double epsilon_crit_target) {
		int i;
		double[] a_v;
		
		compute_matrices_for_optimization(docs, label, unlabeled,
				exclude_from_eq_const, eq_target, chosen, active2dnum,
				working2dnum, model, a, lin, c, varnum, totdoc, learn_parm,
				aicache, kernel_parm, qp);
	

		/* call the qp-subsolver */
		SVMHideo shid = new SVMHideo();

		/**
		 * the weights (a_v = primal) of qp problem(D1) is the dual variables of P1 
		 */
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
			LEARNPARM learn_parm, double[] aicache, KERNELPARM kernel_parm,
			QP qp) {

		int ki, kj, i, j;
		double kernel_temp;

		if (SVMCommon.verbosity >= 3) {
			logger.info("Computing qp-matrices (type "
					+ kernel_parm.kernel_type + " kernel [degree "
					+ kernel_parm.poly_degree + ", rbf_gamma "
					+ kernel_parm.rbf_gamma + ", coef_lin "
					+ kernel_parm.coef_lin + ",coef_const "
					+ kernel_parm.coef_const + "]...");
		}

		qp.opt_n = varnum;
		qp.opt_ce0 = -eq_target;

	
		for (j = 1; j < model.sv_num; j++) {
		
			if ((chosen[model.supvec[j].docnum] == 0)
					&& (exclude_from_eq_const[model.supvec[j].docnum] == 0)) {
				qp.opt_ce0 += model.alpha[j];
			}
		}

		if (learn_parm.biased_hyperplane != 0) {
			qp.opt_m = 1;
		} else {
			qp.opt_m = 0;
		}

		for (i = 0; i < varnum; i++) {
			qp.opt_g0[i] = lin[key[i]];
		}

		for (i = 0; i < varnum; i++) {
			ki = key[i];
			qp.opt_ce[i] = label[ki];
			qp.opt_low[i] = 0;
			qp.opt_up[i] = learn_parm.svm_cost[ki];
		
			kernel_temp = sc.kernel(kernel_parm, docs[ki], docs[ki]);
		
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

			}
	

		}

		for (i = 0; i < varnum; i++) {
			/* assure starting at feasible point */
			qp.opt_xinit[i] = a[key[i]];
			/* set linear part of objective function */
			qp.opt_g0[i] = (learn_parm.eps - (double) label[key[i]] * c[key[i]])
					+ qp.opt_g0[i] * (double) label[key[i]];
		}

		if (SVMCommon.verbosity >= 3) {
			logger.info("done");
		}

	}

	/** Return value of objective function.  Works only relative to the active variables! */
	public double compute_objective_function(double[] a, double[] lin,
			double[] c, double eps, int[] label, int[] active2dnum)
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
			LEARNPARM learn_parm, double epsilon_crit_org, int[] inconsistent,
			int[] active2dnum, int[] last_suboptimal_at, int iteration,
			KERNELPARM kernel_parm) {
		int i, ii, retrain;
		double dist = 0, ex_c, target;
		// logger.info("in check_optimality");
		if (kernel_parm.kernel_type == SVMCommon.LINEAR) { /* be optimistic */
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
			double[] a, SHRINKSTATE shrink_state, double[] lin, double[] c,
			int totdoc, int totwords, int iteration, LEARNPARM learn_parm,
			int[] inconsistent, DOC[] docs, KERNELPARM kernel_parm,
			KERNELCACHE kernel_cache, MODEL model, double[] aicache,
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

		if (kernel_parm.kernel_type == SVMCommon.LINEAR) {
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
				if (SVMCommon.verbosity >= 2) {
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
				} else if (learn_parm.sharedslack != 0) { /* make all active when sharedslack*/
					shrink_state.active[i] = 1;
				}
			}
		}

		if (kernel_parm.kernel_type != SVMCommon.LINEAR) { /* update history for non-linear*/
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
			int transductcycle, KERNELPARM kernel_parm, LEARNPARM learn_parm) {
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
			posratio = (double) orgpos / (double) (orgpos + orgneg); /* use ratio of pos/neg*/
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
		if (SVMCommon.verbosity >= 2) {
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
			if (SVMCommon.verbosity >= 1) {
				System.out.println("Classifying unlabeled data as "
						+ unsupaddnum1 + " POS / " + unsupaddnum2 + " NEG.");

			}
			if (SVMCommon.verbosity >= 1) {
				System.out.println("Retraining.");
			}
			if (SVMCommon.verbosity >= 2) {
				// System.out.println();
			}
			return 3;
		}

		if ((transductcycle % check_every) == 0) {
			if (SVMCommon.verbosity >= 1) {
				System.out.println("Retraining.");
			}
			if (SVMCommon.verbosity >= 2) {
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

			if (SVMCommon.verbosity >= 2) {
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
			if (SVMCommon.verbosity >= 2) {
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
					if (SVMCommon.verbosity >= 1) {
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
				if (SVMCommon.verbosity >= 1)
					System.out
							.println("Increasing influence of unlabeled examples to "
									+ learn_parm.svm_unlabbound * 100.0 + " .");
			} else if (SVMCommon.verbosity >= 1) {
				System.out.println(upos + " positive -> Switching labels of "
						+ unsupaddnum1 + " POS / " + unsupaddnum2
						+ " NEG unlabeled examples.");
			}

			if (SVMCommon.verbosity >= 2) {
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
			LEARNPARM learn_parm) {
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

		if (SVMCommon.verbosity >= 1) {
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
		if (SVMCommon.verbosity >= 1) {
			// System.out.println("done");
		}

	}

	/**
	 * Shrink some variables away. Do the shrinking only if at least minshrink
	 * variables can be removed.
	 */
	public int shrink_problem(DOC[] docs, LEARNPARM learn_parm,
			SHRINKSTATE shrink_state, KERNELPARM kernel_parm,
			int[] active2dnum, int[] last_suboptimal_at, int iteration,
			int totdoc, int minshrink, double[] a, int[] inconsistent)
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
			if (SVMCommon.verbosity >= 2) {
				System.out.println(" Shrinking...");
			}
			if (kernel_parm.kernel_type != SVMCommon.LINEAR) { /*
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
			if (kernel_parm.kernel_type == SVMCommon.LINEAR) {
				shrink_state.deactnum = 0;
			}
			if (SVMCommon.verbosity >= 2) {
				System.out.println("done.\n");
				System.out.println(" Number of inactive variables = "
						+ (totdoc - activenum));
			}
		}
		return (activenum);
	}

	/**
	 * Remove numshrink columns in the cache which correspond to examples marked
	 * 0 in after.
	 */
	public void kernel_cache_shrink(KERNELCACHE kernel_cache, int totdoc,
			int numshrink, int[] after)
	{
		int i, j, jj, from = 0, to = 0, scount;
		int[] keep;

		if (SVMCommon.verbosity >= 2) {
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

		if (SVMCommon.verbosity >= 2) {
			System.out.println("done.");
			System.out.println(" Cache-size in rows = "
					+ kernel_cache.max_elems);
		}
	}

	/** Throw out examples with multipliers at upper bound. This  corresponds to the -i 1 option. 
	 ATTENTION: this is just a heuristic for finding a close  to minimum number of examples to exclude to 
	 make the problem separable with desired margin */
	int identify_inconsistent(double[] a, int[] label, int[] unlabeled,
			int totdoc, LEARNPARM learn_parm, int[] inconsistent) {
		int i, retrain;
		retrain = 0;
		for (i = 0; i < totdoc; i++) {
			if ((inconsistent[i] == 0)
					&& (unlabeled[i] == 0)
					&& (a[i] >= (learn_parm.svm_cost[i] - learn_parm.epsilon_a))) {
				inconsistentnum++;
				inconsistent[i] = 1; /* never choose again */
				retrain = 2; /* start over */
				if (SVMCommon.verbosity >= 3) {
					System.out.println("inconsistent(" + i + ")..");
				}
			}
		}
		return (retrain);
	}


	/** Throw out misclassified examples. This  corresponds to the -i 2 option. 
	 ATTENTION: this is just a heuristic for finding a close  to minimum number of examples to exclude to 
	 make the problem separable with desired margin */
	public int identify_misclassified(double[] lin, int[] label,
			int[] unlabeled, int totdoc, MODEL model, int[] inconsistent) {
		int i, retrain;
		double dist;
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
				if (SVMCommon.verbosity >= 3) {
					System.out.println("inconsistent(" + i + ")..");
				}
			}
		}
		return (retrain);
	}

	/** Throw out the 'most misclassified' example. This corresponds to the -i 3 option. 
	 ATTENTION: this is just a heuristic for finding a close to minimum number of examples to exclude to 
	 make the problem separable with desired margin */
	public int identify_one_misclassified(double[] lin, int[] label,
			int[] unlabeled, int totdoc, MODEL model, int[] inconsistent) {
		int i, retrain, maxex = -1;
		double dist, maxdist = 0;
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
			if (SVMCommon.verbosity >= 3) {
				System.out.println("inconsistent(" + i + ")..");
			}
		}
		return (retrain);
	}

	/***
	 * 此方法有问题
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
			int totdoc, int totwords, LEARNPARM learn_parm,
			KERNELPARM kernel_parm,
			SHRINKSTATE shrink_state, MODEL model, double[] a, double[] lin,
			double[] c) {
	
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
		SVMCommon.verbosity = 0;
		epsilon_crit_org = learn_parm.epsilon_crit; /* save org */
		if (kernel_parm.kernel_type == SVMCommon.LINEAR) {
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
		qp.opt_ce0 = 0;
		qp.opt_g = new double[learn_parm.svm_maxqpsize
				* learn_parm.svm_maxqpsize];

		qp.opt_g0 = new double[learn_parm.svm_maxqpsize];
		qp.opt_xinit = new double[learn_parm.svm_maxqpsize];
		qp.opt_low = new double[learn_parm.svm_maxqpsize];
		qp.opt_up = new double[learn_parm.svm_maxqpsize];

		if (kernel_parm.kernel_type == SVMCommon.LINEAR) {
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
			 
	

			if (SVMCommon.verbosity >= 0) {
				//if (iteration % 51 == 0) {
					//System.out.println("Iteration " + iteration + ": ");
				//	logger.info("Iteration " + iteration + ": ");
				//}
			} else if (SVMCommon.verbosity == 1) {
				System.out.println(".");
			}

			if (SVMCommon.verbosity >= 2) {
				t0 = SVMCommon.get_runtime();
			}

			if (SVMCommon.verbosity >= 3) {
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
				if (((iteration % 100) == 0) || (slackset == 0)
						|| (maxsharedviol < learn_parm.epsilon_crit)) {
					/* do a step with examples from different slack sets */
					if (SVMCommon.verbosity >= 2) {
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
						}
					}
					working2dnum[i] = -1;
					already_chosen = 0;
					if ((Math.min(learn_parm.svm_newvarsinqp,
							learn_parm.svm_maxqpsize - choosenum) >= 4)
							&& (kernel_parm.kernel_type != SVMCommon.LINEAR)) {
						/* select part of the working set from cache */

						
						already_chosen = select_next_qp_subproblem_grad(label,
								unlabeled, a, lin, c, totdoc, (int) (Math.min(
										learn_parm.svm_maxqpsize - choosenum,
										learn_parm.svm_newvarsinqp) / 2),
								learn_parm, inconsistent, active2dnum,
								working2dnum, selcrit, selexam, kernel_cache,
								1, key, chosen);

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
				} else { /* do a step with all examples from same slack set */
					if (SVMCommon.verbosity >= 2) {
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
				choosenum += select_next_qp_subproblem_rand(label, unlabeled,
						a, lin, c, totdoc, Math.min(learn_parm.svm_maxqpsize
								- choosenum, learn_parm.svm_newvarsinqp),
						learn_parm, inconsistent, active2dnum, working2dnum,
						selcrit, selexam, kernel_cache, key, chosen, iteration);
			}

			if (SVMCommon.verbosity >= 2) {
				System.out.println(choosenum + " vectors chosen");
			}

			if (SVMCommon.verbosity >= 2) {
				t1 = SVMCommon.get_runtime();
			}

			/*
			if (kernel_cache != null) {
				cache_multiple_kernel_rows(kernel_cache, docs, working2dnum,
						choosenum, kernel_parm);
			}
			*/

			if (SVMCommon.verbosity >= 2) {
				t2 = SVMCommon.get_runtime();
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

			if (SVMCommon.verbosity >= 2) {
				t3 = SVMCommon.get_runtime();
			}

			update_linear_component(docs, label, active2dnum, a, a_old,
					working2dnum, totdoc, totwords, kernel_parm, kernel_cache,
					lin, aicache, weights);
			compute_shared_slacks(docs, label, a, lin, c, active2dnum,
					learn_parm, slack, alphaslack);

			if (SVMCommon.verbosity >= 2) {
				t4 = SVMCommon.get_runtime();
			}

			supvecnum = calculate_svm_model(docs, label, unlabeled, lin, a,
					a_old, c, learn_parm, working2dnum, active2dnum, model);

			if (SVMCommon.verbosity >= 2) {
				t5 = SVMCommon.get_runtime();
			}

			/* The following computation of the objective function works only */
			/* relative to the active variables */
			if (SVMCommon.verbosity >= 3) {
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

			/* checking whether optimizer got stuck */
			if (maxdiff < bestmaxdiff) {
				bestmaxdiff = maxdiff;
				bestmaxdiffiter = iteration;
			}

			if (iteration > (bestmaxdiffiter + learn_parm.maxiter)) {
				/* long time no progress? */
				terminate = 1;
				retrain = 0;
				if (SVMCommon.verbosity >= 1) {
					System.out
							.println("\nWARNING: Relaxing KT-Conditions due to slow progress! Terminating!");
				}
			}
			noshrink = 0;

			if ((retrain == 0)
					&& (inactivenum > 0)
					&& ((learn_parm.skip_final_opt_check == 0) || (kernel_parm.kernel_type == SVMCommon.LINEAR))) {
				if (((SVMCommon.verbosity >= 1) && (kernel_parm.kernel_type != SVMCommon.LINEAR))
						|| (SVMCommon.verbosity >= 2)) {
					if (SVMCommon.verbosity == 1) {
						// System.out.println();
					}
					System.out.println(" Checking optimality of inactive variables...");

				}

				t1 = SVMCommon.get_runtime();
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
				
				if (((SVMCommon.verbosity >= 1) && (kernel_parm.kernel_type != SVMCommon.LINEAR))
						|| (SVMCommon.verbosity >= 2)) {
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
			if (SVMCommon.verbosity >= 2) {
				System.out.println(" => (" + supvecnum + " SV (incl. "+ model.at_upper_bound
						+ " SV at u-bound), max violation=" + maxdiff + ")");
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
				/*
				if ((kernel_cache != null)
						&& (supvecnum > kernel_cache.max_elems)
						&& ((kernel_cache.activenum - activenum) > Math.max(
								(int) (activenum / 10), 500))) {
					kernel_cache_shrink(kernel_cache, totdoc, Math.min(
							(kernel_cache.activenum - activenum),
							(kernel_cache.activenum - supvecnum)),
							shrink_state.active);
				}
				*/
			}

		}//end loop for

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
		  qp.opt_g=null;
		  qp.opt_g0=null;
		  qp.opt_xinit=null;
		  qp.opt_low=null;
		  qp.opt_up=null;
		}
		qp=null;
		
		if(weights!=null)
		{
			weights=null;
		}
		learn_parm.epsilon_crit = epsilon_crit_org; /* restore org */
		model.maxdiff = maxdiff;

		return (iteration);

	}

	/** compute the value of shared slacks and the joint alphas */
	public void compute_shared_slacks(DOC[] docs, int[] label, double[] a,
			double[] lin, double[] c, int[] active2dnum, LEARNPARM learn_parm,
			double[] slack, double[] alphaslack)
	{
		int jj, i;
		double dist, target;

		for (jj = 0; (i = active2dnum[jj]) >= 0; jj++) { /* clear slack variables */
			slack[docs[i].slackid] = 0.0;
			/* alphaslack[docs[i].slackid]=0.0; */
		}
		for (jj = 0; (i = active2dnum[jj]) >= 0; jj++) {
	
			dist = (lin[i]) * (double) label[i];

			target=-(learn_parm.eps-(double)label[i]*c[i]);

			if ((target - dist) > slack[docs[i].slackid])//0.5 set may be bug 
			{
				slack[docs[i].slackid]=target-dist;
				// alphaslack[docs[i].slackid]+=a[i];
			}
		}
	}

	public int select_next_qp_slackset(DOC[] docs, int[] label, double[] a,
			double[] lin, double[] slack, double[] alphaslack, double[] c,
			LEARNPARM learn_parm, int[] active2dnum)
	/* returns the slackset with the largest internal violation */
	{
		int i, ii, maxdiffid;
		double dist, target, maxdiff, ex_c;

		maxdiff = 0;
		maxdiffid = 0;
		for (ii = 0; (i = active2dnum[ii]) >= 0; ii++) {
			ex_c = learn_parm.svm_c - learn_parm.epsilon_a;
			if (alphaslack[docs[i].slackid] >= ex_c) {
				dist = (lin[i]) * (double) label[i] + slack[docs[i].slackid]; /* distance rhs of constraint */
				target = -(learn_parm.eps - (double) label[i] * c[i]); 
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

	/**
	 *  Check KT-conditions 
	 *  */
	public int check_optimality_sharedslack(DOC[] docs, MODEL model,
			int[] label, double[] a, double[] lin, double[] c, double[] slack,
			double[] alphaslack, int totdoc, LEARNPARM learn_parm,
			double epsilon_crit_org, int[] active2dnum,
			int[] last_suboptimal_at, int iteration, KERNELPARM kernel_parm)
	{
		int i, ii, retrain;
		double dist, dist_noslack, ex_c = 0, target;

		// logger.info("in check_optimality_sharedslack");
		if (kernel_parm.kernel_type == SVMCommon.LINEAR) { /* be optimistic */
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
			if ((a[i] > learn_parm.epsilon_a) && (dist > target)) {
				if ((dist - target) > maxdiff) { /* largest violation */
					maxdiff = dist - target;
				}
			}
			if ((alphaslack[docs[i].slackid] < ex_c)
					&& (slack[docs[i].slackid] > 0)) {
				if ((slack[docs[i].slackid]) > (maxdiff)) { /* largest violation */
					maxdiff = slack[docs[i].slackid];

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

    /** Approximates the radius of the ball containing the support vectors by bounding it with the 
	length of the longest support vector. This is documents have feature vectors of length 1.
    It assumes that the center of the ball is at the  origin of the space. 
    */
	public double estimate_sphere(MODEL model)
	
	{ 
		int j; 
		double xlen, maxxlen = 0;
		DOC nulldoc; 
		WORD[] nullword = new WORD[1];
		KERNELPARM kernel_parm = model.kernel_parm;
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
		
		/********free memory**********/
		nulldoc=null;
		nullword[0]=null;
		nullword=null;
		/****************************/

		return (maxxlen);
	}

	public double length_of_longest_document_vector(DOC[] docs, int totdoc,
			KERNELPARM kernel_parm) {
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

		for (i = 0; i < totdoc; i++) {
			pw.println(a[i] * (double) label[i]);
		}
		try {
			pw.close();
			fw.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	
	}

	public double estimate_r_delta(DOC[] docs, int totdoc,
			KERNELPARM kernel_parm) {
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
	
			xlen = Math.sqrt(sc.kernel(kernel_parm, docs[i], docs[i])
					- 2 * sc.kernel(kernel_parm, docs[i], nulldoc)
					+ sc.kernel(kernel_parm, nulldoc, nulldoc));
			if (xlen > maxxlen) {
				maxxlen = xlen;
			}
		}

		/********free memory**********/
		nulldoc=null;
		nullword[0]=null;
		nullword=null;
		/****************************/
		
		return (maxxlen);
	}

	/** optional: length of model vector in feature space 
	    optional: radius of ball containing the data */
	public double estimate_margin_vcdim(MODEL model, double w, double R)
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

	
	/**
	 * Computes xa-estimate of error rate, recall, and precision. See T.
	 * Joachims, Estimating the Generalization Performance of an SVM
	 * Efficiently, IMCL, 2000.
	 */
	public void compute_xa_estimates(MODEL model, int[] label, int[] unlabeled,
			int totdoc, DOC[] docs, double[] lin, double[] a,
			KERNELPARM kernel_parm, LEARNPARM learn_parm)
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

	/**
	 * Experimental Code improving plain XiAlpha Estimates by computing a better
	 * bound using a greedy optimzation strategy.
	 */
	public double distribute_alpha_t_greedily(int[] sv2dnum, int svnum,
			DOC[] docs, double[] a, int docnum, int[] label,
			KERNELPARM kernel_parm, LEARNPARM learn_parm, double thresh)
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

	/**
	 * Loo-bound based on observation that loo-errors must have an equal
	 * distribution in both training and test examples, given that the test
	 * examples are classified correctly. Compare chapter
	 * "Constraints on the Transductive Hyperplane" in my Dissertation.
	 */
	public void estimate_transduction_quality(MODEL model, int[] label,
			int[] unlabeled, int totdoc, DOC[] docs, double[] lin)
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

	public static KERNELCACHE kernel_cache_init(int totdoc, int buffsize) {
		int i;
		KERNELCACHE kernel_cache = new KERNELCACHE();

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
