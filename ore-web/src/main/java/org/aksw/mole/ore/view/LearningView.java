package org.aksw.mole.ore.view;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.aksw.mole.ore.LearningManager;
import org.aksw.mole.ore.OREApplication;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.Individual;
import org.aksw.mole.ore.util.Renderer;
import org.aksw.mole.ore.util.Renderer.Syntax;
import org.aksw.mole.ore.widget.IndividualsTable;
import org.aksw.mole.ore.widget.LearningOptionsPanel;
import org.dllearner.core.owl.Description;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.core.owl.Thing;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.vaadin.appfoundation.view.AbstractView;
import org.vaadin.sasha.portallayout.PortalLayout;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

public class LearningView extends AbstractView<HorizontalSplitPanel>{
	
	private PortalLayout mainPortal = new PortalLayout();
	private PortalLayout posExamplesPortal = new PortalLayout();
	private PortalLayout negExamplesPortal = new PortalLayout();
	private PortalLayout rightPortal = new PortalLayout();
	private PortalLayout leftPortal = new PortalLayout();
	
	private LearningOptionsPanel optionsPanel;
	
	private Table classExpressionTable;
	private IndividualsTable falsePositivesTable;
	private IndividualsTable falseNegativesTable;
	
	private IndexedContainer container;
	private Button startBtn;
	private Button stopBtn;
	
	private Tree tree;
	
	private Refresher refresher;
	private Renderer renderer = new Renderer();
	
	public LearningView() {
		super(new HorizontalSplitPanel());
		
		initUI();
	}

	public void activated(Object... params) {
		// TODO Auto-generated method stub
		
	}

	public void deactivated(Object... params) {
		// TODO Auto-generated method stub
		
	}
	
	private void initUI(){
		getContent().setSizeFull();
		getContent().setSplitPosition(80);
		
		buildLeftPortal();
		buildMainPortal();
	    buildBottomPortals();
	    buildRightPortal();
	    
        HorizontalSplitPanel bottomHLayout = new HorizontalSplitPanel();
        bottomHLayout.setSizeFull();
        bottomHLayout.addComponent(posExamplesPortal);
        bottomHLayout.addComponent(negExamplesPortal);
        
        VerticalSplitPanel layout = new VerticalSplitPanel();
        layout.setSizeFull();
        layout.addComponent(mainPortal);
        layout.addComponent(bottomHLayout);
        layout.setSplitPosition(70);
        
        HorizontalSplitPanel hLayout = new HorizontalSplitPanel();
        hLayout.setSizeFull();
		hLayout.addComponent(leftPortal);
        hLayout.addComponent(layout);
        hLayout.setSplitPosition(20);
        
        getContent().addComponent(hLayout);
        getContent().addComponent(rightPortal);
        
        final LearningManager manager = UserSession.getLearningManager();
        final Renderer renderer = new Renderer();
        
        refresher = new Refresher();
        refresher.addListener(new RefreshListener() {
			
			@Override
			public void refresh(Refresher source) {
				if(!manager.isPreparing() && !manager.isRunning()){
					source.setEnabled(false);
					setLearningEnabled(true);
					((OREApplication)getApplication()).busy(false);
				}
				
				if(!manager.isPreparing() && manager.isRunning()){
					List<EvaluatedDescriptionClass> result = manager.getCurrentlyLearnedDescriptions();
//					synchronized (getApplication()) {
						container.removeAllItems();
						for(EvaluatedDescriptionClass ec : result){
							Item item = container.addItem(ec);//container.getItem(container.addItem());
							item.getItemProperty("class expression").setValue(new Label(renderer.render(ec.getDescription(), Syntax.MANCHESTER), Label.CONTENT_XHTML));
							item.getItemProperty("accuracy").setValue(ec.getAccuracy());
						}
//					}
				}
			}
		});
        refresher.setRefreshInterval(1000L);
        refresher.setEnabled(false);
        
		
	}
	
	@Override
	public void attach() {
		super.attach();
		getWindow().addComponent(refresher);
	}
	
	public void reset(){
		optionsPanel.reset();
		classExpressionTable.removeAllItems();
		falseNegativesTable.removeAllItems();
		falsePositivesTable.removeAllItems();
		tree.removeAllItems();
		setRootClasses();
	}
	
