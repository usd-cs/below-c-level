/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

/**
 *
 * @author Caitlin
 */
public class RegisterState {
    private byte [] value;
    private int origin;
    
    public RegisterState(byte[] value, int origin){
        this.value = value;
        this.origin = origin;
    }
    
    public byte[] getValue(){
        return value;
    }
    
    public void setValue(byte [] val){
        this.value = val;
    }
    
    public int getOrigin(){
        return origin;
    }
    
    public void setOrigin(int or){
        this.origin = or;
    }
}
