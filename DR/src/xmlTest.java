import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class xmlTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String fileName = "/home/abhiraj/workspace/DR/files/CSCI570_2014114020140409-0.dfxp";
		
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		
		try{
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
			while(xmlEventReader.hasNext()){
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()){
					StartElement startElement = xmlEvent.asStartElement();
					if(startElement.getName().getLocalPart().equals("p")){
						xmlEvent = xmlEventReader.nextEvent();
						System.out.println(xmlEvent.asCharacters().getData());
					}
				}
				
			}
		} catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
		
	}

}
