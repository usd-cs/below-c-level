package reversey.backy;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@FunctionalInterface
interface UnaryX86Operation {

    MachineState apply(MachineState state, Operand dest);
}

/**
 * Class representing an x86 instruction with a single operand.
 *
 * @author Sat Garcia (sat@sandiego.edu)
 */
public class x86UnaryInstruction extends x86Instruction {

    /**
     * The function that this instruction performs.
     */
    private UnaryX86Operation operation;

    /**
     * @param instType String representation of the instruction's operation.
     * @param destOp Operand representing the destination of the instruction.
     * @param size Number of bytes this instruction works on.
     */
    public x86UnaryInstruction(String instType, Operand destOp, OpSize size, int line) {
        this.destination = destOp;
        this.opSize = size;
        this.lineNum = line;

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

                            return dest.updateState(state, Optional.of(result), flags, true);
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

                            return dest.updateState(state, Optional.of(result), flags, true);
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

                            return dest.updateState(state, Optional.of(result), flags, true);
                        };
                break;
            case "not":
                this.type = InstructionType.NOT;
                this.operation
                        = (state, dest) -> {
                            BigInteger result = dest.getValue(state).not();
                            return dest.updateState(state, Optional.of(result), flags, true);
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
                            return dest.updateState(state, Optional.of(result), flags, true);
                        };
                break;
            case "push":
                this.type = InstructionType.PUSH;
                this.operation
                        = (state, src) -> {
                            // step 1: subtract 8 from rsp
                            RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
                            MachineState tmp = rsp.updateState(state, Optional.of(rsp.getValue(state).subtract(BigInteger.valueOf(8))), flags, false);

                            // step 2: store src operand value in (%rsp)
                            MemoryOperand dest = new MemoryOperand("rsp", null, 1, 0, this.opSize);
                            return dest.updateState(tmp, Optional.of(src.getValue(tmp)), flags, true);
                        };
                break;
            case "pop":
                this.type = InstructionType.POP;
                this.operation
                        = (state, dest) -> {
                            // step 1: store (%rsp) value in dest operand 
                            MemoryOperand src = new MemoryOperand("rsp", null, 1, 0, this.opSize);
                            MachineState tmp = dest.updateState(state, Optional.of(src.getValue(state)), flags, true);

                            // step 2: add 8 to rsp
                            RegOperand rsp = new RegOperand("rsp", OpSize.QUAD);
                            return rsp.updateState(tmp, Optional.of(rsp.getValue(tmp).add(BigInteger.valueOf(8))), flags, false);

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
        return lineNum + ": " + type + " " + destination.toString();
    }
}

