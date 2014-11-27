package cn.clickwise.medlda;

public class Params {

	private int LAG;
	
	private double EM_CONVERGED;
	
	private int EM_MAX_ITER;
	
	private int ESTIMATE_ALPHA;
	
	private double INITIAL_ALPHA;
	
	private double INITIAL_C;
	
	private int NTOPICS;
	
	private int NLABELS;
	
	private int NFOLDS;
	
	private int FOLDIX;
	
	private boolean INNER_CV;
	
	private double DELTA_ELL;
	
	private int PHI_DUALOPT;
	
	private int VAR_MAX_ITER;
	
	private double VAR_CONVERGED;
	
	private double[] vec_cvparam;
	
	private int INNER_FOLDNUM;
	
	private int CV_PARAMNUM;
	
	private int SVM_ALGTYPE;
	
	private String train_filename;
	
	private String test_filename;
	
	public void read_settings(String filename)
	{
		
	}

	public int getLAG() {
		return LAG;
	}

	public void setLAG(int lAG) {
		LAG = lAG;
	}

	public double getEM_CONVERGED() {
		return EM_CONVERGED;
	}

	public void setEM_CONVERGED(double eM_CONVERGED) {
		EM_CONVERGED = eM_CONVERGED;
	}

	public int getEM_MAX_ITER() {
		return EM_MAX_ITER;
	}

	public void setEM_MAX_ITER(int eM_MAX_ITER) {
		EM_MAX_ITER = eM_MAX_ITER;
	}

	public int getESTIMATE_ALPHA() {
		return ESTIMATE_ALPHA;
	}

	public void setESTIMATE_ALPHA(int eSTIMATE_ALPHA) {
		ESTIMATE_ALPHA = eSTIMATE_ALPHA;
	}

	public double getINITIAL_ALPHA() {
		return INITIAL_ALPHA;
	}

	public void setINITIAL_ALPHA(double iNITIAL_ALPHA) {
		INITIAL_ALPHA = iNITIAL_ALPHA;
	}

	public double getINITIAL_C() {
		return INITIAL_C;
	}

	public void setINITIAL_C(double iNITIAL_C) {
		INITIAL_C = iNITIAL_C;
	}

	public int getNTOPICS() {
		return NTOPICS;
	}

	public void setNTOPICS(int nTOPICS) {
		NTOPICS = nTOPICS;
	}

	public int getNLABELS() {
		return NLABELS;
	}

	public void setNLABELS(int nLABELS) {
		NLABELS = nLABELS;
	}

	public int getNFOLDS() {
		return NFOLDS;
	}

	public void setNFOLDS(int nFOLDS) {
		NFOLDS = nFOLDS;
	}

	public int getFOLDIX() {
		return FOLDIX;
	}

	public void setFOLDIX(int fOLDIX) {
		FOLDIX = fOLDIX;
	}

	public boolean isINNER_CV() {
		return INNER_CV;
	}

	public void setINNER_CV(boolean iNNER_CV) {
		INNER_CV = iNNER_CV;
	}

	public double getDELTA_ELL() {
		return DELTA_ELL;
	}

	public void setDELTA_ELL(double dELTA_ELL) {
		DELTA_ELL = dELTA_ELL;
	}

	public int getPHI_DUALOPT() {
		return PHI_DUALOPT;
	}

	public void setPHI_DUALOPT(int pHI_DUALOPT) {
		PHI_DUALOPT = pHI_DUALOPT;
	}

	public int getVAR_MAX_ITER() {
		return VAR_MAX_ITER;
	}

	public void setVAR_MAX_ITER(int vAR_MAX_ITER) {
		VAR_MAX_ITER = vAR_MAX_ITER;
	}

	public double getVAR_CONVERGED() {
		return VAR_CONVERGED;
	}

	public void setVAR_CONVERGED(double vAR_CONVERGED) {
		VAR_CONVERGED = vAR_CONVERGED;
	}

	public double[] getVec_cvparam() {
		return vec_cvparam;
	}

	public void setVec_cvparam(double[] vec_cvparam) {
		this.vec_cvparam = vec_cvparam;
	}

	public int getINNER_FOLDNUM() {
		return INNER_FOLDNUM;
	}

	public void setINNER_FOLDNUM(int iNNER_FOLDNUM) {
		INNER_FOLDNUM = iNNER_FOLDNUM;
	}

	public int getCV_PARAMNUM() {
		return CV_PARAMNUM;
	}

	public void setCV_PARAMNUM(int cV_PARAMNUM) {
		CV_PARAMNUM = cV_PARAMNUM;
	}

	public int getSVM_ALGTYPE() {
		return SVM_ALGTYPE;
	}

	public void setSVM_ALGTYPE(int sVM_ALGTYPE) {
		SVM_ALGTYPE = sVM_ALGTYPE;
	}

	public String getTrain_filename() {
		return train_filename;
	}

	public void setTrain_filename(String train_filename) {
		this.train_filename = train_filename;
	}

	public String getTest_filename() {
		return test_filename;
	}

	public void setTest_filename(String test_filename) {
		this.test_filename = test_filename;
	}
	
	
	
	
	
}
