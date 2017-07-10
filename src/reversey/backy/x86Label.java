/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.Set;
import java.util.HashSet;

/**
 * Class representing a line with a label in an x86 program.
 * 
 * @author Caitlin Fanning
 */
public class x86Label extends x86ProgramLine {

    /**
     * The name associated with this label.
     */
    private final String name;

    public x86Label(String name, int lineNum) {
        this.name = name;
        this.lineNum = lineNum;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return lineNum + ": " + name + ": ";
    }

    @Override
    public MachineState eval(MachineState state) {
         return state.getNewState();
    }
    
    @Override
    public Set<String> getUsedRegisters(){
        return new HashSet<>();
    }
}
