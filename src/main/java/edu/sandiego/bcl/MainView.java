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
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.selection.*;
import com.vaadin.flow.server.StreamResource;
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
    
    // Declare Labels
    private Label stack;
    private Label register;
    private Label valueFormatLabel;
    private Label sfLabel;
    private Label ofLabel;
    private Label zfLabel;
    private Label cfLabel;

    // Instruction input
    private TextField instructionInput;

    // Declare grids
    private Grid<x86ProgramLine> instructionTable;
    private Grid<Register> registerTable;
    private Grid<StackEntry> stackTable;

    // Register table context menu
    private ComboBox<String> valueFormat;

    // Declare buttons
    private Button restart;
    private Button forward;
    private Button current;
    private Button back;
    private Button end;

    public MainView() {
        
        activeSimulation = new Simulation();

        // Image placement currently wrong, only displays alt text
        StreamResource res = new StreamResource("BCLHeader.png", () -> 
            // eg. load image data from classpath (src/main/resources/images/image.png)
            MainView.class.getClassLoader().getResourceAsStream("images/BCLHeader.png")
        );
        Image headerImage = new Image( res,"Alternativ text description for logo image");
        setBranding(headerImage);

        // Navigation Bar
        AppLayoutMenu nav = createMenu();
        AppLayoutMenuItem home = new AppLayoutMenuItem("Home");
        AppLayoutMenuItem userGuide = new AppLayoutMenuItem("User Guide");
        AppLayoutMenuItem tutorials = new AppLayoutMenuItem("Tutorials");
        AppLayoutMenuItem feedback = new AppLayoutMenuItem("Feedback");
        nav.addMenuItems(home, userGuide, tutorials, feedback);
     
        // Set up x86-64 instruction list table
        instructionTable  = new Grid<>(x86ProgramLine.class);
        GridSelectionModel<x86ProgramLine> selectionMode = instructionTable
            .setSelectionMode(Grid.SelectionMode.SINGLE);
        initializeInstructionTable();
    
        // Set up stack entry table
        stack = new Label("Program Stack");
        stack.setWidthFull();
        stackTable = new Grid<>();
        initializeStackTable();
                       
        // Set up Combo Box for number value format
        valueFormatLabel = new Label("Select a Value Format:"); 
        valueFormat = new ComboBox<>();
        valueFormat.setItems("Hexidecimal", "Unsigned Decimal", "Signed Decimal");
        valueFormat.setPlaceholder("Select an option");
        valueFormat.addValueChangeListener( event ->
            Notification.show("Selected option: " + event.getValue()));
        
        // Set up register table
        register = new Label("Register View");
        register.setWidthFull();
        registerTable = new Grid<>();
        registerTable.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        initializeRegisterTable();

        // Set up status flags
        HorizontalLayout statusFlags = new HorizontalLayout();
        sfLabel = new Label();
        zfLabel = new Label();
        ofLabel = new Label();
        cfLabel = new Label();
        updateStatusFlags();
        statusFlags.add(sfLabel, zfLabel, ofLabel, cfLabel);
        
        // Set up instruction input field
        instructionInput = new TextField();
        initializeInputField();

        // Set up instruction input field event listener
        instructionInput.addValueChangeListener(event -> { 
            if(event.getValue().length() > 0) {
                try {  
                    Notification.show("event: " + event.getValue());
                    activeSimulation.appendToProgram(event.getValue());
                    instructionInput.setInvalid(false);
                    Notification.show("Successfully added!");
                    instructionTable.getDataProvider().refreshAll();
                    updateSimulation();
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
         restart = new Button("Restart");
         restart.addClickListener( event -> {
             activeSimulation.restart();
             Notification.show("Simulation Reset");
             updateSimulation();
             instructionTable.setItems(activeSimulation.getProgramLines());
         });
         restart.setEnabled(false);
        
         // Undo the simulation of 1 instruction (DONE)
         back = new Button("Step Back");
         back.addClickListener( event -> {
                 activeSimulation.stepBackward();
                 Notification.show("Backward");
                 updateSimulation();
         });
         back.setEnabled(false);
        
         // Jump to current instruction (DONE)
         current = new Button("Play");
         current.addClickListener(event -> { 
             Notification.show("Play");
             scrollToSelectedInstruction();
         });
         current.setEnabled(false);
        
         // Simulate 1 instruction (DONE)
         forward = new Button("Step Forward");
         forward.addClickListener(event -> {
             try { 
                 activeSimulation.stepForward();
                 Notification.show("Forward");
                 updateSimulation();
                 }
             catch(x86RuntimeException e) {
                Notification.show("Forward: x86RuntimeException");
             }
         });
         forward.setEnabled(false);

         // Skip to end of simulation
         end = new Button("End");
         end.addClickListener( event -> {
             try {
                 Notification.show("Execute simulation");
                 activeSimulation.finish();
                 updateSimulation();
                 }
             catch(x86RuntimeException e) {
                 Notification.show("End: x86RuntimeException");
             }
         });
         end.setEnabled(false);
        
        buttons.add(restart, back, current, forward, end);
        
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
        right.add(stackLayout, regLayout, statusFlags);
        right.setSizeFull(); 
 
       // Container for Simulation
        HorizontalLayout simContainer = new HorizontalLayout();
        simContainer.add(left, right);
        
        // For visibility and scroll testing purposes
        simContainer.setWidth("90%");
        simContainer.setHeight("90%");
    
        // Start a new, blank simulation
        Button newSim = new Button("Start a New Simulation");
        newSim.addClickListener( event -> {
            Notification.show("Starting new sim!");
            activeSimulation.createNewSim();
            instructionTable.getDataProvider().refreshAll();
            instructionTable.setItems(activeSimulation.getProgramLines());
            updateSimulation();
            
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
     */
    public void initializeInstructionTable() {
        instructionTable.setWidthFull();
        instructionTable.removeAllColumns();
        instructionTable.addColumn(TemplateRenderer.<x86ProgramLine>of(
            "<div title='[[item.description]]'>[[item.Instruction]]</div>")
            .withProperty("Instruction", line -> line.toString())
            .withProperty("description", line -> line.getDescriptionString()))
            .setHeader("Instruction");
        instructionTable.setItems(activeSimulation.getProgramLines());    
    }

    /**
    * Method to initialize the instruction input field
    */
    public void initializeInputField() {
        instructionInput.setWidthFull();
        instructionInput.setPlaceholder("Enter an x86 instruction or comment");
        instructionInput.setClearButtonVisible(true);
    }

    /** 
    * Method to initialize the stack table
    */
    public void initializeStackTable() {
        stackTable.setSizeFull();
        stackTable.removeAllColumns();
        stackTable.addColumn(StackEntry::getStartAddress).setHeader("Start");
        stackTable.addColumn(StackEntry::getEndAddress).setHeader("End");
        stackTable.addColumn(StackEntry::getValue).setHeader("Value");
        stackTable.addColumn(StackEntry::getOrigin)
            .setComparator(StackEntry.comparator)
            .setHeader("Line #");
        stackTable.setItems(activeSimulation.getStackEntries());
    }

    /** 
    * Method to initialize the register table 
    */
    public void initializeRegisterTable() {
        registerTable.setSizeFull();
        registerTable.removeAllColumns();
        registerTable.addColumn(Register::getName).setHeader("Register")
            .setFooter(valueFormatLabel);
        registerTable.addColumn(Register::getValue).setHeader("Value")
            .setFooter(valueFormat);
        registerTable.addColumn(Register::getOrigin).setHeader("Line #");
        registerTable.setItems(activeSimulation.getRegisters());
        
    }

    /**
     * Method to update the stack and register tables when an 
     * instruction is executed
     */
    public void updateSimulation() {
        instructionTable.getSelectionModel().select(activeSimulation.getCurrentLine());
        scrollToSelectedInstruction();
        stackTable.setItems(activeSimulation.getStackEntries());
        registerTable.setItems(activeSimulation.getRegisters());
        updateStatusFlags();
        updateSimulationControls();
    }

    /**
     * Method to update the status flags
     */
    public void updateStatusFlags() {
        sfLabel.setText("SF: " + (activeSimulation.hasSignFlagSet() ? "1" : "0"));
        zfLabel.setText("ZF: " + (activeSimulation.hasZeroFlagSet() ? "1" : "0"));
        ofLabel.setText("OF: " + (activeSimulation.hasOverflowFlagSet() ? "1" : "0"));
        cfLabel.setText("CF: " + (activeSimulation.hasCarryFlagSet() ? "1" : "0"));
    }

    /**
     * Method to find the index of the current instruction and scroll to it   
     */
    private void scrollToSelectedInstruction() {
        Set<x86ProgramLine> sourceIndexSet = instructionTable.getSelectedItems();
        ArrayList<x86ProgramLine> selectedIndices = new ArrayList<>(sourceIndexSet);
        if (!selectedIndices.isEmpty()) {
            int selectedIndex = selectedIndices.get(0).getLineNum();
            Notification.show("Selected Index: " + selectedIndices.get(0).getLineNum());
            if (selectedIndex < 0) {
                selectedIndex = 0;
            }
            scrollToIndex(selectedIndex);
       }
    }
    
    /**
     * JavaScript workaround because the scrollToRow method is missing 
     * in our version of Vaadin
     * @param index the index to scroll to 
      */
    public void scrollToIndex(int index) {
       UI.getCurrent().getPage().executeJavaScript("$0._scrollToIndex($1)", instructionTable, index);
    }

    /**
     * Method to update simulation buttons
     */
    private void updateSimulationControls() {
        forward.setEnabled(!activeSimulation.isFinished());
        end.setEnabled(!activeSimulation.isFinished());
        current.setEnabled(!activeSimulation.getProgramLines().isEmpty() || activeSimulation.isFinished());
        back.setEnabled(!activeSimulation.isAtBeginning());
        restart.setEnabled(!activeSimulation.isAtBeginning());
    }
}
