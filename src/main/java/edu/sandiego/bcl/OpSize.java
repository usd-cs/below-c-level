package edu.sandiego.bcl;

import java.math.BigInteger;

/**
 * Representation of the size of an x86 instruction or operand.
 */
public enum OpSize {
    BYTE('b', 1),
    WORD('w', 2),
    LONG('l', 4),
    QUAD('q', 8);

    /**
     * The number of bytes used for this op.
     */
    private int numBytes;
    
    /**
     * Single character abbreviation of this size.
     */
    private final char abbreviation;

    private OpSize(char abbrev, int nb) {
        this.abbreviation = abbrev;
        this.numBytes = nb;
    }

    public int numBytes() {
        return this.numBytes;
    }

    public int numBits() {
        return this.numBytes * 8;
    }
    
    public char getAbbreviation() { 
        return this.abbreviation;
    }
    
    public static OpSize getOpSizeFromAbbrev(String abbrev) throws X86ParsingException {
        switch (abbrev) {
            case "b":
                return OpSize.BYTE;
            case "w":
                return OpSize.WORD;
            case "l":
                return OpSize.LONG;
            case "q":
                return OpSize.QUAD;
            default:
                throw new X86ParsingException("unexpected size suffix",
                                                0,
                                                abbrev.length());
        }
    }
    
    public long getValue(BigInteger b) {
        switch (this) {
            case BYTE:
                return b.byteValue();
            case WORD:
                return b.shortValue();
            case LONG:
                return b.intValue();
            case QUAD:
                return b.longValue();
            default:
                throw new RuntimeException("unimplemented opsize");
        }
    }
}
