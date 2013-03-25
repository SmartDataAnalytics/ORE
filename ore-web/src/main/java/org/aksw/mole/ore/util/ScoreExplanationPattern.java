package org.aksw.mole.ore.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.aksw.mole.ore.PatOMatPatternLibrary;
import org.semanticweb.owlapi.model.AxiomType;

public class ScoreExplanationPattern {
	
	private static String patternFile = "score_explanations.txt";
	
	private static Map<AxiomType, String> axiomType2Pattern;
	private static String genericPattern = "Total: $total\n Positive: $pos";
	
	public static void init(){
		axiomType2Pattern = new HashMap<AxiomType, String>();
		
		DataInputStream in = null;
		try {
			in = new DataInputStream(PatOMatPatternLibrary.class.getClassLoader().getResourceAsStream(patternFile));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null) {
				if(!line.startsWith("#")){
					String[] split = line.split("\\|");
					try {
						AxiomType axiomType = AxiomType.getAxiomType(split[0].trim());
						String pattern = genericPattern;
						if(split.length == 2){
							pattern = split[1].trim();
						}
						axiomType2Pattern.put(axiomType, pattern);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String getExplanationPattern(AxiomType axiomType){
		return axiomType2Pattern.get(axiomType);
	}
	
	public static String getGenericPattern() {
		return genericPattern;
	}
}
