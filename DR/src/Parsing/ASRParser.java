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
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import Segmentation.ASR_TSF;

public class ASRParser {
	static List<String> segments;
	static List<Double[]> segmentTimes;
	
	public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException{
		String dir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/transcripts_asr/";	   
		String segdir = "C:/Users/Abhiraj/git/Search Engine for Lecture Video Archives/DR/files/cs570/segments_asr/";
		//String fileName = dir+"MIT6_006F11_lec14_300k.xml/";//"CSCI570_2014140920140122.dat";
	    //fileName = "C:/Users/Abhiraj/Desktop/DR/ffmpeg/yoyo.xml";
		//getText(fileName);
		
		File folder = new File(dir);
	    for (File fileEntry : folder.listFiles()) {	       
	        String fileName = dir+fileEntry.getName();
	        System.out.println(fileEntry.getName());
	        getText(fileName);
	        // break;
	        
	        //Write Segments to files
	        //System.out.println(segments.size());
	        for(int i=0;i<segments.size();i++){
				String segment = segments.get(i);
				String title = fileName.substring( fileName.lastIndexOf('/')+1, fileName.lastIndexOf('.'))+"_"+(i+1)+".xml";
				
				PrintWriter writer = new PrintWriter(segdir+title, "ISO-8859-1");
				writer.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
				writer.println("<body>");
				writer.println("<startTime>"+segmentTimes.get(i)[0]+"</startTime>");	
				writer.println("<endTime>"+segmentTimes.get(i)[1]+"</endTime>");
				writer.println("<text>"+segment+"</text>");	
				writer.println("</body>");
				writer.close();
				//System.out.println(segment);
			}
	        
	    }
	}
	
	public static void getText(String fileName) throws UnsupportedEncodingException{
		StringBuilder text = new StringBuilder();		
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		List<String> words = new ArrayList<String>();
		List<Double> wordTimes = new ArrayList<Double>();
		List<String> sentences = new ArrayList<String>();
		List<String[]> startEndTimes = new ArrayList<String[]>();
		
		
		QName startAttr = new QName("Start");
		QName endAttr = new QName("End");
		
		boolean firstSentence = true;
		StringBuilder sb = new StringBuilder();
		//List<String> sentList = new ArrayList<String>();
		String[] times = new String[2];
		try{
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName), "ISO-8859-1");
						
			while(xmlEventReader.hasNext()){
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()){
					StartElement startElement = xmlEvent.asStartElement();
					
					if(startElement.getName().getLocalPart().equals("UTT")){
						
						QName name = new QName("SRT");
						if(!firstSentence){
							String sentence = sb.toString().trim();
							
							sentences.add(sentence.replaceAll("\\\\.*\\s+", " "));
							startEndTimes.add(times);
						}
						else firstSentence = false;
						
						sb = new StringBuilder();
						times = new String[2];
						times[0] = startElement.getAttributeByName(startAttr).getValue();
						times[1] = startElement.getAttributeByName(endAttr).getValue();
												
						while(true){
							xmlEvent = xmlEventReader.nextEvent();
							if(xmlEvent.isEndElement() ){
								EndElement endElement = xmlEvent.asEndElement();
								if(endElement.getName().getLocalPart().equals("UTT")){
									break;
								}								
							}
							
							else if (xmlEvent.isStartElement()){
								
								startElement = xmlEvent.asStartElement();
								if(startElement.getName().getLocalPart().equals("WRD")){
									String word = startElement.getAttributeByName(name).getValue().replaceAll("\\\\.*\\s*", " ");
									sb.append(word+" ");
									words.add(word);
									String time = startElement.getAttributeByName(startAttr).getValue();
									wordTimes.add(Double.parseDouble(time));
								}
							}							
						}
					}
				}				
			}
			String sentence = sb.toString().trim();
			//sentList.add(sentence);
			sentences.add(sentence.replaceAll("\\\\.*\\s+", " "));
			startEndTimes.add(times);
			//for(int i = 0 ;i<sentences.size();i++)System.out.println(startEndTimes.get(i)[0]+","+startEndTimes.get(i)[1]+","+sentences.get(i));
			ASR_TSF segmenter = new ASR_TSF(words,wordTimes,0.30,15);
			segments = segmenter.getSegments();
			segmentTimes = segmenter.getSegmentTimes();
			/*
			//Removing the offset on time
			double offset = segmentTimes.get(0)[0];
			System.out.println("Offset: "+offset);
			for(int i = 0;i<segmentTimes.size();i++){
				segmentTimes.get(i)[0] = segmentTimes.get(i)[0]-offset;
				segmentTimes.get(i)[1] = segmentTimes.get(i)[1]-offset;				
			}
			*/
			//for(int i = 0 ;i<segments.size();i++)System.out.println(segmentTimes.get(i)[0]+"\n"+segmentTimes.get(i)[1]+"\n"+segments.get(i).replace("\n", " "));
			System.out.println("Total Segments: "+segments.size());			
		} catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
	}
	
	public List<Double[]> getSegmentTimes(){
		return segmentTimes;
	}
	public List<String> getSegments(){
		return segments;
	}
}
