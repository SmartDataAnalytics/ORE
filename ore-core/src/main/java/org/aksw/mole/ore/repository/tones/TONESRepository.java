package org.aksw.mole.ore.repository.tones;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.aksw.mole.ore.repository.OntologyRepository;
import org.aksw.mole.ore.repository.OntologyRepositoryEntry;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.util.OntologyIRIShortFormProvider;

import com.google.common.base.Strings;
import com.google.common.io.Files;

public class TONESRepository implements OntologyRepository{
	
	private final String repositoryName = "TONES";

    private final URI repositoryLocation = URI.create("http://rpc295.cs.man.ac.uk:8080/repository");

    private List<RepositoryEntry> entries;

    private OWLOntologyIRIMapper iriMapper;
    
    private boolean loadLocally = true;


    public TONESRepository() {
        entries = new ArrayList<RepositoryEntry>();
        iriMapper = new RepositoryIRIMapper();
    }

    @Override
    public void initialize() {
    	refresh();
    }


    public String getName() {
        return repositoryName;
    }


    public String getLocation() {
        return repositoryLocation.toString();
    }


    public void refresh() {
        fillRepository();
    }


    public Collection<OntologyRepositoryEntry> getEntries() {
        List<OntologyRepositoryEntry> ret = new ArrayList<OntologyRepositoryEntry>();
        ret.addAll(entries);
        return ret;
    }


    public List<Object> getMetaDataKeys() {
        return Collections.emptyList();
    }


    public void dispose() throws Exception {
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //  Implementation details


    private void fillRepository() {
        try {
            entries.clear();
            
            BufferedReader br;
            if(loadLocally){
            	br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("tones_repository_uris.txt")));
            } else {
            	 URI listURI = URI.create(repositoryLocation + "/list");
                 br = new BufferedReader(new InputStreamReader(listURI.toURL().openStream()));
            }
            String line;
            while((line = br.readLine()) != null) {
                try {
                    entries.add(new RepositoryEntry(new URI(line)));
                }
                catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
           br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class RepositoryEntry implements OntologyRepositoryEntry {

        private String shortName;

        private URI ontologyURI;

        private URI physicalURI;

        public RepositoryEntry(URI ontologyIRI) {
            this.ontologyURI = ontologyIRI;
            OntologyIRIShortFormProvider sfp = new OntologyIRIShortFormProvider();
            shortName = sfp.getShortForm(IRI.create(ontologyIRI));   System.out.println(ontologyIRI + "\n" +shortName);
            physicalURI = URI.create(repositoryLocation + "/download?ontology=" + ontologyIRI);
        }

        public String getOntologyShortName() {
            return shortName;
        }


        public URI getOntologyURI() {
            return ontologyURI;
        }


        public URI getPhysicalURI() {
            return physicalURI;
        }


        public String getMetaData(Object key) {
            return null;
        }

    }


    private class RepositoryIRIMapper implements OWLOntologyIRIMapper {

        public IRI getDocumentIRI(IRI iri) {
            for(RepositoryEntry entry : entries) {
                if(entry.getOntologyURI().equals(iri.toURI())) {
                    return IRI.create(entry.getPhysicalURI());
                }
            }
            return null;
        }
    }
    
    public static void main(String[] args) throws Exception {
    	System.out.println("koala.owl".substring(0,"koala.owl".length() - ".owl".length()));
    	OntologyIRIShortFormProvider sfp = new OntologyIRIShortFormProvider();
    	IRI iri = IRI.create("http://protege.stanford.edu/plugins/owl/owl-library/koala.owl");
		String shortForm = sfp.getShortForm(iri);
    	System.out.println(shortForm);
//		new TONESRepository().fillRepository();
	}
}
