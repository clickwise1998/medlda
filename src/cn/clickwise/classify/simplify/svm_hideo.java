package cn.clickwise.classify.simplify;

import org.apache.log4j.Logger;

import cn.clickwise.math.random.SimFunc;

/**
 * 解决下面的二次规划问题 minimize g0 * x + 1/2 x' * G * x s.t. ce*x - ce0 = 0 l <= x <= u
 * ce 的元素只能取值 -1 或 1
 * 
 * @author lq
 */

public class svm_hideo {

	private static final int PRIMAL_OPTIMAL = 1;
	private static final int DUAL_OPTIMAL = 2;
	private static final int MAXITER_EXCEEDED = 3;
	private static final int NAN_SOLUTION = 4;
	private static final int ONLY_ONE_VARIABLE = 5;
	private static final int QP_SIZE=10;

	private static final int SMALLROUND = 1;

	private static final double DEF_PRECISION = 1E-5;
	private static final int DEF_MAX_ITERATIONS = 200;
	private static final double DEF_LINDEP_SENSITIVITY = 1E-8;
	private static final double EPSILON_HIDEO = 1E-20;
	private static final double EPSILON_EQ = 1E-5;
	
	/**
	 * store the values of vector x 
	 */
	private static double[] primal = new double[QP_SIZE];
	
	/**
	 * store the values of dual variables respect to equality constraints and inequality constraints 
	 */
	private static double[] dual = new double[2 * (QP_SIZE + 1)];

	private long precision_violations = 0;
	private double opt_precision = DEF_PRECISION;
	private int maxiter = DEF_MAX_ITERATIONS;
	private static double lindep_sensitivity = DEF_LINDEP_SENSITIVITY;
	private static double[] buffer = new double[(QP_SIZE + 1) * 2 * (QP_SIZE + 1) * 2 + QP_SIZE * QP_SIZE + 2
	                 					* (QP_SIZE + 1) * 2 + 2 * QP_SIZE + 1 + 2 * QP_SIZE + QP_SIZE + QP_SIZE + QP_SIZE * QP_SIZE];
	private static int[] nonoptimal = new int[QP_SIZE];

	private  int smallroundcount = 0;

	private  int roundnumber = 0;

	private double progress;
	
	private static Logger logger = Logger.getLogger(svm_hideo.class);

	public double[] optimize_qp(QP qp, double epsilon_crit, int nx,
			double threshold, LEARN_PARM learn_param) {
		
		int i;
		int result;
		roundnumber++;

		if (primal == null) {
			/*
			primal = new double[nx];
			dual = new double[2 * (nx + 1)];
			nonoptimal = new int[nx];
			buffer = new double[(nx + 1) * 2 * (nx + 1) * 2 + nx * nx + 2
					* (nx + 1) * 2 + 2 * nx + 1 + 2 * nx + nx + nx + nx * nx];	
			for (i = 0; i < nx; i++) {
				primal[i] = 0;
			}
			*/
			threshold = 0;
		}

		result = optimize_hildreth_despo(qp.opt_n, qp.opt_m, opt_precision,
				epsilon_crit, learn_param.epsilon_a, maxiter, 0, 0,
				lindep_sensitivity, qp.opt_g, qp.opt_g0, qp.opt_ce, qp.opt_ce0,
				qp.opt_low, qp.opt_up, primal, qp.opt_xinit, dual, nonoptimal,
				buffer);

		if (learn_param.totwords < learn_param.svm_maxqpsize) {
			learn_param.svm_maxqpsize = SimFunc.maxi(learn_param.totwords, 2);
		}

		if (result == NAN_SOLUTION) {
			lindep_sensitivity *= 2;
			if (learn_param.svm_maxqpsize > 2) {
				learn_param.svm_maxqpsize--;
			}
			precision_violations++;
		}
	
		

		if ((result != PRIMAL_OPTIMAL) || (roundnumber % 31 == 0)
				|| (progress <= 0)) {
			smallroundcount++;

			result = optimize_hildreth_despo(qp.opt_n, qp.opt_m, opt_precision,
					epsilon_crit, learn_param.epsilon_a, maxiter,
					PRIMAL_OPTIMAL, SMALLROUND, lindep_sensitivity, qp.opt_g,
					qp.opt_g0, qp.opt_ce, qp.opt_ce0, qp.opt_low, qp.opt_up,
					primal, qp.opt_xinit, dual, nonoptimal, buffer);

			if (result != PRIMAL_OPTIMAL) {
				if (result != ONLY_ONE_VARIABLE) {
					precision_violations++;
				}

				if (result == MAXITER_EXCEEDED) {
					maxiter += 100;
				}

				if (result == NAN_SOLUTION) {
					lindep_sensitivity *= 2;

					for (i = 0; i < qp.opt_n; i++) {
						primal[i] = qp.opt_xinit[i];
					}
				}
			}
		}

		if (precision_violations > 50) {
			precision_violations = 0;
			epsilon_crit = 10.0;
			logger.info("\nWARNING: Relaxing epsilon on KT-Conditions "
					+ (epsilon_crit));
		
		}

		if ((qp.opt_m > 0) && (result != NAN_SOLUTION)
				&& (!(Double.isNaN(dual[1] - dual[0])))) {
			threshold = dual[1] - dual[0];
		} else {
			threshold = 0;
		}

		freeMemory();
		
		return primal;
	}

