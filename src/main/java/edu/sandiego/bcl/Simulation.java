/**
 * Class that represents the simulation of an assembly program.
 */
package edu.sandiego.bcl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;

/**
 *
 * @author sat
 */
public class Simulation {    
    /**
     * The history of execution states in our simulation.
     */
    private final List<MachineState> stateHistory;
    
    /**
     * History of registers used by the simulation. This list may contain
     * duplicates as one is added for each register used by an instruction when
     * it is executed.
     */
    private final List<String> regHistory;
    
    /**
     * The line in the program where simulation is currently at.
     * This will be null when the simulation is complete and when the program is
     * empty (i.e. contains no lines).
     */
    private x86ProgramLine currentLine;
    
    /**
     * The program being simulated.
     */
    private final x86Program program;
    
    /**
     * The evaluation is stopped because of a RunTime Error.
     */
    private boolean stuckOnError;
    
    public Simulation() {
        this.program = new x86Program();
        this.stateHistory = new ArrayList<>();
        this.stateHistory.add(new MachineState());
        this.regHistory = new ArrayList<>();
        this.stuckOnError = false;
    }
    
    public Simulation(File assemblyFile) throws FileNotFoundException,
                                                IOException,
                                                X86ParsingException {
        this.program = new x86Program(assemblyFile);
        stateHistory = new ArrayList<>();
        stateHistory.add(new MachineState());
        
        regHistory = new ArrayList<>();
        if (!this.program.isEmpty()) {
            currentLine = this.program.getBeginningOfProgram();
            stateHistory.get(0).setRip(currentLine.getLineNum());
            regHistory.addAll(this.program.getLine(0).getUsedRegisters());
        }
        this.stuckOnError = false;
    }
    
    public String getProgramFileName() { return this.program.getFileName(); }
    
    public boolean isProgramUnsaved() { return this.program.isUnsaved(); }
    
    public x86ProgramLine getCurrentLine() { return this.currentLine; }
    
    public ObservableList<x86ProgramLine> getProgramLines() {
        return this.program.getProgramLines();
    }
    
