package cn.clickwise.medlda;

import cn.clickwise.classify.svm_struct.KERNEL_PARM;
import cn.clickwise.classify.svm_struct.LEARN_PARM;
import cn.clickwise.classify.svm_struct.STRUCT_LEARN_PARM;

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
    
    public double doc_e_step(Document doc,double[] gamma,double[][] phi,SuffStats ss,Params param)
    {
    	
    	return 0;
    }
    
    public boolean mle(SuffStats ss,Params param,boolean bInit)
    {
    	
    	return false;
    }
    
    
    public int run_em(String start,String directory,Corpus corpus,Params param)
    {
    	
    	
    	
    	return 0;
    }
    
    public double infer(String model_dir,Corpus corpus,Params param,String userId)
    {
    	
    	return 0;
    }
    
    public double inference(Document doc,int docix,double[] var_gamma,double[][] phi,Params param)
    {
    	
    	return 0;
    }
    
    public double inference_pred(Document doc,double[] var_gamma,double[][] phi,Params param)
    {
    	
    	return 0;
    }
    
    public double compute_mrgterm(Document doc,int d, int n, int k,Params param)
    {
    	
    	return 0;
    }
    
    public double compute_lhood(Document doc,double[][] phi, double[] var_gamma)
    {
    	
    	return 0;
    }
    
    public void predict(Document doc,double[][] phi)
    {
    	
    }
    
    public void loss_aug_predict(Document doc,double[] zbar_mean)
    {
    	
    }
    
    public double loss(int y, int gnd)
    {
    	
    	return 0;
    }
    
    public void new_model(int num_docs, int num_terms, int num_topics, int num_labels, double C)
    {
    	
    }
    
    public SuffStats new_suffstats()
    {
    	return null;
    }
    
    public void corpus_init_ss(SuffStats ss,Corpus c)
    {
    	
    }
    
    public void random_init_ss(SuffStats ss,Corpus c)
    {
    	
    }
    
    public void zero_init_ss(SuffStats ss)
    {
    	
    }
    
    public void load_model(String model_root)
    {
    	
    }
    
    public void set_init_param(STRUCT_LEARN_PARM struct_parm,LEARN_PARM learn_parm,KERNEL_PARM kernel_parm,int alg_type)
    {
    	
    }
    
    public void svmStructSolver(SuffStats ss, Params param, double res)
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
