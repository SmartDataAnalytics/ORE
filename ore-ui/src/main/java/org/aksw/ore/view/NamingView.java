package org.aksw.ore.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.mole.ore.naming.NamingIssue;
import org.aksw.mole.ore.naming.NamingIssueDetection;
import org.aksw.mole.ore.naming.RenamingInstruction;
import org.aksw.ore.OREConfiguration;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.ProgressDialog;
import org.aksw.ore.component.WhitePanel;
import org.aksw.ore.model.EntityRenaming;
import org.aksw.ore.model.NamingPattern;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.rendering.Renderer;
import org.aksw.ore.util.PatOMatPatternLibrary;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.hene.popupbutton.PopupButton;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityEvent;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityListener;
import org.w3c.dom.Document;

import com.google.common.collect.Sets;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class NamingView extends VerticalLayout implements View, Refreshable{
	
	private static final Logger logger = LoggerFactory.getLogger(NamingView.class);
	
	private Table namingPatternsTable;
	private Table patternInstancesTable;
	private Table instructionsTable;
	
	private Button detectButton;
	private Button instructButton;
	private Button transformButton;
	
	private NamingPattern currentNamingPattern;
	private OWLOntology currentlyLoadedOntology;
	Document doc;
	
	private GridLayout layout;
	
	private Set<NamingIssue> detectedPatternInstances;
	private Set<NamingIssue> selectedPatternInstances;
	
	private List<RenamingInstruction> generatedInstructions;
	private List<RenamingInstruction> selectedInstructions;

	private Table filteredSuperclassesTable;

	private Set<String> selectedSuperClasses;

	private NamingIssueDetection namingIssueDetection;

	public NamingView() {
		namingIssueDetection = new NamingIssueDetection(OREConfiguration.getWordNetDirectory());
		initUI();
	}
	
	private void initUI(){
		setSizeFull();
//		setMargin(true);
		addStyleName("dashboard-view");
		addStyleName("naming-view");
		
//		layout = new GridLayout(6, 1);
		HorizontalLayout layout = new HorizontalLayout();
		layout.setSizeFull();
		addComponent(layout);
//		setComponentAlignment(layout, Alignment.MIDDLE_CENTER);

//		layout.addComponent(createNamingPatternView(), 0, 0);
//		layout.addComponent(createPatternInstancesView(), 2, 0);
//		layout.addComponent(createInstructionsView(), 4, 0);
		
		Component namingPatternView = createNamingPatternView();
		layout.addComponent(namingPatternView);
		
		detectButton = new Button("Detect");
		detectButton.setIcon(FontAwesome.SEARCH);
		detectButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		detectButton.setEnabled(false);
		detectButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onDetectNamingPattern();
			}
		});
//		layout.addComponent(detectButton, 1, 0);
		layout.addComponent(detectButton);
		layout.setComponentAlignment(detectButton, Alignment.MIDDLE_CENTER);
		
		Component patternInstancesView = createPatternInstancesView();
		layout.addComponent(patternInstancesView);
		
		instructButton = new Button("Instruct");
		instructButton.setIcon(FontAwesome.SHARE_ALT);
		instructButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		instructButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onGenerateInstructions();
			}
		});
//		layout.addComponent(instructButton, 3, 0);
		layout.addComponent(instructButton);
		layout.setComponentAlignment(instructButton, Alignment.MIDDLE_CENTER);
		
		Component instructionsView = createInstructionsView();
		layout.addComponent(instructionsView);
		
		transformButton = new Button("Transform");
		transformButton.setIcon(FontAwesome.GEARS);
		transformButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		transformButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onTransform();
			}
		});
//		layout.addComponent(transformButton, 5, 0);
		layout.addComponent(transformButton);
		layout.setComponentAlignment(transformButton, Alignment.MIDDLE_CENTER);
		
		layout.setExpandRatio(namingPatternView, 1f);
		layout.setExpandRatio(patternInstancesView, 1f);
		layout.setExpandRatio(instructionsView, 1f);
		
