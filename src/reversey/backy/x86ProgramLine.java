/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.Set;

/**
 * Abstract class that represents a line of an X86 program.
 *
 * @author Caitlin
 */
public abstract class x86ProgramLine {

    /**
     * The line number where this instruction is located.
     */
    protected int lineNum;

    public int getLineNum() {
        return lineNum;
    }

    /**
     * Perform the operation specific to this line.
     * 
     * @param state The state of the machine before evaluation begins.
     * @return State of machine after evaluating the instruction.
     */
    public abstract MachineState eval(MachineState state);


    /**
     * Returns the names of the registers used by this instruction.
     * This includes any implicit registers used (e.g. %rsp by push and pop)
     *
     * @return Set containing names of registers used by this instruction.
     */
    public abstract Set<String> getUsedRegisters();
}
