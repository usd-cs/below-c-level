package reversey.backy;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@FunctionalInterface
interface UnaryX86Operation {

    MachineState apply(MachineState state, Operand dest);
}

/**
 * Class representing an x86 instruction with a single operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class x86UnaryInstruction extends x86Instruction {
    /**
     * The function that this instruction performs.
     */
    private UnaryX86Operation operation;

	/**
	 * An optional predicate to be used with conditional instructions.
	 */
	private Optional<Predicate<MachineState>> conditionCheck = Optional.empty();

    /**
     * @param instType String representation of the instruction's operation.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     */
    public x86UnaryInstruction(String instType, Operand destOp, OpSize size, int line) {
        this.destination = destOp;
        this.opSize = size;
        this.lineNum = line;
		this.type = InstructionType.valueOf(instType.toUpperCase());

        switch (instType) {
            case "inc":
                this.operation = this::inc;
                break;
            case "dec":
                this.operation = this::dec;
                break;
            case "neg":
                this.operation = this::neg;
                break;
            case "not":
                this.operation = this::not;
                break;
            case "sete":
            case "setne":
            case "sets":
            case "setns":
            case "setg":
            case "setge":
            case "setl":
            case "setle":
				this.conditionCheck = Optional.of(conditions.get(instType.substring(3)));
                this.operation = this::set;
                break;
            case "je":
            case "jne":
            case "js":
            case "jns":
            case "jg":
            case "jge":
            case "jl":
            case "jle":
				this.conditionCheck = Optional.of(conditions.get(instType.substring(1)));
                this.operation = this::jump;
                break;
            case "push":
                this.operation = this::push;
                break;
            case "pop":
                this.operation = this::pop;
                break;
            default:
                System.err.println("invalid instr type for unary inst: " + instType);
                System.exit(1);
        }
    }

	public MachineState inc(MachineState state, Operand dest) {
		BigInteger result = dest.getValue(state).add(BigInteger.ONE);

		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

		result = truncate(result);
		setSignAndZeroFlags(result, flags);

		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState dec(MachineState state, Operand dest) {
		BigInteger result = dest.getValue(state).subtract(BigInteger.ONE);

		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

		result = truncate(result);
		setSignAndZeroFlags(result, flags);

		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState neg(MachineState state, Operand dest) {
		BigInteger orig = dest.getValue(state);
		BigInteger result = orig.negate();

		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

		result = truncate(result);
		setSignAndZeroFlags(result, flags);
		flags.put("cf", orig.compareTo(BigInteger.ZERO) != 0);

		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState not(MachineState state, Operand dest) {
		BigInteger result = dest.getValue(state).not();
		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState push(MachineState state, Operand src) {
		Map<String, Boolean> flags = new HashMap<String, Boolean>();

		// step 1: subtract 8 from rsp
		RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
		MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);

		// step 2: store src operand value in (%rsp)
		MemoryOperand dest = new MemoryOperand("rsp", null, 1, 0, this.opSize);

		return dest.updateState(tmp, Optional.of(src.getValue(tmp)), flags, true);
	}

	public MachineState pop(MachineState state, Operand dest) {
		Map<String, Boolean> flags = new HashMap<String, Boolean>();

		// step 1: store (%rsp) value in dest operand 
		MemoryOperand src = new MemoryOperand("rsp", null, 1, 0, this.opSize);
		MachineState tmp = dest.updateState(state, Optional.of(src.getValue(state)), flags, true);

		// step 2: add 8 to rsp
		RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);

		return rsp.updateState(tmp, Optional.of(rsp.getValue(tmp).add(BigInteger.valueOf(8))), flags, false);
	}

	public MachineState set(MachineState state, Operand dest) {
		assert this.conditionCheck.isPresent();
		BigInteger result = this.conditionCheck.get().test(state) ? BigInteger.ONE : BigInteger.ZERO;
		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		return dest.updateState(state, Optional.of(result), flags, true);
	}

	public MachineState jump(MachineState state, Operand dest) {
		assert this.conditionCheck.isPresent();
		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		if(this.conditionCheck.get().test(state)){
			return dest.updateState(state, Optional.of(dest.getValue(state)), flags, false); 
		} else {
			return dest.updateState(state, Optional.empty(), flags, true); 
		}
	}

    @Override
    public MachineState eval(MachineState state) {
        return operation.apply(state, this.destination);
    }

    @Override
    public Set<String> getUsedRegisters() {
        return destination.getUsedRegisters();
    }
    
    @Override
    public String toString() {
        return lineNum + ": \t" + getInstructionTypeString() + " " + destination.toString();
    }
}

