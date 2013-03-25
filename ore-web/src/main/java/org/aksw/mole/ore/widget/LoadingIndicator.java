package org.aksw.mole.ore.widget;

import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class LoadingIndicator extends VerticalLayout{
	
	private final Resource loadingIcon = new ThemeResource("images/ajax-loader.gif");
	
	
	public LoadingIndicator(String text) {
		setSizeUndefined();
		Embedded emb = new Embedded("", loadingIcon);
		addComponent(emb);
		addComponent(new Label(text));
		setExpandRatio(emb, 1f);
//		Label l = new Label("<p><img src=\"images/ajax-loader.gif\"></p><p>Detecting pattern instances</p>", Label.CONTENT_XHTML);
//		l.setSizeUndefined();
		
	}
	
	public LoadingIndicator() {
		// TODO Auto-generated constructor stub
	}

}
