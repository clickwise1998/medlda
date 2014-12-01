package cn.clickwise.medlda;

public class CorpusFactoryInstantiate {

	public static CorpusFactory getCorpusFactory(){
		return new SimpleCorpusFactory();
	}
}
