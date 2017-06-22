/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.util.Pair;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.beans.property.SimpleStringProperty;

/**
 * An abstract class representing an x86-64 instruction.
 *
 * @author Dr. Sat
 */
public abstract class x86Instruction {

    /**
     * The operand where the instruction will write its results.
     */
    protected Operand destination;

    /**
     * The type of instruction (e.g. add)
     */
    protected InstructionType type;

    /**
     * The number of bytes the operation works on.
     */
    protected OpSize opSize;

    // Getters
    public InstructionType getType() {
        return this.type;
    }

    public OpSize getOpSize() {
        return this.opSize;
    }

    /**
     * Perform the operation specific to the instruction.
     *
     * @param state The state of the machine before evaluation begins.
     * @return State of machine after evaluating the instruction.
     */
    public abstract MachineState eval(MachineState state);

    /**
     * Returns the names of all the registers used by this instruction 
     */
    public abstract Set<String> getUsedRegisters();
    
    /**
     * Checks that instruction is a valid, supported x86 instruction.
     *
     * @param instrName The name of the instruction (e.g. addl)
     * @return A pair containing both the instruction type and op size unary
     * instruction.
     */
    public static Pair<InstructionType, OpSize> parseTypeAndSize(String instrName) {
        InstructionType type;
        OpSize size;

        String validInstrNames = "^(add|sub|xor|or|and|shl|sal|shr|sar|mov|lea|inc|dec|neg|not|push|pop|cmp|test)(b|w|l|q)$";
        String validSetInstrNames = "^(set)(e|ne|s|ns|g|ge|l|le)$";
        if (Pattern.matches(validInstrNames, instrName)) {
            type = InstructionType.valueOf(instrName.substring(0, instrName.length() - 1).toUpperCase());
            switch (instrName.charAt(instrName.length() - 1)) {
                case 'b':
                    System.out.println("instruction size is BYTE");
                    size = OpSize.BYTE;
                    break;
                case 'w':
                    System.out.println("instruction size is WORD");
                    size = OpSize.WORD;
                    break;
                case 'l':
                    System.out.println("instruction size is LONG");
                    size = OpSize.LONG;
                    break;
                case 'q':
                    System.out.println("instruction size is QUAD");
                    size = OpSize.QUAD;
                    break;
                default:
                    System.err.println("ERRRRROR: this should never happen...");
                    return null;
            }
        } else if (Pattern.matches(validSetInstrNames, instrName)) {
            type = InstructionType.valueOf(instrName.toUpperCase());
            size = OpSize.BYTE;
        } else {
            System.err.println("ERROR: invalid or unsupported instruction: " + instrName);
            return null; // TODO: throw exception
        }

        return new Pair<InstructionType, OpSize>(type, size);
    }

    /**
     * Get the register name from a given string.
     *
     * @param str The string that contains a register name, starting with %
     * @return A pair containing the name of the register and its size
     */
    public static Pair<String, OpSize> parseRegister(String str) {
        System.out.println("parseRegister: " + str);

        OpSize opSize = OpSize.BYTE;
        String longRegNames = "^\\%(e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)d)$";
        String quadRegNames = "^\\%r(ax|bx|cx|dx|si|di|bp|sp|8|9|10|11|12|13|14|15)$";
        String wordRegNames = "^\\%((ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)w)$";
        String byteRegNames = "^\\%((al|ah|bl|bh|cl|ch|dl|dh|sil|dil|bpl|spl)|r(8|9|10|11|12|13|14|15)b)$";
        if (Pattern.matches(longRegNames, str)) {
            opSize = OpSize.LONG;
        } else if (Pattern.matches(quadRegNames, str)) {
            opSize = OpSize.QUAD;
        } else if (Pattern.matches(wordRegNames, str)) {
            opSize = OpSize.WORD;
        } else if (Pattern.matches(byteRegNames, str)) {
            opSize = OpSize.BYTE;
        } else {
            System.err.println("ERROR: unknown register name");
            return null; // TODO: throw exception
        }

        String name = str.substring(1);
        return new Pair<String, OpSize>(name, opSize);
    }

