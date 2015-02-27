package cn.clickwise.medldatb;

import java.io.File;

import cn.clickwise.classify.svm_struct.svm_common;
import cn.clickwise.classify.svm_struct.svmconfig;
import cn.clickwise.file.utils.FileCreateUtil;

public class Main {
	
	public static CorpusFactory corpusFactory;
	
	public static  void main(String[] args)
	{
		corpusFactory=CorpusFactoryInstantiate.getCorpusFactory();
		
		/*******设置 label 和  word 的格式和medlda一致，label=originLabel+1,word=originWord+1**********/
		svmconfig.model_type=1;
		
		if (args.length > 1)
		{
			Corpus c = corpusFactory.getCorpus();
			Params param=new Params();
			param.INNER_CV = true;
			
			if ( args[0].equals("estinf")  ) {
				param.read_settings("settings.txt");
				param.NTOPICS = Integer.parseInt(args[1]);
				param.NLABELS = Integer.parseInt(args[2]);
				param.NFOLDS = Integer.parseInt(args[3]);
				param.INITIAL_C = Double.parseDouble(args[4]);
				param.DELTA_ELL = Double.parseDouble(args[5]);

				System.err.printf("K: %d, C: %.3f, Alpha: %d, svm: %d\n", param.NTOPICS, 
					param.INITIAL_C, param.ESTIMATE_ALPHA, param.SVM_ALGTYPE);

				c.read_data(param.train_filename, param.NLABELS);
				String dir="";
				dir="20ng"+param.NTOPICS+"_c"+(int)param.INITIAL_C+"_f"+param.NFOLDS;
				//System.err.printf(dir, "20ng%d_c%d_f%d", param.NTOPICS, (int)param.INITIAL_C, param.NFOLDS);
			
				FileCreateUtil.make_directory(dir);

				if ( param.INNER_CV ) {
					c.shuffle();

					String modelDir="";
					modelDir=dir+"/innercv";
					FileCreateUtil.make_directory(modelDir);
	
					param.INITIAL_C = innerCV(modelDir, c, param);
					System.err.printf("\n\nBest C: %f\n", param.INITIAL_C);
				}
				MedLDA model=new MedLDA();
				model.run_em(args[6], dir, c, param);

				// testing.
				Corpus tstC =corpusFactory.getCorpus();
				tstC.read_data(param.test_filename, param.NLABELS);
				MedLDA evlModel=new MedLDA();
				double dAcc = evlModel.infer(dir, tstC, param,"");
				System.err.printf("Accuracy: %.3f\n", dAcc);
				model=null;
				evlModel=null;
		
			}
			if ( args[0].equals("est")) {
				param.read_settings("settings.txt");
				param.NTOPICS = Integer.parseInt(args[1]);
				param.NLABELS = Integer.parseInt(args[2]);
				param.NFOLDS = Integer.parseInt(args[3]);
				param.INITIAL_C = Double.parseDouble(args[4]);
				param.DELTA_ELL = Double.parseDouble(args[5]);
				param.INNER_CV=false;
				c.read_data(param.train_filename, param.NLABELS);
				String dir="";
				dir=args[6]+param.NTOPICS+"_c"+(int)param.INITIAL_C+"_f"+param.NFOLDS;
			
			    FileCreateUtil.make_directory(dir);

				if ( param.INNER_CV ) {
					c.shuffle();

					String modelDir="";
					modelDir=dir+"/innercv";
					
					 FileCreateUtil.make_directory(modelDir);

					param.INITIAL_C = innerCV(modelDir, c, param);
					System.err.printf("\n\nBest C: %f\n", param.INITIAL_C);
				}
				MedLDA model=new MedLDA();
				model.run_em(args[7], dir, c, param);
				model=null;
			}
			if (args[0].equals("inf"))
			{
				param.read_settings("settings.txt");
				param.NLABELS = Integer.parseInt(args[1]);
				param.INNER_CV=false;
				c.read_data(param.test_filename, param.NLABELS);
				MedLDA model=new MedLDA();
				double dAcc = model.infer(args[2], c, param,"");
				System.err.printf("Accuracy: %.3f\n", dAcc);
				model=null;
			}

		} else {
			System.err.printf("usage : MEDsLDAc estinf [k] [labels] [fold] [initial C] [l] [random/seeded/*]\n");
			System.err.printf("        MEDsLDAc est [k] [labels] [fold] [initial C] [l] [dir root] [random/seeded/*]\n");
			System.err.printf("        MEDsLDAc inf [labels] [model]\n");
		}
		
	}
	
	public static double innerCV(String modelDir, Corpus c, Params param)
	{
		int nMaxUnit = c.num_docs / 5 + 5;

		double dBestAccuracy = 0;
		double dBestC = 0;
		for ( int k=0; k<param.CV_PARAMNUM; k++ )
		{
			System.err.printf("\n\n$$$ Learning with C: %.4f $$$ \n\n", param.vec_cvparam[k]);

			param.INITIAL_C = param.vec_cvparam[k];
			double dAvgAccuracy = 0;
			for ( int i=1; i<=param.INNER_FOLDNUM; i++ )
			{
				// get training & test data
				Corpus trDoc = c.get_traindata(param.INNER_FOLDNUM, i);
				Corpus tstDoc = c.get_testdata(param.INNER_FOLDNUM, i);

				MedLDA model=new MedLDA();
				model.run_em("random", modelDir, trDoc, param);

				// predict on test corpus
				MedLDA evlModel=new MedLDA();
				double dAcc = evlModel.infer(modelDir, tstDoc, param,"");
				dAvgAccuracy += dAcc / param.INNER_FOLDNUM;

				System.err.println("\n\n");
			}
			System.err.printf("@@@ Avg Accuracy: %.4f\n\n", dAvgAccuracy);

			if ( dAvgAccuracy > dBestAccuracy )	{
				dBestC = param.vec_cvparam[k];
				dBestAccuracy = dAvgAccuracy;
			}
		}
		
		return dBestC;
	}
}
