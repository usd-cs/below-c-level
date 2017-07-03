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
    private long constant;

    public ConstantOperand(long val) {
        this.constant = val;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        return BigInteger.valueOf(constant);
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP) {
        System.err.println("Why are you trying to set a constant?");
        // TODO: exception here?
        return currState;
    }
    
    @Override
    public Set<String> getUsedRegisters() {
        HashSet<String> s = new HashSet<String>();
        return s;
    }

    @Override
    public String toString() {
        return "$" + constant;
    }
}

