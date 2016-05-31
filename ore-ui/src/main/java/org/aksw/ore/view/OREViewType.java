package org.aksw.ore.view;

import com.vaadin.navigator.View;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;

public enum OREViewType {
    KNOWLEDGEBASE("knowledge base", KnowledgebaseView.class, FontAwesome.DATABASE, true),
    ENRICHMENT_SPARQL("enrichment", EnrichmentView.class, FontAwesome.ARCHIVE, true), 
    ENRICHMENT_FILE("enrichment", LearningView.class, FontAwesome.ARCHIVE, true), 
    DEBUGGING_INCOHERENCY("logical debugging", DebuggingView.class, FontAwesome.BUG, true), 
    DEBUGGING_INCONSISTENCY("logical debugging", InconsistencyDebuggingView.class, FontAwesome.BUG, true), 
    DEBUGGING_SPARQL("logical debugging", SPARQLDebuggingView.class, FontAwesome.BUG, true), 
    NAMING("naming issue detection", NamingView.class, FontAwesome.PENCIL, true), 
    CONSTRAINT("constraint validation", ConstraintValidationView.class, FontAwesome.EYE, true);

    private final String viewName;
    private final Class<? extends View> viewClass;
    private final Resource icon;
    private final boolean stateful;

    OREViewType(final String viewName,
                final Class<? extends View> viewClass, final Resource icon,
                final boolean stateful) {
        this.viewName = viewName;
        this.viewClass = viewClass;
        this.icon = icon;
        this.stateful = stateful;
    }

    public boolean isStateful() {
        return stateful;
    }

    public String getViewName() {
        return viewName;
    }

    public Class<? extends View> getViewClass() {
        return viewClass;
    }

    public Resource getIcon() {
        return icon;
    }

    public static OREViewType getByViewName(final String viewName) {
        OREViewType result = null;
        for (OREViewType viewType : values()) {
            if (viewType.getViewName().equals(viewName)) {
                result = viewType;
                break;
            }
        }
        return result;
    }

}