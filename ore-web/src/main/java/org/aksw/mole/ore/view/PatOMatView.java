package org.aksw.mole.ore.view;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

import org.aksw.mole.ore.PatOMatPatternLibrary;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.NamingPattern;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.aksw.mole.ore.model.RenamingInstruction;
import org.aksw.mole.ore.widget.LoadingIndicator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.vaadin.appfoundation.view.AbstractView;
import org.vaadin.overlay.CustomOverlay;
import org.vaadin.sasha.portallayout.PortalLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import cz.vse.keg.patomat.detection.OntologyPatternDetectionImpl;
import cz.vse.keg.patomat.transformation.OntologyTransformation;
import cz.vse.keg.patomat.transformation.OntologyTransformation.TransformationStrategy;
import cz.vse.keg.patomat.transformation.OntologyTransformationImpl;
import cz.vse.keg.patomat.transformation.pattern.InstructionGenerator;
import cz.vse.keg.patomat.transformation.pattern.InstructionGeneratorImpl;
import cz.vse.keg.patomat.transformation.pattern.TransformationPattern;
import cz.vse.keg.patomat.transformation.pattern.TransformationPatternImpl;

public class PatOMatView extends AbstractView<VerticalLayout>{
	
	private TreeTable namingPatternsTable;
	private Table patternInstancesTable;
	private Table instructionsTable;
	private BeanItemContainer<NamingPattern> namingPatternsContainer;
	private BeanItemContainer<RenamingInstruction> instructionsContainer;
	
	private Button detectButton;
	private Button instructButton;
	private Button transformButton;
	
	TransformationPattern tp1;
	OntologyTransformation<OWLOntology> transformation;
	NamingPattern currentNamingPattern;
	String ontologyURI = "http://nb.vse.cz/~svabo/oaei2011/data/confOf.owl";
	private OWLOntology currentlyLoadedOntology;
	Document doc;
	
	private GridLayout layout;
	
	VerticalLayout wrapper;
	VerticalLayout instructionsWrapper;
	
	Refresher ref;
	Refresher instructionsRef;
	
	private ArrayList<String> detectedPatternInstances;
	private List<RenamingInstruction> generatedInstructions;
	Set<String> selectedPatternInstances;
	
	private int nrOfSelectedInstructions = 0;

	public PatOMatView() {
		super(new VerticalLayout());
		initUI();
	}
	
