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
    private SimpleLongProperty startAddress;
    private SimpleLongProperty endAddress;
    private SimpleStringProperty value;
    private SimpleIntegerProperty origin;
    private byte[] valueArr;
    
    public StackEntry() {
    }
    
    public StackEntry (long l1, long l2, byte[] val, int orig) {
        startAddress = new SimpleLongProperty(l1);
        endAddress = new SimpleLongProperty(l2);
        valueArr = val;
        String s = "";
         for (byte i : val) {
                s += String.format("%02x", i);
            }
        value = new SimpleStringProperty(s);
        origin = new SimpleIntegerProperty(orig);
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
