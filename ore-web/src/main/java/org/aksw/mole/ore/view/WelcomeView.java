package org.aksw.mole.ore.view;

import org.aksw.mole.ore.widget.KnowledgebaseInfoPanel;
import org.vaadin.appfoundation.view.AbstractView;

import com.vaadin.ui.VerticalLayout;

public class WelcomeView extends AbstractView<VerticalLayout>{
	
	private KnowledgebaseInfoPanel kbInfoPanel;
	
	public WelcomeView() {
		super(new VerticalLayout());
		initUI();
	}
	
	private void initUI(){
		getContent().setSizeFull();
		
		kbInfoPanel = new KnowledgebaseInfoPanel();
		getContent().addComponent(kbInfoPanel);
	}

	public void activated(Object... params) {
		// TODO Auto-generated method stub
		
	}

	public void deactivated(Object... params) {
		// TODO Auto-generated method stub
		
	}
	
	public void refresh(){
		kbInfoPanel.refresh();
	}

}
