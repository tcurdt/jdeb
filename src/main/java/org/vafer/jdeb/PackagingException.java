package org.vafer.jdeb;

public final class PackagingException extends Exception {

	private static final long serialVersionUID = 1L;

	public PackagingException() {
		super();
	}

	public PackagingException(String message, Throwable cause) {
		super(message, cause);
	}

	public PackagingException(String message) {
		super(message);
	}

	public PackagingException(Throwable cause) {
		super(cause);
	}

}
