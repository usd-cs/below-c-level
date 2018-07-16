package edu.sandiego.bcl;

@SuppressWarnings("serial")

/**
 * Class representing an exception in parsing an X86 instruction.
 */
public class x86RuntimeException extends Exception {

	public x86RuntimeException(String message) {
		super(message);
	}

	@Override
	public String toString() { 
		return "x86RuntimeException: " + super.getMessage();
	}

	@Override
	public String getMessage() { 
		return super.getMessage();
	}
}

