/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.HashMap;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.util.Pair;

/**
 *
 * @author Dr. Sat
 */

// TODO: Add some Javadoc comments to our classes and methods!
public abstract class x86Instruction {
	protected Operand destination;
	protected String instructionType;

	// Perform the operation specific to the instruction type

	/**
	 *
	 * @return
	 */
	public abstract void eval();

	/**
	 * @return true if a supported binary instruction, false if a supported
	 * unary instruction.
	 */
	// TODO: throw exception on invalid / unsupported instruction
	public static boolean validateInstruction(String instrName) {
		switch (instrName) {
			case "addl":
			case "subl":
			case "movl":
				return true;
			case "incl":
			case "decl":
				return false;
			default:
				System.err.println("invalid or unsupported instruction: " + instrName);
				System.exit(1);
				return false;
		}
	}

	public static String parseRegister(String str) {
		System.out.println("parseRegister: " + str);
		String regRegEx = "^\\%(eax|ebx|ecx|edx|esi|edi|ebp|esp|eip)$";
		if (Pattern.matches(regRegEx, str))
			return str.substring(1);
		else
			return null; // TODO: this should raise illegal register exception
	}

	public static Pair<Operand, Integer> parseOperand(String str) {
		System.out.println("parsing Operand in: " + str);

		Operand op = null;
		int endPoint = 0;

		if (str.charAt(0) == '$') {
			// constant operand
			System.out.println("found a constant op");
			String[] splits = str.split("$,");
			op = new ConstantOperand(Integer.parseInt(splits[0])); // TODO: handle hex constant
		}
		else if (str.charAt(0) == '%') {
			// register operand
			System.out.println("found a register op");
			String[] splits = str.split(",");
			String regName = parseRegister(splits[0]);
			op = new RegOperand(regName, null); // FIXME: this shouldn't be null
		}
		else {
			// memory operand
			System.out.println("found a memory op");

			if (str.indexOf('(') == -1 || str.indexOf(')') == -1) {
				System.err.println("ERROR: missing ( and/or )");
				return null; // TODO: throw exception
			}

			int opEndIndex = str.indexOf(')')+1;
			if (opEndIndex != str.length() && str.charAt(opEndIndex) != ',') {
				System.err.println("ERROR: missing separator between first and second operand");
				return null;
			}

			String opString = str.substring(0, opEndIndex);
			System.out.println("operand string: " + opString);

			if (opString.indexOf('(') == -1) {
				System.err.println("unmatched ) found");
				return null; // TODO: throw exception
			}

			/*
			if (opString.charAt(0) != '(') {
				System.out.println("got an offset for memory op");
				offset = Integer.parseInt(opString.substring(0, opString.indexOf('('))); // TODO: handle hex
				opString = opString.substring(opString.indexOf('('));
			}
			*/

			System.out.println("opString: " + opString);

			String[] components = opString.split("[(),]");
			if (components.length < 2 || components.length > 4) {
				System.err.println("ERROR: invalid number of memory op components");
				return null; // TODO: throw exception
			}

			//for (String s : components)
			//	System.out.println("component: " + s);

			int offset = 0;
			if (!components[0].isEmpty()) {
				System.out.println("got an offset for memory op");
				offset = Integer.parseInt(components[0]); // TODO: handle hex
			}

			String baseReg = parseRegister(components[1]);
			String indexReg = null;
			int scale = 1;
			if (components.length > 2) {
				indexReg = parseRegister(components[2]);
			}
			if (components.length > 3) {
				scale = Integer.parseInt(components[3]);
				if (scale != 1 && scale != 2 && scale != 4 && scale != 8) {
					System.err.println("ERROR: invalid scaling factor: " + scale);
					return null; // TODO: throw exception
				}
			}

			op = new MemoryOperand(baseReg, indexReg, scale, offset, null);
		}

		if (str.charAt(0) == '$' || str.charAt(0) == '%') {
			endPoint = str.indexOf(',');
			if (endPoint == -1) endPoint = str.length();
			else endPoint++;
		}
		else {
			endPoint = str.indexOf(')') + 1;

			// @tricky this should never happen, should throw exception above
			if (endPoint == -1) return null;
			else if (endPoint != str.length()) endPoint++;
		}

		return new Pair<Operand, Integer>(op, endPoint);
	}

