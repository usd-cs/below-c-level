/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ListView;

/**
 *
 * @author Caitlin
 */
public class TabState {

    private List<String> currTabRegHistory;
    private List<MachineState> currTabStateHistory;
    private ListView<x86ProgramLine> currTabInstrList;
    private X86Parser currTabParser;
    private String currTabFileName;
    private boolean currTabIsEdited;

    public TabState() {
        this.currTabRegHistory = new ArrayList<String>();
        this.currTabStateHistory = new ArrayList<MachineState>();
        this.currTabInstrList = new ListView<x86ProgramLine>();
        this.currTabParser = new X86Parser();
        this.currTabFileName = null;
        this.currTabIsEdited = false;
    }

    public TabState(List<String> tabRegs, List<MachineState> tabStates, ListView<x86ProgramLine> tabInstrList, X86Parser tabParser, String tabFileName) {
        this.currTabRegHistory = tabRegs;
        this.currTabStateHistory = tabStates;
        this.currTabInstrList = tabInstrList;
        this.currTabParser = tabParser;
        this.currTabFileName = tabFileName;
        this.currTabIsEdited = false;
    }

    public List<String> getCurrTabRegHistory() {
        return this.currTabRegHistory;
    }

    public List<MachineState> getCurrTabStateHistory() {
        return this.currTabStateHistory;
    }

    public ListView<x86ProgramLine> getCurrTabInstrList() {
        return this.currTabInstrList;
    }

    public X86Parser getCurrTabParser() {
        return this.currTabParser;
    }

    public String getCurrFileName() {
        return this.currTabFileName;
    }
    
    public boolean getCurrTabIsEdited(){
        return this.currTabIsEdited;
    }
    
    public void setCurrTabIsEdited(boolean b){
        currTabIsEdited = b;
    }
}
