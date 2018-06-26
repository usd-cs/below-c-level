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

    // TODO: remove this field?
    private final ListView<x86ProgramLine> instrList;
    private final X86Parser parser;
    private final Simulation simulator;
    private String fileName;
    private boolean isEdited;

    public TabState() {
        this.instrList = new ListView<>();
        this.parser = new X86Parser();
        this.simulator = new Simulation(this.instrList.getItems());
        this.fileName = null;
        this.isEdited = false;
    }

    public TabState(ListView<x86ProgramLine> instrList,
                    X86Parser parser,
                    Simulation simulator,
                    String fileName) {
        this.instrList = instrList;
        this.parser = parser;
        this.simulator = simulator;
        this.fileName = fileName;
        this.isEdited = false;
    }

    public Simulation getSimulator() {
        return this.simulator;
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