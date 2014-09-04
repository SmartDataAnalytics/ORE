package org.aksw.ore;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.aksw.ore.util.PatOMatPatternLibrary;
import org.aksw.ore.util.ScoreExplanationPattern;

@WebListener
public class OREContextListener implements ServletContextListener{
	

	@Override
	public void contextDestroyed(ServletContextEvent e) {
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		PatOMatPatternLibrary.init();
		ScoreExplanationPattern.init();
		OREConfiguration.loadSettings(servletContextEvent.getServletContext());
	}
	

}
