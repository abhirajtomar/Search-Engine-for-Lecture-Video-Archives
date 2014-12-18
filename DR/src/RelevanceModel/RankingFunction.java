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
	private double lambda;
	private double[] docProbs;
	private boolean useWholeCollection;
	private List<Integer> LuceneDocs;
	
	public RankingFunction(Directory index, double lambda) throws IOException{
		this.index=index;
		this.lambda = lambda;
		reader = DirectoryReader.open(this.index);	
		useWholeCollection = true;
		this.LuceneDocs = null;
	}
	
	public RankingFunction(Directory index, double lambda,List<Integer> LuceneDocs) throws IOException{
		this.index=index;
		this.lambda = lambda;
		reader = DirectoryReader.open(this.index);	
		useWholeCollection = false;
		this.LuceneDocs = LuceneDocs;
	}
	
	public void getQueryProbability(Query q) throws IOException{
		int[] docFreqs = getDocFrequencies();
		
		Set<Term> terms = new HashSet<Term>();
		//Map<Term,double[]> queryTermProbs = new HashMap<Term,double[]>();
		q.extractTerms(terms);
		
		double [] queryProbs = new double[reader.numDocs()];
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
			Map<Integer,Double> termFreqInDocs = getTokenCounts(term);
			for(int doc:termFreqInDocs.keySet()){
				if(queryProbsMap.keySet().contains(doc)) queryProbsMap.put(doc, queryProbsMap.get(doc)*termFreqInDocs.get(doc));
				else queryProbsMap.put(doc,termFreqInDocs.get(doc));
		    }
		}
		
		for(int doc:queryProbsMap.keySet()){
			queryProbsMap.put(doc, queryProbsMap.get(doc)/(Math.pow(docFreqs[doc],terms.size())));
	    }
		
		
		/*
		for(double[] counts:queryTermProbs.values()){			
			 for(int i=0;i<counts.length;i++){
				 counts[i] /= docFreqs[i];
				 queryProbs[i] *= counts[i];
			 }
		}
		*/
		/*
				
		for(int i=0;i<queryProbs.length;i++){
			System.out.println("Doc "+i+" : "+queryProbs[i]);
		 }
		 */
		
		//Query Related terms have been calculated till this point
		//Need the vocab related terms now
		double q_denom = 0.0;
		//for(int i=0;i<queryProbs.length;i++)q_denom +=queryProbs[i];
		for(int i = 0;i<reader.numDocs();i++){
			if(queryProbsMap.keySet().contains(i))q_denom += queryProbsMap.get(i);
			else q_denom += 1/(Math.pow(docFreqs[i],terms.size()));
		}
				
		Bits liveDocs = MultiFields.getLiveDocs(reader);
		
		docProbs = new double[reader.numDocs()];//P(Q|d)
		
		//new way by using additional space
		//Iterate over Vocab
		if(useWholeCollection){
			TermsEnum termEnum = MultiFields.getTerms(reader, "contents").iterator(null);
			BytesRef bytesRef;		
	        while ((bytesRef = termEnum.next()) != null){
	        	double totalFreqRatio = reader.totalTermFreq(new Term("contents",bytesRef));
	        	totalFreqRatio /= reader.getSumTotalTermFreq("contents");
	        	//Get the word frequency for each document
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
		else{
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
	
	public Map<Integer,Double> getTokenCounts(Term term) throws IOException{
		Bits liveDocs = MultiFields.getLiveDocs(reader);
	    Fields fields = MultiFields.getFields(reader);
	    
	    double[] wordCounts = new double[reader.numDocs()];
		Arrays.fill(wordCounts, 1.0);
		
		//Since only a small number of documents will have the query term,
		//having the whole array does not make sense (very sparse)
		//Store counts only for the documents in which term is present
		// <DocId to Count>
		Map<Integer,Double> termFreqInDocs = new HashMap<Integer,Double>(); 
		
	    for (String field : fields) {	    	
			TermsEnum termEnum = MultiFields.getTerms(reader, field).iterator(null);
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
	public int[] getDocFrequencies() throws IOException{
		
		int[] docFreqs = new int[reader.numDocs()];
		Fields fields = MultiFields.getFields(reader);
		
		for (String field : fields){
			for(int i=0;i<docFreqs.length;i++){
				
				Document doc = reader.document(i);
				//System.out.println(doc.get("title"));
				Terms termVector = reader.getTermVector(i, field);
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
