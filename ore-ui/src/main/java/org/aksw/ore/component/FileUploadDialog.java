package org.aksw.ore.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.aksw.ore.ORESession;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.Window;

public class FileUploadDialog extends Window implements Receiver, SucceededListener{
	
	private static final String DIALOG_TITLE = "Upload OWL ontology";
	
	private ByteArrayOutputStream baos;
	
	public FileUploadDialog() {
		super(DIALOG_TITLE);
		setModal(true);
		
		initUI();
	}
	
	private void initUI(){
		Upload upload = new Upload();
		upload.setReceiver(this);
		upload.addSucceededListener(this);
		upload.setSizeUndefined();
		setContent(upload);
		
//		final UploadField uploadField = new UploadField() {
//            @Override
//            protected void updateDisplay() {
//            	super.updateDisplay();
//                final byte[] data = (byte[]) getValue();
//                
//                InputStream is = new ByteArrayInputStream(data);
//                onLoadOntology(is);
//            }
//        };
//        uploadField.setFieldType(FieldType.BYTE_ARRAY);
//        uploadField.setCaption("Select OWL ontology file to upload:");
        
        
	}
	
	private void onLoadOntology(InputStream is){
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			man.addOntologyLoaderListener(ORESession.getKnowledgebaseManager());
			OWLOntology ontology = man.loadOntologyFromOntologyDocument(is);
			ORESession.getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
			Notification.show("Successfully loaded ontology.", Type.TRAY_NOTIFICATION);
			close();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see com.vaadin.ui.Upload.SucceededListener#uploadSucceeded(com.vaadin.ui.Upload.SucceededEvent)
	 */
	@Override
	public void uploadSucceeded(SucceededEvent event) {
		onLoadOntology(new ByteArrayInputStream(baos.toByteArray()));
	}

	/* (non-Javadoc)
	 * @see com.vaadin.ui.Upload.Receiver#receiveUpload(java.lang.String, java.lang.String)
	 */
	@Override
	public OutputStream receiveUpload(String filename, String mimeType) {
//        FileOutputStream fos = null; // Stream to write to
//        try {
//            // Open the file for writing.
//            file = new File("/tmp/uploads/" + filename);
//            fos = new FileOutputStream(file);
//        } catch (final java.io.FileNotFoundException e) {
//            Notification.show(
//                    "Could not open file<br/>", e.getMessage(),
//                    Notification.Type.ERROR_MESSAGE);
//            return null;
//        }
//        return fos;
		baos = new ByteArrayOutputStream();
		return baos;
	}
}
