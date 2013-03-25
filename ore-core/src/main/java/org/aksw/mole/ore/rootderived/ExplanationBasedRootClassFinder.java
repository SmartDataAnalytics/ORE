package org.aksw.mole.ore.rootderived;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.mole.ore.explanation.api.Explanation;
import org.aksw.mole.ore.explanation.impl.PelletExplanationGenerator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.SimpleRenderer;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class ExplanationBasedRootClassFinder implements RootClassFinder {

	private OWLOntologyManager manager;
	  private OWLReasoner baseReasoner;
	  private OWLReasonerFactory reasonerFactory;
	  private Map<OWLClass, Set<Explanation>> cls2JustificationMap;
	  private Set<OWLClass> roots = new HashSet<OWLClass>();

	  public ExplanationBasedRootClassFinder(OWLOntologyManager manager, OWLReasoner baseReasoner, OWLReasonerFactory reasonerFactory)
	  {
	    this.manager = manager;
	    this.baseReasoner = baseReasoner;
	    this.reasonerFactory = reasonerFactory;
	  }

	  public Set<OWLClass> getRootUnsatisfiableClasses()
	  {
//	    StructureBasedRootClassFinder srd = new StructureBasedRootClassFinder(this.baseReasoner);
	    StructuralRootDerivedReasoner srd = new StructuralRootDerivedReasoner(this.manager, this.baseReasoner, this.reasonerFactory);
	    Set<OWLClass> estimatedRoots = srd.getRootUnsatisfiableClasses();
	    this.cls2JustificationMap = new HashMap<OWLClass, Set<Explanation>>();
	    Set<OWLAxiom> allAxioms = new HashSet<OWLAxiom>();
	    
	    for (OWLOntology ont : this.baseReasoner.getRootOntology().getImportsClosure()) {
	      allAxioms.addAll(ont.getLogicalAxioms());
	    }

	    for (OWLClass cls : estimatedRoots) {
	      this.cls2JustificationMap.put(cls, new HashSet<Explanation>());
	      System.out.println("POTENTIAL ROOT: " + cls);
	    }
	    System.out.println("Finding real roots from " + estimatedRoots.size() + " estimated roots");

	    int done = 0;
	    this.roots.addAll(estimatedRoots);
	    for (OWLClass estimatedRoot : estimatedRoots) {
	      try {
			PelletExplanationGenerator gen = new PelletExplanationGenerator(manager.createOntology(allAxioms));
			  OWLDataFactory df = this.manager.getOWLDataFactory();
			  Set<Explanation> expls = gen.getExplanations(df.getOWLSubClassOfAxiom(estimatedRoot, df.getOWLNothing()));
			  cls2JustificationMap.get(estimatedRoot).addAll(expls);
			  ++done;
			  System.out.println("Done " + done);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	    }
	    for (OWLClass clsA : estimatedRoots) {
	      for (OWLClass clsB : estimatedRoots)
	        if (!clsA.equals(clsB)) {
	          Set<Explanation> clsAExpls = cls2JustificationMap.get(clsA);
	          Set<Explanation> clsBExpls = cls2JustificationMap.get(clsB);
	          boolean clsARootForClsB = false;
	          boolean clsBRootForClsA = false;

	          for (Explanation clsAExpl : clsAExpls) {
	            for (Explanation clsBExpl : clsBExpls)
	              if (isRootFor(clsAExpl, clsBExpl))
	              {
	                clsARootForClsB = true;
	              }
	              else if (isRootFor(clsBExpl, clsAExpl))
	              {
	                clsBRootForClsA = true;
	              } }

	          Explanation clsAExpl;
	          if ((!clsARootForClsB) || (!clsBRootForClsA))
	            if (clsARootForClsB) {
	              this.roots.remove(clsB);
	            }
	            else if (clsBRootForClsA)
	              this.roots.remove(clsA);
	        } }

	    OWLClass clsA;
	    return this.roots;
	  }

	  private static boolean isRootFor(Explanation explA, Explanation explB) {
	    return (explB.getAxioms().containsAll(explA.getAxioms())) && (!explA.getAxioms().equals(explB.getAxioms()));
	  }

	  public Set<OWLClass> getDependentChildClasses(OWLClass cls)
	  {
	    return null;
	  }

	  public Set<OWLClass> getDependentDescendantClasses(OWLClass cls) {
	    return null;
	  }

	public static void main(String[] args) {
		try {
			SimpleRenderer renderer = new SimpleRenderer();
			renderer.setShortFormProvider(new DefaultPrefixManager("http://www.mindswap.org/ontologies/tambis-full.owl#"));
			ToStringRenderer.getInstance().setRenderer(renderer);
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ont = man.loadOntology(IRI.create("http://owl.cs.manchester.ac.uk/repository/download?ontology=http://www.cs.manchester.ac.uk/owl/ontologies/tambis-patched.owl"));

			System.out.println("Loaded!");
			OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
			OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ont);
			reasoner.getUnsatisfiableClasses();
			ExplanationBasedRootClassFinder rdr = new ExplanationBasedRootClassFinder(man, reasoner, reasonerFactory);
			for (OWLClass cls : rdr.getRootUnsatisfiableClasses())
				System.out.println("ROOT! " + cls);
		} catch (TimeOutException e) {
			e.printStackTrace();
		} catch (ReasonerInterruptedException e) {
			e.printStackTrace();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}

	}

	@Override
	public Set<OWLClass> getDerivedUnsatisfiableClasses() {
		// TODO Auto-generated method stub
		return null;
	}

}
