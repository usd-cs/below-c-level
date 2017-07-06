package reversey.backy;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.util.Pair;

/**
 * Class for parsing X86-64 programs.
 */
public class X86Parser {

    private static final String qRegNames = "r(ax|bx|cx|dx|si|di|bp|sp|8|9|1[0-5])";
    private static final String lRegNames = "e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|1[0-5])d";
    private static final String wRegNames = "(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|1[0-5])w";
    private static final String bRegNames = "(al|ah|bl|bh|cl|ch|dl|dh|sil|dil|bpl|spl)|r(8|9|1[0-5])b";
    private static final String allRegNames = "(" + qRegNames + "|" + lRegNames + "|" + wRegNames + "|" + bRegNames + ")";

    private static final String constOpRegEx = "\\$(?<const>-?\\p{Digit}+)";
    private static final String regOpRegEx = "\\%(?<regName>\\p{Alnum}+)";
    private static final String memOpRegEx = "(?<imm>-?\\p{Digit}+)?\\s*(?!\\(\\s*\\))\\(\\s*(%(?<base>\\p{Alnum}+))?\\s*(,\\s*%(?<index>\\p{Alnum}+)\\s*(,\\s*(?<scale>\\p{Digit}+))?)?\\s*\\)";
    private static final String labelOpEx = "(?<label>\\w+)";
    private static final String operandRegEx = "\\s*(?<operand>" + constOpRegEx + "|" + regOpRegEx + "|" + memOpRegEx + "|" + labelOpEx + ")\\s*";

    
    private static int currLineNum = 0;
    private static Map<String, Label> labels = new HashMap<String, Label>();

    /**
     * Checks that instruction is a valid, supported x86 instruction.
     *
     * @param instrName The name of the instruction (e.g. addl)
     * @return A pair containing both the instruction type and op size unary
     * instruction.
     */
    public static Pair<InstructionType, OpSize> parseTypeAndSize(String instrName) throws X86ParsingException {
        InstructionType type;
        OpSize size;

        String validTypedInstrNames = "(?<name>add|sub|xor|or|and|shl|sal|shr|sar|mov|lea|inc|dec|neg|not|push|pop|cmp|test)(?<size>b|w|l|q)";
        String validSetInstrNames = "(?<name>set|j)(?<op>e|ne|s|ns|g|ge|l|le)";
        
        Matcher typedMatcher = Pattern.compile(validTypedInstrNames).matcher(instrName);
        Matcher setMatcher = Pattern.compile(validSetInstrNames).matcher(instrName);
            
        if (typedMatcher.matches()) {
            type = InstructionType.valueOf(typedMatcher.group("name").toUpperCase());
            switch (typedMatcher.group("size")) {
                case "b":
                    size = OpSize.BYTE;
                    break;
                case "w":
                    size = OpSize.WORD;
                    break;
                case "l":
                    size = OpSize.LONG;
                    break;
                case "q":
                    size = OpSize.QUAD;
                    break;
                default:
                    throw new X86ParsingException("unexpected size suffix", typedMatcher.start("size"), instrName.length());
            }
        } else if (setMatcher.matches()) {
            // The SET instruction is implicitly BYTE sized.
            System.out.println("instrName: "+ instrName);
            type = InstructionType.valueOf(instrName.toUpperCase());
            size = OpSize.BYTE;
        } else {
            throw new X86ParsingException("invalid/unsupported instruction", 0, instrName.length());
        }

        return new Pair<InstructionType, OpSize>(type, size);
    }

    /**
     * Get the size of the register with the given name.
     *
     * @param name The register's name
     * @return The size of the register.
     */
    public static OpSize getRegisterSize(String name) throws X86ParsingException {
        OpSize opSize = OpSize.BYTE;
        if (name.matches(lRegNames)) {
            opSize = OpSize.LONG;
        } else if (name.matches(qRegNames)) {
            opSize = OpSize.QUAD;
        } else if (name.matches(wRegNames)) {
            opSize = OpSize.WORD;
        } else if (name.matches(bRegNames)) {
            opSize = OpSize.BYTE;
        } else {
            throw new X86ParsingException("invalid register name", 0, name.length());
        }

        return opSize;
    }