    /**
     * Construct an operand based on a given string.
     *
     * @param String that contains the operand at the beginning.
     * @return The parsed operand and the index in str where the operand ends.
     */
    public static Pair<Operand, Integer> parseOperand(String str, OpSize instrOpSize) {
        System.out.println("parsing Operand in: " + str);

        Operand op = null;
        int endPoint = 0;

        if (str.charAt(0) == '$') {
            // constant operand
            System.out.println("found a constant op");
            String[] splits = str.split("[$,]");
            op = new ConstantOperand(Integer.parseInt(splits[1])); // TODO: handle hex constant
        } else if (str.charAt(0) == '%') {
            // register operand
            System.out.println("found a register op");
            String[] splits = str.split(",");

            Pair<String, OpSize> regDetails = parseRegister(splits[0]);
            String regName = regDetails.getKey();
            OpSize opSize = regDetails.getValue();

            // TODO: move this to a "validation" method
            if (opSize != instrOpSize) {
                System.err.println("ERROR: op size mismatch (expected: " + instrOpSize + "; got: " + opSize + ")");
                return null; // TODO: throw exception
            }

            op = new RegOperand(regName, opSize);
        } else {
            // memory operand
            System.out.println("found a memory op");

            if (str.indexOf('(') == -1 || str.indexOf(')') == -1) {
                System.err.println("ERROR: missing ( and/or )");
                return null; // TODO: throw exception
            }

            int opEndIndex = str.indexOf(')') + 1;
            if (opEndIndex != str.length() && str.charAt(opEndIndex) != ',') {
                System.err.println("ERROR: missing separator between first and second operand");
                return null;
            }

            String opString = str.substring(0, opEndIndex);
            System.out.println("operand string: " + opString);

            if (opString.indexOf('(') == -1) {
                System.err.println("unmatched ) found");
                return null; // TODO: throw exception
            }

            System.out.println("opString: " + opString);

            String[] components = opString.split("[(),]");
            if (components.length < 2 || components.length > 4) {
                System.err.println("ERROR: invalid number of memory op components");
                return null; // TODO: throw exception
            }

            int offset = 0;
            if (!components[0].isEmpty()) {
                System.out.println("got an offset for memory op");
                offset = Integer.parseInt(components[0]); // TODO: handle hex
            }

            Pair<String, OpSize> baseRegDetails = parseRegister(components[1]);
            String baseReg = baseRegDetails.getKey();
            //OpSize baseOpSize = baseRegDetails.getValue();
            String indexReg = null;
            int scale = 1;
            if (components.length > 2) {
                Pair<String, OpSize> indexRegDetails = parseRegister(components[2]);
                indexReg = indexRegDetails.getKey();
                //OpSize indexOpSize = indexRegDetails.getValue();
            }
            if (components.length > 3) {
                scale = Integer.parseInt(components[3]);
                if (scale != 1 && scale != 2 && scale != 4 && scale != 8) {
                    System.err.println("ERROR: invalid scaling factor: " + scale);
                    return null; // TODO: throw exception
                }
            }

            op = new MemoryOperand(baseReg, indexReg, scale, offset, instrOpSize);
        }

        if (str.charAt(0) == '$' || str.charAt(0) == '%') {
            endPoint = str.indexOf(',');
            if (endPoint == -1) {
                endPoint = str.length();
            } else {
                endPoint++;
            }
        } else {
            endPoint = str.indexOf(')') + 1;

            // @tricky this should never happen, should throw exception above
            if (endPoint == -1) {
                return null;
            } else if (endPoint != str.length()) {
                endPoint++;
            }
        }

        return new Pair<Operand, Integer>(op, endPoint);
    }

    /**
     * Create an x86-64 instruction by parsing a given string.
     *
     * @param instr A string representation of the instruction.
     * @return The parsed instruction.
     */
    public static x86Instruction parseInstruction(String instr) {
        String[] tokens = instr.split("\\s+");
        String instrName = tokens[0]; // should be instruction name, e.g. "addl"
        System.out.println("instr name: " + instrName);

        Pair<InstructionType, OpSize> instDetails = parseTypeAndSize(instrName);
        InstructionType instrType = instDetails.getKey();
        OpSize opSize = instDetails.getValue();

        String operandsStr = String.join("", tokens).substring(tokens[0].length());
        //System.out.println("operand str: " + operandsStr);

        Pair<Operand, Integer> firstOperandAndEndPt = parseOperand(operandsStr, opSize);
        //System.out.println("First operand was: " + firstOperandAndEndPt.getKey());

        if (instrType.numOperands() == 2) {
            if (operandsStr.indexOf(',') == -1) {
                System.err.println("ERROR: Couldn't find separator between first and second operand.");
                return null; // TODO: throw exception
            }

            String secondOpStr = operandsStr.substring(firstOperandAndEndPt.getValue());

            Pair<Operand, Integer> secondOperandAndEndPt = parseOperand(secondOpStr, opSize);

            if (secondOperandAndEndPt.getValue() != secondOpStr.length()) {
                System.err.println("ERROR: extra stuff left over: " + secondOpStr.substring(secondOperandAndEndPt.getValue()));
                return null; // TODO: make this throw an exception
            }

            return new x86BinaryInstruction(instrName.substring(0, instrName.length() - 1),
                    firstOperandAndEndPt.getKey(),
                    secondOperandAndEndPt.getKey(),
                    opSize);
        } else if (instrType.numOperands() == 1) {
            if (firstOperandAndEndPt.getValue() != operandsStr.length()) {
                System.err.println("ERROR: extra stuff left over: " + operandsStr.substring(firstOperandAndEndPt.getValue()));
                return null; // TODO: make this throw an exception
            }

            if (!instrName.startsWith("set")) {
                instrName = instrName.substring(0, instrName.length() - 1);
            }

            return new x86UnaryInstruction(instrName,
                    firstOperandAndEndPt.getKey(),
                    opSize);
        } else {
            System.err.println("ERROR: Only support binary and unary x86 instructions.");
            System.exit(1);
            return null;
        }

    }

    public abstract String toString();
}

