/**
 * 
 */
package org.aksw.ore.component;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

/**
 * @author Lorenz Buehmann
 *
 */
public class ConfigurablePanel extends CssLayout{

	private static final ClickListener NOT_IMPLEMENTED_LISTENER = new ClickListener() {
        @Override
        public void buttonClick(ClickEvent event) {
            Notification.show("Not implemented in this demo");
        }
    };
	
	private Button configureButton;

	public ConfigurablePanel(Component content) {
        addStyleName("layout-panel");
        setSizeFull();

        configureButton = new Button();
        configureButton.addStyleName("configure");
        configureButton.addStyleName("icon-cog");
        configureButton.addStyleName("icon-only");
        configureButton.addStyleName("borderless");
        configureButton.setDescription("Configure");
        configureButton.addStyleName("small");
        configureButton.addClickListener(NOT_IMPLEMENTED_LISTENER);
        addComponent(configureButton);

        addComponent(content);
	}
	
	public void addClickListener(ClickListener clickListener){
		configureButton.removeClickListener(NOT_IMPLEMENTED_LISTENER);
		configureButton.addClickListener(clickListener);
	}

}
