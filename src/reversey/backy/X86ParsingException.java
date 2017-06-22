package reversey.backy;

@SuppressWarnings("serial")

/**
 * Class representing an exception in parsing an X86 instruction.
 */
public class X86ParsingException extends Exception {
	private int startIndex;
	private int endIndex;

	public X86ParsingException(String message, int start, int end) {
		super(message);
		this.startIndex = start;
		this.endIndex = end;
	}

	/*
	public X86ParsingException(String message, Throwable cause, int start, int end) {
		super(message, cause);
		this.startIndex = start;
		this.endIndex = end;
	}
	*/

	@Override
	public String toString() { 
		return "X86ParsingException: " + super.getMessage() + " (start = " +
				this.startIndex + ", end = " + this.endIndex + ")";
	}

	@Override
	public String getMessage() { 
		return super.getMessage();
	}

	public int getStartIndex() { return this.startIndex; }
	public int getEndIndex() { return this.endIndex; }
}

