/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@FunctionalInterface
interface NullaryX86Operation {
    MachineState apply(MachineState state) throws x86RuntimeException;
}

/**
 * Class representing an x86 instruction with no operands.
 * 
 * @author Sat Garcia
 */
public class x86NullaryInstruction extends x86Instruction {

    /**
     * The function that this instruction performs.
     */
    private NullaryX86Operation operation;

    /**
     * @param instType The type of operation performed by the instruction.
     * @param size Number of bytes this instruction works on.
     * @param line The line number associated with this instruction.
     * @param c A commend associated with this line of the program.
     */
    public x86NullaryInstruction(InstructionType instType, OpSize size, int line, 
            x86Comment c) {
        this.type = instType;
        this.opSize = size;
        this.lineNum = line;
        this.comment = Optional.ofNullable(c);

        switch (instType) {
            case CLT:
                this.operation = this::clt;
                break;
            case RET:
                this.operation = this::ret;
                break;
            default:
                throw new RuntimeException("unsupported instr type: " + instType);
        }
    }

    /**
     * Sign extends the eax register to fill the rax register.
     * 
     * @param state The machine state in which to work.
     * @return A clone of {@code state}, but with an incremented rip and
     * register eax sign extended to fill register rax.
     * @throws x86RuntimeException if there is a runtime error while performing
     * the operation.
     */
    private MachineState clt(MachineState state) throws x86RuntimeException {
        // Gets the value of eax, sign extends it then updates rax with that value
        RegOperand eaxReg = new RegOperand("eax", OpSize.LONG);
        BigInteger eaxVal = eaxReg.getValue(state);
        byte[] raxByteArray = MachineState.getExtendedByteArray(eaxVal, 4, 8, false);
        BigInteger raxVal = new BigInteger(raxByteArray);
        RegOperand raxReg = new RegOperand("rax", OpSize.QUAD);
        
        // TODO: make sure CLT doesn't update any status flags
        return raxReg.updateState(state, Optional.of(raxVal), new HashMap<>(), true);
    }
    
    /**
     * Performs a function call return by popping the return address from the top
     * of the stack and jumping to that address.
     * 
     * @param state The machine state in which to work.
     * @return @return A clone of {@code state}, but with a quad word popped off
     * the stack and the rip set to the value that was popped off.
     * @throws x86RuntimeException if there is a runtime error while performing
     * the operation.
     */
    private MachineState ret(MachineState state) throws x86RuntimeException {
        Map<String, Boolean> flags = new HashMap<>();
        
        // step 1: store (%rsp) value in rip register 
        MemoryOperand src = new MemoryOperand("rsp", null, null, null, this.opSize, "");
        MachineState tmp, mS;
        try {
            tmp = state.cloneWithNewRIP(src.getValue(state).intValue());
            
            // step 2: add 8 to rsp
            RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
            mS = rsp.updateState(tmp, Optional.of(rsp.getValue(tmp).add(BigInteger.valueOf(8))), flags, false);
        } catch (x86RuntimeException ex) {
            if(state.getCallStackSize() != 0){
                throw ex;
            }
            // FIXME: Think about incrementing rip or not
            mS = src.updateState(state, Optional.empty(), flags, false);
        }
        mS.popFromCallStack();
        return mS;
    }
    
    @Override
    public void updateLabels(String labelName, x86Label label) {}
    
    @Override
    public String toString() {
        String s = lineNum + ": \t" + getInstructionTypeString();
        if(comment.isPresent()){
            s += comment.get().toString();
        }
        return s;
    }

    @Override
    public MachineState eval(MachineState state) throws x86RuntimeException {
        return operation.apply(state); // the journey is the destination
    }

    @Override
    public Set<String> getUsedRegisters() {
        Set<String> result = new HashSet<>();
        
        // Check for implicitly used registers
        if (this.type == InstructionType.RET) result.add("rsp");
        else if (this.type == InstructionType.CLT) result.add("rax");
        return result;
    }

    @Override
    public String getDescriptionString() {
        switch (this.type) {
            case CLT:
                return "Sign-extends register %eax to fill register %rax.";
            case RET:
                return "Pops return address off the stack and jumps to that address.";
            default:
                throw new RuntimeException("unsupported instr type: " + this.type);
        }
    }
}
