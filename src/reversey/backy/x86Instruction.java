package reversey.backy;

import java.util.Set;

/**
 * An abstract class representing an x86-64 instruction.
 *
 * @author Dr. Sat
 */
public abstract class x86Instruction extends x86ProgramLine {

	/**
	 * The operand where the instruction will write its results.
	 */
	protected Operand destination;

	/**
	 * The type of instruction (e.g. add)
	 */
	protected InstructionType type;

	/**
	 * The number of bytes the operation works on.
	 */
	protected OpSize opSize;

	// Getters
	public InstructionType getType() { return this.type; }
	public OpSize getOpSize() { return this.opSize; }

	public abstract String toString();

	public String getInstructionTypeString() {
		String s = this.type.name().toLowerCase();
		if (!this.type.name().startsWith("SET")) {
			switch (this.opSize) {
				case QUAD:
					s += "q";
					break;
				case LONG:
					s += "l";
					break;
				case WORD:
					s += "w";
					break;
				default:
					s += "b";
					break;
			}
		}

		return s;
	}
}
