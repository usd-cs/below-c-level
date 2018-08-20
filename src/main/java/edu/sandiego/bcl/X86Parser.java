package edu.sandiego.bcl;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.mifmif.common.regex.*;
import info.debatty.java.stringsimilarity.*;

/**
 * Class for parsing X86-64 programs.
 */
public class X86Parser {

    // Regular expressions used for parsing registers
    private static final String QUAD_REG_REGEX = "r(ax|bx|cx|dx|si|di|bp|sp|8|9|1[0-5])";
    private static final String LONG_REG_REGEX = "e(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|1[0-5])d";
    private static final String WORD_REG_REGEX = "(ax|bx|cx|dx|si|di|bp|sp)|r(8|9|1[0-5])w";
    private static final String BYTE_REG_REGEX = "(al|ah|bl|bh|cl|ch|dl|dh|sil|dil|bpl|spl)|r(8|9|1[0-5])b";
    private static final String ALL_REG_REGEX = "(" 
            + QUAD_REG_REGEX 
            + "|" + LONG_REG_REGEX 
            + "|" + WORD_REG_REGEX 
            + "|" + BYTE_REG_REGEX 
            + ")";

    // Regular expressions used for parsing operands
    private static final String DECIMAL_CONST_REGEX = "-?(?!0x)\\p{Digit}+";
    private static final String HEX_CONST_REGEX = "-?0x\\p{XDigit}+";
    
    private static final String CONST_OPERAND_REGEX = "\\$?(?<const>" 
            + DECIMAL_CONST_REGEX 
            + "|" + HEX_CONST_REGEX 
            + ")";
    private static final String REGISTER_OPERAND_REGEX = "\\%(?<regName>\\p{Alnum}+)";
    private static final String MEM_OPERAND_REGEX = 
            "(?<imm>\\$?" // "$" is "allowed" here for better error reporting.
            + DECIMAL_CONST_REGEX + "|" + HEX_CONST_REGEX 
            + ")?" // immediate is optional
            + "\\s*"
            + "(?!\\(\\s*\\))" // Don't allow empty parens string, i.e. "()"
            + "\\(\\s*(%(?<base>\\p{Alnum}+))?" // base register (optional)
            + "\\s*(,\\s*%(?<index>\\p{Alnum}+)" // index register
            + "\\s*(,\\s*(?<scale>\\$?\\p{Digit}+))" // scaling factor (again, "$" allowed only for better error checking)
            + "?)?" // both index and scaling factor are optional
            + "\\s*\\)";
    private static final String LABEL_OPERAND_REGEX = "(?<label>[\\.\\p{Alpha}][\\.\\w]*)";
    
    // ordering is important here: constant must go after mem
    private static final String OPERAND_REGEX = "\\s*(?<operand>"
            + MEM_OPERAND_REGEX 
            + "|" + REGISTER_OPERAND_REGEX 
            + "|" + LABEL_OPERAND_REGEX 
            + "|" + CONST_OPERAND_REGEX
            + ")(?=\\s+|,|$)";
    
    private static final String ONE_SUFFIX_INSTRUCTIONS_REGEX = 
            "add|sub|imul"
                + "|idiv|xor|or|and|shl|sal|shr|sar"
                + "|mov|lea|inc|dec|neg|not|push|pop|cmp|test|call|ret|clt";
    private static final String TWO_SUFFIX_INSTRUCTIONS_REGEX = "movz|movs";
    private static final String CONDITIONAL_INSTRUCTIONS_REGEX = "set|j|cmov";

    /**
     * The line number that will be given to the next parsed line.
     */
    private int currLineNum;
    
    /**
     * Map for keeping track of all the labelFromName we have parsed so far.
     */
    private final Map<String, x86Label> labelFromName;

    /**
     * Map for tracking all the instructions that use a label with a specific name.
     */
    private final Map<String, List<x86Instruction>> labelUsersFromName;
    
    /**
     * Object to construct operands for instructions.
     */
    private OperandGetter operandGetter;

    public X86Parser(){
        this.currLineNum = 0;
        this.labelFromName = new HashMap<>();
        this.labelUsersFromName = new HashMap<>();
        this.operandGetter = new x86OperandGetter();
    }