	private void initUI(){
		getContent().setSizeFull();
		
		layout = new GridLayout(6, 1);
		layout.setSizeFull();
		layout.setHeight("80%");
		getContent().addComponent(layout);
		getContent().setComponentAlignment(layout, Alignment.MIDDLE_CENTER);

		layout.addComponent(createNamingPatternView(), 0, 0);
		layout.addComponent(createPatternInstancesView(), 2, 0);
		layout.addComponent(createInstructionsView(), 4, 0);
		
		detectButton = new NativeButton();
		detectButton.setStyleName("borderless detect");
//		detectButton.setWidth("128px");
//		detectButton.setHeight("128px");
		detectButton.setWidth("100px");
		detectButton.setHeight("100px");
		detectButton.addListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onDetectNamingPattern();
			}
		});
		layout.addComponent(detectButton, 1, 0);
		layout.setComponentAlignment(detectButton, Alignment.MIDDLE_CENTER);
		
		instructButton = new NativeButton();
		instructButton.setStyleName("borderless instruct");
		instructButton.setWidth("100px");
		instructButton.setHeight("100px");
		instructButton.addListener(new Button.ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onGenerateInstructions();
			}
		});
		layout.addComponent(instructButton, 3, 0);
		layout.setComponentAlignment(instructButton, Alignment.MIDDLE_CENTER);
		
		transformButton = new NativeButton();
		transformButton.setStyleName("borderless execute");
		transformButton.setWidth("100px");
		transformButton.setHeight("100px");
		transformButton.addListener(new Button.ClickListener() {
			
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
		
		instructionsTable.setEnabled(false);
		instructButton.setEnabled(false);
		transformButton.setEnabled(false);
		
	}
	
	private Component createNamingPatternView(){
		PortalLayout portal = new PortalLayout();
		portal.setSizeFull();
		
//		namingPatternsTable = new Table("Naming patterns");
//		namingPatternsTable.setCaption("Naming patterns");
//		namingPatternsTable.setSizeFull();
//		namingPatternsTable.setSelectable(true);
//		namingPatternsTable.setMultiSelect(true);
//		namingPatternsTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
//		namingPatternsContainer = new BeanItemContainer<NamingPattern>(NamingPattern.class);
//		namingPatternsContainer.addAll(createPatternList());
//		namingPatternsTable.setContainerDataSource(namingPatternsContainer);
//		namingPatternsTable.setVisibleColumns(new String[] {"label"});
		
		namingPatternsTable = new TreeTable();
		namingPatternsTable.setCaption("Naming patterns");
		namingPatternsTable.setSizeFull();
		namingPatternsTable.setSelectable(true);
		namingPatternsTable.setMultiSelect(true);
		namingPatternsTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
		for(NamingPattern p : PatOMatPatternLibrary.getPattern()){
			if(!p.isUseReasoning() || ((OWLOntologyKnowledgebase) UserSession.getKnowledgebaseManager().getKnowledgebase()).isCoherent()){
				namingPatternsTable.addItem(p);
				namingPatternsTable.addItem(p.getUrl());
				namingPatternsTable.setChildrenAllowed(p.getUrl(), false);
				namingPatternsTable.setParent(p.getUrl(), p);
			}
		}
		namingPatternsTable.addGeneratedColumn("generated", new ColumnGenerator() {

			@Override
			public Component generateCell(Table source, final Object itemId, Object columnId){

			    // Get the object associated with the row
			    Object obj = source.getItem(itemId);

			    //Missing casting instruction

			    try {
					if(itemId instanceof URL){
						InputStream is = ((URL) itemId).openConnection().getInputStream();
						Writer writer = null;
						if (is != null) {
					        writer = new StringWriter();
 
					        char[] buffer = new char[1024];
					        try {
					            Reader reader = new BufferedReader(
					                    new InputStreamReader(is, "UTF-8"));
					            int n;
					            while ((n = reader.read(buffer)) != -1) {
					                writer.write(buffer, 0, n);
					            }
					        } finally {
					            is.close();
					        }
					    }
						Label l = new Label(writer.toString(), Label.CONTENT_PREFORMATTED);
					   return l;
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			    return new Label(((NamingPattern)itemId).getLabel());
			}
		});
		portal.addComponent(namingPatternsTable);
		
		
		return portal;
	}
	
	private Component createPatternInstancesView(){
		PortalLayout portal = new PortalLayout();
		
		wrapper = new VerticalLayout();
		wrapper.setSizeFull();
		
		patternInstancesTable = new Table();
//		patternInstancesTable.setCaption("Detected pattern instances");
		patternInstancesTable.setSizeFull();
		patternInstancesTable.setImmediate(true);
		patternInstancesTable.setSelectable(true);
		patternInstancesTable.setMultiSelect(true);
		patternInstancesTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
		patternInstancesTable.addContainerProperty("Pattern", String.class,  null);
		patternInstancesTable.addListener(new Property.ValueChangeListener() {
		    public void valueChange(ValueChangeEvent event) {
		    	instructButton.setEnabled(true);
		    }
		});
		
		wrapper.addComponent(patternInstancesTable);
		wrapper.setExpandRatio(patternInstancesTable, 1f);
		ref = new Refresher();
		ref.setRefreshInterval(0);
		wrapper.addComponent(ref);
		portal.addComponent(wrapper);
		portal.setComponentCaption(wrapper, "Detected pattern instances");
		
		return portal;
	}
	
	private Component createInstructionsView(){
		PortalLayout portal = new PortalLayout();
		
		instructionsTable = new Table();
		instructionsTable.addStyleName("multiline");
		instructionsTable.setSizeFull();
//		instructionsTable.setCaption("Renaming instructions");
		instructionsTable.setImmediate(true);
		instructionsTable.setColumnHeaderMode(Table.COLUMN_HEADER_MODE_HIDDEN);
		instructionsContainer = new BeanItemContainer<RenamingInstruction>(RenamingInstruction.class);
		instructionsTable.addGeneratedColumn("selected", new ColumnGenerator() {

            @Override
            public Component generateCell(final Table source, final Object itemId, final Object columnId) {
            	
                final RenamingInstruction bean = (RenamingInstruction) itemId;

                final CheckBox checkBox = new CheckBox();
                checkBox.setImmediate(true);
                checkBox.addListener(new Property.ValueChangeListener() {
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

                return new Label(bean.getNLRepresentationHTML(), Label.CONTENT_XHTML);
            }
        });
		instructionsTable.addListener(new Property.ValueChangeListener() {
		    public void valueChange(ValueChangeEvent event) {
		    	transformButton.setEnabled(true);
		    }
		});
		instructionsTable.setContainerDataSource(instructionsContainer);
		instructionsTable.setVisibleColumns(new String[] {"selected", "instruction"});
		instructionsTable.setColumnWidth("selected", 30);
		
//		portal.addComponent(instructionsTable);
		
		instructionsWrapper = new VerticalLayout();
		instructionsWrapper.setSizeFull();
		instructionsWrapper.addComponent(instructionsTable);
		instructionsWrapper.setExpandRatio(instructionsTable, 1f);
		instructionsRef = new Refresher();
		instructionsRef.setRefreshInterval(0);
		instructionsWrapper.addComponent(instructionsRef);
		
		portal.addComponent(instructionsWrapper);
		portal.setComponentCaption(instructionsWrapper, "Renaming instructions");
		
		
		return portal;
	}
	
	private void onDetectNamingPattern(){
//		final ImageOverlay overlay = new ImageOverlay(patternInstancesTable, loadingIcon);
		final CustomOverlay overlay = new CustomOverlay(new LoadingIndicator("Detecting pattern instances"), patternInstancesTable);
		wrapper.addComponent(overlay);
		overlay.setComponentAnchor(Alignment.MIDDLE_CENTER);
//		overlay.setOverlayAnchor(Alignment.MIDDLE_CENTER);
		overlay.setXOffset(-10);
		overlay.setYOffset(-10);
		
		Set<NamingPattern> np = (Set<NamingPattern>) namingPatternsTable.getValue();
		if(np.isEmpty()){
			getApplication().getMainWindow().showNotification("Please select a pattern from the table of the left side.");
		} else {
			detectedPatternInstances = null;
			ref.setEnabled(true);
			showDetectingNamingPattern(true);
			ref.setRefreshInterval(500);
			
			currentNamingPattern = np.iterator().next();
			ref.addListener(new RefreshListener() {
				
				@Override
				public void refresh(Refresher source) {
					if (detectedPatternInstances != null) {
						wrapper.removeComponent(overlay);
						showDetectingNamingPattern(false);
				        // stop polling
				        source.setEnabled(false);
				        
				       for(String s : detectedPatternInstances){
				    	   patternInstancesTable.addItem(new Object[]{s}, s);
				       }
				      }
					
				}
			});
			new PatternDetectionProcess().start();
				
		}
			 
	}
	
	private void onGenerateInstructions(){
		final CustomOverlay overlay = new CustomOverlay(new LoadingIndicator("Generating instructions..."), instructionsTable);
		instructionsWrapper.addComponent(overlay);
		overlay.setComponentAnchor(Alignment.MIDDLE_CENTER);
		
		instructionsContainer.removeAllItems();
		selectedPatternInstances = (Set<String>) patternInstancesTable.getValue();
		if(selectedPatternInstances.isEmpty()){
			getApplication().getMainWindow().showNotification("Please select some pattern instances.");
		} else {
			generatedInstructions = null;
			instructionsRef.setEnabled(true);
			showGeneratingInstructions(true);
			instructionsRef.setRefreshInterval(500);
			
			instructionsRef.addListener(new RefreshListener() {
				
				@Override
				public void refresh(Refresher source) {
					if (generatedInstructions != null) {
						instructionsWrapper.removeComponent(overlay);
						showGeneratingInstructions(false);
				        // stop polling
				        source.setEnabled(false);
				        
				        instructionsContainer.addAll(generatedInstructions);
				      }
					
				}
			});
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
		for(RenamingInstruction i : instructionsContainer.getItemIds()){
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
			String instructions = result.getWriter().toString();
			transformation.setInstructions(instructions);
			transformation.transformOntology(TransformationStrategy.Progressive, true);
			getWindow().showNotification("Transformation was successful.");
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
	
	
	private void showDetectingNamingPattern(boolean detecting){
		patternInstancesTable.setEnabled(!detecting);
		detectButton.setEnabled(!detecting);
//		instructionsTable.setEnabled(!detecting);
//		instructButton.setEnabled(!detecting);
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


	@Override
	public void activated(Object... params) {
		OWLOntologyKnowledgebase kb = (OWLOntologyKnowledgebase) UserSession.getKnowledgebaseManager().getKnowledgebase();
		currentlyLoadedOntology = kb.getOntology();
		ontologyURI = currentlyLoadedOntology.getOWLOntologyManager().getOntologyDocumentIRI(currentlyLoadedOntology).toString();
		namingPatternsTable.removeAllItems();
		for(NamingPattern p : PatOMatPatternLibrary.getPattern()){
			if(!p.isUseReasoning() || ((OWLOntologyKnowledgebase) UserSession.getKnowledgebaseManager().getKnowledgebase()).isCoherent()){
				namingPatternsTable.addItem(p);
				namingPatternsTable.addItem(p.getUrl());
				namingPatternsTable.setChildrenAllowed(p.getUrl(), false);
				namingPatternsTable.setParent(p.getUrl(), p);
			}
		}
	}

	@Override
	public void deactivated(Object... params) {
		// TODO Auto-generated method stub
		
	}
	
	public class PatternDetectionProcess extends Thread {
		
		public PatternDetectionProcess() {
			// TODO Auto-generated constructor stub
		}
		
	    @Override
	    public void run() {
	    	detectedPatternInstances = detectPatternInstances();
	    }
	    
	    private ArrayList<String> detectPatternInstances() {
	    	tp1 = new TransformationPatternImpl(currentNamingPattern.getUrl().toString());
			//pattern detection
			transformation = new OntologyTransformationImpl(currentlyLoadedOntology, UserSession.getWordNetDirectory(), UserSession.getPosTaggerModelsDirectory(), "/");
			OntologyPatternDetectionImpl detection = new OntologyPatternDetectionImpl(transformation.getDictionaryPath(), transformation.getModelsPath());		
					
			//detection and transformation instructions generation:
			return detection.queryPatternStructuralAspect2(tp1, ontologyURI, false, currentNamingPattern.isUseReasoning());
	    }
	  }
	
	public class InstructionsGenerationProcess extends Thread {
	    @Override
	    public void run() {
	    	generatedInstructions = generateInstructions();
	    }
	    
	    private List<RenamingInstruction> generateInstructions(){
			List<RenamingInstruction> instructions = new ArrayList<RenamingInstruction>();
			boolean POStagger=true;		
			//generate the instructions
			InstructionGenerator ig = new InstructionGeneratorImpl(tp1,POStagger,transformation.getDictionaryPath(),transformation.getModelsPath());
			ig.generateGeneralTransformationInstructions();
			for(String pi : selectedPatternInstances){
				ig.generateInstantiatedInstructions(ig.parseXMLpatternInstanceBinding(OntologyPatternDetectionImpl.outputOnePairXML(ontologyURI, currentNamingPattern.getUrl().toString(), pi)), true);
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
				
//				//remove OPPL part of XML 
//				//TODO make it in a better way
//				xpath = XPathFactory.newInstance().newXPath();
//			    expression = "/instructions/oppl_script";
//				nodesOP = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
//				Node opplNode = nodesOP.item(0);
//				opplNode.getParentNode().removeChild(opplNode);
				
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
		ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		String ontologyURI = "http://nb.vse.cz/~svabo/oaei2011/data/confOf.owl";
		String namingPattern = "http://nb.vse.cz/~svabo/patomat/tp/np/tp_np1b.xml";
		OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntology(IRI.create(ontologyURI));
		TransformationPattern tp1 = new TransformationPatternImpl(namingPattern);
		//pattern detection
		OntologyTransformation<OWLOntology> transformation = new OntologyTransformationImpl(ontology, "src/main/resources/wordnet", "src/main/resources/postagger", "/tmp/");//UserSession.getOntology());
		OntologyPatternDetectionImpl detection = new OntologyPatternDetectionImpl(transformation.getDictionaryPath(), transformation.getModelsPath());	
		ArrayList<String> pattern = detection.queryPatternStructuralAspect2(tp1, ontologyURI, false, false);
		
		List<String> selectedPattern = new ArrayList<String>();
		selectedPattern.add(pattern.get(0));
		selectedPattern.add(pattern.get(1));
		
		List<RenamingInstruction> instructions = new ArrayList<RenamingInstruction>();
		boolean POStagger=true;		
		//generate the instructions
		InstructionGenerator ig = new InstructionGeneratorImpl(tp1,POStagger,transformation.getDictionaryPath(),transformation.getModelsPath());
		ig.generateGeneralTransformationInstructions();
		for(String pi : selectedPattern){
			ig.generateInstantiatedInstructions(ig.parseXMLpatternInstanceBinding(OntologyPatternDetectionImpl.outputOnePairXML(ontologyURI, namingPattern, pi)), true);
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
			
			//remove OPPL part of XML 
			//TODO make it in a better way
			xpath = XPathFactory.newInstance().newXPath();
		    expression = "/instructions/oppl_script";
			nodesOP = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
			Node opplNode = nodesOP.item(0);
			opplNode.getParentNode().removeChild(opplNode);
			
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
				for(RenamingInstruction i : selectedInstructions){
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
					System.out.println(transformedOntology.getLogicalAxiomCount());
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
