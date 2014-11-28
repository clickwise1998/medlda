package cn.clickwise.medlda;

import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import cn.clickwise.classify.svm_struct.KERNEL_PARM;
import cn.clickwise.classify.svm_struct.LEARN_PARM;
import cn.clickwise.classify.svm_struct.STRUCT_LEARN_PARM;
import cn.clickwise.classify.svm_struct.svm_struct_main;
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

	private static Logger logger = Logger.getLogger(MedLDA.class);

	/**
	 * perform inference on a Document and update sufficient statistics
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

		for (int k = 0; k < m_nK; k++) {
			double dVal = 0;
			for (int n = 0; n < doc.getLength(); n++) {
				ss.class_word[k][doc.words[n]] += doc.counts[n] * phi[n][k];
				ss.class_total[k] += doc.counts[n] * phi[n][k];
				dVal += phi[n][k] * (double) doc.counts[n]
						/ (double) doc.getTotal();
			}

			// suff-stats for supervised LDA
			ss.exp[ss.num_docs][k] = dVal;

		}

		ss.num_docs = ss.num_docs + 1;

		return lhood;
	}

	public boolean mle(SuffStats ss, Params param, boolean bInit) {

		return false;
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
				m_alpha[k] = param.getINITIAL_ALPHA();
			}

		} else if (start.equals("random")) {
			new_model(corpus.num_docs, corpus.num_terms, param.getNTOPICS(),
					param.getNLABELS(), param.getINITIAL_C());
			ss = new_suffstats();
			random_init_ss(ss, corpus);
			mle(ss, param, true);
			for (int k = 0; k < m_nK; k++) {
				m_alpha[k] = param.getINITIAL_ALPHA();
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

			while (((converged < 0) || (converged > param.getEM_CONVERGED() || (i <= 2)))
					&& (i <= param.getEM_MAX_ITER())) {
				logger.info("**** em iteration " + (i + 1) + " ****");
				lhood = 0;
				zero_init_ss(ss);

				// e-step
				for (d = 0; d < corpus.num_docs; d++) {
					for (n = 0; n < max_length; n++)// initialize to uniform
					{
						for (int k = 0; k < param.getNTOPICS(); k++) {
							phi[n][k] = 1.0 / (double) param.getNTOPICS();
						}

						if (d % 1000 == 0) {
							logger.info("Document " + d);
							lhood += doc_e_step(corpus.docs[d], var_gamma[d],
									phi, ss, param);
						}
					}
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
		double dAcc=0;
		
		try {
			PrintWriter fileptr = new PrintWriter(new FileWriter(filename));

			for(d=0;d<corpus.num_docs;d++)
			{
				if(((d%1000)==0)&&(d>0))
				{
					logger.info("Document "+d);
				}
				
				doc=corpus.docs[d];
				phi=new double[doc.length][m_nK];
				
				for(n=0;n<doc.length;n++)
				{
					//initialize to uniform distribution
					for(int k=0;k<m_nK;k++)
					{
						phi[n][k]=1.0;
					}
				}
				
				lhood=inference_pred(doc, var_gamma[d], phi, param);
				
				//do prediction
				predict(doc,phi);
				doc.lhood=lhood;
				
				fileptr.println(lhood);
				
				//update the exp
				for(int k=0;k<m_nK;k++){
					double dVal=0;
					for(n=0;n<doc.length;n++){
						dVal+=phi[n][k]*(double)doc.counts[n]/(double)doc.total;
					}
					
					exp[d][k]=dVal;	
				}					
			}
			
			filename="MedLDA_("+m_nK+"topic)_test.txt";
			outputData2(filename, corpus, exp, m_nK, m_nLabelNum);
			
			filename=model_dir+"/evl-gamma.dat";
			save_gamma(filename,var_gamma,corpus.num_docs,m_nK);
			
			filename=model_dir+"/evl-performance.dat";
		    dAcc=save_prediction(filename,corpus);
			fileptr.close();
			
			fileptr=new PrintWriter(new FileWriter("overall-res.txt",true));
			fileptr.printf("setup (K: %d; C: %.3f; fold: %d; ell: %.2f; dual-opt: %d; alpha: %d; svm_alg: %d; maxIt: %d): accuracy %.3f\n", m_nK, m_dC, param.NFOLDS, param.DELTA_ELL, param.PHI_DUALOPT, param.ESTIMATE_ALPHA, param.SVM_ALGTYPE, param.EM_MAX_ITER, dAcc);
			
	
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
			converged = (lhood_old - lhood) / lhood_old;
			lhood_old = lhood;
		}

		return 0;
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

		return 0;
	}

	public double compute_mrgterm(Document doc, int d, int n, int k,
			Params param) {

		return 0;
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

		return 0;
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
	
	public double save_prediction(String filename, Corpus corpus)
	{
		
		return 0;
	}
	


	public void loss_aug_predict(Document doc, double[] zbar_mean) {

	}

	public double loss(int y, int gnd) {

		return 0;
	}

	public void new_model(int num_docs, int num_terms, int num_topics,
			int num_labels, double C) {

	}

	public SuffStats new_suffstats() {
		return null;
	}

	public void corpus_init_ss(SuffStats ss, Corpus c) {

	}

	public void random_init_ss(SuffStats ss, Corpus c) {

	}

	public void zero_init_ss(SuffStats ss) {

	}

	public void load_model(String model_root) {

	}

	public void set_init_param(STRUCT_LEARN_PARM struct_parm,
			LEARN_PARM learn_parm, KERNEL_PARM kernel_parm, int alg_type) {

	}

	public void svmStructSolver(SuffStats ss, Params param, double res) {

	}

	public void save_model(String model_root) {

	}

	public void save_gamma(String filename, double[][] gamma, int num_docs,
			int num_topics) {

	}

	public void write_word_assignment(PrintWriter f, Document doc,
			double[][] phi) {

	}

	public void outputData2(String filename,Corpus corpus,double[][] exp,int ntopic,int nLabels)
	{
		
		
		
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
