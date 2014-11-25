package Segmentation;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import Parsing.ASRParser;

public class SegmentMatcher {

	List<String> segments;
	List<Double[]> segmentTimes;
	List<String> sentences;
	List<Double[]> startEndTimes;
	String asrFile;
	
	public SegmentMatcher(List<String> sentences,List<Double[]> startEndTimes, String asrFile){
		this.sentences = sentences;
		this.startEndTimes = startEndTimes;
		this.asrFile = asrFile;
	}
	
	public void generateSegments() throws UnsupportedEncodingException{
		segments= new ArrayList<String>();
		segmentTimes= new ArrayList<Double[]>();
		ASRParser asrp = new ASRParser();
		asrp.getText(asrFile);
		//List<String> segments_asr = asrp.getSegments();
		List<Double[]> segmentTimes_asr = asrp.getSegmentTimes();
		
		int i = 0;
		StringBuilder sb =  new StringBuilder();
		sb.append(sentences.get(i)+" ");
		Double startTime = startEndTimes.get(i)[0];
		Double[] times = new Double[2];
		times[0] = startTime;
		i++;
		
		for(Double[] segTime_asr:segmentTimes_asr){
			
			while(startTime<segTime_asr[1]){
				sb.append(sentences.get(i)+" ");
				i++;
				if(i>=sentences.size())break;
				startTime = startEndTimes.get(i)[0];
			}
			
			times[1] = startEndTimes.get(i-1)[1];
			segments.add(sb.toString().trim());
			segmentTimes.add(times);
			
			sb =  new StringBuilder();
			times = new Double[2];
			times[0] = startTime;
			if(i>=sentences.size())break;
		}
	}
	
	public List<Double[]> getSegmentTimes(){
		return segmentTimes;
	}
	public List<String> getSegments(){
		return segments;
	}
}
