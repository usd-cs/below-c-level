/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.Map;
import java.util.HashMap;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.util.Pair;

/**
 * An abstract class representing an x86-64 instruction.
 *
 * @author Dr. Sat
 */
public abstract class x86Instruction {
	/**
	 * The operand where the instruction will write its results.
	 */
	protected Operand destination;

	/**
	 * The type of instruction (e.g. add)
	 */
	protected String instructionType;

	// 

	/**
	 * Perform the operation specific to the instruction.
	 * 
	 * @param state The state of the machine before evaluation begins.
	 * @return State of machine after evaluating the instruction.
	 */
	public abstract MachineState eval(MachineState state);

	/**
	 * Checks that instruction is a valid, supported x86 instruction.
	 *
	 * @param instrName The name of the instruction (e.g. addl)
	 * @return true if a supported binary instruction, false if a supported
	 * unary instruction.
	 */
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
				System.exit(1); // TODO: throw exception
				return false;
		}
	}

	/**
	 * Get the register name from a given string.
	 *
	 * @param str The string that contains a register name, starting with %
	 * @return Name of the register, or null if invalid register.
	 */
	public static String parseRegister(String str) {
		System.out.println("parseRegister: " + str);
		String regRegEx = "^\\%(eax|ebx|ecx|edx|esi|edi|ebp|esp|eip)$";
		if (Pattern.matches(regRegEx, str))
			return str.substring(1);
		else
			return null; // TODO: throw exception
	}

	/**
	 * Construct an operand based on a given string.
	 *
	 * @param String that contains the operand at the beginning.
	 * @return The parsed operand and the index in str where the operand ends.
	 */
	public static Pair<Operand, Integer> parseOperand(String str) {
		System.out.println("parsing Operand in: " + str);

		Operand op = null;
		int endPoint = 0;

		if (str.charAt(0) == '$') {
			// constant operand
			System.out.println("found a constant op");
			String[] splits = str.split("[$,]");
			op = new ConstantOperand(Integer.parseInt(splits[1])); // TODO: handle hex constant
		}
		else if (str.charAt(0) == '%') {
			// register operand
			System.out.println("found a register op");
			String[] splits = str.split(",");
			String regName = parseRegister(splits[0]);
			op = new RegOperand(regName);
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

			op = new MemoryOperand(baseReg, indexReg, scale, offset);
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

	/**
	 * Create an x86-64 instruction by parsing a given string.
	 *
	 * @param instr A string representation of the instruction.
	 * @return The parsed instruction.
	 */
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

	public abstract String toString();
}

/**
 * Class representing an x86 instruction with a single operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86UnaryInstruction extends x86Instruction {
	/**
	 * The function that this instruction performs.
	 */
	private IntUnaryOperator operation;

	/**
	 * @param instType String representation of the instruction's operation.
	 * @param destOp Operand representing the destination of the instruction.
	 */
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
	public MachineState eval(MachineState state) {
		switch (this.instructionType) {
			case "inc":
			case "dec":
				int destVal = operation.applyAsInt(destination.getValue(state));
				return destination.updateState(state, destVal);
			case "push":
				System.out.println("push not supported yet");
				return state;
			case "pop":
				System.out.println("pop not supported yet");
				return state;
			default:
				System.err.println("Something went terribly wrong.");
				return null;
		}
	}

	@Override
	public String toString() {
		return instructionType + " " + destination.toString();
	}
}

/**
 * Class representing an x86 instruction with two operands.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86BinaryInstruction extends x86Instruction{
	private Operand source;
	private IntBinaryOperator operation;

	/**
	 * @param instType String representation of the instruction's operation.
	 * @param srcOp A source operand of the instruction.
	 * @param destOp Operand representing the destination of the instruction.
	 */
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
	public MachineState eval(MachineState state) {
		return destination.updateState(state, operation.applyAsInt(source.getValue(state), destination.getValue(state)));
	}

	@Override
	public String toString() {
		return instructionType + " " + source.toString() + ", " + destination.toString();
	}
}

/**
 * An abstract class representing an x86 operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
abstract class Operand {

	/**
	 * @param state The state of the machine.
	 * @return The value of the operand in a machine with the given state.
	 */
	public abstract int getValue(MachineState state);

	/**
	 * @param currState The current state of the machine.
	 * @param val The value to update the operand with.
	 * @return The state after updating the current state with the new value for
	 * the operand.
	 */
	public abstract MachineState updateState(MachineState currState, int val);
} 

