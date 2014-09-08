/**
 * 
 */
package org.aksw.ore;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinServlet;

/**
 * @author Lorenz Buehmann
 *
 */
@WebServlet(value = "/*", asyncSupported = true, displayName = "ORE - Ontology Repair and Enrichment")
@VaadinServletConfiguration(productionMode = false, ui = OREUI.class, widgetset="org.aksw.ore.AppWidgetSet")
public class OREServlet extends VaadinServlet{

}