/**
 * Class representing an x86 instruction with a single operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86UnaryInstruction extends x86Instruction {

    /**
     * The function that this instruction performs.
     */
    private UnaryX86Operation operation;

    /**
     * @param instType String representation of the instruction's operation.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     */
    public x86UnaryInstruction(String instType, Operand destOp, OpSize size) {
        this.destination = destOp;
        this.opSize = size;

        Map<String, Boolean> flags = new HashMap<String, Boolean>();

        switch (instType) {
            case "inc":
                this.type = InstructionType.INC;
                this.operation
                        = (state, dest) -> {
                            BigInteger result = dest.getValue(state).add(BigInteger.ONE);
                            flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

                            // truncate if we are too long
                            byte[] resArray = result.toByteArray();
                            if (resArray.length > this.opSize.numBytes()) {
                                byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
                                result = new BigInteger(ba);
                            }

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);

                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "dec":
                this.type = InstructionType.DEC;
                this.operation
                        = (state, dest) -> {
                            BigInteger result = dest.getValue(state).subtract(BigInteger.ONE);
                            flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

                            // truncate if we are too long
                            byte[] resArray = result.toByteArray();
                            if (resArray.length > this.opSize.numBytes()) {
                                byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
                                result = new BigInteger(ba);
                            }

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);

                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "neg":
                this.type = InstructionType.NEG;
                this.operation
                        = (state, dest) -> {
                            BigInteger orig = dest.getValue(state);
                            BigInteger result = orig.negate();
                            flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

                            // truncate if we are too long
                            byte[] resArray = result.toByteArray();
                            if (resArray.length > this.opSize.numBytes()) {
                                byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
                                result = new BigInteger(ba);
                            }

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("cf", orig.compareTo(BigInteger.ZERO) != 0);

                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "not":
                this.type = InstructionType.NOT;
                this.operation
                        = (state, dest) -> {
                            BigInteger result = dest.getValue(state).not();
                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "sete":
            case "setne":
            case "sets":
            case "setns":
            case "setg":
            case "setge":
            case "setl":
            case "setle":
                final Predicate<MachineState> p;
                switch (instType) {
                    case "sete":
                        this.type = InstructionType.SETE;
                        p = state -> state.getZeroFlag();
                        break;
                    case "setne":
                        this.type = InstructionType.SETNE;
                        p = state -> !state.getZeroFlag();
                        break;
                    case "sets":
                        this.type = InstructionType.SETS;
                        p = state -> state.getSignFlag();
                        break;
                    case "setns":
                        this.type = InstructionType.SETNS;
                        p = state -> !state.getSignFlag();
                        break;
                    case "setg":
                        this.type = InstructionType.SETG;
                        p = state -> !(state.getSignFlag() ^ state.getOverflowFlag()) & !state.getZeroFlag();
                        break;
                    case "setge":
                        this.type = InstructionType.SETGE;
                        p = state -> !(state.getSignFlag() ^ state.getOverflowFlag());
                        break;
                    case "setl":
                        this.type = InstructionType.SETL;
                        p = state -> (state.getSignFlag() ^ state.getOverflowFlag());
                        break;
                    case "setle":
                        this.type = InstructionType.SETLE;
                        p = state -> (state.getSignFlag() ^ state.getOverflowFlag()) | state.getZeroFlag();
                        break;
                    default:
                        p = null;
                        System.err.println("ERROR: set that isn't a set: " + instType);
                        System.exit(1);
                }
                this.operation
                        = (state, dest) -> {
                            BigInteger result = p.test(state) ? BigInteger.ONE : BigInteger.ZERO;
                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "push":
                this.type = InstructionType.PUSH;
                this.operation
                        = (state, src) -> {
                            // step 1: subtract 8 from rsp
                            RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
                            MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags);

                            // step 2: store src operand value in (%rsp)
                            MemoryOperand dest = new MemoryOperand("rsp", null, 1, 0, this.opSize);
                            return dest.updateState(tmp, Optional.of(src.getValue(tmp)), flags);
                        };
                break;
            case "pop":
                this.type = InstructionType.POP;
                this.operation
                        = (state, dest) -> {
                            // step 1: store (%rsp) value in dest operand 
                            MemoryOperand src = new MemoryOperand("rsp", null, 1, 0, this.opSize);
                            MachineState tmp = dest.updateState(state, Optional.of(src.getValue(state)), flags);

                            // step 2: add 8 to rsp
                            RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
                            return rsp.updateState(tmp, Optional.of(rsp.getValue(tmp).add(BigInteger.valueOf(8))), flags);

                        };
                break;
            default:
                System.err.println("invalid instr type for unary inst: " + instType);
                System.exit(1);
        }
    }

    @Override
    public MachineState eval(MachineState state) {
        return operation.apply(state, this.destination);
    }

    @Override
    public Set<String> getUsedRegisters() {
        return destination.getUsedRegisters();
    }
    
    @Override
    public String toString() {
        return type + " " + destination.toString();
    }
}

/**
 * Class representing an x86 instruction with two operands.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86BinaryInstruction extends x86Instruction {

    /**
     * An operand used solely as a source for the operation.
     */
    private Operand source;

    /**
     * The function performed by this instruction.
     */
    private BinaryX86Operation operation;

    // TODO: instType should be of type InstructionType
    /**
     * @param instType String representation of the instruction's operation.
     * @param srcOp A source operand of the instruction.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     */
    public x86BinaryInstruction(String instType, Operand srcOp, Operand destOp, OpSize size) {
        this.source = srcOp;
        this.destination = destOp;
        this.opSize = size;

        Map<String, Boolean> flags = new HashMap<String, Boolean>();

        switch (instType) {
            case "add":
                this.type = InstructionType.ADD;
                this.operation
                        = (state, src, dest) -> {
                            BigInteger src1 = dest.getValue(state);
                            BigInteger src2 = src.getValue(state);
                            BigInteger result = src1.add(src2);

                            flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

                            // truncate if we are too long
                            byte[] resArray = result.toByteArray();
                            if (resArray.length > this.opSize.numBytes()) {
                                byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
                                result = new BigInteger(ba);
                            }

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("cf", false); // FIXME: implement
                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "sub":
                this.type = InstructionType.SUB;
                this.operation
                        = (state, src, dest) -> {
                            BigInteger src1 = dest.getValue(state);
                            BigInteger src2 = src.getValue(state);
                            BigInteger result = src1.subtract(src2);

                            flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

                            // truncate if we are too long
                            byte[] resArray = result.toByteArray();
                            if (resArray.length > this.opSize.numBytes()) {
                                byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
                                result = new BigInteger(ba);
                            }

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("cf", false); // FIXME: implement
                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "cmp":
                this.type = InstructionType.CMP;
                this.operation
                        = (state, src, dest) -> {
                            BigInteger src1 = dest.getValue(state);
                            BigInteger src2 = src.getValue(state);
                            BigInteger result = src1.subtract(src2);

                            flags.put("of", (result.bitLength() + 1) > this.opSize.numBits());

                            // truncate if we are too long
                            byte[] resArray = result.toByteArray();
                            if (resArray.length > this.opSize.numBytes()) {
                                byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
                                result = new BigInteger(ba);
                            }

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("cf", false); // FIXME: implement
                            return dest.updateState(state, Optional.empty(), flags);
                        };
                break;
            case "xor":
                this.type = InstructionType.XOR;
                this.operation
                        = (state, src, dest) -> {
                            BigInteger result = dest.getValue(state).xor(src.getValue(state));
                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("of", false);
                            flags.put("cf", false);
                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "or":
                this.type = InstructionType.OR;
                this.operation
                        = (state, src, dest) -> {
                            BigInteger result = dest.getValue(state).or(src.getValue(state));
                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("of", false);
                            flags.put("cf", false);
                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "and":
                this.type = InstructionType.AND;
                this.operation
                        = (state, src, dest) -> {
                            BigInteger result = dest.getValue(state).and(src.getValue(state));
                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("of", false);
                            flags.put("cf", false);
                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "test":
                this.type = InstructionType.TEST;
                this.operation
                        = (state, src, dest) -> {
                            BigInteger result = dest.getValue(state).and(src.getValue(state));
                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);
                            flags.put("of", false);
                            flags.put("cf", false);
                            return dest.updateState(state, Optional.empty(), flags);
                        };
                break;
            case "sal":
            case "shl":
                this.type = InstructionType.SAL;
                this.operation
                        = (state, src, dest) -> {
                            int shamt = src.getValue(state).intValue() % 32; // max shift amount is 31
                            BigInteger orig = dest.getValue(state);
                            BigInteger result = orig.shiftLeft(shamt);

                            int msbIndex = this.opSize.numBits() - 1;

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);

                            if (shamt > 0 && (msbIndex + 1) >= shamt) {
                                flags.put("cf", orig.testBit((msbIndex + 1) - shamt));
                            } else if ((msbIndex + 1) >= shamt) {
                                flags.put("cf", false); // TODO: not sure if this is handled correctly
                            }
                            // overflow is only defined when shifting by 1
                            if (shamt == 1) {
                                flags.put("of", orig.testBit(msbIndex) != orig.testBit(msbIndex - 1));
                            } else {
                                // This is an undefined case... false sounds great
                                // doesn't it?
                                flags.put("of", false);
                            }

                            byte[] resArray = result.toByteArray();
                            if (resArray.length > this.opSize.numBytes()) {
                                byte[] ba = Arrays.copyOfRange(resArray, 1, resArray.length);
                                result = new BigInteger(ba);
                            }

                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "sar":
                this.type = InstructionType.SAR;
                this.operation
                        = (state, src, dest) -> {
                            int shamt = src.getValue(state).intValue() % 32; // max shift amount is 31
                            BigInteger orig = dest.getValue(state);
                            BigInteger result = orig.shiftRight(shamt);

                            if (result.bitLength() + 1 > this.opSize.numBits()) {
                                System.err.println("ERROR: shifting right made it bigger???");
                                System.exit(1);
                            }

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);

                            // overflow is false if shifting by 1, otherwise
                            // undefined
                            if (shamt == 1) {
                                flags.put("of", false);
                            } else {
                                // This is an undefined case... false sounds great
                                // doesn't it?
                                flags.put("of", false);
                            }

                            // shift by zero means CF isn't changed
                            if (shamt > 0) {
                                flags.put("cf", orig.testBit(shamt - 1));
                            }

                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "shr":
                this.type = InstructionType.SHR;
                this.operation
                        = (state, src, dest) -> {
                            int shamt = src.getValue(state).intValue() % 32; // max shift amount is 31
                            BigInteger orig = dest.getValue(state);

                            // BigInteger doesn't have logical right shift (>>>)
                            // so we do the ugly thing here and use the >>>
                            // operator by converting to the native types of the
                            // operators.
                            String s = null;
                            switch (this.opSize) {
                                case BYTE:
                                    byte b = orig.byteValue();
                                    b = (byte) (b >>> shamt);
                                    s = "" + b;
                                    break;
                                case WORD:
                                    short w = orig.shortValue();
                                    w = (short) (w >>> shamt);
                                    s = "" + w;
                                    break;
                                case LONG:
                                    int l = orig.intValue();
                                    l = l >>> shamt;
                                    s = "" + l;
                                    break;
                                case QUAD:
                                    long q = orig.longValue();
                                    q = q >>> shamt;
                                    s = "" + q;
                                    break;
                            }
                            BigInteger result = new BigInteger(s);

                            int signum = result.signum();
                            flags.put("zf", signum == 0);
                            flags.put("sf", signum == -1);

                            // overflow is the most sig bit of original if shifting by 1, otherwise
                            // undefined
                            if (shamt == 1) {
                                flags.put("of", orig.testBit(this.opSize.numBytes() * 8 - 1));
                            } else {
                                // This is an undefined case... false sounds great
                                // doesn't it?
                                flags.put("of", false);
                            }

                            if (shamt > 0) {
                                // shift by zero means CF isn't changed
                                flags.put("cf", orig.testBit(shamt - 1));
                            }

                            return dest.updateState(state, Optional.of(result), flags);
                        };
                break;
            case "mov":
                this.type = InstructionType.MOV;
                this.operation
                        = (state, src, dest) -> dest.updateState(state, Optional.of(src.getValue(state)), flags);
                break;
            case "lea":
                this.type = InstructionType.LEA;
                this.operation
                        = (state, src, dest) -> {
                            // TODO: Use polymorophism to avoid this instanceof junk
                            if (!(src instanceof MemoryOperand)) {
                                System.err.println("ERROR: lea src must be a memory operand");
                                return null;
                            }

                            MemoryOperand mo = (MemoryOperand) src;
                            return dest.updateState(state, Optional.of(BigInteger.valueOf(mo.calculateAddress(state))), flags);
                        };
                break;
            default:
                System.err.println("unknown instr type for binary inst: " + instType);
                System.exit(1);
        }
    }

    @Override
    public MachineState eval(MachineState state) {
        return operation.apply(state, this.source, this.destination);
    }

    @Override
    public Set<String> getUsedRegisters() {
        Set<String> sourceRegs = source.getUsedRegisters();
        Set<String> destRegs = destination.getUsedRegisters();
        sourceRegs.addAll(destRegs);
        return sourceRegs;
    }
    
    @Override
    public String toString() {
        return type + " " + source.toString() + ", " + destination.toString();
    }
}

/**
 * The type of an x86 Instruction.
 */
enum InstructionType {
    ADD(2),
    SUB(2),
    CMP(2),
    OR(2),
    AND(2),
    TEST(2),
    XOR(2),
    SHL(2),
    SAL(2),
    SHR(2),
    SRL(2),
    SAR(2),
    MOV(2),
    LEA(2),
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
    SETLE(1);
    // TODO: set instructions for unsigned (e.g. seta)

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

/**
 * Representation of the size of an x86 instruction or operand.
 */
enum OpSize {
    BYTE(1),
    WORD(2),
    LONG(4),
    QUAD(8);

    /**
     * The number of bytes used for this op.
     */
    private int numBytes;

    private OpSize(int nb) {
        this.numBytes = nb;
    }

    public int numBytes() {
        return this.numBytes;
    }

    public int numBits() {
        return this.numBytes * 8;
    }
}

/**
 * An abstract class representing an x86 operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
abstract class Operand {

    /**
     * @param state The state of the machine.
     * @return The value of the operand in a machine with the given state.
     */
    public abstract BigInteger getValue(MachineState state);

    /**
     * @param currState The current state of the machine.
     * @param val The value to update the operand with.
     * @param flags The condition flags to be set in the new state.
     * @return The state after updating the current state with the new value for
     * the operand.
     */
    public abstract MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags);

    public abstract Set<String> getUsedRegisters();
}

/**
 * A class representing an x86-64 register operand (e.g. %eax).
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class RegOperand extends Operand {

    /**
     * The name of the register, sans % (e.g. "eax")
     */
    private String regName;

    /**
     * The size of the operand.
     */
    private OpSize opSize;

    public RegOperand(String regName, OpSize opSize) {
        this.regName = regName;
        this.opSize = opSize;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        return state.getRegisterValue(regName);
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags) {
        return currState.getNewState(this.regName, val, flags);
    }

    @Override
    public Set<String> getUsedRegisters(){ 
        HashSet<String> s = new HashSet<String>();
        s.add(regName);
        return s;
    }
    
    @Override
    public String toString() {
        return "%" + regName;
    }
}

/**
 * A class representing an x86-64 memory operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class MemoryOperand extends Operand {

    /**
     * Name of the base register.
     */
    private String baseReg;

    /**
     * Name of the index register.
     */
    private String indexReg;

    /**
     * The scaling factor for the index register.
     */
    private int scale;

    /**
     * The offset amount.
     */
    private int offset;

    /**
     * The size of the operand.
     */
    private OpSize opSize;

    public MemoryOperand(String baseReg, String indexReg, int scale, int offset, OpSize opSize) {
        this.baseReg = baseReg;
        this.indexReg = indexReg;
        this.scale = scale;
        this.offset = offset;
        this.opSize = opSize;
    }

    /**
     * Calculate the effective address of the operand, given the specified
     * machine state.
     *
     * @param state The state in which to calculate the address.
     * @return The effective address.
     */
    public long calculateAddress(MachineState state) {
        /**
         * @tricky should this return BigInteger
         */
        long address = state.getRegisterValue(baseReg).add(BigInteger.valueOf(offset)).longValue();
        if (indexReg != null) {
            address += state.getRegisterValue(indexReg).multiply(BigInteger.valueOf(scale)).longValue();
        }

        return address;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        return state.getMemoryValue(calculateAddress(state), opSize.numBytes());
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags) {
        return currState.getNewState(calculateAddress(currState), val, opSize.numBytes(), flags);
    }
    
    @Override
    public Set<String> getUsedRegisters() {
        HashSet<String> s = new HashSet<String>();
        if (baseReg != null) // TODO: OPTIONAL!!!
            s.add(baseReg);
        if (indexReg != null)
            s.add(indexReg);
        return s;
    }

    @Override
    public String toString() {
        String res = offset + "(%" + baseReg;
        if (indexReg != null) {
            res += ", %" + indexReg + ", " + scale;
        }

        res += ")";
        return res;
    }
}

/**
 * A class representing an x86-64 constant (i.e. immediate) operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class ConstantOperand extends Operand {

    /**
     * The operand's value.
     */
    private long constant;

    public ConstantOperand(long val) {
        this.constant = val;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        return BigInteger.valueOf(constant);
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags) {
        System.err.println("Why are you trying to set a constant?");
        // TODO: exception here?
        return currState;
    }
    
    @Override
    public Set<String> getUsedRegisters() {
        HashSet<String> s = new HashSet<String>();
        return s;
    }

    @Override
    public String toString() {
        return "$" + constant;
    }
}

