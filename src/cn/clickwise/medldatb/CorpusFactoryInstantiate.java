package cn.clickwise.medldatb;

public class CorpusFactoryInstantiate {

	public static CorpusFactory getCorpusFactory(){
		
		if(MedLDAConfig.sampleType==0)
		{
		  return new SimpleCorpusFactory();
		}
		else if(MedLDAConfig.sampleType==1)
		{
		  return new TBCorpusFactory();
		}
		return null;
	}
}
