package org.aksw.mole.ore.cache;

import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;


public class DLSyntaxRenderingCache implements OWLOntologyChangeListener, OWLOntologyLoaderListener{
	
	private Map<OWLObject, String> cache = new LRUMap<OWLObject, String>(50, 1, 50);
	private DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();

    public String getRendering(OWLObject object) {
        String s = null;
        if (s == null){
            s = cache.get(object);
            if (s == null){
                s = renderer.render(object);
                cache.put(object, s);
            }
        }
        return s;
    }
    
    public void clear() {
        cache.clear();
    }

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