	public int optimize_hildreth_despo(int n, int m, double precision,
			double epsilon_crit, double epsilon_a, int maxiter, int goal,
			int smallround, double lindep_sensitivity, double[] g, double[] g0,
			double[] ce, double ce0, double[] low, double[] up,
			double[] primal, double[] init, double[] dual, int[] lin_dependent,
			double[] buffer) {
		int i, j, k, from, to, n_indep, changed;
		double sum, bmin = 0, bmax = 0;
		double[] d, d0, ig, dual_old, temp, start;
		double[] g0_new, g_new, ce_new, ce0_new, low_new, up_new;
		double add, t;
		int result;

		double obj_before, obj_after;
		int b1, b2;
		double g0_b1 = 0, g0_b2 = 0;
		//double ce0_b;

		g0_new = new double[n];
		d = new double[(n + m) * 2 * (n + m) * 2];
		d0 = new double[(n + m) * 2];
		ce_new = new double[n];
		ce0_new = new double[m];
		ig = new double[n * n];
		dual_old = new double[(n + m) * 2];
		low_new = new double[n];
		up_new = new double[n];
		start = new double[n];
		g_new = new double[n * n];
		temp = new double[n];

		b1 = -1;
		b2 = -1;

		for (i = 0; i < n; i++) {
			sum = g0[i];
			for (j = 0; j < n; j++) {
				sum += init[j] * g[i * n + j];
			}
			sum = sum * ce[i];
			if (((b1 == -1) || (sum < bmin))
					&& (!((init[i] <= (low[i] + epsilon_a)) && (ce[i] < 0.0)))
					&& (!((init[i] >= (up[i] - epsilon_a)) && (ce[i] > 0.0)))) {
				bmin = sum;
				b1 = i;
			}
			if (((b2 == -1) || (sum >= bmax))
					&& (!((init[i] <= (low[i] + epsilon_a)) && (ce[i] > 0.0)))
					&& (!((init[i] >= (up[i] - epsilon_a)) && (ce[i] < 0.0)))) {
				bmax = sum;
				b2 = i;
			}
		}

		if ((b1 == -1) || (b2 == -1)) {
			b1 = SimFunc.maxi(b1, b2);
			b2 = SimFunc.maxi(b1, b2);
		}

		for (i = 0; i < n; i++) {
			start[i] = init[i];
		}

		add = 0;
		changed = 0;
		if ((b1 != b2) && (m == 1)) {
			for (i = 0; i < n; i++) { /* fix other vectors */
				if (i == b1)
					g0_b1 = g0[i];
				if (i == b2)
					g0_b2 = g0[i];
			}
			//ce0_b = ce0;
			for (i = 0; i < n; i++) {
				if ((i != b1) && (i != b2)) {
					for (j = 0; j < n; j++) {
						if (j == b1)
							g0_b1 += start[i] * g[i * n + j];
						if (j == b2)
							g0_b2 += start[i] * g[i * n + j];
					}
					//ce0_b -= (start[i] * ce[i]);
				}
			}
			if ((g[b1 * n + b2] == g[b1 * n + b1])
					&& (g[b1 * n + b2] == g[b2 * n + b2])) {
				/* printf("euqal\n"); */
				if (ce[b1] == ce[b2]) {
					if (g0_b1 <= g0_b2) { /* set b1 to upper bound */
						/* printf("case +=<\n"); */
						changed = 1;
						t = up[b1] - init[b1];
						if ((init[b2] - low[b2]) < t) {
							t = init[b2] - low[b2];
						}
						start[b1] = init[b1] + t;
						start[b2] = init[b2] - t;
					} else if (g0_b1 > g0_b2) { /* set b2 to upper bound */
						/* printf("case +=>\n"); */
						changed = 1;
						t = up[b2] - init[b2];
						if ((init[b1] - low[b1]) < t) {
							t = init[b1] - low[b1];
						}
						start[b1] = init[b1] - t;
						start[b2] = init[b2] + t;
					}
				} else if (((g[b1 * n + b1] > 0) || (g[b2 * n + b2] > 0))) { 
					/* printf("case +!\n"); */
					t = ((ce[b2] / ce[b1]) * g0[b1] - g0[b2] + ce0
							* (g[b1 * n + b1] * ce[b2] / ce[b1] - g[b1 * n + b2]
									/ ce[b1]))
							/ ((ce[b2] * ce[b2] / (ce[b1] * ce[b1]))
									* g[b1 * n + b1] + g[b2 * n + b2] - 2 * (g[b1
									* n + b2]
									* ce[b2] / ce[b1])) - init[b2];
					changed = 1;
					if ((up[b2] - init[b2]) < t) {
						t = up[b2] - init[b2];
					}
					if ((init[b2] - low[b2]) < -t) {
						t = -(init[b2] - low[b2]);
					}
					if ((up[b1] - init[b1]) < t) {
						t = (up[b1] - init[b1]);
					}
					if ((init[b1] - low[b1]) < -t) {
						t = -(init[b1] - low[b1]);
					}
					start[b1] = init[b1] + t;
					start[b2] = init[b2] + t;
				}
			}
			if ((-g[b1 * n + b2] == g[b1 * n + b1])
					&& (-g[b1 * n + b2] == g[b2 * n + b2])) {
			
				if (ce[b1] != ce[b2]) {
					if ((g0_b1 + g0_b2) < 0) { /* set b1 and b2 to upper bound */
					
						changed = 1;
						t = up[b1] - init[b1];
						if ((up[b2] - init[b2]) < t) {
							t = up[b2] - init[b2];
						}
						start[b1] = init[b1] + t;
						start[b2] = init[b2] + t;
					} else if ((g0_b1 + g0_b2) >= 0) { /* set b1 and b2 to lower bound*/
						
						changed = 1;
						t = init[b1] - low[b1];
						if ((init[b2] - low[b2]) < t) {
							t = init[b2] - low[b2];
						}
						start[b1] = init[b1] - t;
						start[b2] = init[b2] - t;
					}
				} else if (((g[b1 * n + b1] > 0) || (g[b2 * n + b2] > 0))) { 
					
					t = ((ce[b2] / ce[b1]) * g0[b1] - g0[b2] + ce0
							* (g[b1 * n + b1] * ce[b2] / ce[b1] - g[b1 * n + b2]
									/ ce[b1]))
							/ ((ce[b2] * ce[b2] / (ce[b1] * ce[b1]))
									* g[b1 * n + b1] + g[b2 * n + b2] - 2 * (g[b1
									* n + b2]
									* ce[b2] / ce[b1])) - init[b2];
					changed = 1;
					if ((up[b2] - init[b2]) < t) {
						t = up[b2] - init[b2];
					}
					if ((init[b2] - low[b2]) < -t) {
						t = -(init[b2] - low[b2]);
					}
					if ((up[b1] - init[b1]) < -t) {
						t = -(up[b1] - init[b1]);
					}
					if ((init[b1] - low[b1]) < t) {
						t = init[b1] - low[b1];
					}
					start[b1] = init[b1] - t;
					start[b2] = init[b2] + t;
				}
			}
		}

		if ((m > 0)
				&& ((Math.abs(g[b1 * n + b1]) < lindep_sensitivity) || (Math
						.abs(g[b2 * n + b2]) < lindep_sensitivity))) {
			add += 0.093274;
		}
		
		/* in case both examples are linear dependent */
		else if ((m > 0)
				&& (g[b1 * n + b2] != 0 && g[b2 * n + b2] != 0)
				&& (Math.abs(g[b1 * n + b1] / g[b1 * n + b2] - g[b1 * n + b2]
						/ g[b2 * n + b2]) < lindep_sensitivity)) {
			add += 0.078274;
		}

		/* special case for zero diagonal entry on unbiased hyperplane */
		if ((m == 0) && (b1 >= 0)) {
			if (Math.abs(g[b1 * n + b1]) < lindep_sensitivity) {
	
				for (i = 0; i < n; i++) { /* fix other vectors */
					if (i == b1)
						g0_b1 = g0[i];
				}
				for (i = 0; i < n; i++) {
					if (i != b1) {
						for (j = 0; j < n; j++) {
							if (j == b1)
								g0_b1 += start[i] * g[i * n + j];
						}
					}
				}
				if (g0_b1 < 0)
					start[b1] = up[b1];
				if (g0_b1 >= 0)
					start[b1] = low[b1];
			}
		}

		if ((m == 0) && (b2 >= 0)) {
			if (Math.abs(g[b2 * n + b2]) < lindep_sensitivity) {
		
				for (i = 0; i < n; i++) { /* fix other vectors */
					if (i == b2)
						g0_b2 = g0[i];
				}
				for (i = 0; i < n; i++) {
					if (i != b2) {
						for (j = 0; j < n; j++) {
							if (j == b2)
								g0_b2 += start[i] * g[i * n + j];
						}
					}
				}
				if (g0_b2 < 0)
					start[b2] = up[b2];
				if (g0_b2 >= 0)
					start[b2] = low[b2];
			}
		}

		lcopy_matrix(g, n, d);

		if ((m == 1) && (add > 0.0)) {
			for (j = 0; j < n; j++) {
				for (k = 0; k < n; k++) {
					d[j * n + k] += add * ce[j] * ce[k];
				}
			}
		} else {
			add = 0.0;
		}
	
		if (n > 2) { /* switch, so that variables are better mixed */
			lswitchrk_matrix(d, n, b1, 0);
			if (b2 == 0) {
				lswitchrk_matrix(d, n, b1, 1);
			} else {
				lswitchrk_matrix(d, n, b2, 1);
			}
		}
		
		if (smallround == SMALLROUND) {
			for (i = 2; i < n; i++) {
				lin_dependent[i] = 1;
			}
			if (m > 0) { /* for biased hyperplane, pick two variables */
				lin_dependent[0] = 0;
				lin_dependent[1] = 0;
			} else { /* for unbiased hyperplane, pick only one variable */
				lin_dependent[0] = smallroundcount % 2;
				lin_dependent[1] = (smallroundcount + 1) % 2;
			}
		} else {
			for (i = 0; i < n; i++) {
				lin_dependent[i] = 0;
			}
		}


		linvert_matrix(d, n, ig, lindep_sensitivity, lin_dependent);

		if (n > 2) { /* now switch back */
			if (b2 == 0) {
				lswitchrk_matrix(ig, n, b1, 1);
				i = lin_dependent[1];
				lin_dependent[1] = lin_dependent[b1];
				lin_dependent[b1] = i;
			} else {
				lswitchrk_matrix(ig, n, b2, 1);
				i = lin_dependent[1];
				lin_dependent[1] = lin_dependent[b2];
				lin_dependent[b2] = i;
			}
			lswitchrk_matrix(ig, n, b1, 0);
			i = lin_dependent[0];
			lin_dependent[0] = lin_dependent[b1];
			lin_dependent[b1] = i;
		}
		
		lcopy_matrix(g, n, g_new); /* restore g_new matrix */
		if (add > 0)
			for (j = 0; j < n; j++) {
				for (k = 0; k < n; k++) {
					g_new[j * n + k] += add * ce[j] * ce[k];
				}
			}

		for (i = 0; i < n; i++) { /* fix linear dependent vectors */
			g0_new[i] = g0[i] + add * ce0 * ce[i];
		}
		if (m > 0)
			ce0_new[0] = -ce0;
		for (i = 0; i < n; i++) { /* fix linear dependent vectors */
			if (lin_dependent[i] > 0) {
				for (j = 0; j < n; j++) {
					if (lin_dependent[j] == 0) {
						g0_new[j] += start[i] * g_new[i * n + j];
					}
				}
				if (m > 0)
					ce0_new[0] -= (start[i] * ce[i]);
			}
		}

		from = 0; /* remove linear dependent vectors */
		to = 0;
		n_indep = 0;
		for (i = 0; i < n; i++) {
			if (lin_dependent[i] == 0) {
				g0_new[n_indep] = g0_new[i];
				ce_new[n_indep] = ce[i];
				low_new[n_indep] = low[i];
				up_new[n_indep] = up[i];
				primal[n_indep] = start[i];
				n_indep++;
			}
			for (j = 0; j < n; j++) {
				if ((lin_dependent[i] == 0) && (lin_dependent[j] == 0)) {
					ig[to] = ig[from];
					g_new[to] = g_new[from];
					to++;
				}
				from++;
			}
		}

		/* cannot optimize with only one variable */
		if ((n_indep <= 1) && (m > 0) && (changed == 0)) {
			for (i = n - 1; i >= 0; i--) {
				primal[i] = init[i];
			}
			return ((int) ONLY_ONE_VARIABLE);
		}

		if ((changed == 0) || (n_indep > 1)) {
			result = solve_dual(n_indep, m, precision, epsilon_crit, maxiter,
					g_new, g0_new, ce_new, ce0_new, low_new, up_new, primal, d,
					d0, ig, dual, dual_old, temp, goal);
		} else {
			result = PRIMAL_OPTIMAL;
		}

		j = n_indep;
		for (i = n - 1; i >= 0; i--) {
			if (lin_dependent[i] == 0) {
				j--;
				primal[i] = primal[j];
			} else {
				primal[i] = start[i]; /* leave as is */
			}
			temp[i] = primal[i];
		}

		obj_before = calculate_qp_objective(n, g, g0, init);
		obj_after = calculate_qp_objective(n, g, g0, primal);
		progress = obj_before - obj_after;

		/***free memory*******/
		
		d=null;
		d0=null;
		ig=null;
		dual_old=null;
		temp=null;
		start=null;
		g0_new=null;
		g_new=null;
		ce_new=null;
		ce0_new=null;
		low_new=null;
	    up_new=null;
	    /*****************/
		
		return ((int) result);
	}

