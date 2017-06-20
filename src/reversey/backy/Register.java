/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Caitlin
 */
public class Register {
    private SimpleStringProperty name;
    private SimpleStringProperty value;
    
    public Register() {
    }
    
    public Register (String s1, String s2) {
        name = new SimpleStringProperty(s1);
        value = new SimpleStringProperty(s2);
    }
    
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
}