    /**
     * Class to represent information about the instruction being parsed,
     * including it's type, size, and operand requirements.
     */
    private class TypeAndOpRequirements {
        public final InstructionType type;
        public final OpSize instrSize;
        public final List<OperandRequirements> operandReqs;
        
        public TypeAndOpRequirements(InstructionType type, OpSize instrSize, List<OperandRequirements> opReqs) {
            this.type = type;
            this.instrSize = instrSize;
            this.operandReqs = opReqs;
        }
    }
    
    /**
     * Checks that instruction is a valid, supported x86 instruction.
     *
     * @param instrName The name of the instruction (e.g. addl)
     * @return Object containing the type and the operand requirements for this instruction.
     * @throws X86ParsingException If it is not a valid instruction or if the
     * size suffix is invalid.
     */
    private Optional<TypeAndOpRequirements> parseTypeAndSize(String instrName) throws X86ParsingException {
        InstructionType type;
        OpSize size;
        List<OpSize> opSizes = new ArrayList<>();
        
        /* 
         * "sized" instructions are those that have an instruction name (e.g.
         * "add") followed by a single character suffix to indicate the size
         * (e.g. "q").
         */
        String validSizedInstrNames = "(?<name>" 
                + ONE_SUFFIX_INSTRUCTIONS_REGEX 
                + ")(?<size>b|w|l|q)";
        Matcher sizedInstrMatcher = Pattern.compile(validSizedInstrNames).matcher(instrName);
        
        /*
         * "two sizes" instructions are those that have an instruction name followed
         * by two characters that indicate the size of two operands (e.g. "bl")
         */
        String validTwoSizedInstrNames = "(?<name>" 
                + TWO_SUFFIX_INSTRUCTIONS_REGEX
                + ")(?<suffices>b[wlq]|w[lq]|lq)";
        Matcher twoSizedInstrMatcher = Pattern.compile(validTwoSizedInstrNames).matcher(instrName);

        /*
         * "conditional" instructions are those whose operations are determined
         * by the status flags (e.g. the overflow flag).
         * Their suffix isn't a size, rather it is the condition to check for
         * (e.g. "ge" for "greater than or equal")
         * The "size" of these instructions is implicit (e.g. byte for SET).
         */
        String validConditionalInstrName = "(jmp|(?<name>" 
                + CONDITIONAL_INSTRUCTIONS_REGEX 
                + ")(?<op>e|ne|s|ns|g|ge|l|le|a|ae|b|be))";
        Matcher condInstrMatcher = Pattern.compile(validConditionalInstrName).matcher(instrName);
        
        String invalidSuffix = "(?<name>"
                + TWO_SUFFIX_INSTRUCTIONS_REGEX // this must come before sizedInstructions
                + "|" + ONE_SUFFIX_INSTRUCTIONS_REGEX
                + "|" + "jmp" // this must come before conditionalInstructions
                + "|" + CONDITIONAL_INSTRUCTIONS_REGEX
                + ")"
                + "(?<suffix>\\p{Alpha}+)";
        Matcher invalidSuffixMatcher = Pattern.compile(invalidSuffix).matcher(instrName);

        String quadOnlyInstructions = "lea|push|pop|call|ret|clt";
        if (sizedInstrMatcher.matches()) {
            type = InstructionType.valueOf(sizedInstrMatcher.group("name").toUpperCase());

            // some instructions can only be quad sized so check for that first
            if (sizedInstrMatcher.group("name").matches("(" + quadOnlyInstructions + ")")
                    && !sizedInstrMatcher.group("size").equals("q")) {
                throw new X86ParsingException("Invalid suffix. Must be q.",
                        sizedInstrMatcher.start("size"),
                        instrName.length());
            }
            
            size = OpSize.getOpSizeFromAbbrev(sizedInstrMatcher.group("size"));
            opSizes.add(size);
            
        } else if (twoSizedInstrMatcher.matches()) {
            type = InstructionType.valueOf(twoSizedInstrMatcher.group("name").toUpperCase());
            String suffix1 = twoSizedInstrMatcher.group("suffices").substring(0,1);
            String suffix2 = twoSizedInstrMatcher.group("suffices").substring(1);
            
            // movzlq doesn't exist because movl automatically zero extends 
            // to fill the full quad register.
            if (type == InstructionType.MOVZ 
                    && suffix1.equals("l") && suffix2.equals("q")) {
                throw new X86ParsingException("MOVZ does not have an lq variant.",
                        twoSizedInstrMatcher.start("suffices"),
                        twoSizedInstrMatcher.end("suffices"));
            }
            
            opSizes.add(OpSize.getOpSizeFromAbbrev(suffix1));
            size = OpSize.getOpSizeFromAbbrev(suffix2);
            opSizes.add(size);
            
        } else if (condInstrMatcher.matches()) {
            type = InstructionType.valueOf(instrName.toUpperCase());

            
            if (instrName.startsWith("cmov")) {
                // The CMOV instruction doesn't have an explicit size. It's
                // size needs to be inferred from the size of the operands.
                size = OpSize.INFERRED;
            }
            else {
                // The SET instruction is implicitly BYTE sized.
                // The JUMP instructions don't really have a size so BYTE is
                // arbitrarily chosen.
                size = OpSize.BYTE;
            }
        } else if (invalidSuffixMatcher.matches()) {
            String errorMessage = "Invalid suffix.";
            
            if (instrName.matches("^(" + quadOnlyInstructions + ").*")) {
                errorMessage += " Must be q.";
            }
            else if (instrName.matches("^(" + ONE_SUFFIX_INSTRUCTIONS_REGEX + ")"
                    + invalidSuffixMatcher.group("suffix"))) {
                errorMessage += " Need one suffix: b, w, l, or q";
            }
            else if (instrName.matches("^(" + TWO_SUFFIX_INSTRUCTIONS_REGEX + ").*")) {     
                // Identify scenario when individual suffices are correct but their
                // ordering is invalid.
                if (invalidSuffixMatcher.group("suffix").matches("[bwlq][bwlq]")) {
                    errorMessage += " First suffix size must be < second.";
                }
                else {
                    errorMessage += " Need two suffices: b, w, l, or q";
                }
            }
            else if (instrName.matches("^(" + CONDITIONAL_INSTRUCTIONS_REGEX + ").*")) {
                errorMessage += " Need one suffix: e, ne, s, ns, g, ge, l, le, a, ae, b, or be";
            }
            else if (instrName.startsWith("jmp")) {
                errorMessage = " No suffix allowed here.";
            }
            
            throw new X86ParsingException(errorMessage,
                            invalidSuffixMatcher.start("suffix"),
                            instrName.length());
        } else {
            Optional<String> intendedInstruction = getProbableInstruction(instrName);
            if (intendedInstruction.isPresent()) {
                throw new X86ParsingException(
                        "Invalid instruction. Did you mean " + intendedInstruction.get() + "?",
                        0,
                        instrName.length());
            }
            else {
                return Optional.empty();
            }
            
        }
        
        List<OperandRequirements> opReqs = getOperandReqs(type, opSizes);
        return Optional.of(new TypeAndOpRequirements(type, size, opReqs));
    }
    
