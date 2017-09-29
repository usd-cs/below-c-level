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
    private final Optional<String> baseReg;

    /**
     * Name of the index register.
     */
    private final Optional<String> indexReg;

    /**
     * The scaling factor for the index register.
     */
    private final Optional<Integer> scale;

    /**
     * The offset amount.
     */
    private final Optional<Integer> offset;
    
    /**
     * Original string representation of the offset (as entered by the user
     */
    private final String offsetStr;

    public MemoryOperand(String baseReg, String indexReg, Integer scale, Integer offset, OpSize opSize, String offsetStr) {
        super(opSize);
        this.baseReg = Optional.ofNullable(baseReg);
        this.indexReg = Optional.ofNullable(indexReg);
        this.scale = Optional.ofNullable(scale);
        this.offset = Optional.ofNullable(offset);
        this.offsetStr = offsetStr;
    }

    /**
     * Calculate the effective address of the operand, given the specified
     * machine state.
     *
     * @param state The state in which to calculate the address.
     * @return The effective address.
     */
    public long calculateAddress(MachineState state) {
        long address = offset.isPresent() ? offset.get() : 0;
        long scaleFactor = scale.isPresent() ? scale.get() : 1;
        if (baseReg.isPresent()) 
            address = state.getRegisterValue(baseReg.get()).add(BigInteger.valueOf(address)).longValue();
        if (indexReg.isPresent()) {
            BigInteger indexRegVal = state.getRegisterValue(indexReg.get());
            address += indexRegVal.multiply(BigInteger.valueOf(scaleFactor)).longValue();
        }

        return address;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        return state.getMemoryValue(calculateAddress(state), opSize.numBytes());
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP) {
        return currState.cloneWithUpdatedMemory(calculateAddress(currState), val, opSize.numBytes(), flags, updateRIP);
    }
    
    @Override
    public Set<String> getUsedRegisters() {
        HashSet<String> s = new HashSet<>();
        if (baseReg.isPresent())
            s.add(baseReg.get());
        if (indexReg.isPresent())
            s.add(indexReg.get());
        return s;
    }

    @Override
    public String toString() {
        String res = offsetStr;
        if (baseReg.isPresent() || indexReg.isPresent()) {
            res += "(";
            
            if (baseReg.isPresent())
                res += "%" + baseReg.get();
            if (indexReg.isPresent()) {
                res += ",%" + indexReg.get();
                if (scale.isPresent())
                    res += "," + scale.get();
            }

            res += ")";
        }
        return res;
    }

    @Override
    public String getDescriptionString() {
        String s = "memory at address (";
        if (baseReg.isPresent()){
            s += "%" + baseReg.get();
        } 
        if (indexReg.isPresent()){
            s += " + %" + indexReg.get();
            if(scale.isPresent()){
                s += " * " + scale.get();
            }
        }
        if (offset.isPresent()) {
            s += (" + " + offsetStr);
        }
        s += ")";
        return s;
    }
}
