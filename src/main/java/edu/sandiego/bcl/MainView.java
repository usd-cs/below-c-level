package edu.sandiego.bcl;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.FooterRow.FooterCell;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.AppLayoutMenu;
import com.vaadin.flow.component.applayout.AppLayoutMenuItem;
import com.vaadin.flow.component.HtmlContainer.*;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.selection.*;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import org.apache.commons.io.IOUtils;



/**
 * The main view contains a button and a click listener.
 */
@Route("")
@PWA(name = "Project Base for Vaadin Flow", shortName = "Project Base")
@HtmlImport("frontend://coolstyle.html")
@StyleSheet("frontend://table-styles.css")

public class MainView extends AppLayout {
    
    // Declare current sim
    private Simulation activeSimulation;
    
    // Declare Labels
    private Label stack;
    private Label register;
    private Label valueFormatLabel;
    private Label condFlags;
    private Label sfLabel;
    private Label ofLabel;
    private Label zfLabel;
    private Label cfLabel;
    private Label runtimeErrorMessage;

    // Instruction input
    private TextField instructionInput;

    // Declare grids
    private Grid<x86ProgramLine> instructionTable;
    private Grid<Register> registerTable;
    private Grid<StackEntry> stackTable;
    private Grid.Column<Register> regValueColumn;

    // Register table context menu
    private ComboBox<String> valueFormat;

    // Declare buttons
    private Button restart;
    private Button forward;
    private Button current;
    private Button back;
    private Button end;
    private Button clearSim;

    // Register display format int
    private int registerDisplayFormat;
    
