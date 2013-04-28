package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Evaluator {

	/**
	 * 
	 * @param dir
	 * @param triggers
	 * @param outfile
	 * @throws Exception
	 */
	public static void generateGoldenAnswer(String dir, String[] triggers, String outfile) throws Exception{
		
		File[] fileList = new File(dir).listFiles();
		
		PrintWriter writer = FileUtils.getWriter(outfile);
		for(File file : fileList){
			String filename = file.getName().replace(".xml", "");
			List<SentenceInstance> sentenceInsts = DocumentParser.loadSentences(file.getCanonicalPath(), triggers);
			sentenceInsts = filterSentenceInstance(sentenceInsts, filename);
			for(SentenceInstance inst : sentenceInsts){
				writer.write(filename + "\t" + inst.toString() + "\n");
			}
			writer.flush();
		}
		writer.close();
		
	}
	
	/**
	 * 
	 * @param sentenceInsts
	 * @param personName
	 * @return
	 */
	public static List<SentenceInstance> filterSentenceInstance(List<SentenceInstance> sentenceInsts, String personName){
		List<SentenceInstance> insts = new ArrayList<SentenceInstance>();
		for(SentenceInstance inst : sentenceInsts){
			if(TreeBuilder.isTermInSequence(inst.toString(), personName))
				insts.add(inst);
		}
		
		return insts;
	}
	
	public static void eval(String answerFile, String goldenFile) throws Exception{
		Map<String, List<String>> gold = loadGolden(goldenFile);
		BufferedReader reader = FileUtils.getReader(answerFile);
		String line = "";
		int correct = 0, totalAnswer = 0;
		while((line = reader.readLine()) != null){
			totalAnswer ++;
			
			String[] parts = line.split("\t");
			if(parts.length == 3){
				String srcName = parts[0];
				String tarName = parts[1];
				
				if(gold.containsKey(srcName)){
					List<String> ans = gold.get(srcName);
					if(ans.contains(tarName)){
						correct ++;
					}
				}
			}
		}
		
		int goldenTotal = getGoldenTotal(gold);
		double precision = (double) correct / (double) totalAnswer;
		double recall = (double) correct / (double) goldenTotal;
		double Fmeasure = 2 * precision * recall / (precision + recall);
		
		precision = getRound(precision);
		recall = getRound(recall);
		Fmeasure = getRound(Fmeasure);
		
		System.out.println("precsion: " + precision + "\t recall: " + recall + "\t Fmeasure: " + Fmeasure);
	}
	
	public static int getGoldenTotal(Map<String, List<String>> gold){
		int total = 0;
		for(String key : gold.keySet()){
			total += gold.get(key).size();
		}
		
		return total;
	}
	
	public static double getRound(double d){
		BigDecimal bd = new BigDecimal(d);
		double result = bd.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
		return result;
	}
	
	public static Map<String, List<String>> loadGolden(String goldenFile){
		Map<String, List<String>> gold = new HashMap<String, List<String>>();
		try{
			BufferedReader reader = FileUtils.getReader(goldenFile);
			String line = "";
			while((line = reader.readLine()) != null){
				String[] parts = line.split("\t");
				if(parts.length == 3){
					String srcName = parts[0];
					String tarName = parts[1];
					
					if(gold.containsKey(srcName)){
						gold.get(srcName).add(tarName);
					}else{
						List<String> list = new ArrayList<String>();
						list.add(tarName);
						gold.put(srcName, list);
					}
				}
			}
		}catch(Exception e){}
		
		return gold;
	}
	
	public static void batchEval(String answerDir, String golden){
		try{
			File[] fileList = new File(answerDir).listFiles();
			for(File file : fileList){
				String answer = file.getCanonicalPath();
				System.out.print(file.getName().replace("result", "") + "\t");
				eval(answer, golden);
			}
		}catch(Exception e){}
	}
	public static void main(String[] args)throws Exception{
		String dir="data/raw2tag";
		String[] triggers = new String[]{"brother", "brothers"}; 
		String golden = "data/golden2";
//		generateGoldenAnswer(dir, triggers, golden) ;
		String answer = "data/result0.2";
//		eval(answer, golden);
		
		batchEval("data/raw2_eval", "data/golden2");
	}
}
