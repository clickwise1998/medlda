package cn.clickwise.medldatb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

import cn.clickwise.str.basic.SSO;

public class TBCorpus extends Corpus{

	private static final int OFFSET =0;  
	
	@Override
	public void read_data(String data_filename, int nLabels) {
		
		BufferedReader fileptr=null;
		int length, count, word, n, nd=0, nw=0;
		String wc="";
		System.err.printf("reading data from %s\n", data_filename);
		Scanner scan=null;
		String line="";
		ArrayList<Document> doclist=new ArrayList<Document>();
		Document doc;
		try{
			docs = null;
			num_terms = 0;
			num_docs = 0;
			fileptr = new BufferedReader(new FileReader(data_filename));
			nd = 0; nw = 0;
			
			while((line=fileptr.readLine())!=null)
			{
				if(SSO.tioe(line))
				{
					continue;
				}
				scan=new Scanner(line);				
				doc=new Document();
				
				length=scan.nextInt();
				doc.setLength(length);
				doc.total=0;
				doc.words=new int[length];
				doc.counts=new int[length];
				
				int flabel,slabel,tlabel,flabelsize,slabelsize,tlabelsize;
				
				flabel=scan.nextInt();
				slabel=scan.nextInt();
				tlabel=scan.nextInt();
				
				flabelsize=scan.nextInt();
				slabelsize=scan.nextInt();
				tlabelsize=scan.nextInt();
				
				doc.tbgndlabel=new TBLabel(flabel,slabel,tlabel,flabelsize,slabelsize,tlabelsize);
					
				//doc.setGndlabel(label);
				
				for (n = 0; n < length; n++) {
					wc=scan.next();
					word=Integer.parseInt(SSO.beforeStr(wc, ":"));
					count=Integer.parseInt(SSO.afterStr(wc, ":"));
		
					word = word - OFFSET;
					doc.words[n] = word;
					doc.counts[n] = count;
					doc.total += count;
					if (word >= nw) { nw = word + 1; }
				}
				doclist.add(doc);
				nd++;
				
			}
			docs=new Document[doclist.size()];
			for(int i=0;i<docs.length;i++)
			{
				docs[i]=doclist.get(i);
			}
			num_docs = nd;
			num_terms = nw;
			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.err.printf("number of docs    : %d\n", nd);
		System.err.printf("number of terms   : %d\n", nw);
	}

	//@Override
	//public void read_buf(String data_filename, int nLabels) {
	//	// TODO Auto-generated method stub	
	//}

	@Override
	public Corpus get_traindata(int nfold, int foldix) {
		// TODO Auto-generated method stub
		
		int nunit = num_docs / nfold;

		Corpus subc = new SimpleCorpus();
		subc.docs = null;
		subc.num_docs = 0;
		subc.num_terms = 0;
		int nd = 0, nw = 0;
		subc.docs=new Document[num_docs];
		for(int i=0;i<num_docs;i++)
		{
			subc.docs[i]=new Document();
		}
		
		for ( int i=0; i<num_docs; i++ )
		{
			if ( foldix < nfold ) {
				if ( (i >= (foldix-1)*nunit) && ( i < foldix*nunit ) ) continue;
			} else {
				if ( i >= (foldix-1) * nunit ) continue;
			}

			subc.docs[nd].length = docs[i].length;
			subc.docs[nd].total = docs[i].total;
			subc.docs[nd].words = new int[docs[i].length];
			subc.docs[nd].counts = new int[docs[i].length];
			
			// read the response variable
			//subc.docs[nd].gndlabel = docs[i].gndlabel;
			subc.docs[nd].tbgndlabel = docs[i].tbgndlabel;
			for (int n=0; n<docs[i].length; n++) {
				subc.docs[nd].words[n] = docs[i].words[n];
				subc.docs[nd].counts[n] = docs[i].counts[n];
				if (docs[i].words[n] >= nw) { nw = docs[i].words[n] + 1; }
			}
			nd++;
		}
		subc.num_docs = nd;
		subc.num_terms = nw;
		
		return subc;
		
	}

	@Override
	public Corpus get_testdata(int nfold, int foldix) {
		int nunit = num_docs / nfold;

		Corpus subc = new SimpleCorpus();
		subc.docs = null;
		subc.num_docs = 0;
		subc.num_terms = 0;
		int nd = 0, nw = 0;
		subc.docs=new Document[num_docs];
		for(int i=0;i<num_docs;i++)
		{
		  subc.docs[i]=new Document();	
		}
		
		for ( int i=0; i<num_docs; i++ )
		{
			if ( foldix < nfold ) {
				if ( i < ((foldix-1)*nunit) || i >= foldix*nunit ) continue;
			} else {
				if ( i < (foldix-1) * nunit ) continue;
			}

			subc.docs[nd].length = docs[i].length;
			subc.docs[nd].total = docs[i].total;
			subc.docs[nd].words = new int[docs[i].length];
			subc.docs[nd].counts = new int[docs[i].length];
			
			// read the response variable
			//subc.docs[nd].gndlabel = docs[i].gndlabel;
			subc.docs[nd].tbgndlabel = docs[i].tbgndlabel;
			
			for (int n = 0; n < docs[i].length; n++)
			{
				subc.docs[nd].words[n] = docs[i].words[n];
				subc.docs[nd].counts[n] = docs[i].counts[n];
				if (docs[i].words[n] >= nw) { nw = docs[i].words[n] + 1; }
			}
			nd++;
		}
		subc.num_docs = nd;
		subc.num_terms = nw;
		return subc;
	}

	@Override
	public int max_corpus_length() {
		int max = 0;
		for (int n=0; n<num_docs; n++)
			if (docs[n].length > max) max = docs[n].length;
		return(max);
	}

	@Override
	public void shuffle() {
	
		int n = 0;
		for ( n=0; n<num_docs*100; n++ )
		{
			int ix1 = ((int)(Math.random()*num_docs)) % num_docs;
			int ix2 = ((int)(Math.random()*num_docs)) % num_docs;
			if ( ix1 == ix2 ) continue;
			
			Document p = docs[ix1];
			docs[ix1] = docs[ix2];
			docs[ix2] = p;
		}
		
	}


}
