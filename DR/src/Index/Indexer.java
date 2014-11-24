package Index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo.IndexOptions;
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

import RelevanceModel.RankingFunction;

public class Indexer {
	static String dir;
	public static void main(String[] args) throws IOException, ParseException, XMLStreamException{
		//Indexing
		StandardAnalyzer analyzer = new StandardAnalyzer();
	    
	    Directory index = new RAMDirectory();
	    IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
	    IndexWriter w = new IndexWriter(index, config);
	    dir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/";
	    /*
	    File folder = new File(dir);
	    for (File fileEntry : folder.listFiles()) {	       
	        //System.out.println(fileEntry.getName());
	        String fileName = dir+fileEntry.getName();
	        //System.out.println(fileName);
			w.addDocument(Parser.getText(fileName));
	    }
	    */
	    
	    //String fileName = dir+"CSCI570_2014140920140122.dat";//"CSCI570_2014140920140122.dat";
	    //w.addDocuments(Parser.getText(fileName));
		//w.addDocument(Parser.getText(fileName));
	    File folder = new File(dir+"segments/");
	    for (File fileEntry : folder.listFiles()) {	       
	        //String fileName = dir+"segments/"+fileEntry.getName()+"/";
	        		
	        //String text = getFileText(fileName);
	        //System.out.println(fileEntry.getName());
	        //System.out.println(text);
	        /*
	        Document doc = new Document();
	        doc.add(new Field("title", fileEntry.getName(), options));
			doc.add(new Field("contents", text, options));
			*/
	        Document doc = generateDoc(fileEntry);
			w.addDocument(doc);
	    }
		
	    w.close();
	    
	    //Query
	    String queryStr = "breadth first search";
	    Query q = new QueryParser("contents", analyzer).parse(queryStr);
	    
	    long startTime = System.currentTimeMillis();
  
	    RankingFunction ranker = new RankingFunction(index,0.7);
	    ranker.getQueryProbability(q);
	    
	    long endTime   = System.currentTimeMillis();
	    long totalTime = endTime - startTime;
	    System.out.println("TIME: "+totalTime);
	    
	    IndexReader reader = DirectoryReader.open(index);
	    List<Integer> topDocs_RM = ranker.getTopNDocs(10);
	    
	    
	    System.out.println("\nRelevance Model Estimation:");
	    for(int i = 1;i<=10;i++){	    
	    	Document docn = reader.document(topDocs_RM.get(i-1));
	    	System.out.println(i+". "+docn.get("title"));
	    	//System.out.println(docn.get("contents").replace("\n"," "));
	    }
	    
	    //Search
	    int hitsPerPage = 10;
	    //IndexReader reader = DirectoryReader.open(index);	    
	    IndexSearcher searcher = new IndexSearcher(reader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    //Output
	    System.out.println("\nLucene:Found " + hits.length + " hits.");
	    List<Integer> topDocs_L = new ArrayList<Integer>();
	    for(int i=0;i<hits.length;++i) {
			  int docId = hits[i].doc;
			  topDocs_L.add(docId);
			  Document d = searcher.doc(docId);
			  System.out.println((i + 1) + ". " + d.get("title"));
			  //System.out.println(d.get("contents"));
	    }
	    
	    Set<Integer> unionSet = new HashSet<Integer>();
	    unionSet.addAll(topDocs_RM);
	    unionSet.addAll(topDocs_L);
	    List<Integer> topDocs = new ArrayList<Integer>(unionSet);
	    	    
	    generateSurveyFile(queryStr,topDocs,reader);
	    
	    reader.close();
	}
	
	private static void generateSurveyFile(String queryStr,
			List<Integer> topDocs,IndexReader reader) throws IOException {
		
		String title = "surveyQualtrics.txt";
	    PrintWriter writer = new PrintWriter(dir+"survey/"+title, "ISO-8859-1");	
	    StringBuilder sb =  new StringBuilder();
	    int qnum = 1;
	    sb.append("[[AdvancedFormat]]\n");
	    int i = 0;
	    while(i<topDocs.size()){
	    	if(i%5==0){
	    		sb.append("\n[[Question:Matrix]]");
		    	sb.append("\n"+qnum+". Q_RM.");
		    	sb.append(" Query:"+queryStr+"\n");	    	
		    	sb.append("[[Choices]]\n");
	    	}
	    	sb.append(getDocText(reader,topDocs.get(i)));
	    	i++;
	    	if(i%5==0){
	    		sb.append("\n[[AdvancedAnswers]]\n[[Answer]]\nRelevant\n[[Answer]]\nPartially Relevant\n[[Answer]]\nIrrelevant\n");
		    	qnum++;
	    	}
	    }
	    if(i%5!=0){
    		sb.append("\n[[AdvancedAnswers]]\n[[Answer]]\nRelevant\n[[Answer]]\nPartially Relevant\n[[Answer]]\nIrrelevant\n");
    	}
	    /*
	    for(int i=1;i<10;i=i+5){
	    	sb.append("\n[[Question:Matrix]]");
	    	sb.append("\n"+qnum+". Q_RM.");
	    	sb.append(" Query:"+queryStr+"\n");	    	
	    	sb.append("[[Choices]]\n");
	    	sb.append(getDocText(reader,topDocs.get(i-1)));
	    	sb.append(getDocText(reader,topDocs.get(i)));
	    	sb.append(getDocText(reader,topDocs.get(i+1)));
	    	sb.append(getDocText(reader,topDocs.get(i+2)));
	    	sb.append(getDocText(reader,topDocs.get(i+3)));
	    	
	    	sb.append("\n[[AdvancedAnswers]]\n[[Answer]]\nRelevant\n[[Answer]]\nPartially Relevant\n[[Answer]]\nIrrelevant\n");
	    	qnum++;
	    }
	    
	    for(int i=0;i<hits.length;i=i+5){
	    	sb.append("\n[[Question:Matrix]]");
	    	//System.out.println(i+". "+docn.get("title"));
	    	sb.append("\n"+qnum+". Q_L.");
	    	sb.append(" Query:"+queryStr+"\n");
	    	sb.append("[[Choices]]\n");
	    	sb.append(getDocText(reader,hits[i].doc));
	    	sb.append(getDocText(reader,hits[i+1].doc));
	    	sb.append(getDocText(reader,hits[i+2].doc));
	    	sb.append(getDocText(reader,hits[i+3].doc));
	    	sb.append(getDocText(reader,hits[i+4].doc));
	    	
	    	sb.append("\n[[AdvancedAnswers]]\n[[Answer]]\nRelevant\n[[Answer]]\nPartially Relevant\n[[Answer]]\nIrrelevant\n");
	    	qnum++;
	    }
	    */
	    writer.println(sb.toString());
		writer.close();
	}

	private static Object getDocText(IndexReader reader, int i) throws IOException {
		Document doc = reader.document(i);
		String text = doc.get("title")+"Text:"+doc.get("contents").replace("\n", " ");
		return text+"\n";
	}

	private static Document generateDoc(File fileEntry) throws FileNotFoundException, XMLStreamException {
		Document doc = new Document();
		String fileName = dir+"segments/"+fileEntry.getName()+"/";
        
        FieldType options = new FieldType();
		options.setIndexed(true); 
		options.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS); 
		options.setStored(true); 
		options.setStoreTermVectors(true); 
		options.setTokenized(true);
		String startTime="0.0";
		String endTime="0.0";
		String text = "";
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		
		XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName), "ISO-8859-1");
		while(xmlEventReader.hasNext()){
			XMLEvent xmlEvent = xmlEventReader.nextEvent();
			if (xmlEvent.isStartElement()){
				StartElement startElement = xmlEvent.asStartElement();
				if(startElement.getName().getLocalPart().equals("startTime")){
					xmlEvent = xmlEventReader.nextEvent();
					if(!xmlEvent.isEndElement()){
						startTime = xmlEvent.asCharacters().getData();
					}					
				}
				else if(startElement.getName().getLocalPart().equals("endTime")){
					xmlEvent = xmlEventReader.nextEvent();
					if(!xmlEvent.isEndElement()){
						endTime = xmlEvent.asCharacters().getData();
					}					
				}
				else if(startElement.getName().getLocalPart().equals("text")){
					text = "";
					xmlEvent = xmlEventReader.nextEvent();
					//if(!xmlEvent.isEndElement()){
					while(xmlEvent.isCharacters()){
						text += xmlEvent.asCharacters().getData();
						xmlEvent = xmlEventReader.nextEvent();
					}					
				}
			}		
		}
		
		doc.add(new Field("title", fileEntry.getName(), options));
		doc.add(new Field("contents", text, options));
		//options.setIndexed(false);
		doc.add(new Field("startTime",startTime,options));
		doc.add(new Field("endTime",endTime,options));
		
		return doc;
	}

	/**
	 * Method takes the name of a file and returns all its content as a single string
	 * @param fileName
	 * @return
	 * @throws IOException 
	 */
	private static String getFileText(String fileName) throws IOException {
		  BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
		 
		  StringBuffer stringBuffer = new StringBuffer();
		  String line = null;
		 
		  while((line =bufferedReader.readLine())!=null){
		 
		   stringBuffer.append(line).append("\n");
		  }
		   
		  //System.out.println(stringBuffer);
		  return stringBuffer.toString();
	}
}