    /**
     * Construct an operand based on a given string.
     *
     * @param String that contains the operand at the beginning.
     * @return The parsed operand.
     */
    public static Operand parseOperand(String str, OpSize instrOpSize) throws X86ParsingException {
        Operand op = null;

        Matcher constMatcher = Pattern.compile(constOpRegEx).matcher(str);
        Matcher regMatcher = Pattern.compile(regOpRegEx).matcher(str);
        Matcher memMatcher = Pattern.compile(memOpRegEx).matcher(str);
        Matcher labelMatcher = Pattern.compile(labelOpEx).matcher(str);

        if (constMatcher.matches()) {
            // constant operand
            op = new ConstantOperand(Integer.parseInt(constMatcher.group("const"))); // TODO: handle hex
        } else if (regMatcher.matches()) {
            // register operand
            String[] splits = str.split(",");

            String regName = regMatcher.group("regName");

            OpSize opSize = null;
            try {
                opSize = getRegisterSize(regName);
            } catch (X86ParsingException e) {
                throw new X86ParsingException(e.getMessage(),
                        regMatcher.start("regName") + e.getStartIndex(),
                        regMatcher.start("regName") + e.getEndIndex());
            }

            // TODO: move this to a "validation" method
            if (opSize != instrOpSize) {
                throw new X86ParsingException("op size mismatch", regMatcher.start("regName"), regMatcher.end("regName"));
            }

            op = new RegOperand(regName, opSize);
        } else if (memMatcher.matches()) {
            // memory operand

            // All components (e.g. offset or base reg) are optional, although
            // at least one of them must be set.
            int offset = 0;
            String offsetStr = memMatcher.group("imm");
            if (offsetStr != null) {
                offset = Integer.parseInt(offsetStr); // TODO: handle hex
            }

            String baseReg = memMatcher.group("base");
            if (baseReg != null) {
                OpSize baseOpSize = null;
                try {
                    baseOpSize = getRegisterSize(baseReg);
                } catch (X86ParsingException e) {
                    throw new X86ParsingException(e.getMessage(),
                            memMatcher.start("base") + e.getStartIndex(),
                            memMatcher.start("base") + e.getEndIndex());
                }
                if (baseOpSize != OpSize.QUAD) {
                    throw new X86ParsingException("base register must be quad sized",
                            memMatcher.start("base"),
                            memMatcher.end("base"));
                }
            }

            String indexReg = memMatcher.group("index");
            if (indexReg != null) {
                OpSize indexOpSize = null;
                try {
                    indexOpSize = getRegisterSize(indexReg);
                } catch (X86ParsingException e) {
                    throw new X86ParsingException(e.getMessage(),
                            memMatcher.start("index") + e.getStartIndex(),
                            memMatcher.start("index") + e.getEndIndex());
                }

                if (indexOpSize != OpSize.QUAD) {
                    throw new X86ParsingException("index register must be quad sized",
                            memMatcher.start("index"),
                            memMatcher.end("index"));
                }
            }

            int scale = 1;
            String scaleStr = memMatcher.group("scale");
            if (scaleStr != null) {
                scale = Integer.parseInt(scaleStr);
                if (scale != 1 && scale != 2 && scale != 4 && scale != 8) {
                    throw new X86ParsingException("invalid scaling factor",
                            memMatcher.start("scale"),
                            memMatcher.end("scale"));
                }
            }

            op = new MemoryOperand(baseReg, indexReg, scale, offset, instrOpSize);
        } else if (labelMatcher.matches()) {
            String labelName = labelMatcher.group("label");
                op = new LabelOperand(labelName, labels.get(labelName));
                System.out.println("Found a label operand");
            // Instruction validation - allow instructions with labels that have not been created yet
            // What happens when instruction with jmp label is executed but jumps to a label that hasn't been created yet?
    } else {
            System.out.println("Unknown type of operand");
            System.out.println("Tried to match " + str);
        }
        return op;
    }

