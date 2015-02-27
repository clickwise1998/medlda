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
			    nsv.words[i]=copy_word(sv.words[i]);
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
	
	
	public static CONSTSET copyCONSTSET(CONSTSET constset)
	{
		CONSTSET nconstset=new CONSTSET();
		nconstset.m=constset.m;
		
		if(constset.lhs!=null)
		{
			nconstset.lhs=new DOC[constset.lhs.length];
			for(int i=0;i<constset.lhs.length;i++)
			{
				nconstset.lhs[i]=copyDoc(constset.lhs[i]);
			}
		}
		else
		{
			nconstset.lhs=null;
		}
		
		if(constset.rhs!=null)
		{
			nconstset.rhs=new double[constset.rhs.length];
			for(int i=0;i<constset.rhs.length;i++)
			{
				nconstset.rhs[i]=constset.rhs[i];
			}
		}
		
		
		return nconstset;
	}
	
	public static double get_runtime() {
		int c = (int) TimeOpera.getCurrentTimeLong();
		double hc = 0;
		hc = ((double) c) ;
		return hc;
	}
	
	public static double[] create_nvector(int n) {
		double[] vector;
		vector = new double[n + 1];
		return vector;
	}
	
	public  static double kernel(KERNELPARM kernel_parm, DOC a, DOC b) {
		// System.out.println("in kernel");
		double sum = 0;
		SVECTOR fa, fb;
		if (kernel_parm.kernel_type == GRAM) {
			// System.out.println("kernel_type:" + GRAM);
			if ((a.kernelid >= 0) && (b.kernelid >= 0)) {

				return kernel_parm.gram_matrix.element[Math.max(a.kernelid,
						b.kernelid)][Math.min(a.kernelid, b.kernelid)];
			} else {
				return 0;
			}
		}
		// System.out.println("fa pro");
		for (fa = a.fvec; fa != null; fa = fa.next) {
			for (fb = b.fvec; fb != null; fb = fb.next) {

				if (fa.kernel_id == fb.kernel_id) {
					// if (sum > 0)
					// System.out.println("sum:" + sum);
					sum += fa.factor * fb.factor
							* single_kernel(kernel_parm, fa, fb);
				}
			}
		}

		return sum;
	}
	
	public  static double single_kernel(KERNELPARM kernel_parm, SVECTOR a,
			SVECTOR b) {

		switch (kernel_parm.kernel_type) {
		case LINEAR:
			return sprod_ss(a, b);
		case POLY:
			return Math.pow(kernel_parm.coef_lin * sprod_ss(a, b)
					+ kernel_parm.coef_const, kernel_parm.poly_degree);
		case RBF:
			if (a.twonorm_sq < 0) {
				a.twonorm_sq = sprod_ss(a, a);
			} else if (b.twonorm_sq < 0) {
				b.twonorm_sq = sprod_ss(b, b);
			}
			return Math.exp(-kernel_parm.rbf_gamma
					* (a.twonorm_sq - 2 * sprod_ss(a, b) + b.twonorm_sq));
		case SIGMOID:
			return Math.tanh(kernel_parm.coef_lin * sprod_ss(a, b)
					+ kernel_parm.coef_const);
		case CUSTOM:
			return 0;
		default:
			System.out.println("Error: Unknown kernel function");
			System.exit(1);

		}

		return 0;
	}
	
	public static double sprod_ss(SVECTOR a, SVECTOR b) {
		double sum = 0;
		WORD[] ai, bj;
		ai = a.words;
		bj = b.words;

		int i = 0;
		int j = 0;
		
		while ((i < ai.length) && (j < bj.length)) {
			if (ai[i] == null || bj[j] == null) {
				break;
			}
			// logger.info("i:"+i+"  j:"+j);
			if (ai[i].wnum > bj[j].wnum) {
				j++;
			} else if (ai[i].wnum < bj[j].wnum) {
				i++;
			} else {

				sum += ai[i].weight * bj[j].weight;
				i++;
				j++;
			}
		}

		return sum;
	}
	
	public static void add_vector_ns(double[] vec_n, SVECTOR vec_s,
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
	}
	
	public static double sprod_ns(double[] vec_n, SVECTOR vec_s) {
		double sum = 0;
		WORD[] ai;
		ai = vec_s.words;

		for (int i = 0; i < ai.length; i++) {
			if (ai[i] != null) {
				
				sum += (vec_n[ai[i].wnum] * ai[i].weight);
			} else {
				continue;
			}
		}

		return sum;
	}
	
	public static void mult_vector_ns(double[] vec_n, SVECTOR vec_s,
			double faktor) {
		WORD[] ai;
		ai = vec_s.words;
		for (int i = 0; i < ai.length; i++) {
			if (ai[i] == null) {
				continue;
			}
			vec_n[ai[i].wnum] *= (faktor * ai[i].weight);
		}

	}
	
	public static DOC create_example(int docnum, int queryid, int slackid,
			double costfactor, SVECTOR fvec) {

		DOC example = new DOC();

		example.docnum = docnum;
		example.kernelid = docnum;
		example.queryid = queryid;
		example.slackid = slackid;
		example.costfactor = costfactor;

		example.fvec = fvec;

		return example;
	}
	
	public static SVECTOR create_svector(WORD[] words, String userdefined,
			double factor) {
		SVECTOR vec;
		int  i;


		vec = new SVECTOR();
		vec.words = new WORD[words.length];

		for (i = 0; i < words.length; i++) {
			vec.words[i] = copy_word(words[i]);
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
	
	  public static WORD copy_word(WORD w)
	  {
		  WORD nw=new WORD();
		  nw.weight=w.weight;
		  nw.wnum=w.wnum;
		  return nw;
	  }
	  
	  /** compute length of weight vector */
	  public static double model_length_s(MODEL model)
		{
			int i, j;
			double sum = 0, alphai;
			DOC supveci;
			KERNELPARM kernel_parm = model.kernel_parm;

			for (i = 1; i < model.sv_num; i++) {
				alphai = model.alpha[i];
				supveci = model.supvec[i];
				for (j = 1; j < model.sv_num; j++) {
					sum += alphai * model.alpha[j]
							* kernel(kernel_parm, supveci, model.supvec[j]);
				}
			}
			return (Math.sqrt(sum));
		}
	  
		/**
		 * Makes a copy of model where the support vectors are replaced with a
		 * single linear weight vector.
		 */
		/* NOTE: It adds the linear weight vector also to newmodel->lin_weights */
		/* WARNING: This is correct only for linear models! */
		public static MODEL compact_linear_model(MODEL model)
		{
			MODEL newmodel;
			newmodel = new MODEL();
			newmodel = copyMODEL(model);
			add_weight_vector_to_linear_model(newmodel);
			newmodel.supvec = new DOC[2];
			newmodel.alpha = new double[2];
			newmodel.index = null; /* index is not copied */
			newmodel.supvec[0] = null;
			newmodel.alpha[0] = 0.0;
			newmodel.supvec[1] = create_example(-1,0,0,0,
					create_svector_n(newmodel.lin_weights, newmodel.totwords, null,1.0));
			newmodel.alpha[1] = 1.0;
			newmodel.sv_num = 2;

			return (newmodel);
		}
		
		/** compute weight vector in linear case and add to model */
		public static void add_weight_vector_to_linear_model(MODEL model)
		{
			int i;
			SVECTOR f;
			//logger.info("model.totwords:" + model.totwords);
			model.lin_weights = create_nvector(model.totwords);
			//clear_nvector(model.lin_weights, model.totwords);
			for (i = 1; i < model.sv_num; i++) {
				for (f = (model.supvec[i]).fvec; f != null; f = f.next)
					add_vector_ns(model.lin_weights, f, f.factor * model.alpha[i]);
			}
		}
		
		public static SVECTOR create_svector_n(double[] nonsparsevec,
				int maxfeatnum, String userdefined, double factor) {
			return (create_svector_n_r(nonsparsevec, maxfeatnum, userdefined,
					factor, 0));
		}

		public static SVECTOR create_svector_n_r(double[] nonsparsevec,
				int maxfeatnum, String userdefined, double factor,
				double min_non_zero) {
			// logger.info("begin create_svector_n_r");
			SVECTOR vec;
			int fnum, i;

			fnum = 0;
			for (i = 1; i <= maxfeatnum; i++)
				if ((nonsparsevec[i] < -min_non_zero)
						|| (nonsparsevec[i] > min_non_zero))
					fnum++;

			vec = new SVECTOR();
			vec.words = new WORD[fnum + 1];
			for (int vi = 0; vi < vec.words.length; vi++) {
				vec.words[vi] = new WORD();
			}

			fnum = 0;
			for (i = 1; i <= maxfeatnum; i++) {
				if ((nonsparsevec[i] < -min_non_zero)
						|| (nonsparsevec[i] > min_non_zero)) {
					vec.words[fnum].wnum = i;
					vec.words[fnum].weight = nonsparsevec[i];
					fnum++;
				}
			}
			vec.words[fnum].wnum = 0;
			vec.twonorm_sq = -1;

			if (userdefined != null) {
				vec.userdefined = userdefined;
			} else
				vec.userdefined = null;

			vec.kernel_id = 0;
			vec.next = null;
			vec.factor = factor;
			// logger.info("end create_svector_n_r");
			return (vec);
		}
		
	
}