	public static x86Instruction parseInstruction(String instr) {
		String[] tokens = instr.split("\\s+");
		String instrName = tokens[0]; // should be instruction name, e.g. "addl"
		System.out.println("instr name: " + instrName);
		String operandsStr = String.join("",tokens).substring(tokens[0].length());
		System.out.println("operand str: " + operandsStr);

		Pair<Operand, Integer> firstOperandAndEndPt = parseOperand(operandsStr);
		System.out.println("First operand was: " + firstOperandAndEndPt.getKey());

		if (validateInstruction(instrName)) {
			if (operandsStr.indexOf(',') == -1) {
				System.err.println("ERROR: Couldn't find separator between first and second operand.");
				return null; // TODO: throw exception
			}

			String secondOpStr = operandsStr.substring(firstOperandAndEndPt.getValue());

			Pair<Operand, Integer> secondOperandAndEndPt = parseOperand(secondOpStr);

			if (secondOperandAndEndPt.getValue() != secondOpStr.length()) {
				System.err.println("extra stuff left over: " + secondOpStr.substring(secondOperandAndEndPt.getValue()));
				return null; // TODO: make this throw an exception
			}

			return new x86BinaryInstruction(instrName.substring(0, instrName.length()-1), 
					firstOperandAndEndPt.getKey(),
					secondOperandAndEndPt.getKey());
		}
		else {
			if (firstOperandAndEndPt.getValue() != operandsStr.length())
				return null; // TODO: make this throw an exception

			return new x86UnaryInstruction(instrName.substring(0, instrName.length()-1), 
					firstOperandAndEndPt.getKey());
		}
	}

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
			case "movq":
			case "addb":
			case "addw":
			case "addq":
				System.err.println("only 32-bit ops supported now");
				return null;

			case "addl":
			case "subl":
			case "movl":
				// TODO: cases for more types of binary instructions
				// found a supported binary instruction
				// Step 1: make sure it has exactly two operands, separated by
				//  a comma (TODO)

				// quick check: splitting by "," results in two tokens
				// FIXME: won't work if we have a memory op that has comma in
				// it.
				String[] tmp = userInput.split(",");
				if (tmp.length != 2) {
					System.err.println("invalid format: expecting exactly 1 ',' in instruction");
					return null;
				}

				// now split first token by whitespace and look only at the
				// second token, which will be the first operand
				String[] tmp2 = tmp[0].trim().split("\\s+");
				if (tmp2.length != 2) {
					System.err.println("unexpected extra token");
					return null;
				}
				else if (tmp2[1].charAt(0) != '%') {
					System.err.println("invalid operand: " + tmp2[1]);
					return null;
				}

				String op1 = tmp2[1].substring(1);
				System.out.println("first operand is " + op1);

				// now split second token by whitespace. It should only have a
				// single token
				String[] tmp3 = tmp[1].trim().split("\\s+");
				if (tmp3.length != 1) {
					System.err.println("unexpected extra tokens in: " + tmp[1]);
					return null;
				}
				else if (tmp3[0].charAt(0) != '%') {
					System.err.println("invalid operand: " + tmp3[0]);
					return null;
				}

				String op2 = tmp3[0].substring(1);
				System.out.println("second operand is " + op2);

				// step 2: make sure operands are valid (TODO)

				// step 3: construct the instruction
				return new x86BinaryInstruction(instrName.substring(0, instrName.length()-1), 
						new RegOperand(op1, registers), 
						new RegOperand(op2, registers));
			case "pushq":
				System.err.println("push not yet supported");
				return null;
			case "popq":
				System.err.println("pop not yet supported");
				return null;
			case "incb":
			case "incw":
			case "incq":
				System.err.println("only 32-bit ops supported now");
				return null;

			case "incl":
			case "decl":
				// step 1: make sure it has exactly one operand (TODO)
				if (tokens[1].charAt(0) != '%') {
					System.err.println("invalid operand: " + tokens[1]);
					return null;
				}

