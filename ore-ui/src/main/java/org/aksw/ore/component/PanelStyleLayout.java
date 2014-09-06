/**
 * 
 */
package org.aksw.ore.component;

import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

/**
 * @author Lorenz Buehmann
 *
 */
public class PanelStyleLayout extends CssLayout{

	public PanelStyleLayout(String caption) {
		addStyleName("card");
		setCaption(caption);
		setSizeFull();
	}
	
	public PanelStyleLayout(String caption, Component content) {
		this(caption);
		addComponent(content);
	}
	
	public PanelStyleLayout(String caption, Component ... headerComponents) {
		setSizeFull();
		addStyleName("card");
		if(headerComponents.length == 0){
			setCaption(caption);
		} else {
			HorizontalLayout panelCaption = new HorizontalLayout();
		    panelCaption.addStyleName("v-panel-caption");
		    panelCaption.setWidth("100%");
		    // panelCaption.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
		    Label label = new Label(caption);
		    panelCaption.addComponent(label);
		    panelCaption.setExpandRatio(label, 1);
		    
		    for (Component component : headerComponents) {
				panelCaption.addComponent(component);
			}
		    
		    addComponent(panelCaption);
		}
		
	}
}
