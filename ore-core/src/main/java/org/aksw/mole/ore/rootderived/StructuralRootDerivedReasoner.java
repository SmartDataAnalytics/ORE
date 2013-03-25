package org.aksw.mole.ore.rootderived;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class StructuralRootDerivedReasoner
{
  private OWLOntologyManager man;
  private OWLReasoner reasoner;
  private OWLReasonerFactory reasonerFactory;
  private OWLOntology mergedOntology;
  private Map<OWLClass, Set<OWLClass>> child2Parent;
  private Map<OWLClass, Set<OWLClass>> parent2Child;
  private Set<OWLClass> roots;
  private boolean dirty;

  public StructuralRootDerivedReasoner(OWLOntologyManager man, OWLReasoner reasoner, OWLReasonerFactory reasonerFactory)
  {
    this.man = man;
    this.reasonerFactory = reasonerFactory;
    this.reasoner = reasoner;
    this.child2Parent = new HashMap<OWLClass, Set<OWLClass>>();
    this.parent2Child = new HashMap<OWLClass, Set<OWLClass>>();
    this.roots = new HashSet<OWLClass>();
    getMergedOntology();
    this.dirty = true;
  }

  public OWLOntology getMergedOntology()
  {
    try {
      if (this.mergedOntology == null) {
        this.mergedOntology = this.man.createOntology(IRI.create("owlapi:ontology:merge"), this.reasoner.getRootOntology().getImportsClosure(), true);
      }
      return this.mergedOntology;
    }
    catch (OWLOntologyCreationException e) {
    }
    catch (OWLOntologyChangeException e) {
    }
    return null;
  }

  private Set<OWLClass> get(OWLClass c, Map<OWLClass, Set<OWLClass>> map) {
    Set<OWLClass> set = map.get(c);
    if (set == null) {
      set = new HashSet<OWLClass>();
      map.put(c, set);
    }
    return set;
  }

  public Set<OWLClass> getDependentChildClasses(OWLClass cls) {
    return get(cls, this.parent2Child);
  }

  public Set<OWLClass> getDependentDescendantClasses(OWLClass cls)
  {
    Set<OWLClass> result = new HashSet<OWLClass>();
    getDescendants(cls, result);
    return result;
  }

  private void getDescendants(OWLClass cls, Set<OWLClass> result) {
    if (result.contains(cls)) {
      return;
    }
    for (OWLClass child : getDependentChildClasses(cls)) {
      result.add(child);
      getDescendants(child, result);
    }
  }

  public Set<OWLClass> getRootUnsatisfiableClasses()
  {
    if (this.dirty) {
      computeRootDerivedClasses();
    }
    return Collections.unmodifiableSet(this.roots);
  }

  private void computeRootDerivedClasses() {
      computeCandidateRoots();
      this.roots.remove(this.man.getOWLDataFactory().getOWLNothing());
//      for (OWLClass child : this.child2Parent.keySet()) {
//    	  Set<OWLClass> parents = child2Parent.get(child);
//    	  if(parents == null){
//    		  parents = new HashSet<OWLClass>();
//    		  child2Parent.put(child, parents);
//    	  }
//    	  for(OWLClass parent : parents){
//    		  Set<OWLClass> children = parent2Child.get(parent);
//    		  if(children == null){
//    			  children = new HashSet<OWLClass>();
//    			  parent2Child.put(parent, children);
//    		  }
//    		  children.add(child);
//    	  }
//      }
      OWLClass child;
      Set<OWLClass> parents;
      Set<OWLClass> children;
      for (Entry<OWLClass, Set<OWLClass>> entry : this.child2Parent.entrySet()) {
    	  child = entry.getKey();
    	  parents = entry.getValue();
    	  if(parents == null){
    		  parents = new HashSet<OWLClass>();
    		  child2Parent.put(child, parents);
    	  }
    	  for(OWLClass parent : parents){
    		  children = parent2Child.get(parent);
    		  if(children == null){
    			  children = new HashSet<OWLClass>();
    			  parent2Child.put(parent, children);
    		  }
    		  children.add(child);
    	  }
      }

      for (OWLClass cls :  this.child2Parent.keySet()) {System.out.println("### " + cls);
        Set<List<OWLClass>> paths = new HashSet<List<OWLClass>>();
        getPaths(cls, new ArrayList<OWLClass>(), new HashSet<OWLClass>(), paths);
        for (List<OWLClass> path : paths)
          if ((path.size() > 1) && ((path.get(0)).equals(path.get(path.size() - 1)))) {
            System.out.println(path);
            this.roots.add(cls);
          } 
        }
  }

  private void getPaths(OWLClass cls, List<OWLClass> curPath, Set<OWLClass> curPathSet, Set<List<OWLClass>> paths) {
    if (curPathSet.contains(cls)) {
      curPathSet.remove(cls);
      curPath.add(cls);
      paths.add(new ArrayList<OWLClass>(curPath));
      curPath.remove(curPath.size() - 1);
      return;
    }
    curPathSet.add(cls);
    curPath.add(cls);
    System.out.println(cls);
    for (OWLClass dep : this.child2Parent.get(cls)) {System.out.println(dep);System.out.println(curPath);
      getPaths(dep, curPath, curPathSet, paths);System.out.println(curPath);
      paths.add(new ArrayList<OWLClass>(curPath));
      if(curPath.size() >= 1){
    	  curPath.remove(curPath.size() - 1);
          
      }
      curPathSet.remove(dep);
    }
  }

  private void pruneRoots() {
    Set<OWLClass> rootUnsatClses;
    Set<OWLClass> potentialRoots;
    OWLReasoner checkingReasoner;
    try {
      rootUnsatClses = new HashSet<OWLClass>(this.roots);
      List<OWLOntologyChange> appliedChanges = new ArrayList<OWLOntologyChange>();

      potentialRoots = new HashSet<OWLClass>();
			for (OWLDisjointClassesAxiom ax : this.mergedOntology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
				for (OWLClass cls : rootUnsatClses)
					if (ax.getSignature().contains(cls)) {
						RemoveAxiom chg = new RemoveAxiom(this.mergedOntology, ax);
						this.man.applyChange(chg);
						appliedChanges.add(chg);
						for (OWLEntity ent : ax.getSignature())
							if (ent.isOWLClass())
								potentialRoots.add(ent.asOWLClass());
					}
			}

			for (OWLClass c : rootUnsatClses) {
				this.man.addAxiom(this.mergedOntology, this.man.getOWLDataFactory().getOWLDeclarationAxiom(c));
			}

      checkingReasoner = this.reasonerFactory.createNonBufferingReasoner(mergedOntology);
      for (OWLClass root : rootUnsatClses) {
        if ((!potentialRoots.contains(root)) && (checkingReasoner.isSatisfiable(root)))
          rootUnsatClses.remove(root);
      }
    }
    catch (OWLOntologyChangeException e)
    {
    }
  }

  private void computeCandidateRoots()
  {
    Set<OWLClass> unsatisfiableClasses = this.reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
    OWLOntology ont = getMergedOntology();
    SuperClassChecker checker = new SuperClassChecker();
    for (OWLClass cls : unsatisfiableClasses) {
      checker.reset();
      for (OWLClassExpression sup : cls.getSuperClasses(this.reasoner.getRootOntology())) {
        sup.accept(checker);
      }
      for (OWLClassExpression sup : cls.getEquivalentClasses(this.reasoner.getRootOntology())) {
        sup.accept(checker);
      }
      Set<OWLClass> dependencies = checker.getDependencies();
      this.child2Parent.put(cls, dependencies);
      if (dependencies.isEmpty())
      {
        this.roots.add(cls);
      }
    }
  }

  private class SuperClassChecker
    implements OWLClassExpressionVisitor
  {
    private Set<OWLClass> dependsOn;
    private int modalDepth;
    private Map<Integer, Set<OWLObjectAllValuesFrom>> modalDepth2UniversalRestrictionPropertyMap;
    private Map<Integer, Set<OWLObjectPropertyExpression>> modalDepth2ExistsRestrictionPropertyMap;

    public SuperClassChecker()
    {
      this.modalDepth2UniversalRestrictionPropertyMap = new HashMap<Integer, Set<OWLObjectAllValuesFrom>>();
      this.modalDepth2ExistsRestrictionPropertyMap = new HashMap<Integer, Set<OWLObjectPropertyExpression>>();
      this.dependsOn = new HashSet<OWLClass>();
      this.modalDepth = 0;
    }

    public void addUniversalRestrictionProperty(OWLObjectAllValuesFrom r)
    {
      Set<OWLObjectAllValuesFrom> props = this.modalDepth2UniversalRestrictionPropertyMap.get(Integer.valueOf(this.modalDepth));
      if (props == null) {
        props = new HashSet<OWLObjectAllValuesFrom>();
        this.modalDepth2UniversalRestrictionPropertyMap.put(Integer.valueOf(this.modalDepth), props);
      }
      props.add(r);
    }

    public void addExistsRestrictionProperty(OWLObjectPropertyExpression prop)
    {
      Set<OWLObjectPropertyExpression> props = this.modalDepth2ExistsRestrictionPropertyMap.get(Integer.valueOf(this.modalDepth));
      if (props == null) {
        props = new HashSet<OWLObjectPropertyExpression>();
        this.modalDepth2ExistsRestrictionPropertyMap.put(Integer.valueOf(this.modalDepth), props);
      }
      props.add(prop);
    }

    public Set<OWLClass> getDependencies()
    { Set<OWLObjectPropertyExpression> successors;
      for (Iterator<Integer> i$ = this.modalDepth2UniversalRestrictionPropertyMap.keySet().iterator(); i$.hasNext(); ) { int depth = i$.next().intValue();
        successors = this.modalDepth2ExistsRestrictionPropertyMap.get(Integer.valueOf(depth));
        if (successors == null) {
          continue;
        }
        for (OWLObjectAllValuesFrom r : this.modalDepth2UniversalRestrictionPropertyMap.get(Integer.valueOf(depth)))
          if ((successors.contains(r.getProperty())) && 
            (!((OWLClassExpression)r.getFiller()).isAnonymous()))
            this.dependsOn.add(((OWLClassExpression)r.getFiller()).asOWLClass()); }

     
      return Collections.unmodifiableSet(this.dependsOn);
    }

    public void reset()
    {
      this.dependsOn.clear();
      this.modalDepth2ExistsRestrictionPropertyMap.clear();
      this.modalDepth2UniversalRestrictionPropertyMap.clear();
    }

    public void visit(OWLClass desc)
    {
      if (!StructuralRootDerivedReasoner.this.reasoner.isSatisfiable(desc))
	  this.dependsOn.add(desc);
    }

    public void visit(OWLDataAllValuesFrom desc)
    {
    }

    public void visit(OWLDataExactCardinality desc)
    {
    }

    public void visit(OWLDataMaxCardinality desc)
    {
    }

    public void visit(OWLDataMinCardinality desc)
    {
    }

    public void visit(OWLDataSomeValuesFrom desc)
    {
    }

    public void visit(OWLDataHasValue desc)
    {
    }

    public void visit(OWLObjectAllValuesFrom desc)
    {
    	if (((OWLClassExpression)desc.getFiller()).isAnonymous()) {
            this.modalDepth += 1;
            ((OWLClassExpression)desc.getFiller()).accept(this);
            this.modalDepth -= 1;
          }
          else if (!StructuralRootDerivedReasoner.this.reasoner.isSatisfiable((OWLClassExpression)desc.getFiller())) {
            addUniversalRestrictionProperty(desc);
            this.dependsOn.add(((OWLClassExpression)desc.getFiller()).asOWLClass());
          }
    }

    public void visit(OWLObjectComplementOf desc)
    {
    }

    public void visit(OWLObjectExactCardinality desc)
    {
    	if (((OWLClassExpression)desc.getFiller()).isAnonymous()) {
            this.modalDepth += 1;
            ((OWLClassExpression)desc.getFiller()).accept(this);
            this.modalDepth -= 1;
          }
          else if ((!StructuralRootDerivedReasoner.this.reasoner.isSatisfiable((OWLClassExpression)desc.getFiller())) && 
            (!((OWLClassExpression)desc.getFiller()).isAnonymous())) {
            this.dependsOn.add(((OWLClassExpression)desc.getFiller()).asOWLClass());
          }

          addExistsRestrictionProperty((OWLObjectPropertyExpression)desc.getProperty());
    }

    public void visit(OWLObjectIntersectionOf desc)
    {
      for (OWLClassExpression op : desc.getOperands()) {
	  if (op.isAnonymous()) {
	    op.accept(this);
	  }
	  else if (!StructuralRootDerivedReasoner.this.reasoner.isSatisfiable(op)) {
	    this.dependsOn.add(op.asOWLClass());
	  }
	}
    }

    public void visit(OWLObjectMaxCardinality desc)
    {
    }

    public void visit(OWLObjectMinCardinality desc)
    {
      if (((OWLClassExpression)desc.getFiller()).isAnonymous()) {
	  this.modalDepth += 1;
	  ((OWLClassExpression)desc.getFiller()).accept(this);
	  this.modalDepth -= 1;
	}
	else if (!StructuralRootDerivedReasoner.this.reasoner.isSatisfiable((OWLClassExpression)desc.getFiller())) {
	  this.dependsOn.add(((OWLClassExpression)desc.getFiller()).asOWLClass());
	}

	addExistsRestrictionProperty((OWLObjectPropertyExpression)desc.getProperty());
    }

    public void visit(OWLObjectOneOf desc)
    {
    }

    public void visit(OWLObjectHasSelf desc)
    {
      addExistsRestrictionProperty((OWLObjectPropertyExpression)desc.getProperty());
    }

    public void visit(OWLObjectSomeValuesFrom desc)
    {
      if (((OWLClassExpression)desc.getFiller()).isAnonymous()) {
	  this.modalDepth += 1;
	  ((OWLClassExpression)desc.getFiller()).accept(this);
	  this.modalDepth -= 1;
	}
	else if (!StructuralRootDerivedReasoner.this.reasoner.isSatisfiable((OWLClassExpression)desc.getFiller())) {
	  this.dependsOn.add(((OWLClassExpression)desc.getFiller()).asOWLClass());
	}

	addExistsRestrictionProperty((OWLObjectPropertyExpression)desc.getProperty());
    }

    public void visit(OWLObjectUnionOf desc)
    {
      for (OWLClassExpression op : desc.getOperands()) {
	  if (StructuralRootDerivedReasoner.this.reasoner.isSatisfiable(op)) {
	    return;
	  }
	}
	for (OWLClassExpression op : desc.getOperands()) {
	  if (op.isAnonymous()) {
	    op.accept(this);
	  }
	  else
	    this.dependsOn.add(op.asOWLClass());
	}
    }

    public void visit(OWLObjectHasValue desc)
    {
      addExistsRestrictionProperty((OWLObjectPropertyExpression)desc.getProperty());
    }
  }
}