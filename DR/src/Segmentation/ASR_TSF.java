package Segmentation;

import java.util.ArrayList;
import java.util.List;

public class ASR_TSF extends TSF {
	List<String[]> startEndTimes;
	List<String[]> segmentTimes;
	public ASR_TSF(List<String> sentences,List<String[]> startEndTimes, double threshold, int minSegSize) {
		super(sentences, threshold, minSegSize);
		this.startEndTimes = startEndTimes;
		segmentTimes = new ArrayList<String[]>();
	}
	
	@Override
	public List<String> getSegments(){
		List<String> segments= new ArrayList<String>();
		List<Integer> boundaries= new ArrayList<Integer>();
		int start = 0;
		int mid = minSegSize;
		int end = 2*minSegSize;
		List<String> pre = new ArrayList<String>();
		pre.add("");
		for(int i=0;i<mid-1;i++)pre.add(doc.get(i));
		List<String> post = new ArrayList<String>();
		for(int i=mid;i<end-1;i++)post.add(doc.get(i));
		
		int candidate = 0;
		double candidateScore = 0.0;
		boolean lookahead= false;
		int lookaheadCounter = 0;
		
		while(mid+minSegSize<doc.size()){
			pre.remove(0);
			pre.add(doc.get(mid));
			post.remove(0);
			post.add(doc.get(mid+minSegSize));
			
			double preSimilarity = getSimilarity(pre,pre);
			double postSimilarity = getSimilarity(post,post);
			double innerSimilarity = (preSimilarity+postSimilarity)/2;
			
			double outerSimilarity = getSimilarity(pre,post);
			
			double dissimilarity = (innerSimilarity-outerSimilarity)/innerSimilarity;
			
			if(lookahead){
				if(dissimilarity>candidateScore){
					candidate = mid;
					candidateScore = dissimilarity;					
				}
				lookaheadCounter--;	
				if(lookaheadCounter==0){
					boundaries.add(candidate);
					lookahead = false;
					mid = candidate+ minSegSize;
				}				
			}
			else{			
				if(dissimilarity>threshold){
					lookahead=true;
					lookaheadCounter = minSegSize;
					candidate = mid;
					candidateScore = dissimilarity;
				}
			}
			//System.out.println(doc.get(mid));
			//System.out.println("Dissimiarity: "+dissimilarity);
			
			mid++;
		}
		boundaries.add(doc.size()-1);
		
		start = 0;
		
		for(Integer boundary:boundaries){
			StringBuilder sb = new StringBuilder();
			String[] times = new String[2];
			times[0] = startEndTimes.get(start)[0];
			times[1] = startEndTimes.get(boundary)[1];
			
			while(start<=boundary){
				sb.append(doc.get(start)+"\n");
				start++;
			}
			segments.add(sb.toString());			
			segmentTimes.add(times);
		}
		/*
		int i=1;
		for(String segment:segments){
			System.out.println("Segment "+i);
			System.out.println(segment);
			System.out.println("");
			i++;
		}
		*/
		System.out.println("Total Segments: "+segments.size());
		return segments;
	}
	
	public List<String[]> getSegmentTimes(){
		return segmentTimes;
	}

}
