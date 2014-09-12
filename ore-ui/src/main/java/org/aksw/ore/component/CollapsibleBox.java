package org.aksw.ore.component;

import com.vaadin.event.LayoutEvents;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

public class CollapsibleBox extends CustomComponent {

	private static final long serialVersionUID = 632703177884323377L;
	final private CssLayout root;
	private CssLayout bodyContainer;
	private CssLayout titleLayout;
	private Button toggleButton;

	void toggleBodyVisible() {
		if (bodyContainer.isVisible()) {
			titleLayout.removeStyleName("collapsiblebox-title-open");
			titleLayout.addStyleName("collapsiblebox-title-closed");
			toggleButton.setIcon(FontAwesome.CARET_RIGHT);
		} else {
			titleLayout.removeStyleName("collipsablebox-title-closed");
			titleLayout.addStyleName("collapsiblebox-title-open");
			toggleButton.setIcon(FontAwesome.CARET_DOWN);
		}
		bodyContainer.setVisible(!bodyContainer.isVisible());
	}

	public CollapsibleBox(String title, Component body) {
		super();
		addStyleName("collapsiblebox");
		root = new CssLayout();
		root.setSizeFull();

		//root.addStyleName("collapsiblebox-container");
		setCompositionRoot(root);

		draw(title, body);
		
		toggleBodyVisible();
	}

	void draw(String title, Component body) {
		titleLayout = new CssLayout();
		titleLayout.addStyleName("collapsiblebox-title");
		titleLayout.addStyleName("collapsiblebox-title-closed");

		titleLayout.addComponent(new Label(title));
		titleLayout.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
			private static final long serialVersionUID = -4750845792730551399L;

			@Override
			public void layoutClick(LayoutEvents.LayoutClickEvent event) {
				toggleBodyVisible();
			}
		});
		
		toggleButton = new Button(title);
		toggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
//		toggleButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
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
//		bodyContainer.setVisible(false);
		
//		root.addComponent(titleLayout);
		root.addComponent(toggleButton);
		root.addComponent(bodyContainer);
	}
}
