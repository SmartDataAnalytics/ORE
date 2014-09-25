/**
 * 
 */
package org.aksw.ore.component;

import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * @author Lorenz Buehmann
 *
 */
public class WhitePanel extends CssLayout{

	
	public WhitePanel(Component content) {
//        addStyleName("layout-panel");
        addStyleName(ValoTheme.LAYOUT_CARD);
        setSizeFull();
        addComponent(content);
	}

}
