package edu.sandiego.bcl;

import java.io.File;
import javafx.scene.image.Image;
import javafx.fxml.FXML;
import javafx.scene.input.*;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import java.util.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.scene.text.Font;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * Class that controls the main FXML file.
 *
 * @author Caitlin
 */
public class FXMLDocumentController implements Initializable {

    // Fields for the menu bar
    @FXML
    private MenuBar menuOptionsBar;
    @FXML
    private Menu fileOption;
    @FXML
    private Menu helpOption;
    @FXML
    private Menu editOption;
    @FXML
    private MenuItem exitMenuItem;
    @FXML
    private MenuItem newMenuItem;
    @FXML
    private MenuItem loadMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem saveAsMenuItem;
    @FXML
    private MenuItem closeTabMenuItem;
    @FXML
    private MenuItem forwardMenuItem;
    @FXML
    private MenuItem backwardMenuItem;
    @FXML
    private MenuItem runMenuItem;
    @FXML
    private MenuItem restartMenuItem;
    @FXML
    private MenuItem helpMenuItem;
    @FXML
    private MenuItem reportBugMenuItem;

    // Fields for TabPane
    @FXML
    private TabPane programTabs;
    @FXML
    private Tab firstTab;

    /**
     * Map of tabs to their state.
     */
    private HashMap<Tab, SimState> simStateFromTab;

    /**
     * Entry in program that is currently being edited. This is null if none are
     * currently being edited.
     */
    private ListCell<x86ProgramLine> cellBeingEdited;

    // UI elements for adding new instructions
    @FXML
    private TextField newLineEntry;
    @FXML
    private Label entryStatusLabel;
    @FXML
    private Label parseErrorText;

    @FXML
    private ListView<x86ProgramLine> programView;

    // Simulation State Label
    @FXML
    private Label simStateLabel;

    // Simulation Control Buttons
    @FXML
    private Button stepForwardButton;
    @FXML
    private Button runAllButton;
    @FXML
    private Button stepBackwardButton;
    @FXML
    private Button restartButton;
    @FXML
    private Button jumpToCurrentButton;

    // Fields for stack/memory table
    @FXML
    private TableView<StackEntry> stackTable;
    @FXML
    private TableColumn<StackEntry, String> startAddressColumn;
    @FXML
    private TableColumn<StackEntry, String> endAddressColumn;
    @FXML
    private TableColumn<StackEntry, String> valueColumn;
    @FXML
    private TableColumn<StackEntry, Integer> originColumn;

    /**
     * List of stack entries in our current state.
     */
    ObservableList<StackEntry> stackTableEntries;

    // Register Table UI Elements
    @FXML
    private TableView<Register> registerTable;
    @FXML
    private TableColumn<Register, String> registerNameColumn;
    @FXML
    private TableColumn<Register, String> registerValueColumn;
    @FXML
    private TableColumn<Register, Integer> registerOriginColumn;

    /**
     * List of registers values in our current state.
     */
    private ObservableList<Register> registerTableEntries;

    // Fields for status flag labels
    @FXML
    private Label sfLabel;
    @FXML
    private Label zfLabel;
    @FXML
    private Label ofLabel;
    @FXML
    private Label cfLabel;

    /**
     * The simulation that is active (i.e. currently open). This is changed when
     * the user selects a different tab.
     */
    private Simulation activeSimulation;

    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        initializeStackTable();
        initializeRegisterTable();
        initializeSimulationControls();
        initializeFileMenuItems();
        initializeHelpMenuItems();
        initializeKeyboardShortcuts();
        initializeButtonGraphics();

        // initialize "instruction entry" text box
        newLineEntry.setOnKeyPressed(this::parseAndAddInstruction);

        // Initialize the simulation state and create our first (blank) tab.
        simStateFromTab = new HashMap<>();
        activeSimulation = new Simulation();
        programTabs.getTabs().remove(firstTab);
        createTab(activeSimulation);

