package org.aksw.ore;

import org.aksw.ore.event.OREEvent.KnowledgebaseChangedEvent;
import org.aksw.ore.event.OREEvent.PostViewChangeEvent;
import org.aksw.ore.event.OREEventBus;
import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.model.OWLOntologyKnowledgebase;
import org.aksw.ore.view.DebuggingView;
import org.aksw.ore.view.LearningView;
import org.aksw.ore.view.OREViewType;

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

        initViewChangeListener();
        initViewProviders();

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
    	Knowledgebase knowledgebase = event.getKb();
    	if(knowledgebase != null){
    		if(knowledgebase instanceof OWLOntologyKnowledgebase){
    			addView(OREViewType.ENRICHMENT_FILE.getViewName(), OREViewType.ENRICHMENT_FILE.getViewClass());
    			if(((OWLOntologyKnowledgebase) knowledgebase).isConsistent()) {
    				addView(OREViewType.DEBUGGING_INCOHERENCY.getViewName(), OREViewType.DEBUGGING_INCOHERENCY.getViewClass());
    			} else {
    				addView(OREViewType.DEBUGGING_INCONSISTENCY.getViewName(), OREViewType.DEBUGGING_INCONSISTENCY.getViewClass());
    			}
    		} else {
    			addView(OREViewType.ENRICHMENT_SPARQL.getViewName(), OREViewType.ENRICHMENT_SPARQL.getViewClass());
    			addView(OREViewType.DEBUGGING_SPARQL.getViewName(), OREViewType.DEBUGGING_SPARQL.getViewClass());
    		}
    	}
    }
    

    private void initViewProviders() {
        // A dedicated view provider is added for each separate view type
        for (final OREViewType viewType : OREViewType.values()) {
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

        setErrorProvider(new ViewProvider() {
            @Override
            public String getViewName(final String viewAndParameters) {
                return ERROR_VIEW.getViewName();
            }

            @Override
            public View getView(final String viewName) {
                return errorViewProvider.getView(ERROR_VIEW.getViewName());
            }
        });
    }
}