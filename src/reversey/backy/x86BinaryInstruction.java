package reversey.backy;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

@FunctionalInterface
interface BinaryX86Operation {

    MachineState apply(MachineState state, Operand src, Operand dest);
}

/**
 * Class representing an x86 instruction with two operands.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class x86BinaryInstruction extends x86Instruction {

    /**
     * An operand used solely as a source for the operation.
     */
    private Operand source;

    /**
     * The function performed by this instruction.
     */
    private BinaryX86Operation operation;

    // TODO: instType should be of type InstructionType
    /**
     * @param instType String representation of the instruction's operation.
     * @param srcOp A source operand of the instruction.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     */
    public x86BinaryInstruction(String instType, Operand srcOp, Operand destOp, OpSize size, int line) {
        this.source = srcOp;
        this.destination = destOp;
        this.opSize = size;
        this.lineNum = line;
		this.type = InstructionType.valueOf(instType.toUpperCase());
        
        switch (instType) {
            case "add":
                this.operation = this::add;
                break;
            case "sub":
                this.operation = this::sub;
                break;
            case "cmp":
                this.operation = this::cmp;
                break;
            case "xor":
                this.operation = this::xor;
                break;
            case "or":
                this.operation = this::or;
                break;
            case "and":
                this.operation = this::and;
                break;
            case "test":
                this.operation = this::test;
                break;
            case "sal":
                this.operation = this::sal;
                break;
            case "shl":
                this.operation = this::sal;
                break;
            case "sar":
                this.operation = this::sar;
                break;
            case "shr":
                this.operation = this::shr;
                break;
            case "mov":
                this.operation = this::mov;
                break;
            case "lea":
                this.operation = this::lea;
                break;
            default:
                System.err.println("unknown instr type for binary inst: " + instType);
                System.exit(1);
        }
    }

	public MachineState add(MachineState state, Operand src, Operand dest) {
		BigInteger src1 = dest.getValue(state);
		BigInteger src2 = src.getValue(state);
		BigInteger result = src1.add(src2);

        Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

		result = truncate(result);

		setSignAndZeroFlags(result, flags);
		flags.put("cf", false); // FIXME: implement
		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState sub(MachineState state, Operand src, Operand dest) {
		BigInteger src1 = dest.getValue(state);
		BigInteger src2 = src.getValue(state);
		BigInteger result = src1.subtract(src2);

        Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

		result = truncate(result);

		setSignAndZeroFlags(result, flags);
		flags.put("cf", false); // FIXME: implement
		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState cmp(MachineState state, Operand src, Operand dest) {
		BigInteger src1 = dest.getValue(state);
		BigInteger src2 = src.getValue(state);
		BigInteger result = src1.subtract(src2);

        Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

		result = truncate(result);

		setSignAndZeroFlags(result, flags);
		flags.put("cf", false); // FIXME: implement
		return dest.updateState(state, Optional.empty(), flags, true);
	}

	/**
	 * Gets set of condition flags for a logical operation, based on the result
	 * being val.
	 *
	 * @param val The result used for setting zf and sf.
	 * @return Set of condition flags.
	 */
	public Map<String, Boolean> getLogicalOpFlags(BigInteger val) {

        Map<String, Boolean> flags = new HashMap<String, Boolean>();
		setSignAndZeroFlags(val, flags);
		flags.put("of", false);
		flags.put("cf", false);

		return flags;
	}

	public MachineState xor(MachineState state, Operand src, Operand dest) {
		BigInteger result = dest.getValue(state).xor(src.getValue(state));
		Map<String, Boolean> flags = getLogicalOpFlags(result);
		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState or(MachineState state, Operand src, Operand dest) {
		BigInteger result = dest.getValue(state).or(src.getValue(state));
		Map<String, Boolean> flags = getLogicalOpFlags(result);
		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState and(MachineState state, Operand src, Operand dest) {
		BigInteger result = dest.getValue(state).and(src.getValue(state));
		Map<String, Boolean> flags = getLogicalOpFlags(result);
		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState test(MachineState state, Operand src, Operand dest) {
		BigInteger result = dest.getValue(state).and(src.getValue(state));
		Map<String, Boolean> flags = getLogicalOpFlags(result);
		return dest.updateState(state, Optional.empty(), flags, true);
	}

	public MachineState sal(MachineState state, Operand src, Operand dest) {
		int shamt = src.getValue(state).intValue() % 32; // max shift amount is 31
		BigInteger orig = dest.getValue(state);
		BigInteger result = orig.shiftLeft(shamt);

		int msbIndex = this.opSize.numBits() - 1;

		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		setSignAndZeroFlags(result, flags);

		if (shamt > 0 && (msbIndex + 1) >= shamt) {
			flags.put("cf", orig.testBit((msbIndex + 1) - shamt));
		} else if ((msbIndex + 1) >= shamt) {
			flags.put("cf", false); // TODO: not sure if this is handled correctly
		}
		// overflow is only defined when shifting by 1
		if (shamt == 1) {
			flags.put("of", orig.testBit(msbIndex) != orig.testBit(msbIndex - 1));
		} else {
			// This is an undefined case... false sounds great
			// doesn't it?
			flags.put("of", false);
		}

		byte[] resArray = result.toByteArray();
		if (resArray.length > this.opSize.numBytes()) {
			byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
			result = new BigInteger(ba);
		}

		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState sar(MachineState state, Operand src, Operand dest) {
		int shamt = src.getValue(state).intValue() % 32; // max shift amount is 31
		BigInteger orig = dest.getValue(state);
		BigInteger result = orig.shiftRight(shamt);

		assert result.bitLength() + 1 > this.opSize.numBits();

		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		setSignAndZeroFlags(result, flags);

		// overflow is false if shifting by 1, otherwise
		// undefined
		if (shamt == 1) {
			flags.put("of", false);
		} else {
			// This is an undefined case... false sounds great
			// doesn't it?
			flags.put("of", false);
		}

		// shift by zero means CF isn't changed
		if (shamt > 0) {
			flags.put("cf", orig.testBit(shamt - 1));
		}

		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState shr(MachineState state, Operand src, Operand dest) {
		int shamt = src.getValue(state).intValue() % 32; // max shift amount is 31
		BigInteger orig = dest.getValue(state);

		// BigInteger doesn't have logical right shift (>>>)
		// so we do the ugly thing here and use the >>>
		// operator by converting to the native types of the
		// operators.
		String s = null;
		switch (this.opSize) {
			case BYTE:
				byte b = orig.byteValue();
				b = (byte) (b >>> shamt);
				s = "" + b;
				break;
			case WORD:
				short w = orig.shortValue();
				w = (short) (w >>> shamt);
				s = "" + w;
				break;
			case LONG:
				int l = orig.intValue();
				l = l >>> shamt;
				s = "" + l;
				break;
			case QUAD:
				long q = orig.longValue();
				q = q >>> shamt;
				s = "" + q;
				break;
		}
		BigInteger result = new BigInteger(s);


		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		setSignAndZeroFlags(result, flags);

		// overflow is the most sig bit of original if shifting by 1, otherwise
		// undefined
		if (shamt == 1) {
			flags.put("of", orig.testBit(this.opSize.numBytes() * 8 - 1));
		} else {
			// This is an undefined case... false sounds great
			// doesn't it?
			flags.put("of", false);
		}

		if (shamt > 0) {
			// shift by zero means CF isn't changed
			flags.put("cf", orig.testBit(shamt - 1));
		}

		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState mov(MachineState state, Operand src, Operand dest) {
        Map<String, Boolean> flags = new HashMap<String, Boolean>();
        return dest.updateState(state, Optional.of(src.getValue(state)), flags, true);
	}

	public MachineState lea(MachineState state, Operand src, Operand dest) {
		// TODO: Use polymorophism to avoid this instanceof junk
		if (!(src instanceof MemoryOperand)) {
			System.err.println("ERROR: lea src must be a memory operand");
			return null;
		}

		MemoryOperand mo = (MemoryOperand) src;
        Map<String, Boolean> flags = new HashMap<String, Boolean>();
		return dest.updateState(state, Optional.of(BigInteger.valueOf(mo.calculateAddress(state))), flags, true);
	}


    @Override
    public MachineState eval(MachineState state) {
        return operation.apply(state, this.source, this.destination);
    }

    @Override
    public Set<String> getUsedRegisters() {
        Set<String> sourceRegs = source.getUsedRegisters();
        Set<String> destRegs = destination.getUsedRegisters();
        sourceRegs.addAll(destRegs);
        return sourceRegs;
    }
    
    @Override
    public String toString() {
        return lineNum + ": \t" + getInstructionTypeString() + " " + source.toString() + ", " + destination.toString();
    }
}
