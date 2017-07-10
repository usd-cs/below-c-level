package reversey.backy;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.Arrays;
import java.math.BigInteger;

/**
 * An abstract class representing an x86-64 instruction.
 *
 * @author Dr. Sat
 */
public abstract class x86Instruction extends x86ProgramLine {

    /**
     * The operand where the instruction will write its results.
     */
    protected Operand destination;

    /**
     * The type of instruction (e.g. add)
     */
    protected InstructionType type;

    /**
     * The number of bytes the operation works on.
     */
    protected OpSize opSize;

    /**
     * A Map from condition code (e.g. "g") to a function that determines whether
     * that condition is true.
     */
    protected static final Map<String, Predicate<MachineState>> conditions;

    static {
        conditions = new HashMap<String, Predicate<MachineState>>();
        conditions.put("e", state -> state.getZeroFlag());
        conditions.put("jmp", state -> true);
        conditions.put("ne", state -> !state.getZeroFlag());
        conditions.put("s", state -> state.getSignFlag());
        conditions.put("ns", state -> !state.getSignFlag());
        conditions.put("g", 
                state -> !(state.getSignFlag() ^ state.getOverflowFlag()) & !state.getZeroFlag());
        conditions.put("ge", 
                state -> !(state.getSignFlag() ^ state.getOverflowFlag()));
        conditions.put("l", 
                state -> (state.getSignFlag() ^ state.getOverflowFlag()));
        conditions.put("le", 
                state -> (state.getSignFlag() ^ state.getOverflowFlag()) | state.getZeroFlag());
    }

    /**
     * Sets the sf and zf flags based on the given value.
     *
     * @param val The value used to determine the sf and zf flags.
     * @param flags Set of flags to update.
     */
    public static void setSignAndZeroFlags(BigInteger val, Map<String, Boolean> flags) {
        int signum = val.signum();
        flags.put("zf", signum == 0);
        flags.put("sf", signum == -1);
    }

    // Getters
    public InstructionType getType() { return this.type; }
    public OpSize getOpSize() { return this.opSize; }

        public abstract void updateLabels(String labelName, x86Label label);
    public abstract String toString();

    /**
     * @return A string that represents the instruction, including its size
     * suffix (when appropriate).
     */
    public String getInstructionTypeString() {
        String s = this.type.name().toLowerCase();
        if (!this.type.name().startsWith("SET")
            && !this.type.name().startsWith("J")) {
            switch (this.opSize) {
                case QUAD:
                    s += "q";
                    break;
                case LONG:
                    s += "l";
                    break;
                case WORD:
                    s += "w";
                    break;
                default:
                    s += "b";
                    break;
            }
        }

        return s;
    }

    /**
     * Truncates the integral value to fit into the size of this instruction.
     *
     * @param val The value to (possibly) truncate.
     * @return The truncated version of val, or val if truncation was not
     * needed.
     */
    protected BigInteger truncate(BigInteger val) {
        byte[] resArray = val.toByteArray();
        if (resArray.length > this.opSize.numBytes()) {
            byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
            return new BigInteger(ba);
        }
        else {
            return val;
        }
    }
}
