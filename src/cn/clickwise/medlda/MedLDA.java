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

	public double doc_e_step(Document doc, double[] gamma, double[][] phi,
			SuffStats ss, Params param) {

		return 0;
	}

	public boolean mle(SuffStats ss, Params param, boolean bInit) {

		return false;
	}

	public int run_em(String start, String directory, Corpus corpus,
			Params param) {
		m_dDeltaEll = param.getDELTA_ELL();

		int d, n;

		long runtime_start=TimeOpera.getCurrentTimeLong();
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
			double lhood=0, lhood_old = 0, converged = 1;
	

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
			
			long runtime_end=TimeOpera.getCurrentTimeLong();
			logger.info("Training time in (cpu-seconds):"+((double)runtime_end-(double)runtime_start)/1000.0);
			
			filename="MedLDA_("+m_nK+"topic)_train.txt";
			
			//output the final model
			filename=directory+"/final";
			save_model(filename);
			
			filename=directory+"/final.gamma";
            save_gamma(filename,var_gamma,corpus.num_docs,m_nK);		
			
			//output the word assignments(for visualization)
	        int nNum=0,nAcc=0;
	        filename=directory+"/word-assignments.dat";
	        
	        PrintWriter w_asgn_file=new PrintWriter(new FileWriter(filename));
	        for(d=0;d<corpus.num_docs;d++)
	        {
	        	if((d%1000)==0)
	        	{
	        	   logger.info("final e step Document "+d);	
	        	}
	        	lhood+=inference(corpus.docs[d],d,var_gamma[d],phi,param);
	        	write_word_assignment(w_asgn_file,corpus.docs[d],phi);
	        	
	        	nNum++;
	        	predict(corpus.docs[d],phi);
	        	
	        	if(corpus.docs[d].getGndlabel()==corpus.docs[d].getPredlabel())
	        	{
	        		nAcc++;
	        	}
	        }
            
	        w_asgn_file.close();
	        likelihood_file.close();
	        logger.info("MedLDA: double count accuracy:"+(double)nAcc / nNum);
            
		} catch (Exception e) {
			e.printStackTrace();
		}

		return nIt;
	}

	public double infer(String model_dir, Corpus corpus, Params param,
			String userId) {

		return 0;
	}

	public double inference(Document doc, int docix, double[] var_gamma,
			double[][] phi, Params param) {

		return 0;
	}

	public double inference_pred(Document doc, double[] var_gamma,
			double[][] phi, Params param) {

		return 0;
	}

	public double compute_mrgterm(Document doc, int d, int n, int k,
			Params param) {

		return 0;
	}

	public double compute_lhood(Document doc, double[][] phi, double[] var_gamma) {

		return 0;
	}

	public void predict(Document doc, double[][] phi) {

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
	
	public void save_gamma(String filename,double[][] gamma,int num_docs,int num_topics)
	{
		
	}
	
	public void write_word_assignment(PrintWriter f,Document doc,double[][] phi)
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
