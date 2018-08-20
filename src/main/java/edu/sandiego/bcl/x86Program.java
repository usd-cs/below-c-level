/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

/**
 * Class that represents an X86-64 program.
 * 
 * @author sat
 */
public class x86Program {
    /**
     * The lines in the program.
     */
    ObservableList<x86ProgramLine> programLines;
    
    /**
     * The parser used to add or modify this program.
     */
    private final X86Parser parser;
    
    /**
     * The name of the file associated with this program.
     * This does not include the full path.
     */
    private String fileName;
    
    /**
     * An (optional) file where the program has been stored.
     */
    private Optional<File> file;
    
    /**
     * Whether the program has unsaved changes of some sort.
     * Note that this could be true because this is a new, blank program.
     */
    private boolean isUnsaved;
    
    /**
     * Next number to give to a new program that isn't backed by a specific file.
     */
    private static int nextUntitledNumber = 0;
    
    /**
     * Constructors a blank program.
     */
    public x86Program() {
        this.programLines = FXCollections.observableArrayList();
        this.parser = new X86Parser();
        this.fileName = "untitled-" + nextUntitledNumber;
        nextUntitledNumber++;
        this.file = Optional.empty();
        this.isUnsaved = true;
    }
    
    /**
     * Creates a new program based on the program stored in the given file.
     * 
     * @param assemblyFile File containing assembly code to be parsed.
     * @throws FileNotFoundException if the file to read from does not exist.
     * @throws IOException if there was a problem reading from the file.
     * @throws X86ParsingException if there was an error while parsing a line in
     * the file.
     */
    public x86Program(File assemblyFile) throws FileNotFoundException, 
                                                IOException,
                                                X86ParsingException {
        this.programLines = FXCollections.observableArrayList();
        this.parser = new X86Parser();
        this.fileName = assemblyFile.getName();
        this.file = Optional.of(assemblyFile);
        this.loadFromFile();
        this.isUnsaved = false;
    }
    
    public ObservableList<x86ProgramLine> getProgramLines() {
        return this.programLines;
    }
    
    public boolean hasFile() {
        return this.file.isPresent();
    }
    
    public boolean isUnsaved() { return this.isUnsaved; }
    
    public String getFileName() { return this.fileName; }
    
    public x86ProgramLine getLine(int index) {
        return this.programLines.get(index);
    }
    
    public int getNumLines() { return this.programLines.size(); }
    
    public boolean isEmpty() {
        return this.programLines.isEmpty();
    }
    
    /**
     * Parses the given line and adds it to the end of this  program.
     * 
     * @param unparsedLine The line to parse.
     * @return The new line that was created.
     * @throws X86ParsingException if there was a problem parsing the line.
     */
    public x86ProgramLine parseThenAddLine(String unparsedLine) 
            throws X86ParsingException {
        x86ProgramLine x = this.parser.parseLine(unparsedLine);
        this.programLines.add(x);
        this.isUnsaved = true;
        return x;
    }
    
    public void setFile(File f) {
        this.file = Optional.of(f);
        this.fileName = this.file.get().getName();
    }
    
    /**
     * Attempts to write the program to the it's file.
     * If no file was set, then this will return false.
     * 
     * @return True if the file was successfully written, False otherwise.
     */
    public boolean writeToFile() {    
        if (file.isPresent()) {
            try (FileWriter fw = new FileWriter(file.get())) {
                for (x86ProgramLine line : this.programLines) {
                    // TODO: make an x86ProgramLine method that
                    fw.write(line.toString()
                            .substring(line.toString().indexOf(":") + 2) 
                            + "\n");
                }
                this.isUnsaved = false;
            } catch (IOException ex) {
                System.out.println("Unable to save to file.");
                return false;
            }
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Read and parse the program from it's file.
     * 
     * @throws FileNotFoundException if the file we are trying to read from does
     * not exist.
     * @throws IOException if there was an error while reading from the file.
     * @throws X86ParsingException if there was an error while parsing one of the
     * lines in the file.
     */
    private void loadFromFile() 
            throws FileNotFoundException, IOException, X86ParsingException {
        assert this.programLines.isEmpty();
        
        if (file.isPresent()) {            
            ArrayList<String> fileLines = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(file.get()));
            String l;
            while ((l = br.readLine()) != null) {
                fileLines.add(l.trim());
            }

            for (String line : fileLines) {
                try {
                    this.parseThenAddLine(line);
                } catch (X86ParsingException e) {                    
                    Alert fileLoadingError = new Alert(Alert.AlertType.ERROR);
                    fileLoadingError.setTitle("File Parsing Error");
                    fileLoadingError.setHeaderText("Error Loading File");
                    fileLoadingError.setContentText("Unable to parse the following line:"
                            + "\n\n" + line
                            + "\n\nReason: " + e.getMessage());
                    fileLoadingError.showAndWait();
                    throw e;
                }
            }
        }
    }
    
    /**
     * Remove the given line from this program.
     * 
     * @param line The line to remove.
     */
    public void removeLine(x86ProgramLine line) {
        if (line instanceof x86Label) {
            x86Label l = (x86Label) line;
            this.parser.removeLabel(l.getName());
        }

        this.programLines.remove(line);
        int i = 0;
        for (x86ProgramLine l : this.programLines) {
            l.setLineNum(i);
            i++;
        }
        this.parser.setCurrLineNum(i);
        this.isUnsaved = true;
    }
    
    /**
     * Replace the existing line with a new one that needs to be parsed.
     * 
     * @param oldLine The existing line that should be replaced.
     * @param newLine The new line that we should replace the old one with.
     * @throws X86ParsingException if there was an error while parsing the new
     * line.
     */
    public void replaceLine(x86ProgramLine oldLine, String newLine) throws X86ParsingException {
        if (oldLine instanceof x86Label) {
            x86Label l = (x86Label) oldLine;
            this.parser.removeLabel(l.getName());
        }
        x86ProgramLine x = this.parser.parseLine(newLine);

        // Find where the existing instruction was and replace
        // it with the new instruction.
        int i = 0;
        for (x86ProgramLine line : this.programLines) {
            if (line == oldLine) {
                parser.setCurrLineNum(x.getLineNum());
                x.setLineNum(i);
                this.programLines.remove(oldLine);
                this.programLines.add(i, x);
                this.isUnsaved = true;
                break;
            }
            i++;
        }
    }
    
    /**
     * Returns the line that marks the beginning of the program.
     * This will be either the line with the "main" label or, if that label
     * doesn't exist, the first line in the program.
     * 
     * @return The first line in the program.
     */
    public x86ProgramLine getBeginningOfProgram(){
        Optional<x86ProgramLine> firstLine = parser.getFirstLineOfMain();
        if(firstLine.isPresent()){
            return firstLine.get();
        } else {
            return programLines.get(0);
        }
    }
}
