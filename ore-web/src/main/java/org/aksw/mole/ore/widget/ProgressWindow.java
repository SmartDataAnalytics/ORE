package org.aksw.mole.ore.widget;

import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class ProgressWindow extends Window{
	
	public ProgressWindow(String task) {
		this(task, true);
	}
	
	public ProgressWindow(String task, boolean indeterminate) {
		setModal(true);
	
		VerticalLayout mainLayout = new VerticalLayout();
		setSizeUndefined();
		setContent(mainLayout);
		
		Label infoLabel = new Label(task);
		mainLayout.addComponent(infoLabel);
		
		ProgressIndicator progressBar = new ProgressIndicator();
		progressBar.setIndeterminate(indeterminate);
		mainLayout.addComponent(progressBar);
	}

}
