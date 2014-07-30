package org.aksw.ore.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.aksw.mole.ore.naming.NamingIssueDetection;
import org.aksw.ore.OREConfiguration;
import org.aksw.ore.ORESession;
import org.aksw.ore.component.ProgressDialog;
import org.aksw.ore.component.WhitePanel;
import org.aksw.ore.manager.KnowledgebaseManager;
import org.aksw.ore.model.EntityRenaming;
import org.aksw.ore.model.NamingPattern;
import org.aksw.ore.model.NamingPatternInstance;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.model.RenamingInstruction;
import org.aksw.ore.util.PatOMatPatternLibrary;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.hene.popupbutton.PopupButton;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityEvent;
import org.vaadin.hene.popupbutton.PopupButton.PopupVisibilityListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import cz.vse.keg.patomat.detection.OntologyPatternDetectionImpl;
import cz.vse.keg.patomat.transformation.OntologyTransformation;
import cz.vse.keg.patomat.transformation.OntologyTransformation.TransformationStrategy;
import cz.vse.keg.patomat.transformation.OntologyTransformationImpl;
import cz.vse.keg.patomat.transformation.pattern.InstructionGenerator;
import cz.vse.keg.patomat.transformation.pattern.InstructionGeneratorImpl;
import cz.vse.keg.patomat.transformation.pattern.TransformationPattern;
import cz.vse.keg.patomat.transformation.pattern.TransformationPatternImpl;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class NamingViewOld extends VerticalLayout implements View{
	
	private static final Logger logger = LoggerFactory.getLogger(NamingViewOld.class);
	
	private Table namingPatternsTable;
	private Table patternInstancesTable;
	private Table instructionsTable;
	private BeanItemContainer<RenamingInstruction> instructionsContainer;
	
	private Button detectButton;
	private Button instructButton;
	private Button transformButton;
	
	TransformationPattern tp1;
	OntologyTransformation<OWLOntology> transformation;
	private NamingPattern currentNamingPattern;
	private String currentOntologyURI;
	private OWLOntology currentlyLoadedOntology;
	Document doc;
	
	private GridLayout layout;
	
	private Set<NamingPatternInstance> detectedPatternInstances;
	private Set<NamingPatternInstance> selectedPatternInstances;
	
	private List<RenamingInstruction> generatedInstructions;
	
	
	private int nrOfSelectedInstructions = 0;

	private Table filteredSuperclassesTable;

	private Set<String> selectedSuperClasses;

	public NamingViewOld() {
		initUI();
	}
	
	private void initUI(){
		setSizeFull();
		setMargin(true);
		addStyleName("dashboard-view");
		addStyleName("naming-view");
		
		layout = new GridLayout(6, 1);
		layout.setSizeFull();
		addComponent(layout);
		setComponentAlignment(layout, Alignment.MIDDLE_CENTER);

		layout.addComponent(createNamingPatternView(), 0, 0);
		layout.addComponent(createPatternInstancesView(), 2, 0);
		layout.addComponent(createInstructionsView(), 4, 0);
		
		detectButton = new Button("Detect");
		detectButton.addStyleName("icon-detect");
		detectButton.setWidth(null);
		detectButton.setEnabled(false);
		detectButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onDetectNamingPattern();
			}
		});
		layout.addComponent(detectButton, 1, 0);
		layout.setComponentAlignment(detectButton, Alignment.MIDDLE_CENTER);
		
		instructButton = new Button("Instruct");
		instructButton.addStyleName("icon-detect");
		instructButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onGenerateInstructions();
			}
		});
		layout.addComponent(instructButton, 3, 0);
		layout.setComponentAlignment(instructButton, Alignment.MIDDLE_CENTER);
		
		transformButton = new Button("Transform");
		transformButton.addStyleName("icon-transform");
		transformButton.addClickListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onTransform();
			}
		});
		layout.addComponent(transformButton, 5, 0);
		layout.setComponentAlignment(transformButton, Alignment.MIDDLE_CENTER);
		
		layout.setColumnExpandRatio(0, 0.3f);
		layout.setColumnExpandRatio(2, 0.3f);
		layout.setColumnExpandRatio(4, 0.3f);
		layout.setRowExpandRatio(0, 1f);
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
		PopupButton filterButton = new PopupButton("Filter");
		filterButton.setDescription("Filter out pattern instances by super class.");
		filteredSuperclassesTable = new Table("Omit pattern instances with super classes:");
		filteredSuperclassesTable.setStyleName("filterTable");
		filteredSuperclassesTable.setSizeFull();
		filteredSuperclassesTable.setImmediate(true);
		filteredSuperclassesTable.setPageLength(10);
		filteredSuperclassesTable.addContainerProperty("superclass", String.class, null);
		filteredSuperclassesTable.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
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
		filterButton.setContent(filteredSuperclassesTable);
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
				NamingPatternInstance patternInstance = (NamingPatternInstance) itemId;
				String s = "<b>Superclass</b>=" + patternInstance.getSuperClass() + "</br><b>Subclass</b>=" + patternInstance.getSubClass();
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
		for (NamingPatternInstance pi : detectedPatternInstances) {
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
		instructionsContainer = new BeanItemContainer<RenamingInstruction>(RenamingInstruction.class);
		instructionsTable.addGeneratedColumn("selected", new ColumnGenerator() {

            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
            	
                final RenamingInstruction bean = (RenamingInstruction) itemId;

                final CheckBox checkBox = new CheckBox();
                checkBox.setImmediate(true);
                checkBox.addValueChangeListener(new Property.ValueChangeListener() {
                    @Override
                    public void valueChange(final ValueChangeEvent event) {
                        bean.setSelected((Boolean) event.getProperty().getValue());
                        if((Boolean) event.getProperty().getValue()){
                        	nrOfSelectedInstructions++;
                        } else {
                        	nrOfSelectedInstructions--;
                        }
                        transformButton.setEnabled(nrOfSelectedInstructions > 0);
                        
                    }
                });

                if (bean.isSelected()) {
                    checkBox.setValue(true);
                } else {
                    checkBox.setValue(false);
                }
                return checkBox;
            }
        });
		instructionsTable.addGeneratedColumn("instruction", new ColumnGenerator() {

            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
            	
                final RenamingInstruction bean = (RenamingInstruction) itemId;

                return new Label(bean.getNLRepresentationHTML(), ContentMode.HTML);
            }
        });
		instructionsTable.addValueChangeListener(new Property.ValueChangeListener() {
		    public void valueChange(ValueChangeEvent event) {
		    	transformButton.setEnabled(true);
		    }
		});
		instructionsTable.setContainerDataSource(instructionsContainer);
		instructionsTable.setVisibleColumns(new Object[] {"selected", "instruction"});
		instructionsTable.setColumnWidth("selected", 30);
		
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
		instructionsContainer.removeAllItems();
		selectedPatternInstances = (Set<NamingPatternInstance>) patternInstancesTable.getValue();
		if(selectedPatternInstances.isEmpty()){
			Notification.show("Please select some pattern instances.");
		} else {
			new InstructionsGenerationProcess().start();
		}
	}
	
	private void onTransform(){
		//get the parent node of all renaming nodes
		String expression = "/instructions/entities";
		Node parent = null;
	    try {
			NodeList nodesOP;
			XPath xpath = XPathFactory.newInstance().newXPath();
			nodesOP = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
			parent = nodesOP.item(0);
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
	    //remove all renaming instance nodes not selected
	    Set<OWLOntologyChange> changes = Sets.newLinkedHashSet();
		for(RenamingInstruction i : instructionsContainer.getItemIds()){
			if(!i.isSelected()){
				parent.removeChild(i.getNode());
			} else {
				changes.add(new EntityRenaming(currentlyLoadedOntology, i.getOriginalName(), i.getNewName()));
			}
		}
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
			String instructions = result.getWriter().toString();
			transformation.setInstructions(instructions);
			OWLOntology transformOntology = transformation.transformOntology(TransformationStrategy.Progressive, true);
			try {
				transformOntology.getOWLOntologyManager().saveOntology(transformOntology, new FileOutputStream("/home/me/renamed.owl"));
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			ORESession.getKnowledgebaseManager().addChanges(changes);
			Notification.show("Transformation was successful.");
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
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
		currentOntologyURI = currentlyLoadedOntology.getOWLOntologyManager().getOntologyDocumentIRI(currentlyLoadedOntology).toString();
//		namingPatternsTable.removeAllItems();
//		for(NamingPattern p : PatOMatPatternLibrary.getPattern()){
//			if(!p.isUseReasoning() || ((OWLOntologyKnowledgebase) ORESession.getKnowledgebaseManager().getKnowledgebase()).isCoherent()){
//				namingPatternsTable.addItem(p);
//				namingPatternsTable.addItem(p.getUrl());
//				namingPatternsTable.setChildrenAllowed(p.getUrl(), false);
//				namingPatternsTable.setParent(p.getUrl(), p);
//			}
//		}
	}
	
	private void updateSuperClassesFilter(){
		filteredSuperclassesTable.removeAllItems();
		Set<String> superClasses = new TreeSet<String>();
		for (NamingPatternInstance patternInstance : detectedPatternInstances) {
			superClasses.add(patternInstance.getSuperClass());
		}
		for (String sup : superClasses) {
			filteredSuperclassesTable.addItem(sup).getItemProperty("superclass").setValue(sup);
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
				detectedPatternInstances = detectPatternInstances();
				
				//show message if nothing was found or show the result
				UI.getCurrent().access(new Runnable() {

					@Override
					public void run() {
						UI.getCurrent().removeWindow(dialog);
						
						patternInstancesTable.removeAllItems();

						if (detectedPatternInstances.isEmpty()) {// if nothing was found
							Notification.show("Could not find any pattern instance.", Type.HUMANIZED_MESSAGE);
						} else {
							for (NamingPatternInstance s : detectedPatternInstances) {
								patternInstancesTable.addItem(s);
							}
							patternInstancesTable.setEnabled(true);
						}
						
						//update the filter
						updateSuperClassesFilter();
					}
				});
			} catch (Exception e) {
				logger.error("Pattern detection failed.", e);
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
	    
	    private Set<NamingPatternInstance> detectPatternInstances() {
	    	tp1 = new TransformationPatternImpl(currentNamingPattern.getUrl().toString());
			//pattern detection
			transformation = new OntologyTransformationImpl(
					currentlyLoadedOntology, 
					OREConfiguration.getWordNetDirectory(), 
					OREConfiguration.getPosTaggerModelsDirectory(), "/");
			OntologyPatternDetectionImpl detection = new OntologyPatternDetectionImpl(transformation.getDictionaryPath(), transformation.getModelsPath());		
					
			//detection and transformation instructions generation:
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OWLOntologyManager man = currentlyLoadedOntology.getOWLOntologyManager();
			try {
				man.saveOntology(currentlyLoadedOntology, new RDFXMLOntologyFormat(), baos);
				InputStream is = new ByteArrayInputStream(baos.toByteArray());
				ArrayList<String> patomatPatternInstances = detection.queryPatternStructuralAspect2(tp1, is, false, currentNamingPattern.isUseReasoning());
				Set<NamingPatternInstance> patternInstances = new TreeSet<NamingPatternInstance>();
				for (String pi : patomatPatternInstances) {
					patternInstances.add(new NamingPatternInstance(pi));
				}
				logger.info("Pattern instances: " + patternInstances);
				return patternInstances;
			} catch (Exception e) {
				logger.error("Naming issue detection failed.", e);
			}
			return new HashSet<NamingPatternInstance>();
//			return detection.queryPatternStructuralAspect2(tp1, currentOntologyURI, false, currentNamingPattern.isUseReasoning());
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
	    	generatedInstructions = generateInstructions();
	    	UI.getCurrent().access(new Runnable() {
				
				@Override
				public void run() {
					UI.getCurrent().removeWindow(dialog);
					instructionsContainer.addAll(generatedInstructions);
				}
			});
	    }
	    
	    private List<RenamingInstruction> generateInstructions(){
			List<RenamingInstruction> instructions = new ArrayList<RenamingInstruction>();
			boolean POStagger=true;		
			//generate the instructions
			InstructionGenerator ig = new InstructionGeneratorImpl(tp1,POStagger,transformation.getDictionaryPath(),transformation.getModelsPath());
			ig.generateGeneralTransformationInstructions();
			for(NamingPatternInstance pi : selectedPatternInstances){
				ig.generateInstantiatedInstructions(ig.parseXMLpatternInstanceBinding(OntologyPatternDetectionImpl.outputOnePairXML(currentOntologyURI, currentNamingPattern.getUrl().toString(), pi.asPatOMatString())), true);
			}
			//get the instructions as XML
			String instructionXML = ig.exportTransformationInstructions(false);
			//process the XML and generate some user-friendly representation while keeping the reference to the corresponding node in the XML document
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			    dbf.setNamespaceAware(true); 
				DocumentBuilder db = dbf.newDocumentBuilder();
				doc = db.parse(new ByteArrayInputStream(instructionXML.getBytes()));
				XPath xpath = XPathFactory.newInstance().newXPath();
			    String expression = "/instructions/entities/rename";
			    NodeList nodesOP;
				nodesOP = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
				Node currentNode;
				for(int i=0;i<nodesOP.getLength();i++) {				
					currentNode = nodesOP.item(i);
					String original_name=((Element)currentNode).getAttribute("original_name");
					String new_name=((Element)nodesOP.item(i)).getTextContent();
					if (!original_name.equals(new_name)) {
						instructions.add(new RenamingInstruction(original_name, new_name, currentNode));
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
			return instructions;
		}
	  }
	
	public void reset(){
		patternInstancesTable.removeAllItems();
		instructionsTable.removeAllItems();
		
		instructionsTable.setEnabled(false);
		instructButton.setEnabled(false);
		transformButton.setEnabled(false);
	}
	
	public static void main(String[] args) throws Exception{
		
		String ontologyURL = "localhost:";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLOntology ontology = man.loadOntologyFromOntologyDocument(new FileInputStream(new File("/home/me/work/projects/DL-Learner/examples/carcinogenesis/carcinogenesis.owl")));
		ontology = man.loadOntology(IRI.create("http://xmlns.com/foaf/spec/20100809.rdf"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		man.saveOntology(ontology, new RDFXMLOntologyFormat(), baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		
		KnowledgebaseManager kbMan = new KnowledgebaseManager();
		kbMan.setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
		
//		String ontologyURI = "http://nb.vse.cz/~svabo/oaei2011/data/confOf.owl";
		String namingPattern = "http://nb.vse.cz/~svabo/patomat/tp/np/tp_np1a.xml";
//		OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntology(IRI.create(ontologyURI));
		TransformationPattern tp1 = new TransformationPatternImpl(namingPattern);
		//pattern detection
		OntologyTransformation<OWLOntology> transformation = new OntologyTransformationImpl(ontology, "/opt/ore/wordnet", "/opt/ore/postagger", "/tmp/");//UserSession.getOntology());
		OntologyPatternDetectionImpl detection = new OntologyPatternDetectionImpl(transformation.getDictionaryPath(), transformation.getModelsPath());	
		ArrayList<String> pattern = detection.queryPatternStructuralAspect2(tp1, bais, false, false);
		
		List<String> selectedPattern = new ArrayList<String>();
		selectedPattern.add(pattern.get(0));
		selectedPattern.add(pattern.get(1));
		
		List<RenamingInstruction> instructions = new ArrayList<RenamingInstruction>();
		boolean POStagger=true;		
		//generate the instructions
		InstructionGenerator ig = new InstructionGeneratorImpl(tp1,POStagger,transformation.getDictionaryPath(),transformation.getModelsPath());
		ig.generateGeneralTransformationInstructions();
		for(String pi : selectedPattern){
			ig.generateInstantiatedInstructions(ig.parseXMLpatternInstanceBinding(OntologyPatternDetectionImpl.outputOnePairXML("ont", namingPattern, pi)), true);
		}
		//get the instructions as XML
		String instructionXML = ig.exportTransformationInstructions(false);
		//process the XML and generate some user-friendly representation while keeping the reference to the corresponding node in the XML document
		Document doc = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		    dbf.setNamespaceAware(true); 
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(new ByteArrayInputStream(instructionXML.getBytes()));
			XPath xpath = XPathFactory.newInstance().newXPath();
		    String expression = "/instructions/entities/rename";
		    NodeList nodesOP;
			nodesOP = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
			Node currentNode;
			for(int i=0;i<nodesOP.getLength();i++) {				
				currentNode = nodesOP.item(i);
				String original_name=((Element)currentNode).getAttribute("original_name");
				String new_name=((Element)nodesOP.item(i)).getTextContent();
				if (!original_name.equals(new_name)) {
					instructions.add(new RenamingInstruction(original_name, new_name, currentNode));
				}
			}
			
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		List<RenamingInstruction> selectedInstructions = new ArrayList<RenamingInstruction>();
		selectedInstructions.add(instructions.get(0));
		
		//get the parent node of all renaming nodes
				String expression = "/instructions/entities";
				Node parent = null;
			    try {
					NodeList nodesOP;
					XPath xpath = XPathFactory.newInstance().newXPath();
					nodesOP = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
					parent = nodesOP.item(0);
				} catch (XPathExpressionException e1) {
					e1.printStackTrace();
				}
			    //remove all renaming instance nodes not selected
				for(RenamingInstruction i : selectedInstructions){System.out.println(i);
					if(!i.isSelected()){
						parent.removeChild(i.getNode());
					}
				}
				try {
					Transformer transformer = TransformerFactory.newInstance().newTransformer();
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					//initialize StreamResult with File object to save to file
					StreamResult result = new StreamResult(new StringWriter());
					DOMSource source = new DOMSource(doc);
					transformer.transform(source, result);
					String instructionsString = result.getWriter().toString();
					transformation.setInstructions(instructionsString);
					OWLOntology transformedOntology = transformation.transformOntology(TransformationStrategy.Progressive, true);
					transformation.saveOntology("patomat_test.owl");
					transformedOntology = transformation.getOntology();
					man.saveOntology(ontology, new FileOutputStream("renamed.owl"));
					man.saveOntology(transformedOntology, new FileOutputStream("renamed1.owl"));
				} catch (TransformerConfigurationException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (TransformerFactoryConfigurationError e) {
					e.printStackTrace();
				} catch (TransformerException e) {
					e.printStackTrace();
				}
	}

}