	public int solve_dual(int n, int m, double precision, double epsilon_crit,
			int maxiter, double[] g, double[] g0, double[] ce, double[] ce0,
			double[] low, double[] up, double[] primal, double[] d,
			double[] d0, double[] ig, double[] dual, double[] dual_old,
			double[] temp, int goal) {
		int i, j, k, iter;
		double sum, w, maxviol, viol, temp1, temp2, isnantest;
		double model_b, dist;
		int retrain, maxfaktor, primal_optimal = 0,  scalemaxiter;
		//double at_bound;
		double epsilon_a = 1E-15, epsilon_hideo;
		double eq;

		if ((m < 0) || (m > 1)) {
			System.err.println("SOLVE DUAL: inappropriate number of eq-constrains!");
		}

		for (i = 0; i < 2 * (n + m); i++) {
			dual[i] = 0;
			dual_old[i] = 0;
		}

		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) { /* dual hessian for box constraints */
				d[i * 2 * (n + m) + j] = ig[i * n + j];
				d[(i + n) * 2 * (n + m) + j] = -ig[i * n + j];
				d[i * 2 * (n + m) + j + n] = -ig[i * n + j];
				d[(i + n) * 2 * (n + m) + j + n] = ig[i * n + j];
			}
			if (m > 0) {
				sum = 0; /* dual hessian for eq constraints */
				for (j = 0; j < n; j++) {
					sum += (ce[j] * ig[i * n + j]);
				}
				d[i * 2 * (n + m) + 2 * n] = sum;
				d[i * 2 * (n + m) + 2 * n + 1] = -sum;
				d[(n + i) * 2 * (n + m) + 2 * n] = -sum;
				d[(n + i) * 2 * (n + m) + 2 * n + 1] = sum;
				d[(n + n) * 2 * (n + m) + i] = sum;
				d[(n + n + 1) * 2 * (n + m) + i] = -sum;
				d[(n + n) * 2 * (n + m) + (n + i)] = -sum;
				d[(n + n + 1) * 2 * (n + m) + (n + i)] = sum;

				sum = 0;
				for (j = 0; j < n; j++) {
					for (k = 0; k < n; k++) {
						sum += (ce[k] * ce[j] * ig[j * n + k]);
					}
				}
				d[(n + n) * 2 * (n + m) + 2 * n] = sum;
				d[(n + n) * 2 * (n + m) + 2 * n + 1] = -sum;
				d[(n + n + 1) * 2 * (n + m) + 2 * n] = -sum;
				d[(n + n + 1) * 2 * (n + m) + 2 * n + 1] = sum;
			}
		}

		for (i = 0; i < n; i++) { /* dual linear component for the box constraints */
			w = 0;
			for (j = 0; j < n; j++) {
				w += (ig[i * n + j] * g0[j]);
			}
			d0[i] = up[i] + w;
			d0[i + n] = -low[i] - w;
		}

		if (m > 0) {
			sum = 0; /* dual linear component for eq constraints */
			for (j = 0; j < n; j++) {
				for (k = 0; k < n; k++) {
					sum += (ce[k] * ig[k * n + j] * g0[j]);
				}
			}
			d0[2 * n] = ce0[0] + sum;
			d0[2 * n + 1] = -ce0[0] - sum;
		}

		maxviol = 999999;
		iter = 0;
		retrain = 1;
		maxfaktor = 1;
		scalemaxiter = maxiter / 5;
		
		/******** main loop ***********/
		while ((retrain > 0) && (maxviol > 0)
				&& (iter < (scalemaxiter * maxfaktor))) {
			iter++;
			while ((maxviol > precision) && (iter < (scalemaxiter * maxfaktor))) {
				iter++;

				maxviol = 0;
				for (i = 0; i < 2 * (n + m); i++) {
					sum = d0[i];
					for (j = 0; j < 2 * (n + m); j++) {
						sum += d[i * 2 * (n + m) + j] * dual_old[j];
					}
					sum -= d[i * 2 * (n + m) + i] * dual_old[i];
					dual[i] = -sum / d[i * 2 * (n + m) + i];
					if (dual[i] < 0)
						dual[i] = 0;

					viol = Math.abs(dual[i] - dual_old[i]);
					if (viol > maxviol)
						maxviol = viol;
					dual_old[i] = dual[i];
				}
			}

			if (m > 0) {
				for (i = 0; i < n; i++) {
					temp[i] = dual[i] - dual[i + n] + ce[i]
							* (dual[n + n] - dual[n + n + 1]) + g0[i];
				}
			} else {
				for (i = 0; i < n; i++) {
					temp[i] = dual[i] - dual[i + n] + g0[i];// dual[i]一个关于上界,一个关于下界
				}
			}
			for (i = 0; i < n; i++) {
				primal[i] = 0; /* calc value of primal variables */
				for (j = 0; j < n; j++) {
					primal[i] += ig[i * n + j] * temp[j];
				}
				primal[i] *= -1.0;
				if (primal[i] <= (low[i])) { /* clip conservatively */
					primal[i] = low[i];
				} else if (primal[i] >= (up[i])) {
					primal[i] = up[i];
				}
			}

			if (m > 0)
				model_b = dual[n + n + 1] - dual[n + n];
			else
				model_b = 0;

			epsilon_hideo = EPSILON_HIDEO;
			for (i = 0; i < n; i++) { /* check precision of alphas */
				dist = -model_b * ce[i];
				dist += (g0[i] + 1.0);
				for (j = 0; j < i; j++) {
					dist += (primal[j] * g[j * n + i]);
				}
				for (j = i; j < n; j++) {
					dist += (primal[j] * g[i * n + j]);
				}
				if ((primal[i] < (up[i] - epsilon_hideo))
						&& (dist < (1.0 - epsilon_crit))) {
					epsilon_hideo = (up[i] - primal[i]) * 2.0;
				} else if ((primal[i] > (low[i] + epsilon_hideo))
						&& (dist > (1.0 + epsilon_crit))) {
					epsilon_hideo = (primal[i] - low[i]) * 2.0;
				}
			}

			for (i = 0; i < n; i++) { /* clip alphas to bounds */
				if (primal[i] <= (low[i] + epsilon_hideo)) {
					primal[i] = low[i];
				} else if (primal[i] >= (up[i] - epsilon_hideo)) {
					primal[i] = up[i];
				}
			}

			retrain = 0;
			primal_optimal = 1;
			//at_bound = 0;
			for (i = 0; (i < n); i++) { /* check primal KT-Conditions */
				dist = -model_b * ce[i];
				dist += (g0[i] + 1.0);
				for (j = 0; j < i; j++) {
					dist += (primal[j] * g[j * n + i]);
				}
				for (j = i; j < n; j++) {
					dist += (primal[j] * g[i * n + j]);
				}

				if ((primal[i] < (up[i] - epsilon_a))
						&& (dist < (1.0 - epsilon_crit))) {
					retrain = 1;
					primal_optimal = 0;
				} else if ((primal[i] > (low[i] + epsilon_a))
						&& (dist > (1.0 + epsilon_crit))) {
					retrain = 1;
					primal_optimal = 0;
				}
				/*
				if ((primal[i] <= (low[i] + epsilon_a))
						|| (primal[i] >= (up[i] - epsilon_a))) {
					at_bound++;
				}
				*/
			}
			if (m > 0) {
				eq = -ce0[0]; /* check precision of eq-constraint */
				for (i = 0; i < n; i++) {
					eq += (ce[i] * primal[i]);
				}
				if ((EPSILON_EQ < Math.abs(eq))) {
					retrain = 1;
					primal_optimal = 0;
				}
			}

			if (retrain > 0) {
				precision /= 10;
				if (((goal == PRIMAL_OPTIMAL) && (maxfaktor < 50000))|| (maxfaktor < 5)) {
					maxfaktor++;
				}
			}
		}

		if (primal_optimal == 0) {
			for (i = 0; i < n; i++) {
				primal[i] = 0; /* calc value of primal variables */
				for (j = 0; j < n; j++) {
					primal[i] += ig[i * n + j] * temp[j];
				}
				primal[i] *= -1.0;
				if (primal[i] <= (low[i] + epsilon_a)) { /* clip conservatively */
					primal[i] = low[i];
				} else if (primal[i] >= (up[i] - epsilon_a)) {
					primal[i] = up[i];
				}
			}
		}

		isnantest = 0;
		for (i = 0; i < n; i++) { /* check for isnan */
			isnantest += primal[i];
		}

		if (m > 0) {
			temp1 = dual[n + n + 1]; /* copy the dual variables for the eq */
			temp2 = dual[n + n]; /* constraints to a handier location */
			for (i = n + n + 1; i >= 2; i--) {
				dual[i] = dual[i - 2];
			}
			dual[0] = temp2;
			dual[1] = temp1;
			isnantest += temp1 + temp2;
		}


		if (Double.isNaN(isnantest)) {
			return ((int) NAN_SOLUTION);
		} else if (primal_optimal > 0) {
			return ((int) PRIMAL_OPTIMAL);
		} else if (maxviol == 0.0) {
			return ((int) DUAL_OPTIMAL);
		} else {
			return ((int) MAXITER_EXCEEDED);
		}

	}

	public void lcopy_matrix(double[] matrix, int depth, double[] matrix2) {
		int i;
		for (i = 0; i < (depth * depth); i++) {
			matrix2[i] = matrix[i];
		}
	}

	public void lswitchrk_matrix(double[] matrix, int depth, int rk1, int rk2) {
		int i;
		double temp;
		for (i = 0; i < depth; i++) {
			temp = matrix[rk1 * depth + i];
			matrix[rk1 * depth + i] = matrix[rk2 * depth + i];
			matrix[rk2 * depth + i] = temp;
		}

		for (i = 0; i < depth; i++) {
			temp = matrix[i * depth + rk1];
			matrix[i * depth + rk1] = matrix[i * depth + rk2];
			matrix[i * depth + rk2] = temp;
		}
	}

	public void linvert_matrix(double[] matrix, int depth, double[] inverse,
			double lindep_sensitivity, int[] lin_dependent) {
		int i, j, k;
		double factor;
		for (i = 0; i < depth; i++) {
			/* lin_dependent[i]=0; */
			for (j = 0; j < depth; j++) {
				inverse[i * depth + j] = 0.0;
			}
			inverse[i * depth + i] = 1.0;
		}
		for (i = 0; i < depth; i++) {
			if ((lin_dependent[i] > 0)
					|| (Math.abs(matrix[i * depth + i]) < lindep_sensitivity)) {
				lin_dependent[i] = 1;
			} else {
				for (j = i + 1; j < depth; j++) {
					factor = matrix[j * depth + i] / matrix[i * depth + i];
					for (k = i; k < depth; k++) {
						matrix[j * depth + k] -= (factor * matrix[i * depth + k]);
					}
					for (k = 0; k < depth; k++) {
						inverse[j * depth + k] -= (factor * inverse[i * depth
								+ k]);
					}
				}
			}
		}
		for (i = depth - 1; i >= 0; i--) {
			if (lin_dependent[i] == 0) {
				factor = 1 / matrix[i * depth + i];
				for (k = 0; k < depth; k++) {
					inverse[i * depth + k] *= factor;
				}
				matrix[i * depth + i] = 1;
				for (j = i - 1; j >= 0; j--) {
					factor = matrix[j * depth + i];
					matrix[j * depth + i] = 0;
					for (k = 0; k < depth; k++) {
						inverse[j * depth + k] -= (factor * inverse[i * depth
								+ k]);
					}
				}
			}
		}

		
	}

	public double calculate_qp_objective(int opt_n, double[] opt_g,
			double[] opt_g0, double[] alpha) {
		double obj;
		int i, j;
		obj = 0; /* calculate objective */

		for (i = 0; i < opt_n; i++) {
			obj+=(opt_g0[i]*alpha[i]);
			obj+=(0.5*alpha[i]*alpha[i]*opt_g[i*opt_n+i]);
			for (j = 0; j < i; j++) {
				obj+=(alpha[j]*alpha[i]*opt_g[j*opt_n+i]);
			}
		}
		return (obj);

	}

	public  void freeMemory()
	{
		dual=null;
		buffer = null;
		nonoptimal = null;
		System.gc();
	}
	
	public static void main(String[] args)
	{
		
		
	}
	
	
}
