package org.aksw.ore;

import java.util.List;

import org.aksw.ore.event.OREEvent.KnowledgebaseChangedEvent;
import org.aksw.ore.event.OREEvent.PostViewChangeEvent;
import org.aksw.ore.event.OREEventBus;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.view.DebuggingView;
import org.aksw.ore.view.KnowledgebaseView;
import org.aksw.ore.view.LearningView;
import org.aksw.ore.view.OREViewType;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class ORENavigator extends Navigator {

    private static final OREViewType ERROR_VIEW = OREViewType.KNOWLEDGEBASE;
    private ViewProvider errorViewProvider;

    public ORENavigator(final ComponentContainer container) {
        super(UI.getCurrent(), container);

        initViewProviders();
        initViewChangeListener();
        
        OREEventBus.register(this);
    }

    private void initViewChangeListener() {
        addViewChangeListener(new ViewChangeListener() {

            @Override
            public boolean beforeViewChange(final ViewChangeEvent event) {
            	if(event.getNewView() instanceof DebuggingView && !ORESession.getKnowledgebaseManager().getKnowledgebase().canDebug()){
            		return false;
            	} else if(event.getNewView() instanceof LearningView && !ORESession.getKnowledgebaseManager().getKnowledgebase().canLearn()){
            		return false;
            	}
                return true;
            }

            @Override
            public void afterViewChange(final ViewChangeEvent event) {
            	OREViewType view = OREViewType.getByViewName(event.getViewName());
            	
                // Appropriate events get fired after the view is changed.
                OREEventBus.post(new PostViewChangeEvent(view));
            }
        });
    }
    
    @Subscribe
    public void updateAvailableViews(KnowledgebaseChangedEvent event) {
    	Knowledgebase kb = event.getKb();
    	
    	List<OREViewType> availableViews = Lists.newArrayList();
    	
    	if(kb instanceof OWLOntologyKnowledgebase){
    		availableViews.add(OREViewType.ENRICHMENT_FILE);
			if(((OWLOntologyKnowledgebase) kb).isConsistent()) {
				availableViews.add(OREViewType.DEBUGGING_INCOHERENCY);
			} else {
				availableViews.add(OREViewType.DEBUGGING_INCONSISTENCY);
			}
			availableViews.add(OREViewType.NAMING);
			availableViews.add(OREViewType.CONSTRAINT);
		} else {
			availableViews.add(OREViewType.ENRICHMENT_SPARQL);
			availableViews.add(OREViewType.DEBUGGING_SPARQL);
		}
    	
    	 // A dedicated view provider is added for each separate view type
    	for (final OREViewType viewType : availableViews) {
            ViewProvider viewProvider = new ClassBasedViewProvider(
                    viewType.getViewName(), viewType.getViewClass()) {
            	
            	

                // This field caches an already initialized view instance if the
                // view should be cached (stateful views).
                private View cachedInstance;

                @Override
                public View getView(final String viewName) {
                    View result = null;
                    if (viewType.getViewName().equals(viewName)) {
                        if (viewType.isStateful()) {
                            // Stateful views get lazily instantiated
                            if (cachedInstance == null) {
                                cachedInstance = super.getView(viewType.getViewName());
                            }
                            result = cachedInstance;
                        } else {
                            // Non-stateful views get instantiated every time
                            // they're navigated to
                            result = super.getView(viewType.getViewName());
                        }
                    }
                    return result;
                }
            };

            if (viewType == ERROR_VIEW) {
                errorViewProvider = viewProvider;
            }

            addProvider(viewProvider);
        }
    }
    
    private void initViewProviders() {
    	KnowledgebaseView view = new KnowledgebaseView();
		addView(OREViewType.KNOWLEDGEBASE.getViewName(), view);
		
		setErrorView(view);
    	
//        setErrorProvider(new ViewProvider() {
//            @Override
//            public String getViewName(final String viewAndParameters) {
//                return ERROR_VIEW.getViewName();
//            }
//
//            @Override
//            public View getView(final String viewName) {
//                return errorViewProvider.getView(ERROR_VIEW.getViewName());
//            }
//        });
    }
}