package org.aksw.ore.component;

import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

public class CollapsibleBox extends CustomComponent{

	private static final long serialVersionUID = 632703177884323377L;
	final private CssLayout root;
	private CssLayout bodyContainer;

	void toggleBodyVisible() {
		bodyContainer.setVisible(!bodyContainer.isVisible());
	}

	public CollapsibleBox(String title, Component body) {
		super();
		root = new CssLayout();
		root.addStyleName("collapsiblebox-container");
		setCompositionRoot(root);
		draw(title, body);
	}

	void draw(String title, Component body) {
		CssLayout titleLayout = new CssLayout();
		titleLayout.addStyleName("collapsiblebox-title");
		titleLayout.addComponent(new Label(title));
		titleLayout.addLayoutClickListener(new LayoutClickListener() {
			private static final long serialVersionUID = -4750845792730551399L;

			@Override
			public void layoutClick(LayoutClickEvent event) {
				toggleBodyVisible();
			}
		});

		bodyContainer = new CssLayout();
		bodyContainer.addStyleName("collapsiblebox-body");
		bodyContainer.addComponent(body);

		root.addComponent(titleLayout);
		root.addComponent(bodyContainer);
	}
}
