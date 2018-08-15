/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

import java.util.Optional;
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
    
    /**
     * Whether this line has a breakpoint or not.
     */
    protected boolean hasBreakpoint;
    
    /**
     * Optional comment.
     */
    protected Optional<x86Comment> comment;
    
    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        if (this.lineNum >= 0)
            this.lineNum = lineNum;
    }
    
    public boolean getBreakpoint(){
        return hasBreakpoint;
    }

    /**
     * Perform the operation specific to this line.
     * 
     * @param state The state of the machine before evaluation begins.
     * @return State of machine after evaluating the instruction.
     */
    public abstract MachineState eval(MachineState state) throws x86RuntimeException;


    /**
     * Returns the names of the registers used by this instruction.
     * This includes any implicit registers used (e.g. %rsp by push and pop)
     *
     * @return Set containing names of registers used by this instruction.
     */
    public abstract Set<String> getUsedRegisters();
    
    public void toggleBreakpoint(){
        hasBreakpoint = !hasBreakpoint;
    }
    
    /**
     * Constructs a string that provides an explanation of the line of code does
     * and/or means.
     * 
     * @return A string with a description of this line of code.
     */
    public abstract String getDescriptionString();
}
