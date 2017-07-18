package reversey.backy;

import java.math.BigInteger;
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
     * The operand where the instruction will write its results.
     */
    protected Operand destination;
    
    /**
     * An operand used solely as a source for the operation.
     */
    private Operand source;

    /**
     * The function performed by this instruction.
     */
    private BinaryX86Operation operation;

    /**
     * @param instType The type of operation performed by the instruction.
     * @param srcOp A source operand of the instruction.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     * @param line The line number associated with this instruction.
     */
    public x86BinaryInstruction(InstructionType instType, Operand srcOp, Operand destOp, OpSize size, int line, x86Comment c) {
        this.type = instType;
        this.source = srcOp;
        this.destination = destOp;
        this.opSize = size;
        this.lineNum = line;
        this.comment = Optional.ofNullable(c);
        
        switch (instType) {
            case ADD:
                this.operation = this::add;
                break;
            case SUB:
                this.operation = this::sub;
                break;
            case IMUL:
                this.operation = this::imul;
                break;
            case CMP:
                this.operation = this::cmp;
                break;
            case XOR:
                this.operation = this::xor;
                break;
            case OR:
                this.operation = this::or;
                break;
            case AND:
                this.operation = this::and;
                break;
            case TEST:
                this.operation = this::test;
                break;
            case SAL:
                this.operation = this::sal;
                break;
            case SHL:
                this.operation = this::sal;
                break;
            case SAR:
                this.operation = this::sar;
                break;
            case SHR:
                this.operation = this::shr;
                break;
            case MOV:
                this.operation = this::mov;
                break;
            case LEA:
                this.operation = this::lea;
                break;
            default:
                throw new RuntimeException("unsupported instr type: " + instType);
        }
    }

    /**
     * Perform the operation dest += src.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to add to {@code dest}.
     * @param dest The operand that will be added to and then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest-src)}.
     */
    public MachineState add(MachineState state, Operand src, Operand dest) {
        BigInteger src1 = dest.getValue(state);
        BigInteger src2 = src.getValue(state);
        BigInteger result = src1.add(src2);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

        result = truncate(result);

        setSignAndZeroFlags(result, flags);
        
        // If src1 and src2 are both negative, their msbs will both be 1, which
        // will always generate a carry out.
        if (src1.signum() == -1 && src2.signum() == -1)
            flags.put("cf", true);
        
        // If one src is negative and the other is non-negative, we can look
        // at the sign of the result to determine whether there is a carry out
        // or not.
        else if ((src1.signum() == -1 || src2.signum() == -1)
                && src1.signum() != src2.signum()
                && result.signum() != -1)
            flags.put("cf", true);
        else
            flags.put("cf", false);
        
        return dest.updateState(state, Optional.of(result), flags, true);
    }
    
    /**
     * Calculates the value for the carry flag (CF) of the operation a - b.
     * 
     * @param a Value being subtracted from.
     * @param b The value to subtract.
     * 
     * @return {@code true} if a-b causes CF to be set, {@code false} otherwise. 
     */
    private boolean calculateCarryForSub(BigInteger a, BigInteger b) {
        // cf set when both numbers have the same sign and a is less than
        // b (i.e. subtracing larger value from smaller value)
        if (((a.signum() >= 0 && b.signum() >= 0)
                || (a.signum() == -1 && b.signum() == -1))
              && a.compareTo(b) == -1) {
            return true;
        }
        
        // also possible to get a cf when b is negative (i.e. has msb of 1)
        // while a is non-negative (i.e. msb is 0)
        else if (a.signum() >= 0 && b.signum() == -1)
            return true;
        else
            return false;
    }
    
    /**
     * Perform dest - src, storing the result back in dest only when specified.
     * 
     * @param state The state in which to work.
     * @param src The value being subtracted.
     * @param dest The value being subtracted from.
     * @param updateDest Whether to update the destination with the result.
     * @return A clone of {@code state}, but with an incremented rip and,
     * if specified, the destination updated with the value of {@code (dest-src)}.
     */
    public MachineState subtract(MachineState state, Operand src, Operand dest, 
                                    boolean updateDest) {
        BigInteger src1 = dest.getValue(state);
        BigInteger src2 = src.getValue(state);
        BigInteger result = src1.subtract(src2);
        result = truncate(result);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());
        flags.put("cf", calculateCarryForSub(src1, src2));
        setSignAndZeroFlags(result, flags);
        
        if (updateDest)
            return dest.updateState(state, Optional.of(result), flags, true);
        else
            return dest.updateState(state, Optional.empty(), flags, true);
    }

    /**
     * Perform the operation dest -= src.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to subtract from {@code dest}.
     * @param dest The operand that will be subtracted from then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest-src)}.
     */
    public MachineState sub(MachineState state, Operand src, Operand dest) {
        return subtract(state, src, dest, true);
    }
 
    /**
     * Perform the operation dest - src, without updating dest.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to subtract from {@code dest}.
     * @param dest The operand that will be subtracted from.
     * @return A clone of {@code state}, but with an incremented rip and status
     * flags set accordingly.
     */
    public MachineState cmp(MachineState state, Operand src, Operand dest) {
        return subtract(state, src, dest, false);
    }
    
    /**
     * Perform the operation dest *= src.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to multiply by.
     * @param dest The operand that will be multiplied from then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest*src)}.
     */
    public MachineState imul(MachineState state, Operand src, Operand dest) {
        BigInteger src1 = dest.getValue(state);
        BigInteger src2 = src.getValue(state);
        BigInteger result = src1.multiply(src2);

        Map<String, Boolean> flags = new HashMap<>();
        flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());
        flags.put("cf", flags.get("of")); // CF is always the same as OF for imul

        result = truncate(result);

        setSignAndZeroFlags(result, flags);
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Gets set of condition flags for a logical operation, based on the result
     * being val.
     *
     * @param val The result used for setting zf and sf.
     * @return Set of condition flags.
     */
    public Map<String, Boolean> getLogicalOpFlags(BigInteger val) {

        Map<String, Boolean> flags = new HashMap<>();
        setSignAndZeroFlags(val, flags);
        flags.put("of", false);
        flags.put("cf", false);

        return flags;
    }

    /**
     * Perform the operation dest ^= src.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to xor by.
     * @param dest The operand that will be xor'ed then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest ^ src)}.
     */
    public MachineState xor(MachineState state, Operand src, Operand dest) {
        BigInteger result = dest.getValue(state).xor(src.getValue(state));
        Map<String, Boolean> flags = getLogicalOpFlags(result);
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Perform the operation dest |= src.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to or by.
     * @param dest The operand that will be or'ed then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest | src)}.
     */
    public MachineState or(MachineState state, Operand src, Operand dest) {
        BigInteger result = dest.getValue(state).or(src.getValue(state));
        Map<String, Boolean> flags = getLogicalOpFlags(result);
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Perform the operation dest &= src.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to and by.
     * @param dest The operand that will be and'ed then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest & src)}.
     */
    public MachineState and(MachineState state, Operand src, Operand dest) {
        BigInteger result = dest.getValue(state).and(src.getValue(state));
        Map<String, Boolean> flags = getLogicalOpFlags(result);
        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Perform the operation dest & src but without updating dest.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to and by.
     * @param dest The operand that will be and'ed.
     * @return A clone of {@code state}, but with an incremented rip.
     */
    public MachineState test(MachineState state, Operand src, Operand dest) {
        BigInteger result = dest.getValue(state).and(src.getValue(state));
        Map<String, Boolean> flags = getLogicalOpFlags(result);
        return dest.updateState(state, Optional.empty(), flags, true);
    }

    /**
     * Perform the operation {@code dest <<= src}.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to shift by.
     * @param dest The operand that will be shifted left then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest << src)}.
     */
    public MachineState sal(MachineState state, Operand src, Operand dest) {
        // FIXME: max shift amount should based on size of dest
        int shamt = src.getValue(state).intValue() % 64; // max shift amount is 63
        BigInteger orig = dest.getValue(state);
        BigInteger result = orig.shiftLeft(shamt);

        int msbIndex = this.opSize.numBits() - 1;

        Map<String, Boolean> flags = new HashMap<>();
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
        
        result = truncate(result);

        return dest.updateState(state, Optional.of(result), flags, true);
    }

    /**
     * Perform the operation {@code dest >>= src} (i.e. arithmetic shift right).
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to shift by.
     * @param dest The operand that will be shifted right then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest >> src)}.
     */
    public MachineState sar(MachineState state, Operand src, Operand dest) {
        // FIXME: max shift amount should based on size of dest
        int shamt = src.getValue(state).intValue() % 64; // max shift amount is 63
        BigInteger orig = dest.getValue(state);
        BigInteger result = orig.shiftRight(shamt);

        assert result.bitLength() + 1 > this.opSize.numBits();

        Map<String, Boolean> flags = new HashMap<>();
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

    /**
     * Perform the operation {@code dest >>>= src} (i.e. logical shift right).
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to shift by.
     * @param dest The operand that will be shifted right then updated.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} updated with the value of {@code (dest >> src)}.
     */
    public MachineState shr(MachineState state, Operand src, Operand dest) {
        // FIXME: max shift amount should based on size of dest
        int shamt = src.getValue(state).intValue() % 64; // max shift amount is 63
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


        Map<String, Boolean> flags = new HashMap<>();
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

    /**
     * Perform the operation {@code dest = src}.
     * 
     * @param state The state in which to work.
     * @param src The operand specifying the value to assign to {@code dest}.
     * @param dest The operand that will be assigned to.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} assigned the value of {@code src}.
     */
    public MachineState mov(MachineState state, Operand src, Operand dest) {
        return dest.updateState(state, Optional.of(src.getValue(state)), new HashMap<>(), true);
    }

    /**
     * Perform the operation {@code dest = &src}.
     * 
     * @param state The state in which to work.
     * @param src The memory operand specifying the address to assign to {@code dest}.
     * @param dest The operand that will be assigned to.
     * @return A clone of {@code state}, but with an incremented rip and
     * {@code dest} assigned the value of {@code &src} (i.e. the address pointed
     * to by {@code src}).
     */
    public MachineState lea(MachineState state, Operand src, Operand dest) {
        // TODO: Use polymorophism to avoid this instanceof junk
        if (!(src instanceof MemoryOperand)) {
            // FIXME: parse should catch this!
            System.err.println("ERROR: lea src must be a memory operand");
            return null;
        }

        MemoryOperand mo = (MemoryOperand) src;
        return dest.updateState(state, Optional.of(BigInteger.valueOf(mo.calculateAddress(state))), new HashMap<>(), true);
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
    public void updateLabels(String labelName, x86Label label){
        destination.updateLabels(labelName, label);
        source.updateLabels(labelName, label);
    }
    
    @Override
    public String toString() {
        String s = lineNum + ": \t" + getInstructionTypeString() + " " + source.toString() + ", " + destination.toString();
        if(comment.isPresent()){
            s += comment.get().toString();
        }
        return s;
    }
}