    private static Optional<String> getProbableRegister(String actualRegister) {
        return getMostSimilarString(ALL_REG_REGEX, actualRegister, 0.8);
    }

    private static Optional<String> getProbableInstruction(String actualInstruction) {
        String validInstructions = "(" 
                + ONE_SUFFIX_INSTRUCTIONS_REGEX
                + ")(b|w|l|q)";
        validInstructions += "|(" 
                + TWO_SUFFIX_INSTRUCTIONS_REGEX
                + ")(b|w|l|q){2}";
        validInstructions += "|jmp|(" 
                + CONDITIONAL_INSTRUCTIONS_REGEX 
                + ")(e|ne|s|ns|g|ge|l|le|a|ae|b|be)";
        return getMostSimilarString(validInstructions, actualInstruction, 0.8);
    }

    private static Optional<String> getMostSimilarString(String validStrings, 
                                                            String actualString,
                                                            double minAcceptableSimilarity) {
        Generex g = new Generex(validStrings);
        List<String> matchedStrs = g.getAllMatchedStrings();
        
        JaroWinkler jw = new JaroWinkler();
        double maxSimilarity = 0.0;
        Optional<String> mostSimilarString = Optional.empty();
        
        for (String s : matchedStrs) {
            double similarity = jw.similarity(s, actualString);
            if (similarity > minAcceptableSimilarity && similarity > maxSimilarity) {
                mostSimilarString = Optional.of(s);
                maxSimilarity = similarity;
            }
        }
                
        return mostSimilarString;
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
        if (name.matches(LONG_REG_REGEX)) {
            opSize = OpSize.LONG;
        } else if (name.matches(QUAD_REG_REGEX)) {
            opSize = OpSize.QUAD;
        } else if (name.matches(WORD_REG_REGEX)) {
            opSize = OpSize.WORD;
        } else if (name.matches(BYTE_REG_REGEX)) {
            opSize = OpSize.BYTE;
        } else {
            String errorMessage = "Invalid register name.";
            Optional<String> intendedRegister = getProbableRegister(name);
            if (intendedRegister.isPresent()) {
                errorMessage += " Did you mean " + intendedRegister.get() + "?";
            }
            throw new X86ParsingException(errorMessage, 0, name.length());
        }

        return opSize;
    }

