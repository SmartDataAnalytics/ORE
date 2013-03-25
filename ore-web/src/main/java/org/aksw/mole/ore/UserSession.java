package org.aksw.mole.ore;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import org.aksw.mole.ore.model.Knowledgebase;
import org.aksw.mole.ore.model.OWLOntologyKnowledgebase;
import org.aksw.mole.ore.model.SPARQLEndpointKnowledgebase;
import org.aksw.mole.ore.sparql.IncrementalInconsistencyFinder;
import org.dllearner.core.ComponentInitException;
import org.dllearner.kb.sparql.ExtractionDBCache;
import org.dllearner.reasoning.PelletReasoner;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.vaadin.jonatan.contexthelp.ContextHelp;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.vaadin.Application;
import com.vaadin.service.ApplicationContext;
import com.vaadin.service.ApplicationContext.TransactionListener;
import com.vaadin.terminal.gwt.server.WebApplicationContext;

public class UserSession implements TransactionListener, Serializable {

	private ResourceBundle bundle;
	private Locale locale; // Current locale
	private Application app; // For distinguishing between apps

	private static ThreadLocal<UserSession> instance = new ThreadLocal<UserSession>();
	
	private KnowledgebaseManager kbMan;
	private ExplanationManager2 expMan;
	private ImpactManager impMan;
	private LearningManager learnMan;
	private EnrichmentManager enrichMan;
	private OWLOntologyManager ontologyMan;
	private RepairManager repMan;
	private IncrementalInconsistencyFinder incFinder;
	
	private OWLReasoner reasoner;
	
	private Knowledgebase knowledgebase;
	
	private ContextHelp contextHelp;
	
	private static String cacheDir;
	private static String wordNetDir;
	private static String posTaggerModelsDir;

	public UserSession(OREApplication app) {
		this.app = app;
		
		loadSettings();

		// It's usable from now on in the current request
		instance.set(this);
		
		instance.get().kbMan = new KnowledgebaseManager();
	}
	
	private void loadSettings(){
		InputStream is;
		try {
			is = this.getClass().getClassLoader().getResourceAsStream("settings.ini");
			if(is != null){
				Ini ini = new Ini(is);
				//base section
				Section baseSection = ini.get("base");
				cacheDir = baseSection.get("cacheDir", String.class).trim();
				wordNetDir = baseSection.get("wordNetDir", String.class).trim();
				posTaggerModelsDir = baseSection.get("posTaggerModelsDir", String.class).trim();
			}
			
			ApplicationContext ctx = app.getContext();
			WebApplicationContext webCtx = (WebApplicationContext) ctx;
			ServletContext sc = webCtx.getHttpSession().getServletContext();
			
			if(cacheDir == null || cacheDir.isEmpty()){
				cacheDir = sc.getRealPath("cache");
			}
			
			if(wordNetDir == null || wordNetDir.isEmpty()){
				wordNetDir = this.getClass().getClassLoader().getResource("wordnet").getPath();
			}
			
			if(posTaggerModelsDir == null || posTaggerModelsDir.isEmpty()){
				posTaggerModelsDir = this.getClass().getClassLoader().getResource("postagger").getPath();
			}
		} catch (InvalidFileFormatException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
	}

	@Override
	public void transactionStart(Application application, Object transactionData) {
		// Set this data instance of this application
		// as the one active in the current thread.
		if (this.app == application)
			instance.set(this);
	}

	@Override
	public void transactionEnd(Application application, Object transactionData) {
		// Clear the reference to avoid potential problems
		if (this.app == application)
			instance.set(null);
	}

	public static void initLocale(Locale locale, String bundleName) {
		instance.get().locale = locale;
		instance.get().bundle = ResourceBundle.getBundle(bundleName, locale);
	}
	
	public static String getCacheDirectory(){
		return cacheDir;
	}
	
	public static String getWordNetDirectory() {
		return wordNetDir;
	}
	
	public static String getPosTaggerModelsDirectory() {
		return posTaggerModelsDir;
	}

	public static Locale getLocale() {
		return instance.get().locale;
	}
	
	public static UserSession getCurrentSession() {
		return instance.get();
	}

	public static String getMessage(String msgId) {
		return instance.get().bundle.getString(msgId);
	}
	
	public static KnowledgebaseManager getKnowledgebaseManager() {
		return instance.get().kbMan;
	}
	
	public static void setExplanationManager(ExplanationManager2 expMan) {
        instance.get().expMan = expMan;
    }
	
	public static ExplanationManager2 getExplanationManager() {
        return instance.get().expMan;
    }
	
	public static OWLReasoner getReasoner() {
		return instance.get().reasoner;
	}
	
	public static void setReasoner(OWLReasoner reasoner) {
		instance.get().reasoner = reasoner;
	}
	
	public static void setRepairManager(RepairManager repMan) {
        instance.get().repMan = repMan;
    }
	
	public static RepairManager getRepairManager() {
        return instance.get().repMan;
    }
	
	public static void setImpactManager(ImpactManager impMan) {
        instance.get().impMan = impMan;
    }
	
	public static ImpactManager getImpactManager() {
        return instance.get().impMan;
    }
	
	public static void setLearningManager(LearningManager learnMan) {
        instance.get().learnMan = learnMan;
    }
	
	public static LearningManager getLearningManager() {
        return instance.get().learnMan;
    }
	
	public static void setEnrichmentManager(EnrichmentManager enrichMan) {
        instance.get().enrichMan = enrichMan;
    }
	
	public static EnrichmentManager getEnrichmentManager() {
        return instance.get().enrichMan;
    }
	
	public static IncrementalInconsistencyFinder getIncrementalInconsistencyFinder() {
        return instance.get().incFinder;
    }

	public static void initialize(Knowledgebase knowledgebase){
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		if(knowledgebase instanceof OWLOntologyKnowledgebase){
			OWLOntologyKnowledgebase ontologyKB = (OWLOntologyKnowledgebase) knowledgebase;
			PelletReasoner reasoner = new PelletReasoner((com.clarkparsia.pellet.owlapiv3.PelletReasoner)ontologyKB.getReasoner());
			try {
				reasoner.init();
			} catch (ComponentInitException e) {
				e.printStackTrace();
			}
			instance.get().learnMan = new LearningManager(reasoner);
			instance.get().expMan = new ExplanationManager2(ontologyKB.getReasoner(), reasonerFactory);
			instance.get().repMan = new RepairManager(ontologyKB.getOntology());
			instance.get().impMan = new ImpactManager(ontologyKB.getReasoner());
		} else if(knowledgebase instanceof SPARQLEndpointKnowledgebase){
			instance.get().enrichMan = new EnrichmentManager(((SPARQLEndpointKnowledgebase) knowledgebase).getEndpoint(), new ExtractionDBCache(UserSession.getCacheDirectory()));
			instance.get().incFinder = new IncrementalInconsistencyFinder(((SPARQLEndpointKnowledgebase) knowledgebase).getEndpoint(), UserSession.getCacheDirectory());
			instance.get().repMan = new RepairManager(instance.get().incFinder.getOntology());
			instance.get().expMan = new ExplanationManager2(instance.get().incFinder.getOWLReasoner(), reasonerFactory);
		}
	}
	
	public static ContextHelp getContextHelp() {
		return instance.get().contextHelp;
	}
	
	public static void setContextHelp(ContextHelp contextHelp) {
		instance.get().contextHelp = contextHelp;
	}

}
