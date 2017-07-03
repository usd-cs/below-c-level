package reversey.backy;

/**
 * Representation of the size of an x86 instruction or operand.
 */
public enum OpSize {
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