    /**
     * Construct an operand based on a given string.
     *
     * @param str String containing the operand at the beginning.
     * @param opReqs The requirements for this operand.
     * @return The parsed operand.
     * @throws X86ParsingException There was an error parsing the string.
     */
    private Operand parseOperand(String str, OperandRequirements opReqs) 
            throws X86ParsingException {
        Operand op = null;

        Matcher constMatcher = Pattern.compile(CONST_OPERAND_REGEX).matcher(str);
        Matcher regMatcher = Pattern.compile(REGISTER_OPERAND_REGEX).matcher(str);
        Matcher memMatcher = Pattern.compile(MEM_OPERAND_REGEX).matcher(str);
        Matcher labelMatcher = Pattern.compile(LABEL_OPERAND_REGEX).matcher(str);

        if (constMatcher.matches()) {
            // Found a constant operand
            if (!str.contains("$"))
                throw new X86ParsingException("Missing $ before constant.", 
                                                constMatcher.start(),
                                                constMatcher.end());

            if (!opReqs.canBeConst())
                throw new X86ParsingException("Operand cannot be a constant.", 
                                                constMatcher.start(),
                                                constMatcher.end());
            
            String constStr = constMatcher.group("const");
            int base = 10;
            if (constStr.contains("0x")) {
                base = 16;
                constStr = constStr.replace("0x", "");
            }
            assert(base == 10 || base == 16);
            
            if (!ConstantOperand.fitsInSize(opReqs.getSize(), constStr, base)) {
                throw new X86ParsingException("Constant is too large for specified size.", 
                                                constMatcher.start(),
                                                constMatcher.end());
            }
            
            BigInteger val = new BigInteger(constStr, base);
            op = new ConstantOperand(opReqs.getSize().getValue(val),
                    opReqs.getSize(),
                    base,
                    constMatcher.group("const"));
        } else if (regMatcher.matches()) {
            // Found a register operand
            if (!opReqs.canBeReg())
                throw new X86ParsingException("Operand cannot be a register.", 
                                                regMatcher.start(), 
                                                regMatcher.end());
            
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
            if (opReqs.getSize() != OpSize.INFERRED 
                    && opSize != opReqs.getSize()) {
                String suggestedRegName = 
                        Register.getSubRegisterName(regName, opReqs.getSize().numBytes());
                
                throw new X86ParsingException("Op size mismatch. Did you mean " + suggestedRegName + "?", 
                                                regMatcher.start("regName"), 
                                                regMatcher.end("regName"));
            }

            op = new RegOperand(regName, opSize);
        } else if (memMatcher.matches()) {
            // Found a memory operand
            if (!opReqs.canBeMem())
                throw new X86ParsingException("Operand cannot be a memory location.", 
                                                memMatcher.start(), 
                                                memMatcher.end());

            // All components (e.g. offset or base reg) are optional, although
            // at least one of them must be set.
            // Note that our regular expression should eliminate the possiblity
            // of getting a memory operand with no components present.
            // Look for an offset, which can be any integral value
            Integer offset = null;
            String offsetStr = memMatcher.group("imm");
            if (offsetStr != null) {
                if (offsetStr.startsWith("$")) {
                    throw new X86ParsingException("Immediate should not start with \"$\".",
                            memMatcher.start("imm"),
                            memMatcher.end("imm"));
                }
                int base = 10;
                if (offsetStr.contains("0x")) {
                    base = 16;
                    offsetStr = offsetStr.replace("0x", "");
                }
                offset = Integer.parseInt(offsetStr, base);
                offsetStr = memMatcher.group("imm"); // back to orig string for display later
            }
            else {
                offsetStr = "";
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
                    throw new X86ParsingException("Base register must be quad sized.",
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
                    throw new X86ParsingException("Index register must be quad sized.",
                            memMatcher.start("index"),
                            memMatcher.end("index"));
                }
            }

            // Look for a scaling factor, which should be 1, 2, 4, or 8
            Integer scale = null;
            String scaleStr = memMatcher.group("scale");
            if (scaleStr != null) {
                if (scaleStr.startsWith("$")) {
                    throw new X86ParsingException("Scale factor should not start with \"$\".",
                            memMatcher.start("scale"),
                            memMatcher.end("scale"));
                }
                scale = Integer.parseInt(scaleStr);
                if (scale != 1 && scale != 2 && scale != 4 && scale != 8) {
                    throw new X86ParsingException("Invalid scaling factor. Expecting 1, 2, 4, or 8",
                            memMatcher.start("scale"),
                            memMatcher.end("scale"));
                }
            }

            op = new MemoryOperand(baseReg, indexReg, scale, offset, opReqs.getSize(), offsetStr);
        } else if (labelMatcher.matches()) {
            // Found a label operand
            String labelName = labelMatcher.group("label");
            if (labelName.matches(ALL_REG_REGEX))
                throw new X86ParsingException("Possibly missing % before register name.",
                                                labelMatcher.start(), 
                                                labelMatcher.end());
            
            // Found a label operand
             if (!opReqs.canBeLabel())
                throw new X86ParsingException("Operand cannot be a label.", 
                                                labelMatcher.start(), 
                                                labelMatcher.end());
            
            op = new LabelOperand(labelName, labelFromName.get(labelName));
        }
        assert op != null;
        return op;
    }

