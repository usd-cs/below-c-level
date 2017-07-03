package reversey.backy;

import java.util.Set;

/**
 * An abstract class representing an x86-64 instruction.
 *
 * @author Dr. Sat
 */
public abstract class x86Instruction {
        
	/**
	 * The line number where this instruction is located.
	 */
	protected int lineNum;

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

	/**
	 * Perform the operation specific to the instruction.
	 * 
	 * @param state The state of the machine before evaluation begins.
	 * @return State of machine after evaluating the instruction.
	 */
	public abstract MachineState eval(MachineState state);


	/**
	 * Returns the names of the registers used by this instruction.
	 * This includes any implicit registers used (e.g. %rsp by push and pop)
	 *
	 * @return Set containing names of registers used by this instruction.
	 */
    public abstract Set<String> getUsedRegisters();
	public abstract String toString();
}
