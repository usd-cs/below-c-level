package edu.sandiego.bcl;

/**
 * The type of an x86 Instruction.
 */
public enum InstructionType {
    ADD(2),
    SUB(2),
    IMUL(2),
    CMP(2),
    OR(2),
    AND(2),
    TEST(2),
    XOR(2),
    SHL(2),
    SAL(2),
    SHR(2),
    SAR(2),
    MOV(2),
    MOVZ(2),
    MOVS(2),
    LEA(2),
    CMOVE(2),
    CMOVNE(2),
    CMOVS(2),
    CMOVNS(2),
    CMOVG(2),
    CMOVGE(2),
    CMOVL(2),
    CMOVLE(2),
    CMOVA(2),
    CMOVAE(2),
    CMOVB(2),
    CMOVBE(2),
    IDIV(1),
    INC(1),
    DEC(1),
    NEG(1),
    NOT(1),
    PUSH(1),
    POP(1),
    SETE(1),
    SETNE(1),
    SETS(1),
    SETNS(1),
    SETG(1),
    SETGE(1),
    SETL(1),
    SETLE(1),
    SETA(1),
    SETAE(1),
    SETB(1),
    SETBE(1),
    JE(1),
    JNE(1),
    JS(1),
    JNS(1),
    JG(1),
    JGE(1),
    JL(1),
    JLE(1),
    JA(1),
    JAE(1),
    JB(1),
    JBE(1),
    JMP(1),
    CALL(1),
    RET(0),
    CLT(0);

    /**
     * Number of operands used by the instruction.
     */
    private int numOperands;

    private InstructionType(int nO) {
        this.numOperands = nO;
    }

    public int numOperands() {
        return this.numOperands;
    }
}
