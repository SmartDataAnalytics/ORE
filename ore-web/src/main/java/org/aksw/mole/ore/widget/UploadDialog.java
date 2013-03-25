package org.aksw.mole.ore.widget;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.vaadin.easyuploads.UploadField;
import org.vaadin.easyuploads.UploadField.FieldType;

import com.vaadin.ui.Window;

public class UploadDialog extends Window {
	
	private static final String DIALOG_TITLE = "Upload OWL ontology";
	
	public UploadDialog() {
		super(DIALOG_TITLE);
		setModal(true);
		
		initUI();
	}
	
	private void initUI(){
		final UploadField uploadField = new UploadField() {
            @Override
            protected void updateDisplay() {
            	super.updateDisplay();
                final byte[] data = (byte[]) getValue();
                
                InputStream is = new ByteArrayInputStream(data);
                onLoadOntology(is);
            }
        };
        uploadField.setFieldType(FieldType.BYTE_ARRAY);
        uploadField.setCaption("Select OWL ontology file to upload:");
        
        addComponent(uploadField);
	}
	
	private void onLoadOntology(InputStream is){
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			man.addOntologyLoaderListener(UserSession.getKnowledgebaseManager());
			OWLOntology ontology = man.loadOntologyFromOntologyDocument(is);
			System.out.println("Loaded ontology: " + ontology);
			UserSession.getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
			close();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
}
