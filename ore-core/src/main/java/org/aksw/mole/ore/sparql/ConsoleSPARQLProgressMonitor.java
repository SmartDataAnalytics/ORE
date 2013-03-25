package org.aksw.mole.ore.sparql;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

public class ConsoleSPARQLProgressMonitor implements SPARQLProgressMonitor{
	
	private long size;
	

	@Override
	public void setStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSize(long size) {
		this.size = size;
		
	}

	@Override
	public void setProgress(long progress) {
		System.out.println(progress + "/" + size);
		
	}

	@Override
	public void setMessage(String message) {
		System.out.println(message);
		
	}

	@Override
	public void setIndeterminate(boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFinished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void inconsistencyFound(Set<OWLAxiom> explanation) {
		System.out.println(explanation);
		
	}

}