/**
 * A class representing the state of the machine, namely its register file and
 * memory.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class MachineState {

    /**
     * The register file.
     */
    private Map<String, byte[]> registers;

    /**
     * The machine's memory.
     */
    private Map<Long, Byte> memory;

    /**
     * The status flags (i.e. condition codes).
     */
    private Map<String, Boolean> statusFlags;

    /**
     * Create a new state with all registers (except %rsp) initialized to 0 but
     * no memory initialization. %rsp is initialized to 0x7FFFFFFF.
     */
    public MachineState() {
        this.registers = new HashMap<String, byte[]>();
        this.memory = new HashMap<Long, Byte>();
        this.statusFlags = new HashMap<String, Boolean>();

        String[] regNames = {"rax", "rbx", "rcx", "rdx", "rsi", "rdi", "rbp", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"};
        for (String s : regNames) {
            registers.put(s, new byte[8]);
        }

        long initRSP = 1 << 30;
        initRSP <<= 30;
        initRSP <<= 3;
        initRSP = ~initRSP;
        registers.put("rsp", ByteBuffer.allocate(8).putLong(initRSP).array()); // rsp = 0x7FFFFFFFFFFFFFFF

        String[] flagNames = {"zf", "sf", "of", "cf"};
        for (String s : flagNames) {
            statusFlags.put(s, false);
        }
    }

    public MachineState(Map<String, byte[]> reg, Map<Long, Byte> mem, Map<String, Boolean> flags) {
        this.registers = reg;
        this.memory = mem;
        this.statusFlags = flags;
    }

    // Getters for the status flags
    public boolean getCarryFlag() {
        return this.statusFlags.get("cf");
    }

    public boolean getOverflowFlag() {
        return this.statusFlags.get("of");
    }

    public boolean getZeroFlag() {
        return this.statusFlags.get("zf");
    }

    public boolean getSignFlag() {
        return this.statusFlags.get("sf");
    }

    /**
     * Create a new MachineState based on the current state but with an updated
     * value for a memory address.
     *
     * @param address The starting (i.e. lowest) address that will be changed.
     * @param val The new value of the given memory address.
     * @param size The number of bytes to write to memory.
     * @param flags The condition flags to modify for the new state.
     * @return A new state that is the same as the current but with new binding
     * from given address to given val.
     */
    public MachineState getNewState(long address, Optional<BigInteger> val, int size, Map<String, Boolean> flags) {
        Map<Long, Byte> mem = this.memory;

        if (val.isPresent()) {
            mem = new HashMap<Long, Byte>(this.memory);
            byte[] valArray = val.get().toByteArray();

            long dest = address + (valArray.length - 1);
            for (int src = 0;
                    src < valArray.length; src++, dest++) {
                mem.put(dest, valArray[src]);
            }

            for (long i = address + valArray.length; i < (address + size); i++) {
                if (val.get().signum() == -1) {
                    mem.put(i, (byte) 0xFF);
                } else {
                    mem.put(i, (byte) 0);
                }
            }
        }

        // TODO: remove code duplication (here and in other version of
        // getNewState.
        if (!flags.containsKey("zf")) {
            flags.put("zf", this.statusFlags.get("zf"));
        }
        if (!flags.containsKey("sf")) {
            flags.put("sf", this.statusFlags.get("sf"));
        }
        if (!flags.containsKey("of")) {
            flags.put("of", this.statusFlags.get("of"));
        }
        if (!flags.containsKey("cf")) {
            flags.put("cf", this.statusFlags.get("cf"));
        }

        return new MachineState(this.registers, mem, flags);
    }

    /**
     * @return Pair of the start (inclusive) and end (exclusive) indices for
     * given register in its full register's byte array.
     */
    private static Pair<Integer, Integer> getByteRange(String regName) {
        String quadRegNames = "^r(ax|bx|cx|dx|si|di|bp|sp|8|9|10|11|12|13|14|15)$";
        String longRegNames = "^(e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)d)$";
        String wordRegNames = "^((ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)w)$";
        String byteLowRegNames = "^((al|bl|cl|dl|sil|dil|bpl|spl)|r(8|9|10|11|12|13|14|15)b)$";
        String byteHighRegNames = "^(ah|bh|ch|dh)$";
        if (Pattern.matches(quadRegNames, regName)) {
            return new Pair<Integer, Integer>(0, 8);
        } else if (Pattern.matches(longRegNames, regName)) {
            return new Pair<Integer, Integer>(4, 8);
        } else if (Pattern.matches(wordRegNames, regName)) {
            return new Pair<Integer, Integer>(6, 8);
        } else if (Pattern.matches(byteLowRegNames, regName)) {
            return new Pair<Integer, Integer>(7, 8);
        } else if (Pattern.matches(byteHighRegNames, regName)) {
            return new Pair<Integer, Integer>(6, 7);
        } else {
            System.err.println("ERROR: Unknown register name: " + regName);
            System.exit(1);
            return null;
        }
    }

    private static String getQuadName(String regName) {
        String quadRegNames = "^r(ax|bx|cx|dx|si|di|bp|sp|8|9|10|11|12|13|14|15)$";
        String longRegNames = "^(e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)d)$";
        String wordRegNames = "^((ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)w)$";
        String byteLowRegNames = "^((al|bl|cl|dl|sil|dil|bpl|spl)|r(8|9|10|11|12|13|14|15)b)$";
        String byteHighRegNames = "^(ah|bh|ch|dh)$";
        if (Pattern.matches(quadRegNames, regName)) {
            return regName;
        } else if (Pattern.matches(longRegNames, regName)) {
            if (regName.charAt(0) == 'e') {
                return "r" + regName.substring(1);
            } else {
                return regName.substring(0, regName.length() - 1);
            }
        } else if (Pattern.matches(wordRegNames, regName)) {
            if (regName.charAt(0) != 'r') {
                return "r" + regName;
            } else // just strip off the "d" from the end
            {
                return regName.substring(0, regName.length() - 1);
            }
        } else if (Pattern.matches(byteLowRegNames, regName)) {
            if (regName.charAt(0) == 'r') {
                return regName.substring(0, regName.length() - 1);
            } else if (regName.length() == 2) {
                return "r" + regName.substring(0, regName.length() - 1) + "x";
            } else {
                return "r" + regName.substring(0, regName.length() - 1);
            }
        } else if (Pattern.matches(byteHighRegNames, regName)) {
            return "r" + regName.substring(0, regName.length() - 1) + "x";
        } else {
            System.err.println("ERROR: Unknown register name: " + regName);
            System.exit(1);
            return null;
        }
    }

    /**
     * Create a new MachineState based on the current state but with an updated
     * value for a register.
     *
     * @param regName The register that will be updated.
     * @param val The new value of the given register.
     * @param flags The condition flags to modify for the new state.
     * @return A new state that is the same as the current but with new binding
     * from given register to given val
     */
    public MachineState getNewState(String regName, Optional<BigInteger> val, Map<String, Boolean> flags) {
        Map<String, byte[]> reg = this.registers;
        if (val.isPresent()) {
            String quadName = getQuadName(regName);
            Pair<Integer, Integer> range = getByteRange(regName);
            int startIndex = range.getKey();
            int endIndex = range.getValue();

            reg = new HashMap<String, byte[]>(this.registers);
            byte[] valArray = val.get().toByteArray();
            byte[] newVal = new byte[endIndex - startIndex];

            for (int src = 0, dest = (newVal.length - valArray.length);
                    src < valArray.length; src++, dest++) {
                newVal[dest] = valArray[src];
            }

            if (val.get().signum() == -1) {
                for (int i = 0; i < newVal.length - valArray.length; i++) {
                    newVal[i] = (byte) 0xFF;
                }
            }

            byte[] newValFull = Arrays.copyOf(this.registers.get(quadName), 8);
            for (int src = 0, dest = startIndex; dest < endIndex; src++, dest++) {
                newValFull[dest] = newVal[src];
            }

            reg.put(quadName, newValFull);
        }

        // TODO: remove code duplication (here and in other version of
        // getNewState.
        if (!flags.containsKey("zf")) {
            flags.put("zf", this.statusFlags.get("zf"));
        }
        if (!flags.containsKey("sf")) {
            flags.put("sf", this.statusFlags.get("sf"));
        }
        if (!flags.containsKey("of")) {
            flags.put("of", this.statusFlags.get("of"));
        }
        if (!flags.containsKey("cf")) {
            flags.put("cf", this.statusFlags.get("cf"));
        }

        return new MachineState(reg, this.memory, flags);
    }

    /**
     * Gets the value stored in the given register.
     */
    public BigInteger getRegisterValue(String regName) {
        byte[] ba = null;

        String quadName = getQuadName(regName);
        Pair<Integer, Integer> range = getByteRange(regName);
        int startIndex = range.getKey();
        int endIndex = range.getValue();

        ba = registers.get(quadName);

        /*
		String longRegNames = "^(e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)d)$";
		String quadRegNames = "^r(ax|bx|cx|dx|si|di|bp|sp|8|9|10|11|12|13|14|15)$";
		String wordRegNames = "^((ax|bx|cx|dx|si|di|bp|sp)|r(8|9|10|11|12|13|14|15)w)$";
		String byteRegNames = "^((al|ah|bl|bh|cl|ch|dl|dh|sil|dil|bpl|spl)|r(8|9|10|11|12|13|14|15)b)$";
		if (Pattern.matches(quadRegNames, regName)) {
			ba = registers.get(regName);
		}
		else if (Pattern.matches(longRegNames, regName)) {
			startIndex = 4;

			if (regName.charAt(0) == 'e')
				ba = registers.get("r" + regName.substring(1));
			else
				ba = registers.get(regName.substring(0, regName.length()-1));
		}
		else {
			System.err.println("ERROR: WORD AND BYTE registers not yet supported.");
			System.exit(1);
		}
         */
        return new BigInteger(Arrays.copyOfRange(ba, startIndex, endIndex));
        //return new BigInteger(registers.get(regName));
    }

    /**
     * Gets the value stored at the given memory address.
     *
     * @param address The starting address where the value is stored.
     * @param size The number of bytes of memory to read.
     */
    public BigInteger getMemoryValue(long address, int size) {
        byte[] val = new byte[size];

        long addr = address + (size - 1);
        for (int dest = 0; dest < size; addr--, dest++) {
            val[dest] = memory.get(addr);
        }

        return new BigInteger(val);
    }

    /**
     */
    public List<Register> getRegisters() {
        ArrayList<Register> arr = new ArrayList<Register>();
        for (Map.Entry<String, byte[]> entry : registers.entrySet()) {
            BigInteger b = new BigInteger(entry.getValue());
            byte[] ba = b.toByteArray();
            String s = "0x";
            for (byte i : ba) {
                s += String.format("%02x", i);
            }
            arr.add(new Register(entry.getKey(), s));
           //System.out.println(entry.getKey() + "/" + entry.getValue());
        }
        return arr;
    }

    public String toString() {
        String s = "Registers:\n";
        for (Map.Entry<String, byte[]> entry : registers.entrySet()) {
            BigInteger b = new BigInteger(entry.getValue());
            byte[] ba = b.toByteArray();
            s += "\t" + entry.getKey() + ": " + b.toString() + " (0x";
            for (byte i : ba) {
                s += String.format("%02x", i);
            }
            s += ")\n";
        }

        s += "Status Flags:\n";
        for (Map.Entry<String, Boolean> entry : statusFlags.entrySet()) {
            s += "\t" + entry.getKey() + ": " + (entry.getValue() ? "1" : "0") + "\n";
        }

        s += "Memory:\n";
        for (Map.Entry<Long, Byte> entry : memory.entrySet()) {
            s += "\t" + Long.toHexString(entry.getKey()) + ": " + String.format("%02x", entry.getValue()) + "\n";
        }

        return s;
    }
}

