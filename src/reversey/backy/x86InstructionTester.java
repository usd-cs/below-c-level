package reversey.backy;

import java.util.ArrayList;

/**
 * Simple class to test x86 instruction parsing and evaluation.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class x86InstructionTester {
	public static void main(String[] args) {
		ArrayList<x86ProgramLine> instructions = new ArrayList<x86ProgramLine>();

		try {
			instructions.add(X86Parser.parseLine("movq $9, %rax"));
			instructions.add(X86Parser.parseLine("movq $4, %rbx"));
			instructions.add(X86Parser.parseLine("addq %rax, %rbx"));
			instructions.add(X86Parser.parseLine("pushq %rbx"));
			instructions.add(X86Parser.parseLine("popq %rcx"));
			instructions.add(X86Parser.parseLine("leaq -12(%rsp), %rdx"));
			instructions.add(X86Parser.parseLine("movl $73, (%rdx)"));
			instructions.add(X86Parser.parseLine("incl %esi"));
			instructions.add(X86Parser.parseLine("decl %edi"));

			// test that smaller register only affect part of the whole register
			instructions.add(X86Parser.parseLine("movl $0, %edx"));
			instructions.add(X86Parser.parseLine("movw $-1, %dx"));
			instructions.add(X86Parser.parseLine("movb $2, %dl"));
			instructions.add(X86Parser.parseLine("movb $3, %dh"));

			// tests for condition codes
			instructions.add(X86Parser.parseLine("movl $0, %ebp"));
			instructions.add(X86Parser.parseLine("movl $1, %ebp"));
			instructions.add(X86Parser.parseLine("sall $31, %ebp"));
			instructions.add(X86Parser.parseLine("decl %ebp"));
			instructions.add(X86Parser.parseLine("addl $0, %ebp"));
			instructions.add(X86Parser.parseLine("incl %ebp"));
			instructions.add(X86Parser.parseLine("negl %ebp"));
			instructions.add(X86Parser.parseLine("andl $0, %ebp"));
			instructions.add(X86Parser.parseLine("notl %ebp"));
			instructions.add(X86Parser.parseLine("shrl $1, %ebp"));

			// more LONG registers
			instructions.add(X86Parser.parseLine("movl $1, %r8d"));
			instructions.add(X86Parser.parseLine("sall $4, %r8d"));
			instructions.add(X86Parser.parseLine("sarl $3, %r8d"));

			// tests for cmp, test, and set instructions
			instructions.add(X86Parser.parseLine("movl $-5, %eax"));
			instructions.add(X86Parser.parseLine("cmpl $-5, %eax"));
			instructions.add(X86Parser.parseLine("setge %bl"));

			// TODO: more tests for cmp, test, and set instructions
		} catch (X86ParsingException e) {
			e.printStackTrace();
		}
		
		MachineState state = new MachineState();
		System.out.println(state);
		for (x86ProgramLine inst : instructions) {
			System.out.println(inst);
			state = inst.eval(state);
			System.out.println(state);
		}

		try {
			X86Parser.parseLine("movl $-5, %eax");
			X86Parser.parseLine("movl 0(%rax, %ecx, 13), %eax");
		} catch (X86ParsingException e) {
			e.printStackTrace();
		}

	}
}
