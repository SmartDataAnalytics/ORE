package org.aksw.mole.ore.sparql.generator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.aksw.mole.ore.sparql.AxiomGenerationTracker;
import org.aksw.mole.ore.sparql.InconsistencyFinder;
import org.aksw.mole.ore.sparql.LinkedDataDereferencer;
import org.aksw.mole.ore.sparql.TimeOutException;
import org.aksw.mole.ore.sparql.generator.generic.AsymmetricPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ClassAssertionAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DataPropertyDomainAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DataPropertyRangeAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DisjointClassesAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.DomainAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.EquivalentClassesAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.EquivalentPropertiesAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.FunctionalPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.InverseFunctionalPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.InverseOfAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.IrreflexivePropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ObjectPropertyDomainAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ObjectPropertyRangeAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.PropertyAssertionAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.RangeAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.ReflexivePropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.SubClassOfAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.SubPropertyOfAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.SymmetricPropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.generator.generic.TransitivePropertyAxiomGenerator;
import org.aksw.mole.ore.sparql.trivial.TrivialInconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.base.Stopwatch;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

public class SPARQLBasedInconsistencyFinder implements InconsistencyFinder {
	
	private static final Logger logger = LoggerFactory.getLogger(SPARQLBasedInconsistencyFinder.class);
	
	private Stopwatch stopWatch = new Stopwatch();
	private Monitor consistencyMonitor = MonitorFactory.getTimeMonitor("consistency checks");
	private Monitor overallMonitor = MonitorFactory.getTimeMonitor("overall runtime");
	
	private volatile boolean stop = false;
	
	private long maxRuntimeInMilliseconds = 
											TimeUnit.MINUTES.toMillis(1);
//											TimeUnit.HOURS.toMillis(1);
	
	//terminate algorithm if first inconsistency found, otherwise until stop is called or timeout occurs
	private boolean stopIfInconsistencyFound = true;
	//use linked data to get more information
	private boolean useLinkedData = true;
	private Set<String> linkedDataNamespaces = new HashSet<String>();
	private LinkedDataDereferencer linkedDataDereferencer = new LinkedDataDereferencer();

	private SparqlEndpointKS ks;
	
	private OWLReasonerFactory reasonerFactory;
	private OWLReasoner reasoner;
	private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	private OWLOntology fragment;
	
	private TrivialInconsistencyFinder trivialInconsistencyFinder;
	private List<Class<? extends SPARQLBasedGeneralAxiomGenerator>> generalAxiomGeneratorsClasses = 
			new ArrayList<Class<? extends SPARQLBasedGeneralAxiomGenerator>>();
	private List<SPARQLBasedGeneralAxiomGenerator> generalAxiomGenerators = 
			new ArrayList<SPARQLBasedGeneralAxiomGenerator>();

	private Set<OWLAxiom> axiomsToIgnore = new HashSet<OWLAxiom>();
	
	private AxiomGenerationTracker tracker;
	
	public SPARQLBasedInconsistencyFinder(SparqlEndpointKS ks) {
		this(ks, PelletReasonerFactory.getInstance());
	}
	
