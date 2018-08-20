package edu.sandiego.bcl;

/**
 * Operand constructor class for x86-64 operands.
 * 
 * @author sat
 */
public class x86OperandGetter implements OperandGetter {
    @Override
    public RegOperand getRegisterOperand(String name) {
        switch (name) {
            case "rax":
                return new RegOperand("rax", OpSize.QUAD);
            case "rdx":
                return new RegOperand("rdx", OpSize.QUAD);
            case "rsp":
                return new RegOperand("rsp", OpSize.QUAD);
            case "eax":
                return new RegOperand("eax", OpSize.LONG);
            case "edx":
                return new RegOperand("edx", OpSize.LONG);
            case "ax":
                return new RegOperand("eax", OpSize.WORD);
            case "dx":
                return new RegOperand("edx", OpSize.WORD);
            case "ah":
                return new RegOperand("eax", OpSize.BYTE);
            case "dl":
                return new RegOperand("edx", OpSize.BYTE);
            default:
                return null; // FIXME
        }
    }

    @Override
    public MemoryOperand getStackPointerOperand() {
        return new MemoryOperand("rsp", null, null, null, OpSize.QUAD, "");
    }
}
