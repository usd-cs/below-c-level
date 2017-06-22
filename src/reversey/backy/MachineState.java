package reversey.backy;

import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.regex.Pattern;
import javafx.util.Pair;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * A class representing the state of the machine, namely its register file and
 * memory.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class MachineState {

    /**
     * The register file.
     */
    private Map<String, byte[]> registers;

    /**
     * The machine's memory.
     */
    private Map<Long, Byte> memory;

    /**
     * The status flags (i.e. condition codes).
     */
    private Map<String, Boolean> statusFlags;

    /**
     * Create a new state with all registers (except %rsp) initialized to 0 but
     * no memory initialization. %rsp is initialized to 0x7FFFFFFF.
     */
    public MachineState() {
        this.registers = new HashMap<String, byte[]>();
        this.memory = new HashMap<Long, Byte>();
        this.statusFlags = new HashMap<String, Boolean>();

        String[] regNames = {"rax", "rbx", "rcx", "rdx", "rsi", "rdi", "rbp", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"};
        for (String s : regNames) {
            registers.put(s, new byte[8]);
        }

        long initRSP = 1 << 30;
        initRSP <<= 30;
        initRSP <<= 3;
        initRSP = ~initRSP;
        registers.put("rsp", ByteBuffer.allocate(8).putLong(initRSP).array()); // rsp = 0x7FFFFFFFFFFFFFFF

        String[] flagNames = {"zf", "sf", "of", "cf"};
        for (String s : flagNames) {
            statusFlags.put(s, false);
        }
    }

    public MachineState(Map<String, byte[]> reg, Map<Long, Byte> mem, Map<String, Boolean> flags) {
        this.registers = reg;
        this.memory = mem;
        this.statusFlags = flags;
    }

    // Getters for the status flags
    public boolean getCarryFlag() {
        return this.statusFlags.get("cf");
    }

    public boolean getOverflowFlag() {
        return this.statusFlags.get("of");
    }

    public boolean getZeroFlag() {
        return this.statusFlags.get("zf");
    }

    public boolean getSignFlag() {
        return this.statusFlags.get("sf");
    }

    /**
     * Create a new MachineState based on the current state but with an updated
     * value for a memory address.
     *
     * @param address The starting (i.e. lowest) address that will be changed.
     * @param val The new value of the given memory address.
     * @param size The number of bytes to write to memory.
     * @param flags The condition flags to modify for the new state.
     * @return A new state that is the same as the current but with new binding
     * from given address to given val.
     */
    public MachineState getNewState(long address, Optional<BigInteger> val, int size, Map<String, Boolean> flags) {
        Map<Long, Byte> mem = this.memory;

        if (val.isPresent()) {
            mem = new HashMap<Long, Byte>(this.memory);
            byte[] valArray = val.get().toByteArray();

            long dest = address + (valArray.length - 1);
            for (int src = 0;
                    src < valArray.length; src++, dest++) {
                mem.put(dest, valArray[src]);
            }

            for (long i = address + valArray.length; i < (address + size); i++) {
                if (val.get().signum() == -1) {
                    mem.put(i, (byte) 0xFF);
                } else {
                    mem.put(i, (byte) 0);
                }
            }
        }

        // TODO: remove code duplication (here and in other version of
        // getNewState.
        if (!flags.containsKey("zf")) {
            flags.put("zf", this.statusFlags.get("zf"));
        }
        if (!flags.containsKey("sf")) {
            flags.put("sf", this.statusFlags.get("sf"));
        }
        if (!flags.containsKey("of")) {
            flags.put("of", this.statusFlags.get("of"));
        }
        if (!flags.containsKey("cf")) {
            flags.put("cf", this.statusFlags.get("cf"));
        }

        return new MachineState(this.registers, mem, flags);
    }

    /**
     * @return Pair of the start (inclusive) and end (exclusive) indices for
     * given register in its full register's byte array.
     */
    private static Pair<Integer, Integer> getByteRange(String regName) {
        String quadRegNames = "^r(ax|bx|cx|dx|si|di|bp|sp|8|9|10|11|12|13|14|15)$";
        String longRegNames = "^(e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)d)$";
        String wordRegNames = "^((ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)w)$";
        String byteLowRegNames = "^((al|bl|cl|dl|sil|dil|bpl|spl)|r(8|9|10|11|12|13|14|15)b)$";
        String byteHighRegNames = "^(ah|bh|ch|dh)$";
        if (Pattern.matches(quadRegNames, regName)) {
            return new Pair<Integer, Integer>(0, 8);
        } else if (Pattern.matches(longRegNames, regName)) {
            return new Pair<Integer, Integer>(4, 8);
        } else if (Pattern.matches(wordRegNames, regName)) {
            return new Pair<Integer, Integer>(6, 8);
        } else if (Pattern.matches(byteLowRegNames, regName)) {
            return new Pair<Integer, Integer>(7, 8);
        } else if (Pattern.matches(byteHighRegNames, regName)) {
            return new Pair<Integer, Integer>(6, 7);
        } else {
            System.err.println("ERROR: Unknown register name: " + regName);
            System.exit(1);
            return null;
        }
    }

    public static String getQuadName(String regName) {
        String quadRegNames = "^r(ax|bx|cx|dx|si|di|bp|sp|8|9|10|11|12|13|14|15)$";
        String longRegNames = "^(e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)d)$";
        String wordRegNames = "^((ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)w)$";
        String byteLowRegNames = "^((al|bl|cl|dl|sil|dil|bpl|spl)|r(8|9|10|11|12|13|14|15)b)$";
        String byteHighRegNames = "^(ah|bh|ch|dh)$";
        if (Pattern.matches(quadRegNames, regName)) {
            return regName;
        } else if (Pattern.matches(longRegNames, regName)) {
            if (regName.charAt(0) == 'e') {
                return "r" + regName.substring(1);
            } else {
                return regName.substring(0, regName.length() - 1);
            }
        } else if (Pattern.matches(wordRegNames, regName)) {
            if (regName.charAt(0) != 'r') {
                return "r" + regName;
            } else // just strip off the "d" from the end
            {
                return regName.substring(0, regName.length() - 1);
            }
        } else if (Pattern.matches(byteLowRegNames, regName)) {
            if (regName.charAt(0) == 'r') {
                return regName.substring(0, regName.length() - 1);
            } else if (regName.length() == 2) {
                return "r" + regName.substring(0, regName.length() - 1) + "x";
            } else {
                return "r" + regName.substring(0, regName.length() - 1);
            }
        } else if (Pattern.matches(byteHighRegNames, regName)) {
            return "r" + regName.substring(0, regName.length() - 1) + "x";
        } else {
            System.err.println("ERROR: Unknown register name: " + regName);
            System.exit(1);
            return null;
        }
    }

    /**
     * Create a new MachineState based on the current state but with an updated
     * value for a register.
     *
     * @param regName The register that will be updated.
     * @param val The new value of the given register.
     * @param flags The condition flags to modify for the new state.
     * @return A new state that is the same as the current but with new binding
     * from given register to given val
     */
    public MachineState getNewState(String regName, Optional<BigInteger> val, Map<String, Boolean> flags) {
        Map<String, byte[]> reg = this.registers;
        if (val.isPresent()) {
            String quadName = getQuadName(regName);
            Pair<Integer, Integer> range = getByteRange(regName);
            int startIndex = range.getKey();
            int endIndex = range.getValue();

            reg = new HashMap<String, byte[]>(this.registers);
            byte[] valArray = val.get().toByteArray();
            byte[] newVal = new byte[endIndex - startIndex];

            for (int src = 0, dest = (newVal.length - valArray.length);
                    src < valArray.length; src++, dest++) {
                newVal[dest] = valArray[src];
            }

            if (val.get().signum() == -1) {
                for (int i = 0; i < newVal.length - valArray.length; i++) {
                    newVal[i] = (byte) 0xFF;
                }
            }

            byte[] newValFull = Arrays.copyOf(this.registers.get(quadName), 8);
            for (int src = 0, dest = startIndex; dest < endIndex; src++, dest++) {
                newValFull[dest] = newVal[src];
            }

            reg.put(quadName, newValFull);
        }

        // TODO: remove code duplication (here and in other version of
        // getNewState.
        if (!flags.containsKey("zf")) {
            flags.put("zf", this.statusFlags.get("zf"));
        }
        if (!flags.containsKey("sf")) {
            flags.put("sf", this.statusFlags.get("sf"));
        }
        if (!flags.containsKey("of")) {
            flags.put("of", this.statusFlags.get("of"));
        }
        if (!flags.containsKey("cf")) {
            flags.put("cf", this.statusFlags.get("cf"));
        }

        return new MachineState(reg, this.memory, flags);
    }

    /**
     * Gets the value stored in the given register.
     */
    public BigInteger getRegisterValue(String regName) {
        byte[] ba = null;

        String quadName = getQuadName(regName);
        Pair<Integer, Integer> range = getByteRange(regName);
        int startIndex = range.getKey();
        int endIndex = range.getValue();

        ba = registers.get(quadName);

        /*
		String longRegNames = "^(e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)d)$";
		String quadRegNames = "^r(ax|bx|cx|dx|si|di|bp|sp|8|9|10|11|12|13|14|15)$";
		String wordRegNames = "^((ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)w)$";
		String byteRegNames = "^((al|ah|bl|bh|cl|ch|dl|dh|sil|dil|bpl|spl)|r(8|9|10|11|12|13|14|15)b)$";
		if (Pattern.matches(quadRegNames, regName)) {
			ba = registers.get(regName);
		}
		else if (Pattern.matches(longRegNames, regName)) {
			startIndex = 4;

			if (regName.charAt(0) == 'e')
				ba = registers.get("r" + regName.substring(1));
			else
				ba = registers.get(regName.substring(0, regName.length()-1));
		}
		else {
			System.err.println("ERROR: WORD AND BYTE registers not yet supported.");
			System.exit(1);
		}
         */
        return new BigInteger(Arrays.copyOfRange(ba, startIndex, endIndex));
        //return new BigInteger(registers.get(regName));
    }

    /**
     * Gets the value stored at the given memory address.
     *
     * @param address The starting address where the value is stored.
     * @param size The number of bytes of memory to read.
     */
    public BigInteger getMemoryValue(long address, int size) {
        byte[] val = new byte[size];

        long addr = address + (size - 1);
        for (int dest = 0; dest < size; addr--, dest++) {
            val[dest] = memory.get(addr);
        }

        return new BigInteger(val);
    }

    /**
	 * Returns a list of all registers.
	 *
	 * @param regHistory Ordered list containing a history of register usage.
	 * @return List of Register objects for all of the registers in this state.
     */
    public List<Register> getRegisters(List<String> regHistory) {
        ArrayList<Register> arr = new ArrayList<Register>();
        for (Map.Entry<String, byte[]> entry : registers.entrySet()) {
            BigInteger b = new BigInteger(entry.getValue());
            byte[] ba = b.toByteArray();
            String s = "0x";
            for (byte i : ba) {
                s += String.format("%02x", i);
            }
            int regHist = regHistory.lastIndexOf(entry.getKey());
            arr.add(new Register(entry.getKey(), s, regHist));
           //System.out.println(entry.getKey() + "/" + entry.getValue());
        }
        return arr;
    }

    public String toString() {
        String s = "Registers:\n";
        for (Map.Entry<String, byte[]> entry : registers.entrySet()) {
            BigInteger b = new BigInteger(entry.getValue());
            byte[] ba = b.toByteArray();
            s += "\t" + entry.getKey() + ": " + b.toString() + " (0x";
            for (byte i : ba) {
                s += String.format("%02x", i);
            }
            s += ")\n";
        }

        s += "Status Flags:\n";
        for (Map.Entry<String, Boolean> entry : statusFlags.entrySet()) {
            s += "\t" + entry.getKey() + ": " + (entry.getValue() ? "1" : "0") + "\n";
        }

        s += "Memory:\n";
        for (Map.Entry<Long, Byte> entry : memory.entrySet()) {
            s += "\t" + Long.toHexString(entry.getKey()) + ": " + String.format("%02x", entry.getValue()) + "\n";
        }

        return s;
    }
}
