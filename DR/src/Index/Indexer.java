package Index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import Parsing.Parser;
import RelevanceModel.RankingFunction;

public class Indexer {
	public static void main(String[] args) throws IOException, ParseException{
		//Indexing
		StandardAnalyzer analyzer = new StandardAnalyzer();
	    
	    Directory index = new RAMDirectory();
	    IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
	    IndexWriter w = new IndexWriter(index, config);
	    String dir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/";
	    /*
	    File folder = new File(dir);
	    for (File fileEntry : folder.listFiles()) {	       
	        //System.out.println(fileEntry.getName());
	        String fileName = dir+fileEntry.getName();
	        //System.out.println(fileName);
			w.addDocument(Parser.getText(fileName));
	    }
	    */
	    
	    String fileName = dir+"CSCI570_2014140920140122.dat";//"CSCI570_2014140920140122.dat";
	    w.addDocuments(Parser.getText(fileName));
		//w.addDocument(Parser.getText(fileName));
		
	    w.close();
	    
	    //Query
	    String queryStr = "what is breadth first search";
	    Query q = new QueryParser("contents", analyzer).parse(queryStr);
	    
	    long startTime = System.currentTimeMillis();
  
	    RankingFunction ranker = new RankingFunction(index,0.7);
	    ranker.getQueryProbability(q);
	    
	    long endTime   = System.currentTimeMillis();
	    long totalTime = endTime - startTime;
	    System.out.println("TIME: "+totalTime);
	    //Search
	    int hitsPerPage = 10;
	    IndexReader reader = DirectoryReader.open(index);	    
	    IndexSearcher searcher = new IndexSearcher(reader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    //Output
	    System.out.println("\nLucene:Found " + hits.length + " hits.");
	    for(int i=0;i<hits.length;++i) {
			  int docId = hits[i].doc;
			  Document d = searcher.doc(docId);
			  System.out.println((i + 1) + ". " + d.get("title"));
	    }
	    
	    reader.close();
	}
}
