package org.aksw.ore.view;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;

import org.aksw.ore.ORESession;
import org.aksw.ore.component.ConfigurablePanel;
import org.aksw.ore.component.IndividualsTable;
import org.aksw.ore.component.LearningOptionsPanel;
import org.aksw.ore.component.ProgressDialog;
import org.aksw.ore.manager.LearningManager;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.LearningSetting;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.util.Renderer;
import org.aksw.ore.util.Renderer.Syntax;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.owl.Individual;
import org.dllearner.core.owl.NamedClass;
import org.dllearner.learningproblems.EvaluatedDescriptionClass;
import org.dllearner.utilities.owl.OWLAPIConverter;
import org.vaadin.peter.contextmenu.ContextMenu;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuOpenedListener;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuOpenedOnTableFooterEvent;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuOpenedOnTableHeaderEvent;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuOpenedOnTableRowEvent;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

public class LearningView extends HorizontalSplitPanel implements View{
	
	private LearningOptionsPanel optionsPanel;
	
	private Table classExpressionTable;
	private IndividualsTable falsePositivesTable;
	private IndividualsTable falseNegativesTable;
	
	private IndexedContainer container;
	private Button startBtn;
	
	private Tree tree;
	
	private Renderer renderer = new Renderer();
	private DecimalFormat df = new DecimalFormat("0.00");
	
	NumberFormat percentFormat = NumberFormat.getPercentInstance();
	{percentFormat.setMaximumFractionDigits(2);}
	
	private ContextMenuOpenedListener.TableListener openListener = new ContextMenuOpenedListener.TableListener() {

		@Override
		public void onContextMenuOpenFromRow(
				ContextMenuOpenedOnTableRowEvent event) {
			System.out.println(event.getItemId());
//			event.getContextMenu().removeAllItems();
//			event.getContextMenu().addItem("Item " + event.getItemId());
		}

		@Override
		public void onContextMenuOpenFromHeader(
				ContextMenuOpenedOnTableHeaderEvent event) {
			event.getContextMenu().removeAllItems();
			event.getContextMenu().addItem("Item " + event.getPropertyId());
		}

		@Override
		public void onContextMenuOpenFromFooter(
				ContextMenuOpenedOnTableFooterEvent event) {
			event.getContextMenu().addItem("Item " + event.getPropertyId());
		}
	};
	
	public LearningView() {
		addStyleName("dashboard-view");
		addStyleName("learning-view");
		initUI();
	}
	
	private void initUI(){
		setSizeFull();
		setSplitPosition(80);
		
		buildClassesPanel();
	    buildBottomPortals();
	    buildRightPortal();
	    
        HorizontalSplitPanel bottomHLayout = new HorizontalSplitPanel();
        bottomHLayout.setSizeFull();
        bottomHLayout.addComponent(new ConfigurablePanel(falsePositivesTable));
        bottomHLayout.addComponent(new ConfigurablePanel(falseNegativesTable));
        
        VerticalSplitPanel layout = new VerticalSplitPanel();
        layout.setSizeFull();
        layout.addComponent(new ConfigurablePanel(createLearningResultTable()));
        layout.addComponent(bottomHLayout);
        layout.setSplitPosition(70);
        
        HorizontalSplitPanel hLayout = new HorizontalSplitPanel();
        hLayout.setSizeFull();
		hLayout.addComponent(new ConfigurablePanel(buildClassesPanel()));
        hLayout.addComponent(layout);
        hLayout.setSplitPosition(20);
        
        addComponent(hLayout);
        addComponent(buildRightPortal());
	}
	
