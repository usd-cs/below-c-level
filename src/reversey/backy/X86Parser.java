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

    // Regular expressions used for parsing registers
    private static final String qRegNames = "r(ax|bx|cx|dx|si|di|bp|sp|8|9|1[0-5])";
    private static final String lRegNames = "e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|1[0-5])d";
    private static final String wRegNames = "(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|1[0-5])w";
    private static final String bRegNames = "(al|ah|bl|bh|cl|ch|dl|dh|sil|dil|bpl|spl)|r(8|9|1[0-5])b";
    private static final String allRegNames = "(" + qRegNames + "|" + lRegNames + "|" + wRegNames + "|" + bRegNames + ")";

    // Regular expressions used for parsing operands
    private static final String constOpRegEx = "\\$(?<const>-?\\p{Digit}+)";
    private static final String regOpRegEx = "\\%(?<regName>\\p{Alnum}+)";
    private static final String memOpRegEx = "(?<imm>-?\\p{Digit}+)?\\s*(?!\\(\\s*\\))\\(\\s*(%(?<base>\\p{Alnum}+))?\\s*(,\\s*%(?<index>\\p{Alnum}+)\\s*(,\\s*(?<scale>\\p{Digit}+))?)?\\s*\\)";
    private static final String labelOpEx = "(?<label>\\w+)";
    private static final String operandRegEx = "\\s*(?<operand>" + constOpRegEx + "|" + regOpRegEx + "|" + memOpRegEx + "|" + labelOpEx + ")\\s*";

    /**
     * The line number that will be given to the next parsed line.
     */
    private static int currLineNum = 0;

    /**
     * Map for keeping track of all the labels we have parsed so far.
     */
    private static final Map<String, x86Label> labels = new HashMap<>();

    private static final Map<String, List<x86Instruction>> labelUsers = new HashMap<>();

    /**
     * Checks that instruction is a valid, supported x86 instruction.
     *
     * @param instrName The name of the instruction (e.g. addl)
     * @return A pair containing both the instruction type and op size unary
     * instruction.
     * @throws X86ParsingException If it is not a valid instruction or if the
     * size suffix is invalid.
     */
    public static Pair<InstructionType, OpSize> parseTypeAndSize(String instrName) throws X86ParsingException {
        InstructionType type;
        OpSize size;

        /* 
         * "sized" instructions are those that have an instruction name (e.g.
         * "add") followed by a single character suffix to indicate the size
         * (e.g. "q").
         */
        String validSizedInstrNames = "(?<name>add|sub|xor|or|and|shl|sal|shr|sar|mov|lea|inc|dec|neg|not|push|pop|cmp|test|call|ret)(?<size>b|w|l|q)";
        Matcher sizedInstrMatcher = Pattern.compile(validSizedInstrNames).matcher(instrName);

        /*
         * "conditional" instructions are those whose operations are determined
         * by the status flags (e.g. the overflow flag).
         * Their suffix isn't a size, rather it is the condition to check for
         * (e.g. "ge" for "greater than or equal")
         * The "size" of these instructions is implicit (e.g. byte for SET).
         */
        String validConditionalInstrName = "((?<name>set|j)(?<op>e|ne|s|ns|g|ge|l|le)|jmp)";
        Matcher condInstrMatcher = Pattern.compile(validConditionalInstrName).matcher(instrName);

        if (sizedInstrMatcher.matches()) {
            type = InstructionType.valueOf(sizedInstrMatcher.group("name").toUpperCase());

            // some instructions can only be quad sized so check for that first
            if (sizedInstrMatcher.group("name").matches("(lea|push|pop|call|ret)")
                    && !sizedInstrMatcher.group("size").equals("q")) {
                throw new X86ParsingException("instruction must have quad suffix (i.e. q)",
                        sizedInstrMatcher.start("size"),
                        instrName.length());
            }

            switch (sizedInstrMatcher.group("size")) {
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
                    throw new X86ParsingException("unexpected size suffix",
                            sizedInstrMatcher.start("size"),
                            instrName.length());
            }
        } else if (condInstrMatcher.matches()) {
            // The SET instruction is implicitly BYTE sized.
            // The JUMP instructions don't really have a size so BYTE is
            // arbitrarily chosen.
            type = InstructionType.valueOf(instrName.toUpperCase());
            size = OpSize.BYTE;
        } else {
            throw new X86ParsingException("invalid/unsupported instruction", 0, instrName.length());
        }

        return new Pair<>(type, size);
    }

    /**
     * Get the size of the register with the given name.
     *
     * @param name The register's name
     * @return The size of the register.
     * @throws X86ParsingException The string did not contain a valid register
     * name.
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
     * @param str String containing the operand at the beginning.
     * @param instrOpSize The size of the instruction that will use this *
     * operand.
     * @return The parsed operand.
     * @throws X86ParsingException There was an error parsing the string.
     */
    public static Operand parseOperand(String str, OpSize instrOpSize) throws X86ParsingException {
        Operand op = null;

        Matcher constMatcher = Pattern.compile(constOpRegEx).matcher(str);
        Matcher regMatcher = Pattern.compile(regOpRegEx).matcher(str);
        Matcher memMatcher = Pattern.compile(memOpRegEx).matcher(str);
        Matcher labelMatcher = Pattern.compile(labelOpEx).matcher(str);

        if (constMatcher.matches()) {
            // Found a constant operand
            op = new ConstantOperand(Integer.parseInt(constMatcher.group("const"))); // TODO: handle hex
        } else if (regMatcher.matches()) {
            // Found a register operand
            String regName = regMatcher.group("regName");

            OpSize opSize = null;
            try {
                opSize = getRegisterSize(regName);
            } catch (X86ParsingException e) {
                throw new X86ParsingException(e.getMessage(),
                        regMatcher.start("regName") + e.getStartIndex(),
                        regMatcher.start("regName") + e.getEndIndex());
            }

            // Make sure the size of this register doesn't conflict with the
            // size the instruction uses/wants.
            // TODO: move this check to a "validation" method
            if (opSize != instrOpSize) {
                throw new X86ParsingException("op size mismatch", regMatcher.start("regName"), regMatcher.end("regName"));
            }

            op = new RegOperand(regName, opSize);
        } else if (memMatcher.matches()) {
            // Found a memory operand

            // All components (e.g. offset or base reg) are optional, although
            // at least one of them must be set.
            // Note that our regular expression should eliminate the possiblity
            // of getting a memory operand with no components present.
            // Look for an offset, which can be any integral value
            int offset = 0;
            String offsetStr = memMatcher.group("imm");
            if (offsetStr != null) {
                offset = Integer.parseInt(offsetStr); // TODO: handle hex
            }

            // Look for a base register, which should be a quad sized register
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

            // Look for an index register, which should be a quad sized register
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

            // Look for a scaling factor, which should be 1, 2, 4, or 8
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
            // Found a label operand
            String labelName = labelMatcher.group("label");
            op = new LabelOperand(labelName, labels.get(labelName));
            // TODO: Instruction validation - allow instructions with labels that have not been created yet
            // What happens when instruction with jmp label is executed but jumps to a label that hasn't been created yet?
        } else {
            // TODO: throw X86ParsingException here
            System.out.println("ERROR: Unknown type of operand.");
            System.out.println("\t Tried to match " + str);
            System.exit(1);
        }
        return op;
    }

    /**
     * Parse all the operands in the given string. These operands should be
     * comma separated.
     *
     * @param operandsStr The string to parse for operands.
     * @param opSize The expected size of the operand.
     * @return The list of operands that were parsed.
     * @throws X86ParsingException There was a problem parsing the operands.
     */
    public static List<Operand> parseOperands(String operandsStr, OpSize opSize) throws X86ParsingException {
        List<Operand> operands = new ArrayList<>();

        Matcher m = Pattern.compile(operandRegEx).matcher(operandsStr);
        if (!m.find()) {
            return operands;
        }

        int nextIndex = -1;
        try {
            // Parse the first operand
            String opStr = m.group("operand");
            Operand op = parseOperand(opStr, opSize);
            nextIndex = m.end();

            operands.add(op);

            // Update pattern to include the comma separator for the following
            // operands
            m = Pattern.compile("," + operandRegEx).matcher(operandsStr);

            // Keep parsing operands until we don't find any more
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

        // Make sure there isn't any leftover cruft after the last parsed operand
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
     * @throws X86ParsingException There was a problem parsing the line.
     */
    public static x86ProgramLine parseLine(String instr) throws X86ParsingException {
        Matcher instMatcher = Pattern.compile("\\s*(?<inst>\\p{Alpha}+)(\\s+(?<operands>.*))?").matcher(instr);
        Matcher labelMatcher = Pattern.compile("\\s*(?<label>\\w+):\\s*").matcher(instr);

        // The line should be either a label or an instruction
        if (!instMatcher.matches() && !labelMatcher.matches()) {
            throw new X86ParsingException("nonsense input", 0, instr.length());
        }

        if (instMatcher.matches()) {
            // This line contains an X86 instruction.

            // Step 1: Get the name of the instruction and use that to determine
            // the type (i.e. what operation it is performing) and size of the
            // instruction.
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

            // Step 2: Parse the operands (putting them into a list) then use
            // those operands plus the instruction type to create a new
            // X86Instruction.
            String operandsStr = instMatcher.group("operands");
            if (operandsStr != null) {

                List<Operand> operands = null;
                try {
                    operands = parseOperands(operandsStr, opSize);
                } catch (X86ParsingException e) {
                    throw new X86ParsingException(e.getMessage(),
                            instMatcher.start("operands") + e.getStartIndex(),
                            instMatcher.start("operands") + e.getEndIndex());
                }

                if (operands.size() != instrType.numOperands()) {
                    throw new X86ParsingException("wrong number of operands",
                            instMatcher.start("operands"),
                            instr.length());
                } else if (instrType.numOperands() == 2) {
                    // This is a binary instruction (e.g. add), then second operand
                    // should be something we can write to (i.e. not a constant or a
                    // label)
                    if (operands.get(1) instanceof ConstantOperand) {
                        // FIXME: start/end index is not right here
                        throw new X86ParsingException("destination cannot be a constant",
                                instMatcher.start("operands"),
                                instr.length());
                    }
                    // TODO: check that the destination isn't a LabelOperand either
                    return new x86BinaryInstruction(instrType,
                            operands.get(0),
                            operands.get(1),
                            opSize,
                            currLineNum++);
                } else if (instrType.numOperands() == 1) {
                    // TODO: throw exception if destination is a constant (or a
                    // label for non-jump instructions)
                    x86UnaryInstruction inst = new x86UnaryInstruction(instrType,
                            operands.get(0),
                            opSize,
                            currLineNum++);

                    if (operands.get(0) instanceof LabelOperand) {
                        LabelOperand lo = (LabelOperand) operands.get(0);
                        String loName = lo.getName();
                        if (labelUsers.containsKey(loName)) {
                            labelUsers.get(loName).add(inst);
                        } else {
                            List<x86Instruction> l = new ArrayList<>();
                            l.add(inst);
                            labelUsers.put(loName, l);
                        }
                    }
                    return inst;
                }
                return null; // FIXME: throw exception
            } else {
                // nullary skullduggery
                return new x86NullaryInstruction(instrType,
                        opSize,
                        currLineNum++);
            }
        } else {
            // This line contains a label
            String labelName = labelMatcher.group("label");

            // Make sure this label doesn't already exist
            if (labels.containsKey(labelName)) {
                System.out.println("Duplicate label: " + labelName);
                System.exit(1);
                throw new X86ParsingException("Duplicate label name",
                        labelMatcher.start("label"),
                        labelMatcher.end("label"));
            }

            x86Label l = new x86Label(labelName, currLineNum++);
            labels.put(labelName, l);
            if (labelUsers.containsKey(labelName)) {
                labelUsers.get(labelName).forEach((inst) -> {
                    inst.updateLabels(labelName, l);
                });
            }
            return l;
        }
        // TODO: allow lines that contain both a label and an instruction?
    }

    // TODO: Find a better solution for this
    public static void clear() {
        labels.clear();
    }
}
