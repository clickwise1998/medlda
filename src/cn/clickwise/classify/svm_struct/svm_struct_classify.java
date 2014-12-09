package cn.clickwise.classify.svm_struct;

import java.io.File;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import cn.clickwise.file.utils.FileWriterUtil;

public class svm_struct_classify {

	public static String testfile="";
	public static String modelfile="";
	public static String predictionsfile="";
	public STRUCTMODEL model;
	private static Logger logger = Logger.getLogger(svm_struct_classify.class);
	
	public static void main(String[] args) throws Exception
	{
		int correct=0,incorrect=0,no_accuracy=0;
		int i;
		double t1,runtime=0;
		double avgloss=0,l=0;
		PrintWriter predfl;
		STRUCTMODEL model;
		STRUCT_LEARN_PARM sparm=new STRUCT_LEARN_PARM();
		STRUCT_TEST_STATS teststats=null;
		SAMPLE testsample;
		LABEL y=new LABEL();
		
		svm_struct_api ssa=new svm_struct_api();
		svm_struct_api.svm_struct_classify_api_init(args.length+1,args);

		read_input_parameters(args.length+1,args,sparm,
					svm_common.verbosity,svm_struct_common.struct_verbosity);
	
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("Reading model ...");
		}
		
		logger.info("testfile:"+testfile);
		logger.info("modelfile:"+modelfile);
		logger.info("predictionsfile:"+predictionsfile);
		
