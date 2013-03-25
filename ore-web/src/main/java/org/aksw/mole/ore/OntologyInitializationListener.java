package org.aksw.mole.ore;

public interface OntologyInitializationListener {
	
	void inconsistentOntologyLoaded();
	void incoherentOntologyLoaded();
	void nonContainingIndividualsOntologyLoaded();

}
