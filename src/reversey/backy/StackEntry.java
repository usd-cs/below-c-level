/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class representing an entry in the program's stack.
 *
 * @author Caitlin
 */
public class StackEntry {
    private final SimpleLongProperty startAddress;
    private final SimpleLongProperty endAddress;
    private final SimpleStringProperty value;
    private final SimpleIntegerProperty origin;
    private final byte[] valueArr;
    
    public StackEntry (long startAddress, long endAddress, byte[] val, int orig) {
        this.startAddress = new SimpleLongProperty(startAddress);
        this.endAddress = new SimpleLongProperty(endAddress);
        this.valueArr = val;
        
        // Convert the value to a hex string, stripping off any leading 0's
        String s = "";
        
        // Note: we go through the array in reverse order because the value was
        // stored in little endian format.
        for (int i = (val.length - 1); i >= 0; i--) {
            s += String.format("%02X", val[i]);
        }
        if (s.charAt(0) == '0') s = s.replaceFirst("0+", "0");
        s = "0x" + s;
        
        this.value = new SimpleStringProperty(s);
        this.origin = new SimpleIntegerProperty(orig);
    }
    
    public long getStartAddress(){
        return startAddress.get();
    }
    
    public void setStartAddress(long l){
        startAddress.set(l);
    }
    
       public long getEndAddress(){
        return endAddress.get();
    }
    
    public void setEndAddress(long l){
        endAddress.set(l);
    }
    
    public byte[] getValueArr(){
        return valueArr;
    }
    
    public String getValue(){
        return value.get();
    }
    
    public void setValue(String s){
        value.set(s);
    }
    
    public int getOrigin(){
        return origin.get();
    }
    
    public void setOrigin(int or){
        origin.set(or);
    }
}
