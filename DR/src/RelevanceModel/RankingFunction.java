package RelevanceModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

public class RankingFunction {
	
	private Directory index;
	private IndexReader reader;
	// index and index reader for external documentation, for query expansion
	private Directory index_ext;
	private IndexReader reader_ext;
	private double lambda;
	private double[] docProbs;
	private boolean useWholeCollection;
	private boolean expandQuery;
	private List<Integer> LuceneDocs;
	
	public RankingFunction(Directory index, double lambda) throws IOException{
		this.index=index;
		this.lambda = lambda;
		reader = DirectoryReader.open(this.index);	
		useWholeCollection = true;
		expandQuery = false;
		this.LuceneDocs = null;
	}
	
	//Constructor for query expansion
	public RankingFunction(Directory index,Directory index_ext, double lambda) throws IOException{
		this.index=index;
		this.index_ext=index_ext;
		this.lambda = lambda;
		reader = DirectoryReader.open(this.index);
		reader_ext = DirectoryReader.open(this.index_ext);	
		useWholeCollection = true;
		expandQuery = true;
		this.LuceneDocs = null;
	}
	
	public RankingFunction(Directory index, double lambda,List<Integer> LuceneDocs) throws IOException{
		this.index=index;
		this.lambda = lambda;
		reader = DirectoryReader.open(this.index);	
		useWholeCollection = false;
		expandQuery = false;
		this.LuceneDocs = LuceneDocs;
	}
	
