package org.aksw.mole.ore.cache;

import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

public class ManchesterSyntaxRenderingCache implements OWLOntologyChangeListener, OWLOntologyLoaderListener{

	private OWLObjectRenderer renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

	private Map<OWLObject, String> owlObjectCache = new LRUMap<OWLObject, String>(50, 1, 50);
//	Map<Description, String> descriptionCache = new LRUMap<Description, String>(50, 1, 50);
//	Map<ObjectProperty, String> objectPropertyCache = new LRUMap<ObjectProperty, String>(50, 1, 50);
//	Map<Individual, String> individualCache = new LRUMap<Individual, String>(50, 1, 50);

	public String getRendering(OWLObject object) {
		String s = null;
		s = owlObjectCache.get(object);
		if (s == null) {
			s = renderer.render(object);
			owlObjectCache.put(object, s);
		}
		return s;
	}
	
	public void clear() {
		owlObjectCache.clear();
//		descriptionCache.clear();
//		objectPropertyCache.clear();
//		individualCache.clear();
	}

//	public String getRendering(Description description) {
//		String s = null;
//		s = descriptionCache.get(description);
//		if (s == null) {
//			s = renderer.render(OWLAPIDescriptionConvertVisitor.getOWLClassExpression(description));
//			descriptionCache.put(description, s);
//		}
//		return s;
//	}
//	
//	public String getRendering(ObjectProperty property) {
//		String s = null;
//		s = objectPropertyCache.get(property);
//		if (s == null) {
//			s = renderer.render(OWLAPIConverter.getOWLAPIObjectProperty(property));
//			objectPropertyCache.put(property, s);
//		}
//		return s;
//	}
//	
//	public String getRendering(Individual individual) {
//		String s = null;
//		s = individualCache.get(individual);
//		if (s == null) {
//			s = renderer.render(OWLAPIConverter.getOWLAPIIndividual(individual));
//			individualCache.put(individual, s);
//		}
//		return s;
//	}

	public void dispose() {
		clear();
	}

	@Override
	public void finishedLoadingOntology(LoadingFinishedEvent e) {
		clear();
	}

	@Override
	public void startedLoadingOntology(LoadingStartedEvent e) {
		
	}

	@Override
	public void ontologiesChanged(List<? extends OWLOntologyChange> e)
			throws OWLException {
		clear();
	}
	
	

}
