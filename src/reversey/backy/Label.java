/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author Caitlin
 */
public class Label extends x86ProgramLine {

    private final String name;

    public Label(String name, int lineNum) {
        this.name = name;
        this.lineNum = lineNum;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return lineNum + ": " + name + ": ";
    }

    public MachineState eval(MachineState state) {
         return state.getNewState();
    }
    
    public Set<String> getUsedRegisters(){
        return new HashSet<String>();
    }
}