    public MainView() {
        
        activeSimulation = new Simulation();

        // Image placement currently wrong, only displays alt text
        StreamResource res = new StreamResource("BelowCLevelHeader.png", () -> 
            // eg. load image data from classpath (src/main/resources/images/image.png)
            MainView.class.getClassLoader().getResourceAsStream("images/BelowCLevelHeader.png")
        );
        Image headerImage = new Image( res,"Alternative text description for logo image");
     //   setBranding(headerImage);

        // Navigation Bar
    //      AppLayoutMenu nav = createMenu();
  //        nav.getElement().getStyle().set("background", "white");
       // AppLayoutMenuItem home = new AppLayoutMenuItem("Home");
      //  AppLayoutMenuItem userGuide = new AppLayoutMenuItem("User Guide");
      //  AppLayoutMenuItem tutorials = new AppLayoutMenuItem("Tutorials");
      //  AppLayoutMenuItem feedback = new AppLayoutMenuItem("Feedback");
      //  nav.addMenuItems(home, userGuide, tutorials, feedback);
     
        // Set up x86-64 instruction list table
        instructionTable = new Grid<>(x86ProgramLine.class);
        GridSelectionModel<x86ProgramLine> iSelectionMode = instructionTable
            .setSelectionMode(Grid.SelectionMode.NONE);
        instructionTable.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        initializeInstructionTable();

        runtimeErrorMessage = new Label();
        runtimeErrorMessage.setWidthFull();
        runtimeErrorMessage.getStyle().set("color", "red");
    
        // Set up stack entry table
        stack = new Label("Program Stack");
        stack.getStyle().set("font-weight", "bold");
        stack.setWidthFull();
        stackTable = new Grid<>();
        GridSelectionModel<StackEntry> sSelectionMode = stackTable
            .setSelectionMode(Grid.SelectionMode.NONE);
        stackTable.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        initializeStackTable();
                       
        // Set up Combo Box for number value format
        valueFormatLabel = new Label("Select a Value Format:"); 
        valueFormat = new ComboBox<>();
        valueFormat.setItems("Hexidecimal", "Unsigned Decimal", "Signed Decimal");
        valueFormat.setPlaceholder("Select");
        valueFormat.setAllowCustomValue(false);
        valueFormat.addValueChangeListener( event -> {
          //  Notification.show("Selected option: " + event.getValue());
            if(event.getValue().equals("Unsigned Decimal")) {
                registerDisplayFormat = 1;
                updateSimulation();
            }
            else if(event.getValue().equals("Signed Decimal")) {
                registerDisplayFormat = 2;
                updateSimulation();
            }
            else {
                registerDisplayFormat = 0;
                updateSimulation();
            }
         });
        // Set up register table
        register = new Label("Register Table");
        register.getStyle().set("font-weight", "bold");
        register.setWidthFull();
        registerTable = new Grid<>();
        GridSelectionModel<Register> rSelectionMode = registerTable
            .setSelectionMode(Grid.SelectionMode.NONE);
        registerTable.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        initializeRegisterTable();

        // Set up status flags
        condFlags = new Label("Condition Flags");
        condFlags.getStyle().set("font-weight", "bold");

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
                    activeSimulation.appendToProgram(event.getValue());
                    instructionInput.setInvalid(false);
                    activeSimulation.restart();
                    instructionTable.getDataProvider().refreshAll();
                    updateSimulation();
                    instructionInput.clear();
                }
                catch (X86ParsingException e) {
                    instructionInput.setErrorMessage(e.getMessage());
                    instructionInput.setInvalid(true);
                }
            }
        });

        /**
         Create Buttons
         */

         // Container for buttons
         HorizontalLayout buttons = new HorizontalLayout();
         
         // Reset program to beginning of instructions (DONE)
         restart = new Button(new Icon(VaadinIcon.FAST_BACKWARD));
         restart.addClickListener( event -> {
             activeSimulation.restart();
             runtimeErrorMessage.setText(" ");
             updateSimulation();
         });
        
         // Undo the simulation of 1 instruction (DONE)
         back = new Button(new Icon(VaadinIcon.STEP_BACKWARD));
         back.addClickListener( event -> {
             activeSimulation.stepBackward();
             runtimeErrorMessage.setText(" ");
             updateSimulation();
         });

         // Jump to current instruction (DONE)
         current = new Button("Scroll to Current Instruction");
         current.addClickListener(event -> { 
          //   Notification.show("scrolling to current");
             scrollToSelectedInstruction();
         });
         current.setEnabled(false);
         current.getStyle().set("font-color", "#5271ffff");
         current.getStyle().set("background-color", "#eee6ff");
        
         // Simulate 1 instruction (DONE)
         forward = new Button(new Icon(VaadinIcon.STEP_FORWARD));
         forward.addClickListener(event -> {
             try { 
                 activeSimulation.stepForward();
                 updateSimulation();
             }
             catch(x86RuntimeException e) {
                 runtimeErrorMessage.setText("Line " + activeSimulation.getCurrentLine().lineNum + ": " + e.getMessage());
                 updateSimulation();
             }
         });

         // Skip to end of simulation
         end = new Button(new Icon(VaadinIcon.FAST_FORWARD));
         end.addClickListener( event -> {
             try {
                 if (!activeSimulation.finish()) {
                     Dialog dialog = new Dialog();

                     dialog.setCloseOnEsc(false);
                     dialog.setCloseOnOutsideClick(false);

                     VerticalLayout vl = new VerticalLayout();

                     Label l = new Label("Your program has executed many instructions. "
                        + "It is possible it may be stuck in an infinite loop. ");
                     vl.add(l);

                     Button confirmButton = new Button("Continue", confirmEvent -> {
                         dialog.close();
                         end.click();
                     });

                     Button cancelButton = new Button("Stop", cancelEvent -> {
                         dialog.close();
                     });

                     cancelButton.getStyle().set("color", "white");
                     cancelButton.getStyle().set("background", "red");
                     confirmButton.getStyle().set("color", "white");
                     confirmButton.getStyle().set("background", "blue");

                     //dialog.add(confirmButton, cancelButton);
                     HorizontalLayout hl = new HorizontalLayout();
                     hl.add(confirmButton, cancelButton);
                     vl.add(hl);
                     dialog.add(vl);
                     dialog.open();
                 }
                 updateSimulation();
             }
             catch(x86RuntimeException e) {
                 runtimeErrorMessage.setText("Line " + activeSimulation.getCurrentLine().lineNum + ": " + e.getMessage());
                 updateSimulation();
             }
             catch(Exception e) {
             }
         });
        buttons.add(restart, back, current, forward, end);
       // buttons.setWidthFull();
       // buttons.setPadding(true);
        buttons.setSpacing(true);

        // Start a new, blank simulation
        clearSim = new Button("Clear Simulation");
        clearSim.addClickListener( event -> {
         //   Notification.show("Starting new sim!");
            activeSimulation = new Simulation();
            instructionTable.getDataProvider().refreshAll();
            instructionTable.setItems(activeSimulation.getProgramLines());
            updateSimulation();
        });
        clearSim.setEnabled(false);
        clearSim.getStyle().set("font-color", "#5271ffff");
        clearSim.getStyle().set("background-color", "#eee6ff");
        HorizontalLayout tableFooter = new HorizontalLayout();
        tableFooter.add(clearSim, current);
        tableFooter.setAlignSelf(Alignment.START, clearSim);
        tableFooter.setAlignSelf(Alignment.END, current);
        tableFooter.setWidthFull();
        tableFooter.setSpacing(true);
