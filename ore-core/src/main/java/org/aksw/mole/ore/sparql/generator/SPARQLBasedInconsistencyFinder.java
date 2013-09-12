package org.aksw.mole.ore.sparql.generator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.aksw.mole.ore.sparql.AxiomGenerationTracker;
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
import org.aksw.mole.ore.sparql.trivial.SPARQLBasedTrivialInconsistencyFinder;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class SPARQLBasedInconsistencyFinder extends AbstractSPARQLBasedInconsistencyFinder {
	
	private static final Logger logger = LoggerFactory.getLogger(SPARQLBasedInconsistencyFinder.class);
	
	private SPARQLBasedTrivialInconsistencyFinder trivialInconsistencyFinder;
	private List<Class<? extends SPARQLBasedGeneralAxiomGenerator>> generalAxiomGeneratorsClasses = 
			new ArrayList<Class<? extends SPARQLBasedGeneralAxiomGenerator>>();
	private List<SPARQLBasedGeneralAxiomGenerator> generalAxiomGenerators = 
			new ArrayList<SPARQLBasedGeneralAxiomGenerator>();

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
		trivialInconsistencyFinder = new SPARQLBasedTrivialInconsistencyFinder(ks);
		reset();
	}
	
	@Override
	public Set<OWLAxiom> getInconsistentFragment() throws TimeOutException {
		stop = false;
		stopWatch.start();
		//firstly, check for trivial inconsistency cases
		logger.info("Looking for trivial inconsistency cases...");
		Set<OWLAxiom> trivialInconsistentFragment = trivialInconsistencyFinder.getInconsistentFragment();
		if(tracker != null){
			tracker.track(trivialInconsistencyFinder, trivialInconsistentFragment);
		}
		addAxioms(trivialInconsistencyFinder, trivialInconsistentFragment);
		if(!terminationCriteriaSatisfied()){//!fragment.getLogicalAxioms().isEmpty()){
			logger.info("Found axioms leading to trivial inconsistency.");
			if(stopIfInconsistencyFound){
				logger.info("Early termination.");
			}
			return fragment.getAxioms();
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
						if(!terminationCriteriaSatisfied()){
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
		}
		stopWatch.stop();
		return fragment.getAxioms();
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#isConsistent()
	 */
	@Override
	protected boolean isConsistent() {
		return reasoner.isConsistent();
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setAxiomsToIgnore(java.util.Set)
	 */
	@Override
	public void setAxiomsToIgnore(Set<OWLAxiom> axiomsToIgnore) {
		super.setAxiomsToIgnore(axiomsToIgnore);
		trivialInconsistencyFinder.setAxiomsToIgnore(axiomsToIgnore);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#addProgressMonitor(org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor)
	 */
	@Override
	public void addProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		super.addProgressMonitor(mon);
		trivialInconsistencyFinder.addProgressMonitor(mon);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#removeProgressMonitor(org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder.SPARQLBasedInconsistencyProgressMonitor)
	 */
	@Override
	public void removeProgressMonitor(SPARQLBasedInconsistencyProgressMonitor mon) {
		super.removeProgressMonitor(mon);
		trivialInconsistencyFinder.removeProgressMonitor(mon);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#reset()
	 */
	@Override
	public void reset() {
		super.reset();
		// create an helper which looks for trivial cases for inconsistency
		trivialInconsistencyFinder = new SPARQLBasedTrivialInconsistencyFinder(ks);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setStopIfInconsistencyFound(boolean)
	 */
	@Override
	public void setStopIfInconsistencyFound(boolean stopIfInconsistencyFound) {
		super.setStopIfInconsistencyFound(stopIfInconsistencyFound);
		trivialInconsistencyFinder.setStopIfInconsistencyFound(stopIfInconsistencyFound);
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.mole.ore.sparql.generator.AbstractSPARQLBasedInconsistencyFinder#setAxiomGenerationTracker(org.aksw.mole.ore.sparql.AxiomGenerationTracker)
	 */
	@Override
	public void setAxiomGenerationTracker(AxiomGenerationTracker tracker) {
		super.setAxiomGenerationTracker(tracker);
		trivialInconsistencyFinder.setAxiomGenerationTracker(tracker);
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
	
	public static void main(String[] args) throws Exception {
		Set<OWLAxiom> inconsistentFragment = new SPARQLBasedInconsistencyFinder(new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpediaLiveAKSW())).getInconsistentFragment();
		System.out.println(inconsistentFragment);
	}
}