@FunctionalInterface
interface BinaryX86Operation {

    MachineState apply(MachineState state, Operand src, Operand dest);
}

@FunctionalInterface
interface UnaryX86Operation {

    MachineState apply(MachineState state, Operand dest);
}

/**
 * Simple class to test x86 instruction parsing and evaluation.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
class x86InstructionTester {

    public static void main(String[] args) {
        ArrayList<x86Instruction> instructions = new ArrayList<x86Instruction>();
        instructions.add(x86Instruction.parseInstruction("movq $9, %rax"));
        instructions.add(x86Instruction.parseInstruction("movq $4, %rbx"));
        instructions.add(x86Instruction.parseInstruction("addq %rax, %rbx"));
        instructions.add(x86Instruction.parseInstruction("pushq %rbx"));
        instructions.add(x86Instruction.parseInstruction("popq %rcx"));
        instructions.add(x86Instruction.parseInstruction("leaq -12(%rsp), %rdx"));
        instructions.add(x86Instruction.parseInstruction("movl $73, (%rdx)"));
        instructions.add(x86Instruction.parseInstruction("incl %esi"));
        instructions.add(x86Instruction.parseInstruction("decl %edi"));

        // test that smaller register only affect part of the whole register
        instructions.add(x86Instruction.parseInstruction("movl $0, %edx"));
        instructions.add(x86Instruction.parseInstruction("movw $-1, %dx"));
        instructions.add(x86Instruction.parseInstruction("movb $2, %dl"));
        instructions.add(x86Instruction.parseInstruction("movb $3, %dh"));

        // tests for condition codes
        instructions.add(x86Instruction.parseInstruction("movl $0, %ebp"));
        instructions.add(x86Instruction.parseInstruction("movl $1, %ebp"));
        instructions.add(x86Instruction.parseInstruction("sall $31, %ebp"));
        instructions.add(x86Instruction.parseInstruction("decl %ebp"));
        instructions.add(x86Instruction.parseInstruction("addl $0, %ebp"));
        instructions.add(x86Instruction.parseInstruction("incl %ebp"));
        instructions.add(x86Instruction.parseInstruction("negl %ebp"));
        instructions.add(x86Instruction.parseInstruction("andl $0, %ebp"));
        instructions.add(x86Instruction.parseInstruction("notl %ebp"));
        instructions.add(x86Instruction.parseInstruction("shrl $1, %ebp"));

        // more LONG registers
        instructions.add(x86Instruction.parseInstruction("movl $1, %r8d"));
        instructions.add(x86Instruction.parseInstruction("sall $4, %r8d"));
        instructions.add(x86Instruction.parseInstruction("sarl $3, %r8d"));

        // tests for cmp, test, and set instructions
        instructions.add(x86Instruction.parseInstruction("movl $-5, %eax"));
        instructions.add(x86Instruction.parseInstruction("cmpl $-5, %eax"));
        instructions.add(x86Instruction.parseInstruction("setge %bl"));

        // TODO: more tests for cmp, test, and set instructions
        MachineState state = new MachineState();
        System.out.println(state);
        for (x86Instruction inst : instructions) {
            System.out.println(inst);
            state = inst.eval(state);
            System.out.println(state);
        }
    }
}
