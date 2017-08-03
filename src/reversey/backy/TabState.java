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

    private final List<String> regHistory;
    private final List<MachineState> stateHistory;
    private final ListView<x86ProgramLine> instrList;
    private final X86Parser parser;
    private String fileName;
    private boolean isEdited;

    public TabState() {
        this.regHistory = new ArrayList<>();
        this.stateHistory = new ArrayList<>();
        this.instrList = new ListView<>();
        this.parser = new X86Parser();
        this.fileName = null;
        this.isEdited = false;
    }

    public TabState(List<String> regHistory,
                    List<MachineState> stateHistory, 
                    ListView<x86ProgramLine> instrList,
                    X86Parser parser, 
                    String fileName) {
        this.regHistory = regHistory;
        this.stateHistory = stateHistory;
        this.instrList = instrList;
        this.parser = parser;
        this.fileName = fileName;
        this.isEdited = false;
    }

    public List<String> getRegHistory() {
        return this.regHistory;
    }

    public List<MachineState> getStateHistory() {
        return this.stateHistory;
    }

    public ListView<x86ProgramLine> getInstrList() {
        return this.instrList;
    }

    public X86Parser getParser() {
        return this.parser;
    }

    public String getFileName() {
        return this.fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public boolean getIsEdited(){
        return this.isEdited;
    }
    
    public void setIsEdited(boolean b){
        isEdited = b;
    }
}