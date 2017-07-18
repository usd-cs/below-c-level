package reversey.backy;

import java.math.BigInteger;
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
     * The operand where the instruction will write its results.
     */
    protected Operand destination;
    
    /**
     * The function that this instruction performs.
     */
    private UnaryX86Operation operation;

    /**
     * An optional predicate to be used with conditional instructions.
     */
    private Optional<Predicate<MachineState>> conditionCheck = Optional.empty();

    /**
     * @param instType The type of operation performed by the instruction.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     * @param line The line number associated with this instruction.
     */
    public x86UnaryInstruction(InstructionType instType, Operand destOp, OpSize size, int line, x86Comment c) {
        this.type = instType;
        this.destination = destOp;
        this.opSize = size;
        this.lineNum = line;
        this.comment = Optional.ofNullable(c);

        switch (instType) {
            case IDIV:
                this.operation = this::idiv;
                break;
            case INC:
                this.operation = this::inc;
                break;
            case DEC:
                this.operation = this::dec;
                break;
            case NEG:
                this.operation = this::neg;
                break;
            case NOT:
                this.operation = this::not;
                break;
            case SETE:
            case SETNE:
            case SETS:
            case SETNS:
            case SETG:
            case SETGE:
            case SETL:
            case SETLE:
            case SETA:
            case SETAE:
            case SETB:
            case SETBE:
                this.conditionCheck = Optional.of(conditions.get(instType.name().toLowerCase().substring(3)));
                this.operation = this::set;
                break;
            case JE:
            case JNE:
            case JS:
            case JNS:
            case JG:
            case JGE:
            case JL:
            case JLE:
            case JA:
            case JAE:
            case JB:
            case JBE:
                this.conditionCheck = Optional.of(conditions.get(instType.name().toLowerCase().substring(1)));
                this.operation = this::jump;
                break;
            case JMP:
                this.conditionCheck = Optional.of(conditions.get("jmp"));
                this.operation = this::jump;
                break;
            case PUSH:
                this.operation = this::push;
                break;
            case POP:
                this.operation = this::pop;
                break;
            case CALL:
                this.operation = this::call;
                break;
            default:
                throw new RuntimeException("unsupported instr type: " + instType);
        }
    }
    
    /**
     * Performs "signed" division, storing the quotient and remainder in two
     * different registers.
     * 
     * @param state The state on which to perform the operation.
     * @param src An operand that acts as the divisor.
     * @return A state that is a clone of the starting state but updated to account
     * for the execution of this instruction.
     */
    public MachineState idiv(MachineState state, Operand src) {
        BigInteger src1 = state.getCombinedRegisterValue(opSize);
        BigInteger src2 = src.getValue(state);
        
        // quotient and remainder are both calculated
        BigInteger divResult = src1.divide(src2);
        BigInteger modResult = src1.mod(src2);
        
        RegOperand modDest = null;
        RegOperand divDest = null;
        
        // determine which registers will be used for the quotient and remainder
        switch (this.opSize) {
            case QUAD:
                modDest = new RegOperand("rdx", this.opSize);
                divDest = new RegOperand("rax", this.opSize);
                break;
            case LONG:
                modDest = new RegOperand("edx", this.opSize);
                divDest = new RegOperand("eax", this.opSize);
                break;
            case WORD:
                modDest = new RegOperand("dx", this.opSize);
                divDest = new RegOperand("ax", this.opSize);
                break;
            case BYTE:
                modDest = new RegOperand("ah", this.opSize);
                divDest = new RegOperand("al", this.opSize);
                break;
            default:
                throw new RuntimeException("Unsupported op size");
        }

        MachineState tmp = divDest.updateState(state, Optional.of(divResult), new HashMap<>(), false);
        return modDest.updateState(tmp, Optional.of(modResult), new HashMap<>(), true);
    }

    public MachineState inc(MachineState state, Operand dest) {
        BigInteger result = dest.getValue(state).add(BigInteger.ONE);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        setSignAndZeroFlags(result, flags);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState dec(MachineState state, Operand dest) {
        BigInteger result = dest.getValue(state).subtract(BigInteger.ONE);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        setSignAndZeroFlags(result, flags);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState neg(MachineState state, Operand dest) {
        BigInteger orig = dest.getValue(state);
        
        // The x64 manual states that neg does 0 - operand so we'll do the
        // same even though BigInteger has a negate method
        BigInteger result = BigInteger.ZERO.subtract(orig);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        
        setSignAndZeroFlags(result, flags);
        flags.put("cf", orig.compareTo(BigInteger.ZERO) != 0);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState not(MachineState state, Operand dest) {
        BigInteger result = dest.getValue(state).not();
        Map<String, Boolean> flags = new HashMap<>();
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    public MachineState push(MachineState state, Operand src) {
        Map<String, Boolean> flags = new HashMap<>();

        // step 1: subtract 8 from rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
        MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);

        // step 2: store src operand value in (%rsp)
        MemoryOperand dest = new MemoryOperand("rsp", null, 1, 0, this.opSize);

        return dest.updateState(tmp, Optional.of(src.getValue(tmp)), flags, true);
    }

    public MachineState pop(MachineState state, Operand dest) {
        Map<String, Boolean> flags = new HashMap<>();

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
        return dest.updateState(state, Optional.of(result), new HashMap<>(), true);
    }

    public MachineState jump(MachineState state, Operand dest) {
        assert this.conditionCheck.isPresent();
        Map<String, Boolean> flags = new HashMap<>();
        if(this.conditionCheck.get().test(state)){
            return dest.updateState(state, Optional.of(dest.getValue(state)), flags, false); 
        } else {
            return dest.updateState(state, Optional.empty(), flags, true); 
        }
    }
    
    public MachineState call(MachineState state, Operand dest) {
        Map<String, Boolean> flags = new HashMap<>();
        
        // step 1: subtract 8 from rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
        MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);
        
        BigInteger returnAddr = tmp.getRipRegister().add(BigInteger.ONE);

        // step 2: store return address in (%rsp)
        MemoryOperand rspMemOperand = new MemoryOperand("rsp", null, 1, 0, this.opSize);
        tmp = rspMemOperand.updateState(tmp, Optional.of(returnAddr), flags, false);

        // return new state with rip set to beginning of callee
        return dest.updateState(tmp, Optional.of(dest.getValue(state)), flags, false); 
    }

    @Override
    public MachineState eval(MachineState state) {
        return operation.apply(state, this.destination);
    }

    @Override
    public Set<String> getUsedRegisters() {
        Set<String> result = destination.getUsedRegisters();
        
        // Check for implicitly used registers
        switch (this.type) {
            case PUSH:
            case POP:
            case CALL:
                result.add("rsp");
                break;
            case IDIV:
                result.add("rax");
                if (this.opSize != OpSize.BYTE) result.add("rdx");
                break;
        }
        return result;
    }
    
    @Override
    public void updateLabels(String labelName, x86Label label){
        destination.updateLabels(labelName, label);
    }
    
    @Override
    public String toString() {
        String s = lineNum + ": \t" + getInstructionTypeString() + " " + destination.toString();
        if(comment.isPresent()){
            s += comment.get().toString();
        }
        return s;
    }
}

