/**
 * 
 */
package org.aksw.ore;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

/**
 * @author Lorenz Buehmann
 *
 */
public class OREConfiguration {

	private static String wordNetDirectory;
	private static String cacheDirectory;
	
	public OREConfiguration() {
	}
	
	public static void loadSettings(ServletContext servletContext){
		InputStream is;
		try {
			is = OREConfiguration.class.getClassLoader().getResourceAsStream("settings.ini");
			if(is != null){
				Ini ini = new Ini(is);
				//base section
				Section baseSection = ini.get("base");
				wordNetDirectory = baseSection.get("wordNetDir", String.class).trim();
				cacheDirectory = baseSection.get("cacheDir", String.class).trim();
			}
			
			if(wordNetDirectory == null || wordNetDirectory.isEmpty()){
				wordNetDirectory = OREConfiguration.class.getClassLoader().getResource("wordnet").getPath();
			}
			
			if(cacheDirectory == null || cacheDirectory.isEmpty() || !cacheDirectory.startsWith("/")){
				cacheDirectory = servletContext.getRealPath("cache");
			}
			
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
	}
	
	/**
	 * @return the wordnetDirectory
	 */
	public static String getWordNetDirectory() {
		return wordNetDirectory;
	}
	
	/**
	 * @return the cacheDirectory
	 */
	public static String getCacheDirectory() {
		return cacheDirectory;
	}
	
	

}
