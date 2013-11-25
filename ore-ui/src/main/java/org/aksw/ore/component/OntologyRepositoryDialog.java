package org.aksw.ore.component;

import java.net.URI;

import org.aksw.mole.ore.repository.OntologyRepository;
import org.aksw.mole.ore.repository.OntologyRepositoryEntry;
import org.aksw.ore.ORESession;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class OntologyRepositoryDialog extends Window {

	private OntologyRepository repository;

	private Button okButton;
	private Button cancelButton;
	
	private Table table;
	private IndexedContainer data;
	
	public OntologyRepositoryDialog(OntologyRepository repository) {
		this.repository = repository;
		addStyleName("transactions");
		
		createDataContainer(repository);

		setCaption(repository.getName() + " Ontology Repository");
		setWidth("800px");
		setHeight("700px");
		center();
		initUI();
	}
	
	private void createDataContainer(OntologyRepository repository){
		data = new IndexedContainer();
		data.addContainerProperty("Name", String.class, null);
		data.addContainerProperty("URI", String.class, null);
		Item item;
		for (OntologyRepositoryEntry entry : repository.getEntries()) {
			item = data.addItem(entry);
			item.getItemProperty("Name").setValue(entry.getOntologyShortName());
			item.getItemProperty("URI").setValue(entry.getPhysicalURI().toString());
		}
	}
	
	private void initUI() {
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(new MarginInfo(true, false, true, false));
		main.setSpacing(true);
		setContent(main);

		// table
		table = new Table();
		table.setSizeFull();
		table.addStyleName("borderless");
		table.setPageLength(0);
		table.setSelectable(true);
		table.setImmediate(true);
		table.addValueChangeListener(new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				okButton.setEnabled(event.getProperty().getValue() != null);
			}
		});
		data.removeAllContainerFilters();
		table.setContainerDataSource(data);
		
		//filter
		final TextField filter = new TextField();
        filter.addTextChangeListener(new TextChangeListener() {
            @Override
            public void textChange(final TextChangeEvent event) {
                data.removeAllContainerFilters();
                data.addContainerFilter(new Filter() {
                    @Override
                    public boolean passesFilter(Object itemId, Item item)
                            throws UnsupportedOperationException {

                        if (event.getText() == null
                                || event.getText().equals("")) {
                            return true;
                        }

                        return filterByProperty("Name", item,
                                event.getText());

                    }

                    @Override
                    public boolean appliesToProperty(Object propertyId) {
                        if (propertyId.equals("Name"))
                            return true;
                        return false;
                    }
                });
            }
        });
        filter.setInputPrompt("Filter by name");
        filter.addShortcutListener(new ShortcutListener("Clear",
                KeyCode.ESCAPE, null) {
            @Override
            public void handleAction(Object sender, Object target) {
                filter.setValue("");
                data.removeAllContainerFilters();
            }
        });
        
        main.addComponent(filter);

		// buttons
		okButton = new Button("Ok");
		okButton.addClickListener(new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				OntologyRepositoryEntry entry = (OntologyRepositoryEntry) table.getValue();
				onLoadOntology(entry.getPhysicalURI().toString());
			}
		});
		okButton.setEnabled(false);
		cancelButton = new Button("Cancel");
		cancelButton.addClickListener(new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				close();
			}
		});
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth("100%");
		buttons.setHeight(null);
		buttons.setSpacing(true);
		buttons.addComponent(okButton);
		buttons.addComponent(cancelButton);
		okButton.setWidth("70px");
		cancelButton.setWidth("70px");
		buttons.setComponentAlignment(okButton, Alignment.MIDDLE_RIGHT);
		buttons.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
		
		main.addComponent(table);
		main.addComponent(buttons);
		main.setExpandRatio(table, 1f);
	}
	
	private boolean filterByProperty(String prop, Item item, String text) {
        if (item == null || item.getItemProperty(prop) == null
                || item.getItemProperty(prop).getValue() == null)
            return false;
        String val = item.getItemProperty(prop).getValue().toString().trim()
                .toLowerCase();
        if (val.startsWith(text.toLowerCase().trim()))
            return true;
        // String[] parts = text.split(" ");
        // for (String part : parts) {
        // if (val.contains(part.toLowerCase()))
        // return true;
        //
        // }
        return false;
    }
	
	private void onLoadOntology(String uri){
		try {
			URI.create(uri);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			man.addOntologyLoaderListener(ORESession.getKnowledgebaseManager());
			OWLOntology ontology = man.loadOntology(IRI.create(uri));
			ORESession.getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
			close();
		} catch (IllegalArgumentException e){
			Notification.show("Error loading ontology", "Invalid URI.", Notification.Type.ERROR_MESSAGE);
			table.focus();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			Notification.show("Error loading ontology", "Could not load the ontology from the given URI.", Notification.Type.ERROR_MESSAGE);
			table.focus();
		}
	}
}
