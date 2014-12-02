package cn.clickwise.medlda;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Vector;

import org.apache.log4j.Logger;

import cn.clickwise.classify.svm_struct.KERNEL_PARM;
import cn.clickwise.classify.svm_struct.LEARN_PARM;
import cn.clickwise.classify.svm_struct.SAMPLE;
import cn.clickwise.classify.svm_struct.STRUCTMODEL;
import cn.clickwise.classify.svm_struct.STRUCT_LEARN_PARM;
import cn.clickwise.classify.svm_struct.svm_common;
import cn.clickwise.classify.svm_struct.svm_struct_api;
import cn.clickwise.classify.svm_struct.svm_struct_common;
import cn.clickwise.classify.svm_struct.svm_struct_learn;
import cn.clickwise.str.basic.SSO;
import cn.clickwise.time.utils.TimeOpera;

public class MedLDA {

	private double[] m_alpha;

	private double[] m_dMu;

	private double[] m_dEta;

	private double m_dC;

	private double m_dB;

	private int m_nDim;

	private double m_dsvm_primalobj;

	private int m_nK;

	private int m_nLabelNum;

	private int m_nNumTerms;

	private double[][] m_dLogProbW;

	private double m_dDeltaEll;

	private static final int NUM_INIT = 1;

	private static Logger logger = Logger.getLogger(MedLDA.class);

	/**
	 * perform inference on a Document and update sufficient statistics
	 * 
	 * @param doc
	 * @param gamma
	 * @param phi
	 * @param ss
	 * @param param
	 * @return
	 */
	public double doc_e_step(Document doc, double[] gamma, double[][] phi,
			SuffStats ss, Params param) {

		// posterior inference
		double lhood = inference(doc, ss.getNum_docs(), gamma, phi, param);

		// update sufficient statistics
		double gamma_sum = 0;
		for (int k = 0; k < m_nK; k++) {
			gamma_sum += gamma[k];
			ss.alpha_suffstats[k] += Utils.digamma(gamma[k]);
		}

		for (int k = 0; k < m_nK; k++) {
			ss.alpha_suffstats[k] -= Utils.digamma(gamma_sum);
		}

		logger.info("phi is:");
		Utils.printinqmatrix(phi, doc.length, m_nK);
		
		for (int k = 0; k < m_nK; k++) {
			double dVal = 0;
			for (int n = 0; n < doc.getLength(); n++) {
				ss.class_word[k][doc.words[n]] += doc.counts[n] * phi[n][k];
				ss.class_total[k] += doc.counts[n] * phi[n][k];
				dVal += phi[n][k] * (double) doc.counts[n]/ (double) doc.getTotal();
			}

			// suff-stats for supervised LDA
			ss.exp[ss.num_docs][k] = dVal;

		}

		ss.num_docs = ss.num_docs + 1;

		return lhood;
	}

	/**
	 * compute MLE lda model from sufficient statistics
	 * 
	 * @param ss
	 * @param param
	 * @param bInit
	 * @return
	 */
	public boolean mle(SuffStats ss, Params param, boolean bInit) {
		int k;
		int w;

		// beta parameters(K*N)
		for (k = 0; k < m_nK; k++) {
			for (w = 0; w < m_nNumTerms; w++) {
				if (ss.class_word[k][w] > 0) {
					m_dLogProbW[k][w] = Math.log(ss.class_word[k][w])
							- Math.log(ss.class_total[k]);
				} else {
					m_dLogProbW[k][w] = -100;
				}
			}
		}

		// alpha parameters
		logger.info("param.ESTIMATE_ALPHA:"+param.ESTIMATE_ALPHA);
		if ((!bInit) && param.ESTIMATE_ALPHA == 1) {// the same prior for all topics
													
			logger.info("param.ESTIMATE_ALPHA is 1");  
			double alpha_suffstats = 0;
			for (k = 0; k < m_nK; k++) {
				alpha_suffstats += (ss.alpha_suffstats[k]);
			}
			logger.info("alpha_suffstats e:");
			Utils.printvector(ss.alpha_suffstats, m_nK);
            logger.info("alpha_suffstats:"+alpha_suffstats);
            
			double alpha = OptAlpha.opt_alpha(alpha_suffstats, ss.num_docs,
					m_nK);
			for (k = 0; k < m_nK; k++) {
				m_alpha[k] = alpha;
			}
			logger.info("new alpha: "+alpha);

		} else if ((!bInit) && param.ESTIMATE_ALPHA == 2)// different priors for														// different topics
		{
			logger.info("param.ESTIMATE_ALPHA is 2"); 
			double alpha_sum = 0;
			for (k = 0; k < m_nK; k++) {
				alpha_sum += m_alpha[k];
			}

			for (k = 0; k < m_nK; k++) {
				alpha_sum -= m_alpha[k];

				m_alpha[k] = OptAlpha.opt_alpha(ss.alpha_suffstats[k],
						alpha_sum, ss.num_docs, m_nK);
			}

			logger.info("new alpha: ");
			for (k = 0; k < m_nK; k++) {
				logger.info(m_alpha[k]);
			}
		}

		boolean bRes = true;
		if (!bInit) {
			svmStructSolver(ss, param, m_dMu);
		}

		return bRes;
	}

