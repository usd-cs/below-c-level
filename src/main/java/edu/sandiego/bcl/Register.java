package edu.sandiego.bcl;

import java.util.Arrays;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class representing a register in our processor.
 *
 * @author Caitlin Fanning
 */
public class Register {
    
    private static final Map<String, List<String>> subRegistersFromFullRegister;
    
    static {
        subRegistersFromFullRegister = new HashMap<String, List<String>>();
        subRegistersFromFullRegister.put("rax", Arrays.asList("rax", "eax", "ax", "al", "ah"));
        subRegistersFromFullRegister.put("rbx", Arrays.asList("rbx", "ebx", "bx", "bl", "bh"));
        subRegistersFromFullRegister.put("rcx", Arrays.asList("rcx", "ecx", "cx", "cl", "ch"));
        subRegistersFromFullRegister.put("rdx", Arrays.asList("rdx", "edx", "dx", "dl", "dh"));
        subRegistersFromFullRegister.put("rsi", Arrays.asList("rsi", "esi", "si", "sil"));
        subRegistersFromFullRegister.put("rdi", Arrays.asList("rdi", "edi", "di", "dil"));
        subRegistersFromFullRegister.put("rbp", Arrays.asList("rbp", "ebp", "bp", "bpl"));
        subRegistersFromFullRegister.put("rsp", Arrays.asList("rsp", "esp", "sp", "spl"));
        for (int i = 8; i < 16; i++) {
            subRegistersFromFullRegister.put("r" + i, 
                    Arrays.asList("r" + i,
                            "r" + i + "d",
                            "r" + i + "w",
                            "r" + i + "b"));
        }
    }
    
    /**
     * The name of the register (e.g. "rax")
     */
    private final SimpleStringProperty name;

    /**
     * The prominence of this register.
     * Larger values mean higher prominence (e.g. more recently used.)
     */
    private final int prominence;
    
    /**
     * The line number from which the register last was updated.
     */
    private final SimpleIntegerProperty origin;
    
    /**
     * The full, quad length (8 bytes) value of the register.
     * This value is a string containing a hexadecimal number.
     */
    private final String quadValue;
    
    /**
     * 32, 16, and 8-bit register names.
     */
    private final String longRegName;
    private final String wordRegName;
    private final String byteLowRegName;
    
    public Register (String quadName, int prom, int origin, String quadVal) {
        assert subRegistersFromFullRegister.keySet().contains(quadName);
        
        this.name = new SimpleStringProperty(quadName);
        this.longRegName = subRegistersFromFullRegister.get(quadName).get(1);
        this.wordRegName = subRegistersFromFullRegister.get(quadName).get(2);
        this.byteLowRegName = subRegistersFromFullRegister.get(quadName).get(3);
        this.prominence = prom;
        this.origin = new SimpleIntegerProperty(origin);
        this.quadValue = quadVal;
    }
    
    // Getters and setters
    public String getName(){
        return name.get();
    }
    
    public String getLongRegName(){
        return longRegName;
    }
    
    public String getWordRegName(){
        return wordRegName;
    }
    
    public String getByteLowRegName(){
        return byteLowRegName;
    }
    
    public void setName(String s){
        name.set(s);
    }
    
    public int getProminence(){
        return this.prominence;
    }
    
    public int getOrigin(){
        return origin.get();
    }
    
    public void setOrigin(int ori){
        origin.set(ori);
    }
    

    public String getSubValue(int numBytes, int base, boolean trim) { 
        String subRegString = quadValue.substring((8 - numBytes) * 2);
        if (base == 0) {
            if (trim) {
                if (subRegString.charAt(0) == '0') {
                    subRegString = subRegString.replaceFirst("0+", "0");
                }
                subRegString = subRegString.replaceFirst("FFFF+", "F..F");
            }
            return "0x" + subRegString;
        }
        else if (base == 1) {
            BigInteger bI = new BigInteger(subRegString, 16);
            return bI.toString();
        }
        else {
            BigInteger bI = new BigInteger(quadValue, 16);
            switch (numBytes) {
                case 1:
                    return String.valueOf(bI.byteValue());
                case 2:
                    return String.valueOf(bI.shortValue());
                case 4:
                    return String.valueOf(bI.intValue());
                case 8:
                    return String.valueOf(bI.intValue());
                default:
                    break;
            }
            System.exit(1);
            return "";
        }
    }
    
    /**
     * Returns the subregister of {@code size} bytes in the same family as the
     * register with the given name.
     * 
     * @note A register "family" is a set of related subregisters (e.g. ebx is
     * in the same family as rbx, as is bx, bh, and bl).
     * 
     * @param name The name of the register whose family we want to search.
     * @param size The number of bytes of the register we want.
     * @return Name of the register of the given size in the same family as the
     *  given register.
     */
    public static String getSubRegisterName(String name, int size) {
        if (size != 1 && size != 2 && size != 4 && size != 8) {
            return null;
        }
        
        for (List<String> subRegisterList : subRegistersFromFullRegister.values()) {
            if (subRegisterList.contains(name)) {
                if (size == 8) return subRegisterList.get(0);
                else if (size == 4) return subRegisterList.get(1);
                else if (size == 2) return subRegisterList.get(2);
                else if (size == 1) return subRegisterList.get(3);
            }
        }
        
        return null;
    }
    
    /**
     * Comparator for registers, based on their relative prominence then their
     * lexicographical ordering.
     */
    public static final Comparator<Register> comparator = (Register r1, Register r2) -> {
        if (r1.getProminence() > r2.getProminence()) {
            return -1;
        } else if (r1.getProminence() == r2.getProminence()) {
            return r1.getName().compareTo(r2.getName());
        } else {
            return 1;
        }
    };
}

