package org.aksw.ore;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.aksw.ore.util.PatOMatPatternLibrary;
import org.aksw.ore.util.AxiomScoreExplanationGenerator;

@WebListener
public class OREContextListener implements ServletContextListener{
	

	@Override
	public void contextDestroyed(ServletContextEvent e) {
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		PatOMatPatternLibrary.init();
		AxiomScoreExplanationGenerator.init();
		OREConfiguration.loadSettings(servletContextEvent.getServletContext());
	}
	

}