    /**
     * Parse all the operands in the given string. These operands should be
     * comma separated.
     *
     * @param operandsStr The string to parse for operands.
     * @param opReqs List of requirements for each operand that is expected.
     * @return The list of operands that were parsed.
     * @throws X86ParsingException There was a problem parsing the operands.
     */
    private List<Operand> parseOperands(String operandsStr, 
            List<OperandRequirements> opReqs) throws X86ParsingException {
        List<Operand> operands = new ArrayList<>();

        Matcher m = Pattern.compile(OPERAND_REGEX).matcher(operandsStr);
        if (!m.find()) {
            return operands;
        }
        
        if (opReqs.isEmpty()) {
            throw new X86ParsingException("Unexpected operand(s).",
                    0, operandsStr.length());
        } else if (m.start("operand") != 0) {
            throw new X86ParsingException("Unexpected character(s) before first operand.", 
                    0, m.start("operand"));
        }

        int nextIndex = -1;
        int opIndex = 0;
        try {
            // Parse the first operand
            String opStr = m.group("operand");
            Operand op = parseOperand(opStr, opReqs.get(opIndex));
            nextIndex = m.end();

            operands.add(op);
            opIndex++;

            // Update pattern to include the comma separator for the following
            // operands
            m = Pattern.compile("," + OPERAND_REGEX).matcher(operandsStr);

            // Keep parsing operands until we don't find any more
            while (m.find(nextIndex)) {
                if (opIndex >= opReqs.size()) {
                    throw new X86ParsingException("Too many operand(s).",
                            nextIndex + m.start("operand"),
                            operandsStr.length());
                }
                opStr = m.group("operand");
                op = parseOperand(opStr, opReqs.get(opIndex));
                nextIndex = m.end();
                operands.add(op);
                opIndex++;
            }
        } catch (X86ParsingException e) {
            throw new X86ParsingException(e.getMessage(),
                    m.start("operand") + e.getStartIndex(),
                    m.start("operand") + e.getEndIndex());
        }
        
        String remainder = operandsStr.substring(nextIndex).trim();

        // Make sure there isn't any leftover cruft after the last parsed operand
        if (!remainder.isEmpty()) {
            throw new X86ParsingException("Could not parse operand(s).", 
                    nextIndex, operandsStr.length());
        }

        return operands;
    }

