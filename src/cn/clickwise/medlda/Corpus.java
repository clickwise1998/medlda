package cn.clickwise.medlda;

public abstract class Corpus {

	Document[] docs;
	
	int num_terms;
	
	int num_docs;
	
	public abstract void read_data(String data_filename,int nLabels);
	
	//public abstract void read_buf(String data_filename,int nLabels);
	
	public abstract Corpus get_traindata(int nfold,int foldix);
	
	public abstract Corpus get_testdata(int nfold,int foldix);
	
	public abstract int max_corpus_length();
	
	public abstract void shuffle();
	
}
