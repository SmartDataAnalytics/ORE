package experiments;

import java.io.File;
import java.util.Collection;

import org.aksw.mole.ore.dataset.BioPortalDataset;
import org.aksw.mole.ore.dataset.OWLOntologyDataset;
import org.aksw.mole.ore.dataset.TONESDataset;
import org.semanticweb.owlapi.model.OWLOntology;

public class JustificationClustering {
	

	public JustificationClustering() {
		
	}
	
	public void run(){
//		OWLOntologyDataset dataset = new TONESDataset();
		OWLOntologyDataset dataset = new BioPortalDataset();
		Collection<OWLOntology> ontologies = dataset.loadOntologies();
	}
	
	public static void main(String[] args) throws Exception {
		new JustificationClustering().run();
	}

}
