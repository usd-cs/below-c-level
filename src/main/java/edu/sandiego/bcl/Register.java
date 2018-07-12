package edu.sandiego.bcl;

import java.util.Arrays;
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
    private SimpleStringProperty name;

    /**
     * String representation of the register's value (in hex).
     */
    private SimpleStringProperty value;

    /**
     * The prominence of this register.
     * Larger values mean higher prominence (e.g. more recently used.)
     */
    private int prominence;
    
    /**
     * The line number from which the register last was updated.
     */
    private SimpleIntegerProperty origin;
    
    /**
     * The full 64 bit value of the register.
     */
    private String fullValue;
    
    /**
     * 32, 16, and 8-bit register names.
     */
    private String longRegName;
    private String wordRegName;
    private String byteLowRegName;
    
    public Register (String name, String value, int prom, int origin, String fullVal) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.prominence = prom;
        this.origin = new SimpleIntegerProperty(origin);
        this.fullValue = fullVal;
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
    
    public String getValue(){
        return value.get();
    }
    
    public void setValue(String s){
        value.set(s);
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
    
    public String getSubValue(int numBytes){ 
        return "0x" + fullValue.substring((8 - numBytes) * 2);
    }
    
    /**
     * Sets the 32, 16, and 8-bit register names based on given 64-bit register
     * 
     * @param name 
     */
    public void setSubName(String name) {
        if (subRegistersFromFullRegister.containsKey(name)) {
            longRegName = subRegistersFromFullRegister.get(name).get(1);
            wordRegName = subRegistersFromFullRegister.get(name).get(2);
            byteLowRegName = subRegistersFromFullRegister.get(name).get(3);
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

