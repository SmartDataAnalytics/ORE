/**
 * 
 */
package org.aksw.ore.component;

import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;

/**
 * @author Lorenz Buehmann
 *
 */
public class WhitePanel extends CssLayout{

	
	public WhitePanel(Component content) {
        addStyleName("layout-panel");
        setSizeFull();
        addComponent(content);
	}

}