//        tableFooter.setPadding(true);

        // Upload file button
        // CURRENTLY UNIMPLEMENTED
    //    Button fileSim = new Button("Try your own");
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
    //    upload.setUploadButton(fileSim);
        upload.setAcceptedFileTypes(".s");
        Label uploadLabel = new Label("Upload a .s file");
        upload.setDropLabel(uploadLabel);

        upload.addSucceededListener(event -> {
            try {
                File temp = new File(buffer.getFileName());
                FileOutputStream outputStream = new FileOutputStream(temp);
                IOUtils.copy(buffer.getInputStream(), outputStream);
                activeSimulation = new Simulation(temp);
                instructionTable.getDataProvider().refreshAll();
                instructionTable.setItems(activeSimulation.getProgramLines());
                updateSimulation();
            }
            catch(IOException e) {
             //   Notification.show("IOEXCEPT" + e.getMessage());
                e.getMessage();
            }

            catch(X86ParsingException e) {
                instructionInput.setErrorMessage(e.getMessage());
                instructionInput.setInvalid(true);
            }
        });

        // Container for left side of app
        VerticalLayout left = new VerticalLayout();
        left.add(upload, instructionInput, instructionTable, runtimeErrorMessage, tableFooter, buttons);
        left.setSpacing(false);
        left.setAlignSelf(Alignment.CENTER, buttons);
    //    left.setAlignItems(Alignment.CENTER);
   //    left.setPadding(false);

        VerticalLayout stackLayout = new VerticalLayout();
        stackLayout.add(stack, stackTable);
        stackLayout.setSpacing(false);
   //     stackLayout.setPadding(false);
        stackLayout.setSizeFull();

        VerticalLayout regLayout = new VerticalLayout();
        regLayout.add(register, registerTable);
        regLayout.setSizeFull();
        regLayout.setSpacing(false);
    //    regLayout.setPadding(false);

        // Container for right side of app
        VerticalLayout right = new VerticalLayout();
        right.add(stackLayout, regLayout, condFlags, statusFlags);
        right.setSizeFull();
        right.setSpacing(false);
        right.setAlignItems(Alignment.CENTER);
     //   right.setPadding(false);
 
       // Container for Simulation
        HorizontalLayout simContainer = new HorizontalLayout();
        simContainer.add(left, right);
        simContainer.setSpacing(false);
      //  simContainer.setMargin(true);
        simContainer.setClassName("sim-container");
     //   simContainer.setPadding(false);
       
        // For visibility and scroll testing purposes
        simContainer.setWidth("90%");
        simContainer.setHeight("80%");
        
        
    
        // Container for page content
        Span content = new Span(headerImage, simContainer);
        setContent(content);

        updateSimulation();
    }

    public void runForward() {
    }

    /**
     * Method to initialize the instruction table
     */
    public void initializeInstructionTable() {
        instructionTable.setWidthFull();
        instructionTable.removeAllColumns();
        instructionTable.addColumn(TemplateRenderer.<x86ProgramLine>of(
            "<div title='[[item.description]]' class='instruction'>[[item.Instruction]]</div>")
            .withProperty("Instruction", line -> line.toString())
            .withProperty("description", line -> line.getDescriptionString()))
            .setWidth("40%")
            .setHeader("Instruction");

        instructionTable.addComponentColumn(this::createInstructionActions)
            .setWidth("5%")
            .setHeader("Actions");
        
 /*       instructionTable.setClassNameGenerator(line -> {
            x86ProgramLine currLine = activeSimulation.getCurrentLine();
            if (currLine == null) {
                return "";
            }
            else if (line.getLineNum() == currLine.getLineNum()) {
                return "current-instruction";
            }
            else {
                return "";
            }
        });
   */     instructionTable.setClassName("instruction-table");

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
        stackTable.addColumn(stackEntry ->
            "0x" + Long.toHexString(stackEntry.getStartAddress())
                                    .toUpperCase().replaceFirst("F{4,}", "F..F"))
            .setHeader("Start");
        stackTable.addColumn(stackEntry ->
            "0x" + Long.toHexString(stackEntry.getEndAddress())
                                    .toUpperCase().replaceFirst("F{4,}", "F..F"))
            .setHeader("End");
        stackTable.addColumn(StackEntry::getValue).setHeader("Value");
        stackTable.addColumn(StackEntry::getOrigin).setHeader("Line #");
        stackTable.setClassName("stack-table");
        updateStackTable();
    }

    /** 
    * Method to initialize the register table 
    */
    public void initializeRegisterTable() {
        registerTable.setSizeFull();
        registerTable.removeAllColumns();
         
        registerTable.addColumn(TemplateRenderer.<Register> of(
        "<div class='register' title='[[item.name]]: [[item.8value]]\n"
        +"[[item.longRegName]]: [[item.4value]]\n"
        +"[[item.wordRegName]]: [[item.2value]]\n"
        +"[[item.byteLowRegName]]: [[item.1value]]'>"
        +"[[item.name]]</div>")
        .withProperty("name", Register::getName).withProperty("8value", 
            register -> register.getSubValue(8, registerDisplayFormat, false))
        .withProperty("longRegName", Register::getLongRegName).withProperty("4value", 
            register -> register.getSubValue(4, registerDisplayFormat, false))
        .withProperty("wordRegName", Register::getWordRegName).withProperty("2value", 
            register -> register.getSubValue(2, registerDisplayFormat, false))
        .withProperty("byteLowRegName", Register::getByteLowRegName).withProperty("1value", 
            register -> register.getSubValue(1, registerDisplayFormat, false)))
        .setHeader("Register").setFooter(valueFormatLabel);
       
        registerTable.addColumn( register ->
                register.getSubValue(8, registerDisplayFormat, true))
            .setHeader("Value") 
            .setFooter(valueFormat)
            .setId("valType");
        registerTable.addColumn(Register::getOrigin).setHeader("Line #");
        registerTable.setClassName("register-table");
        updateRegisterTable();
    }

    public void updateStackTable() {
        List<StackEntry> stackEntries = activeSimulation.getStackEntries();
        Collections.sort(stackEntries, StackEntry.comparator);
        stackTable.setItems(stackEntries);
        stackTable.setClassNameGenerator(stack -> {
            if(!stackEntries.isEmpty()){
                if (stack == stackEntries.get(stackEntries.size() - 1)) {
                    return "current-instruction";
                }
            }
            return "";
        });
    }
    public void updateRegisterTable() {
        List<Register> registers = activeSimulation.getRegisters();
        Collections.sort(registers, Register.comparator);
        registerTable.setItems(registers);
        registerTable.setClassNameGenerator(register -> {
            if(activeSimulation.getCurrentLine() != null) {
                Set<String> usedRegisters = activeSimulation.getCurrentLine()
                    .getUsedRegisters();
                if(usedRegisters.isEmpty()) {
                    return "";
                }
                else if(usedRegisters.contains(register.getName())) {
                    return "current-instruction";
                }
                else {
                    return "";
                }
            }
            return "";
        });
    }

    /**
     * Method to update the stack and register tables when an 
     * instruction is executed
     */
    public void updateSimulation() {
    //    instructionTable.getSelectionModel().select(activeSimulation.getCurrentLine());
        updateInstructionTable();
        scrollToSelectedInstruction();
        updateStackTable();
        updateRegisterTable();
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

    public void updateInstructionTable() {
        instructionTable.setClassNameGenerator(line -> {
        x86ProgramLine currLine = activeSimulation.getCurrentLine();
            if (currLine == null) {
                return "";
            }
            else if (line.getLineNum() == currLine.getLineNum()) {
                return "current-instruction";
            }
            else {
                return "";
            }
        });
    }
    /**
     * Method to find the index of the current instruction and scroll to it   
     */
    private void scrollToSelectedInstruction() {
        x86ProgramLine currLine = activeSimulation.getCurrentLine();
        if (currLine != null) {
            int selectedIndex = currLine.getLineNum();
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
        boolean simulationDone = activeSimulation.isFinished();
        boolean atBeginning = activeSimulation.isAtBeginning();
        boolean emptyProgram = activeSimulation.getProgramLines().isEmpty();

        // set enabled/disabled state for all buttons
        forward.setEnabled(!simulationDone);
        end.setEnabled(!simulationDone);
        current.setEnabled(!emptyProgram && !simulationDone);
        back.setEnabled(!atBeginning && !emptyProgram);
        restart.setEnabled(!atBeginning);
        clearSim.setEnabled(!emptyProgram);

        if (simulationDone && !emptyProgram) {
            runtimeErrorMessage.setText("Simulation Complete!");
        }

        // apply styling for buttons
        styleButton(restart);
        styleButton(back);
        styleButton(forward);
        styleButton(end);
    }

    /**
     * Method to create buttons for setting breakpoints, editing a line,
     * and deleting a line and place them in a Horizontal Layout.
     * // EDIT BUTTON CURRENTLY UNIMPLIMENTED
     * @param line the x86ProgramLine to target with actions
     */
    private HorizontalLayout createInstructionActions(x86ProgramLine line) {

        Icon breakPt = new Icon(VaadinIcon.DOT_CIRCLE);
        breakPt.setColor("#abaaaf");
        breakPt.addClickListener( event -> {
        //    Notification.show("Breakpoint button clicked!");
            line.toggleBreakpoint();
            if(line.getBreakpoint() == true) {
                breakPt.setColor("#5271ffff");
            }
            else {
                breakPt.setColor("#abaaaf");
            }
        });

        Icon edit = new Icon(VaadinIcon.EDIT);
        edit.addClickListener( event -> {
        //    Notification.show("Edit button clicked!");
        });

        Icon delete = new Icon(VaadinIcon.TRASH);
        delete.setColor("#abaaaf");
        delete.addClickListener( event -> {
        //    Notification.show("Remove button Clicked");
            activeSimulation.removeFromProgram(line);
            ListDataProvider<x86ProgramLine> dataProvider = (ListDataProvider<x86ProgramLine>) instructionTable
                .getDataProvider();
        dataProvider.getItems().remove(line);
        dataProvider.refreshAll();
        activeSimulation.restart();
        updateSimulation();
        });

        HorizontalLayout actionItems = new HorizontalLayout();
        actionItems.add(breakPt, delete);

        return actionItems;
    }

    public void styleButton(Button b) {
        Icon icon = (Icon)b.getIcon();
        icon.setSize("30px");
        if (b.isEnabled()) {
            icon.setColor("#5271ffff");
        }
        else {
            icon.setColor("gray");
        }
    }
}
