package org.aksw.mole.ore.dataset;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;

import org.aksw.mole.ore.repository.OntologyRepositoryEntry;
import org.aksw.mole.ore.repository.tones.TONESRepository;

public class TONESDataset extends AbstractOWLOntologyDataset{
	
	private static final String name = "TONES";

	public TONESDataset() {
		super(name);
	}
	
	@Override
	protected void addOntologyURLs() {
		TONESRepository tones = new TONESRepository();
		tones.initialize();
		for (OntologyRepositoryEntry entry : tones.getEntries()) {
			try {
				String name = URLEncoder.encode(entry.getOntologyShortName(), "UTF-8");
				ontologyURLs.put(entry.getPhysicalURI().toURL(), name);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	

}