	private void setRootClasses(){
		SortedSet<Description> children = UserSession.getLearningManager().getReasoner().getSubClasses(Thing.instance);
    	for(Description sub : children){
    		tree.addItem(sub).getItemProperty("label").setValue(renderer.render(sub, Syntax.MANCHESTER, false));
    		if(!(UserSession.getLearningManager().getReasoner().getSubClasses(sub).size() > 1)){
    			tree.setChildrenAllowed(sub, false);
    		}
    	}
	}
	
	private void buildLeftPortal(){
		leftPortal.setSizeFull();
		
		VerticalLayout l = new VerticalLayout();
		l.setSizeFull();
		l.setCaption("Classes");
		
		Panel p = new Panel();
		p.setSizeFull();
		p.setScrollable(true);
		p.getContent().setSizeFull();
//		p.getContent().setSizeUndefined();
		
		l.addComponent(p);
		
		final Renderer renderer = new Renderer();
		tree = new Tree();
		tree.addContainerProperty("label", String.class, null);
//		tree.addItem(Thing.instance).getItemProperty("label").setValue("TOP");
		tree.setItemCaptionPropertyId("label");
		tree.setImmediate(true);
		tree.addListener(new Tree.ExpandListener() {

		    public void nodeExpand(ExpandEvent event) {
		    	SortedSet<Description> children = UserSession.getLearningManager().getReasoner().getSubClasses((Description)event.getItemId());
		    	for(Description sub : children){
		    		tree.addItem(sub).getItemProperty("label").setValue(renderer.render(sub, Syntax.MANCHESTER, false));
		    		if(!(UserSession.getLearningManager().getReasoner().getSubClasses(sub).size() > 1)){
		    			tree.setChildrenAllowed(sub, false);
		    		}
	                tree.setParent(sub, event.getItemId());
		    	}
		        
		    }
		});
		tree.addListener(new ItemClickListener() {
			
			@Override
			public void itemClick(ItemClickEvent event) {
				onClassSelected((NamedClass) event.getItemId());
			}
		});
		
		tree.setSizeFull();
		p.addComponent(tree);
		leftPortal.addComponent(l);
//		leftPortal.addComponent(tree);
//		leftPortal.setComponentCaption(tree, "Classes");
//		leftPortal.setLocked(tree, true);
//		leftPortal.setClosable(tree, false);
		leftPortal.setComponentCaption(l, "Classes");
		leftPortal.setLocked(l, true);
		leftPortal.setClosable(l, false);
		leftPortal.setCollapsible(l, false);
//		leftPortal.addComponent(tree);
	}
	
