package org.aksw.ore.component;

import java.util.Collection;

import org.aksw.ore.ORESession;
import org.aksw.ore.manager.RepairManager;
import org.aksw.ore.manager.RepairManager.RepairManagerListener;
import org.aksw.ore.rendering.Renderer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.label.ContentMode;
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
		addStyleName("repair-plan-table");
		addStyleName("borderless");
		
		container = new IndexedContainer();
		container.addContainerProperty(ACTION, String.class, null);
		container.addContainerProperty(AXIOM, OWLAxiom.class, null);
		container.addContainerProperty(REMOVE, Button.class, null);
		setColumnHeader(REMOVE, "");
		setContainerDataSource(container);
		
		addGeneratedColumn(AXIOM, new ColumnGenerator() {
			
			private Renderer renderer = ORESession.getRenderer();
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if (AXIOM.equals(columnId)) {
					if(itemId instanceof OWLOntologyChange){
						OWLAxiom axiom = ((OWLOntologyChange) itemId).getAxiom();
						return new Label(renderer.render(axiom), ContentMode.HTML);
		        	}
				}
				return null;
			}
		});
		
		addGeneratedColumn(REMOVE, new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, Object columnId) {
				
				final OWLOntologyChange change = (OWLOntologyChange) itemId;
				Button button = new Button();
				button.setStyleName(BaseTheme.BUTTON_LINK);
				button.addStyleName("cancel");
				button.setDescription("Click to remove the ontology change from the repair plan.");

				button.addClickListener(new ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						RepairManager repMan = ORESession.getRepairManager();
						repMan.removeFromRepairPlan(change);
					}
				});

				return button;
			}
		});
		
		setCellStyleGenerator(new Table.CellStyleGenerator() {
			
			@Override
			public String getStyle(Table source, Object itemId, Object propertyId) {
				if(propertyId == null){
					return null;
				} else if(propertyId.equals(ACTION)){
					OWLOntologyChange change = ((OWLOntologyChange) itemId);
					if(change instanceof RemoveAxiom){
						return "remove-axiom";
					} else {
						return "add-axiom";
					}
				} 
				return null;
			}
		});
		
		setColumnWidth(ACTION, 25);
		setColumnExpandRatio(AXIOM, 1.0f);
		
		setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
	}
	
	public void setRepairPlan(Collection<OWLOntologyChange> repairPlan){
		this.repairPlan = repairPlan;
		container.removeAllItems();
		Item item;
		for(OWLOntologyChange c : repairPlan){
			item = container.addItem(c);
//			item.getItemProperty(ACTION).setValue(c instanceof RemoveAxiom ? "Remove" : "Add");
			item.getItemProperty(AXIOM).setValue(c.getAxiom());
		}
	}
	
	public void reset(){
		removeAllItems();
	}

	@Override
	public void repairPlanChanged() {
		setRepairPlan(ORESession.getRepairManager().getRepairPlan());
	}

	@Override
	public void repairPlanExecuted() {
		removeAllItems();
	}

}
