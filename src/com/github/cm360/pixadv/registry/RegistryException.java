package com.github.cm360.pixadv.registry;

public class RegistryException extends Exception {

	private static final long serialVersionUID = 1L;

	public RegistryException() {
	}

	public RegistryException(String message) {
		super(message);
	}

	public RegistryException(Throwable cause) {
		super(cause);
	}

	public RegistryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RegistryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
