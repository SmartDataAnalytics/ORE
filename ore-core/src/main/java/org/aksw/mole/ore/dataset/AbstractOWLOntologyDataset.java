package org.aksw.mole.ore.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.io.Files;

public abstract class AbstractOWLOntologyDataset implements OWLOntologyDataset{
	
	protected Collection<OWLOntology> ontologies = new TreeSet<OWLOntology>();
	protected Collection<OWLOntology> correctOntologies = new TreeSet<OWLOntology>();
	protected Collection<OWLOntology> incoherentOntologies = new TreeSet<OWLOntology>();
	protected Collection<OWLOntology> inconsistentOntologies = new TreeSet<OWLOntology>();
	
	protected String name;
	
	protected File directory;
	protected File correctSubdirectory;
	protected File inconsistentSubdirectory;
	protected File incoherentSubdirectory;
	protected File tooLargeSubdirectory;
	
	protected OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	
	protected Map<URL, String> ontologyURLs = new HashMap<URL, String>();
	
	public AbstractOWLOntologyDataset(String name) {
		this.name = name;
		//create file structure
		directory = new File(datasetDirectory, name);
		directory.mkdirs();
		correctSubdirectory = new File(directory, "correct");
		correctSubdirectory.mkdirs();
		incoherentSubdirectory = new File(directory, "incoherent");
		incoherentSubdirectory.mkdirs();
		inconsistentSubdirectory = new File(directory, "inconsistent");
		inconsistentSubdirectory.mkdirs();
		tooLargeSubdirectory = new File(directory, "too_large");
		tooLargeSubdirectory.mkdirs();
		addOntologyURLs();
		initialize();
	}

	public void initialize(){
		ExecutorService threadPool = Executors.newFixedThreadPool(8);
		for (java.util.Map.Entry<URL, String> entry : ontologyURLs.entrySet()) {
			System.out.println("Processing " + entry.getValue());
			URL url = entry.getKey();
//			loadOWLOntology(url);
			threadPool.submit(new OntologyLoadingTask(url));
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected abstract void addOntologyURLs();
	
	private void analyzeAndCategorizeOntology(OWLOntology ontology, String filename){
		System.out.println("Analyzing ontology...");
		OWLReasoner reasoner;
		try {
			Configuration conf = new Configuration();
			conf.ignoreUnsupportedDatatypes = true;
			reasoner = new Reasoner(conf, ontology);
			int logicalAxiomCount = ontology.getLogicalAxiomCount();
			boolean consistent = reasoner.isConsistent();
			Set<OWLClass> unsatisfiableClasses = null;
			File from = new File(man.getOntologyDocumentIRI(ontology).toURI());
			if(consistent){
				unsatisfiableClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
				if(!unsatisfiableClasses.isEmpty()){
					File to = new File(incoherentSubdirectory, filename);
					Files.move(from, to);
				} else {
					File to = new File(correctSubdirectory, filename);
					Files.move(from, to);
				}
			} else {
				File to = new File(inconsistentSubdirectory, filename);
				Files.move(from, to);
			}
			System.out.println(consistent + "\t" + logicalAxiomCount + "\t" + ((unsatisfiableClasses != null) ? unsatisfiableClasses.size() : "n/a"));
			reasoner.dispose();
		} catch (TimeOutException e) {
			e.printStackTrace();
		} catch (InconsistentOntologyException e) {
			e.printStackTrace();
		} catch (ReasonerInterruptedException e) {
			e.printStackTrace();
		} catch (Exception e){e.printStackTrace();
			reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
			int logicalAxiomCount = ontology.getLogicalAxiomCount();
			boolean consistent = reasoner.isConsistent();
			Set<OWLClass> unsatisfiableClasses = null;
			if(consistent){
				unsatisfiableClasses = reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
			}
			System.out.println(consistent + "\t" + logicalAxiomCount + "\t" + ((unsatisfiableClasses != null) ? unsatisfiableClasses.size() : "n/a"));
			reasoner.dispose();
		}
	}
	
	protected void loadOWLOntology(URL url) {
		boolean local = loadFromLocal(url);
		if(!local){
			try {
				File file = downloadFile(url);
				if(file != null){
//					OWLOntology ontology = man.loadOntologyFromOntologyDocument(file);
//					analyzeAndCategorizeOntology(ontology, getFilename(url));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean loadFromLocal(URL url){
		String filename = getFilename(url);
		for(File parent : Arrays.asList(directory, tooLargeSubdirectory, correctSubdirectory, incoherentSubdirectory, inconsistentSubdirectory)){
			File file = new File(parent, filename);
			if(file.exists()){
				if(!parent.equals(tooLargeSubdirectory)){
//					try {
//						OWLOntology ontology = man.loadOntologyFromOntologyDocument(file);
//						if(parent.equals(incoherentSubdirectory)){
//							incoherentOntologies.add(ontology);
//						} else if(parent.equals(inconsistentSubdirectory)){
//							inconsistentOntologies.add(ontology);
//						} else if(parent.equals(correctSubdirectory)){
//							correctOntologies.add(ontology);
//						} else {
//							analyzeAndCategorizeOntology(ontology, filename);
//						}
//					} catch (OWLOntologyCreationException e) {
//						e.printStackTrace();
//					}
				}
				return true;
			}
		}
		return false;
	}
	
	private String getFilename(URL url){
		return ontologyURLs.get(url);
//		String filename = url.toString().substring(url.toString().lastIndexOf("/"));
//		return filename;
	}
	
	/**
	 * Download the file such that later on we can load it from the local file system.
	 */
	protected File downloadFile(URL url){
		
		String filename = getFilename(url);
		File file = new File(directory + "/" + filename);
		if(!file.exists()){
			System.out.print("Downloading file...");
			try {
				InputStream is = url.openConnection().getInputStream();
				OutputStream out = new FileOutputStream(file);
				int read = 0;
				byte[] bytes = new byte[1024];
 
				while ((read = is.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
 
				is.close();
				out.flush();
				out.close();
				System.out.println("done.");
				return file;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public Collection<OWLOntology> loadOntologies() {
		return ontologies;
	}

	@Override
	public Collection<OWLOntology> loadIncoherentOntologies() {
		return incoherentOntologies;
	}

	@Override
	public Collection<OWLOntology> loadInconsistentOntologies() {
		return inconsistentOntologies;
	}
	
	class OntologyLoadingTask implements Callable<OWLOntology>{
		
		private URL url;

		public OntologyLoadingTask(URL url) {
			this.url = url;
		}

		@Override
		public OWLOntology call() throws Exception {
			loadOWLOntology(url);
			return null;
		}

	}

}
