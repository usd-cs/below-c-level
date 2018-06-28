/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

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
 *
 * @author sat
 */
public class x86Program {
    ObservableList<x86ProgramLine> programLines;
    private final X86Parser parser;
    private String fileName;
    private Optional<File> file;
    private boolean isUnsaved;
    
    private static int nextUntitledNumber = 0;
    
    public x86Program() {
        this.programLines = FXCollections.observableArrayList();
        this.parser = new X86Parser();
        this.fileName = "untitled-" + nextUntitledNumber;
        nextUntitledNumber++;
        this.isUnsaved = true;
    }
    
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
    
    public x86ProgramLine parseThenAddLine(String lineText) throws X86ParsingException {
        x86ProgramLine x = this.parser.parseLine(lineText);
        this.programLines.add(x);
        this.isUnsaved = true;
        return x;
    }
    
    public void setFile(File f) {
        this.file = Optional.of(f);
        this.fileName = this.file.get().getName();
    }
    
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
    
    private void loadFromFile() throws FileNotFoundException, IOException, X86ParsingException {
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
}
