package edu.sandiego.bcl;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import java.util.ArrayList;
import java.util.List;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
@PWA(name = "Project Base for Vaadin Flow", shortName = "Project Base")
public class MainView extends VerticalLayout {
    
    // Declare current sim
    private Simulation activeSimulation;
    
    public MainView() {
       
        activeSimulation = new Simulation();

        // Set up x86-64 instruction list table
        Grid<x86ProgramLine> instructionTable  = new Grid<>(x86ProgramLine.class);
        initializeInstructionTable(instructionTable);
        
        // Set up instruction input field
        TextField instructionInput = new TextField();
        initializeInputField(instructionInput);

        // Set up instruction input field event listener
        instructionInput.addValueChangeListener(event -> { 
            if(event.getValue().length() > 0) {
                try {  
                    Notification.show("event: " + event.getValue());
                    activeSimulation.appendToProgram(event.getValue());
                    instructionInput.setInvalid(false);
                    Notification.show("Successfully added!");
                    instructionTable.getDataProvider().refreshAll();
                    instructionInput.clear();
                }
                catch (X86ParsingException e) {
                    instructionInput.setErrorMessage(e.getMessage());
                    instructionInput.setInvalid(true);
                    Notification.show("Bad instruction!");
                }
            }
        });

        // Set up stack entry table
        Grid<StackEntry> stackTable = new Grid<>();
        initializeStackTable(stackTable);
                       
        // Set up register table
        Grid<Register> registerTable = new Grid<>();
        initializeRegisterTable(registerTable);
    
        /**
         Create Buttons
         */

         // Container for buttons
         HorizontalLayout buttons = new HorizontalLayout();
         
         // Reset program to beginning of instructions (DONE)
         Button reset = new Button("Restart");
         reset.addClickListener( event -> {
             activeSimulation.restart();
             Notification.show("Simulation Reset");
             registerTable.setItems(activeSimulation.getRegisters());
             stackTable.setItems(activeSimulation.getStackEntries());
             instructionTable.setItems(activeSimulation.getProgramLines());
         });
        
         // Undo the simulation of 1 instruction (DONE)
         Button back = new Button("Step Back");
         back.addClickListener( event -> {
                 activeSimulation.stepBackward();
                 Notification.show("Backward");
                 registerTable.setItems(activeSimulation.getRegisters());
                 stackTable.setItems(activeSimulation.getStackEntries());
         });
        
         // FXML: Jump to current button
         // TODO
         Button play = new Button("Play");
        
         // Simulate 1 instruction (DONE)
         Button forward = new Button("Step Forward");
         forward.addClickListener(event -> {
             try { 
                 activeSimulation.stepForward();
                 Notification.show("Forward");
                 registerTable.setItems(activeSimulation.getRegisters());
                 stackTable.setItems(activeSimulation.getStackEntries());
                 }
             catch(x86RuntimeException e) {
                Notification.show("Forward: x86RuntimeException");
             }
            });

         // Skip to end of simulation
         Button end = new Button("End");
         end.addClickListener( event -> {
             try {
                 Notification.show("Execute simulation");
                 activeSimulation.finish();
                   //  activeSimulation.stepForward();

              updateTables(stackTable, registerTable);
                 }
                 //activeSimulation.finish();
                // Notification.show("Execute simulation");
                // registerTable.setItems(activeSimulation.getRegisters());
                // stackTable.setItems(activeSimulation.getStackEntries());
             
             catch(x86RuntimeException e) {
                 Notification.show("End: x86RuntimeException");
             }
         });
        
    buttons.add(reset, back, play, forward, end);
    
    // Container for left side of app
    VerticalLayout left = new VerticalLayout();
    left.add(instructionInput, instructionTable, buttons);
    
    // Container for right side of app
    VerticalLayout right = new VerticalLayout();
   right.add(stackTable, registerTable); 

    // Container for Simulation
    // CURRENTLY DOESN'T WORK RIGHT SIDE WONT SHOW
    HorizontalLayout simContainer = new HorizontalLayout();
    simContainer.add(left, right);

        add(left);
        add(right);
    }

    /**
     * Method to initialize the instruction table
     * @param g the grid to contain x86-64 program instructions 
     */
    public void initializeInstructionTable(Grid<x86ProgramLine> g) {
        g.removeAllColumns();
        g.addColumn(TemplateRenderer.<x86ProgramLine>of(
            "<div title='[[item.description]]'>[[item.Instruction]]</div>")
            .withProperty("Instruction", line -> line.toString())
            .withProperty("description", line -> line.getDescriptionString()))
            .setHeader("Instruction");
        g.setItems(activeSimulation.getProgramLines());    
    }

    /**
    * Method to initialize the instruction input field
    * @param t the textfield to input instructions
    */
    public void initializeInputField(TextField t) {
        t.setWidthFull();
        t.setPlaceholder("Enter an x86 instruction or comment");
        t.setClearButtonVisible(true);
    }

    /** 
    * Method to initialize the stack table
    * @param g the table containing stackEntry objects
    */
    public void initializeStackTable(Grid<StackEntry> g) {
        g.removeAllColumns();
        g.addColumn(StackEntry::getStartAddress).setHeader("Start");
        g.addColumn(StackEntry::getEndAddress).setHeader("End");
        g.addColumn(StackEntry::getValue).setHeader("Value");
        g.addColumn(StackEntry::getOrigin).setHeader("Line #");
        g.setItems(activeSimulation.getStackEntries());
    }

    /** 
    * Method to initialize the register table
    * @param g the table containing registers and their states 
    */
    public void initializeRegisterTable(Grid<Register> g) {
        g.removeAllColumns();
        g.addColumn(Register::getName).setHeader("Register");
        g.addColumn(Register::getValue).setHeader("Value");
        g.addColumn(Register::getOrigin).setHeader("Line #");
        g.setItems(activeSimulation.getRegisters());
    }

    /**
     * Method to update the stack and register tables when an 
     * instruction is executed
     * @param s the stack table to update
     * @param r the register table to update
     */
    public void updateTables(Grid<StackEntry> s, Grid<Register> r) {
        s.setItems(activeSimulation.getStackEntries());
        r.setItems(activeSimulation.getRegisters());
    }
}