    /**
     * Create an x86-64 instruction by parsing a given string.
     *
     * @param instr A string representation of the instruction.
     * @return The parsed line.
     * @throws X86ParsingException There was a problem parsing the line.
     */
    public x86ProgramLine parseLine(String instr) throws X86ParsingException {
        Matcher commentMatcher = Pattern.compile("(?<other>[^#]*)(?<comment>#.*)").matcher(instr);
        
        x86Comment c = null;
        if (commentMatcher.matches()){
            // This line contains a comment
            String comment = commentMatcher.group("comment");
            c = new x86Comment(comment);
            instr = commentMatcher.group("other");
        }
        
        if(instr.matches("\\s*")){
            return new x86BlankLine(currLineNum++, c);
        }
        
        Matcher instMatcher = Pattern.compile("\\s*(?<inst>\\p{Alpha}+)(\\s+(?<operands>.*))?").matcher(instr);
        Matcher labelMatcher = Pattern.compile("\\s*" + LABEL_OPERAND_REGEX + ":\\s*").matcher(instr);
        
        // The line should be either a label or an instruction
        if (!instMatcher.matches() && !labelMatcher.matches()) {
            throw new X86ParsingException("Could not parse line: invalid syntax.", 0, instr.length());
        }

        if (instMatcher.matches()) {
            // This line contains an X86 instruction.

            // Step 1: Get the name of the instruction and use that to determine
            // the type (i.e. what operation it is performing) and size of the
            // instruction.
            String instrName = instMatcher.group("inst");

            Optional<TypeAndOpRequirements> instDetails = Optional.empty();
            try {
                instDetails = parseTypeAndSize(instrName);
            } catch (X86ParsingException e) {
                throw new X86ParsingException(e.getMessage(),
                        instMatcher.start("inst") + e.getStartIndex(),
                        instMatcher.start("inst") + e.getEndIndex());
            }

            String operandsStr = instMatcher.group("operands");
            
            // Check to see if the user might have meant a label here but forgot
            // to add the ":" after it.
            if (!instDetails.isPresent()) {
                String errorMessage = "Invalid instruction.";
                if (operandsStr == null) {
                    errorMessage += " Did you forget a \":\" after a label?";
                }
                throw new X86ParsingException(errorMessage,
                            instMatcher.start("inst"),
                            instMatcher.end("inst"));
            }
            
            InstructionType instrType = instDetails.get().type;
            OpSize instrSize = instDetails.get().instrSize;
            List<OperandRequirements> opReqs = instDetails.get().operandReqs;

            // Step 2: Parse the operands (putting them into a list) then use
            // those operands plus the instruction type to create a new
            // X86Instruction.
            if (operandsStr != null) {

                List<Operand> operands = null;
                try {
                    operands = parseOperands(operandsStr, opReqs);
                } catch (X86ParsingException e) {
                    throw new X86ParsingException(e.getMessage(),
                            instMatcher.start("operands") + e.getStartIndex(),
                            instMatcher.start("operands") + e.getEndIndex());
                }

                if (operands.size() != instrType.numOperands()) {
                    throw new X86ParsingException(
                            instrName + " should have " + instrType.numOperands() + " operand(s)",
                            instMatcher.start("operands"),
                            instr.length());
                } else if (instrType.numOperands() == 2) {
                    // Don't allow both operands to be memory operands.
                    if (operands.get(0) instanceof MemoryOperand
                            && operands.get(1) instanceof MemoryOperand) {
                        throw new X86ParsingException("Cannot have two memory operands.",
                                instMatcher.start("operands"),
                                instr.length());
                    }
                    
                    // Determine what size is inferred by the actual operands.
                    if (instrSize == OpSize.INFERRED) {
                        // Second operand has to be a register, which will always
                        // be the explicit size.
                        OpSize inferredSize = operands.get(1).getOpSize();
                        instrSize = inferredSize;
                        OpSize srcSize = operands.get(0).getOpSize();
                        if (srcSize != OpSize.INFERRED) {
                            // If not inferred, then src must match size
                            if (inferredSize != srcSize) {
                                throw new X86ParsingException("Mismatched operand sizes.",
                                        instMatcher.start("operands"),
                                        instr.length());
                            }
                        }
                        else {
                            boolean ok = operands.get(0).makeSizeExplicit(inferredSize);
                            assert ok;
                        }
                    }
                    
                    // According to the Intel IA32/64 manual, CMOV instructions
                    // cannot be used to move byte sized values.
                    if (instrType.toString().startsWith("CMOV") 
                            && instrSize == OpSize.BYTE) {
                        throw new X86ParsingException("CMOV instructions may not be byte sized.",
                                instMatcher.start("operands"),
                                instr.length());
                    }
                    
                    return new x86BinaryInstruction(instrType,
                            operands.get(0),
                            operands.get(1),
                            instrSize,
                            currLineNum++,
                            c);
                } else if (instrType.numOperands() == 1) {
                    x86UnaryInstruction inst = new x86UnaryInstruction(instrType,
                            operands.get(0), instrSize, currLineNum++, c,
                            this.operandGetter);

                    if (operands.get(0) instanceof LabelOperand) {
                        LabelOperand lo = (LabelOperand) operands.get(0);
                        String loName = lo.getName();
                        if (labelUsersFromName.containsKey(loName)) {
                            labelUsersFromName.get(loName).add(inst);
                        } else {
                            List<x86Instruction> l = new ArrayList<>();
                            l.add(inst);
                            labelUsersFromName.put(loName, l);
                        }
                    }
                    return inst;
                }
                throw new X86ParsingException("I am confusion", instMatcher.start("operands"), instrName.length());
            } else {
                if (instrType.numOperands() != 0)
                    throw new X86ParsingException(
                                instrName + " should have " + instrType.numOperands() + " operand(s)",
                                instMatcher.end("inst"),
                                instr.length());
                // nullary skullduggery
                return new x86NullaryInstruction(instrType, instrSize,
                        currLineNum++, c, this.operandGetter);
            }
        } else {
            // This line contains a label
            String labelName = labelMatcher.group("label");
            
            if (labelName.matches(ALL_REG_REGEX))
                throw new X86ParsingException("Label name should not be a register name", 
                                                labelMatcher.start("label"), 
                                                labelMatcher.end("label"));

            // Make sure this label doesn't already exist
            if (labelFromName.containsKey(labelName)) {
                System.out.println("Duplicate label: " + labelName);
                throw new X86ParsingException("Duplicate label name",
                        labelMatcher.start("label"),
                        labelMatcher.end("label"));
            }

            x86Label l = new x86Label(labelName, currLineNum++, c);
            labelFromName.put(labelName, l);
            if (labelUsersFromName.containsKey(labelName)) {
                labelUsersFromName.get(labelName).forEach((inst) -> {
                    inst.updateLabels(labelName, l);
                });
            }
            return l;
        } 
        // TODO: allow lines that contain both a label and an instruction?
    }

