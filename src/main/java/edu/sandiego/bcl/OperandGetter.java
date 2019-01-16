/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

/**
 * A constructor of instruction operands.
 * 
 * @author sat
 */
public interface OperandGetter {
    public RegOperand getRegisterOperand(String name);
    
    public MemoryOperand getStackPointerOperand();
}