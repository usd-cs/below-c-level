package edu.sandiego.bcl;

import java.util.Comparator;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class representing a register in our processor.
 *
 * @author Caitlin Fanning
 */
public class Register {
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
     * @param name 
     */
    public void setSubName(String name) {
        switch (name) {
            case "rax":
            case "rbx":
            case "rcx":
            case "rdx":
                longRegName = "eZ".replace("Z", name.subSequence(1, 3));
                wordRegName = "Zx".replace("Z", name.subSequence(1, 2));
                byteLowRegName = "Zl".replace("Z", name.subSequence(1, 2));
                break;
            case "rsi":
            case "rdi":
                longRegName = "eZ".replace("Z", name.subSequence(1, 3));
                wordRegName = "Zi".replace("Z", name.subSequence(1, 2));
                byteLowRegName = "Zil".replace("Z", name.subSequence(1, 2));
                break;
            case "rbp":
            case "rsp":
                longRegName = "eZ".replace("Z", name.subSequence(1, 3));
                wordRegName = "Zp".replace("Z", name.subSequence(1, 2));
                byteLowRegName = "Zpl".replace("Z", name.subSequence(1, 2));
                break;
            case "r8":
            case "r9":
            case "r10":
            case "r11":
            case "r12":
            case "r13":
            case "r14":
            case "r15":
                longRegName = "rXd".replace("X", name.substring(1));
                wordRegName = "rXw".replace("X", name.substring(1));
                byteLowRegName = "rXb".replace("X", name.substring(1));
                break; 
        }
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

