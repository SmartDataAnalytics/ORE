/**
 * 
 */
package org.aksw.ore.component;

import org.aksw.ore.ORESession;
import org.aksw.ore.manager.KnowledgebaseManager.KnowledgebaseLoadingListener;
import org.aksw.ore.model.Knowledgebase;

import com.vaadin.ui.UI;

/**
 * @author Lorenz Buehmann
 *
 */
public class KnowledgebaseAnalyzationDialog extends ProgressDialog implements KnowledgebaseLoadingListener{
	
	/**
	 * 
	 */
	public KnowledgebaseAnalyzationDialog() {
		setCaption("Analyzing knowledge base...");
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
//		ORESession.getKnowledgebaseManager().removeListener(this);
		close();
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
	public void message(final String message) {
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				setMessage(message);
			}
		});
	}

	

}
