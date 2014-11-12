package Parsing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class ASRParser {
	public static void main(String[] args) throws UnsupportedEncodingException{
		String dir = "C:/Users/Abhiraj/Desktop/";	   
	    String fileName = dir+"MIT6_006F11_lec14_300k.xml/";//"CSCI570_2014140920140122.dat";
		getText(fileName);
	}
	
	public static void getText(String fileName) throws UnsupportedEncodingException{
		StringBuilder text = new StringBuilder();		
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		List<String> words = new ArrayList<String>();
		List<List<String>> sentences = new ArrayList<List<String>>();
		List<String> startTimes = new ArrayList<String>();
		List<String> endTimes = new ArrayList<String>();
		
		QName startAttr = new QName("Start");
		QName endAttr = new QName("End");
		
		boolean firstSentence = true;
		StringBuilder sb = new StringBuilder();
		List<String> sentList = new ArrayList<String>();
		try{
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName), "ISO-8859-1");
						
			while(xmlEventReader.hasNext()){
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()){
					StartElement startElement = xmlEvent.asStartElement();
					if(startElement.getName().getLocalPart().equals("REF")){						
						xmlEvent = xmlEventReader.nextEvent();
						if(!xmlEvent.isEndElement()){
							words.add(xmlEvent.asCharacters().getData());
						}
					}
					else if(startElement.getName().getLocalPart().equals("UTT")){
						
						QName name = new QName("SRT");
						if(!firstSentence){
							String sentence = sb.toString().trim();
							sentList.add(sentence);
							sentences.add(sentList);
						}
						else firstSentence = false;
						
						sb = new StringBuilder();
						sentList = new ArrayList<String>();						
						sentList.add(startElement.getAttributeByName(startAttr).getValue());
						sentList.add(startElement.getAttributeByName(endAttr).getValue());
						
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
									sb.append(startElement.getAttributeByName(name).getValue()+" ");
									
								}
							}							
						}
					}
				}				
			}
			String sentence = sb.toString().trim();
			sentList.add(sentence);
			sentences.add(sentList);
			for(int i = 0 ;i<sentences.size();i++)System.out.println(sentences.get(i).get(0)+","+sentences.get(i).get(1)+","+sentences.get(i).get(2));
			
						
		} catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
	}
}