	public int run_em(String start, String directory, Corpus corpus,
			Params param) {

		m_dDeltaEll = param.getDELTA_ELL();

		int d, n;

		long runtime_start = TimeOpera.getCurrentTimeLong();
		double[][] var_gamma = new double[corpus.num_docs][param.getNTOPICS()];

		int max_length = corpus.max_corpus_length();

		double[][] phi = new double[max_length][param.getNTOPICS()];

		// initialize model
		SuffStats ss = null;
		if (start.equals("seeded")) {
			new_model(corpus.num_docs, corpus.num_terms, param.getNTOPICS(),
					param.getNLABELS(), param.getINITIAL_C());
			ss = new_suffstats();
			corpus_init_ss(ss, corpus);
			mle(ss, param, true);
			for (int k = 0; k < m_nK; k++) {
				m_alpha[k] = param.getINITIAL_ALPHA()/(double) param.NTOPICS;
			}

		} else if (start.equals("random")) {
			new_model(corpus.num_docs, corpus.num_terms, param.getNTOPICS(),
					param.getNLABELS(), param.getINITIAL_C());
			ss = new_suffstats();
			random_init_ss(ss, corpus);
			mle(ss, param, true);
			for (int k = 0; k < m_nK; k++) {
				m_alpha[k] = param.getINITIAL_ALPHA()/(double) param.NTOPICS;
			}

		} else {
			load_model(start);
			m_dC = param.getINITIAL_C();
			ss = new_suffstats();

			ss.setY(new int[corpus.num_docs]);
			ss.setExp(new double[corpus.num_docs][param.getNTOPICS()]);

		}

		ss.setDir(directory);

		String filename;
		filename = directory + "/000";
		save_model(filename);

		filename = directory + "/lhood.dat";
		int nIt = 0;
		try {

			PrintWriter likelihood_file = new PrintWriter(new FileWriter(
					filename));

			int i = 0;
			double lhood = 0, lhood_old = 0, converged = 1;
            int ci=0;
			while (((converged < 0) || (converged > param.getEM_CONVERGED() || (i <= 2)))
					&& (i <= param.getEM_MAX_ITER())) {
				logger.info("**** em iteration " + (i + 1) + " ****");
				lhood = 0;
				zero_init_ss(ss);

				if(ci>0)
				{
					break;
				}
				ci++;
				// e-step
				for (d = 0; d < corpus.num_docs; d++) {
					for (n = 0; n < max_length; n++)// initialize to uniform
					{
						for (int k = 0; k < param.getNTOPICS(); k++) {
							phi[n][k] = 1.0 / (double) param.getNTOPICS();
						}
					}
						if (d % 1000 == 0) {
							logger.info("Document " + d);
						}
					lhood += doc_e_step(corpus.docs[d], var_gamma[d],
									phi, ss, param);
						
					
				}

				// m-step
				if (mle(ss, param, false)) {
					nIt = i + 1;
				} else {
					break;
				}

				// check for convergence
				lhood += m_dsvm_primalobj;

				converged = (lhood_old - lhood) / (lhood_old);
				lhood_old = lhood;

				// output model and lhood
				likelihood_file.printf("%10.10f\t%5.5e\n", lhood, converged);
				likelihood_file.flush();

				i++;

			}

			long runtime_end = TimeOpera.getCurrentTimeLong();
			logger.info("Training time in (cpu-seconds):"
					+ ((double) runtime_end - (double) runtime_start) / 1000.0);

			filename = "MedLDA_(" + m_nK + "topic)_train.txt";

			// output the final model
			filename = directory + "/final";
			save_model(filename);

			filename = directory + "/final.gamma";
			save_gamma(filename, var_gamma, corpus.num_docs, m_nK);

			// output the word assignments(for visualization)
			int nNum = 0, nAcc = 0;
			filename = directory + "/word-assignments.dat";

			PrintWriter w_asgn_file = new PrintWriter(new FileWriter(filename));
			for (d = 0; d < corpus.num_docs; d++) {
				if ((d % 1000) == 0) {
					logger.info("final e step Document " + d);
				}
				lhood += inference(corpus.docs[d], d, var_gamma[d], phi, param);
				write_word_assignment(w_asgn_file, corpus.docs[d], phi);

				nNum++;
				predict(corpus.docs[d], phi);

				if (corpus.docs[d].getGndlabel() == corpus.docs[d]
						.getPredlabel()) {
					nAcc++;
				}
			}

			w_asgn_file.close();
			likelihood_file.close();
			logger.info("MedLDA: double count accuracy:" + (double) nAcc / nNum);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return nIt;
	}

	public double infer(String model_dir, Corpus corpus, Params param,
			String userId) {

		String filename;
		int i, d, n;
		double[][] var_gamma;
		double lhood;
		double[][] phi;
		Document doc;

		String model_root;
		model_root = model_dir + "/final";
		load_model(model_root);

		// remove unseen words
		if (corpus.num_terms > m_nNumTerms) {

			for (i = 0; i < corpus.num_docs; i++) {
				for (int k = 0; k < corpus.docs[i].length; k++) {
					if (corpus.docs[i].words[k] >= m_nNumTerms) {
						corpus.docs[i].words[k] = m_nNumTerms - 1;
					}
				}
			}
		}

		var_gamma = new double[corpus.num_docs][m_nK];
		double[][] exp = new double[corpus.num_docs][m_nK];

		filename = model_dir + "/evl-lda-lhood.dat";
		double dAcc = 0;

		try {
			PrintWriter fileptr = new PrintWriter(new FileWriter(filename));

			for (d = 0; d < corpus.num_docs; d++) {
				if (((d % 1000) == 0) && (d > 0)) {
					logger.info("Document " + d);
				}

				doc = corpus.docs[d];
				phi = new double[doc.length][m_nK];

				for (n = 0; n < doc.length; n++) {
					// initialize to uniform distribution
					for (int k = 0; k < m_nK; k++) {
						phi[n][k] = 1.0;
					}
				}

				lhood = inference_pred(doc, var_gamma[d], phi, param);

				// do prediction
				predict(doc, phi);
				doc.lhood = lhood;

				fileptr.println(lhood);

				// update the exp
				for (int k = 0; k < m_nK; k++) {
					double dVal = 0;
					for (n = 0; n < doc.length; n++) {
						dVal += phi[n][k] * (double) doc.counts[n]
								/ (double) doc.total;
					}

					exp[d][k] = dVal;
				}
			}

			filename = "MedLDA_(" + m_nK + "topic)_test.txt";
			outputData2(filename, corpus, exp, m_nK, m_nLabelNum);

			filename = model_dir + "/evl-gamma.dat";
			save_gamma(filename, var_gamma, corpus.num_docs, m_nK);

			filename = model_dir + "/evl-performance.dat";
			dAcc = save_prediction(filename, corpus);
			fileptr.close();

			fileptr = new PrintWriter(new FileWriter("overall-res.txt", true));
			fileptr.printf(
					"setup (K: %d; C: %.3f; fold: %d; ell: %.2f; dual-opt: %d; alpha: %d; svm_alg: %d; maxIt: %d): accuracy %.3f\n",
					m_nK, m_dC, param.NFOLDS, param.DELTA_ELL,
					param.PHI_DUALOPT, param.ESTIMATE_ALPHA, param.SVM_ALGTYPE,
					param.EM_MAX_ITER, dAcc);
			fileptr.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dAcc;
	}

	public double inference(Document doc, int docix, double[] var_gamma,
			double[][] phi, Params param) {

		double converged = 1;
		double phisum = 0, lhood = 0;
		double lhood_old = 0;
		double[] oldphi = new double[m_nK];
		double[] digamma_gam = new double[m_nK];

		// compute poster for dirichlet
		for (int k = 0; k < m_nK; k++) {
			var_gamma[k] = m_alpha[k] + (double) doc.total / (double) m_nK;
			digamma_gam[k] = Utils.digamma(var_gamma[k]);
			for (int n = 0; n < doc.length; n++) {
				phi[n][k] = 1.0 / (double) m_nK;
			}
		}

		/*
		logger.info("m_alpha is:");
		Utils.printvector(m_alpha, m_nK);	
		logger.info("var_gamma is:");
		Utils.printvector(var_gamma, m_nK);
		logger.info("digamma_gam is:");
		Utils.printvector(digamma_gam, m_nK);
		logger.info("phi is:");
		Utils.printinqmatrix(phi, doc.length, m_nK);
		*/
		
		int var_iter = 0;
		while ((converged > param.VAR_CONVERGED)
				&& ((var_iter < param.VAR_MAX_ITER) || (param.VAR_MAX_ITER == -1))) {
			var_iter++;
			for (int n = 0; n < doc.length; n++) {
				phisum = 0;

				for (int k = 0; k < m_nK; k++) {
					oldphi[k] = phi[n][k];

					/*
					 * update the phi: add additional terms here for supervised
					 * LDA
					 */
					double dVal = compute_mrgterm(doc, docix, n, k, param);
					phi[n][k] = digamma_gam[k] + m_dLogProbW[k][doc.words[n]]
							+ dVal;

					if (k > 0)
						phisum = Utils.log_sum(phisum, phi[n][k]);
					else
						phisum = phi[n][k];
				}

				// update gamma and normalize phi
				for (int k = 0; k < m_nK; k++) {
					phi[n][k] = Math.exp(phi[n][k] - phisum);
					var_gamma[k] = var_gamma[k] + doc.counts[n]
							* (phi[n][k] - oldphi[k]);

					digamma_gam[k] = Utils.digamma(var_gamma[k]);
				}
			}
			
			lhood = compute_lhood(doc, phi, var_gamma);
			//logger.info("var_iter:"+var_iter+" lhood:"+lhood);
			converged = (lhood_old - lhood) / lhood_old;
			lhood_old = lhood;
		}

		return lhood;
	}

	/**
	 * Given the model and w, compute the E[Z] for prediction
	 * 
	 * @param doc
	 * @param var_gamma
	 * @param phi
	 * @param param
	 * @return
	 */

	public double inference_pred(Document doc, double[] var_gamma,
			double[][] phi, Params param) {

		double converged = 1;
		double phisum = 0, lhood = 0;
		double lhood_old = 0;
		double[] oldphi = new double[m_nK];
		int k, n, var_iter;
		double[] digamma_gam = new double[m_nK];

		// compute posterior dirichlet
		for (k = 0; k < m_nK; k++) {
			var_gamma[k] = m_alpha[k] + (double) doc.total / (double) m_nK;
			digamma_gam[k] = Utils.digamma(var_gamma[k]);
			for (n = 0; n < doc.length; n++) {
				phi[n][k] = 1.0 / m_nK;
			}

		}

		var_iter = 0;

		while ((converged > param.VAR_CONVERGED)
				&& ((var_iter < param.VAR_MAX_ITER) || (param.VAR_MAX_ITER == -1))) {
			var_iter++;
			for (n = 0; n < doc.length; n++) {
				phisum = 0;
				for (k = 0; k < m_nK; k++) {
					oldphi[k] = phi[n][k];

					phi[n][k] = digamma_gam[k] + m_dLogProbW[k][doc.words[n]];

					if (k > 0)
						phisum = Utils.log_sum(phisum, phi[n][k]);
					else
						phisum = phi[n][k];
				}

				// update gamma and normalize phi
				for (k = 0; k < m_nK; k++) {
					phi[n][k] = Math.exp(phi[n][k] - phisum);
					var_gamma[k] = var_gamma[k] + doc.counts[n]
							* (phi[n][k] - oldphi[k]);
					digamma_gam[k] = Utils.digamma(var_gamma[k]);
				}

			}

			lhood = compute_lhood(doc, phi, var_gamma);
			converged = (lhood_old - lhood) / lhood_old;
			lhood_old = lhood;

		}

		return lhood;
	}

	public double compute_mrgterm(Document doc, int d, int n, int k,
			Params param) {
		double dval = 0;
		int gndetaIx = doc.gndlabel * m_nK + k;
		param.PHI_DUALOPT = 1;

		if (param.PHI_DUALOPT == 1) {
			for (int m = 0; m < m_nLabelNum; m++) {
				int muIx = d * m_nLabelNum + m;
				int etaIx = m * m_nK + k;

				dval += m_dMu[muIx] * (m_dEta[gndetaIx] - m_dEta[etaIx]);
			}
		} else {
			int etaIx = doc.lossAugLabel * m_nK + k;
			dval = m_dC * (m_dEta[gndetaIx] - m_dEta[etaIx]);
		}

		dval = dval * doc.counts[n] / doc.total;

		return dval;
	}

	public double compute_lhood(Document doc, double[][] phi, double[] var_gamma) {

		double lhood = 0, digsum = 0, var_gamma_sum = 0, alpha_sum = 0;
		double[] dig = new double[m_nK];

		for (int k = 0; k < m_nK; k++) {
			dig[k] = Utils.digamma(var_gamma[k]);
			var_gamma_sum += var_gamma[k];
			alpha_sum += m_alpha[k];
		}

		digsum = Utils.digamma(var_gamma_sum);

		lhood = Utils.lgamma(alpha_sum) - (Utils.lgamma(var_gamma_sum));
		for (int k = 0; k < m_nK; k++) {
			lhood -= Utils.lgamma(m_alpha[k]);
		}

		for (int k = 0; k < m_nK; k++) {
			lhood += (m_alpha[k] - 1) * (dig[k] - digsum)
					+ Utils.lgamma(var_gamma[k]) - (var_gamma[k] - 1)
					* (dig[k] - digsum);

			double dVal = 0;
			for (int n = 0; n < doc.length; n++) {
				if (phi[n][k] > 0) {
					lhood += doc.counts[n]
							* (phi[n][k] * ((dig[k] - digsum)
									- Math.log(phi[n][k]) + m_dLogProbW[k][doc.words[n]]));
				}
				dVal += phi[n][k] * doc.counts[n] / doc.total;
			}
		}

		return lhood;
	}

	public void predict(Document doc, double[][] phi) {
		doc.predlabel = -1;
		double dMaxScore = 0;
		for (int y = 0; y < m_nLabelNum; y++) {
			double dScore = 0;
			for (int k = 0; k < m_nK; k++) {
				int etaIx = y * m_nK + k;

				double dVal = 0;
				for (int n = 0; n < doc.length; n++) {
					dVal += phi[n][k] * (double) doc.counts[n]
							/ (double) doc.total;
				}

				dScore += dVal * m_dEta[etaIx];
			}
			dScore -= m_dB;
			if (doc.predlabel == -1 || dScore > dMaxScore) {
				doc.predlabel = y;
				dMaxScore = dScore;
			}

		}

	}

	public double save_prediction(String filename, Corpus corpus) {

		double dmean = 0;
		double sumlikelihood = 0;
		int nterms = 0;
		double sumavglikelihood = 0;
		for (int d = 0; d < corpus.num_docs; d++) {
			sumlikelihood += corpus.docs[d].lhood;
			nterms += corpus.docs[d].total;
			sumavglikelihood += corpus.docs[d].lhood
					/ ((double) corpus.docs[d].total);
		}
		double perwordlikelihood1 = sumlikelihood / nterms;
		double perwordlikelihood2 = sumavglikelihood / corpus.num_docs;

		int nAcc = 0;
		for (int d = 0; d < corpus.num_docs; d++)
			if (corpus.docs[d].gndlabel == corpus.docs[d].predlabel)
				nAcc += 1;
		double dAcc = (double) nAcc / corpus.num_docs;

		logger.info("Accuracy:" + dAcc);
		PrintWriter fileptr = null;

		try {
			fileptr = new PrintWriter(new FileWriter(filename));
			fileptr.printf("accuracy: %5.10f\n", dAcc);
			fileptr.printf("perword likelihood1: %5.10f\n", perwordlikelihood1);
			fileptr.printf("perword likelihood2: %5.10f\n", perwordlikelihood2);

			for (int d = 0; d < corpus.num_docs; d++)
				fileptr.printf("%d\t%d\n", corpus.docs[d].predlabel,
						corpus.docs[d].gndlabel);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dAcc;
	}

	/*
	 * public void loss_aug_predict(Document doc, double[] zbar_mean) {
	 * 
	 * }
	 */

	public double loss(int y, int gnd) {
		if (y == gnd)
			return 0;
		else
			return m_dDeltaEll;
	}

	public void new_model(int num_docs, int num_terms, int num_topics,
			int num_labels, double C) {
		int i, j;
		m_nK = num_topics;
		m_nLabelNum = num_labels;
		m_nNumTerms = num_terms;

		m_alpha = new double[m_nK];
		for (int k = 0; k < m_nK; k++)
			m_alpha[k] = 1.0 / num_topics;
		//logger.info("new model: num_topics:"+num_topics);
		//Utils.printvector(m_alpha, m_nK);
		m_dLogProbW = new double[num_topics][num_terms];
		m_dEta = new double[num_topics * num_labels];// Eta使用向量存储二维矩阵，行是主题，列是标记，元素
		// [i*num_labels + j]指主题i和标记j的转换概率
		m_dMu = new double[num_docs * num_labels];// Mu使用向量存储二维矩阵，行是文档，列是标记，元素[i*num_labels
													// + j]
		// 指文档i和标记j之间的关系值
		for (i = 0; i < num_topics; i++) {
			for (j = 0; j < num_terms; j++)
				m_dLogProbW[i][j] = 0;
			for (j = 0; j < num_labels; j++)
				m_dEta[i * num_labels + j] = 0;
		}
		for (i = 0; i < num_docs; i++)
			for (j = 0; j < num_labels; j++)
				m_dMu[i * num_labels + j] = 0;

		m_nDim = num_docs;
		m_dC = C;
	}

	public SuffStats new_suffstats() {
		int num_topics = m_nK;
		int num_terms = m_nNumTerms;

		SuffStats ss = new SuffStats();
		ss.class_total = new double[num_topics];
		ss.class_word = new double[num_topics][num_terms];

		for (int i = 0; i < num_topics; i++) {
			ss.class_total[i] = 0;
			for (int j = 0; j < num_terms; j++) {
				ss.class_word[i][j] = 0;
			}
		}

		ss.alpha_suffstats = new double[m_nK];

		for (int i = 0; i < ss.alpha_suffstats.length; i++) {
			ss.alpha_suffstats[i] = 0;
		}

		return ss;
	}

	public void corpus_init_ss(SuffStats ss, Corpus c) {

		logger.info("in corpus_init_ss");
		int num_topics = m_nK;
		int i, k, d, n;
		Document doc;

		for (k = 0; k < num_topics; k++) {
			for (i = 0; i < NUM_INIT; i++) {
				d = (int) Math.floor(Math.random() * (c.num_docs));
				doc = c.docs[d];
				for (n = 0; n < doc.length; n++) {
					ss.class_word[k][doc.words[n]] += doc.counts[n];
				}
			}

			for (n = 0; n < m_nNumTerms; n++) {
				ss.class_word[k][n] += 1.0;// 避免单词个数为0的情况
				ss.class_total[k] = ss.class_total[k] + ss.class_word[k][n];
			}

		}

		// for sLDA only
		ss.y = new int[c.num_docs];
		ss.exp = new double[c.num_docs][num_topics];

		for (d = 0; d < c.num_docs; d++) {
			ss.y[d] = c.docs[d].gndlabel;
		}

	}

	public void random_init_ss(SuffStats ss, Corpus c) {
		logger.info("in random_init_ss");
		int num_topics = m_nK;
		int num_terms = m_nNumTerms;
		for (int k = 0; k < num_topics; k++) {
			for (int n = 0; n < num_terms; n++) {
				ss.class_word[k][n] += (10.0 + Math.random());
				ss.class_total[k] += ss.class_word[k][n];
			}
		}

		ss.y = new int[c.num_docs];
		ss.exp = new double[c.num_docs][m_nK];

		for (int k = 0; k < c.num_docs; k++) {
			ss.y[k] = c.docs[k].gndlabel;
		}

	}

	public void zero_init_ss(SuffStats ss) {
		logger.info("in zero_init_ss");
		for (int k = 0; k < m_nK; k++) {
			ss.class_total[k] = 0;
			for (int w = 0; w < m_nNumTerms; w++) {
				ss.class_word[k][w] = 0;
			}
		}
		ss.num_docs = 0;

		for (int i = 0; i < ss.alpha_suffstats.length; i++) {
			ss.alpha_suffstats[i] = 0;
		}

	}

	public void load_model(String model_root) {

		String filename = "";
		BufferedReader fileptr = null;
		Scanner sc = null;
		int i, j, num_terms, num_topics, num_labels, num_docs;
		double x, alpha, C, learnRate;
		Vector<Double> vecAlpha = new Vector<Double>();

		filename = model_root + ".other";
		logger.info("loading " + filename);

		String line = "";
		try {
			fileptr = new BufferedReader(new FileReader(filename));
			line = fileptr.readLine();
			num_topics = Integer.parseInt(SSO.afterStr(line, "num_topics")
					.trim());

			line = fileptr.readLine();
			num_labels = Integer.parseInt(SSO.afterStr(line, "num_labels")
					.trim());

			line = fileptr.readLine();
			num_terms = Integer
					.parseInt(SSO.afterStr(line, "num_terms").trim());

			line = fileptr.readLine();
			num_docs = Integer.parseInt(SSO.afterStr(line, "num_docs").trim());

			line = fileptr.readLine();
			line = SSO.afterStr(line, "alpha ");
			sc = new Scanner(line);

			for (int k = 0; k < num_topics; k++) {
				alpha = sc.nextDouble();
				m_alpha[k] = alpha;
			}

			fileptr.close();

			line = fileptr.readLine();
			C = Integer.parseInt(SSO.afterStr(line, "C ").trim());

			new_model(num_docs, num_terms, num_topics, num_labels, C);

			filename = model_root + ".beta";
			logger.info("loading " + filename);
			if (sc != null) {
				sc.close();
			}
			// fileptr=new BufferedReader(new FileReader(filename));
			sc = new Scanner(new FileInputStream(filename));
			m_dB = sc.nextDouble();

			for (i = 0; i < m_nK; i++) {
				for (int k = 0; k < m_nLabelNum; k++) {
					x = sc.nextDouble();
					m_dEta[i + k * m_nK] = x;
				}

				for (j = 0; j < m_nNumTerms; j++) {
					x = sc.nextDouble();
					m_dLogProbW[i][j] = x;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void set_init_param(STRUCT_LEARN_PARM struct_parm,
			LEARN_PARM learn_parm, KERNEL_PARM kernel_parm, int alg_type) {

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
		struct_parm.delta_ell = m_dDeltaEll;

		learn_parm.predfile = "trans_predictions";
		learn_parm.alphafile = "";
		svm_common.verbosity = 0;/* verbosity for svm_light */
		svm_struct_common.struct_verbosity = 1;
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
		if (learn_parm.svm_iter_to_shrink == -9999) {
			learn_parm.svm_iter_to_shrink = 100;
		}

		if ((learn_parm.skip_final_opt_check != 0)
				&& (kernel_parm.kernel_type == svm_common.LINEAR)) {
			logger.info("\nIt does not make sense to skip the final optimality check for linear kernels.\n\n");
			learn_parm.skip_final_opt_check = 0;
		}

		if ((learn_parm.skip_final_opt_check != 0)
				&& (learn_parm.remove_inconsistent != 0)) {
			logger.info("\nIt is necessary to do the final optimality check when removing inconsistent \nexamples.\n");

		}

		if ((learn_parm.svm_maxqpsize < 2)) {
			logger.info("\nMaximum size of QP-subproblems not in valid range: "
					+ learn_parm.svm_maxqpsize + " [2..]\n");

		}

		if ((learn_parm.svm_maxqpsize < learn_parm.svm_newvarsinqp)) {
			logger.info("\nMaximum size of QP-subproblems ["
					+ learn_parm.svm_maxqpsize
					+ "] must be larger than the number of\n");
			logger.info("new variables [" + learn_parm.svm_newvarsinqp
					+ "] entering the working set in each iteration.\n");
		}

		if (learn_parm.svm_iter_to_shrink < 1) {
			logger.info("\nMaximum number of iterations for shrinking not in valid range: "
					+ learn_parm.svm_iter_to_shrink + " [1,..]\n");
		}

		if (((alg_type) < 0) || (((alg_type) > 5) && ((alg_type) != 9))) {
			logger.info("\nAlgorithm type must be either '0', '1', '2', '3', '4', or '9'!\n\n");
		}

		if (learn_parm.transduction_posratio > 1) {
			logger.info("\nThe fraction of unlabeled examples to classify as positives must\n");
			logger.info("be less than 1.0 !!!\n\n");
		}
		if (learn_parm.svm_costratio <= 0) {
			logger.info("\nThe COSTRATIO parameter must be greater than zero!\n\n");
		}

		if (struct_parm.epsilon <= 0) {
			logger.info("\nThe epsilon parameter must be greater than zero!\n\n");
		}

		if ((struct_parm.ccache_size <= 0) && ((alg_type) == 4)) {
			logger.info("\nThe cache size must be at least 1!\n\n");
		}

		if (((struct_parm.batch_size <= 0) || (struct_parm.batch_size > 100))
				&& ((alg_type) == 4)) {
			logger.info("\nThe batch size must be in the interval ]0,100]!\n\n");
		}

		if ((struct_parm.slack_norm < 1) || (struct_parm.slack_norm > 2)) {
			logger.info("\nThe norm of the slacks must be either 1 (L1-norm) or 2 (L2-norm)!\n\n");
		}
		if ((struct_parm.loss_type != svm_struct_learn.SLACK_RESCALING)
				&& (struct_parm.loss_type != svm_struct_learn.MARGIN_RESCALING)) {
			logger.info("\nThe loss type must be either 1 (slack rescaling) or 2 (margin rescaling)!\n\n");

		}

		if (learn_parm.rho < 0) {
			logger.info("\nThe parameter rho for xi/alpha-estimates and leave-one-out pruning must\n");
			logger.info("be greater than zero (typically 1.0 or 2.0, see T. Joachims, Estimating the\n");
			logger.info("Generalization Performance of an SVM Efficiently, ICML, 2000.)!\n\n");
		}

		if ((learn_parm.xa_depth < 0) || (learn_parm.xa_depth > 100)) {
			logger.info("\nThe parameter depth for ext. xi/alpha-estimates must be in [0..100] (zero\n");
			logger.info("for switching to the conventional xa/estimates described in T. Joachims,\n");
			logger.info("Estimating the Generalization Performance of an SVM Efficiently, ICML, 2000.)\n");
		}

		svm_struct_api.parse_struct_parameters(struct_parm);
	}

	public void svmStructSolver(SuffStats ss, Params param, double[] res) {

		LEARN_PARM learn_parm = new LEARN_PARM();
		KERNEL_PARM kernel_parm = new KERNEL_PARM();
		STRUCT_LEARN_PARM struct_parm = new STRUCT_LEARN_PARM();
		STRUCTMODEL structmodel = new STRUCTMODEL();
		int alg_type = 0;

		/* set the parameters. */
		set_init_param(struct_parm, learn_parm, kernel_parm, alg_type);
		struct_parm.C = m_dC;

		// output the features
		String buff;
		buff = ss.dir + "/Feature.txt";
		outputLowDimData(buff, ss);

		/* read the training examples */
		SAMPLE sample = svm_struct_api.read_struct_examples(buff, struct_parm);

		svm_struct_learn sl = new svm_struct_learn();
		if (param.SVM_ALGTYPE == 0) {
			sl.svm_learn_struct(sample, struct_parm, learn_parm, kernel_parm,
					structmodel, alg_type);
		} else if (param.SVM_ALGTYPE == 2) {
			struct_parm.C = m_dC * ss.num_docs; // Note: in n-slack formulation,
												// C is not divided by N.
			sl.svm_learn_struct_joint(sample, struct_parm, learn_parm,
					kernel_parm, structmodel,
					svm_struct_learn.ONESLACK_PRIMAL_ALG);
		}

		int nVar = ss.num_docs * m_nLabelNum;

		if (param.SVM_ALGTYPE == 0) {
			for (int k = 1; k < structmodel.svm_model.sv_num; k++) {
				int n = structmodel.svm_model.supvec[k].docnum;
				int docnum = structmodel.svm_model.supvec[k].orgDocNum;
				m_dMu[docnum] = structmodel.svm_model.alpha[k];
			}
		} else if (param.SVM_ALGTYPE == 2) {
			for (int k = 1; k < structmodel.svm_model.sv_num; k++) {
				int[] vecLabel = structmodel.svm_model.supvec[k].lvec;

				double dval = structmodel.svm_model.alpha[k] / ss.num_docs;
				for (int d = 0; d < ss.num_docs; d++) {
					int label = vecLabel[d];
					m_dMu[d * m_nLabelNum + label] += dval;
				}
			}
		}

		PrintWriter fileptr = null;
		try {
			fileptr = new PrintWriter(new FileWriter("MuSolution.txt", true));
			for (int i = 0; i < ss.num_docs; i++) {
				for (int k = 0; k < m_nLabelNum; k++) {
					int muIx = i * m_nLabelNum + k;
					if (m_dMu[muIx] > 0) {
						fileptr.printf("%d:%.5f ", k, m_dMu[muIx]);
					}
					fileptr.println();
				}
			}
			fileptr.println();
			fileptr.close();

			m_dB = structmodel.svm_model.b;

			for (int y = 0; y < m_nLabelNum; y++) {
				for (int k = 0; k < m_nK; k++) {
					int etaIx = y * m_nK + k;
					m_dEta[etaIx] = structmodel.w[etaIx + 1];
				}
			}

			m_dsvm_primalobj = structmodel.primalobj;

		} catch (Exception e) {
			e.printStackTrace();
		}
		// 待续

	}

	public void save_model(String model_root) {

		String filename = "";
		PrintWriter fileptr = null;
		int i, j;

		filename = model_root + ".beta";

		try {
			fileptr = new PrintWriter(new FileWriter(filename));
			fileptr.printf("%5.10f\n", m_dB);

			for (i = 0; i < m_nK; i++) {
				// the first element is eta[k]
				for (int k = 0; k < m_nLabelNum; k++) {
					if (k == m_nLabelNum - 1)
						fileptr.printf("%5.10f", m_dEta[i + k * m_nK]);
					else
						fileptr.printf("%5.10f ", m_dEta[i + k * m_nK]);
				}

				for (j = 0; j < m_nNumTerms; j++) {
					fileptr.printf(" %5.10f", m_dLogProbW[i][j]);
				}
				fileptr.println();
			}
			fileptr.close();
			
			filename=model_root+".other";
			fileptr=new PrintWriter(new FileWriter(filename));
			
			fileptr.printf( "num_topics %d\n", m_nK);
			fileptr.printf( "num_labels %d\n", m_nLabelNum);
			fileptr.printf( "num_terms %d\n", m_nNumTerms);
			fileptr.printf( "num_docs %d\n", m_nDim);
			fileptr.printf( "alpha ");
			for ( int k=0; k<m_nK; k++ ) {
				fileptr.printf( "%5.10f ", m_alpha[k]);
			}
			fileptr.printf( "\n");
			fileptr.printf( "C %5.10f\n", m_dC);
			fileptr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void save_gamma(String filename, double[][] gamma, int num_docs,
			int num_topics) {

		PrintWriter fileptr=null;
		int d,k;
		try{
			fileptr=new PrintWriter(new FileWriter(filename));
			
			for (d = 0; d < num_docs; d++) {
				fileptr.printf( "%5.10f", gamma[d][0]);
				for (k = 1; k < num_topics; k++) {
					fileptr.printf(" %5.10f", gamma[d][k]);
				}
				fileptr.printf("\n");
			}
			
			fileptr.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

	public void write_word_assignment(PrintWriter fileptr, Document doc,
			double[][] phi) {
		
		fileptr.printf( "%03d", doc.length);
		for (int n = 0; n < doc.length; n++) {
			fileptr.printf( " %04d:%02d", doc.words[n], Utils.argmax(phi[n], m_nK));
		}
		fileptr.println();
		fileptr.flush();
		
	}

	public void outputData2(String filename, Corpus corpus, double[][] exp,
			int ntopic, int nLabels) {
		
		PrintWriter fileptr = null;
		try{
			fileptr=new PrintWriter(new FileWriter(filename));
		for ( int i=0; i<corpus.num_docs; i++ ) {
			int label = corpus.docs[i].gndlabel;
			if ( nLabels == 2 ) {
				if ( corpus.docs[i].gndlabel == -1 ) label = 1;
				if ( corpus.docs[i].gndlabel == 1 ) label = 0;
			}

			fileptr.printf( "%d %d", ntopic, label);
			for ( int k=0; k<ntopic; k++ ) 
				fileptr.printf( " %d:%.10f", k, exp[i][k]);
			fileptr.println();
		}
		fileptr.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void outputLowDimData(String filename, SuffStats ss) {
		try {
			PrintWriter fileptr = new PrintWriter(new FileWriter(filename));

			for (int d = 0; d < ss.num_docs; d++) {
				int label = ss.y[d];
				//fileptr.printf("%d %d", m_nK, label);
				fileptr.printf("%d",  label);
				for (int k = 0; k < m_nK; k++) {
					fileptr.printf(" %d:%.5f", k, ss.exp[d][k]*10000);
				}
				fileptr.println();

			}
			fileptr.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public double[] getM_alpha() {
		return m_alpha;
	}

	public void setM_alpha(double[] m_alpha) {
		this.m_alpha = m_alpha;
	}

	public double[] getM_dMu() {
		return m_dMu;
	}

	public void setM_dMu(double[] m_dMu) {
		this.m_dMu = m_dMu;
	}

	public double[] getM_dEta() {
		return m_dEta;
	}

	public void setM_dEta(double[] m_dEta) {
		this.m_dEta = m_dEta;
	}

	public double getM_dC() {
		return m_dC;
	}

	public void setM_dC(double m_dC) {
		this.m_dC = m_dC;
	}

	public double getM_dB() {
		return m_dB;
	}

	public void setM_dB(double m_dB) {
		this.m_dB = m_dB;
	}

	public int getM_nDim() {
		return m_nDim;
	}

	public void setM_nDim(int m_nDim) {
		this.m_nDim = m_nDim;
	}

	public double getM_dsvm_primalobj() {
		return m_dsvm_primalobj;
	}

	public void setM_dsvm_primalobj(double m_dsvm_primalobj) {
		this.m_dsvm_primalobj = m_dsvm_primalobj;
	}

	public int getM_nK() {
		return m_nK;
	}

	public void setM_nK(int m_nK) {
		this.m_nK = m_nK;
	}

	public int getM_nLabelNum() {
		return m_nLabelNum;
	}

	public void setM_nLabelNum(int m_nLabelNum) {
		this.m_nLabelNum = m_nLabelNum;
	}

	public int getM_nNumTerms() {
		return m_nNumTerms;
	}

	public void setM_nNumTerms(int m_nNumTerms) {
		this.m_nNumTerms = m_nNumTerms;
	}

	public double[][] getM_dLogProbW() {
		return m_dLogProbW;
	}

	public void setM_dLogProbW(double[][] m_dLogProbW) {
		this.m_dLogProbW = m_dLogProbW;
	}

	public double getM_dDeltaEll() {
		return m_dDeltaEll;
	}

	public void setM_dDeltaEll(double m_dDeltaEll) {
		this.m_dDeltaEll = m_dDeltaEll;
	}

}
