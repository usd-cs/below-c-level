/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

import java.io.File;
import java.util.Optional;
import java.util.Scanner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sat
 */
public class X86ParserTest {
    
    public X86ParserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getRegisterSize method, of class X86Parser.
     */
    @Test
    public void testGetRegisterSize() throws Exception {
        System.out.println("getRegisterSize");
        
        String[] quad_registers = {"rax", "rbx", "rcx", "rdx", "rsi", "rdi", 
            "rbp", "rsp", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"};
        checkRegisterNames(quad_registers, OpSize.QUAD);
        
        String[] long_registers = {"eax", "ebx", "ecx", "edx", "esi", "edi", "ebp",
            "esp", "r8d", "r9d", "r10d", "r11d", "r12d", "r13d", "r14d",
            "r15d"};
        checkRegisterNames(long_registers, OpSize.LONG);

        String[] word_registers = {"ax", "bx", "cx", "dx", "si", "di", "bp", "sp",
            "r8w", "r9w", "r10w", "r11w", "r12w", "r13w", "r14w", "r15w"};
        checkRegisterNames(word_registers, OpSize.WORD);

        String[] byte_registers = {"al", "bl", "cl", "dl", "sil", "dil", "bpl", "spl",
            "r8b", "r9b", "r10b", "r11b", "r12b", "r13b", "r14b", "r15b"};
        checkRegisterNames(byte_registers, OpSize.BYTE);
    }

    private void checkRegisterNames(String[] registers, OpSize expResult) 
            throws X86ParsingException {
        for (String name : registers) {
            //String name = "rsp";
            OpSize result = X86Parser.getRegisterSize(name);
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of parseLine method, of class X86Parser.
     */
    @Test
    public void testParseLine() throws Exception {
        System.out.println("parseLine");
        Scanner s = new Scanner(new File("src/test/resources/all-instructions.s"));
        
        X86Parser instance = new X86Parser();
        int lineNum = 0;
        while (s.hasNextLine()) {
            String instr = s.nextLine();
            String expResult = lineNum + ": \t" + instr;
            x86ProgramLine result = instance.parseLine(instr);
            assertEquals(expResult, result.toString());
            lineNum++;
        }
        s.close();
        
        s = new Scanner(new File("src/test/resources/labels.s"));
        
        while (s.hasNextLine()) {
            String instr = s.nextLine();
            String expResult = lineNum + ": " + instr;
            x86ProgramLine result = instance.parseLine(instr);
            assertEquals(expResult, result.toString());
            lineNum++;
        }
        s.close();
    }

    /**
     * Test of getFirstLineOfMain method, of class X86Parser.
     */
    @Test
    public void testGetFirstLineOfMain() throws X86ParsingException {
        System.out.println("getFirstLineOfMain");
        X86Parser instance = new X86Parser();
        Optional<x86ProgramLine> expResult = Optional.empty();
        Optional<x86ProgramLine> result = instance.getFirstLineOfMain();
        assertEquals(expResult, result);
        
        instance.parseLine("pushq %rax");
        result = instance.getFirstLineOfMain();
        assertEquals(expResult, result);

        instance.parseLine("main:");
        result = instance.getFirstLineOfMain();
        System.out.println(result.get().toString());
        assertEquals("1: main:", result.get().toString());
    }
    
}
