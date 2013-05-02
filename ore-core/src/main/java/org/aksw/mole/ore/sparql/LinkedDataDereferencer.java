package org.aksw.mole.ore.sparql;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.generator.AxiomGenerator;
import org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class LinkedDataDereferencer implements AxiomGenerator {
	
	private static final Logger logger = LoggerFactory.getLogger(SPARQLBasedInconsistencyFinder.class);
	
	protected OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
	
	private LoadingCache<OWLEntity, Set<OWLAxiom>> cache = CacheBuilder.newBuilder()
		       .maximumSize(1000)
		       .expireAfterWrite(10, TimeUnit.MINUTES)
		       .build(
		           new CacheLoader<OWLEntity, Set<OWLAxiom>>() {
		             public Set<OWLAxiom> load(OWLEntity key) {
		               return dereference(key.getIRI());
		             }
		           });

	public LinkedDataDereferencer() {
	
	}
	
	public Set<OWLAxiom> dereference(String iri){
		logger.info("Dereferencing " + iri + "...");
		Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
    	try {
			URLConnection conn = new URL(iri).openConnection();
			conn.setRequestProperty("Accept", "application/rdf+xml");
			Model model = ModelFactory.createDefaultModel();
			InputStream in = conn.getInputStream();
			model.read(in, null);
			OWLOntology ontology = convert(model);
			axioms.addAll(ontology.getLogicalAxioms());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	logger.info("Got " + axioms.size() + " axioms.");
    	return axioms;
	}
	
	public Set<OWLAxiom> dereference(IRI iri){
		return dereference(iri.toString());
	}
	
	public Set<OWLAxiom> dereference(OWLEntity entity) throws ExecutionException{
		return cache.get(entity);
	}
	
	private OWLOntology convert(Model model){
		OWLOntology ontology = null;
		ByteArrayOutputStream baos = null;
		ByteArrayInputStream bais = null;
		try {
			baos = new ByteArrayOutputStream();
			model.write(baos, "TURTLE");
			bais = new ByteArrayInputStream(baos.toByteArray());
			ontology = ontologyManager.loadOntologyFromOntologyDocument(bais);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} finally {
			try {
				baos.close();
				bais.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ontology;
	}

	@Override
	public int compareTo(AxiomGenerator other) {
		return getClass().getName().compareTo(other.getClass().getName());
	}

}