	public void reset(){
		optionsPanel.reset();
		classExpressionTable.removeAllItems();
		falseNegativesTable.removeAllItems();
		falsePositivesTable.removeAllItems();
		tree.removeAllItems();
		Knowledgebase knowledgebase = ORESession.getKnowledgebaseManager().getKnowledgebase();
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			if(((OWLOntologyKnowledgebase) knowledgebase).isConsistent()){
				setRootClasses();
			}
		}
	}
	
	private void setRootClasses(){
		SortedSet<NamedClass> topLevelClasses = ORESession.getLearningManager().getTopLevelClasses();
    	for(NamedClass topClass : topLevelClasses){
    		tree.addItem(topClass).getItemProperty("label").setValue(renderer.render(topClass, Syntax.MANCHESTER, false));
//    		if(!(ORESession.getLearningManager().getDirectSubClasses(topClass).size() > 1)){
//    			tree.setChildrenAllowed(topClass, false);
//    		}
    		if(ORESession.getLearningManager().getDirectSubClasses(topClass).isEmpty()){
    			tree.setChildrenAllowed(topClass, false);
    		}
    	}
	}
	
	private Component buildClassesPanel(){
		VerticalLayout l = new VerticalLayout();
		l.setSizeFull();
		l.setCaption("Classes");
		
		final Renderer renderer = new Renderer();
		tree = new Tree();
		tree.addContainerProperty("label", String.class, null);
//		tree.addItem(Thing.instance).getItemProperty("label").setValue("TOP");
		tree.setItemCaptionPropertyId("label");
		tree.setImmediate(true);
		tree.addExpandListener(new Tree.ExpandListener() {

		    public void nodeExpand(ExpandEvent event) {
		    	SortedSet<NamedClass> subClasses = ORESession.getLearningManager().getDirectSubClasses((NamedClass)event.getItemId());
		    	
		    	Item item;
		    	for(NamedClass sub : subClasses){
		    		item = tree.addItem(sub);
		    		if(item != null){
		    			item.getItemProperty("label").setValue(renderer.render(sub, Syntax.MANCHESTER, false));
			    		if(ORESession.getLearningManager().getDirectSubClasses(sub).isEmpty()){
			    			tree.setChildrenAllowed(sub, false);
			    		}
		                tree.setParent(sub, event.getItemId());
		    		}
		    	}
		        
		    }
		});
//		tree.addCollapseListener(new Tree.CollapseListener() {
//
//			@Override
//			public void nodeCollapse(CollapseEvent event) {
//				// Remove all children of the collapsing node
//				removeItemsRecursively(tree, event.getItemId());
//			}
//
//			void removeItemsRecursively(Tree tree, Object item) {
//				// Find all child nodes of the node
//				Collection<?> children = tree.getChildren(item);
//				if (children == null)
//					return;
//
//				// The list is unmodifiable so must copy to another list
//				LinkedList<String> children2 = new LinkedList<String>();
//				for (Iterator<?> i = children.iterator(); i.hasNext();)
//					children2.add((String) i.next());
//
//				// Remove the children of the collapsing node
//				for (Iterator<String> i = children2.iterator(); i.hasNext();) {
//					String child = i.next();
//					removeItemsRecursively(tree, child);
//					tree.removeItem(child);
//				}
//			}
//		});
		tree.addItemClickListener(new ItemClickListener() {
			
			@Override
			public void itemClick(ItemClickEvent event) {
				onClassSelected((NamedClass) event.getItemId());
			}
		});
		
		tree.setSizeFull();
		l.addComponent(tree);
		return l;
	}
	
	private Component buildRightPortal(){
		VerticalLayout vl = new VerticalLayout();
		vl.setSizeFull();
		vl.setSpacing(true);
		
		optionsPanel = new LearningOptionsPanel();
		ConfigurablePanel c = new ConfigurablePanel(optionsPanel);
		c.setSizeFull();
		vl.addComponent(c);
		vl.setExpandRatio(c, 1f);
		
		startBtn = new Button("Start", new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				onLearning();
			}
		});