    /**
     * Resets the parser back to its starting state.
     */
    public void clear() {
        labelFromName.clear();
        labelUsersFromName.clear();
        currLineNum = 0;
    }
    
    /**
     * Sets the line number of next parsed line.
     * 
     * @param l The next line number.
     */
    public void setCurrLineNum(int l) {
        if (l >= 0) currLineNum = l;
    }
    
    /**
     * Removes the given label from our parser.
     * 
     * @param labelName The label to remove.
     */
    public void removeLabel(String labelName){
        labelFromName.remove(labelName);
    }
    
    public Optional<x86ProgramLine> getFirstLineOfMain(){
        x86Label l = labelFromName.get("main");
        if (l != null) {
            return Optional.of(l);
        } else {
            return Optional.empty();
        }
    } 
    
    /**
     * Returns a list of operand requirements for an instruction of the given type
     * with the given operand sizes.
     * 
     * @param type The type of the instruction.
     * @param sizes A List of sizes for the operands the instruction expects.
     * 
     * @return A List containing the requirements for each of the operands of the instruction.
     */
    private static List<OperandRequirements> getOperandReqs(InstructionType type, List<OpSize> sizes) {
        List<OperandRequirements> opReqs = new ArrayList<>();
        
        // TODO: check that length of sizes is equal to number of operands 
        //  for instruction type
        
        switch (type) {
            case ADD:
            case SUB:
            case IMUL:
            case CMP:
            case OR:
            case AND:
            case TEST:
            case XOR:
            case MOV:
                opReqs.add(new OperandRequirements(sizes.get(0), true, true, true, false));
                opReqs.add(new OperandRequirements(sizes.get(0), false, true, true, false));
                break;
                
            case MOVZ:
            case MOVS:
                opReqs.add(new OperandRequirements(sizes.get(0), false, true, true, false));
                opReqs.add(new OperandRequirements(sizes.get(1), false, true, false, false));
                break;
                
            case SHL:
            case SAL:
            case SHR:
            case SAR:
                opReqs.add(new OperandRequirements(OpSize.BYTE, true, false, false, false));
                opReqs.add(new OperandRequirements(sizes.get(0), false, true, true, false));
                break;
                
            case LEA:
                opReqs.add(new OperandRequirements(OpSize.QUAD, false, false, true, false));
                opReqs.add(new OperandRequirements(OpSize.QUAD, false, true, false, false));
                break;
                
            case CMOVE:
            case CMOVNE:
            case CMOVS:
            case CMOVNS:
            case CMOVG:
            case CMOVGE:
            case CMOVL:
            case CMOVLE:
            case CMOVA:
            case CMOVAE:
            case CMOVB:
            case CMOVBE:
                opReqs.add(new OperandRequirements(OpSize.INFERRED, false, true, true, false));
                opReqs.add(new OperandRequirements(OpSize.INFERRED, false, true, false, false));
                break;
                
            case INC:
            case DEC:
            case NEG:
            case NOT:
            case IDIV:
                opReqs.add(new OperandRequirements(sizes.get(0), false, true, true, false));
                break;
                
            case PUSH:
                opReqs.add(new OperandRequirements(OpSize.QUAD, true, true, true, false));
                break;
                
            case POP:
                opReqs.add(new OperandRequirements(OpSize.QUAD, false, true, true, false));
                break;
                
            case SETE:
            case SETNE:
            case SETS:
            case SETNS:
            case SETG:
            case SETGE:
            case SETL:
            case SETLE:
            case SETA:
            case SETAE:
            case SETB:
            case SETBE:
                opReqs.add(new OperandRequirements(OpSize.BYTE, false, true, true, false));
                break;
                
            case JE:
            case JNE:
            case JS:
            case JNS:
            case JG:
            case JGE:
            case JL:
            case JLE:
            case JA:
            case JAE:
            case JB:
            case JBE:
            case JMP:
            case CALL:
                opReqs.add(new OperandRequirements(OpSize.QUAD, false, false, false, true));
                break;
                
            case RET:
            case CLT:
                // these are nullary instructions (i.e. no operands)
                break;
            default:
        }
        
        return opReqs;
    }
}

/**
 * A class representing the requirements for an operand.
 */
class OperandRequirements {

    private final OpSize size;
    private final boolean canBeConst;
    private final boolean canBeReg;
    private final boolean canBeMem;
    private final boolean canBeLabel;

    public OperandRequirements(OpSize size, boolean con, boolean reg, boolean mem, boolean lab) {
        this.size = size;
        this.canBeConst = con;
        this.canBeReg = reg;
        this.canBeMem = mem;
        this.canBeLabel = lab;
    }

    public OpSize getSize() {
        return this.size;
    }

    public boolean canBeConst() {
        return this.canBeConst;
    }

    public boolean canBeReg() {
        return this.canBeReg;
    }

    public boolean canBeMem() {
        return this.canBeMem;
    }

    public boolean canBeLabel() {
        return this.canBeLabel;
    }
}