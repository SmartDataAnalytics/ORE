package org.aksw.mole.ore.rootderived;

import com.clarkparsia.modularity.ModularityUtils;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

import java.util.*;

public class StructureBasedRootClassFinder implements RootClassFinder, OWLClassExpressionVisitor {
	
	private static final Logger log = Logger.getLogger(StructureBasedRootClassFinder.class);
	
	private OWLOntologyManager manager;
	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	
	private Set<OWLClass> depend2Classes;
	private OWLOntology ontology;
	
	
	private Set<OWLClass> rootClasses;
	private Set<OWLClass> derivedClasses;
	private Set<OWLClass> unsatClasses;
	
	private boolean ontologyChanged = true;
	
	
	private int depth;
	private Map<Integer, Set<OWLObjectAllValuesFrom>> depth2UniversalRestrictionPropertyMap;
    private Map<Integer, Set<OWLObjectPropertyExpression>> depth2ExistsRestrictionPropertyMap;
    
    private Map<OWLClass, Set<OWLClass>> child2Parents;
    private Map<OWLClass, Set<OWLClass>> parent2Children;
    
//    private Map<OWLClass, Map<OWLAxiom, Set<OWLClass>>> class2Dependency;
	
	public StructureBasedRootClassFinder(OWLReasoner reasoner){
		
		this.manager = OWLManager.createOWLOntologyManager();
		this.reasoner = reasoner;
		this.reasonerFactory = new PelletReasonerFactory();
		try {
			this.ontology = manager.createOntology(IRI.create("http://all"), reasoner.getRootOntology().getImportsClosure());
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		} catch (OWLOntologyChangeException e) {
			e.printStackTrace();
		}
		rootClasses = new HashSet<OWLClass>();
		derivedClasses = new HashSet<OWLClass>();
		unsatClasses = new HashSet<OWLClass>();
		
		depend2Classes = new HashSet<OWLClass>();
		depth2UniversalRestrictionPropertyMap = new HashMap<Integer, Set<OWLObjectAllValuesFrom>>();
		depth2ExistsRestrictionPropertyMap = new HashMap<Integer, Set<OWLObjectPropertyExpression>>();
		
		child2Parents = new HashMap<OWLClass, Set<OWLClass>>();
		parent2Children = new HashMap<OWLClass, Set<OWLClass>>();
		
//		class2Dependency = new HashMap<OWLClass, Map<OWLAxiom, Set<OWLClass>>>();
		
	}
	
	public void computeRootDerivedClasses(){
		log.info("Computing root/derived classes...");
		unsatClasses.clear();
		rootClasses.clear();
		derivedClasses.clear();
		depend2Classes.clear();
		depth2ExistsRestrictionPropertyMap.clear();
		depth2UniversalRestrictionPropertyMap.clear();
		child2Parents.clear();
		parent2Children.clear();
		computePossibleRoots();
		pruneRoots();
		derivedClasses.addAll(unsatClasses);
		derivedClasses.removeAll(rootClasses);
		
		rootClasses.remove(manager.getOWLDataFactory().getOWLNothing());
	}
	
	private void computePossibleRoots(){
		unsatClasses.addAll(reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom());
		
			for(OWLClass cls : unsatClasses){
				reset();
				for(OWLClassExpression equi : EntitySearcher.getEquivalentClasses(cls, ontology)){
					equi.accept(this);
				}
				for(OWLClassExpression sup : EntitySearcher.getSuperClasses(cls, ontology)){
					sup.accept(this);
				}
				for(Integer depth : depth2UniversalRestrictionPropertyMap.keySet()){
					Set<OWLObjectPropertyExpression> successors = depth2ExistsRestrictionPropertyMap.get(depth);
					if(successors != null){
						for(OWLObjectAllValuesFrom all : depth2UniversalRestrictionPropertyMap.get(depth)){
							if(successors.contains(all.getProperty())){
								depend2Classes.add(all.getFiller().asOWLClass());
							}
						}
					}
				}
				child2Parents.put(cls, depend2Classes);
				if(depend2Classes.isEmpty()){
					rootClasses.add(cls);	
				}
			}
		
	}
	
