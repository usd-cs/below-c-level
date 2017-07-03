package reversey.backy;

import java.util.ArrayList;
import java.util.Set;

/**
 * An abstract class representing an x86-64 instruction.
 *
 * @author Dr. Sat
 */
public abstract class x86Instruction {
        
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


/**
 * Simple class to test x86 instruction parsing and evaluation.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86InstructionTester {
	public static void main(String[] args) {
		ArrayList<x86Instruction> instructions = new ArrayList<x86Instruction>();

		try {
			instructions.add(X86Parser.parseInstruction("movq $9, %rax"));
			instructions.add(X86Parser.parseInstruction("movq $4, %rbx"));
			instructions.add(X86Parser.parseInstruction("addq %rax, %rbx"));
			instructions.add(X86Parser.parseInstruction("pushq %rbx"));
			instructions.add(X86Parser.parseInstruction("popq %rcx"));
			instructions.add(X86Parser.parseInstruction("leaq -12(%rsp), %rdx"));
			instructions.add(X86Parser.parseInstruction("movl $73, (%rdx)"));
			instructions.add(X86Parser.parseInstruction("incl %esi"));
			instructions.add(X86Parser.parseInstruction("decl %edi"));

			// test that smaller register only affect part of the whole register
			instructions.add(X86Parser.parseInstruction("movl $0, %edx"));
			instructions.add(X86Parser.parseInstruction("movw $-1, %dx"));
			instructions.add(X86Parser.parseInstruction("movb $2, %dl"));
			instructions.add(X86Parser.parseInstruction("movb $3, %dh"));

			// tests for condition codes
			instructions.add(X86Parser.parseInstruction("movl $0, %ebp"));
			instructions.add(X86Parser.parseInstruction("movl $1, %ebp"));
			instructions.add(X86Parser.parseInstruction("sall $31, %ebp"));
			instructions.add(X86Parser.parseInstruction("decl %ebp"));
			instructions.add(X86Parser.parseInstruction("addl $0, %ebp"));
			instructions.add(X86Parser.parseInstruction("incl %ebp"));
			instructions.add(X86Parser.parseInstruction("negl %ebp"));
			instructions.add(X86Parser.parseInstruction("andl $0, %ebp"));
			instructions.add(X86Parser.parseInstruction("notl %ebp"));
			instructions.add(X86Parser.parseInstruction("shrl $1, %ebp"));

			// more LONG registers
			instructions.add(X86Parser.parseInstruction("movl $1, %r8d"));
			instructions.add(X86Parser.parseInstruction("sall $4, %r8d"));
			instructions.add(X86Parser.parseInstruction("sarl $3, %r8d"));

			// tests for cmp, test, and set instructions
			instructions.add(X86Parser.parseInstruction("movl $-5, %eax"));
			instructions.add(X86Parser.parseInstruction("cmpl $-5, %eax"));
			instructions.add(X86Parser.parseInstruction("setge %bl"));

			// TODO: more tests for cmp, test, and set instructions
		} catch (X86ParsingException e) {
			e.printStackTrace();
		}
		
		MachineState state = new MachineState();
		System.out.println(state);
		for (x86Instruction inst : instructions) {
			System.out.println(inst);
			state = inst.eval(state);
			System.out.println(state);
		}

		try {
			X86Parser.parseInstruction("movl $-5, %eax");
			X86Parser.parseInstruction("movl 0(%rax, %ecx, 13), %eax");
		} catch (X86ParsingException e) {
			e.printStackTrace();
		}

	}
}
