package reversey.backy;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An abstract class representing an x86 operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public abstract class Operand {

    /**
     * @param state The state of the machine.
     * @return The value of the operand in a machine with the given state.
     */
    public abstract BigInteger getValue(MachineState state);

    /**
     * @param currState The current state of the machine.
     * @param val The value to update the operand with.
     * @param flags The condition flags to be set in the new state.
     * @return The state after updating the current state with the new value for
     * the operand.
     */
    public abstract MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP);

    /**
     * Returns the names of the registers used by this operand.
     *
     * @return Set containing names of registers used by this operand.
     */
    public abstract Set<String> getUsedRegisters();
    
    public void updateLabels(String labelName, Label label){}
}