    public List<Register> getRegisters() {
        List<Register> regList = stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory);
        return regList;
    }
    
    public List<StackEntry> getStackEntries() {
        return stateHistory.get(this.stateHistory.size() - 1).getStackEntries();
    }
    
    public boolean getStuckOnError() {
        return this.stuckOnError;
    }
    
    public boolean hasSignFlagSet() {
        return stateHistory.get(this.stateHistory.size() - 1).getSignFlag();
    }
    
    public boolean hasZeroFlagSet() {
        return stateHistory.get(this.stateHistory.size() - 1).getZeroFlag();
    }
    
    public boolean hasOverflowFlagSet() {
        return stateHistory.get(this.stateHistory.size() - 1).getOverflowFlag();
    }
    
    public boolean hasCarryFlagSet() {
        return stateHistory.get(this.stateHistory.size() - 1).getCarryFlag();
    }
    
    public boolean hasProgramFile() {
        return this.program.hasFile();
    }
    
    /**
     * Restarts simulation back to its beginning state.
     */
    public void restart() {
        this.stateHistory.clear();
        this.stateHistory.add(new MachineState());

        this.regHistory.clear();

        if (!this.program.isEmpty()) {
            currentLine = this.program.getBeginningOfProgram();
            stateHistory.get(0).setRip(currentLine.getLineNum());
            regHistory.addAll(currentLine.getUsedRegisters());
        }
        
        this.stuckOnError = false;
    }
    
    /**
     * Checks if end of simulation has been reached.
     * 
     * @return True if simulation is at the end, false otherwise.
     */
    public boolean isFinished(){
        return (stateHistory.get(stateHistory.size() - 1).getRipRegister() 
                >= this.program.getNumLines()) 
                || (stateHistory.get(stateHistory.size() - 1).getCallStackSize() < 0);
    }
    
    /**
     * Checks if simulation is at the beginning (i.e. hasn't executed any instructions).
     * 
     * @return True if simulation is at the beginning, false otherwise.
     */
    public boolean isAtBeginning(){
        return stateHistory.size() == 1; 
    }
    
    /**
     * Executes the next instruction in our simulation.
     */
    public void stepForward() throws x86RuntimeException {
        evalCurrentInstruction();
    }
    
    /**
     * Executes instructions until it reaches the end of the program.
     * 
     * @return True if simulation completed or we reached a breakpoint. False otherwise.
     */
    public boolean finish() throws x86RuntimeException {
        int numExecuted = 0; // number of instructions we have executed so far
        
        while (!isFinished()
                && (!currentLine.getBreakpoint() || numExecuted == 0)
                && numExecuted < 100) {
            evalCurrentInstruction();
            numExecuted++;
        }
        
        return isFinished() || currentLine.getBreakpoint();
    }
    
    /**
     * Evaluates the current instruction, adding the newly produced state to our
     * history and selecting the next instruction.
     */
    private void evalCurrentInstruction() throws x86RuntimeException {
        try {
            // evaluate the current instruction, adding its new state to our history
            MachineState nextState = currentLine.eval(stateHistory.get(stateHistory.size() - 1));
            stateHistory.add(nextState);

            // select next instruction based on the updated value of the rip register
            if (isFinished()) {
                currentLine = null;
            }
            else {
                currentLine = this.program.getLine(stateHistory.get(stateHistory.size() - 1).getRipRegister());
                regHistory.addAll(currentLine.getUsedRegisters());
            } 
        } catch (x86RuntimeException e) {
            this.stuckOnError = true;
            throw e;
        }
    }
    
    /**
     * Undoes the previous instruction in our simulation.
     * If you are at the beginning of simulation, this has method has no effect.
     */
    public void stepBackward() {
        // We'll only have one state in our history when we are at the beginning
        // of simulation. In this case, going backwards shouldn't do anything.
        if (stateHistory.size() == 1) {
            this.stuckOnError = false;
            return;
        }
        
        stateHistory.remove(stateHistory.size() - 1);
        if (!this.program.isEmpty() && currentLine != null) {
            regHistory.removeAll(currentLine.getUsedRegisters());
        }
        currentLine = this.program.getLine(stateHistory.get(stateHistory.size() - 1).getRipRegister());
        
        this.stuckOnError = false;
    }
    
    /**
     * Add line to end of the program associated with this simulation.
     * 
     * @param lineText The line to be added to the program.
     * @throws X86ParsingException if the given line cannot be parsed.
     */
    public void appendToProgram(String lineText) throws X86ParsingException {
        x86ProgramLine newLine = this.program.parseThenAddLine(lineText);

        // If this is the first instruction entered, "select" it and
        // make sure it gets added to our register history list.
        if (this.program.getNumLines() == 1) {
            regHistory.addAll(newLine.getUsedRegisters());
            currentLine = newLine;
        }
    }
    
    /**
     * Removes the given program line from the program associated with this simulation.
     * 
     * @param line The line to remove.
     */
    public void removeFromProgram(x86ProgramLine line) {
        this.program.removeLine(line);
    }
    
    /**
     * Replaces an existing line with a new one in the program associated with
     * this simulation.
     * @param existingLine The line that will be replaced.
     * @param newLine What the existing line will be replaced with.
     * @throws X86ParsingException if the new line cannot be parsed.
     */
    public void replaceInProgram(x86ProgramLine existingLine, String newLine) throws X86ParsingException {
        this.program.replaceLine(existingLine, newLine);
    }
    
    /**
     * Writes the program out to a file. The location of the file is based on where
     * it was last saved.
     * 
     * @return True if the program was successfully written, False otherwise. 
     */
    public boolean saveProgram() {
        return this.program.writeToFile();
    }
    
    /**
     * Writes the program out to the given file.
     * 
     * @param f The file to write the program out to.
     * @return True if the program was successfully written, False otherwise.
     */
    public boolean saveProgramAs(File f) {
        this.program.setFile(f);
        return this.program.writeToFile();
    }
}
