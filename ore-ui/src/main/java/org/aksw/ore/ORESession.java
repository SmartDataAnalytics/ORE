/**
 * 
 */
package org.aksw.ore;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.extra.CacheCoreEx;
import org.aksw.jena_sparql_api.cache.extra.CacheCoreH2;
import org.aksw.jena_sparql_api.cache.extra.CacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheExImpl;
import org.aksw.mole.ore.sparql.generator.SPARQLBasedInconsistencyFinder;
import org.aksw.ore.manager.ConstraintValidationManager;
import org.aksw.ore.manager.EnrichmentManager;
import org.aksw.ore.manager.ExplanationManager;
import org.aksw.ore.manager.KnowledgebaseManager;
import org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener;
import org.aksw.ore.manager.LearningManager;
import org.aksw.ore.manager.RepairManager;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.model.SPARQLEndpointKnowledgebase;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.PelletReasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

/**
 * @author Lorenz Buehmann
 *
 */
public class ORESession extends VaadinSession implements KnowledgebaseLoadingListener{

	private static final long serialVersionUID = -3399015588524238428L;
	
	public ORESession(VaadinService service) {
		super(service);
	}
	
	public static void init(){
		try {
			//KB manager
			KnowledgebaseManager kbMan = new KnowledgebaseManager();
			VaadinSession.getCurrent().setAttribute(KnowledgebaseManager.class, kbMan);
//			kbMan.addListener(this);
			//use Pellet as OWL reasoner
			OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
			
			//dummy ontology
			OWLOntology ontology = null;
			try {
				OWLOntologyManager man = OWLManager.createOWLOntologyManager();
				ontology = man.loadOntology(IRI.create("http://xmlns.com/foaf/spec/20100809.rdf"));
				
				OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
				VaadinSession.getCurrent().setAttribute(OWLReasoner.class, reasoner);
				//dummy explanation manager
				ExplanationManager expMan = new ExplanationManager(reasoner, reasonerFactory);
				VaadinSession.getCurrent().setAttribute(ExplanationManager.class, expMan);
				//dummy repair manager
				RepairManager repMan = new RepairManager(ontology);
				VaadinSession.getCurrent().setAttribute(RepairManager.class, repMan);
				//learning manager
				PelletReasoner pelletReasoner = new PelletReasoner((com.clarkparsia.pellet.owlapiv3.PelletReasoner)reasoner);
				try {
					pelletReasoner.init();
				} catch (ComponentInitException e) {
					e.printStackTrace();
				}
				LearningManager learnMan = new LearningManager(pelletReasoner);
				VaadinSession.getCurrent().setAttribute(LearningManager.class, learnMan);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//dummy SPARQL endpoint
			try {
				SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
				SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
				//SPARQL cache
				long timeToLive = TimeUnit.DAYS.toMillis(30);
				CacheCoreEx cacheBackend = CacheCoreH2.create(true, OREConfiguration.getCacheDirectory(), "sparql", timeToLive, true);
				CacheEx cache = new CacheExImpl(cacheBackend);
				//dummy constraint manager
				ConstraintValidationManager valMan = new ConstraintValidationManager(ks, cache);
				VaadinSession.getCurrent().setAttribute(ConstraintValidationManager.class, valMan);
				//dummy enrichment manager
				EnrichmentManager enMan = new EnrichmentManager(ks.getEndpoint(), cache);
				VaadinSession.getCurrent().setAttribute(EnrichmentManager.class, enMan);
				//dummy incremental inconsistency finder
				SPARQLBasedInconsistencyFinder sparqlBasedInconsistencyFinder = new SPARQLBasedInconsistencyFinder(ks, reasonerFactory);
				VaadinSession.getCurrent().setAttribute(SPARQLBasedInconsistencyFinder.class, sparqlBasedInconsistencyFinder);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			//set current KB
			if(ontology != null){
				getKnowledgebaseManager().setKnowledgebase(new OWLOntologyKnowledgebase(ontology));
			}
//			kbMan.setKnowledgebase(new SPARQLEndpointKnowledgebase(endpoint));
			
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void initialize(Knowledgebase knowledgebase){
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			OWLOntologyKnowledgebase ontologyKB = (OWLOntologyKnowledgebase) knowledgebase;
			OWLOntology ontology = ontologyKB.getOntology();
			OWLReasoner reasoner = ontologyKB.getReasoner();
			VaadinSession.getCurrent().setAttribute(OWLReasoner.class, reasoner);
			
			PelletReasoner pelletReasoner = new PelletReasoner((com.clarkparsia.pellet.owlapiv3.PelletReasoner)reasoner);
			try {
				pelletReasoner.init();
			} catch (ComponentInitException e) {
				e.printStackTrace();
			}
			//learning manager
			LearningManager learnMan = new LearningManager(pelletReasoner);
			VaadinSession.getCurrent().setAttribute(LearningManager.class, learnMan);
			//explanation manager
			ExplanationManager expMan = new ExplanationManager(reasoner, reasonerFactory);
			VaadinSession.getCurrent().setAttribute(ExplanationManager.class, expMan);
			//repair manager
			RepairManager repMan = new RepairManager(ontology);
			VaadinSession.getCurrent().setAttribute(RepairManager.class, repMan);
			//impact manager
			
			
			
		} else if(knowledgebase instanceof SPARQLEndpointKnowledgebase){
			SparqlEndpoint endpoint = ((SPARQLEndpointKnowledgebase) knowledgebase).getEndpoint();
			SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
			//SPARQL cache
			CacheEx cache = null;
			try {
				long timeToLive = TimeUnit.DAYS.toMillis(30);
				CacheCoreEx cacheBackend = CacheCoreH2.create(true, OREConfiguration.getCacheDirectory(), "sparql", timeToLive, true);
				cache = new CacheExImpl(cacheBackend);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			//constraint manager
			ConstraintValidationManager valMan = new ConstraintValidationManager(ks, cache);
			VaadinSession.getCurrent().setAttribute(ConstraintValidationManager.class, valMan);
			//enrichment manager
			EnrichmentManager enMan = new EnrichmentManager(ks.getEndpoint(), cache);
			VaadinSession.getCurrent().setAttribute(EnrichmentManager.class, enMan);
			//incremental inconsistency finder
			SPARQLBasedInconsistencyFinder sparqlBasedInconsistencyFinder = new SPARQLBasedInconsistencyFinder(ks, reasonerFactory);
			VaadinSession.getCurrent().setAttribute(SPARQLBasedInconsistencyFinder.class, sparqlBasedInconsistencyFinder);
			//explanation manager
			ExplanationManager expMan = new ExplanationManager(sparqlBasedInconsistencyFinder.getReasoner(), reasonerFactory);
			VaadinSession.getCurrent().setAttribute(ExplanationManager.class, expMan);
			//repair manager
			RepairManager repMan = new RepairManager(sparqlBasedInconsistencyFinder.getReasoner().getRootOntology());
			VaadinSession.getCurrent().setAttribute(RepairManager.class, repMan);
		}
	}
	
	public static KnowledgebaseManager getKnowledgebaseManager(){
		return VaadinSession.getCurrent().getAttribute(KnowledgebaseManager.class);
	}
	
	public static ExplanationManager getExplanationManager(){
		return VaadinSession.getCurrent().getAttribute(ExplanationManager.class);
	}
	
	public static RepairManager getRepairManager(){
		return VaadinSession.getCurrent().getAttribute(RepairManager.class);
	}
	
	public static ConstraintValidationManager getConstraintValidationManager(){
		return VaadinSession.getCurrent().getAttribute(ConstraintValidationManager.class);
	}
	
	public static EnrichmentManager getEnrichmentManager(){
		return VaadinSession.getCurrent().getAttribute(EnrichmentManager.class);
	}
	
	public static LearningManager getLearningManager(){
		return VaadinSession.getCurrent().getAttribute(LearningManager.class);
	}
	
	public static SPARQLBasedInconsistencyFinder getSparqlBasedInconsistencyFinder(){
		return VaadinSession.getCurrent().getAttribute(SPARQLBasedInconsistencyFinder.class);
	}
	
	public static OWLReasoner getOWLReasoner(){
		return VaadinSession.getCurrent().getAttribute(OWLReasoner.class);
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseChanged(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseChanged(Knowledgebase knowledgebase) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseAnalyzed(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseAnalyzed(Knowledgebase knowledgebase) {
		initialize(knowledgebase);
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#knowledgebaseStatusChanged(org.aksw.ore.model.Knowledgebase)
	 */
	@Override
	public void knowledgebaseStatusChanged(Knowledgebase knowledgebase) {
	}

	/* (non-Javadoc)
	 * @see org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener#message(java.lang.String)
	 */
	@Override
	public void message(String message) {
	}

}
