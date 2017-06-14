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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
			case "xorl":
			case "orl":
			case "andl":
			case "shll":
			case "sall":
			case "shrl":
			case "sarl":
			case "movl":
			case "leal":
				return true;
			case "incl":
			case "decl":
			case "negl":
			case "notl":
			case "pushl":
			case "popl":
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

			System.out.println("opString: " + opString);

			String[] components = opString.split("[(),]");
			if (components.length < 2 || components.length > 4) {
				System.err.println("ERROR: invalid number of memory op components");
				return null; // TODO: throw exception
			}

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
	private UnaryX86Operation operation;

	/**
	 * @param instType String representation of the instruction's operation.
	 * @param destOp Operand representing the destination of the instruction.
	 */
	public x86UnaryInstruction(String instType, Operand destOp) {
		this.instructionType = instType;
		this.destination = destOp;

		switch (instType) {
			case "inc":
				this.operation = 
					(state, dest) -> dest.updateState(state, dest.getValue(state).add(BigInteger.ONE)); 
				break;
			case "dec":
				this.operation = 
					(state, dest) -> dest.updateState(state, dest.getValue(state).subtract(BigInteger.ONE)); 
				break;
			case "neg":
				this.operation = 
					(state, dest) -> dest.updateState(state, dest.getValue(state).negate());
				break;
			case "not":
				this.operation = 
					(state, dest) -> dest.updateState(state, dest.getValue(state).not());
				break;
			case "push":
				this.operation = 
					(state, src) -> { 
						// step 1: subtract 8 from rsp
						// FIXME: esp should become rsp, 4 should become 8
						RegOperand rsp = new RegOperand("esp");
						MachineState tmp = rsp.updateState(state, rsp.getValue(state).subtract(BigInteger.valueOf(4)));

						// step 2: store src operand value in (%rsp)
						MemoryOperand dest = new MemoryOperand("esp", null, 1, 0);
						return dest.updateState(tmp, src.getValue(tmp)); 
					};
				break;
			case "pop":
				this.operation = 
					(state, dest) -> { 
						// step 1: store (%rsp) value in dest operand 
						MemoryOperand src = new MemoryOperand("esp", null, 1, 0);
						MachineState tmp = dest.updateState(state, src.getValue(state)); 

						// step 2: add 8 to rsp
						// FIXME: esp should become rsp, 4 should become 8
						RegOperand rsp = new RegOperand("esp");
						return rsp.updateState(tmp, rsp.getValue(tmp).add(BigInteger.valueOf(4)));

					};
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
			case "neg":
			case "not":
			case "push":
			case "pop":
				return operation.apply(state, this.destination);
			default:
				System.err.println("Something went terribly wrong.");
				return null; // TODO: exception?
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
	/**
	 * An operand used solely as a source for the operation.
	 */
	private Operand source;

	/**
	 * The function performed by this instruction.
	 */
	private BinaryX86Operation operation;

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
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state).add(src.getValue(state)));
				break;
			case "sub":
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state).subtract(src.getValue(state)));
				break;
			case "xor":
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state).xor(src.getValue(state)));
				break;
			case "or":
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state).or(src.getValue(state)));
				break;
			case "and":
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state).and(src.getValue(state)));
				break;
			case "sal":
			case "shl":
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state).shiftLeft(src.getValue(state).intValue()));
				break;
			case "sar":
			case "shr": // FIXME: shr needs to be arithmetic shift, not sure how to do this with BigInteger
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state).shiftRight(src.getValue(state).intValue()));
				break;
			/*
			case "shr":
				this.operation = 
					(state, src, dest) -> dest.updateState(state, dest.getValue(state) >>> src.getValue(state));
				break;
			*/
			case "mov":
				this.operation = 
					(state, src, dest) -> dest.updateState(state, src.getValue(state));
				break;
			case "lea":
				this.operation = 
					(state, src, dest) -> {
						// TODO: Use polymorophism to avoid this instanceof junk
						if (!(src instanceof MemoryOperand)) {
							System.err.println("ERROR: lea src must be a memory operand");
							return null;
						}

						MemoryOperand mo = (MemoryOperand)src;
						return dest.updateState(state, BigInteger.valueOf(mo.calculateAddress(state)));
					};
				break;
			default:
				System.err.println("unknown instr type for binary inst: " + instType);
				System.exit(1);
		}
	}

	@Override
	public MachineState eval(MachineState state) {
		return operation.apply(state, this.source, this.destination);
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
	public abstract BigInteger getValue(MachineState state);

	/**
	 * @param currState The current state of the machine.
	 * @param val The value to update the operand with.
	 * @return The state after updating the current state with the new value for
	 * the operand.
	 */
	public abstract MachineState updateState(MachineState currState, BigInteger val);
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
	public BigInteger getValue(MachineState state) { 
		return state.getRegisterValue(regName);
	}

	@Override
	public MachineState updateState(MachineState currState, BigInteger val) {
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
	public int calculateAddress(MachineState state) { // FIXME: should this return long?
		int address = state.getRegisterValue(baseReg).add(BigInteger.valueOf(offset)).intValue();
		if (indexReg != null) {
			address += state.getRegisterValue(indexReg).multiply(BigInteger.valueOf(scale)).intValue();
		}

		return address;
	}

	@Override
	public BigInteger getValue(MachineState state) { 
		return state.getMemoryValue(calculateAddress(state), 4); // FIXME: 4 should be based on op size
	}

	@Override
	public MachineState updateState(MachineState currState, BigInteger val) {
		return currState.getNewState(calculateAddress(currState), val, 4); // FIXME: 4 should be based op size
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
	private long constant;

	public ConstantOperand(long val) {
		this.constant = val;
	}

	@Override
	public BigInteger getValue(MachineState state) { return BigInteger.valueOf(constant); }

	@Override
	public MachineState updateState(MachineState currState, BigInteger val) { 
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
	private Map<String, byte[]> registers;

	/**
	 * The machine's memory.
	 */
	private Map<Integer, Byte> memory;

	/**
	 * Create a new state with all registers (except %rsp) initialized to 0 but
	 * no memory initialization. %rsp is initialized to 0x7FFFFFFF.
	 */
	public MachineState() {
		this.registers = new HashMap<String, byte[]>();
		this.memory = new HashMap<Integer, Byte>();

		String[] regNames = {"eax", "ebx", "ecx", "edx", "esi", "edi", "ebp"};
		for (String s : regNames)
			registers.put(s, new byte[4]); // FIXME: should be 8 to support 64-bit registers

		registers.put("esp", ByteBuffer.allocate(4).putInt(0x7FFFFFFF).array()); // FIXME 8 bytes when adding 64-bit support
	}

	public MachineState(Map<String, byte[]> reg, Map<Integer, Byte> mem) {
		this.registers = reg;
		this.memory = mem;
	}

	/**
	 * Create a new MachineState based on the current state but with an updated
	 * value for a memory address.
	 *
	 * @param address The starting (i.e. lowest) address that will be changed.
	 * @param val The new value of the given memory address.
	 * @param size The number of bytes to write to memory.
	 * @return A new state that is the same as the current but with new binding
	 * from given address to given val.
	 */
	public MachineState getNewState(int address, BigInteger val, int size) {
		Map<Integer, Byte> mem = new HashMap<Integer, Byte>(this.memory);

		byte[] valArray = val.toByteArray();

		for (int src = 0, dest = address + (valArray.length - 1);
				src < valArray.length; src++, dest++) {
			mem.put(dest, valArray[src]);
		}

		for (int i = address + valArray.length; i < (address + size); i++) {
			if (val.signum() == -1) mem.put(i, (byte)0xFF);
			else mem.put(i, (byte)0);
		}

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
	public MachineState getNewState(String regName, BigInteger val) {
		Map<String, byte[]> reg = new HashMap<String, byte[]>(registers);
		byte[] valArray = val.toByteArray();
		byte[] newVal = new byte[4]; // FIXME: this should be 8 for x86-64

		for (int src = 0, dest = (newVal.length - valArray.length); 
				src < valArray.length; src++, dest++) {
			newVal[dest] = valArray[src];
		}

		if (val.signum() == -1) {
			for (int i = 0; i < newVal.length - valArray.length; i++)
				newVal[i] = (byte)0xFF;
		}

		reg.put(regName, newVal);
		return new MachineState(reg, this.memory);
	}

	/**
	 * Gets the value stored in the given register.
	 */
	public BigInteger getRegisterValue(String regName) {
		return new BigInteger(registers.get(regName));
	}

	/**
	 * Gets the value stored at the given memory address.
	 *
	 * @param address The starting address where the value is stored.
	 * @param size The number of bytes of memory to read.
	 */
	public BigInteger getMemoryValue(int address, int size) {
		byte[] val = new byte[4];

		for (int addr = address+(size-1), dest = 0; dest < size; addr--, dest++)
			val[dest] = memory.get(addr);

		return new BigInteger(val);
	}

	public String toString() {
		String s = "Registers:\n";
		for (Map.Entry<String, byte[]> entry : registers.entrySet()) {
			BigInteger b = new BigInteger(entry.getValue());
			s += "\t" + entry.getKey() + ": " + b.toString() + " (0x" + b.toString(16) + ")\n";
		}

		s += "Memory:\n";
		for (Map.Entry<Integer, Byte> entry : memory.entrySet()) {
			s += "\t" + Integer.toHexString(entry.getKey()) + ": " + String.format("%02x", entry.getValue()) + "\n";
		}

		return s;
	}
}

@FunctionalInterface
interface BinaryX86Operation {
	MachineState apply(MachineState state, Operand src, Operand dest);
}

@FunctionalInterface
interface UnaryX86Operation {
	MachineState apply(MachineState state, Operand dest);
}

/**
 * Simple class to test x86 instruction parsing and evaluation.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86InstructionTester {
	public static void main(String[] args) {
		ArrayList<x86Instruction> instructions = new ArrayList<x86Instruction>();
		instructions.add(x86Instruction.parseInstruction("movl $9, %eax"));
		instructions.add(x86Instruction.parseInstruction("movl $4, %ebx"));
		instructions.add(x86Instruction.parseInstruction("addl %eax, %ebx"));
		instructions.add(x86Instruction.parseInstruction("pushl %ebx"));
		instructions.add(x86Instruction.parseInstruction("popl %ecx"));
		instructions.add(x86Instruction.parseInstruction("leal -8(%esp), %edx"));
		instructions.add(x86Instruction.parseInstruction("movl $73, (%edx)"));
		instructions.add(x86Instruction.parseInstruction("incl %esi"));
		instructions.add(x86Instruction.parseInstruction("decl %edi"));

		MachineState state = new MachineState();
		System.out.println(state);
		for (x86Instruction inst : instructions) {
			System.out.println(inst);
			state = inst.eval(state);
			System.out.println(state);
		}
	}
}