        Platform.runLater(() -> {
        });
    }

    private void initializeButtonGraphics() {
        //TODO: Resizing icons/nodes to pane
        ImageView skipToStartImgVw = new ImageView(new Image(getClass().getResourceAsStream("/images/skipToStart.png")));
        ImageView prevInstrImgVw = new ImageView(new Image(getClass().getResourceAsStream("/images/prevInstr.png")));
        ImageView currInstrImgVw = new ImageView(new Image(getClass().getResourceAsStream("/images/currInstr.png")));
        ImageView nextInstrImgVw = new ImageView(new Image(getClass().getResourceAsStream("/images/nextInstr.png")));
        ImageView skipToEndImgVw = new ImageView(new Image(getClass().getResourceAsStream("/images/skipToEnd.png")));

        this.setIconsFitHeightAndWidth(skipToStartImgVw, prevInstrImgVw, currInstrImgVw,
                nextInstrImgVw, skipToEndImgVw, 35);

        restartButton.setGraphic(skipToStartImgVw);
        stepBackwardButton.setGraphic(prevInstrImgVw);
        jumpToCurrentButton.setGraphic(currInstrImgVw);
        stepForwardButton.setGraphic(nextInstrImgVw);
        runAllButton.setGraphic(skipToEndImgVw);
    }

    private void initializeFileMenuItems() {
        exitMenuItem.setOnAction((event) -> System.exit(0));
        loadMenuItem.setOnAction(this::loadFile);
        saveAsMenuItem.setOnAction(this::saveFileAs);

        newMenuItem.setOnAction((event) -> {
            createTab(new Simulation());
        });

        saveMenuItem.setOnAction(event -> {
            if (!activeSimulation.saveProgram()) {
                this.saveFileAs(event);
            }
            else {
                this.setUnsavedTabIndicator(false);
            }
        });

        closeTabMenuItem.setOnAction(this::closeTab);
    }

    private void initializeHelpMenuItems() {
        /**
         * Event handler for "User Guide" menu item. This will create a WebView
         * that displays the user guide on the BCL GitHub wiki.
         */
        helpMenuItem.setOnAction((event) -> {
            WebView webby = new WebView();
            WebEngine wE = webby.getEngine();
            // Gives a StringIndexOutofBounds exception error but successfully pulls up window
            String url = "https://github.com/caf365/reverseybacky/wiki/Below-C-Level-User-Guide";
            wE.load(url);

            Scene scene = new Scene(webby, 700, 550);
            Stage helpStage = new Stage();
            helpStage.setTitle("Below C Level - Help");
            helpStage.setScene(scene);
            helpStage.show();
        });

        /**
         * Event handler for "Report Bug" menu item. This will create a WebView
         * that pulls up GitHub BCL Issues page.
         */
        reportBugMenuItem.setOnAction((event) -> {
            WebView webV = new WebView();
            WebEngine webE = webV.getEngine();
            // Gives a StringIndexOutofBounds exception error but successfully pulls up window
            String url = "https://github.com/caf365/reverseybacky/issues";
            webE.load(url);

            Scene s = new Scene(webV, 700, 550);
            Stage reportBugStage = new Stage();
            reportBugStage.setTitle("Below C Level - Report Bug");
            reportBugStage.setScene(s);
            reportBugStage.show();
        });
    }

    private void initializeKeyboardShortcuts() {
        newMenuItem.setMnemonicParsing(true);
        newMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N,
                KeyCombination.SHORTCUT_DOWN));
        loadMenuItem.setMnemonicParsing(true);
        loadMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O,
                KeyCombination.SHORTCUT_DOWN));
        closeTabMenuItem.setMnemonicParsing(true);
        closeTabMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W,
                KeyCombination.SHORTCUT_DOWN));
        exitMenuItem.setMnemonicParsing(true);
        exitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q,
                KeyCombination.SHORTCUT_DOWN));
        saveMenuItem.setMnemonicParsing(true);
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S,
                KeyCombination.SHORTCUT_DOWN));

        forwardMenuItem.setMnemonicParsing(true);
        forwardMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F,
                KeyCombination.SHORTCUT_DOWN));
        backwardMenuItem.setMnemonicParsing(true);
        backwardMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.B,
                KeyCombination.SHORTCUT_DOWN));
        restartMenuItem.setMnemonicParsing(true);
        restartMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.R,
                KeyCombination.SHORTCUT_DOWN));
        runMenuItem.setMnemonicParsing(true);
        runMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E,
                KeyCombination.SHORTCUT_DOWN));
    }

    private void initializeSimulationControls() {
        stepForwardButton.setOnAction(this::stepForward);
        stepForwardButton.setTooltip(new Tooltip("Step Forward"));       
        forwardMenuItem.setOnAction(this::stepForward);
        
        runAllButton.setOnAction(this::runForward);
        runAllButton.setTooltip(new Tooltip("Run"));
        runMenuItem.setOnAction(this::runForward);

        /**
         * Event handler for "scroll back to current instruction" button.
         */
        jumpToCurrentButton.setOnAction(event -> {
            scrollToSelectedInstruction();
        });
        jumpToCurrentButton.setTooltip(new Tooltip("Snap To Current"));

        stepBackwardButton.setOnAction(this::stepBackward);
        stepBackwardButton.setDisable(true);
        stepBackwardButton.setTooltip(new Tooltip("Step Backward"));
        backwardMenuItem.setOnAction(this::stepBackward);

        restartButton.setOnAction(this::restartSim);
        restartButton.setDisable(true);
        restartButton.setTooltip(new Tooltip("Restart"));
        restartMenuItem.setOnAction(this::restartSim);
    }

    private void scrollToSelectedInstruction() {
        ObservableList<Integer> selectedIndices = programView.getSelectionModel().getSelectedIndices();
        if (!selectedIndices.isEmpty()) {
            int selectedIndex = selectedIndices.get(0) - 2;
            if (selectedIndex < 0) {
                selectedIndex = 0;
            }
            programView.scrollTo(selectedIndex);
        }
    }

    private void initializeRegisterTable() {
        // Initialize the register table
        registerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        registerValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        registerOriginColumn.setCellValueFactory(new PropertyValueFactory<>("origin"));

        registerTableEntries = FXCollections.observableArrayList();
        registerTable.setItems(registerTableEntries.sorted(Register.comparator));

        registerTable.setRowFactory(tableView -> {
            final TableRow<Register> row = new TableRow<>();

            row.hoverProperty().addListener((observable) -> {
                final Register reg = row.getItem();

                if (row.isHover() && reg != null) {
                    reg.setSubName(reg.getName());
                    String s = reg.getName() + ": " + reg.getSubValue(8) + "\n"
                            + reg.getLongRegName() + ": " + reg.getSubValue(4)
                            + "\n" + reg.getWordRegName() + ": " + reg.getSubValue(2)
                            + "\n" + reg.getByteLowRegName() + ": " + reg.getSubValue(1);
                    Tooltip t = new Tooltip(s);
                    row.setTooltip(t);
                }
            });

            return row;
        });

        initializeRegisterTableContextMenu();
    }

    /**
     * Creates and initializes a right-click menu for the Register Table.
     */
    private void initializeRegisterTableContextMenu() {
        ToggleGroup valueFormatGroup = new ToggleGroup();
        
        // Radio select group for the format the value column is displayed in
        final RadioMenuItem hexMenuItem = new RadioMenuItem("Hexadecimal");
        hexMenuItem.setToggleGroup(valueFormatGroup);
        hexMenuItem.setSelected(true);
        final RadioMenuItem unsignedDecMenuItem = new RadioMenuItem("Unsigned Decimal");
        unsignedDecMenuItem.setToggleGroup(valueFormatGroup);
        final RadioMenuItem signedDecMenuItem = new RadioMenuItem("Signed Decimal");
        signedDecMenuItem.setToggleGroup(valueFormatGroup);
        
        Menu valueFormatMenu = new Menu("Value Format");
        valueFormatMenu.getItems().addAll(hexMenuItem,
                                            unsignedDecMenuItem,
                                            signedDecMenuItem);
    
        hexMenuItem.setOnAction((ActionEvent event) -> {
            if (hexMenuItem.isSelected()) {
                activeSimulation.setRegisterBase(0);
                this.updateSimulatorUIElements();
            }
        });

        unsignedDecMenuItem.setOnAction((ActionEvent event) -> {
            if (unsignedDecMenuItem.isSelected()) {
                activeSimulation.setRegisterBase(1);
                this.updateSimulatorUIElements();
            }
        });

        signedDecMenuItem.setOnAction((ActionEvent event) -> {
            if (signedDecMenuItem.isSelected()) {
                activeSimulation.setRegisterBase(2);
                this.updateSimulatorUIElements();
            }
        });
        
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(valueFormatMenu);
        registerTable.setContextMenu(menu);
    }

    private void initializeStackTable() {
        startAddressColumn.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getStartAddress()).toUpperCase()));

        endAddressColumn.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getEndAddress()).toUpperCase()));

        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        originColumn.setCellValueFactory(new PropertyValueFactory<>("origin"));

        stackTableEntries = FXCollections.observableArrayList();
        stackTable.setItems(stackTableEntries.sorted(StackEntry.comparator));
    }

    /**
     * Executes the next instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepForward(Event event) {
        try {
            activeSimulation.stepForward();
        } catch (x86RuntimeException ex) {
            showRuntimeErrorDialogue(ex);
        }
        updateSimulatorUIElements();
    }

    /**
     * Executes instructions until it reaches the end of the program.
     *
     * @param event The event that triggered this action.
     */
    private void runForward(Event event) {
        try {
            while (!activeSimulation.finish()) {
                Alert longRunningConfirmation = new Alert(AlertType.CONFIRMATION);
                longRunningConfirmation.setTitle("Long Running Computation");
                longRunningConfirmation.setHeaderText("Infinite Loop?");
                longRunningConfirmation.setContentText("Your program has executed many instructions. "
                        + "It is possible it may be stuck in an infinite loop. "
                        + "\n\nClick OK to continue simulation, or Cancel to stop.");
                
                Optional<ButtonType> result = longRunningConfirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                    break;
                }
            }
        } catch (x86RuntimeException ex) {
            showRuntimeErrorDialogue(ex);
        }
        updateSimulatorUIElements();
    }

    private void showRuntimeErrorDialogue(x86RuntimeException e){
        simStateLabel.setText("Line " + activeSimulation.getCurrentLine().lineNum + ": " + e.getMessage());
            ImageView completePic = new ImageView(
                    new Image(this.getClass().getResourceAsStream("/images/error.png")));
            completePic.setFitHeight(16);
            completePic.setFitWidth(16);
            completePic.setSmooth(true);
            completePic.setPreserveRatio(true);
            simStateLabel.setGraphic(completePic);
        /*
        Alert evalError = new Alert(Alert.AlertType.ERROR);
        evalError.setTitle("Simulation Error");
        evalError.setHeaderText("Error during simulation");
        evalError.setContentText("The following error occurred while simulating the instruction on line number " + activeSimulation.getCurrentLine().lineNum
                    + "\n\n" + e.getMessage());
            evalError.showAndWait();
        */
    }
    /**
     * Undoes the previous instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepBackward(Event event) {
        activeSimulation.stepBackward();
        updateSimulatorUIElements();
    }

    /**
     * Restarts simulation back to its starting state.
     *
     * @param event The event that triggered this action.
     */
    private void restartSim(Event event) {
        activeSimulation.restart();
        updateSimulatorUIElements();
    }

    /**
     * Enables and disables simulation controls based on state of simulator.
     * 
     */
    private void updateSimulationControls() {
        stepForwardButton.setDisable(activeSimulation.isFinished());
        runAllButton.setDisable(activeSimulation.isFinished());
        jumpToCurrentButton.setDisable(activeSimulation.getProgramLines().isEmpty() || activeSimulation.isFinished());
        stepBackwardButton.setDisable(activeSimulation.isAtBeginning());
        restartButton.setDisable(activeSimulation.isAtBeginning());
    }

    /**
     * Updates all the graphical elements of the simulator based on the
     * currently active simulation state.
     */
    private void updateSimulatorUIElements() {
        programView.getSelectionModel().select(activeSimulation.getCurrentLine());
        registerTableEntries.setAll(activeSimulation.getRegisters());
        stackTableEntries.setAll(activeSimulation.getStackEntries());
        updateStatusFlags();
        updateSimulationControls();
        scrollToSelectedInstruction();
        if (activeSimulation.isFinished() && !activeSimulation.getProgramLines().isEmpty()) {
            simStateLabel.setText("Simulation Complete");
            ImageView completePic = new ImageView(
                    new Image(this.getClass().getResourceAsStream("/images/checkmark.png")));
            completePic.setFitHeight(16);
            completePic.setFitWidth(16);
            completePic.setSmooth(true);
            completePic.setPreserveRatio(true);
            simStateLabel.setGraphic(completePic);
        } else if (!activeSimulation.getStuckOnError()) {
            simStateLabel.setText(null);
            simStateLabel.setGraphic(null);
        }
    }

    /**
     * Visually indicates to the user whether the current tab is unsaved.
     * 
     * @param isUnsaved Whether the tab is unsaved or not.
     */
    public void setUnsavedTabIndicator(boolean isUnsaved) {
        Tab currTab = programTabs.getSelectionModel().getSelectedItem();
        String currTabName = currTab.getText();
        
        if (isUnsaved) {
            // Already indicating we have an edited file so don't need to do anything
            if (!currTabName.endsWith("*")) {
                currTab.setText(currTabName + "*");
            }
        }
        else {
            if (currTabName.endsWith("*")) {
                currTab.setText(currTabName.substring(0, currTabName.length()-1));
            }
        }
    }

    /**
     * Update UI to indicate that a parsing error was encountered.
     *
     * @param e The parsing error that was encountered.
     */
    public void indicateParsingError(X86ParsingException e) {
        // If we had a parsing error, set the background to pink,
        // select the part of the input that reported the error,
        // and set the error label's text.
        newLineEntry.setStyle("-fx-control-inner-background: pink;");
        newLineEntry.selectRange(e.getStartIndex(), e.getEndIndex());
        parseErrorText.setText(e.getMessage());
        ImageView errorPic = new ImageView(
                new Image(this.getClass().getResourceAsStream("/images/error.png")));
        errorPic.setFitHeight(16);
        errorPic.setFitWidth(16);
        errorPic.setSmooth(true);
        errorPic.setPreserveRatio(true);
        parseErrorText.setGraphic(errorPic);
    }

    /**
     * Gets input from instruction entry text field, parses it, and (if
     * successful) adds it to the end of the instruction list.
     *
     * @param keyEvent The event that caused the handler to engage.
     */
    private void parseAndAddInstruction(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            String text = newLineEntry.getText();
            try {
                activeSimulation.appendToProgram(text);

                // If we reach this point, the parsing was successful so get
                // rid of any error indicators that may have been set up.
                newLineEntry.setStyle("-fx-control-inner-background: white;");
                parseErrorText.setText(null);
                parseErrorText.setGraphic(null);
                newLineEntry.clear();
                restartSim(keyEvent);

                setUnsavedTabIndicator(true);

            } catch (X86ParsingException e) {
                this.indicateParsingError(e);
            }
        }
    }

    /**
     * Optionally returns the open tab associated with the given filename.
     *
     * @param fileName Name of the file to look for in open tabs.
     * @return Tab associated with that file, or an empty optional if one
     * doesn't exist.
     */
    private Optional<Tab> getTabIfOpen(String fileName) {
        for (Map.Entry<Tab, SimState> entry : simStateFromTab.entrySet()) {
            String tabFileName = entry.getValue().getSimulator().getProgramFileName();
            if (tabFileName != null && tabFileName.equals(fileName)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * This will reset the simulation, returning to the very first instruction
     * of the loaded text file.
     *
     * @param event The event that triggered this action.
     */
    private void loadFile(Event event) {
        FileChooser loadFileChoice = new FileChooser();
        loadFileChoice.setTitle("Open File");
        loadFileChoice.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("x86-64 assembly files (*.s)", "*.s"));

        File fileToLoad = loadFileChoice.showOpenDialog(menuOptionsBar.getScene().getWindow());
        if (fileToLoad != null) {
            Optional<Tab> openTab = this.getTabIfOpen(fileToLoad.getName());
            if (openTab.isPresent()) {
                programTabs.getSelectionModel().select(openTab.get());
            } else {
                try {
                    Simulation newSim = new Simulation(fileToLoad);
                    createTab(newSim);
                } catch (X86ParsingException e) {
                    // Info about this is already given to them so just ignore it here.
                } catch (Exception e) {
                    // TODO: make this visual
                    System.out.println(e);
                }
            }
        }
    }

    /**
     * Saves current programView as text file.
     *
     * @param event The event that triggered this action.
     */
    private void saveFileAs(Event event) {
        FileChooser saveFileChoice = new FileChooser();
        saveFileChoice.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("x86-64 assembly files (*.s)", "*.s"));

        File file = saveFileChoice.showSaveDialog(menuOptionsBar.getScene().getWindow());
        if (file != null) {
            programTabs.getSelectionModel().getSelectedItem().setText(file.getName());
            if (activeSimulation.saveProgramAs(file)) {
                this.setUnsavedTabIndicator(false);
            }
        }
    }

    private void setIconsFitHeightAndWidth(ImageView i, ImageView j, ImageView k,
            ImageView l, ImageView m, int size) {
        i.setFitHeight(size);
        i.setFitWidth(size);
        j.setFitHeight(size);
        j.setFitWidth(size);
        k.setFitHeight(size);
        k.setFitWidth(size);
        l.setFitHeight(size);
        l.setFitWidth(size);
        m.setFitHeight(size);
        m.setFitWidth(size);
    }

    /**
     * Sets the "Status Flags" display based on the current simulation state.
     */
    private void updateStatusFlags() {
        sfLabel.setText("SF: " + (activeSimulation.hasSignFlagSet() ? "1" : "0"));
        zfLabel.setText("ZF: " + (activeSimulation.hasZeroFlagSet() ? "1" : "0"));
        ofLabel.setText("OF: " + (activeSimulation.hasOverflowFlagSet() ? "1" : "0"));
        cfLabel.setText("CF: " + (activeSimulation.hasCarryFlagSet() ? "1" : "0"));
    }

    /**
     * Creates new tab and adds addNewTab to the end of the current list of tabs
     */
    private void createTab(Simulation sim) {
        Tab t = new Tab(sim.getProgramFileName());
        ListView<x86ProgramLine> programView = new ListView<>(sim.getProgramLines());
        programView.setCellFactory(this::instructionListCellFactory);
        t.setContent(programView);

        simStateFromTab.put(t, new SimState(programView, sim));
        programTabs.getTabs().add(t);

        t.setOnSelectionChanged((event) -> {
            if (t.isSelected()) {
                setAsActiveTab(t);
            }
            newLineEntry.setOnKeyPressed(this::parseAndAddInstruction);
            newLineEntry.setStyle("-fx-control-inner-background: white;");
            newLineEntry.clear();
            entryStatusLabel.setText(null);
            if (cellBeingEdited != null) {
                cellBeingEdited.setStyle("");
                cellBeingEdited = null;
            }
        });

        t.setOnCloseRequest((event) -> {
            if (simStateFromTab.get(t).getSimulator().isProgramUnsaved()) {
                Alert closingConfirmation = new Alert(AlertType.CONFIRMATION);
                closingConfirmation.setTitle("Closing Tab Confirmation");
                closingConfirmation.setHeaderText("Unsaved changes");
                closingConfirmation.setContentText("Selecting OK will close this file immediately. Any unsaved changes will be lost.");
                closingConfirmation.showAndWait()
                        .filter(response -> response == ButtonType.CANCEL)
                        .ifPresent(response -> event.consume());
            }
        });

        t.setOnClosed((event) -> {
            if (programTabs.getTabs().isEmpty()) {
                createTab(new Simulation());
            }
            simStateFromTab.remove(t);
        });

        programTabs.getSelectionModel().select(t);
        setAsActiveTab(t);
    }

    /**
     * Closes the currently selected tab.
     *
     * @param e The event which caused the tab closing to trigger.
     */
    private void closeTab(Event e) {
        Tab currTab = programTabs.getSelectionModel().getSelectedItem();
        currTab.getOnCloseRequest().handle(e);
        if (!e.isConsumed()) {
            programTabs.getTabs().remove(currTab);
            currTab.getOnClosed().handle(e);
        }
    }

    /**
     * Sets the given tab as the active tab, including setting the stack,
     * registers, and instruction list to be those associated with this tab.
     *
     * @param t The tab to make active.
     */
    private void setAsActiveTab(Tab t) {
        programView = simStateFromTab.get(t).getProgramView();
        activeSimulation = simStateFromTab.get(t).getSimulator();
        updateSimulatorUIElements();
    }

    /**
     * Custom cell factory for instruction list entry. This method creates a
     * custom ListCell class then sets up the right-click context menu for this
     * custom cell.
     *
     * @param lv The ListView of x86 instructions we're working with.
     * @return The custom listcell that was constructed.
     */
    private ListCell<x86ProgramLine> instructionListCellFactory(ListView<x86ProgramLine> lv) {
        ListCell<x86ProgramLine> cell = new ListCell<x86ProgramLine>() {
            @Override
            protected void updateItem(x86ProgramLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setFont(new Font("Courier", 14));
                    setText(item.toString());
                    Circle circle = new Circle(4);
                    circle.setFill(Color.TRANSPARENT);
                    setGraphic(circle);
                }
            }
        };

        // Tooltip will show up just to the right of the mouse when we enter
        // this cell and disappear as soon as we leave the cell.
        final Tooltip descriptionTip = new Tooltip();

        cell.setOnMouseEntered(event -> {
            if (cell.getItem() != null) {
                descriptionTip.setText(cell.getItem().getDescriptionString());
                Point2D p = cell.localToScreen(event.getX() + 5, event.getY());
                descriptionTip.show(cell, p.getX(), p.getY());
            }
        });

        cell.setOnMouseExited(event -> descriptionTip.hide());

        // Set up the right click context menu, including the actions to take
        // for each of the menu entries.
        ContextMenu rightClickMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete");
        MenuItem editItem = new MenuItem("Edit");
        MenuItem toggleBreakpointItem = new MenuItem("Toggle breakpoint");

        deleteItem.setOnAction(event -> {
            activeSimulation.removeFromProgram(cell.getItem());
            this.restartSim(null);
        });

        editItem.setOnAction(event -> {
            /* 
             * Visually indicate that text box will be used for editing by:
             * 1. Changing its background color and the background color of the
             *    item in the list.
             * 2. Updating label next to box to say that we are editing a line.
             */
            newLineEntry.setStyle("-fx-control-inner-background: #77c0f4;");
            newLineEntry.setText(cell.getItem().toString().substring(cell.getItem().toString().indexOf(":") + 1).trim());
            entryStatusLabel.setText("Editing line " + cell.getItem().getLineNum());
            cell.setStyle("-fx-background-color: #77c0f4;");
            cellBeingEdited = cell;

            // Change instruction entry box to replace instruction rather
            // than adding a new one at the end.
            newLineEntry.setOnKeyPressed((KeyEvent keyEvent) -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    String text = newLineEntry.getText();

                    try {
                        activeSimulation.replaceInProgram(cell.getItem(), text);
                        setUnsavedTabIndicator(true);

                        newLineEntry.setStyle("-fx-control-inner-background: white;");
                        parseErrorText.setText(null);
                        parseErrorText.setGraphic(null);
                        entryStatusLabel.setText(null);
                        newLineEntry.clear();

                        cell.setStyle(""); // previously background was set to blue
                        cellBeingEdited = null; // oh whale

                        this.restartSim(null);
                        // Out of editing mode so go back to default behavior
                        // for entering an instruction.
                        newLineEntry.setOnKeyPressed(this::parseAndAddInstruction);
                    } catch (X86ParsingException e) {
                        this.indicateParsingError(e);
                    }
                }
            });
        });

        // Event handler for toggling the breakpoint status of an instruction.
        toggleBreakpointItem.setOnAction(event -> {
            cell.getItem().toggleBreakpoint();

            // Breakpoints are indicated by a black circle
            if (cell.getItem().getBreakpoint()) {
                cell.setGraphic(new Circle(4));
            } else {
                Circle c = new Circle(4);
                c.setFill(Color.TRANSPARENT);
                cell.setGraphic(c);
            }
        });

        rightClickMenu.getItems().addAll(editItem, toggleBreakpointItem, deleteItem);

        cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY && !cell.isEmpty()) {
                lv.getFocusModel().focus(1);
                cell.setContextMenu(rightClickMenu);
            }
            event.consume();
        });
        return cell;
    }
}
