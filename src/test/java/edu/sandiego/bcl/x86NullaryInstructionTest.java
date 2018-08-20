/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author sat
 */
public class x86NullaryInstructionTest {
    
    // Class under test
    private x86NullaryInstruction instruction;
    
    // Dependencies
    private MachineState initialState, finalState;
    private x86OperandGetter operandGetter;
    
    public x86NullaryInstructionTest() {
    }
    
    @Before
    public void setUp() {
        this.initialState = mock(MachineState.class);
        this.finalState = new MachineState();
        this.operandGetter = mock(x86OperandGetter.class);
    }

    /**
     * Test of toString method, of class x86NullaryInstruction.
     */
    @Test
    public void testToStringWithoutComment() {
        System.out.println("toString without a comment");
        x86NullaryInstruction instance = new x86NullaryInstruction(
                InstructionType.RET, OpSize.QUAD, 2, null, this.operandGetter);
        String result = instance.toString();
        assertEquals("2: \tretq", result);
    }
    
    @Test
    public void testToStringWithComment() {
        System.out.println("toString with a comment");
        x86Comment c = new x86Comment(" # foo");
        x86NullaryInstruction instance = new x86NullaryInstruction(
                InstructionType.RET, OpSize.QUAD, 2, c, this.operandGetter);
        String result = instance.toString();
        assertEquals("2: \tretq # foo", result);
    }

    /**
     * Test of clt method when eax is a negative number.
     */
    @Test
    public void testEvalCLTNeg() throws Exception {
        System.out.println("evalCLTNeg");
        
        HashMap<String, Boolean> flags = new HashMap<>();
        RegOperand mockEAX = mock(RegOperand.class);
        when(
                mockEAX.getValue(initialState)
        ).thenReturn(new BigInteger("-1"));
        
        RegOperand mockRAX = mock(RegOperand.class);
        when(
                mockRAX.updateState(initialState, Optional.of(new BigInteger("-1")), flags, true)
        ).thenReturn(finalState);
        
        when(
                this.operandGetter.getRegisterOperand("rax")
        ).thenReturn(mockRAX);
        when(
                this.operandGetter.getRegisterOperand("eax")
        ).thenReturn(mockEAX);
        
        instruction = new x86NullaryInstruction(InstructionType.CLT, OpSize.QUAD, 0, null, this.operandGetter);
        assertEquals(instruction.eval(initialState), finalState);
    }
    
    /**
     * Test of clt method when eax is a non-negative number.
     */
    @Test
    public void testEvalCLTNonNeg() throws Exception {
        System.out.println("evalCLTNonNeg");
        
        HashMap<String, Boolean> flags = new HashMap<>();
        RegOperand mockEAX = mock(RegOperand.class);
        when(
                mockEAX.getValue(initialState)
        ).thenReturn(BigInteger.ONE);
        
        RegOperand mockRAX = mock(RegOperand.class);
        when(
                mockRAX.updateState(initialState, Optional.of(BigInteger.ONE), flags, true)
        ).thenReturn(finalState);
        
        when(
                this.operandGetter.getRegisterOperand("rax")
        ).thenReturn(mockRAX);
        when(
                this.operandGetter.getRegisterOperand("eax")
        ).thenReturn(mockEAX);
        
        instruction = new x86NullaryInstruction(InstructionType.CLT, OpSize.QUAD, 0, null, this.operandGetter);
        assertEquals(instruction.eval(initialState), finalState);
    }
    
    /**
     * Test of ret method.
     */
    @Test
    public void testEvalRET() throws Exception {
        System.out.println("evalRET");
        
        // mock of top of stack returns 8 when asked for it's initial state value
        MemoryOperand mockSP = mock(MemoryOperand.class);
        when(mockSP.getValue(initialState)).thenReturn(new BigInteger("8"));
        when(this.operandGetter.getStackPointerOperand()).thenReturn(mockSP);
        
        // intermediate state will represent state with RIP set to popped value
        MachineState intermediateState = new MachineState();
        when(initialState.cloneWithNewRIP(8)).thenReturn(intermediateState);
        
        
        // mock of RSP register, returns 8 when asked for its value
        RegOperand mockRSP = mock(RegOperand.class);
        when(mockRSP.getValue(intermediateState)).thenReturn(new BigInteger("8"));
        when(this.operandGetter.getRegisterOperand("rsp")).thenReturn(mockRSP);
        
        // RET should increment RSP by 8, making it 16
        HashMap<String, Boolean> flags = new HashMap<>();
        when(
                mockRSP.updateState(intermediateState, Optional.of(new BigInteger("16")), flags, false)
        ).thenReturn(finalState);
        
        instruction = new x86NullaryInstruction(InstructionType.RET, OpSize.QUAD, 0, null, this.operandGetter);
        assertEquals(instruction.eval(initialState), finalState);
    }

    /**
     * Test of getUsedRegisters method for a CLT instruction.
     */
    @Test
    public void testGetUsedRegistersCLT() {
        System.out.println("getUsedRegisters CLT");
        x86NullaryInstruction instance = new x86NullaryInstruction(
                InstructionType.CLT, OpSize.QUAD, 0, null, this.operandGetter);
        Set<String> expResult = new HashSet<>();
        expResult.add("rax");
        Set<String> result = instance.getUsedRegisters();
        assertEquals(expResult, result);
    }
    
    /**
     * Test of getUsedRegisters method for a RET instruction.
     */
    @Test
    public void testGetUsedRegistersRET() {
        System.out.println("getUsedRegisters RET");
        x86NullaryInstruction instance = new x86NullaryInstruction(
                InstructionType.RET, OpSize.QUAD, 0, null, this.operandGetter);
        Set<String> expResult = new HashSet<>();
        expResult.add("rsp");
        Set<String> result = instance.getUsedRegisters();
        assertEquals(expResult, result);
    }
}
