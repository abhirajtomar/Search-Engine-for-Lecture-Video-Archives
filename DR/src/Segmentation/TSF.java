package Segmentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TSF {
	List<String> doc;
	double threshold;
	int minSegSize;
	List<Double[]> startEndTimes;
	List<Double[]> segmentTimes;
	
	public TSF(List<String> sentences,List<Double[]> startEndTimes,double threshold,int minSegSize){
		this.doc = sentences;
		this.threshold = threshold;
		this.minSegSize = minSegSize;
		this.startEndTimes = startEndTimes;
	}
	
	public List<String> getSegments(){
		formatSentencesOnWords();
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
			Double[] times = new Double[2];
			times[0] = startEndTimes.get(start)[0];
			times[1] = startEndTimes.get(boundary)[1];
			/*
			if((boundary-start) > 30){
				int tempmid = (boundary+start)/2; 
				while(start<=tempmid){
					sb.append(doc.get(start)+"\n");
					start++;
				}
				segments.add(sb.toString());
				times[1] = startEndTimes.get(tempmid)[1];
				segmentTimes.add(times);
				times[0] = startEndTimes.get(start)[0];
				times[1] = startEndTimes.get(boundary)[1];
				sb = new StringBuilder();
			}
			*/
			while(start<=boundary){
				sb.append(doc.get(start)+"\n");
				start++;
			}
			String newseg = sb.toString();
			
			int wordcount = newseg.replace("\n"," ").split(" ").length;
			if(wordcount>350){
				
				System.out.println("Bigger!!!"+wordcount);
				int midpoint = newseg.length()/2;
				while(newseg.charAt(midpoint)!=' ')midpoint++;
				String seg1 = newseg.substring(0,midpoint);
				String seg2 = newseg.substring(midpoint);
				
				//*** Times need to be modified***
				segments.add(seg1);
				segmentTimes.add(times);
				segments.add(seg2);
				segmentTimes.add(times);
				
				
			}
			else{
				segments.add(newseg);			
				segmentTimes.add(times);
			}
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

	protected double getSimilarity(List<String> pre, List<String> post) {
		
		double similarity = 0.0;
		List<Map<String, Integer>> preScores= new ArrayList<Map<String, Integer>>();
		List<Map<String, Integer>> postScores= new ArrayList<Map<String, Integer>>();
		
		for(String sentence:pre){
			Map<String, Integer> scoreMap = new HashMap<>();
			String[] words = sentence.split(" ");
		    for (String w : words) {
		        Integer n = scoreMap.get(w);
		        n = (n == null) ? 1 : ++n;
		        scoreMap.put(w, n);
		    }
		    preScores.add(scoreMap);
		}
		for(String sentence:post){
			Map<String, Integer> scoreMap = new HashMap<>();
			String[] words = sentence.split(" ");
		    for (String w : words) {
		        Integer n = scoreMap.get(w);
		        n = (n == null) ? 1 : ++n;
		        scoreMap.put(w, n);
		    }
		    postScores.add(scoreMap);
		}
		for(Map<String, Integer> preScore: preScores){
			for(Map<String, Integer> postScore: postScores){
				similarity += cosine_similarity(preScore,postScore);
			}			
		}
		
		similarity /= (preScores.size()*postScores.size());
		
		return similarity;
	}
	
	public double cosine_similarity(Map<String, Integer> v1, Map<String, Integer> v2) {
        Set<String> both = new HashSet<String>(v1.keySet());
        both.retainAll(v2.keySet());
        double sclar = 0, norm1 = 0, norm2 = 0;
        for (String k : both) sclar += v1.get(k) * v2.get(k);
        for (String k : v1.keySet()) norm1 += v1.get(k) * v1.get(k);
        for (String k : v2.keySet()) norm2 += v2.get(k) * v2.get(k);
        return sclar / Math.sqrt(norm1 * norm2);
	}
	public void formatSentencesOnWords(){
		List<String> newdoc = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for(String sent:doc){
			sb.append(sent.replace("\n", "")+" ");
		}
		String[] docTokens=  sb.toString().split(" ");
		int i = 0;
		while(i<docTokens.length){
			int counter = 0;
			String text="";
			while(counter<10 && i<docTokens.length){
				text += docTokens[i]+" ";
				counter++;
				i++;
			}
			//text = text.trim();
			newdoc.add(text);
		}
		doc = newdoc;
	}
	public List<Double[]> getSegmentTimes(){
		return segmentTimes;
	}
}
