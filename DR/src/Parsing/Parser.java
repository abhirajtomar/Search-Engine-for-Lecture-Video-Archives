package Parsing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo.IndexOptions;

import Segmentation.TSF;

public class Parser {
	
	public static void main(String[] args) throws UnsupportedEncodingException{
		String dir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/";	   
	    String fileName = dir+"CSCI570_2014140920140122.dat";//"CSCI570_2014140920140122.dat";
		getText(fileName);
	}
	
	public static void getText(String fileName) throws UnsupportedEncodingException{
			
	    String dir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/segments/";

		StringBuilder text = new StringBuilder();		
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		
		try{
			//XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName), "ISO-8859-1");

			List<String> sentences = new ArrayList<String>();
			
			while(xmlEventReader.hasNext()){
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()){
					StartElement startElement = xmlEvent.asStartElement();
					if(startElement.getName().getLocalPart().equals("p")){
						xmlEvent = xmlEventReader.nextEvent();
						if(!xmlEvent.isEndElement()){
							//System.out.println(xmlEvent.asCharacters().getData());
							text.append(xmlEvent.asCharacters().getData());
							sentences.add(xmlEvent.asCharacters().getData());
						}
					}
				}				
			}
			
			TSF segmenter = new TSF(sentences,0.35,40);
			List<String> segments = segmenter.getSegments();
						
			for(int i=0;i<segments.size();i++){
				String segment = segments.get(i);
				String title = (i+1)+"_"+ fileName.substring( fileName.lastIndexOf('/')+1, fileName.length());
				
				PrintWriter writer = new PrintWriter(dir+title, "ISO-8859-1");
				writer.println(segment);				
				writer.close();
				
			}
						
		} catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
		
		return;
	}
}