	public SPARQLBasedInconsistencyFinder(SparqlEndpointKS ks, OWLReasonerFactory reasonerFactory) {
		this.ks = ks;
		this.reasonerFactory = reasonerFactory;
		
		//class axioms
		generalAxiomGeneratorsClasses.add(SubClassOfAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(EquivalentClassesAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(DisjointClassesAxiomGenerator.class);
		//property axioms
		generalAxiomGeneratorsClasses.add(SubPropertyOfAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(EquivalentPropertiesAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(DomainAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(ObjectPropertyDomainAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(DataPropertyDomainAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(RangeAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(ObjectPropertyRangeAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(DataPropertyRangeAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(FunctionalPropertyAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(InverseFunctionalPropertyAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(SymmetricPropertyAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(AsymmetricPropertyAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(ReflexivePropertyAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(IrreflexivePropertyAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(TransitivePropertyAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(InverseOfAxiomGenerator.class);
		//individual axioms
		generalAxiomGeneratorsClasses.add(ClassAssertionAxiomGenerator.class);
		generalAxiomGeneratorsClasses.add(PropertyAssertionAxiomGenerator.class);
		
		//instantiate the entity independent axiom generators
		for (Class<? extends SPARQLBasedGeneralAxiomGenerator> cls : generalAxiomGeneratorsClasses) {
			try {
				SPARQLBasedGeneralAxiomGenerator generator = cls.getConstructor(SparqlEndpointKS.class).newInstance(ks);
				generalAxiomGenerators.add(generator);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
		
		//create a helper which looks for trivial cases for inconsistency
		trivialInconsistencyFinder = new TrivialInconsistencyFinder(ks);
		reset();
	}
	
	public void reset(){
		//create empty ontology to which retrieved axioms are added
		try {
			fragment = manager.createOntology();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		//create the reasoner which is used to check for consistency
		reasoner = reasonerFactory.createNonBufferingReasoner(fragment);
		//create an helper which looks for trivial cases for inconsistency
		trivialInconsistencyFinder = new TrivialInconsistencyFinder(ks);
	}

	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		stop = false;
		stopWatch.start();
		//firstly, check for trivial inconsistency cases
		logger.info("Looking for trivial inconsistency cases...");
		Set<OWLAxiom> trivialInconsistentFragment = trivialInconsistencyFinder.getInconsistentFragment();
		tracker.track(trivialInconsistencyFinder, trivialInconsistentFragment);
		addAxioms(trivialInconsistencyFinder, trivialInconsistentFragment);
		if(terminationCriteriaSatisfied()){
			logger.info("Found axioms leading to trivial inconsistency.");
			if(stopIfInconsistencyFound){
				logger.info("Early termination.");
			}
		}
		
		//iteration starts here
		int i = 1;
		while(!terminationCriteriaSatisfied()){
			logger.info("Iteration " + i++ + "...");
			showStatistics();
			//for each entity independent axiom generator
			for (SPARQLBasedGeneralAxiomGenerator generator : generalAxiomGenerators) {
				//generate the 'next' set of axioms and add it to the working ontology
				if(generator.hasNext()){
					Set<OWLAxiom> axioms = generator.nextAxioms();
					addAxioms(generator, axioms);
					//check for consistency or other termination criteria
					boolean terminate = terminationCriteriaSatisfied();
					if(terminate){
						stopWatch.stop();
						return fragment.getAxioms();
					}
				}
			}
			//get axioms available via linked data
			if(useLinkedData){
				List<OWLEntity> entities = new ArrayList<OWLEntity>();
				entities.addAll(fragment.getClassesInSignature());
				entities.addAll(fragment.getObjectPropertiesInSignature());
				entities.addAll(fragment.getDataPropertiesInSignature());
				entities.addAll(fragment.getIndividualsInSignature());
				for(OWLEntity entity : entities){
					for (String ns : linkedDataNamespaces) {
						if(entity.toStringID().startsWith(ns)){
							try {
								Set<OWLAxiom> axioms = linkedDataDereferencer.dereference(entity);
								addAxioms(linkedDataDereferencer, axioms);
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		stopWatch.stop();
		return fragment.getAxioms();
	}
	
	@Override
	public void setMaximumRuntime(long duration, TimeUnit timeUnit) {
		this.maxRuntimeInMilliseconds = timeUnit.toMillis(duration);
	}
	
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		this.axiomsToIgnore = axiomsToIgnore;
		trivialInconsistencyFinder.setAxiomsToIgnore(axiomsToIgnore);
		manager.removeAxioms(fragment, axiomsToIgnore);
	}
	
	public void setLinkedDataNamespaces(Set<String> linkedDataNamespaces) {
		this.linkedDataNamespaces = linkedDataNamespaces;
	}
	
	public void setUseLinkedData(boolean useLinkedData) {
		this.useLinkedData = useLinkedData;
	}
	
	public void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound) {
		this.stopIfInconsistencyFound = stopIfInconsistencyFound;
	}
	
	public void stop() {
		this.stop = true;
	}
	
	public void setAxiomGenerationTracker(AxiomGenerationTracker tracker){
		this.tracker = tracker;
		trivialInconsistencyFinder.setAxiomGenerationTracker(tracker);
	}
	
	private void addAxioms(AxiomGenerator generator, Set<OWLAxiom> axioms){
		if(tracker != null){
			tracker.track(generator, axioms);
		}
		Set<OWLAxiom> axiomsWithoutIgnored = new HashSet<OWLAxiom>(axioms);
		axiomsWithoutIgnored.removeAll(axiomsToIgnore);
		manager.addAxioms(fragment, axiomsWithoutIgnored);
	}
	
	private boolean timeExpired(){
		boolean timeOut = stopWatch.elapsed(TimeUnit.MILLISECONDS) >= maxRuntimeInMilliseconds;
		return timeOut;
	}
	
	public void showStatistics(){
		logger.info("Fragment size: " + fragment.getLogicalAxiomCount() + " logical axioms");
		logger.info("TBox size: " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
				new ArrayList<AxiomType<?>>(AxiomType.TBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms");
		logger.info("RBox size: " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
				new ArrayList<AxiomType<?>>(AxiomType.RBoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms");
		logger.info("ABox size: " + AxiomType.getAxiomsOfTypes(fragment.getAxioms(), 
				new ArrayList<AxiomType<?>>(AxiomType.ABoxAxiomTypes).toArray(new AxiomType[]{})).size() + " axioms");
	}
	
	private boolean terminationCriteriaSatisfied() throws TimeOutException {
		if(stop){
			return true;
		} else {
			//check for timeout
			boolean timeOut = timeExpired();
			if(timeOut){
				consistencyMonitor.start();
				boolean consistent = reasoner.isConsistent();
				consistencyMonitor.stop();
				if(!consistent){
					return true;
				}
				throw new TimeOutException();
			} else if(stopIfInconsistencyFound){
				//check for consistency
				consistencyMonitor.start();
				boolean consistent = reasoner.isConsistent();
				consistencyMonitor.stop();
				return !consistent;
			}
		}
		return false;
	}

	@Override
	public int compareTo(AxiomGenerator o) {
		return 0;
	}
}
