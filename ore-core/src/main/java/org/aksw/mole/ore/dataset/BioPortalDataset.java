package org.aksw.mole.ore.dataset;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.aksw.mole.ore.repository.OntologyRepositoryEntry;
import org.aksw.mole.ore.repository.bioportal.BioPortalRepository;

public class BioPortalDataset extends AbstractOWLOntologyDataset{
	
	private static final String name = "BioPortal";
	
	public BioPortalDataset() {
		super(name);
	}

	@Override
	protected void addOntologyURLs() {
		BioPortalRepository bioportal = new BioPortalRepository();
		bioportal.initialize();
		for (OntologyRepositoryEntry entry : bioportal.getEntries()) {
			try {
				String name = URLEncoder.encode(entry.getOntologyShortName(), "UTF-8");
				super.ontologyURLs.put(entry.getPhysicalURI().toURL(), name);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

}
