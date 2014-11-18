package Parsing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
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

import Segmentation.ASR_TSF;
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
			List<String[]> startEndTimes = new ArrayList<String[]>();
			QName startAttr = new QName("begin");
			QName durAttr = new QName("dur");
			String [] times;
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
							String startTime = startElement.getAttributeByName(startAttr).getValue();
							String duration = startElement.getAttributeByName(durAttr).getValue();
							startTime = getStartTimeInSec(startTime);
							//System.out.println(startTime +", "+duration );
							
							times = new String[2];
							times[0] = startTime;
							times[1] = String.valueOf(Double.parseDouble(startTime)+Double.parseDouble(duration));
							startEndTimes.add(times);
						}
					}
				}				
			}
			double offset = Double.parseDouble(startEndTimes.get(0)[0]);
			for(int i = 0;i<startEndTimes.size();i++){
				startEndTimes.get(i)[0] = String.valueOf(Double.parseDouble(startEndTimes.get(i)[0])-offset);
				startEndTimes.get(i)[1] = String.valueOf(Double.parseDouble(startEndTimes.get(i)[1])-offset);				
			}
			//TSF segmenter = new TSF(sentences,0.35,40);
			//List<String> segments = segmenter.getSegments();
			ASR_TSF segmenter = new ASR_TSF(sentences,startEndTimes,0.35,40);
			List<String> segments = segmenter.getSegments();
			List<String[]> segmentTimes = segmenter.getSegmentTimes();
			
			for(int i = 0 ;i<segments.size();i++)System.out.println(segmentTimes.get(i)[0]+"\n"+segmentTimes.get(i)[1]+"\n"+segments.get(i));			
			
			for(int i=0;i<segments.size();i++){
				String segment = segments.get(i);
				String title = (i+1)+"_"+ fileName.substring( fileName.lastIndexOf('/')+1, fileName.length());
				
				PrintWriter writer = new PrintWriter(dir+title, "ISO-8859-1");
				writer.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
				writer.println("<body>");
				writer.println("<startTime>"+segmentTimes.get(i)[0]+"</startTime>");	
				writer.println("<endTime>"+segmentTimes.get(i)[1]+"</endTime>");
				writer.println("<text>"+segment+"</text>");	
				writer.println("</body>");
				writer.close();
				System.out.println(segment);
			}
			
			System.out.println("Total Segments: "+segments.size());	
						
		} catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
		
		return;
	}

	private static String getStartTimeInSec(String startTime) {
		String[] temp = startTime.split(":");
		Double hrs = Double.parseDouble(temp[0]);
		Double mins = Double.parseDouble(temp[1]);
		Double secs = Double.parseDouble(temp[2]);
		
		return String.valueOf(hrs*3600+mins*60 + secs);
	}
}