		model=svm_struct_api.read_struct_model(modelfile, sparm);
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("done");
		}
		
		
		if(model.svm_model.kernel_parm.kernel_type==svm_common.LINEAR)
		{
			svm_common.add_weight_vector_to_linear_model(model.svm_model);
			model.w=model.svm_model.lin_weights;
			/*
			String wstr="";
			for(int wi=0;wi<model.w.length;wi++)
			{
				wstr+=(wi+":"+model.w[wi]+" ");
			}
			*/
		//	logger.info("wstr in ssc:"+wstr);
			
		}
		
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("Reading test examples ...");
			System.out.println("Reading test examples ...");
		}
		
		testsample=ssa.read_struct_examples(testfile, sparm);
		
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("done.");
		}
		
		
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("Classifying test examples ...");
		}
		
		
		predfl=FileWriterUtil.getPW(predictionsfile);
		
		for(i=0;i<testsample.n;i++)
		{
			
			t1=svm_common.get_runtime();
			logger.info("doc ["+i+"] "+testsample.examples[i].x.doc.fvec.toString());
			y=ssa.classify_struct_example(testsample.examples[i].x, model, sparm);
			if(y==null)
			{
				continue;
			}
			logger.info("y:"+y.class_index+"  testsample.examples["+i+"].y:"+testsample.examples[i].y.class_index);
			runtime+=(svm_common.get_runtime()-t1);
			svm_struct_api.write_label(predfl, y);
			
			l=svm_struct_api.loss(testsample.examples[i].y, y, sparm);
			
			avgloss+=l;
			if(l==0)
			{
				correct++;
			}
			else
			{
				incorrect++;
			}
			
			svm_struct_api.eval_prediction(i,testsample.examples[i],y, model, sparm, teststats);
			
			if(svm_struct_api.empty_label(testsample.examples[i].y))
			{
				no_accuracy=1;
			}
			
			if(svm_struct_common.struct_verbosity>=2)
			{
				if((i+1)%100==0)
				{
					logger.info(i+1);
				}
			}
			
		}
		
		avgloss/=testsample.n;
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("done");
			logger.info("Runtime (without IO) in cpu-seconds:"+(float)(runtime/100.0));
		}
		
		//if((no_accuracy==0)&&(svm_struct_common.struct_verbosity>=1))
		//{
			logger.info("Average loss on test set:"+(float)avgloss);
			logger.info("Zero/one-error on test set "+(float)100.0*incorrect/testsample.n+"("+correct+" correct, "+incorrect+" incorrect,"+testsample.n+", total");
		//}
		
		svm_struct_api.print_struct_testing_stats(testsample, model, sparm, teststats);
		
	}
	
	/*
	public void classfiy(String[] args,STRUCTMODEL model,STRUCT_LEARN_PARM sparm,MODEL modelt,SAMPLE sample)
	{
		int correct=0,incorrect=0,no_accuracy=0;
		int i;
		double t1,runtime=0;
		double avgloss=0,l=0;
		PrintWriter predfl;
		STRUCT_TEST_STATS teststats=null;
		//SAMPLE testsample;
		LABEL y;
		testfile=args[0];
	    modelfile=args[1];
		predictionsfile=args[2];
		svm_struct_api.svm_struct_classify_api_init(args.length+1,args);
		
		read_input_parameters(args.length+1,args,sparm,
					svm_common.verbosity,svm_struct_common.struct_verbosity);
	
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("Reading model ...");
		}
		
		//model=svm_struct_api.read_struct_model(modelfile, sparm);
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("done");
		}
		
		
		if(model.svm_model.kernel_parm.kernel_type==svm_common.LINEAR)
		{
			//svm_common.add_weight_vector_to_linear_model(model.svm_model);
			//model.w=model.svm_model.lin_weights;
		}
		
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("Reading test examples ...");
		}
		
		//testsample=svm_struct_api.read_struct_examples(testfile, sparm);
		
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("done.");
		}
		
		
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("Classifying test examples ...");
		}
		
		
		predfl=FileWriterUtil.getPW(predictionsfile);
		
		for(i=0;i<sample.n;i++)
		{
			//logger.info("test i:"+i+" "+testsample.examples[i].x.doc.fvec.toString());
			t1=svm_common.get_runtime();
			y=svm_struct_api.classify_struct_example(sample.examples[i].x, model, sparm);
			logger.info("y:"+y.class_index+"  testsample.examples[i].y:"+sample.examples[i].y.class_index);
			runtime+=(svm_common.get_runtime()-t1);
			svm_struct_api.write_label(predfl, y);
			
			l=svm_struct_api.loss(sample.examples[i].y, y, sparm);
			
			avgloss+=l;
			if(l==0)
			{
				correct++;
			}
			else
			{
				incorrect++;
			}
			
			svm_struct_api.eval_prediction(i,sample.examples[i],y, model, sparm, teststats);
			
			if(svm_struct_api.empty_label(sample.examples[i].y))
			{
				no_accuracy=1;
			}
			
			if(svm_struct_common.struct_verbosity>=2)
			{
				if((i+1)%100==0)
				{
					logger.info(i+1);
				}
			}
			
		}
		
		avgloss/=sample.n;
		if(svm_struct_common.struct_verbosity>=1)
		{
			logger.info("done");
			logger.info("Runtime (without IO) in cpu-seconds:"+(float)(runtime/100.0));
		}
		
		if((no_accuracy==0)&&(svm_struct_common.struct_verbosity>=1))
		{
			logger.info("Average loss on test set:"+(float)avgloss);
			logger.info("Zero/one-error on test set "+(float)100.0*incorrect/sample.n+"("+correct+" correct, "+incorrect+" incorrect,"+sample.n+", total");
		}
		
		svm_struct_api.print_struct_testing_stats(sample, model, sparm, teststats);
	}
	
	*/
	public static void read_input_parameters(int argc,String[] argv,STRUCT_LEARN_PARM struct_parm,int verbosity,int struct_verbosity)
	{
		int i;
		modelfile="svm_model";
		predictionsfile="svm_predictions";
		verbosity=0;
		struct_verbosity=1;
		struct_parm.custom_argc=0;
		
		  for(i=1;(i<argc) &&((argv[i].charAt(0)) == '-');i++) {
			    switch ((argv[i].charAt(1))) 
			      { 
			      case 'h': print_help(); System.exit(0);
			      case '?': print_help(); System.exit(0);
			      case '-': struct_parm.custom_argv[struct_parm.custom_argc++]=argv[i];i++; struct_parm.custom_argv[struct_parm.custom_argc++]=argv[i];break; 
			      case 'v': i++; struct_verbosity=Integer.parseInt(argv[i]); break;
			      case 'y': i++; verbosity=Integer.parseInt(argv[i]); break;
			      default: System.out.println("\nUnrecognized option "+argv[i]+"!\n\n");
				       print_help();
				       System.exit(0);
			      }
			  }
		
		    if((i+1)>=argc)
		    {
		    	System.out.println("Not enough input parameters!");
		    	print_help();
		    	System.exit(0);
		    }
		  
		  testfile=argv[0];
          modelfile=argv[1];		
	
          System.out.println("testfile:"+testfile);
          System.out.println("modelfile:"+modelfile);
          
          if((i+2)<argc){
        	  predictionsfile=argv[2];
          }
          
          
          svm_struct_api.parse_struct_parameters_classify(struct_parm);
	}
	
	
	public static void print_help()
	{
		  System.out.println("\nSVM-struct classification module: "+svm_struct_common.INST_NAME+", "+svm_struct_common.INST_VERSION+", "+svm_struct_common.INST_VERSION_DATE+"\n");
		  System.out.println("   includes SVM-struct "+svm_struct_common.STRUCT_VERSION+" for learning complex outputs, "+svm_struct_common.STRUCT_VERSION_DATE+"\n");
		  System.out.println("   includes SVM-light "+svm_common.VERSION+" quadratic optimizer, "+svm_common.VERSION_DATE+"\n");
		  svm_common.copyright_notice();
		  System.out.println("   usage: svm_struct_classify [options] example_file model_file output_file\n\n");
		  System.out.println("options: -h         -> this help\n");
		  System.out.println("         -v [0..3]  -> verbosity level (default 2)\n\n");

		  svm_struct_api.print_struct_help_classify();
		}
	
}
