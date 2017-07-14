/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.List;
import javafx.scene.control.ListView;

/**
 *
 * @author Caitlin
 */
public class TabState {
    private List<String> currRegisters;
    private List<MachineState> currTabStateHistory;
    private ListView<x86ProgramLine> currTabInstrList;
    
    public TabState(List<String> tabRegs, List<MachineState> tabStates, ListView<x86ProgramLine> tabInstrList) {
		this.currRegisters = tabRegs;
		this.currTabStateHistory = tabStates;
                this.currTabInstrList = tabInstrList;
	}
    
    public List<String> getCurrRegisters(){ return this.currRegisters; }
    public List<MachineState> getCurrTabStateHistory() { return this.currTabStateHistory; }
    public ListView<x86ProgramLine> getCurrTabInstrList() { return this.currTabInstrList; }
    
    public void setCurrRegisters(List<String> regs){
        this.currRegisters = regs;
    } 
    
    public void setCurrTabStateHistory(List<MachineState> states){
        this.currTabStateHistory = states;
    } 
    
    public void setCurrRegisters(ListView<x86ProgramLine> instList){
        this.currTabInstrList = instList;
    } 
}
