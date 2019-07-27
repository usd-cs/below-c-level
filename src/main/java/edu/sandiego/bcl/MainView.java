package edu.sandiego.bcl;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.GridSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.Grid.*;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.AppLayoutMenu;
import com.vaadin.flow.component.applayout.AppLayoutMenuItem;
import com.vaadin.flow.component.HtmlContainer.*;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.selection.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * The main view contains a button and a click listener.
 */
@Route("")
@PWA(name = "Project Base for Vaadin Flow", shortName = "Project Base")
public class MainView extends AppLayout {
    
    // Declare current sim
    private Simulation activeSimulation;
    
    public MainView() {
        
        activeSimulation = new Simulation();

        // Image placement currently wrong, only displays alt text
        Image headerImage = new Image("/below-c-level/resources/images/interface.png", "Below C Level");
        setBranding(headerImage);

        // Navigation Bar
        AppLayoutMenu nav = createMenu();
        AppLayoutMenuItem home = new AppLayoutMenuItem("Home");
        AppLayoutMenuItem userGuide = new AppLayoutMenuItem("User Guide");
        AppLayoutMenuItem tutorials = new AppLayoutMenuItem("Tutorials");
        AppLayoutMenuItem feedback = new AppLayoutMenuItem("Feedback");
        nav.addMenuItems(home, userGuide, tutorials, feedback);
     
        // Set up x86-64 instruction list table
        Grid<x86ProgramLine> instructionTable  = new Grid<>(x86ProgramLine.class);
        GridSelectionModel<x86ProgramLine> selectionMode = instructionTable
            .setSelectionMode(Grid.SelectionMode.SINGLE);
        initializeInstructionTable(instructionTable);
    
        // Set up stack entry table
        Label stack = new Label("Program Stack");
        stack.setWidthFull();
        Grid<StackEntry> stackTable = new Grid<>();
        initializeStackTable(stackTable);
                       
        // Set up register table
        Label register = new Label("Register View");
        register.setWidthFull();
        Grid<Register> registerTable = new Grid<>();
        registerTable.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        initializeRegisterTable(registerTable);
    
        // Set up status flags
        HorizontalLayout statusFlags = new HorizontalLayout();
        Label sfLabel = new Label();
        Label zfLabel = new Label();
        Label ofLabel = new Label();
        Label cfLabel = new Label();
        updateStatusFlags(sfLabel, zfLabel, ofLabel, cfLabel);
        statusFlags.add(sfLabel, zfLabel, ofLabel, cfLabel);
        
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
                    updateTables(instructionTable, stackTable, registerTable, sfLabel, zfLabel, ofLabel, cfLabel);
                    instructionInput.clear();
                }
                catch (X86ParsingException e) {
                    instructionInput.setErrorMessage(e.getMessage());
                    instructionInput.setInvalid(true);
                    Notification.show("Bad instruction!");
                }
            }
        });

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
             updateTables(instructionTable, stackTable, registerTable, sfLabel, zfLabel, ofLabel, cfLabel);
             instructionTable.setItems(activeSimulation.getProgramLines());
         });
        
         // Undo the simulation of 1 instruction (DONE)
         Button back = new Button("Step Back");
         back.addClickListener( event -> {
                 activeSimulation.stepBackward();
                 Notification.show("Backward");
                 updateTables(instructionTable, stackTable, registerTable, sfLabel, zfLabel, ofLabel, cfLabel);
         });
        
         // Jump to current instruction (DONE)
         Button play = new Button("Play");
         play.addClickListener(event -> { 
             Notification.show("Play");
             scrollToSelectedInstruction(instructionTable);
         });
        
         // Simulate 1 instruction (DONE)
         Button forward = new Button("Step Forward");
         forward.addClickListener(event -> {
             try { 
                 activeSimulation.stepForward();
                 Notification.show("Forward");
                 updateTables(instructionTable, stackTable, registerTable, sfLabel, zfLabel, ofLabel, cfLabel);
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
                 updateTables(instructionTable, stackTable, registerTable, sfLabel, zfLabel, ofLabel, cfLabel);
                 }
             catch(x86RuntimeException e) {
                 Notification.show("End: x86RuntimeException");
             }
         });
        
        buttons.add(reset, back, play, forward, end);
        
        // Container for left side of app
        VerticalLayout left = new VerticalLayout();
        left.add(instructionInput, instructionTable, buttons);
        left.setSpacing(false);

        VerticalLayout stackLayout = new VerticalLayout();
        stackLayout.add(stack, stackTable);
        stackLayout.setSpacing(false);
        stackLayout.setSizeFull();

        VerticalLayout regLayout = new VerticalLayout();
        regLayout.add(register, registerTable);
        regLayout.setSizeFull();
        regLayout.setSpacing(false);

        // Container for right side of app
        VerticalLayout right = new VerticalLayout();
        //right.setwidthFull();
        right.add(stackLayout, regLayout, statusFlags);
        right.setSizeFull(); 
 
       // Container for Simulation
        HorizontalLayout simContainer = new HorizontalLayout();
        simContainer.add(left, right);
        
        // For visibility and scroll testing purposes
        simContainer.setWidth("90%");
        simContainer.setHeight("80%");
    
        // Start a new, blank simulation
        Button newSim = new Button("Start a New Simulation");
        newSim.addClickListener( event -> {
            Notification.show("Starting new sim!");
            activeSimulation.createNewSim();
            instructionTable.getDataProvider().refreshAll();
            instructionTable.setItems(activeSimulation.getProgramLines());
            updateTables(instructionTable, stackTable, registerTable, sfLabel, zfLabel, ofLabel, cfLabel);
            
            // Test confirmation
            if(activeSimulation.isAtBeginning()) {
                Notification.show("SUCCESS");
            }
        });
        
        // Upload file button
        // CURRENTLY UNIMPLEMENTED
        Button fileSim = new Button("Try your own");
    
        // Container for page content
        Span content = new Span(newSim, fileSim, simContainer);
        setContent(content);
    }

    /**
     * Method to initialize the instruction table
     * @param g the grid to contain x86-64 program instructions 
     */
    public void initializeInstructionTable(Grid<x86ProgramLine> g) {
        g.setWidthFull();
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
        g.setSizeFull();
        g.removeAllColumns();
        g.addColumn(StackEntry::getStartAddress).setHeader("Start");
        g.addColumn(StackEntry::getEndAddress).setHeader("End");
        g.addColumn(StackEntry::getValue).setHeader("Value");
        g.addColumn(StackEntry::getOrigin)
            .setComparator(StackEntry.comparator)
            .setHeader("Line #");
        g.setItems(activeSimulation.getStackEntries());
    }

    /** 
    * Method to initialize the register table
    * @param g the table containing registers and their states 
    */
    public void initializeRegisterTable(Grid<Register> g) {
        g.setSizeFull();
        g.removeAllColumns();
        Comparator<Register> regComp = Register.comparator;
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
     * @param sf, zf, of, cf the status flags to update
     */
    public void updateTables(Grid<x86ProgramLine> g, Grid<StackEntry> s, Grid<Register> r, 
                                Label sf, Label zf, Label of, Label cf) {
        g.getSelectionModel().select(activeSimulation.getCurrentLine());
        scrollToSelectedInstruction(g);
        s.setItems(activeSimulation.getStackEntries());
        r.setItems(activeSimulation.getRegisters());
        updateStatusFlags(sf, zf, of, cf);
    }

    /**
     * Method to update the status flags
     * @param sf the sign flag
     * @param zf the zero flag
     * @param of the overflow flag
     * @param cf the carry flag
     */
    public void updateStatusFlags(Label sf, Label zf, Label of, Label cf) {
        sf.setText("SF: " + (activeSimulation.hasSignFlagSet() ? "1" : "0"));
        zf.setText("ZF: " + (activeSimulation.hasZeroFlagSet() ? "1" : "0"));
        of.setText("OF: " + (activeSimulation.hasOverflowFlagSet() ? "1" : "0"));
        cf.setText("CF: " + (activeSimulation.hasCarryFlagSet() ? "1" : "0"));
    }

    /**
     * Method to find the index of the current instruction and scroll to it
     * @param g the grid that contains the instruction list   
     */
    private static void scrollToSelectedInstruction(Grid g) {
        Set<x86ProgramLine> sourceIndexSet = g.getSelectedItems();
        ArrayList<x86ProgramLine> selectedIndices = new ArrayList<>(sourceIndexSet);
        if (!selectedIndices.isEmpty()) {
            int selectedIndex = selectedIndices.get(0).getLineNum();
            //int selectedIndex = selectedIndices.get(0) - 2;
            Notification.show("Selected Index: " + selectedIndices.get(0).getLineNum());
            if (selectedIndex < 0) {
                selectedIndex = 0;
            }
            scrollToIndex(g, selectedIndex);
       }
    }
    
    /**
     * JavaScript workaround because the scrollToRow method is missing 
     * in our version of Vaadin
     * @param grid the grid that contains the instruction list
     * @param index the index to scroll to 
      */
    public static void scrollToIndex(Grid<?> grid, int index) {
       UI.getCurrent().getPage().executeJavaScript("$0._scrollToIndex($1)", grid, index);
    }
}