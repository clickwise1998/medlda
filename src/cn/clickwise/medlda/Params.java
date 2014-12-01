package cn.clickwise.medlda;

import java.io.BufferedReader;
import java.io.FileReader;

import cn.clickwise.str.basic.SSO;

public class Params {

	public int LAG;
	
	public double EM_CONVERGED;
	
	public int EM_MAX_ITER;
	
	public int ESTIMATE_ALPHA;
	
	public double INITIAL_ALPHA;
	
	public double INITIAL_C;
	
	public int NTOPICS;
	
	public int NLABELS;
	
	public int NFOLDS;
	
	public int FOLDIX;
	
	public boolean INNER_CV;
	
	public double DELTA_ELL;
	
	public int PHI_DUALOPT;
	
	public int VAR_MAX_ITER;
	
	public double VAR_CONVERGED;
	
	public double[] vec_cvparam;
	
	public int INNER_FOLDNUM;
	
	public int CV_PARAMNUM;
	
	public int SVM_ALGTYPE;
	
	public String train_filename;
	
	public String test_filename;
	
	public void read_settings(String filename)
	{
		BufferedReader fileptr=null;
		try{
			String alpha_action="";
			fileptr=new BufferedReader(new FileReader(filename));
			String line="";
			line=fileptr.readLine();
			VAR_MAX_ITER=Integer.parseInt(SSO.afterStr(line, "var max iter").trim());
			
			line=fileptr.readLine();
			VAR_CONVERGED=Double.parseDouble(SSO.afterStr(line, "var convergence").trim());
			
			line=fileptr.readLine();
			EM_MAX_ITER=Integer.parseInt(SSO.afterStr(line, "em max iter").trim());
			
			line=fileptr.readLine();
			EM_CONVERGED=Double.parseDouble(SSO.afterStr(line, "em convergence").trim());
			
			line=fileptr.readLine();
			INITIAL_C=Double.parseDouble(SSO.afterStr(line, "model C").trim());
			
			line=fileptr.readLine();
			INITIAL_ALPHA=Double.parseDouble(SSO.afterStr(line, "init alpha").trim());
			
			line=fileptr.readLine();
			SVM_ALGTYPE=Integer.parseInt(SSO.afterStr(line, "svm_alg_type").trim());
			
			line=fileptr.readLine();
			ESTIMATE_ALPHA=Integer.parseInt(SSO.afterStr(line, "alpha").trim());
			
			line=fileptr.readLine();
			PHI_DUALOPT=Integer.parseInt(SSO.afterStr(line, "phi-dual-opt").trim());
			
			line=fileptr.readLine();
			alpha_action=SSO.afterStr(line, "inner-cv").trim();
			
			line=fileptr.readLine();
			INNER_FOLDNUM=Integer.parseInt(SSO.afterStr(line, "inner_foldnum").trim());
			
			line=fileptr.readLine();
			CV_PARAMNUM=Integer.parseInt(SSO.afterStr(line, "cv_paramnum").trim());
			
			vec_cvparam = new double[CV_PARAMNUM];
			for ( int i=0; i<CV_PARAMNUM; i++ ) {
				double tmp;
				line=fileptr.readLine();
				tmp=Double.parseDouble(line);
				vec_cvparam[i] = tmp;
			}
			
			line=fileptr.readLine();
			train_filename=SSO.afterStr(line, "train_file:").trim();
			line=fileptr.readLine();
			test_filename=SSO.afterStr(line, "test_file:").trim();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
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
