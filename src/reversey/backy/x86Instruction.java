/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.HashMap;

/**
 *
 * @author Caitlin
 */

// TODO: Add some Javadoc comments to our classes and methods!
public class x86Instruction {
    protected operand destination;
    protected String instructionType;
    // Perform the operation specific to the instruction type

    /**
     *
     * @return
     */
    public int eval() { return 0; }

    // Create a new x86Instruction if input is valid otherwise return null
    //TODO: Pass in memory structure whatever that means... 
    public static x86Instruction create(String userInput, HashMap <String, Integer> registers) { 
		String[] tokens = userInput.split("\\s+");
		String instrName = tokens[0]; // should be instruction name, e.g. "addl"
		System.out.println("instruction name: " + instrName);

		// TODO: a regular expression is probably a better approach here
		switch (instrName) {
			case "movb":
			case "movw":
			case "movl":
			case "movq":
			case "addb":
			case "addw":
			case "addl":
			case "addq":
			// TODO: cases for more types of binary instructions
				// found binary instruction
				// step 1: make sure it has exactly two operands (TODO)
				// step 2: make sure operands are valid (TODO)
				// step 3: get operands to pass into constructor (TODO)
				System.out.println("yay got a valid binary inst!");
				return new x86BinaryInstruction(instrName, null, null);
			case "pushq":
			case "popq":
			case "incb":
			case "incw":
			case "incl":
			case "incq":
			// TODO: cases for more types of unary instructions
				// step 1: make sure it has exactly one operand (TODO)
				// step 2: make sure operand is valid (TODO)
				// step 3: get operand to pass into constructor (TODO)
				System.out.println("yay got a valid unary inst!");
				return new x86UnaryInstruction(instrName, null);
			default:
				return null;
		}
	}
    public String toString() {
        return null; // TODO: prettify
    }
    
}

class x86UnaryInstruction extends x86Instruction{
	public x86UnaryInstruction(String instType, operand op) {
		this.instructionType = instType;
		this.destination = op;
	}

    public int eval() { return 1; }
    
    @Override
    public String toString() {
        return instructionType; // FIXME
    }
}

class x86BinaryInstruction extends x86Instruction{
    private operand source;

	public x86BinaryInstruction(String instType, operand src, operand dest) {
		this.instructionType = instType;
		this.source = src;
		this.destination = dest;
	}

    public int eval() { return 3; }
    
    @Override
    public String toString() {
        return instructionType + "yay"; // FIXME
    }
}
        
class operand{
    HashMap <String, Integer> registers;
     public int getValue() { return 99; }
} 
        
class regOperand{
    String regName;
    public int getValue() { return 162; }
    
}
     
class memoryOperand{
    String memName;
     public int getValue() { return 5298; }
}
        
class constantOperand{
      int constant;
     public int getValue() { return 0; }
}