	private void pruneRoots() {
		try {
			Set<OWLClass> roots = new HashSet<OWLClass>(rootClasses);
			List<OWLOntologyChange> appliedChanges = new ArrayList<OWLOntologyChange>();
			Set<OWLClass> potentialRoots = new HashSet<OWLClass>();
			for (OWLDisjointClassesAxiom dis : new ArrayList<OWLDisjointClassesAxiom>(
					ontology.getAxioms(AxiomType.DISJOINT_CLASSES))) {
				for (OWLClass cls : roots) {
					if (dis.getSignature().contains(cls)) {
						OWLOntologyChange rem = new RemoveAxiom(ontology, dis);
						manager.applyChange(rem);
						appliedChanges.add(rem);
						for (OWLEntity ent : dis.getSignature()) {
							if (ent.isOWLClass()) {
								potentialRoots.add(ent.asOWLClass());
							}
						}
					}
				}
			}
			for (OWLClass cls : roots) {
				OWLOntologyChange add = new AddAxiom(ontology, manager.getOWLDataFactory()
						.getOWLDeclarationAxiom(cls));
				manager.applyChange(add);
				appliedChanges.add(add);
			}
			OWLReasoner checker = null;
			for (OWLClass root : new ArrayList<OWLClass>(roots)) {
				
				checker = reasonerFactory.createNonBufferingReasoner(manager.createOntology(ModularityUtils.extractModule
						(ontology, root.getSignature(), ModuleType.STAR)));
				if (!potentialRoots.contains(root) && checker.isSatisfiable(root)) {
					rootClasses.remove(root);
				}
			}
			for(OWLOntologyChange change: appliedChanges){
				if(change instanceof RemoveAxiom){
					manager.applyChange(new AddAxiom(ontology, change.getAxiom()));
				} else if(change instanceof AddAxiom){
					manager.applyChange(new RemoveAxiom(ontology, change.getAxiom()));
				}
			}
		} catch (OWLOntologyChangeException e) {
			e.printStackTrace();
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void reset(){
		depend2Classes.clear();
		depth2ExistsRestrictionPropertyMap.clear();
		depth2UniversalRestrictionPropertyMap.clear();
	}
	

	private void checkObjectRestriction(OWLQuantifiedObjectRestriction restr){
		OWLClassExpression filler = restr.getFiller();
		
			if(filler.isAnonymous()){
				depth++;
				filler.accept(this);
				depth--;
			} else {
				if(unsatClasses.contains(filler.asOWLClass())){
					depend2Classes.add(filler.asOWLClass());
					if(restr instanceof OWLObjectAllValuesFrom){
						addAllRestrictionProperty((OWLObjectAllValuesFrom) restr);
						return;
					}
				}
				addExistsRestrictionProperty(restr.getProperty());
			}
		
		
	}
	
	private void checkObjectCardinalityRestriction(OWLObjectCardinalityRestriction restr){
		OWLClassExpression filler = restr.getFiller();
		
			if(filler.isAnonymous()){
				depth++;
				filler.accept(this);
				depth--;
			} else {
				if(unsatClasses.contains(filler.asOWLClass())){
					depend2Classes.add(filler.asOWLClass());
				}
				addExistsRestrictionProperty(restr.getProperty());
			}
		
		
	}
	
	private void addExistsRestrictionProperty(OWLObjectPropertyExpression expr){
		Set<OWLObjectPropertyExpression> properties = depth2ExistsRestrictionPropertyMap.get(Integer.valueOf(depth));
		if(properties == null){
			properties = new HashSet<OWLObjectPropertyExpression>();
			depth2ExistsRestrictionPropertyMap.put(Integer.valueOf(depth), properties);
		}
		properties.add(expr);
	}
	
	private void addAllRestrictionProperty(OWLObjectAllValuesFrom all){
		Set<OWLObjectAllValuesFrom> properties = depth2UniversalRestrictionPropertyMap.get(Integer.valueOf(depth));
		if(properties == null){
			properties = new HashSet<OWLObjectAllValuesFrom>();
			depth2UniversalRestrictionPropertyMap.put(Integer.valueOf(depth), properties);
		}
		properties.add(all);
	}
	
	
	@Override
	public void visit(OWLClass cls) {	
			if(unsatClasses.contains(cls)) {
				depend2Classes.add(cls);
			}
	}

	@Override
	public void visit(OWLObjectIntersectionOf and) {
		
		
			for(OWLClassExpression op : and.getOperands()) {
				if(op.isAnonymous()){
					op.accept(this);
				} else if(unsatClasses.contains(op.asOWLClass())) {
					depend2Classes.add(op.asOWLClass());				
				}
			}
					
	}

	@Override
	public void visit(OWLObjectUnionOf or) {

		// check whether one of the union operands is satisfiable
		for (OWLClassExpression op : or.getOperands()) {
			if (!unsatClasses.contains(op)) {
				return;
			}
		}
		// all operands are unsatisfiable
		for (OWLClassExpression op : or.getOperands()) {
			if (op.isAnonymous()) {
				op.accept(this);
			} else {
				depend2Classes.add(op.asOWLClass());
			}
		}

	}
	
	@Override
	public void visit(OWLObjectAllValuesFrom all) {
		checkObjectRestriction(all);		
	}
	
	@Override
	public void visit(OWLObjectMinCardinality min) {
		checkObjectCardinalityRestriction(min);	
	}
	
	@Override
	public void visit(OWLObjectExactCardinality exact) {
		checkObjectCardinalityRestriction(exact);		
	}
	
	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		
	}
	
	@Override
	public void visit(OWLObjectHasSelf self) {
		addExistsRestrictionProperty(self.getProperty());	
	}
	
	@Override
	public void visit(OWLObjectSomeValuesFrom some) {
		checkObjectRestriction(some);		
	}

	@Override
	public void visit(OWLObjectComplementOf arg0) {	
	}

	@Override
	public void visit(OWLObjectHasValue arg0) {
	}

	@Override
	public void visit(OWLObjectOneOf arg0) {
	}

	@Override
	public void visit(OWLDataSomeValuesFrom arg0) {
	}

	@Override
	public void visit(OWLDataAllValuesFrom arg0) {
	}

	@Override
	public void visit(OWLDataHasValue arg0) {
	}

	@Override
	public void visit(OWLDataMinCardinality arg0) {
	}

	@Override
	public void visit(OWLDataExactCardinality arg0) {
	}

	@Override
	public void visit(OWLDataMaxCardinality arg0) {	
	}


	@Override
	public Set<OWLClass> getRootUnsatisfiableClasses() {
		if(ontologyChanged){
			computeRootDerivedClasses();
			ontologyChanged = false;
		}
		return new TreeSet<OWLClass>(rootClasses);
	}
	
	@Override
	public Set<OWLClass> getDerivedUnsatisfiableClasses() {
		if(ontologyChanged){
			computeRootDerivedClasses();
			ontologyChanged = false;
		}
		return new TreeSet<OWLClass>(derivedClasses);
	}

	@Override
	public Set<OWLClass> getDependentChildClasses(OWLClass unsatClass) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void refresh(){
		log.info("Refreshing root finder");
		ontologyChanged = true;
	}
	
	public static void main(String[] args) throws OWLOntologyCreationException{
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLOntology ontology = manager.loadOntology(IRI.create("http://protege.stanford.edu/plugins/owl/owl-library/koala.owl"));
		String NS = "http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#";
		OWLReasoner reasoner = PelletReasonerFactory.getInstance().createNonBufferingReasoner(ontology);
		StructureBasedRootClassFinder f = new StructureBasedRootClassFinder(reasoner);
		System.out.println(f.getRootUnsatisfiableClasses());
		System.out.println(f.getDerivedUnsatisfiableClasses());
		manager.removeAxiom(ontology, 
				factory.getOWLDataPropertyDomainAxiom(
						factory.getOWLDataProperty(IRI.create(NS + "isHardWorking")),
						factory.getOWLClass(IRI.create(NS + "Person"))));
		f.refresh();
		System.out.println(f.getRootUnsatisfiableClasses());
		System.out.println(f.getDerivedUnsatisfiableClasses());
	}

	

}
