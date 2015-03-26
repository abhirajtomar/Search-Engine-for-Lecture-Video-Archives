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
import java.util.Scanner;
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
	static String instructions;
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
	    String folderName = "segments";
	    File folder = new File(dir+folderName+"/");
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
	        Document doc = generateDoc(fileEntry.getName(),folderName);
			if(doc!=null)w.addDocument(doc);
	    }
		
	    w.close();
	    
	    //Indexing the external corpus for query expansion
	    Directory index_ext = new RAMDirectory();
	    IndexWriterConfig config_ext = new IndexWriterConfig(Version.LATEST, analyzer);
	    w = new IndexWriter(index_ext, config_ext);
	    
	    folder = new File(dir+"Cormen"+"/");
	    for (File fileEntry : folder.listFiles()) {	       
	    	FileInputStream fis = new FileInputStream(fileEntry);
	    	byte[] data = new byte[(int) fileEntry.length()];
	    	fis.read(data);
	    	fis.close();

	    	String str = new String(data, "UTF-8");
	    	
	    	Document doc = new Document();			
	        FieldType options = new FieldType();
			options.setIndexed(true); 
			options.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS); 
			options.setStored(true); 
			options.setStoreTermVectors(true); 
			options.setTokenized(true);
			
			doc.add(new Field("title", fileEntry.getName(), options));
			doc.add(new Field("contents", str, options));
			
			if(doc!=null)w.addDocument(doc);
	    }		
	    w.close();
	    
	    
	    //Reading the instructions file
	    instructions = new Scanner(new File(dir+"instructions.txt")).useDelimiter("\\Z").next();
	    
	    //Query
	    String queryFile = dir+"queries.txt";
	    BufferedReader br = new BufferedReader(new FileReader(queryFile));
	    //String queryStr;// = "breadth first search";
	    
	    List<String[]> queryInfo = new ArrayList<String[]>();
	    String qReader;
	    while ((qReader = br.readLine()) != null){
	    	String [] newQuery = new String[4];
	    	newQuery[0] = qReader;
	    	newQuery[1] = br.readLine();
	    	newQuery[2] = br.readLine();
	    	newQuery[3] = br.readLine();
	    	queryInfo.add(newQuery);
	    }
	    
	    IndexReader reader = DirectoryReader.open(index);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    String title = "surveyQualtrics.txt";
	    PrintWriter writer = new PrintWriter(dir+"survey/"+title, "ISO-8859-1");
	    writer.println("[[AdvancedFormat]]\n");
	    
		for(String[] query : queryInfo){
			String queryStr = query[0];
		    Query q = new QueryParser("contents", analyzer).parse(queryStr);		    
		    
		    //Search
		    int hitsPerPage = 10;		    
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
		    
		    long startTime = System.currentTimeMillis();
		    
		    //RankingFunction ranker = new RankingFunction(index,0.7);
		    //RankingFunction ranker = new RankingFunction(index,0.4,topDocs_L);
		    RankingFunction ranker = new RankingFunction(index,index_ext,0.7);
		    ranker.getQueryProbability(q);
		    
		    long endTime   = System.currentTimeMillis();
		    long totalTime = endTime - startTime;
		    System.out.println("TIME: "+totalTime);
		    
		    
		    List<Integer> topDocs_RM = ranker.getTopNDocs(10);
		    
		    
		    System.out.println("\nRelevance Model Estimation:");
		    for(int i = 1;i<=10;i++){	    
		    	Document docn = reader.document(topDocs_RM.get(i-1));
		    	System.out.println(i+". "+docn.get("title"));
		    	//System.out.println(docn.get("contents").replace("\n"," "));
		    }
		    
		    Set<Integer> unionSet = new HashSet<Integer>();
		    unionSet.addAll(topDocs_RM);
		    unionSet.addAll(topDocs_L);
		    List<Integer> topDocs = new ArrayList<Integer>(unionSet);
		    writeToSurveyFile(query,topDocs,reader,writer);
	    }	    
	    //writeToSurveyFile(queryStr,topDocs,reader);
	    //generateOutputFile(topDocs_RM,reader,"RM_docs.txt");
	    //generateOutputFile(topDocs_L,reader,"L_docs.txt");
	    writer.close();
	    reader.close();
	}
	
	private static void generateOutputFile(List<Integer> docs,IndexReader reader, String title) throws IOException{
		PrintWriter writer = new PrintWriter(dir+"survey/"+title, "ISO-8859-1");	
	    StringBuilder sb =  new StringBuilder();
	    for(int i = 0; i < docs.size(); i++){
	    	sb.append((i+1)+"\n");
	    	sb.append(getDocText(reader,docs.get(i)));
	    	sb.append("\n");
	    }
	    
	    writer.println(sb.toString());
		writer.close();
	}
	
	private static void writeToSurveyFile(String[] query,
			List<Integer> topDocs,IndexReader reader,PrintWriter writer) throws IOException, XMLStreamException {
		
			
	    StringBuilder sb =  new StringBuilder();
	    int qnum = 1;
	    //sb.append("[[AdvancedFormat]]\n");
	    int i = 0;
	    while(i<topDocs.size()){
	    	if(i%5==0){
	    		sb.append("\n[[Question:Matrix]]");
		    	sb.append("\n"+qnum+". ");
		    	sb.append(instructions+"\n\n");
		    	sb.append("<br /><br /><strong>Query:</strong>"+query[0]+"\n");
		    	sb.append("<div><br><ul><li><strong>Relevant Segments:</strong>"+query[1]+"</li>\n");
		    	sb.append("<li><strong>Partially Relevant Segments:</strong>"+query[2]+"</li>\n");
		    	sb.append("<li><strong>Irrelevant Segments:</strong>"+query[3]+"</li></ul></div>\n\n");
		    		    	
		    	sb.append("[[AdvancedChoices]]\n");
	    	}
	    	sb.append(getDocText(reader,topDocs.get(i)));
	    	//sb.append(getTwinDocText(reader,topDocs.get(i)));
	    	i++;
	    	if(i%5==0){
	    		sb.append("\n[[AdvancedAnswers]]\n[[Answer]]\nRelevant\n[[Answer]]\nPartially Relevant\n[[Answer]]\nIrrelevant\n");
		    	qnum++;
	    	}
	    }
	    if(i%5!=0){
    		sb.append("\n[[AdvancedAnswers]]\n[[Answer]]\nRelevant\n[[Answer]]\nPartially Relevant\n[[Answer]]\nIrrelevant\n");
    	}
	    
	    writer.println(sb.toString());
		
	}

	private static String getDocText(IndexReader reader, int i) throws IOException {
		Document doc = reader.document(i);
		String text = "[[Choice]]\n<div style=\"width:500px\"><span style=\"visibility: hidden\">"+doc.get("title")+":</span><br />\n"+doc.get("contents").replace("\n", " ")+"</div>";
		return text+"\n";
	}
	private static String getTwinDocText(IndexReader reader,int i) throws IOException, XMLStreamException{
		Document doc = reader.document(i);
		Document twinDoc = generateDoc(doc.get("title"),"segments");
		String text = "[[Choice]]\n<div style=\"width:500px\"><span style=\"visibility: hidden\">"+doc.get("title")+":</span><br />\n"+twinDoc.get("contents").replace("\n", " ")+"</div>";
		return text+"\n";
	}
	private static Document generateDoc(String file, String fileFolder) throws FileNotFoundException, XMLStreamException {
		Document doc = new Document();
		String fileName = dir+fileFolder+"/"+file+"/";
        //System.out.println(fileFolder+"/"+file);
		
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
		if(text=="")return null;
		doc.add(new Field("title", file, options));
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