//		layout.setColumnExpandRatio(0, 0.3f);
//		layout.setColumnExpandRatio(2, 0.3f);
//		layout.setColumnExpandRatio(4, 0.3f);
//		layout.setRowExpandRatio(0, 1f);
		layout.setSpacing(true);
		
//		instructionsTable.setEnabled(false);
		instructButton.setEnabled(false);
		transformButton.setEnabled(false);
		
		//add PatOMat logo
		Link link = new Link("",
		        new ExternalResource("http://patomat.vse.cz/"));
		link.setIcon(new ThemeResource("img/logo_patomat.png"));
		link.setTargetName("_blank");
		link.addStyleName("patomat-logo");
		VerticalLayout patomatLogo = new VerticalLayout();
		patomatLogo.setWidth(null);
		Label label = new Label("Powered by");
		label.setWidth(null);
		patomatLogo.addComponent(label);
		patomatLogo.addComponent(link);
		addComponent(patomatLogo);
		setComponentAlignment(patomatLogo, Alignment.MIDDLE_RIGHT);
		setExpandRatio(layout, 1f);
	}
	
	private Component createNamingPatternView(){
		namingPatternsTable = new Table();
		namingPatternsTable.setCaption("Naming patterns");
		namingPatternsTable.setSizeFull();
//		namingPatternsTable.setHeight(null);
//		namingPatternsTable.setWidth("200px");
		namingPatternsTable.setPageLength(0);
		namingPatternsTable.setImmediate(true);
		namingPatternsTable.setSelectable(true);
		namingPatternsTable.setMultiSelect(false);
		namingPatternsTable.setStyleName("wordwrap-table");
		namingPatternsTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		namingPatternsTable.setColumnExpandRatio("pattern", 1f);
		namingPatternsTable.addContainerProperty("pattern", String.class, null);
		for(NamingPattern p : PatOMatPatternLibrary.getPattern()){
//			if(!p.isUseReasoning() || ((OWLOntologyKnowledgebase) ORESession.getKnowledgebaseManager().getKnowledgebase()).isCoherent()){
				namingPatternsTable.addItem(p).getItemProperty("pattern").setValue(p.getDescription());
//				namingPatternsTable.addItem(p.getImageFilename());
//				namingPatternsTable.setChildrenAllowed(p.getImageFilename(), false);
//				namingPatternsTable.setParent(p.getImageFilename(), p);
//			}
		}
		
		namingPatternsTable.addGeneratedColumn("pattern", new ColumnGenerator() {

			@Override
			public Component generateCell(final Table source, final Object itemId, Object columnId){
				if(itemId instanceof NamingPattern){
					final NamingPattern p = (NamingPattern)itemId;
					String s = "<b>" + p.getLabel() + "</b></br>";
					s += p.getDescription();
					VerticalLayout l = new VerticalLayout();
					Label label = new Label(s, ContentMode.HTML);
					l.addComponent(label);
					final Button more = new Button("More…");
					more.addStyleName("link");
			        l.addComponent(more);
			        more.addClickListener(new Button.ClickListener() {
			            @Override
			            public void buttonClick(ClickEvent event) {
			            	Resource res = new ThemeResource("img/" + p.getImageFilename());
							final Embedded object = new Embedded(p.getLabel(), res);
							object.setSizeFull();
							Window w = new Window();
							w.setWidth("800px");
							w.setHeight(UI.getCurrent().getPage().getBrowserWindowHeight()-100, Unit.PIXELS);
							VerticalLayout content = new VerticalLayout();
							content.setSizeFull();
							content.addComponent(object);
							w.setContent(content);
							getUI().addWindow(w);
							w.center();
							w.focus();
			            }
			        });
			        if(!p.isUseReasoning() || ((OWLOntologyKnowledgebase) ORESession.getKnowledgebaseManager().getKnowledgebase()).isCoherent()){
			        	l.addLayoutClickListener(new LayoutClickListener() {
							private static final long serialVersionUID = 1L;

							@Override
							public void layoutClick(final LayoutClickEvent event) {
								if (source.isSelected(itemId)) {
									source.unselect(itemId);
								} else {
									source.select(itemId);
								}
							}
						});
			        } else {
			        	l.setEnabled(false);
			        	l.setDescription("The selection of this pattern is not possible because there exist some unsatisfiable classes in the ontology which would influence the reasoning used for this pattern.");
			        }
					
					return l;
				}
				return null;
			}
		});
		
		namingPatternsTable.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				detectButton.setEnabled(namingPatternsTable.getValue() != null);
			}
		});
		
		return new WhitePanel(namingPatternsTable);
	}
	
	private Component createPatternInstancesView(){
		VerticalLayout view = new VerticalLayout();
		view.setCaption("Detected pattern instances");
		view.setSizeFull();
		
		//filter button to allow for omitting patterns instances with selected super classes
		PopupButton filterButton = new PopupButton("Pattern Instances Filter");
		filterButton.addStyleName("filterTable");
		filterButton.setDescription("Filter out pattern instances by super class.");
		filteredSuperclassesTable = new Table("Omit pattern instances with super class:");
		filteredSuperclassesTable.setStyleName("filterTable");
		filteredSuperclassesTable.setSizeFull();
		filteredSuperclassesTable.setImmediate(true);
		filteredSuperclassesTable.setPageLength(10);
		filteredSuperclassesTable.addContainerProperty("superclass", String.class, null);
		filteredSuperclassesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		filteredSuperclassesTable.setColumnExpandRatio("superclass", 1f);
		selectedSuperClasses = new TreeSet<String>();
		filteredSuperclassesTable.addGeneratedColumn("selected", new ColumnGenerator() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				CheckBox box = new CheckBox();
				box.setValue(selectedSuperClasses.contains(itemId));
				box.setImmediate(true);
				box.addValueChangeListener(new Property.ValueChangeListener() {
					@Override
					public void valueChange(Property.ValueChangeEvent event) {
						if((Boolean) event.getProperty().getValue()){
							selectedSuperClasses.add((String) itemId);
						} else {
							selectedSuperClasses.remove(itemId);
						}
					}
				});
				return box;
			}
		});
		filteredSuperclassesTable.setVisibleColumns(new String[] {"selected", "superclass"});
		filterButton.addPopupVisibilityListener(new PopupVisibilityListener() {
			
			@Override
			public void popupVisibilityChange(PopupVisibilityEvent event) {
				if(!event.isPopupVisible()){
					applySuperClassFilter();
				}
			}
		});
		filteredSuperclassesTable.setWidth("300px");
		filteredSuperclassesTable.setHeight("300px");
		filterButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				final Window filterDialog = new Window("Filter");
				filterDialog.setModal(true);
				VerticalLayout content = new VerticalLayout();
				filterDialog.setContent(content);
				content.addComponent(filteredSuperclassesTable);
				UI.getCurrent().addWindow(filterDialog);
				filterDialog.setClosable(true);
				Button applyButton = new Button("Apply");
				applyButton.addClickListener(new ClickListener() {
					
					@Override
					public void buttonClick(ClickEvent event) {
						applySuperClassFilter();
						filterDialog.close();
					}
				});
				content.addComponent(applyButton);
				content.setComponentAlignment(applyButton, Alignment.MIDDLE_CENTER);
				content.setExpandRatio(filteredSuperclassesTable, 1f);
			}
		});
		
