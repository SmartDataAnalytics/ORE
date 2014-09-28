package org.aksw.ore.component;

import java.util.Collection;
import java.util.Set;

import org.aksw.ore.ORESession;
import org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.rendering.Renderer;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class KnowledgebaseChangesTable extends Table implements KnowledgebaseLoadingListener{
	
	private static final String ACTION = "action";
	private static final String AXIOM = "axiom";
	private static final String REMOVE = "remove";
	
	private IndexedContainer container;
	
	public KnowledgebaseChangesTable() {
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
					if(itemId instanceof AddAxiom || itemId instanceof RemoveAxiom){
						OWLAxiom axiom = ((OWLOntologyChange) itemId).getAxiom();
						return new Label(renderer.renderHTML(axiom), ContentMode.HTML);
		        	} else {
						return new Label(itemId.toString());
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
				button.setStyleName(ValoTheme.BUTTON_BORDERLESS);
				button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
				button.setIcon(FontAwesome.TRASH_O);
//				button.addStyleName("cancel");
				button.setDescription("Click to remove the ontology change.");

				button.addClickListener(new ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						ORESession.getKnowledgebaseManager().removeChange(change);
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
					} else if(change instanceof AddAxiom){
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
	
	public void setChanges(Collection<OWLOntologyChange> changes){
		container.removeAllItems();
		Item item;
		for(OWLOntologyChange c : changes){
			item = container.addItem(c);
//			item.getItemProperty(ACTION).setValue(c instanceof RemoveAxiom ? "Remove" : "Add");
			item.getItemProperty(AXIOM).setValue(c.getAxiom());
		}
	}
	
	public void reset(){
		removeAllItems();
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseChanged(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseChanged(Knowledgebase knowledgebase) {
		reset();
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseAnalyzed(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseAnalyzed(Knowledgebase knowledgebase) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseStatusChanged(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseStatusChanged(Knowledgebase knowledgebase) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#message(java.lang.String)
	 */
	@Override
	public void message(String message) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseModified(java.util.List)
	 */
	@Override
	public void knowledgebaseModified(Set<OWLOntologyChange> changes) {
		setChanges(changes);
	}

}
