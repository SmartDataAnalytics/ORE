package org.aksw.mole.ore;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.aksw.mole.ore.util.ScoreExplanationPattern;

public class OREContextListener implements ServletContextListener{
	

	@Override
	public void contextDestroyed(ServletContextEvent e) {
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		PatOMatPatternLibrary.init();
		ScoreExplanationPattern.init();
	}
	

}
