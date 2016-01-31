/**
 * 
 */
package org.aksw.ore.view;

import com.vaadin.data.Item;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table.ColumnHeaderMode;
import org.aksw.mole.ore.validation.constraint.ConstraintViolation;
import org.aksw.mole.ore.validation.constraint.SubjectObjectViolation;
import org.aksw.mole.ore.validation.constraint.SubjectViolation;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.ConstraintViolationExplanationWindow;
import org.aksw.ore.component.ProgressDialog;
import org.aksw.ore.component.WhitePanel;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParser;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;

/**
 * @author Lorenz Buehmann
 *
 */
public class ConstraintValidationView extends VerticalLayout implements View{
	
	private Table constraintsTable;
	private Table violationsTable;
	
	private OWLAxiom currentConstraint;
	private Button validateButton;
	private Label resultInfoLabel;
	
	public ConstraintValidationView() {
		addStyleName("dashboard-view");
		setSizeFull();
		setSpacing(true);
		setMargin(true);
		
		//constraint panel
		WhitePanel panel = new WhitePanel(createConstraintsPanel());
		addComponent(panel);
		setExpandRatio(panel, 1f);
		
		validateButton = new Button("Validate");
		validateButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				OWLAxiom constraint = (OWLAxiom) constraintsTable.getValue();
				currentConstraint = constraint;
				if(constraint != null){
					validate(constraint);
				}
			}
		});
		validateButton.setEnabled(false);
		addComponent(validateButton);
		setComponentAlignment(validateButton, Alignment.MIDDLE_CENTER);
		
		//violations panel
		panel = new WhitePanel(createConstraintViolationsPanel());
		addComponent(panel);
		setExpandRatio(panel, 1f);
		
	}
	
	private Component createConstraintsPanel(){
		HorizontalLayout l = new HorizontalLayout();
		l.setCaption("Constraints");
		l.setSizeFull();
		l.setSpacing(true);
		
		constraintsTable = new Table();
		constraintsTable.setPageLength(10);
		constraintsTable.addStyleName("borderless");
		constraintsTable.addContainerProperty("axiom", String.class, null);
		constraintsTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		constraintsTable.setSizeFull();
		constraintsTable.setSelectable(true);
		constraintsTable.addItemClickListener(new ItemClickListener() {
			
			@Override
			public void itemClick(ItemClickEvent event) {
				validateButton.setEnabled(true);
			}
		});
		l.addComponent(constraintsTable);
		l.setExpandRatio(constraintsTable, 1f);
		
		Button addButton = new Button("Add");
		addButton.addStyleName("default");
		addButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				Window dialog = showConstraintsInputDialog();
				UI.getCurrent().addWindow(dialog);
			}
		});
		l.addComponent(addButton);
		
		return l;
	}
	
	private Component createConstraintViolationsPanel(){
		VerticalLayout l = new VerticalLayout();
		l.setSpacing(true);
		l.setCaption("Violations");
		l.setSizeFull();
		
		resultInfoLabel = new Label();
		l.addComponent(resultInfoLabel);
		
		violationsTable = new Table();
		violationsTable.setPageLength(10);
		violationsTable.addContainerProperty("subject", String.class, null);
		violationsTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		violationsTable.setSizeFull();
		violationsTable.setSelectable(true);
		violationsTable.addItemClickListener(new ItemClickListener() {
			
			@Override
			public void itemClick(ItemClickEvent event) {
				String uri = (String) event.getItemId();
//				showViolationExplanation(currentConstraint, uri);
			}
		});
		violationsTable.addGeneratedColumn("subject", new Table.ColumnGenerator() {
            public Component generateCell(Table source, Object itemId,
                    Object columnId) {
                Item item = violationsTable.getItem(itemId);
                if(item != null){
                	String uri = (String) item.getItemProperty("subject").getValue();
                	if(uri != null){
                		String decodedURI = uri;
	                    try {
	    					decodedURI = URLDecoder.decode(uri, "UTF-8");
	    				} catch (UnsupportedEncodingException e) {
	    					e.printStackTrace();
	    				}
	                    Link link = new Link(decodedURI, new ExternalResource(uri));
	                    link.setTargetName("_blank");
	                    return link;
                	}
                }
                return null;
            }

        });
		l.addComponent(violationsTable);
		l.setExpandRatio(violationsTable, 1f);
		
		return l;
	}
	
	private void validate(final OWLAxiom constraint){
		final ProgressDialog progressDialog = new ProgressDialog("Searching for violating resources...");
		getUI().addWindow(progressDialog);
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					final Set<ConstraintViolation> violations = ORESession.getConstraintValidationManager().getViolatingResources(constraint);
					UI.getCurrent().access(new Runnable() {
						
						@Override
						public void run() {
							showViolations(violations);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				UI.getCurrent().access(new Runnable() {
					
					@Override
					public void run() {
						getUI().removeWindow(progressDialog);
					}
				});
			}
		});
		t.start();
		
//		Set<String> violations = ORESession.getConstraintValidationManager().validateWithExplanations(constraint);
//		for (String uri : violations) {
//			violationsTable.addItem(uri).getItemProperty("individual").setValue(uri);
//		}
	}
	
	private void showViolations(Set<ConstraintViolation> violations){
		violationsTable.removeAllItems();
		violationsTable.removeContainerProperty("object");
		violationsTable.removeGeneratedColumn("object");
		if(violations.isEmpty()){
			resultInfoLabel.setValue("Found no violations.");
		} else {
			//add object column
			boolean subjectObjectViolations = violations.iterator().next() instanceof SubjectObjectViolation;
			if(subjectObjectViolations){
				violationsTable.addContainerProperty("object", String.class, null);
				violationsTable.addGeneratedColumn("object", new Table.ColumnGenerator() {
		            public Component generateCell(Table source, Object itemId,
		                    Object columnId) {
		                Item item = violationsTable.getItem(itemId);
		                if(item != null){
		                	String uri = (String) item.getItemProperty("object").getValue();
							if (uri != null) {
								String decodedURI = uri;
								try {
									decodedURI = URLDecoder.decode(uri, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								Link link = new Link(decodedURI, new ExternalResource(uri));
								link.setTargetName("_blank");
								return link;
							}
		                   
		                }
		                return null;
		            }

		        });
			}
			resultInfoLabel.setValue("Found " + violations.size() + " violations:");
			String subject;
			String object;
			Item item;
			for (ConstraintViolation violation : violations) {
				item = violationsTable.addItem(violation);
				if(subjectObjectViolations){
					subject = ((SubjectObjectViolation)violation).getSubject();
					item.getItemProperty("subject").setValue(subject);
					object = ((SubjectObjectViolation)violation).getObject();
					item.getItemProperty("object").setValue(object);
				} else {
					subject = ((SubjectViolation)violation).getSubject();
					item.getItemProperty("subject").setValue(subject);
				}
				
			}
		}
	}
	
	private Window showConstraintsInputDialog(){
		VerticalLayout l = new VerticalLayout();
        l.setWidth("400px");
        l.setMargin(true);
        l.setSpacing(true);
    	final Window alert = new Window("Add Constraint(s)", l);
        alert.setModal(true);
//        alert.setResizable(false);
//        alert.setDraggable(false);
        alert.addStyleName("dialog");
        alert.setClosable(false);

        //the information
        Label message = new Label(
                "You can add constraints which you want to validate by adding OWL axioms in " +
                "<a href=\"http://www.w3.org/TR/owl2-syntax/#Functional-Style_Syntax\">functional-style syntax</a> to the field below.", ContentMode.HTML);
        l.addComponent(message);
        //the constraint input field
        final TextArea constraintInput = new TextArea();
        constraintInput.setWidth("90%");
//		constraintInput.setValue("FunctionalDataProperty(<http://dbpedia.org/ontology/birthDate>)");//"Enter constraint(s) here.");
		l.addComponent(constraintInput);
		l.setExpandRatio(constraintInput, 1f);
		l.setComponentAlignment(constraintInput, Alignment.MIDDLE_CENTER);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidth("100%");
        buttons.setSpacing(true);
        l.addComponent(buttons);

        Button ok = new Button("Add");
        ok.addStyleName("default");
        ok.addStyleName("small");
        ok.addStyleName("wide");
        ok.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
            	try {
					OWLOntology ontology = asOntology(constraintInput.getValue());
					addConstraints(ontology);
					alert.close();
					Notification
					        .show("The constraint was successfully loaded. Please select the constrainst from the list and click on 'Validate' to run the validation process.",
					                Type.TRAY_NOTIFICATION);
				} catch (OWLParserException e) {
					Notification
			        .show("Could not load the given input. Please check the syntax.", Type.ERROR_MESSAGE);
					e.printStackTrace();
				}

            }
        });
        buttons.addComponent(ok);
        
        Button cancel = new Button("Cancel");
        cancel.addStyleName("small");
        cancel.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                alert.close();
            }
        });
        buttons.addComponent(cancel);
        
        buttons.setComponentAlignment(ok, Alignment.MIDDLE_RIGHT);
        buttons.setComponentAlignment(cancel, Alignment.MIDDLE_LEFT);
        ok.focus();

        alert.addShortcutListener(new ShortcutListener("Cancel",
                KeyCode.ESCAPE, null) {
            @Override
            public void handleAction(Object sender, Object target) {
                alert.close();
            }
        });
        return alert;
	}
	
	private void addConstraints(OWLOntology ontology){
		for (OWLAxiom constraint : ontology.getLogicalAxioms()) {
			constraintsTable.addItem(constraint).getItemProperty("axiom").setValue(constraint.toString());
		}
	}
	
	private OWLOntology asOntology(String functionalSyntaxAxiomString) throws OWLParserException{
		try {
			StringDocumentSource s = new StringDocumentSource("Ontology(<http://www.ore.org> " + functionalSyntaxAxiomString + ")");
			OWLFunctionalSyntaxOWLParser p = new OWLFunctionalSyntaxOWLParser();
			OWLOntology newOntology = OWLManager.createOWLOntologyManager().createOntology();
			p.parse(s, newOntology, new OWLOntologyLoaderConfiguration());
			return newOntology;
		} catch (UnloadableImportException e) {
			e.printStackTrace();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void showViolationExplanation(OWLAxiom constraint, String uri){
		String explanation = ORESession.getConstraintValidationManager().getViolationExplanation(constraint, uri);
		ConstraintViolationExplanationWindow w = new ConstraintViolationExplanationWindow(constraint, explanation);
		getUI().addWindow(w);
		w.focus();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
		
	}
	
}