	private void buildRightPortal(){
		VerticalLayout vl = new VerticalLayout();
		vl.setSizeFull();
		vl.setHeight(null);
		vl.setSpacing(true);
		vl.addStyleName("white");
		
		optionsPanel = new LearningOptionsPanel();
		vl.addComponent(optionsPanel);
		vl.setExpandRatio(optionsPanel, 1f);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setSizeUndefined();
		startBtn = new Button("Start", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLearning();
			}
		});
		buttonPanel.addComponent(startBtn);
		stopBtn = new Button("Stop", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				
			}
		});
		buttonPanel.addComponent(stopBtn);
		vl.addComponent(buttonPanel);
		vl.setComponentAlignment(buttonPanel, Alignment.MIDDLE_CENTER);
		rightPortal.addComponent(vl);
		rightPortal.setSizeFull();
		rightPortal.setComponentCaption(vl, "Config");
		rightPortal.setCollapsible(vl, false);
		rightPortal.setClosable(vl, false);
		
		startBtn.setEnabled(false);
		stopBtn.setEnabled(false);
	}
	
	private void buildMainPortal(){
		mainPortal.setSizeFull();
        mainPortal.setSpacing(false);
        mainPortal.setCommunicative(false);
        Component c = createLearningResultTable();
        mainPortal.addComponent(c);
        mainPortal.setComponentCaption(c, "Suggested class expressions");
        mainPortal.setClosable(c, false);
        mainPortal.setCollapsible(c, false);
	}
	
	private void buildBottomPortals(){
		posExamplesPortal.setSizeFull();
        falsePositivesTable = new IndividualsTable();
        falsePositivesTable.setSizeFull();
        posExamplesPortal.addComponent(falsePositivesTable);
        posExamplesPortal.setLocked(falsePositivesTable, true);
        posExamplesPortal.setClosable(falsePositivesTable, false);
        posExamplesPortal.setCollapsible(falsePositivesTable, false);
        posExamplesPortal.setComponentCaption(falsePositivesTable, "False positive examples");
        
        negExamplesPortal.setSizeFull();
        falseNegativesTable = new IndividualsTable();
        falseNegativesTable.setSizeFull();
        negExamplesPortal.addComponent(falseNegativesTable);
        negExamplesPortal.setLocked(falseNegativesTable, true);
        negExamplesPortal.setClosable(falseNegativesTable, false);
        negExamplesPortal.setCollapsible(falseNegativesTable, false);
        negExamplesPortal.setComponentCaption(falseNegativesTable, "False negative examples");
	}
	
	private Table createLearningResultTable(){
		classExpressionTable = new Table(){
			
			DecimalFormat df = new DecimalFormat("0.00");
			
			@Override
			protected String formatPropertyValue(Object rowId, Object colId, Property property) {
				if(property.getType() == Double.class){
					return df.format((Double)property.getValue() * 100d) + "%";
				}
				return super.formatPropertyValue(rowId, colId, property);
			}
		};
		container = new IndexedContainer();
		container.addContainerProperty("accuracy", Double.class, 0d);
		container.addContainerProperty("class expression", Label.class, "");
		classExpressionTable.setContainerDataSource(container);
        classExpressionTable.setSelectable(true);
        classExpressionTable.setSizeFull();
        classExpressionTable.setImmediate(true);
        classExpressionTable.setColumnWidth("accuracy", 100);
        classExpressionTable.addListener(new Property.ValueChangeListener(){

			@Override
			public void valueChange(ValueChangeEvent event) {
				onClassExpressionSelected();
			}
        	
        });
        return classExpressionTable;
	}

    private void onClassExpressionSelected(){
    	falsePositivesTable.removeAllItems();
    	falseNegativesTable.removeAllItems();
    	
    	if(classExpressionTable.getValue() != null){
    		EvaluatedDescriptionClass ec = (EvaluatedDescriptionClass)classExpressionTable.getValue();
    		Set<Individual> falsePositives = new HashSet<Individual>();
        	for(org.dllearner.core.owl.Individual ind : ec.getAdditionalInstances()){
        		falsePositives.add(new Individual(renderer.render(ind, Syntax.MANCHESTER, false), ind.getName()));
        	}
        	
        	Set<Individual> falseNegatives = new HashSet<Individual>();
        	for(org.dllearner.core.owl.Individual ind : ec.getNotCoveredInstances()){
        		falseNegatives.add(new Individual(renderer.render(ind, Syntax.MANCHESTER, false), ind.getName()));
        	}
        	
        	falsePositivesTable.setIndividuals(falsePositives);
        	falseNegativesTable.setIndividuals(falseNegatives);
    	}
    }
    
	private void onLearning() {
		classExpressionTable.removeAllItems();
		falsePositivesTable.removeAllItems();
		falseNegativesTable.removeAllItems();
		
		setLearningEnabled(false);

		final LearningManager manager = UserSession.getLearningManager();
		manager.setClass2Describe((NamedClass) tree.getValue());
		manager.setMaxNrOfResults(optionsPanel.getMaxNrOfResults());
		manager.setMaxExecutionTimeInSeconds(optionsPanel.getMaxExecutionTimeInSeconds());
		manager.setNoisePercentage(optionsPanel.getNoiseInPercentage()/100);
		manager.setThreshold(optionsPanel.getThresholdInPercentage()/100);
		manager.setUseHasValue(optionsPanel.useHasValueQuantifier());
		manager.setUseExistentialQuantifier(optionsPanel.useExistentialQuantifier());
		manager.setUseUniversalQuantifier(optionsPanel.useUniversalQuantifier());
		manager.setUseNegation(optionsPanel.useNegation());
		manager.setUseCardinality(optionsPanel.useCardinalityRestriction());
		manager.setCardinalityLimit(optionsPanel.getCardinalityLimit());
		refresher.setEnabled(true);
		((OREApplication)getApplication()).busy(true);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				manager.prepareLearning();
				manager.startLearning();
			}
		});
		t.start();

	}
	
	private void onClassSelected(NamedClass cls){
		//TODO use FastInstanceChecker
		if(UserSession.getReasoner().getInstances(OWLAPIConverter.getOWLAPIDescription(cls), true).getFlattened().size() >= 2){
			setLearningEnabled(true);
		} else {
			setLearningEnabled(false);
		}
	}
	
	private void setLearningEnabled(boolean enabled){
		startBtn.setEnabled(enabled);
		stopBtn.setEnabled(!enabled);
	}


}
