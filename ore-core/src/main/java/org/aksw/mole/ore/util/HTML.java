package org.aksw.mole.ore.util;

public class HTML {

	public static String asLink(String uri){
		return "<a href=\"" + uri + "\">" + uri + "</a>";
	}
	
	public static String asLink(String uri, String text){
		return "<a href=\"" + uri + "\">" + text + "</a>";
	}

}
