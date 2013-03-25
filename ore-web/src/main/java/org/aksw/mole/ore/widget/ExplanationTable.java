package org.aksw.mole.ore.widget;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.aksw.mole.ore.RepairManagerListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.explanation.formatter.ExplanationFormatter2.FormattedExplanation;
import org.aksw.mole.ore.util.Renderer;
import org.aksw.mole.ore.util.Renderer.Syntax;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owlapi.model.OWLAxiom;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

@SuppressWarnings("serial")
public class ExplanationTable extends Table implements RepairManagerListener{
	
	private Explanation<OWLAxiom> explanation;
	private Set<Object> selectedObjects = new HashSet<Object>();
	private FormattedExplanation formattedExplanation;
	
	public ExplanationTable(Explanation<OWLAxiom> explanation, Set<OWLAxiom> selectedAxioms) {
		this(explanation, selectedAxioms, true);
	}
	
	public ExplanationTable(Explanation<OWLAxiom> explanation, Set<OWLAxiom> selectedAxioms, final boolean formatted) {
		this.explanation = explanation;
		selectedObjects.addAll(selectedAxioms);
		
		setSizeFull();
		setPageLength(0);
		setHeight(null);
		setColumnWidth("Selected", 30);
		setColumnExpandRatio("Axiom", 1.0f);
//		setColumnWidth("Frequency", 60);
		setColumnWidth("Usage", 40);
		setSelectable(true);
        setMultiSelect(true);
        setImmediate(true);
		
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty("Selected", CheckBox.class, null);
		container.addContainerProperty("Axiom", String.class, null);
		container.addContainerProperty("Frequency", Integer.class, null);
		container.addContainerProperty("Usage", Integer.class, null);
		container.addContainerProperty("Score", Double.class, null);
		setContainerDataSource(container);
		
		addGeneratedColumn("Selected", new ColumnGenerator() {
			
			@Override
			public Object generateCell(Table source, final Object itemId, Object columnId) {
				if ("Selected".equals(columnId)) {
					CheckBox cb = new CheckBox();
					cb.setValue(selectedObjects.contains(itemId));
					cb.setImmediate(true);
					cb.addListener(new ValueChangeListener() {
						
						@Override
						public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
							boolean selected = (Boolean) event.getProperty().getValue();
							if(selected){
								selectedObjects.add(itemId);
							} else {
								selectedObjects.remove(itemId);
							}
							ExplanationTable.this.setValue(selectedObjects);
							
						}
					});
					ExplanationTable.this.getItem(itemId).getItemProperty("Selected").setValue(cb);
					return cb;
				}
				return null;
			}
		});
		
		addGeneratedColumn("Axiom", new ColumnGenerator() {
			
			private Renderer renderer = new Renderer();
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if ("Axiom".equals(columnId)) {
					if(itemId instanceof OWLAxiom){
						OWLAxiom ax = ((OWLAxiom) itemId);
						String indention = "";
						if(formatted){
							for(int i = 0; i < formattedExplanation.getIndention(ax); i++){
								indention += "&nbsp;";
							}
						}
						return new Label(indention + renderer.render(ax, Syntax.MANCHESTER), Label.CONTENT_XHTML);
		        	}
				}
				return null;
			}
		});
		
		setColumnHeader("Selected", "");
		
		Collection<OWLAxiom> axioms;
		if(formatted){
			formattedExplanation = UserSession.getExplanationManager().format(explanation);
			axioms = formattedExplanation.getOrderedAxioms();
		} else {
			axioms = explanation.getAxioms();
		}
		Item item;
		for(OWLAxiom axiom : axioms){
			item = container.addItem(axiom);
			item.getItemProperty("Axiom").setValue(axiom);
			item.getItemProperty("Frequency").setValue(UserSession.getExplanationManager().getAxiomFrequency(axiom));
			item.getItemProperty("Usage").setValue(UserSession.getExplanationManager().getAxiomUsageCount(axiom));
			item.getItemProperty("Score").setValue(UserSession.getExplanationManager().getAxiomRelevanceScore(axiom));
		}
		
		setValue(selectedObjects);
	}
	
	public void selectAxiom(OWLAxiom axiom){
		selectedObjects.add(axiom);
		setValue(selectedObjects);
	}
	
	public void selectAxioms(Collection<OWLAxiom> axioms){
		selectedObjects.clear();
		selectedObjects.addAll(explanation.getAxioms());
		selectedObjects.retainAll(axioms);
		for(OWLAxiom ax : explanation.getAxioms()){
			((CheckBox)getItem(ax).getItemProperty("Selected").getValue()).setValue(axioms.contains(ax));
		}
//		setValue(axioms);
	}

	public Explanation getExplanation() {
		return explanation;
	}

	@Override
	public void repairPlanChanged() {
		selectAxioms(UserSession.getRepairManager().getAxiomsScheduledToRemove());
	}

	@Override
	public void repairPlanExecuted() {
		// TODO Auto-generated method stub
		
	}

}
