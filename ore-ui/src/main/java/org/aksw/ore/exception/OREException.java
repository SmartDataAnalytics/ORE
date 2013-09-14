package org.aksw.ore.exception;

public class OREException extends Exception {

	private static final long serialVersionUID = 4412336192214777558L;

	public OREException() {
	}

	public OREException(String message, Throwable e) {
		super(message, e);
	}

	public OREException(String message) {
		super(message);
	}

	public OREException(Throwable e) {
		super(e);
	}

	public OREException(Exception e) {
		super(e);
	}

}