//		startBtn.addStyleName("icon-naming");
		startBtn.setEnabled(false);
		startBtn.setSizeUndefined();
		
		vl.addComponent(startBtn);
		vl.setComponentAlignment(startBtn, Alignment.TOP_CENTER);
		
		return vl;
	}
	
	
	private void buildBottomPortals(){
        falsePositivesTable = new IndividualsTable();
        falsePositivesTable.setSizeFull();
        falsePositivesTable.setCaption("False positive examples");
        ContextMenu tableContextMenu = new ContextMenu();
		tableContextMenu.addContextMenuTableListener(openListener);
		tableContextMenu.addItem("Explain why");
		tableContextMenu.setAsTableContextMenu(falsePositivesTable);
        
        
        falseNegativesTable = new IndividualsTable();
        falseNegativesTable.setSizeFull();
        falseNegativesTable.setCaption("False negative examples");
	}
	
	private Table createLearningResultTable(){
		classExpressionTable = new Table(){
			
			@Override
			protected String formatPropertyValue(Object rowId, Object colId, Property property) {
				if(property.getType() == Double.class){
					return df.format((Double)property.getValue() * 100d) + "%";
				}
				return super.formatPropertyValue(rowId, colId, property);
			}
		};
		classExpressionTable.setCaption("Suggested class expressions");
		container = new IndexedContainer();
		container.addContainerProperty("Accuracy", Double.class, 0d);
		container.addContainerProperty("Class Expression", Label.class, "");
//		container.addContainerFilter(new Filter() {
//			
//			@Override
//			public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
//				double threshold = optionsPanel.getThresholdInPercentage()/100;
//				return ((Double) item.getItemProperty("Accuracy").getValue()).doubleValue() >= threshold;
//			}
//			
//			@Override
//			public boolean appliesToProperty(Object propertyId) {
//				return propertyId.equals("Accuracy");
//			}
//		});
		classExpressionTable.setContainerDataSource(container);
        classExpressionTable.setSelectable(true);
        classExpressionTable.setSizeFull();
        classExpressionTable.setImmediate(true);
        classExpressionTable.setColumnWidth("Accuracy", 100);
        classExpressionTable.addValueChangeListener(new Property.ValueChangeListener(){

			@Override
			public void valueChange(ValueChangeEvent event) {
				onClassExpressionSelected();
			}
        	
        });
//        classExpressionTable.addStyleName("plain");
//        classExpressionTable.addStyleName("borderless");
        return classExpressionTable;
	}

    private void onClassExpressionSelected(){
    	falsePositivesTable.removeAllItems();
    	falseNegativesTable.removeAllItems();
    	
    	if(classExpressionTable.getValue() != null){
    		EvaluatedDescriptionClass ec = (EvaluatedDescriptionClass)classExpressionTable.getValue();
    		
    		Set<Individual> falsePositives = ec.getAdditionalInstances();
        	Set<Individual> falseNegatives = ec.getNotCoveredInstances();
        	
        	falsePositivesTable.setIndividuals(falsePositives);
        	falseNegativesTable.setIndividuals(falseNegatives);
    	}
    }
    
	private void onLearning() {
		classExpressionTable.removeAllItems();
		falsePositivesTable.removeAllItems();
		falseNegativesTable.removeAllItems();
		
//		setLearningEnabled(false);
		
		final LearningManager manager = ORESession.getLearningManager();
		
		LearningSetting learningSetting = new LearningSetting(
				(NamedClass) tree.getValue(), 
				optionsPanel.getMaxNrOfResults(), 
				optionsPanel.getMaxExecutionTimeInSeconds(), 
				optionsPanel.getNoiseInPercentage()/100, 
				optionsPanel.getThresholdInPercentage()/100, 
				optionsPanel.useHasValueQuantifier(), 
				optionsPanel.useExistentialQuantifier(), 
				optionsPanel.useUniversalQuantifier(), 
				optionsPanel.useNegation(), 
				optionsPanel.useCardinalityRestriction(), 
				optionsPanel.getCardinalityLimit());
		manager.setLearningSetting(learningSetting);
		
		//check if this setup was already used for learning
		List<EvaluatedDescriptionClass> learnedDescriptions = manager.getCurrentlyLearnedDescriptionsCached();
		if(learnedDescriptions != null){
			showClassExpressions(learnedDescriptions);
		} else {
			final ProgressDialog dialog = new ProgressDialog("Learning..."){
				@Override
				protected void onCancelled() {
					manager.stopLearning();
					super.onCancelled();
				}
			};
			getUI().addWindow(dialog);
			
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					dialog.setMessage("Preparing learning environment...");
					manager.prepareLearning();
					dialog.setMessage("Generating class expressions...");
					Timer timer = new Timer();
					timer.schedule(new TimerTask() {
						
						@Override
						public void run() {
							final List<EvaluatedDescriptionClass> currentlyLearnedDescriptions = manager.getCurrentlyLearnedDescriptions();
							getUI().access(new Runnable() {
								
								@Override
								public void run() {
									showClassExpressions(currentlyLearnedDescriptions);
								}
							});
							
						}
					}, 1000, 1000);
					manager.startLearning();
					timer.cancel();
					final List<EvaluatedDescriptionClass> result = manager.getCurrentlyLearnedDescriptions();
					getUI().access(new Runnable() {
						
						@Override
						public void run() {
							showClassExpressions(result);
							dialog.close();
							if(result.isEmpty()){
								EvaluatedDescription bestSolution = manager.getBestLearnedDescriptions();
								Notification notification = new Notification("Could not find any solution above the threshold!",
										"<br/>Best solution found in " + optionsPanel.getMaxExecutionTimeInSeconds() + "s was <b>" + 
												renderer.render(bestSolution.getDescription(), Syntax.MANCHESTER, false) +
										"</b> with " + percentFormat.format(bestSolution.getAccuracy()), Type.WARNING_MESSAGE);
								notification.setHtmlContentAllowed(true);
								notification.show(Page.getCurrent());
							}
						}
					});
				}
			});
			t.start();
		}
		
		

	}
	
	private void showClassExpressions(List<EvaluatedDescriptionClass> result){
		container.removeAllItems();
		for(EvaluatedDescriptionClass ec : result){
			Item item = container.addItem(ec);//container.getItem(container.addItem());
			item.getItemProperty("Class Expression").setValue(new Label(renderer.render(ec.getDescription(), Syntax.MANCHESTER), ContentMode.HTML));
			item.getItemProperty("Accuracy").setValue(ec.getAccuracy());
		}
	}
	
	private void onClassSelected(NamedClass cls){
		//TODO use FastInstanceChecker
		if(ORESession.getOWLReasoner().getInstances(OWLAPIConverter.getOWLAPIDescription(cls), true).getFlattened().size() >= 2){
			setLearningEnabled(true);
		} else {
			setLearningEnabled(false);
		}
	}
	
	private void setLearningEnabled(boolean enabled){
		startBtn.setEnabled(enabled);
	}

	/* (non-Javadoc)
	 * @see com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.ViewChangeEvent)
	 */
	@Override
	public void enter(ViewChangeEvent event) {
		reset();
	}


}
