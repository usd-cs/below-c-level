package reversey.backy;

import java.io.File;
import javafx.scene.image.Image;
import javafx.fxml.FXML;
import javafx.scene.input.*;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import java.util.*;
import java.net.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
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
    private TabPane listViewTabPane;
    @FXML
    private Tab firstTab;

    /**
     * Map of tabs to their state.
     */
    private HashMap<Tab, SimState> simStateFromTab;

    // UI elements for adding new instructions
    @FXML
    private TextField instrText;
    @FXML
    private Label entryStatusLabel;
    @FXML
    private Label parseErrorText;

    @FXML
    private ListView<x86ProgramLine> instrList;

    // Simulation Control Buttons
    @FXML
    private Button nextInstr;
    @FXML
    private Button skipToEnd;
    @FXML
    private Button prevInstr;
    @FXML
    private Button skipToStart;
    @FXML
    private Button currInstr;

    // Fields for stack/memory table
    @FXML
    private TableView<StackEntry> stackTable;
    @FXML
    private TableColumn<StackEntry, String> startAddressCol;
    @FXML
    private TableColumn<StackEntry, String> endAddressCol;
    @FXML
    private TableColumn<StackEntry, String> valCol;
    @FXML
    private TableColumn<StackEntry, Integer> originCol;

    /**
     * List of stack entries in our current state.
     */
    ObservableList<StackEntry> stackTableList;

    // Fields for the register table
    @FXML
    private TableView<Register> promRegTable;
    @FXML
    private TableColumn<Register, String> registerName;
    @FXML
    private TableColumn<Register, String> registerVal;
    @FXML
    private TableColumn<Register, Integer> registerOrigin;

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
     * List of registers values in our current state.
     */
    private ObservableList<Register> registerTableList;

    private ListCell<x86ProgramLine> cellBeingEdited;

    private Simulation simulator;

    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        // Initialize stack table
        startAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getStartAddress()).toUpperCase()));

        endAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getEndAddress()).toUpperCase()));

        valCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));

        stackTableList = FXCollections.observableArrayList();
        stackTable.setItems(stackTableList.sorted(StackEntry.comparator));

        // Initialize the register table
        registerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        registerVal.setCellValueFactory(new PropertyValueFactory<>("value"));
        registerOrigin.setCellValueFactory(new PropertyValueFactory<>("origin"));

        registerTableList = FXCollections.observableArrayList();
        promRegTable.setItems(registerTableList.sorted(Register.comparator));

        promRegTable.setRowFactory(tableView -> {
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

        // Initialize the simulation state and create our first (blank) tab.
        simStateFromTab = new HashMap<>();
        simulator = new Simulation();
        listViewTabPane.getTabs().remove(firstTab);
        createTab(simulator);

        // Set up handlers for simulation control, both via buttons and menu
        // items.
        nextInstr.setOnAction(this::stepForward);
        forwardMenuItem.setOnAction(this::stepForward);

        skipToEnd.setOnAction(this::runForward);
        runMenuItem.setOnAction(this::runForward);

        /**
         * Event handler for "scroll back to current instruction" button.
         */
        currInstr.setOnAction(event -> {
            ObservableList<Integer> selectedIndices = instrList.getSelectionModel().getSelectedIndices();
            if (!selectedIndices.isEmpty()) {
                instrList.scrollTo(selectedIndices.get(0));
            }
        });

        prevInstr.setOnAction(this::stepBackward);
        backwardMenuItem.setOnAction(this::stepBackward);

        skipToStart.setOnAction(this::restartSim);
        restartMenuItem.setOnAction(this::restartSim);

        /**
         * Event handler for when user clicks button to insert a new
         * instruction.
         */
        instrText.setOnKeyPressed(this::parseAndAddInstruction);

        // Set up actions for the menubar
        exitMenuItem.setOnAction((event) -> System.exit(0));
        loadMenuItem.setOnAction(this::loadFile);
        saveAsMenuItem.setOnAction(this::saveFileAs);

        newMenuItem.setOnAction((event) -> {
            createTab(new Simulation());
        });

        // Add keyboard shortcuts
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

        /**
         * Event handler for "saveMenuItem" menu. This will save the current
         * simulation to a text file specified by the user if file does not
         * exist, and save changes to existing file.
         */
        // FIXME: this should be disabled until they save as or load from file
        // and should switch on and off based on which tab is selected
        saveMenuItem.setOnAction(event -> {
            simulator.saveProgram();
        });

        closeTabMenuItem.setOnAction(this::closeTab);

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

        //TODO: Resizing icons/nodes to pane
        // Initialize buttons with fancy graphics.
        ImageView skipToStartImgVw = new ImageView(new Image(getClass().getResourceAsStream("skipToStart.png")));
        ImageView prevInstrImgVw = new ImageView(new Image(getClass().getResourceAsStream("prevInstr.png")));
        ImageView currInstrImgVw = new ImageView(new Image(getClass().getResourceAsStream("currInstr.png")));
        ImageView nextInstrImgVw = new ImageView(new Image(getClass().getResourceAsStream("nextInstr.png")));
        ImageView skipToEndImgVw = new ImageView(new Image(getClass().getResourceAsStream("skipToEnd.png")));

        this.setIconsFitHeightAndWidth(skipToStartImgVw, prevInstrImgVw, currInstrImgVw,
                nextInstrImgVw, skipToEndImgVw, 35);

        skipToStart.setGraphic(skipToStartImgVw);
        prevInstr.setGraphic(prevInstrImgVw);
        currInstr.setGraphic(currInstrImgVw);
        nextInstr.setGraphic(nextInstrImgVw);
        skipToEnd.setGraphic(skipToEndImgVw);

        Platform.runLater(() -> {
        });

    }

    /**
     * Executes the next instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepForward(Event event) {
        simulator.stepForward();
        updateSimulatorUIElements();
    }

    /**
     * Executes instructions until it reaches the end of the program.
     *
     * @param event The event that triggered this action.
     */
    private void runForward(Event event) {
        while (!simulator.finish()) {
            Alert longRunningConfirmation = new Alert(AlertType.CONFIRMATION);
            longRunningConfirmation.setTitle("Long Running Computation");
            longRunningConfirmation.setHeaderText("Infinited Loop?");
            longRunningConfirmation.setContentText("Your program has executed many instructions. "
                    + "It is possible it may be stuck in an infinite loop. "
                    + "\n\nClick OK to continue simulation, or Cancel to stop.");

            Optional<ButtonType> result = longRunningConfirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                break;
            }
        }

        updateSimulatorUIElements();
    }

    /**
     * Undoes the previous instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepBackward(Event event) {
        simulator.stepBackward();
        updateSimulatorUIElements();
    }

    /**
     * Restarts simulation back to its starting state.
     *
     * @param event The event that triggered this action.
     */
    private void restartSim(Event event) {
        simulator.restart();
        updateSimulatorUIElements();
    }

    /**
     * Checks if end of program has been reached and if so, disable nextInstr
     * and skipToEnd buttons.
     */
    private void updateSimulationControls() {
        if (!simulator.isFinished()) {
            nextInstr.setOnAction(this::stepForward);
            skipToEnd.setOnAction(this::runForward);
        } else {
            nextInstr.setOnAction(null);
            skipToEnd.setOnAction(null);
        }
    }

    /**
     * Updates all the graphical elements of the simulator based on the currently
     * active simulation state.
     */
    private void updateSimulatorUIElements() {
        instrList.getSelectionModel().select(simulator.getCurrentLine());
        registerTableList.setAll(simulator.getRegisters());
        stackTableList.setAll(simulator.getStackEntries());
        updateStatusFlags();
        updateSimulationControls();
    }

    /**
     * Sets the currently selected tab as having unsaved changes.
     */
    public void indicateCurrentTabIsUnsaved() {
        Tab currTab = listViewTabPane.getSelectionModel().getSelectedItem();
        String currTabName = currTab.getText();

        // Already indicating we have an edited file so don't need to do anything
        if (!currTabName.endsWith("*")) {
            currTab.setText(currTabName + "*");
        }
    }
    
    public void indicateParsingError(X86ParsingException e) {
        // If we had a parsing error, set the background to pink,
        // select the part of the input that reported the error,
        // and set the error label's text.
        instrText.setStyle("-fx-control-inner-background: pink;");
        instrText.selectRange(e.getStartIndex(), e.getEndIndex());
        parseErrorText.setText(e.getMessage());
        ImageView errorPic = new ImageView(
                new Image(this.getClass().getResourceAsStream("error.png"), 16, 16, true, true));
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
            String text = instrText.getText();
            try {
                simulator.appendToProgram(text);

                // If we reach this point, the parsing was successful so get
                // rid of any error indicators that may have been set up.
                instrText.setStyle("-fx-control-inner-background: white;");
                parseErrorText.setText(null);
                parseErrorText.setGraphic(null);
                instrText.clear();
                restartSim(keyEvent);

                indicateCurrentTabIsUnsaved();

            } catch (X86ParsingException e) {
                this.indicateParsingError(e);
            }
        }
    }
    
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
                listViewTabPane.getSelectionModel().select(openTab.get());
            }
            else {
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
     * Saves current instrList as text file.
     *
     * @param event The event that triggered this action.
     */
    private void saveFileAs(Event event) {
        FileChooser saveFileChoice = new FileChooser();
        saveFileChoice.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("x86-64 assembly files (*.s)", "*.s"));
        
        File file = saveFileChoice.showSaveDialog(menuOptionsBar.getScene().getWindow());
        if (file != null) {
            listViewTabPane.getSelectionModel().getSelectedItem().setText(file.getName());
            simulator.saveProgramAs(file);
        }
    }

    // TODO: method comment
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
        sfLabel.setText("SF: " + (simulator.hasSignFlagSet() ? "1" : "0"));
        zfLabel.setText("ZF: " + (simulator.hasZeroFlagSet() ? "1" : "0"));
        ofLabel.setText("OF: " + (simulator.hasOverflowFlagSet() ? "1" : "0"));
        cfLabel.setText("CF: " + (simulator.hasCarryFlagSet() ? "1" : "0"));
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
        listViewTabPane.getTabs().add(t);

        t.setOnSelectionChanged((event) -> {
            if (t.isSelected()) {
                setAsActiveTab(t);
            }
            instrText.setOnKeyPressed(this::parseAndAddInstruction);
            instrText.setStyle("-fx-control-inner-background: white;");
            instrText.clear();
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
            if (listViewTabPane.getTabs().isEmpty()) {
                createTab(new Simulation());
            }
            simStateFromTab.remove(t);
        });
        
        listViewTabPane.getSelectionModel().select(t);
        setAsActiveTab(t);
    }

    /**
     * Closes the currently selected tab.
     *
     * @param e The event which caused the tab closing to trigger.
     */
    private void closeTab(Event e) {
        Tab currTab = listViewTabPane.getSelectionModel().getSelectedItem();
        currTab.getOnCloseRequest().handle(e);
        if (!e.isConsumed()) {
            listViewTabPane.getTabs().remove(currTab);
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
        instrList = simStateFromTab.get(t).getProgramView();
        simulator = simStateFromTab.get(t).getSimulator();
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
            simulator.removeFromProgram(cell.getItem());
            this.restartSim(null);
        });

        editItem.setOnAction(event -> {
            /* 
             * Visually indicate that text box will be used for editing by:
             * 1. Changing its background color and the background color of the
             *    item in the list.
             * 2. Updating label next to box to say that we are editing a line.
             */
            instrText.setStyle("-fx-control-inner-background: #77c0f4;");
            instrText.setText(cell.getItem().toString().substring(cell.getItem().toString().indexOf(":") + 1).trim());
            entryStatusLabel.setText("Editing line " + cell.getItem().getLineNum());
            cell.setStyle("-fx-background-color: #77c0f4;");
            cellBeingEdited = cell;

            // Change instruction entry box to replace instruction rather
            // than adding a new one at the end.
            instrText.setOnKeyPressed((KeyEvent keyEvent) -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    String text = instrText.getText();

                    try {
                        simulator.replaceInProgram(cell.getItem(), text);
                        indicateCurrentTabIsUnsaved();

                        instrText.setStyle("-fx-control-inner-background: white;");
                        parseErrorText.setText(null);
                        parseErrorText.setGraphic(null);
                        entryStatusLabel.setText(null);
                        instrText.clear();
                        
                        cell.setStyle(""); // previously background was set to blue
                        cellBeingEdited = null; // oh whale

                        this.restartSim(null);
                        // Out of editing mode so go back to default behavior
                        // for entering an instruction.
                        instrText.setOnKeyPressed(this::parseAndAddInstruction);
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
