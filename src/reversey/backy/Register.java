package reversey.backy;

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
    private SimpleIntegerProperty origin;
    
    public Register (String name, String value, int prom, int origin) {
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.prominence = prom;
        this.origin = new SimpleIntegerProperty(origin);
    }
    
	// Getters and setters
    public String getName(){
        return name.get();
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
}

