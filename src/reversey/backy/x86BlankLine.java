/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author Caitlin
 */
public class x86BlankLine extends x86ProgramLine {

    public x86BlankLine(int lineNum, x86Comment c) {
        this.lineNum = lineNum;
        this.comment = Optional.ofNullable(c);
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
    public String toString(){
        String s = lineNum + ": ";
        if(comment.isPresent()){
            s += comment.get().toString();
        }
            return s;
    }
}
