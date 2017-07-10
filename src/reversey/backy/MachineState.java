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
    private Map<String, RegisterState> registers;

    /**
     * The machine's memory.
     */
    private List<StackEntry> memory;
    
    /**
     * The status flags (i.e. condition codes).
     */
    private Map<String, Boolean> statusFlags;

    /**
     * Create a new state with all registers (except %rsp) initialized to 0 but
     * no memory initialization. %rsp is initialized to 0x7FFFFFFF.
     */
    public MachineState() {
        this.registers = new HashMap<String, RegisterState>();
        this.memory = new ArrayList<StackEntry>();
        this.statusFlags = new HashMap<String, Boolean>();

        String[] regNames = {"rax", "rbx", "rcx", "rdx", "rsi", "rdi", "rbp", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15", "rip"};
        for (String s : regNames) {
            registers.put(s, new RegisterState(new byte [8], -1));
        }

        long initRSP = 1 << 30;
        initRSP <<= 30;
        initRSP <<= 3;
        initRSP = ~initRSP;
        registers.put("rsp", new RegisterState(ByteBuffer.allocate(8).putLong(initRSP).array(), -1)); // rsp = 0x7FFFFFFFFFFFFFFF

        String[] flagNames = {"zf", "sf", "of", "cf"};
        for (String s : flagNames) {
            statusFlags.put(s, false);
        }
    }

    public MachineState(Map<String, RegisterState> reg, List<StackEntry> mem, Map<String, Boolean> flags) {
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
    public MachineState getNewState(long address, Optional<BigInteger> val, int size, Map<String, Boolean> flags, boolean updateRIP) {
        List<StackEntry> mem = this.memory;
        Map<String, RegisterState> reg = this.registers;

        if (val.isPresent()) {
            mem = new ArrayList<StackEntry>(this.memory);
            reg = new HashMap<String, RegisterState>(this.registers);
            byte[] valArray = val.get().toByteArray();
            byte[] finalArray = new byte[size];
            int numToFill = size - valArray.length;
            byte toFill = 0;
            
                if(val.get().signum() == -1){
                  toFill = (byte) 0xFF;  
                }
                
                for(int i = 0; i < numToFill; i++){
                    finalArray[i] = toFill;
                }

                for(int dest = numToFill, src = 0; dest < size; dest++, src++){
                    finalArray[dest] = valArray[src];
                }
                      
                StackEntry entry = new StackEntry(address, address + size - 1, finalArray, (new BigInteger(reg.get("rip").getValue())).intValue());
                mem.add(entry);
                
                if (updateRIP) {
                    BigInteger ripVal = (new BigInteger(reg.get("rip").getValue())).add(BigInteger.ONE);
                    reg.put("rip", new RegisterState(ripVal.toByteArray(), ripVal.intValue()));
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

        return new MachineState(reg, mem, flags);
    }

    /**
     * Determines which parts of the full 8-byte register will be used by the
     * given register.
     *
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

    /**
     * Determine the name of the 8-byte register used by the given register
     * name.
     * For example: "eax", "ax", "ah", and "al" are all part of the "rax"
     * register.
     *
     * @param regName The name of the register to find.
     * @return Name of the 8-byte register that the given register was part of.
     */
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
     * Creates a new MachineState that is the same as the calling object but
     * with the rip register incremented by 1.
     *
     * @return A new MachineState that is identical to the calling object except
     * for the incremented rip register.
     */
    public MachineState getNewState(){
            Map<String, RegisterState> reg = this.registers;
            BigInteger ripVal = (new BigInteger(reg.get("rip").getValue())).add(BigInteger.ONE);
            reg.put("rip", new RegisterState(ripVal.toByteArray(), ripVal.intValue()));
            return new MachineState(reg, this.memory, this.statusFlags);
    }
    
    /**
     * Creates a new MachineState that is the same as the calling object but
     * with the rip register set to the given value.
     *
     * @param lineNum The value used by the rip register in the new state.
     * @return A new MachineState that is identical to the calling object except
     * for updated rip register.
     */
    public MachineState getNewState(BigInteger lineNum){
            Map<String, RegisterState> reg = this.registers;
            reg.put("rip", new RegisterState(lineNum.toByteArray(), lineNum.intValue()));
            return new MachineState(reg, this.memory, this.statusFlags);
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
    public MachineState getNewState(String regName, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP) {
        Map<String, RegisterState> reg = this.registers;
        List<StackEntry> mem = this.memory;
        if (val.isPresent()) {
            if (regName.equals("rsp") && val.get().compareTo(getRegisterValue("rsp")) == 1) {
                mem = new ArrayList<StackEntry>(this.memory);
                // reduced size of stack, so need to remove stack entries?!?!?
                List<StackEntry> toRemove = new ArrayList<StackEntry>();
                for (StackEntry se : this.memory) {
                    long seStartAddr = se.getStartAddress();
                    if (seStartAddr >= getRegisterValue("rsp").longValue()
                            && seStartAddr < val.get().longValue()) {
                        // need to remove this entry... eventually
                        toRemove.add(se);
                    }
                }
                mem.removeAll(toRemove);
            }
            String quadName = getQuadName(regName);
            Pair<Integer, Integer> range = getByteRange(regName);
            int startIndex = range.getKey();
            int endIndex = range.getValue();

            reg = new HashMap<String, RegisterState>(this.registers);
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

            byte[] newValFull = Arrays.copyOf(this.registers.get(quadName).getValue(), 8);
            for (int src = 0, dest = startIndex; dest < endIndex; src++, dest++) {
                newValFull[dest] = newVal[src];
            }

            reg.put(quadName, new RegisterState(newValFull, (new BigInteger(reg.get("rip").getValue())).intValue()));
        }
        if (updateRIP) {
            BigInteger ripVal = (new BigInteger(reg.get("rip").getValue())).add(BigInteger.ONE);
            reg.put("rip", new RegisterState(ripVal.toByteArray(), ripVal.intValue()));
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

        return new MachineState(reg, mem, flags);
    }

    /**
     * @return The BigInteger representation of the value in the rip register.
     */
    public BigInteger getRipRegister(){
        return new BigInteger(registers.get("rip").getValue());
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

        ba = registers.get(quadName).getValue();
        return new BigInteger(Arrays.copyOfRange(ba, startIndex, endIndex));
    }

    /**
     * Gets the value stored at the given memory address.
     *
     * @param address The starting address where the value is stored.
     * @param size The number of bytes of memory to read.
     */
    public BigInteger getMemoryValue(long address, int size) {
        //TODO: Allow addresses that aren't starting addresses but are still valid 
        
        for(StackEntry e : this.memory){
            if(e.getStartAddress() == address){
                return new BigInteger(e.getValueArr());
            }
        }
        System.out.println("Error: No value at address");
        System.exit(1);
        return null;
    }

    /**
     * Returns a list of all registers.
     *
     * @param regHistory Ordered list containing a history of register usage.
     * @return List of Register objects for all of the registers in this state.
     */
    public List<Register> getRegisters(List<String> regHistory) {
        ArrayList<Register> arr = new ArrayList<Register>();
        for (Map.Entry<String, RegisterState> entry : registers.entrySet()) {
            BigInteger b = new BigInteger(entry.getValue().getValue());
            byte[] ba = b.toByteArray();
            String s = "0x";
            for (byte i : ba) {
                s += String.format("%02x", i);
            }
            int regHist = regHistory.lastIndexOf(entry.getKey());
            arr.add(new Register(entry.getKey(), s, regHist, entry.getValue().getOrigin()));
        }
        return arr;
    }

    /**
     * Returns a list of stack entries.
     */
    public List<StackEntry> getStackEntries(){
        return memory;
    }
    
    public String toString() {
        String s = "Registers:\n";
        for (Map.Entry<String, RegisterState> entry : registers.entrySet()) {
            BigInteger b = new BigInteger(entry.getValue().getValue());
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
        for(StackEntry e : this.memory){
            byte [] ba = e.getValueArr();
            s += "\t" + Long.toHexString(e.getStartAddress()) + ": ";
            for(byte b : ba){
                s += String.format("%02x", b);
            }
            s += "\n";
        }
        return s;
    }
}