    public static List<Operand> parseOperands(String operandsStr, OpSize opSize) throws X86ParsingException {
        List<Operand> operands = new ArrayList<Operand>();

        Matcher m = Pattern.compile(operandRegEx).matcher(operandsStr);
        if (!m.find()) {
            return operands;
        }

        int nextIndex = -1;
        try {
            String opStr = m.group("operand");
            Operand op = parseOperand(opStr, opSize);
            nextIndex = m.end();

            operands.add(op);

            m = Pattern.compile("," + operandRegEx).matcher(operandsStr);

            while (m.find(nextIndex)) {
                opStr = m.group("operand");
                op = parseOperand(opStr, opSize);
                nextIndex = m.end();
                operands.add(op);
            }
        } catch (X86ParsingException e) {
            throw new X86ParsingException(e.getMessage(),
                    m.start("operand") + e.getStartIndex(),
                    m.start("operand") + e.getEndIndex());
        }

        if (nextIndex != operandsStr.length()) {
            throw new X86ParsingException("unexpected value", nextIndex, operandsStr.length());
        }

        return operands;
    }

    /**
     * Create an x86-64 instruction by parsing a given string.
     *
     * @param instr A string representation of the instruction.
     * @return The parsed instruction.
     */
    public static x86ProgramLine parseLine(String instr) throws X86ParsingException {
        Matcher instMatcher = Pattern.compile("\\s*(?<inst>\\S+)\\s+(?<operands>.*)").matcher(instr);
        Matcher labelMatcher = Pattern.compile("\\s*(?<label>\\w+):\\s*").matcher(instr);

        if (!instMatcher.matches() && !labelMatcher.matches()) {
            throw new X86ParsingException("nonsense input", 0, instr.length());
        }

        if (instMatcher.matches()) {
            String instrName = instMatcher.group("inst");

            Pair<InstructionType, OpSize> instDetails = null;
            try {
                instDetails = parseTypeAndSize(instrName);
            } catch (X86ParsingException e) {
                throw new X86ParsingException(e.getMessage(),
                        instMatcher.start("inst") + e.getStartIndex(),
                        instMatcher.start("inst") + e.getEndIndex());
            }

            InstructionType instrType = instDetails.getKey();
            OpSize opSize = instDetails.getValue();

            String operandsStr = instMatcher.group("operands");

            List<Operand> operands = null;
            try {
                operands = parseOperands(operandsStr, opSize);
            } catch (X86ParsingException e) {
                throw new X86ParsingException(e.getMessage(),
                        instMatcher.start("operands") + e.getStartIndex(),
                        instMatcher.start("operands") + e.getEndIndex());
            }

            if (operands.size() != instrType.numOperands()) {
                System.out.println("Num operands: " + operands.size());
                throw new X86ParsingException("too many operands",
                        instMatcher.start("operands"),
                        instr.length());
            } else if (instrType.numOperands() == 2) {
                if (operands.get(1) instanceof ConstantOperand) {
                    // FIXME: start/end index is not right here
                    throw new X86ParsingException("destination cannot be a constant",
                            instMatcher.start("operands"),
                            instr.length());
                }
                return new x86BinaryInstruction(instrName.substring(0, instrName.length() - 1),
                        operands.get(0),
                        operands.get(1),
                        opSize,
                        currLineNum++);
            } else if (instrType.numOperands() == 1) {
                if (!instrName.startsWith("set") && !instrName.startsWith("j")) {
                    instrName = instrName.substring(0, instrName.length() - 1);
                }

                // TODO: throw exception if destination is a constant
                return new x86UnaryInstruction(instrName,
                        operands.get(0),
                        opSize,
                        currLineNum++);
            } else {
                throw new X86ParsingException("unsupported instruction type", 0, instrName.length());
            }
        } else {
            String labelName = labelMatcher.group("label");
            if (labels.containsKey(labelName)) {
                System.out.println("Duplicate label");
                System.exit(1);
            }

            Label l = new Label(labelName, currLineNum++);
            labels.put(labelName, l);
            return l;
        }
    }
}
