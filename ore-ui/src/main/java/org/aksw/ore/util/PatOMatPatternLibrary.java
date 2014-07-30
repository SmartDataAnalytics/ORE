package org.aksw.ore.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.aksw.ore.model.NamingPattern;

public class PatOMatPatternLibrary {

	private static String patternFile = "patomat_pattern.txt";
	
	private static List<NamingPattern> pattern = new ArrayList<NamingPattern>();

	public static void init() {
		loadPatterns();
	}
	
	public static List<NamingPattern> getPattern() {
		return pattern;
	}

	private static void loadPatterns() {
		DataInputStream in = null;
		try {
			in = new DataInputStream(PatOMatPatternLibrary.class.getClassLoader().getResourceAsStream(patternFile));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null) {
				if(!line.startsWith("#")){
					String[] split = line.split(",");
					try {
						int id = Integer.valueOf(split[0]);
						String label = split[1].trim();
						URL url = new URL(split[2].trim());
						boolean useReasoning = Boolean.valueOf(split[3].trim());
						String imageFilename =  split[4].trim();
						String description =  split[5].trim();
						pattern.add(new NamingPattern(id, label, url, useReasoning, imageFilename, description));
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
		System.out.println("Loaded " + pattern.size() + " pattern.");
	}

}
