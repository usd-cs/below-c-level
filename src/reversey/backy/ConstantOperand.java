package reversey.backy;

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

    public ConstantOperand(long val, OpSize size, int base) {
        super(size);
        this.constant = val;
        assert(base == 10 || base == 16);
        this.base = base;
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
        String s = "$";
        if (base == 16) s += "0x";
        s += Long.toString(constant, base).toUpperCase();
        s = s.replace("0x-", "-0x");
        return s;
    }
}