	public void getQueryProbability(Query q) throws IOException{
		int[] docFreqs = getDocFrequencies(reader);
		int[] docFreqs_ext = null;
		
		IndexReader ir = expandQuery==true?reader_ext:reader;
		
		Set<Term> terms = new HashSet<Term>();
		//Map<Term,double[]> queryTermProbs = new HashMap<Term,double[]>();
		q.extractTerms(terms);
		
		double [] queryProbs = new double[ir.numDocs()];
		Map<Integer,Double> queryProbsMap = new HashMap<Integer,Double>();
		Arrays.fill(queryProbs, 1.0);
		
		for(Term term:terms){
			//System.out.println(reader.totalTermFreq(term));
			//queryTermProbs.put(term,getTokenCounts(term));//System.out.println(term.text());
			/*
			double[] counts = getTokenCounts(term);
			for(int i=0;i<counts.length;i++){
				 counts[i] /= docFreqs[i];
				 queryProbs[i] *= counts[i];
			 }
			 */
			Map<Integer,Double> termFreqInDocs = getTokenCounts(term,ir);
			for(int doc:termFreqInDocs.keySet()){
				if(queryProbsMap.keySet().contains(doc)) queryProbsMap.put(doc, queryProbsMap.get(doc)*termFreqInDocs.get(doc));
				else queryProbsMap.put(doc,termFreqInDocs.get(doc));
		    }
		}
		
		double q_denom = 0.0;
		//for(int i=0;i<queryProbs.length;i++)q_denom +=queryProbs[i];
		
		if(expandQuery){
			docFreqs_ext = getDocFrequencies(reader_ext);
			for(int doc:queryProbsMap.keySet()){
				queryProbsMap.put(doc, queryProbsMap.get(doc)/(Math.pow(docFreqs_ext[doc],terms.size())));
		    }
			for(int i = 0;i<reader_ext.numDocs();i++){
				if(queryProbsMap.keySet().contains(i))q_denom += queryProbsMap.get(i);
				else q_denom += 1/(Math.pow(docFreqs_ext[i],terms.size()));
			}
		}
		else{
			for(int doc:queryProbsMap.keySet()){
				queryProbsMap.put(doc, queryProbsMap.get(doc)/(Math.pow(docFreqs[doc],terms.size())));
		    }
			for(int i = 0;i<reader.numDocs();i++){
				if(queryProbsMap.keySet().contains(i))q_denom += queryProbsMap.get(i);
				else q_denom += 1/(Math.pow(docFreqs[i],terms.size()));
			}
		}
				
		//Query Related terms have been calculated till this point
		//Need the vocab related terms now		
		
		//new way by using additional space
		//Iterate over Vocab
		if(useWholeCollection){
			Bits liveDocs = MultiFields.getLiveDocs(reader);	
			
			docProbs = new double[reader.numDocs()];//P(Q|d)
			
			TermsEnum termEnum = MultiFields.getTerms(reader, "contents").iterator(null);
			//TermsEnum termEnum_ext = MultiFields.getTerms(reader_ext, "contents").iterator(null);
			BytesRef bytesRef;		
	        while ((bytesRef = termEnum.next()) != null){
	        	double totalFreqRatio = reader.totalTermFreq(new Term("contents",bytesRef));
	        	//System.out.println(new Term("contents",bytesRef).text());
	        	//System.out.println(totalFreqRatio);
	        	//System.out.println(reader_ext.totalTermFreq(new Term("contents",bytesRef)));
	        	
	        	totalFreqRatio /= reader.getSumTotalTermFreq("contents");
	        	double smoothingCollectionTerm = (1-lambda)*(totalFreqRatio);
	        	
	        	//Get the word frequency for each document in the collection
	        	DocsEnum docsEnum = termEnum.docs(liveDocs, null);
	            double[] wordCounts = new double[reader.numDocs()];
	            //Arrays.fill(wordCounts, 1.0);
	            
	        	if (docsEnum != null) {
	                int doc;
	                while ((doc = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
	                	wordCounts[doc] += docsEnum.freq(); 
	                }
	            }
	        	
	        	//Calculate the probability of word given query
	        	double v_q = 0.0;
	        	
	            if(!expandQuery){	  	            
		            for(int i=0;i<wordCounts.length;i++){
		            	
		            	double smoothedCount = lambda*(wordCounts[i]/docFreqs[i]) + smoothingCollectionTerm;
		            	if(queryProbsMap.keySet().contains(i)){
		            		v_q += (smoothedCount)*queryProbsMap.get(i);
		            	}
		            	else v_q += (smoothedCount)*(1/(Math.pow(docFreqs[i],terms.size())));
		            }
		            //System.out.println("v_q="+v_q);		            
		            
	            }
	            else{	//***expandQuery==true***
	            	
	            	//Get the word frequency for each document in the External Corpus
	            	Bits liveDocs_ext = MultiFields.getLiveDocs(reader_ext);
		        	double[] wordCounts_ext = new double[reader_ext.numDocs()];
		        	TermsEnum termEnum_ext = MultiFields.getTerms(reader_ext, "contents").iterator(null);
		        	if (termEnum_ext.seekExact(bytesRef)==true){
		        		DocsEnum docsEnum_ext = termEnum_ext.docs(liveDocs_ext, null);
		        		if (docsEnum_ext != null) {
		        			int doc;
			                while ((doc = docsEnum_ext.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
			                	wordCounts_ext[doc] += docsEnum_ext.freq(); 
			                }
		        		}
		        	}
		        	
		        	// Calculating the smoothing term for External Corpus
		        	double totalFreqRatio_ext = reader_ext.totalTermFreq(new Term("contents",bytesRef));
		        	totalFreqRatio_ext /= reader_ext.getSumTotalTermFreq("contents");
		        	double smoothingCollectionTerm_ext = (1-lambda)*(totalFreqRatio_ext);
		        			        	
		        	for(int i=0;i<wordCounts_ext.length;i++){
		            	
		            	double smoothedCount = lambda*(wordCounts_ext[i]/docFreqs_ext[i]) + smoothingCollectionTerm_ext;
		            	if(queryProbsMap.keySet().contains(i)){
		            		v_q += (smoothedCount)*queryProbsMap.get(i);
		            	}
		            	else v_q += (smoothedCount)*(1/(Math.pow(docFreqs_ext[i],terms.size())));
		            }
		            //System.out.println("v_q="+v_q);
		            
		            		            
	            }
	            v_q /= q_denom;
	            //System.out.println("v_q after div="+v_q);
	            //System.out.println("smoothing collection term"+smoothingCollectionTerm);
	            //Update the probabilities of query given each doc 
	            for(int i=0;i<docProbs.length;i++){            	            	
	            	//docProbs[i] += (v_q*(Math.log(wordCounts[i])-Math.log(docFreqs[i])));
	            	double smoothedCount = lambda*(wordCounts[i]/docFreqs[i]) + smoothingCollectionTerm;
	            	docProbs[i] +=  v_q*(Math.log(smoothedCount));
	            }  
	        }
		}		
		else{
			Bits liveDocs = MultiFields.getLiveDocs(reader);			
			docProbs = new double[reader.numDocs()];//P(Q|d)
			
			Set<BytesRef> vocab = new HashSet<BytesRef>();
		
			for(int ii=0;ii<LuceneDocs.size();ii++){
				int docIndex = LuceneDocs.get(ii);
				Terms termVector = reader.getTermVector(docIndex, "contents");
			    TermsEnum itr = termVector.iterator(null);
			    BytesRef bytesRef = null;
			    
			    while ((bytesRef = itr.next()) != null) {   
			    	if(vocab.contains(bytesRef)){
			    		continue;
			    	}
			    	
			        vocab.add(bytesRef);
			        //System.out.println(bytesRef.utf8ToString());
			    	double totalFreqRatio = reader.totalTermFreq(new Term("contents",bytesRef));
		        	totalFreqRatio /= reader.getSumTotalTermFreq("contents");
		        	//Get the word frequency for each document
		        	DocsEnum docsEnum = MultiFields.getTermDocsEnum(reader, liveDocs, "contents", bytesRef);
		            double[] wordCounts = new double[reader.numDocs()];
		            if (docsEnum != null) {
		                int doc;
		                while ((doc = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
		                	wordCounts[doc] += docsEnum.freq(); 
		                	//System.out.println(doc);
		                }
		            }
		           
		            
		            //Calculate the probability of word given query
		            double v_q = 0.0;
		            double smoothingCollectionTerm = (1-lambda)*(totalFreqRatio);
		            for(int i=0;i<wordCounts.length;i++){
		            	
		            	double smoothedCount = lambda*(wordCounts[i]/docFreqs[i]) + smoothingCollectionTerm;
		            	if(queryProbsMap.keySet().contains(i)){
		            		v_q += (smoothedCount)*queryProbsMap.get(i);
		            	}
		            	else v_q += (smoothedCount)*(1/(Math.pow(docFreqs[i],terms.size())));
		            }
		            //System.out.println("v_q="+v_q);
		            v_q /= q_denom;
		            //System.out.println("v_q after div="+v_q);
		            //System.out.println("smoothing collection term"+smoothingCollectionTerm);
		            
		            //Update the probabilities of query given each doc 
		            for(int i=0;i<docProbs.length;i++){            	            	
		            	//docProbs[i] += (v_q*(Math.log(wordCounts[i])-Math.log(docFreqs[i])));
		            	double smoothedCount = lambda*(wordCounts[i]/docFreqs[i]) + smoothingCollectionTerm;
		            	docProbs[i] +=  v_q*(Math.log(smoothedCount));
		            }
		            //break;
			    }
			}
		}
		/*
		System.out.println("Final P(Q|d): ");
		for(int i=0;i<docProbs.length;i++){
			System.out.println((i+1)+". "+reader.document(i).getValues("title")[0]+" : "+docProbs[i]);
		 }
		 */
	}
	
	/**
	 * Returns a list of the titles of top n documents in the index 
	 * @param n : The number of top documents required
	 * @return List of titles of top documents
	 * @throws IOException 
	 */
	public List<Integer> getTopNDocs(int n) throws IOException{
		List<Integer> topDocs = new ArrayList<Integer>();
		docScore[] docScores = new docScore[docProbs.length];
		for (int i=0;i<docProbs.length;i++){
			docScores[i] = new docScore(i,docProbs[i]);			
		}
		ScoreComparator scoreComp = new ScoreComparator();
		PriorityQueue<docScore> docHeap = new PriorityQueue<docScore>(reader.numDocs(),scoreComp);
		for (int i=0;i<docProbs.length;i++) {
			docHeap.offer(docScores[i]);
			//System.out.println("");
			//for(docScore yo:docHeap)System.out.print(yo.docNum+1 +",");
		}
		//System.out.println("");
		for(int i=0; i<n ; i++){
			//topDocs.add(reader.document(docHeap.poll()).getValues("title")[0]);
			topDocs.add(docHeap.poll().docNum);
			//System.out.println("");
			//for(docScore yo:docHeap)System.out.print(yo.docNum+1 +",");
		}
		
		return topDocs;
	}
	
	public Map<Integer,Double> getTokenCounts(Term term,IndexReader ir) throws IOException{
		Bits liveDocs = MultiFields.getLiveDocs(ir);
	    Fields fields = MultiFields.getFields(ir);
	    
	    double[] wordCounts = new double[ir.numDocs()];
		Arrays.fill(wordCounts, 1.0);
		
		//Since only a small number of documents will have the query term,
		//having the whole array does not make sense (very sparse)
		//Store counts only for the documents in which term is present
		// <DocId to Count>
		Map<Integer,Double> termFreqInDocs = new HashMap<Integer,Double>(); 
		
	    for (String field : fields) {	    	
			TermsEnum termEnum = MultiFields.getTerms(ir, field).iterator(null);
			BytesRef bytesRef = term.bytes();
			
			if (termEnum.seekExact(bytesRef)==true) {
				
                DocsEnum docsEnum = termEnum.docs(liveDocs, null);                
                if (docsEnum != null) {
                    int doc;
                    while ((doc = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                    	wordCounts[doc] += docsEnum.freq(); 
                    	termFreqInDocs.put(doc, (double) docsEnum.freq());
                    }
                }                
            }
			
		}
	    /*
	    for(int doc:termFreqInDocs.keySet()){
	    	System.out.println(doc+":"+termFreqInDocs.get(doc));
	    }
	    */
	    //return wordCounts;
	    return termFreqInDocs;
	}
	public int[] getDocFrequencies(IndexReader ir) throws IOException{
		
		int[] docFreqs = new int[ir.numDocs()];
		Fields fields = MultiFields.getFields(ir);
		
		for (String field : fields){
			for(int i=0;i<docFreqs.length;i++){
				
				Document doc = ir.document(i);
				//System.out.println(doc.get("title"));
				Terms termVector = ir.getTermVector(i, field);
			    TermsEnum itr = termVector.iterator(null);
			    BytesRef term = null;
			    
			    while ((term = itr.next()) != null) {   
			        docFreqs[i] += itr.totalTermFreq();   
			    }
			}
		}
		return docFreqs;
	}
	
	public class ScoreComparator implements Comparator<docScore>{

		@Override
		public int compare(docScore o1, docScore o2) {
			// TODO Auto-generated method stub
			return o2.score.compareTo(o1.score);
		}
		
		
	}
	public class docScore{
		int docNum;
		Double score;
		
		public docScore(int d,Double s){
			docNum = d;
			score = s;
		}
	}

}