				String op = tokens[1].substring(1);

				// step 2: make sure operand is valid (TODO)

				// step 3: get operand to pass into constructor (TODO)
				return new x86UnaryInstruction(instrName.substring(0, instrName.length()-1), 
						new RegOperand(op, registers));
			default:
				System.out.println("unsupported instruction: " + instrName);
				return null;
		}
	}

	public abstract String toString();

}

class x86UnaryInstruction extends x86Instruction {
	private IntUnaryOperator operation;

	public x86UnaryInstruction(String instType, Operand destOp) {
		this.instructionType = instType;
		this.destination = destOp;

		switch (instType) {
			case "inc":
				this.operation = (dest) -> dest + 1;
				break;
			case "dec":
				this.operation = (dest) -> dest - 1;
				break;
			case "push":
				this.operation = IntUnaryOperator.identity();
				break;
			case "pop":
				this.operation = IntUnaryOperator.identity();
				break;
			default:
				System.err.println("invalid instr type for unary inst: " + instType);
				System.exit(1);
		}
	}

	@Override
		public void eval() { 
			switch (this.instructionType) {
				case "inc":
				case "dec":
					int destVal = operation.applyAsInt(destination.getValue());
					destination.setValue(destVal);
					break;
				case "push":
					System.out.println("push not supported yet");
					break;
				case "pop":
					System.out.println("pop not supported yet");
					break;
				default:
					System.err.println("Something went terribly wrong.");
			}
		}

	@Override
		public String toString() {
			return instructionType + " " + destination.toString();
		}
}

class x86BinaryInstruction extends x86Instruction{
	private Operand source;
	private IntBinaryOperator operation;

	public x86BinaryInstruction(String instType, Operand srcOp, Operand destOp) {
		this.instructionType = instType;
		this.source = srcOp;
		this.destination = destOp;

		switch (instType) {
			case "add":
				this.operation = (src, dest) -> dest + src;
				break;
			case "sub":
				this.operation = (src ,dest) -> dest - src;
				break;
			case "mov":
				this.operation = (src ,dest) -> src;
				break;
			default:
				System.err.println("unknown instr type for binary inst: " + instType);
				System.exit(1);
		}
	}

	@Override
		public void eval() { 
			destination.setValue(operation.applyAsInt(source.getValue(), destination.getValue()));
		}

	@Override
		public String toString() {
			return instructionType + " " + source.toString() + ", " + destination.toString();
		}
}

abstract class Operand {
	protected HashMap <String, Integer> registers;
	public abstract int getValue();
	public abstract void setValue(int val);
} 

class RegOperand extends Operand {
	private String regName;

	public RegOperand(String regName, HashMap<String, Integer> registers) {
		this.registers = registers;
		this.regName = regName;
	}

	@Override
		public int getValue() { return registers.get(regName); }

	@Override
		public void setValue(int val) {
			registers.put(this.regName, val);
		}

	@Override
		public String toString() {
			return "%" + regName;
		}
}

class MemoryOperand extends Operand {
	private String baseReg;
	private String indexReg;
	private int scale;
	private int offset;

	public MemoryOperand(String baseReg, String indexReg, int scale, int offset, HashMap<String, Integer> registers) {
		this.baseReg = baseReg;
		this.indexReg = indexReg;
		this.scale = scale;
		this.offset = offset;
		this.registers = registers;
	}

	@Override
		public int getValue() { return 5298; }

	@Override
		public void setValue(int val) {}

	@Override
		public String toString() {
			String res = offset + "(%" + baseReg;
			if (indexReg != null) {
				res += ", %" + indexReg + ", " + scale;
			}

			res += ")";
			return res;
		}
}

class ConstantOperand extends Operand {
	private int constant;

	public ConstantOperand(int val) {
		this.constant = val;
	}

	@Override
		public int getValue() { return 0; }

	@Override
		public void setValue(int val) { 
			System.err.println("Why are you trying to set a constant?");
		}

	@Override
		public String toString() {
			return "$" + constant;
		}
}
