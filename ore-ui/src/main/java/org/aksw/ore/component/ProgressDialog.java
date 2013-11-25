/**
 * 
 */
package org.aksw.ore.component;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * @author Lorenz Buehmann
 *
 */
public class ProgressDialog extends Window {
	
	protected volatile boolean cancelled = false;
	protected Label messageLabel;
	protected Label traceMessageLabel;
	private ProgressBar progressBar;

	public ProgressDialog() {
		this("");
	}
	
	public ProgressDialog(String title) {
		this(title, false);
	}
	
	public ProgressDialog(String title, boolean showProgressBar) {
		super(title);
		
		VerticalLayout l = new VerticalLayout();
        l.setWidth("500px");
        l.setMargin(true);
        l.setSpacing(true);
        setContent(l);
        
        setModal(true);
        setResizable(false);
        setDraggable(false);
        addStyleName("dialog");
        setClosable(false);

        messageLabel = new Label();
        messageLabel.setImmediate(true);
        messageLabel.setContentMode(ContentMode.HTML);
        l.addComponent(messageLabel);
        
        traceMessageLabel = new Label();
        traceMessageLabel.setImmediate(true);
        traceMessageLabel.setContentMode(ContentMode.HTML);
        l.addComponent(traceMessageLabel);
        
        if(showProgressBar){
        	progressBar = new ProgressBar();
        	l.addComponent(progressBar);
        	l.setComponentAlignment(progressBar, Alignment.MIDDLE_CENTER);
        }

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidth("100%");
        buttons.setSpacing(true);
        l.addComponent(buttons);

        Button cancel = new Button("Stop");
        cancel.addStyleName("small");
        cancel.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	onCancelled();
            }
        });
        buttons.addComponent(cancel);
        buttons.setComponentAlignment(cancel, Alignment.MIDDLE_CENTER);

        cancel.focus();
	}
	
	protected void onCancelled(){
		cancelled = true;
        close();
	}
	
	public void setMessage(final String messageText){
		UI.getCurrent().access(new Runnable() {
			@Override
			public void run() {
				messageLabel.setValue(messageText);
			}
		});
		
	}
	
	public void setTraceMessage(final String message){
		UI.getCurrent().access(new Runnable() {
			@Override
			public void run() {
				traceMessageLabel.setValue(message);
			}
		});
		
	}
	
	public void setProgress(final float progress){
		UI.getCurrent().access(new Runnable() {
			@Override
			public void run() {
				progressBar.setValue(progress);
			}
		});
		
	}
	
	public void setProgress(int current, int total){
		setProgress((float)current/total);
	}
}
