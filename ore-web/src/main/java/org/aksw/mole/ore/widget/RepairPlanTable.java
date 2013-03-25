package org.aksw.mole.ore.widget;

import java.util.Collection;

import org.aksw.mole.ore.RepairManager;
import org.aksw.mole.ore.RepairManagerListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.util.Renderer;
import org.aksw.mole.ore.util.Renderer.Syntax;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class RepairPlanTable extends Table implements RepairManagerListener{
	
	private static final String ACTION = "action";
	private static final String AXIOM = "axiom";
	private static final String REMOVE = "remove";
	
	private Collection<OWLOntologyChange> repairPlan;
	private IndexedContainer container;
	
	public RepairPlanTable() {
		setSizeFull();
//		setCaption("Repair Plan");
		
		container = new IndexedContainer();
		container.addContainerProperty(ACTION, String.class, null);
		container.addContainerProperty(AXIOM, OWLAxiom.class, null);
		container.addContainerProperty(REMOVE, Button.class, null);
		setColumnHeader(REMOVE, "");
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
		
		addGeneratedColumn(REMOVE, new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, Object columnId) {
				
				final OWLOntologyChange change = (OWLOntologyChange) itemId;
				Button button = new Button("Remove");
				button.setStyleName(BaseTheme.BUTTON_LINK);
				button.setDescription("Click to remove the ontology change from the repair plan.");

				button.addListener(new ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						RepairManager repMan = UserSession.getRepairManager();
						repMan.removeFromRepairPlan(change);
					}
				});

				return button;
			}
		});
		
		setColumnWidth(ACTION, 50);
		setColumnExpandRatio(AXIOM, 1.0f);
	}
	
	public void setRepairPlan(Collection<OWLOntologyChange> repairPlan){
		this.repairPlan = repairPlan;
		synchronized (getApplication()) {
			container.removeAllItems();
			Item item;
			for(OWLOntologyChange c : repairPlan){
				item = container.addItem(c);
				item.getItemProperty(ACTION).setValue(c instanceof RemoveAxiom ? "Remove" : "Add");
				item.getItemProperty(AXIOM).setValue(c.getAxiom());
			}
		}
	}
	
	public void reset(){
		removeAllItems();
	}

	@Override
	public void repairPlanChanged() {
		setRepairPlan(UserSession.getRepairManager().getRepairPlan());
	}

	@Override
	public void repairPlanExecuted() {
		removeAllItems();
	}

}
