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

    public String getName() {
        return name;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label l) {
        label = l;
    }

    @Override
    public BigInteger getValue(MachineState state) {
        if (label == null) {
            // FIXME: change this to a special exception type
            throw new RuntimeException("nonexistent label: " + this.name);
        }
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

    @Override
    public void updateLabels(String labelName, Label label){
        if(this.name.equals(labelName)){
            this.label = label;
        }
    }
    
    public String toString() {
        String res = "" + name;
        return res;
    }
}
