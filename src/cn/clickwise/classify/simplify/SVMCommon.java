package cn.clickwise.classify.simplify;

import cn.clickwise.time.utils.TimeOpera;

/**
 * utility methods
 * @author lq
 */
public class SVMCommon {
	
	public static int verbosity = 0;
	
	//kernel types
	public static final short LINEAR = 0;
	public static final short POLY = 1;
	public static final short RBF = 2;
	public static final short SIGMOID = 3;
	public static final short CUSTOM = 4;
	public static final short GRAM = 5;
	
	
	
	/**
	 * copy doc
	 * @param doc
	 * @return
	 */
	public  static DOC copyDoc(DOC doc) {
		DOC ndoc = new DOC();
		ndoc.docnum = doc.docnum;
		ndoc.queryid = doc.queryid;
		ndoc.costfactor = doc.costfactor;
		ndoc.slackid = doc.slackid;
		ndoc.kernelid = doc.kernelid;
		if (doc.fvec == null) {
			ndoc.fvec = null;
		} else {
			ndoc.fvec = copySVECTOR(doc.fvec);
		}
		if (doc.lvec == null) {
			ndoc.lvec = null;
		} else {

			ndoc.lvec = new int[doc.lvec.length];
			for (int i = 0; i < doc.lvec.length; i++) {
				ndoc.lvec[i] = doc.lvec[i];
			}
		}
		return ndoc;
	}
	
	/**
	 * copy svector
	 * @param sv
	 * @return
	 */
	public static SVECTOR copySVECTOR(SVECTOR sv)
	{
		  SVECTOR nsv=new SVECTOR();
		  nsv.twonorm_sq=sv.twonorm_sq;
		  nsv.userdefined=sv.userdefined;
		  nsv.kernel_id=sv.kernel_id;
		  nsv.factor=sv.factor;
		  nsv.words=new WORD[sv.words.length];
		  for(int i=0;i<sv.words.length;i++)
		  {
			  if(sv.words[i]!=null)
			  {
			    nsv.words[i]=sv.words[i].copy_word();
			  }
			  else
			  {
				  nsv.words[i]=null;
			  }
		  }
		  if(sv.next!=null)
		  {
		  nsv.next=copySVECTOR(sv.next);	  
		  }
		  else
		  {
			  sv.next=null;
		  }
		  return nsv;
	}
	
	/**
	 * copy the kernel parms
	 * @param kp
	 * @return
	 */
	public static KERNELPARM copyKERNELPARM(KERNELPARM kp)
	{
		  KERNELPARM nkp=new KERNELPARM();
		  nkp.kernel_type=kp.kernel_type;
		  nkp.poly_degree=kp.poly_degree;
		  nkp.rbf_gamma=kp.rbf_gamma;
		  nkp.coef_lin=kp.coef_lin;
		  nkp.coef_const=kp.coef_const;
		  nkp.custom=kp.custom;
		  
		  return nkp;
	}
	
	
	public static MODEL copyMODEL(MODEL model)
	  {
		MODEL nmodel=new MODEL();
		nmodel.sv_num=model.sv_num;
		nmodel.at_upper_bound=model.at_upper_bound;
		nmodel.b=model.b;
		if(model.supvec!=null)
		{
		  nmodel.supvec=new DOC[model.supvec.length];
		  for(int i=0;i<model.supvec.length;i++)
		  {
			  if(model.supvec[i]!=null)
			  {
			    nmodel.supvec[i]=copyDoc(model.supvec[i]);
			  }
			  else
			  {
				nmodel.supvec[i]=null;
			  }
		  }
		}
		else
		{
			nmodel.supvec=null;
		}
		nmodel.alpha=new double[model.alpha.length];
		for(int i=0;i<model.alpha.length;i++)
		{
			nmodel.alpha[i]=model.alpha[i];
		}
		if(model.index!=null)
		{
		  nmodel.index=new int[model.index.length];
		  for(int i=0;i<model.index.length;i++)
		  {
			nmodel.index[i]=model.index[i];
		  }
		}
		else
		{
			nmodel.index=null;
		}
		
		nmodel.totwords=model.totwords;
		nmodel.totdoc=model.totdoc;
		nmodel.kernel_parm=copyKERNELPARM(model.kernel_parm);
		nmodel.loo_error=model.loo_error;
		nmodel.loo_recall=model.loo_recall;
		nmodel.loo_precision=model.loo_precision;
		nmodel.xa_error=model.xa_error;
		nmodel.xa_recall=model.xa_recall;
		nmodel.xa_precision=model.xa_precision;
		if(model.lin_weights!=null)
		{
		nmodel.lin_weights=new double[model.lin_weights.length];
		for(int i=0;i<model.lin_weights.length;i++)
		{
			nmodel.lin_weights[i]=model.lin_weights[i];
		}
		}
		else
		{
			nmodel.lin_weights=null;
		}
		nmodel.maxdiff=model.maxdiff;
		
		return nmodel;
	  }
	
	
	public static double get_runtime() {
		int c = (int) TimeOpera.getCurrentTimeLong();
		double hc = 0;
		hc = ((double) c) ;
		return hc;
	}
}
