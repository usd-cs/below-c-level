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
    
    /**
     * The original string representation (entered by the user)
     */
    private final String stringRep;

    public ConstantOperand(long val, OpSize size, int base, String stringRep) {
        super(size);
        this.constant = val;
        assert(base == 10 || base == 16);
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
}