//		filterButton.setContent(filteredSuperclassesTable);
		view.addComponent(filterButton);
		
		
		//pattern instances table
		patternInstancesTable = new Table();
//		patternInstancesTable.setCaption();
		patternInstancesTable.setSizeFull();
		patternInstancesTable.setImmediate(true);
		patternInstancesTable.setSelectable(true);
		patternInstancesTable.setMultiSelect(true);
		patternInstancesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		patternInstancesTable.addContainerProperty("pattern", String.class,  null);
		patternInstancesTable.addValueChangeListener(new Property.ValueChangeListener() {
		    public void valueChange(ValueChangeEvent event) {
		    	instructButton.setEnabled(true);
		    }
		});
		patternInstancesTable.addGeneratedColumn("pattern", new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				NamingIssue patternInstance = (NamingIssue) itemId;
				Renderer renderer = ORESession.getRenderer();
				String superClass = renderer.render(patternInstance.getSuperClass());
				String subClass = renderer.render(patternInstance.getSubClass());
				String s = "<b>Superclass</b>=" + superClass + "</br><b>Subclass</b>=" + subClass;
				Label l = new Label(s, ContentMode.HTML);
				return l;
			}
		});
		view.addComponent(patternInstancesTable);
		
		view.setSpacing(true);
		view.setExpandRatio(patternInstancesTable, 1f);
		
		return new WhitePanel(view);
	}
	

	private void applySuperClassFilter() {
		patternInstancesTable.removeAllItems();
		//show all pattern instances whose super class is not filtered out
		for (NamingIssue pi : detectedPatternInstances) {
			if(!selectedSuperClasses.contains(pi.getSuperClass())){
				patternInstancesTable.addItem(pi);
			}
		}
	}

	private Component createInstructionsView(){
		instructionsTable = new Table();
		instructionsTable.addStyleName("multiline");
		instructionsTable.setSizeFull();
		instructionsTable.setCaption("Renaming instructions");
		instructionsTable.setImmediate(true);
		instructionsTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
		selectedInstructions = new ArrayList<RenamingInstruction>();
		instructionsTable.addGeneratedColumn("selected", new ColumnGenerator() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				CheckBox box = new CheckBox();
				box.setValue(selectedInstructions.contains(itemId));
				box.setImmediate(true);
				box.addValueChangeListener(new Property.ValueChangeListener() {
					@Override
					public void valueChange(Property.ValueChangeEvent event) {
						if((Boolean) event.getProperty().getValue()){
							selectedInstructions.add((RenamingInstruction) itemId);
						} else {
							selectedInstructions.remove(itemId);
						}
						transformButton.setEnabled(!selectedInstructions.isEmpty());
					}
				});
				return box;
			}
		});
		instructionsTable.addGeneratedColumn("instruction", new ColumnGenerator() {
			final String template = "Rename <b>%s</b> to <b>%s</b>";
            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
            	
                RenamingInstruction ri = (RenamingInstruction) itemId;
                Renderer renderer = ORESession.getRenderer();
				String newURI = renderer.render(ri.getNewURI());
				String originalURI = renderer.render(ri.getOriginalURI());
                return new Label(String.format(template, originalURI, newURI), ContentMode.HTML);
            }
        });
		instructionsTable.addValueChangeListener(new Property.ValueChangeListener() {
		    public void valueChange(ValueChangeEvent event) {
		    	transformButton.setEnabled(true);
		    }
		});
		instructionsTable.setVisibleColumns(new Object[] {"selected", "instruction"});
		instructionsTable.setColumnExpandRatio("instruction", 1f);
		
		return new WhitePanel(instructionsTable);
	}
	
	private void onDetectNamingPattern(){
		currentNamingPattern = (NamingPattern) namingPatternsTable.getValue();
		if(currentNamingPattern == null){
			Notification.show("Please select a pattern from the table of the left side.");
		} else {
			new PatternDetectionProcess().start();
		}
	}
	
	private void onGenerateInstructions(){
		selectedPatternInstances = (Set<NamingIssue>) patternInstancesTable.getValue();
		if(selectedPatternInstances.isEmpty()){
			Notification.show("Please select some pattern instances.");
		} else {
			new InstructionsGenerationProcess().start();
		}
	}
	
	private void onTransform(){
		Set<OWLOntologyChange> changes = Sets.newLinkedHashSet();
		for(RenamingInstruction i : selectedInstructions){
			changes.add(new EntityRenaming(currentlyLoadedOntology, i.getOriginalURI(), i.getNewURI()));
		}
		ORESession.getKnowledgebaseManager().addChanges(changes);
		Notification.show("Transformation was successful.");
	}
	
	private void showGeneratingInstructions(boolean generating){
		instructionsTable.setEnabled(!generating);
		instructButton.setEnabled(!generating);
//		transformButton.setEnabled(!generating);
		
		if(generating){
			instructButton.addStyleName("disabled");
			instructButton.removeStyleName("borderless");
		} else {
			instructButton.removeStyleName("disabled");
			instructButton.addStyleName("borderless");
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
		OWLOntologyKnowledgebase kb = (OWLOntologyKnowledgebase) ORESession.getKnowledgebaseManager().getKnowledgebase();
		currentlyLoadedOntology = kb.getOntology();
	}
	
	private void updateSuperClassesFilter(){
		filteredSuperclassesTable.removeAllItems();
		Set<String> superClasses = new TreeSet<String>();
		for (NamingIssue patternInstance : detectedPatternInstances) {
			superClasses.add(patternInstance.getSuperClass());
		}
		Renderer renderer = ORESession.getRenderer();
		for (String sup : superClasses) {
			filteredSuperclassesTable.addItem(sup).getItemProperty("superclass").setValue(renderer.render(sup));
		}
	}
	
	public class PatternDetectionProcess extends Thread {
		
		public PatternDetectionProcess() {
			// TODO Auto-generated constructor stub
		}
		
	    @Override
	    public void run() {
	    	//show progress dialog
	    	final ProgressDialog dialog = new ProgressDialog("Detecting pattern instances...");
	    	UI.getCurrent().access(new Runnable() {
				
				@Override
				public void run() {
					UI.getCurrent().addWindow(dialog);
				}
			});
	    	
	    	try {
	    		//detect the pattern instances
	    		try {
	    			NamingPattern pattern = (NamingPattern) namingPatternsTable.getValue();
	    			int id = pattern.getID();
	    			switch(id){
	    				case 1:detectedPatternInstances = namingIssueDetection.detectNonExactMatchingDirectChildIssues(currentlyLoadedOntology);break;
	    				case 2:detectedPatternInstances = namingIssueDetection.detectNonMatchingChildIssues(currentlyLoadedOntology, true);break;
	    				case 3:detectedPatternInstances = namingIssueDetection.detectNonMatchingChildIssues(currentlyLoadedOntology, false);break;
	    			}
				} catch (Exception e) {
					logger.error("Naming issue detection failed.", e);
				}
				
				//show message if nothing was found or show the result
				UI.getCurrent().access(new Runnable() {

					@Override
					public void run() {
						UI.getCurrent().removeWindow(dialog);
						
						patternInstancesTable.removeAllItems();

						if (detectedPatternInstances.isEmpty()) {// if nothing was found
							Notification.show("Could not find any pattern instance.", Type.HUMANIZED_MESSAGE);
						} else {
							for (NamingIssue s : detectedPatternInstances) {
								patternInstancesTable.addItem(s);
							}
							patternInstancesTable.setEnabled(true);
						}
						
						//update the filter
						updateSuperClassesFilter();
					}
				});
			} catch (Exception e) {
				logger.error("Pattern instance detection failed.", e);
				UI.getCurrent().access(new Runnable() {
					@Override
					public void run() {
						UI.getCurrent().removeWindow(dialog);
						Notification.show("Pattern detection failed.", Type.ERROR_MESSAGE);
						patternInstancesTable.setEnabled(true);
					}
				});
			}
	    }
	  }
	
	public class InstructionsGenerationProcess extends Thread {
	    @Override
	    public void run() {
	    	final ProgressDialog dialog = new ProgressDialog("Generating transformation instructions...");
	    	UI.getCurrent().access(new Runnable() {
				@Override
				public void run() {
					UI.getCurrent().addWindow(dialog);
				}
			});
	    	
	    	generateInstructions();
	    	
	    	UI.getCurrent().access(new Runnable() {
				@Override
				public void run() {
					UI.getCurrent().removeWindow(dialog);
					instructionsTable.removeAllItems();
					for (RenamingInstruction i : generatedInstructions) {
						instructionsTable.addItem(i);
					}
				}
			});
	    }
	    
	    private void generateInstructions(){
	    	generatedInstructions = new ArrayList<RenamingInstruction>();
	    	for(NamingIssue pi : selectedPatternInstances){
				generatedInstructions.add(pi.getRenamingInstruction());
			}
	    }
	    }
	
	public void reset(){
		patternInstancesTable.removeAllItems();
		instructionsTable.removeAllItems();
		
		instructionsTable.setEnabled(false);
		instructButton.setEnabled(false);
		transformButton.setEnabled(false);
	}
	
	public void refreshRendering(){
		patternInstancesTable.refreshRowCache();
	}
}
