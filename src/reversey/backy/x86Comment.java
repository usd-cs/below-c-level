/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Caitlin
 */
public class x86Comment extends x86ProgramLine {

    public final String comment;

    public x86Comment(String c, int lineNum) {
        this.comment = c;
        this.lineNum = lineNum;
    }

    @Override
    public MachineState eval(MachineState state) {
         return state.cloneWithIncrementedRIP();
    }

    @Override
    public Set<String> getUsedRegisters() {
       return new HashSet<>();
    }
    
    @Override
    public String toString() {
        return lineNum + ": " + comment;
    }
}
