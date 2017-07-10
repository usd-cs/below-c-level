/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@FunctionalInterface
interface NullaryX86Operation {
    MachineState apply(MachineState state);
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
     */
    public x86NullaryInstruction(InstructionType instType, OpSize size, int line) {
        this.type = instType;
        this.opSize = size;
        this.lineNum = line;

        switch (instType) {
            case RET:
                this.operation = this::ret;
                break;
            default:
                throw new RuntimeException("unsupported instr type: " + instType);
        }
    }

    public MachineState ret(MachineState state) {
        Map<String, Boolean> flags = new HashMap<>();
        
        // step 1: store (%rsp) value in rip register 
        MemoryOperand src = new MemoryOperand("rsp", null, 1, 0, this.opSize);
        MachineState tmp = state.getNewState(src.getValue(state));

        // step 2: add 8 to rsp
        RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
        return rsp.updateState(tmp, Optional.of(rsp.getValue(tmp).add(BigInteger.valueOf(8))), flags, false);
    }
    
    @Override
    public void updateLabels(String labelName, x86Label label) {}
    
    @Override
    public String toString() {
        return lineNum + ": \t" + getInstructionTypeString();
    }

    @Override
    public MachineState eval(MachineState state) {
        return operation.apply(state); // the journey is the destination
    }

    @Override
    public Set<String> getUsedRegisters() {
        return new HashSet<>();
    }
}
