package org.aksw.mole.ore.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.aksw.mole.ore.ImpactManager;
import org.aksw.mole.ore.RepairManager;
import org.aksw.mole.ore.RepairManagerListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.task.ImpactComputationTask;
import org.aksw.mole.ore.util.Renderer;
import org.aksw.mole.ore.util.Renderer.Syntax;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class ImpactTable extends Table implements RepairManagerListener{
	
	private static final Logger logger = Logger.getLogger(ImpactTable.class.getName());
	
	private static final String ACTION = "action";
	private static final String AXIOM = "axiom";
	private static final String KEEP_IF_LOST = "keep";
	
	private Collection<OWLOntologyChange> impact;
	private IndexedContainer container;
	
	private Refresher refresher = new Refresher();
	
	public ImpactTable() {
		setSizeFull();
//		setCaption("Impact");
		
		container = new IndexedContainer();
		container.addContainerProperty(ACTION, String.class, null);
		container.addContainerProperty(AXIOM, OWLAxiom.class, null);
		container.addContainerProperty(KEEP_IF_LOST, Button.class, null);
		setColumnHeader(KEEP_IF_LOST, "");
		setContainerDataSource(container);
		
		addGeneratedColumn(AXIOM, new ColumnGenerator() {
			
			private Renderer renderer = new Renderer();
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if (AXIOM.equals(columnId)) {
					if(itemId instanceof OWLOntologyChange){
						OWLAxiom axiom = ((OWLOntologyChange) itemId).getAxiom();
						return new Label(renderer.render(axiom, Syntax.MANCHESTER), Label.CONTENT_XHTML);
		        	}
				}
				return null;
			}
		});
		
		addGeneratedColumn(KEEP_IF_LOST, new ColumnGenerator() {
			 
			  @Override public Object generateCell(final Table source, final Object itemId, Object columnId) {
			 
				if (itemId instanceof RemoveAxiom) {
					final OWLOntologyChange change = (RemoveAxiom) itemId;
					Button button = new Button("Keep");
					button.setStyleName(BaseTheme.BUTTON_LINK);
					button.setDescription("Click to keep the axiom in the ontology when the repair plan is executed.");

					button.addListener(new ClickListener() {

						@Override
						public void buttonClick(ClickEvent event) {
							RepairManager repMan = UserSession.getRepairManager();
							repMan.addToRepairPlan(repMan.reverse(change));
						}
					});

					return button;
				}
				return null;
			  }
			});
		
		setColumnWidth(ACTION, 50);
		setColumnExpandRatio(AXIOM, 1.0f);
		
		refresher.addListener(new RefreshListener() {
			
			@Override
			public void refresh(Refresher source) {
				if(impact != null){
					setImpact(impact);
					source.setEnabled(false);
				}
				
			}
		});
		refresher.setRefreshInterval(500);
	}
	
//	@Override
//	public void attach() {
//		super.attach();
//		getWindow().addComponent(refresher);
//		UserSession.getRepairManager().addListener(this);
//		
//	}
//	
//	@Override
//	public void detach() {
//		super.detach();
//		getWindow().removeComponent(refresher);
//		UserSession.getRepairManager().removeListener(this);
//	}
	
	public void setImpact(Collection<OWLOntologyChange> impact){
		synchronized (getApplication()) {
			container.removeAllItems();
			Item item;
			for(OWLOntologyChange c : impact){
				item = container.addItem(c);
				item.getItemProperty(ACTION).setValue(c instanceof RemoveAxiom ? "Lost" : "Retained");
				item.getItemProperty(AXIOM).setValue(c.getAxiom());
			}
		}
		
	}
	
	public void clear(){
		container.removeAllItems();
	}

	@Override
	public void repairPlanChanged() {
		computeImpact();
	}

	@Override
	public void repairPlanExecuted() {
		removeAllItems();
	}
	
	private void computeImpact(){
		if (logger.isDebugEnabled()) {
			logger.debug("Recomputing impact...");
		}
		ExecutorService s = Executors.newSingleThreadExecutor();
		s.execute(new ImpactComputationTask(null, null, null));
		impact = null;
		refresher.setEnabled(true);
		final ImpactManager impMan = UserSession.getImpactManager();
		final List<OWLOntologyChange> repairPlan = new ArrayList<OWLOntologyChange>(UserSession.getRepairManager().getRepairPlan());
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				impact = impMan.getImpact(repairPlan);
			}
		});
		t.start();
	}
	
}
