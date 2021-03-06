package Parsing;

import java.io.File;
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

import Segmentation.SegmentMatcher;

public class Parser {
	
	public static void main(String[] args) throws UnsupportedEncodingException{
		String dir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/";	   
	    //String fileName = dir+"transcripts/CSCI570_2014195320140305.dat";//"CSCI570_2014140920140122.dat";
	    //fileName = "F:/EE441/EE441_2014312120140910.dfxp";
		//getText(fileName);
		
		File folder = new File(dir+"transcripts/");
	    for (File fileEntry : folder.listFiles()) {	       
	        String fileName = dir+"transcripts/"+fileEntry.getName();
	        System.out.println(fileEntry.getName());
	        getText(fileName);
	        //break;	        	        
	        
	    }
	    
	}
	
	public static void getText(String fileName) throws UnsupportedEncodingException{
			
	    String dir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/";

		StringBuilder text = new StringBuilder();		
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		
		try{
			//XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName), "ISO-8859-1");

			List<String> sentences = new ArrayList<String>();
			List<Double[]> startEndTimes = new ArrayList<Double[]>();
			QName startAttr = new QName("begin");
			QName durAttr = new QName("dur");
			Double [] times;
			while(xmlEventReader.hasNext()){
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()){
					StartElement startElement = xmlEvent.asStartElement();
					if(startElement.getName().getLocalPart().equals("p")){
						xmlEvent = xmlEventReader.nextEvent();
						if(!xmlEvent.isEndElement()){
							//System.out.println(xmlEvent.asCharacters().getData());
							text.append(xmlEvent.asCharacters().getData());
							sentences.add(xmlEvent.asCharacters().getData().trim());
							String startTime_str = startElement.getAttributeByName(startAttr).getValue();
							String duration = startElement.getAttributeByName(durAttr).getValue();
							Double startTime = getStartTimeInSec(startTime_str);
							//System.out.println(startTime +", "+duration );
							
							times = new Double[2];
							times[0] = startTime;
							times[1] = startTime+Double.parseDouble(duration);
							startEndTimes.add(times);
						}
					}
				}				
			}
			/*
			double offset = startEndTimes.get(0)[0];
			for(int i = 0;i<startEndTimes.size();i++){
				startEndTimes.get(i)[0] = startEndTimes.get(i)[0]-offset;
				startEndTimes.get(i)[1] = startEndTimes.get(i)[1]-offset;				
			}
			*/
			
			//***for generating text file for training
			/*
			PrintWriter writer2 = new PrintWriter(dir+"yoyo_orig.txt", "ISO-8859-1");
			for(String sent:sentences)writer2.println(sent);
			writer2.close();
			*/
			
			//for(int i = 0 ;i<segments.size();i++)System.out.println(segmentTimes.get(i)[0]+"\n"+segmentTimes.get(i)[1]+"\n"+segments.get(i));			
			//String asrFile = "C:/Users/Abhiraj/Desktop/DR/ffmpeg/yoyo.xml";
			String fname = fileName.substring( fileName.lastIndexOf('/')+1, fileName.lastIndexOf('.'))+".xml";;
			
			String asrFile = dir+"transcripts_asr/"+fname;
			System.out.println("ASR FILE:"+asrFile);	
			
			SegmentMatcher segMat = new SegmentMatcher(sentences, startEndTimes,asrFile);
			segMat.generateSegments();
			List<String> segments = segMat.getSegments();
			List<Double[]> segmentTimes = segMat.getSegmentTimes();
			
			for(int i=0;i<segments.size();i++){
				String segment = segments.get(i);
				String title = fileName.substring( fileName.lastIndexOf('/')+1, fileName.lastIndexOf('.'))+"_"+(i+1)+".xml";
				
				PrintWriter writer = new PrintWriter(dir+"segments/"+title, "ISO-8859-1");
				writer.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
				writer.println("<body>");
				writer.println("<startTime>"+segmentTimes.get(i)[0]+"</startTime>");	
				writer.println("<endTime>"+segmentTimes.get(i)[1]+"</endTime>");
				writer.println("<text>"+segment+"</text>");	
				writer.println("</body>");
				writer.close();
				//System.out.println(segment);
			}
			
			System.out.println("Total Segments: "+segments.size());	
			
						
		} catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
		
		return;
	}

	private static Double getStartTimeInSec(String startTime) {
		String[] temp = startTime.split(":");
		Double hrs = Double.parseDouble(temp[0]);
		Double mins = Double.parseDouble(temp[1]);
		Double secs = Double.parseDouble(temp[2]);
		
		return (hrs*3600+mins*60 + secs);
	}
}
