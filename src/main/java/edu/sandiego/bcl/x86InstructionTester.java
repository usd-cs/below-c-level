package edu.sandiego.bcl;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple class to test x86 instruction parsing and evaluation.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class x86InstructionTester {
	public static void main(String[] args) {
		ArrayList<x86ProgramLine> instructions = new ArrayList<x86ProgramLine>();
                X86Parser oldPerry = new X86Parser();
		try {
			instructions.add(oldPerry.parseLine("movq $9, %rax"));
			instructions.add(oldPerry.parseLine("movq $4, %rbx"));
			instructions.add(oldPerry.parseLine("addq %rax, %rbx"));
			instructions.add(oldPerry.parseLine("pushq %rbx"));
			instructions.add(oldPerry.parseLine("popq %rcx"));
			instructions.add(oldPerry.parseLine("leaq -12(%rsp), %rdx"));
			instructions.add(oldPerry.parseLine("movl $73, (%rdx)"));
			instructions.add(oldPerry.parseLine("incl %esi"));
			instructions.add(oldPerry.parseLine("decl %edi"));

			// test that smaller register only affect part of the whole register
			instructions.add(oldPerry.parseLine("movl $0, %edx"));
			instructions.add(oldPerry.parseLine("movw $-1, %dx"));
			instructions.add(oldPerry.parseLine("movb $2, %dl"));
			instructions.add(oldPerry.parseLine("movb $3, %dh"));

			// tests for condition codes
			instructions.add(oldPerry.parseLine("movl $0, %ebp"));
			instructions.add(oldPerry.parseLine("movl $1, %ebp"));
			instructions.add(oldPerry.parseLine("sall $31, %ebp"));
			instructions.add(oldPerry.parseLine("decl %ebp"));
			instructions.add(oldPerry.parseLine("addl $0, %ebp"));
			instructions.add(oldPerry.parseLine("incl %ebp"));
			instructions.add(oldPerry.parseLine("negl %ebp"));
			instructions.add(oldPerry.parseLine("andl $0, %ebp"));
			instructions.add(oldPerry.parseLine("notl %ebp"));
			instructions.add(oldPerry.parseLine("shrl $1, %ebp"));

			// more LONG registers
			instructions.add(oldPerry.parseLine("movl $1, %r8d"));
			instructions.add(oldPerry.parseLine("sall $4, %r8d"));
			instructions.add(oldPerry.parseLine("sarl $3, %r8d"));

			// tests for cmp, test, and set instructions
			instructions.add(oldPerry.parseLine("movl $-5, %eax"));
			instructions.add(oldPerry.parseLine("cmpl $-5, %eax"));
			instructions.add(oldPerry.parseLine("setge %bl"));

			// TODO: more tests for cmp, test, and set instructions
		} catch (X86ParsingException e) {
			e.printStackTrace();
		}
		
		MachineState state = new MachineState();
		System.out.println(state);
		for (x86ProgramLine inst : instructions) {
			System.out.println(inst);
                    try {
                        state = inst.eval(state);
                    } catch (x86RuntimeException ex) {
                        Logger.getLogger(x86InstructionTester.class.getName()).log(Level.SEVERE, null, ex);
                    }
			System.out.println(state);
		}

		try {
			oldPerry.parseLine("movl $-5, %eax");
			oldPerry.parseLine("movl 0(%rax, %ecx, 13), %eax");
		} catch (X86ParsingException e) {
			e.printStackTrace();
		}

	}
}
