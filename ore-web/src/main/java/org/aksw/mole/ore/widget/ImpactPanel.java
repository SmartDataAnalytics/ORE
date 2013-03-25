package org.aksw.mole.ore.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.aksw.mole.ore.ImpactManager;
import org.aksw.mole.ore.ImpactManager.ImpactManagerListener;
import org.aksw.mole.ore.RepairManagerListener;
import org.aksw.mole.ore.UserSession;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.vaadin.overlay.TextOverlay;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

public class ImpactPanel extends VerticalLayout implements RepairManagerListener, ImpactManagerListener{
	
	
	private static final Logger logger = Logger.getLogger(ImpactPanel.class.getName());
	
	private ImpactTable impactTable;
	private TextOverlay loadingIndicator;
	
	private Refresher refresher = new Refresher();
	private Collection<OWLOntologyChange> impact;
	
	public ImpactPanel() {
		setSizeFull();
		impactTable = new ImpactTable();
		addComponent(impactTable);
		
		loadingIndicator = new TextOverlay(impactTable, "<i>Computing impact...</i>");
		loadingIndicator.setContentMode(TextOverlay.CONTENT_RAW);
		loadingIndicator.setComponentAnchor(Alignment.MIDDLE_CENTER);
		loadingIndicator.setOverlayAnchor(Alignment.MIDDLE_CENTER);
		addComponent(loadingIndicator);
		loadingIndicator.setVisible(false);
		
//		refresher.addListener(new RefreshListener() {
//
//			@Override
//			public void refresh(Refresher source) {
//				if (impact != null) {
//					showLoadingImpact(false);
//					impactTable.setImpact(impact);
//					source.setEnabled(false);
//				}
//
//			}
//		});
		refresher.setRefreshInterval(500);
		refresher.setEnabled(false);
		
		setExpandRatio(impactTable, 1f);
	}
	
	@Override
	public void attach() {
		super.attach();
		getWindow().addComponent(refresher);
		UserSession.getImpactManager().addListener(this);
		UserSession.getRepairManager().addListener(this);
		
	}
	
	@Override
	public void detach() {
		super.detach();
		getWindow().removeComponent(refresher);
		UserSession.getImpactManager().removeListener(this);
		UserSession.getRepairManager().removeListener(this);
	}
	
	public void showLoadingImpact(boolean loading){
		loadingIndicator.setVisible(loading);
	}

	@Override
	public void repairPlanChanged() {
		computeImpact();
	}

	@Override
	public void repairPlanExecuted() {
		// TODO Auto-generated method stub
		
	}
	
	private void computeImpact(){
		if (logger.isDebugEnabled()) {
			logger.debug("Recomputing impact...");
		}
//		impactTable.clear();
//		showLoadingImpact(true);
//		impact = null;
//		refresher.setEnabled(true);
		final ImpactManager impMan = UserSession.getImpactManager();
		final List<OWLOntologyChange> repairPlan = new ArrayList<OWLOntologyChange>(UserSession.getRepairManager().getRepairPlan());
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				impact = impMan.getImpact(repairPlan);
			}
		});
		t.start();
//		impact = impMan.getImpact(repairPlan);
	}
	
	public void reset(){
		impact = null;
		impactTable.clear();
		showLoadingImpact(false);
		refresher.setEnabled(false);
	}

	@Override
	public void impactComputationStarted() {
		refresher.setEnabled(true);
		showLoadingImpact(true);
		impactTable.clear();
	}

	@Override
	public void impactComputationFinished(Set<OWLOntologyChange> impact) {
		showLoadingImpact(false);
		impactTable.setImpact(impact);
		refresher.setEnabled(false);
	}

	@Override
	public void impactComputationCanceled() {
		showLoadingImpact(false);
		refresher.setEnabled(false);
	}
	

}
