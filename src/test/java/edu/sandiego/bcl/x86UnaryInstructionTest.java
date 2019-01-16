/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Test class for the x86UnaryInstruction class
 * 
 * @author sat
 */
public class x86UnaryInstructionTest {
    
    // Class under test
    private x86UnaryInstruction instruction;
    
    // Dependencies
    private Operand zeroOperand, oneOperand, negOneOperand;
    private Operand maxQuadOperand, minQuadOperand;
    private MachineState initialState, finalState;
    private OperandGetter operandGetter;
    
    public x86UnaryInstructionTest() {
    }
    
    private static HashMap<String, Boolean> makeFlagsMap(Optional<Boolean> zf,
            Optional<Boolean> sf, Optional<Boolean> of, Optional<Boolean> cf) {
        HashMap<String, Boolean> flags = new HashMap<>();
        zf.ifPresent(b -> flags.put("zf", b));
        sf.ifPresent(b -> flags.put("sf", b));
        of.ifPresent(b -> flags.put("of", b));
        cf.ifPresent(b -> flags.put("cf", b));
        return flags;
    }
    
    @Before
    public void setUp() throws Exception {
        zeroOperand = mock(Operand.class);
        when(
                zeroOperand.getValue(any(MachineState.class))
        ).thenReturn(BigInteger.ZERO);
        
        oneOperand = mock(Operand.class);
        when(
                oneOperand.getValue(any(MachineState.class))
        ).thenReturn(BigInteger.ONE);
        
        negOneOperand = mock(Operand.class);
        when(
                negOneOperand.getValue(any(MachineState.class))
        ).thenReturn(new BigInteger("-1"));
        
        byte[] maxQuadByteArray = {(byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, 
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
        BigInteger maxQuadBI = new BigInteger(maxQuadByteArray);
        
        maxQuadOperand = mock(Operand.class);
        when(
                maxQuadOperand.getValue(any(MachineState.class))
        ).thenReturn(maxQuadBI);
        
        byte[] minQuadByteArray = {(byte)0x80, 0, 0, 0, 0, 0, 0, 0};
        BigInteger minQuadBI = new BigInteger(minQuadByteArray);
        
        minQuadOperand = mock(Operand.class);
        when(
                minQuadOperand.getValue(any(MachineState.class))
        ).thenReturn(minQuadBI);
        
        initialState = mock(MachineState.class);
        finalState = new MachineState();
        this.operandGetter = mock(x86OperandGetter.class);
    }
    
    @Test
    public void testEvalINC1() throws Exception {
        System.out.println("evalINC");

        HashMap<String, Boolean> flags = makeFlagsMap(Optional.of(false), 
                Optional.of(false), Optional.of(false), Optional.empty());
        when(
                zeroOperand.updateState(initialState, Optional.of(BigInteger.ONE), flags, true)
        ).thenReturn(finalState);
        instruction = new x86UnaryInstruction(InstructionType.INC, zeroOperand, 
                OpSize.QUAD, 0, null, this.operandGetter);
        assertEquals(instruction.eval(initialState), finalState);
    }
    
    @Test
    public void testEvalINC2() throws Exception {
        System.out.println("evalINC2");

        HashMap<String, Boolean> flags = makeFlagsMap(Optional.of(false), 
                Optional.of(true), Optional.of(true), Optional.empty());
        when(
                maxQuadOperand.updateState(initialState, 
                        Optional.of(new BigInteger("-8000000000000000", 16)), 
                        flags, 
                        true)
        ).thenReturn(finalState);
        
        instruction = new x86UnaryInstruction(InstructionType.INC, maxQuadOperand,
                OpSize.QUAD, 0, null, this.operandGetter);
        assertEquals(finalState, instruction.eval(initialState));
    }
    
    @Test
    public void testEvalDEC1() throws Exception {
        System.out.println("evalDEC1");

        HashMap<String, Boolean> flags = makeFlagsMap(Optional.of(true), 
                Optional.of(false), Optional.of(false), Optional.empty());
        when(
                oneOperand.updateState(initialState, Optional.of(BigInteger.ZERO), flags, true)
        ).thenReturn(finalState);
        
        instruction = new x86UnaryInstruction(InstructionType.DEC, oneOperand,
                OpSize.QUAD, 0, null, this.operandGetter);
        assertEquals(instruction.eval(initialState), finalState);
    }
    
    @Test
    public void testEvalDEC2() throws Exception {
        System.out.println("evalDEC2");

        HashMap<String, Boolean> flags = makeFlagsMap(Optional.of(false), 
                Optional.of(false), Optional.of(true), Optional.empty());
        when(
                minQuadOperand.updateState(initialState, 
                        Optional.of(new BigInteger("7FFFFFFFFFFFFFFF", 16)), 
                        flags, 
                        true)
        ).thenReturn(finalState);
        
        instruction = new x86UnaryInstruction(InstructionType.DEC, minQuadOperand,
                OpSize.QUAD, 0, null, this.operandGetter);
        assertEquals(finalState, instruction.eval(initialState));
    }
    
}
