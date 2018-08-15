package edu.sandiego.bcl;

import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@FunctionalInterface
interface UnaryX86Operation {

    MachineState apply(MachineState state, Operand dest) throws x86RuntimeException;
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
     * @param c A comment to be associated with this instruction (or null if none).
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
     * @return A state that is a clone of the starting state but updated to
     * account for the execution of this instruction.
     */
    private MachineState idiv(MachineState state, Operand src) throws x86RuntimeException {
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

    /**
     * Perform the operation dest++.
     *
     * @param state The state in which to work.
     * @param dest The operand that will be incremented and then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest+1)}.
     */
    private MachineState inc(MachineState state, Operand dest) throws x86RuntimeException{
        BigInteger result = dest.getValue(state).add(BigInteger.ONE);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        setSignAndZeroFlags(result, flags);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Perform the operation dest--.
     *
     * @param state The state in which to work.
     * @param dest The operand that will be decremented and then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest-1)}.
     */
    private MachineState dec(MachineState state, Operand dest) throws x86RuntimeException {
        BigInteger result = dest.getValue(state).subtract(BigInteger.ONE);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);
        setSignAndZeroFlags(result, flags);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Perform the operation -dest.
     *
     * @param state The state in which to work.
     * @param dest The operand that will be negated and then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code -dest}.
     */
    private MachineState neg(MachineState state, Operand dest) throws x86RuntimeException {
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

    /**
     * Perform the operation ~dest.
     *
     * @param state The state in which to work.
     * @param dest The operand whose bits will be inverted and then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code ~dest}.
     */
    private MachineState not(MachineState state, Operand dest) throws x86RuntimeException {
        BigInteger result = dest.getValue(state).not();
        Map<String, Boolean> flags = new HashMap<>();
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Pushes the source operand onto the stack..
     *
     * @param state The state in which to work.
     * @param src The operand that will written to the top of the stack.
     * @return A clone of {@code state}, but with an incremented rip, {@code %rsp}
     * decremented by 8 and memory updated to contain {@code src} at the address
     * {@code %rsp}.
     */
    private MachineState push(MachineState state, Operand src) throws x86RuntimeException {
        Map<String, Boolean> flags = new HashMap<>();

        // step 1: subtract 8 from rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
        MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);

        // step 2: store src operand value in (%rsp)
        MemoryOperand dest = new MemoryOperand("rsp", null, null, null, this.opSize, "");

        return dest.updateState(tmp, Optional.of(src.getValue(tmp)), flags, true);
    }

    /**
     * Pops the value off the top of the stack, storing it in dest.
     *
     * @param state The state in which to work.
     * @param dest The location where the popped value will be stored.
     * @return A clone of {@code state}, but with an incremented rip, 
     * {@code dest} updated with the value at {@code %rsp}, and {@code %rsp}
     * incremented by 8.
     */
    private MachineState pop(MachineState state, Operand dest) throws x86RuntimeException {
        Map<String, Boolean> flags = new HashMap<>();

        // step 1: store (%rsp) value in dest operand 
        MemoryOperand src = new MemoryOperand("rsp", null, null, null, this.opSize, "");
        MachineState tmp = dest.updateState(state, Optional.of(src.getValue(state)), flags, true);

        // step 2: add 8 to rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);

        return rsp.updateState(tmp, Optional.of(rsp.getValue(tmp).add(BigInteger.valueOf(8))), flags, false);
    }

    /**
     * Sets dest to 0 or 1 based on the condition associated with this instruction.
     *
     * @param state The state in which to work.
     * @param dest The operand that will be set to 0 or 1 based on the condition.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of 0 or 1 based on the condition.
     */
    private MachineState set(MachineState state, Operand dest) 
            throws x86RuntimeException {
        assert this.conditionCheck.isPresent();
        BigInteger result = this.conditionCheck.get().test(state) ? BigInteger.ONE : BigInteger.ZERO;
        return dest.updateState(state, Optional.of(result), new HashMap<>(), true);
    }

    /**
     * Jumps to a destination if the instruction's condition is true, otherwise
     * falling through to the next instruction after the jump.
     *
     * @param state The state in which to work.
     * @param dest The label we will jump to.
     * @return A clone of {@code state}, but with a rip changed based on the
     * result of the condition.
     */
    private MachineState jump(MachineState state, Operand dest) throws x86RuntimeException {
        assert this.conditionCheck.isPresent();
        Map<String, Boolean> flags = new HashMap<>();
        if (this.conditionCheck.get().test(state)) {
            return dest.updateState(state, Optional.of(dest.getValue(state)), flags, false);
        } else {
            return dest.updateState(state, Optional.empty(), flags, true);
        }
    }

    /**
     * Jumps to a destination, storing the location of the next instruction (i.e.
     * the return address) onto the stack.
     *
     * @param state The state in which to work.
     * @param dest The label we will jump to.
     * @return A clone of {@code state}, but with a rip set to the address of
     * {@code dest} and the memory updated to contain rip+1 at the top of the stack.
     */
    private MachineState call(MachineState state, Operand dest) throws x86RuntimeException {
        Map<String, Boolean> flags = new HashMap<>();

        // step 1: subtract 8 from rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
        MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);

        int rA = tmp.getRipRegister() + 1;
        BigInteger returnAddr = new BigInteger("" + rA);

        // step 2: store return address in (%rsp)
        MemoryOperand rspMemOperand = new MemoryOperand("rsp", null, null, null, this.opSize, "");
        tmp = rspMemOperand.updateState(tmp, Optional.of(returnAddr), flags, false);

        // return new state with rip set to beginning of callee
        MachineState mS = dest.updateState(tmp, Optional.of(dest.getValue(state)), flags, false);
        mS.pushToCallStack();
        return mS;
    }

    @Override
    public MachineState eval(MachineState state) throws x86RuntimeException {
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
                if (this.opSize != OpSize.BYTE) {
                    result.add("rdx");
                }
                break;
        }
        return result;
    }

    @Override
    public void updateLabels(String labelName, x86Label label) {
        destination.updateLabels(labelName, label);
    }

    @Override
    public String toString() {
        String s = lineNum + ": \t" + getInstructionTypeString() + " " + destination.toString();
        if (comment.isPresent()) {
            s += " " + comment.get().toString();
        }
        return s;
    }

    @Override
    public String getDescriptionString() {
        String destDesc = destination.getDescriptionString();
        String setTemplate = "Sets " + destDesc + " to 1 if the result of the last ";
        String jumpTemplate = "Jumps to " + destDesc + " if the result of the last ";
        switch (this.type) {
            case IDIV:
                return "Concatenates %rdx and %rax, divides that by " + destDesc + " and stores the quotient in %rax and the remainder in %rdx.";
            case INC:
                return "Increments " + destDesc + " by 1.";
            case DEC:
                return "Decrements " + destDesc + " by 1.";
            case NEG:
                return "Negates " + destDesc + " (i.e. performs the - operation).";
            case NOT:
                return "Inverts all the bits in " + destDesc + " (i.e. performs the ~ operation).";
            case SETE:
                return setTemplate + "comparison was equal/zero, \notherwise 0 (i.e. operand = ZF)";
            case SETNE:
                return setTemplate + "comparison was not equal/zero, \notherwise 0 (i.e. operand = ~ZF)";
            case SETS:
                return setTemplate + "operation was negative, \notherwise 0 (i.e. operand = SF)";
            case SETNS:
                return setTemplate + "operation was non-negative, \notherwise 0 (i.e. operand = ~SF)";
            case SETG:
                return setTemplate + "signed comparison was greater than, \notherwise 0 (i.e. operand = ~(SF ^ OF) & ~ZF)";
            case SETGE:
                return setTemplate + "signed comparison was greater than or equal, \notherwise 0 (i.e. operand = ~(SF ^ OF))";
            case SETL:
                return setTemplate + "signed comparison was less than, \notherwise 0 (i.e. operand = SF ^ OF)";
            case SETLE:
                return setTemplate + "signed comparison was less than or equal, \notherwise 0 (i.e. operand = (SF ^ OF) | ZF)";
            case SETA:
                return setTemplate + "unsigned comparison was greater than, \notherwise 0 (i.e. operand = ~CF & ~ZF)";
            case SETAE:
                return setTemplate + "unsigned comparison was greater than or equal, \notherwise 0 (i.e. operand = ~CF)";
            case SETB:
                return setTemplate + "unsigned comparison was less than, \notherwise 0 (i.e. operand = CF)";
            case SETBE:
                return setTemplate + "unsigned comparison was less than or equal, \notherwise 0 (i.e. operand = CF | ZF)";
            case JE:
                return jumpTemplate + "comparison was equal, \notherwise moves to next line (i.e. jump if ZF == 1)";
            case JNE:
                return jumpTemplate + "comparison was not equal, \notherwise moves to next line (i.e. jump if ZF == 0)";
            case JS:
                return jumpTemplate + "operation was negative, \notherwise moves to next line (i.e. jump if SF == 1)";
            case JNS:
                return jumpTemplate + "operation was non-negative, \notherwise moves to next line (i.e. jump if SF == 0)";
            case JG:
                return jumpTemplate + "signed comparison was greater than, \notherwise moves to next line (i.e. jump if ~(SF ^ OF) & ~ZF == 1)";
            case JGE:
                return jumpTemplate + "signed comparison was greater than or equal, \notherwise moves to next line (i.e. jump if ~(SF ^ OF) == 1)";
            case JL:
                return jumpTemplate + "signed comparison was less than, \notherwise moves to next line (i.e. jump if SF ^ OF == 1)";
            case JLE:
                return jumpTemplate + "signed comparison was less than or equal, \notherwise moves to next line (i.e. jump if (SF ^ OF) | ZF == 1)";
            case JA:
                return jumpTemplate + "unsigned comparison was greater than, \notherwise moves to next line (i.e. jump if ~CF & ~ZF == 1)";
            case JAE:
                return jumpTemplate + "unsigned comparison was greater than or equal, \notherwise moves to next line (i.e. jump if CF == 0)";
            case JB:
                return jumpTemplate + "unsigned comparison was less than, \notherwise moves to next line (i.e. jump if CF == 1)";
            case JBE:
                return jumpTemplate + "unsigned comparison was less than or equal, \notherwise moves to next line (i.e. jump if CF | ZF == 1)";
            case JMP:
                return "Jumps to " + destDesc;
            case PUSH:
                return "Decrements %rsp by 8 and stores " + destDesc + " at the updated value for %rsp.";
            case POP:
                return "Stores the value at the top of the stack into " + destDesc + " and increments %rsp by 8.";
            case CALL:
                return "Pushes the return address (i.e. the address of the instruction after this call) \nonto the stack and jumps to " + destDesc;
            default:
                throw new RuntimeException("unsupported instr type: " + this.type);
        }
    }
}
