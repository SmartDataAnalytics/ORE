package org.aksw.ore.component;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.themes.ValoTheme;

public class CollapsibleBox extends CssLayout {

	private static final long serialVersionUID = 632703177884323377L;
//	final private CssLayout root;
	private CssLayout bodyContainer;
	private Button toggleButton;

	void toggleBodyVisible() {
		if (bodyContainer.isVisible()) {
			toggleButton.setIcon(FontAwesome.CARET_RIGHT);
		} else {
			toggleButton.setIcon(FontAwesome.CARET_DOWN);
		}
		bodyContainer.setVisible(!bodyContainer.isVisible());
	}

	public CollapsibleBox(String title, Component body) {
		super();
		addStyleName("collapsiblebox");
//		root = new CssLayout();
//		root.setSizeFull();
//
//		root.addStyleName("collapsiblebox-container");
//		setCompositionRoot(root);

		toggleButton = new Button(title);
		toggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		toggleButton.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				toggleBodyVisible();
			}
		});

		bodyContainer = new CssLayout();
		bodyContainer.setSizeFull();
		bodyContainer.addStyleName("collapsiblebox-body");
		bodyContainer.addComponent(body);

		addComponent(toggleButton);
		addComponent(bodyContainer);
		
		toggleBodyVisible();
	}
}
