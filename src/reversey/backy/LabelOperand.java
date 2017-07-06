/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author Caitlin
 */
public class LabelOperand extends Operand {

    private String name;
    private Label label;

    public LabelOperand(String name, Label label) {
        this.name = name;
        this.label = label;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        String num = "" + label.getLineNum();
        return new BigInteger(num);
    }

    @Override
    public MachineState updateState(MachineState currState, Optional<BigInteger> val, Map<String, Boolean> flags, boolean updateRIP) {
        if (val.isPresent()) {
            return currState.getNewState(val.get());
        } else {
            return currState.getNewState();
        }
    }

    @Override
    public Set<String> getUsedRegisters() {
        return new HashSet<String>();
    }

    public String toString() {
        String res = "" + name;
        return res;
    }
}