/**
 * A class representing an x86-64 register operand (e.g. %eax).
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class RegOperand extends Operand {
	private String regName;

	public RegOperand(String regName) {
		this.regName = regName;
	}

	@Override
	public int getValue(MachineState state) { 
		return state.getRegisterValue(regName);
	}

	@Override
	public MachineState updateState(MachineState currState, int val) {
		return currState.getNewState(this.regName, val);
	}

	@Override
		public String toString() {
			return "%" + regName;
		}
}


/**
 * A class representing an x86-64 memory operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class MemoryOperand extends Operand {
	/**
	 * Name of the base register.
	 */
	private String baseReg;

	/**
	 * Name of the index register.
	 */
	private String indexReg;

	/**
	 * The scaling factor for the index register.
	 */
	private int scale;

	/**
	 * The offset amount.
	 */
	private int offset;

	public MemoryOperand(String baseReg, String indexReg, int scale, int offset) {
		this.baseReg = baseReg;
		this.indexReg = indexReg;
		this.scale = scale;
		this.offset = offset;
	}

	/**
	 * Calculate the effective address of the operand, given the specified
	 * machine state.
	 *
	 * @param state The state in which to calculate the address.
	 * @return The effective address.
	 */
	private int calculateAddress(MachineState state) {
		int address = state.getRegisterValue(baseReg) + offset;
		if (indexReg != null) {
			address += state.getRegisterValue(indexReg) * scale;
		}

		return address;
	}

	@Override
	public int getValue(MachineState state) { 
		return state.getMemoryValue(calculateAddress(state));
	}

	@Override
	public MachineState updateState(MachineState currState, int val) {
		return currState.getNewState(calculateAddress(currState), val);
	}

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

/**
 * A class representing an x86-64 constant (i.e. immediate) operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class ConstantOperand extends Operand {
	/**
	 * The operand's value.
	 */
	private int constant;

	public ConstantOperand(int val) {
		this.constant = val;
	}

	@Override
	public int getValue(MachineState state) { return constant; }

	@Override
	public MachineState updateState(MachineState currState, int val) { 
		System.err.println("Why are you trying to set a constant?");
		// TODO: exception here?
		return currState;
	}

	@Override
	public String toString() {
		return "$" + constant;
	}
}

/**
 * A class representing the state of the machine, namely its register file and
 * memory.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class MachineState {
	/**
	 * The register file.
	 */
	private Map<String, Integer> registers;

	/**
	 * The machine's memory.
	 */
	private Map<Integer, Integer> memory;

	/**
	 * Create a new state with all registers initialized to 0 but no memory
	 * initialization.
	 */
	public MachineState() {
		this.registers = new HashMap<String, Integer>();
		this.memory = new HashMap<Integer, Integer>();

		String[] regNames = {"eax", "ebx", "ecx", "edx", "esi", "edi", "ebp", "esp"};
		for (String s : regNames)
			registers.put(s, 0);
	}

	public MachineState(Map<String, Integer> reg, Map<Integer, Integer> mem) {
		this.registers = reg;
		this.memory = mem;
	}

	/**
	 * Create a new MachineState based on the current state but with an updated
	 * value for a memory address.
	 *
	 * @param address The address that will be changed in the new state.
	 * @param val The new value of the given memory address.
	 * @return A new state that is the same as the current but with new binding
	 * from given address to given val.
	 */
	public MachineState getNewState(int address, int val) {
		Map<Integer, Integer> mem = new HashMap<Integer, Integer>(this.memory);
		mem.put(address, val);
		return new MachineState(this.registers, mem);
	}

	/**
	 * Create a new MachineState based on the current state but with an updated
	 * value for a register.
	 *
	 * @param regName The register that will be updated.
	 * @param val The new value of the given register.
	 * @return A new state that is the same as the current but with new binding
	 * from given register to given val
	 */
	public MachineState getNewState(String regName, int val) {
		Map<String, Integer> reg = new HashMap<String, Integer>(registers);
		reg.put(regName, val);
		return new MachineState(reg, this.memory);
	}

	/**
	 * Gets the value stored in the given register.
	 */
	public int getRegisterValue(String regName) {
		return registers.get(regName);
	}

	/**
	 * Gets the value stored at the given memory address.
	 */
	public int getMemoryValue(int address) {
		return memory.get(address);
	}

	public String toString() {
		String s = "Registers:\n";
		for (Map.Entry<String, Integer> entry : registers.entrySet()) {
			s += "\t" + entry.getKey() + ": " + entry.getValue() + "\n";
		}

		s += "Memory:\n";
		for (Map.Entry<Integer, Integer> entry : memory.entrySet()) {
			s += "\t" + entry.getKey() + ": " + entry.getValue() + "\n";
		}

		return s;
	}
}

/**
 * Simple class to test x86 instruction parsing and evaluation.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86InstructionTester {
	public static void main(String[] args) {
		MachineState state = new MachineState();
		x86Instruction inst1 = x86Instruction.parseInstruction("movl $17, %eax");
		System.out.println("Before:");
		System.out.println(state);
		MachineState state2 = inst1.eval(state);
		System.out.println("After:");
		System.out.println(state2);
	}
}
