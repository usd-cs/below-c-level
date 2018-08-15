package edu.sandiego.bcl;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/**
 * A class representing an x86-64 constant (i.e. immediate) operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class ConstantOperand extends Operand {

    /**
     * The operand's value.
     */
    private final long constant;
    
    /**
     * The base representation that was entered by the user (e.g. 16 = hex)
     */
    private final int base;
    
    /**
     * The original string representation (entered by the user)
     */
    private final String stringRep;

    public ConstantOperand(long val, OpSize size, int base, String stringRep) {
        super(size);
        
        assert size != OpSize.INFERRED;
        assert(base == 10 || base == 16);

        this.constant = val;
        this.base = base;
        this.stringRep = stringRep;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        return BigInteger.valueOf(constant);
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP) {
        throw new RuntimeException("Update of constant operand.");
    }
    
    @Override
    public Set<String> getUsedRegisters() {
        HashSet<String> s = new HashSet<>();
        return s;
    }

    @Override
    public String toString() {
        return "$" + stringRep;
    }

    @Override
    public String getDescriptionString() {
        //return "" + constant;
        return stringRep;
    }
    
    /**
     * Checks whether a given constant fits within a specified size.
     * 
     * @param size The size to check the fit against.
     * @param constStr A string representing the constant.
     * @param base The number base of the constant (e.g. 10 for decimal) given 
     * as a string.
     * @return True if the constant fits in the given size, false otherwise.
     */
    public static boolean fitsInSize(OpSize size, String constStr, int base) {
        assert !constStr.startsWith("0x");
        
        BigInteger val = new BigInteger(constStr, base);

        // check that the constant is within the operand size limit
        int valSize = base == 10 ? val.bitLength() + 1 : constStr.length() * 4;

        return valSize <= size.numBits();
    }
}

