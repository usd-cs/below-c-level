package reversey.backy;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/**
 * A class representing an x86-64 memory operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class MemoryOperand extends Operand {

    /**
     * Name of the base register.
     */
    private final String baseReg;

    /**
     * Name of the index register.
     */
    private final String indexReg;

    /**
     * The scaling factor for the index register.
     */
    private final int scale;

    /**
     * The offset amount.
     */
    private final int offset;

    /**
     * The size of the operand.
     */
    private final OpSize opSize;

    public MemoryOperand(String baseReg, String indexReg, int scale, int offset, OpSize opSize) {
        this.baseReg = baseReg;
        this.indexReg = indexReg;
        this.scale = scale;
        this.offset = offset;
        this.opSize = opSize;
    }

    /**
     * Calculate the effective address of the operand, given the specified
     * machine state.
     *
     * @param state The state in which to calculate the address.
     * @return The effective address.
     */
    public long calculateAddress(MachineState state) {
        /**
         * @tricky should this return BigInteger
         */
        long address = state.getRegisterValue(baseReg).add(BigInteger.valueOf(offset)).longValue();
        if (indexReg != null) {
            address += state.getRegisterValue(indexReg).multiply(BigInteger.valueOf(scale)).longValue();
        }

        return address;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        return state.getMemoryValue(calculateAddress(state), opSize.numBytes());
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP) {
        return currState.getNewState(calculateAddress(currState), val, opSize.numBytes(), flags, updateRIP);
    }
    
    @Override
    public Set<String> getUsedRegisters() {
        HashSet<String> s = new HashSet<>();
        if (baseReg != null) // TODO: OPTIONAL!!!
            s.add(baseReg);
        if (indexReg != null)
            s.add(indexReg);
        return s;
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
