package reversey.backy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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
    private MenuItem forwardMenuItem;
    @FXML
    private MenuItem backwardMenuItem;
    @FXML
    private MenuItem runMenuItem;
    @FXML
    private MenuItem restartMenuItem;
    @FXML
    private MenuItem aboutMenuItem;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem reorderMenuItem;

    @FXML
    private BorderPane entirePane;

    // Fields for TabPane
    @FXML
    private TabPane listViewTabPane;
    @FXML
    private Tab firstTab;

    /**
     * Map of tabs to their state.
     */
    private HashMap<Tab, TabState> tabMap;

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

    /**
     * The history of execution states in our simulation.
     */
    private List<MachineState> stateHistory;

    /**
     * History of registers used by the simulation. This list may contain
     * duplicates as one is added for each register used by an instruction when
     * it is executed.
     */
    private List<String> regHistory;

    /**
     * Current file name.
     */
    private String lastLoadedFileName;

    /**
     * Parser for current tab.
     */
    private X86Parser parser;

    /**
     * Comparator for registers, based on their relative prominence then their
     * lexicographical ordering.
     */
    private final Comparator<Register> regComp = (Register r1, Register r2) -> {
        if (r1.getProminence() > r2.getProminence()) {
            return -1;
        } else if (r1.getProminence() == r2.getProminence()) {
            return r1.getName().compareTo(r2.getName());
        } else {
            return 1;
        }
    };
    
    /**
     * Comparator for stackEntries, based on their start addresses.
     */
    private final Comparator<StackEntry> stackComp = (StackEntry s1, StackEntry s2) -> {
        if (Long.compareUnsigned(s1.getStartAddress(), s2.getStartAddress()) < 0) {
            return 1;
        } else if (Long.compareUnsigned(s1.getStartAddress(), s2.getStartAddress()) == 0) {
            return 0;
        } else {
            return -1;
        }
    };

    // TODO: Comment
    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        // Initialize the simulation state.
        stateHistory = new ArrayList<>();
        stateHistory.add(new MachineState());
        regHistory = new ArrayList<>();
        tabMap = new HashMap<>();
        parser = new X86Parser();
        lastLoadedFileName = null;

        // Initialize stack table
        startAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getStartAddress()).toUpperCase()));

        endAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getEndAddress()).toUpperCase()));

        valCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));

        stackTableList = FXCollections.observableArrayList(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        SortedList<StackEntry> stackSortedList = stackTableList.sorted(stackComp);
        stackTable.setItems(stackSortedList);

        // Initialize the register table
        registerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        registerVal.setCellValueFactory(new PropertyValueFactory<>("value"));
        registerOrigin.setCellValueFactory(new PropertyValueFactory<>("origin"));

        registerTableList = FXCollections.observableArrayList(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        SortedList<Register> regSortedList = registerTableList.sorted(regComp);
        promRegTable.setItems(regSortedList);

        promRegTable.setRowFactory(tableView -> {
            final TableRow<Register> row = new TableRow<>();

            row.hoverProperty().addListener((observable) -> {
                final Register reg = row.getItem();

                if (row.isHover() && reg != null) {
                    reg.setSubName(reg.getName());
                    String s = reg.getName() + ": " + reg.getSubValue(8) + "\n" 
                            + reg.getLongRegName() + ": " + reg.getSubValue(4) 
                            + "\n" + reg.getWordRegName() + ": " + reg.getSubValue(2) 
                            + "\n" + reg.getByteLowRegName() +": " + reg.getSubValue(1);
                    Tooltip t = new Tooltip(s);
                    row.setTooltip(t);
                }
            });

            return row;
        });
        
        listViewTabPane.getTabs().remove(firstTab);
        createTab("New File", regHistory, instrList, parser, null);

        // Set up handlers for simulation control, both via buttons and menu
        // items.
        nextInstr.setOnAction(this::stepForward);
        forwardMenuItem.setOnAction(this::stepForward);

        skipToEnd.setOnAction(this::runForward);
        runMenuItem.setOnAction(this::runForward);

        /*
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
            listViewTabPane.getSelectionModel().select(createTab("New File",
                    new ArrayList<>(),
                    new ListView<>(),
                    new X86Parser(),
                    null));
        });

        // TODO: reorderMenuItem

        /*
         * Event handler for "saveMenuItem" menu.
         * This will save the current simulation to a text file specified 
         * by the user if file does not exist, and save changes to existing file.
         */
        saveMenuItem.setOnAction((event) -> {
            if (lastLoadedFileName != null) {
                try {
                    File file = new File(lastLoadedFileName);
                    FileWriter fW = new FileWriter(file);
                    for (int i = 0; i < instrList.getItems().size(); i++) {
                        fW.write(instrList.getItems().get(i).toString().substring(instrList.getItems().get(i).toString().indexOf(":") + 2) + "\n");
                    }
                    fW.close();
                } catch (IOException e) {
                    System.out.println("File cannot be saved.");
                }
            } else {
                saveFileAs(event);
            }
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
     * Evaluates the current instruction, adding the newly produced state to our
     * history and selecting the next instruction.
     */
    private void evalCurrentInstruction() {
        // evaluate the current instruction, adding its new state to our history
        stateHistory.add(instrList.getSelectionModel().getSelectedItem().eval(stateHistory.get(stateHistory.size() - 1)));

        // select next instruction based on the updated value of the rip register
        instrList.getSelectionModel().select(stateHistory.get(stateHistory.size() - 1).getRipRegister());
        regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
    }

    /**
     * Executes the next instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepForward(Event event) {
        evalCurrentInstruction();
        updateStateDisplays();
    }

    /**
     * Executes instructions until it reaches the end of the program (TODO: or a
     * breakpoint).
     *
     * @param event The event that triggered this action.
     */
    private void runForward(Event event) {
        // TODO: DANGER WILL ROBISON! Do we want to warn the user if they
        // appear to be stuck in an infinite loop?
        for (int x = instrList.getSelectionModel().getSelectedIndex(); x < instrList.getItems().size(); x++) {
            evalCurrentInstruction();
            if (instrList.getSelectionModel().getSelectedItem().getBreakpoint()) {
                break;
            }
        }

        updateStateDisplays();
    }

    /**
     * Undoes the previous instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepBackward(Event event) {
        stateHistory.remove(stateHistory.size() - 1);
        regHistory.removeAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
        instrList.getSelectionModel().select(stateHistory.get(stateHistory.size() - 1).getRipRegister());
        updateStateDisplays();
    }

    /**
     * Restarts simulation back to its starting state.
     *
     * @param event The event that triggered this action.
     */
    private void restartSim(Event event) {
        instrList.getSelectionModel().selectFirst();

        this.stateHistory.clear();
        regHistory.clear();

        stateHistory.add(new MachineState());
        regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
        updateStateDisplays();
    }

    /**
     * Updates all the graphical elements that display state information based
     * on the current state.
     */
    private void updateStateDisplays() {
        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        setStatusFlagLabels();
    }

    private void parseLine(X86Parser perry, String line, ListView<x86ProgramLine> instructions) throws X86ParsingException {
        x86ProgramLine x = perry.parseLine(line);
        instrText.setStyle("-fx-control-inner-background: white;");
        parseErrorText.setText(null);
        parseErrorText.setGraphic(null);

        //Enter text in listView
        instructions.getItems().add(x);

        // If this is the first instruction entered, "select" it and
        // make sure it gets added to our register history list.
        if (instructions.getItems().size() == 1) {
            regHistory.addAll(x.getUsedRegisters());
            instructions.getSelectionModel().select(0);
            registerTableList = FXCollections.observableArrayList(stateHistory.get(stateHistory.size() - 1).getRegisters(regHistory));
            SortedList<Register> regSortedList1 = registerTableList.sorted(regComp);
            promRegTable.setItems(regSortedList1);
        }
        instrText.clear();
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
                this.parseLine(parser, text, instrList);

                // If we reach this point, the parsing was successful so get
                // rid of any error indicators that may have been set up.
                instrText.setStyle("-fx-control-inner-background: white;");
                parseErrorText.setText(null);
                parseErrorText.setGraphic(null);
                instrText.clear();
            } catch (X86ParsingException e) {
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
        }
    }

    /**
     * This will reset the simulation, returning to the very first instruction
     * of the loaded text file.
     *
     * @param event The event that triggered this action.
     */
    private void loadFile(Event event) {
        // Force user to reset? All previously entered instructions are removed currently
        //clearSim();

        FileChooser loadFileChoice = new FileChooser();
        loadFileChoice.setTitle("Open File");

        // Filter only allows user to choose a text file
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        loadFileChoice.getExtensionFilters().add(extFilter);
        File loadFile = loadFileChoice.showOpenDialog(menuOptionsBar.getScene().getWindow());

        if (loadFile != null) {
            lastLoadedFileName = loadFile.getAbsolutePath();

            BufferedReader bufferedReader = null;
            ArrayList<String> instrTmp = new ArrayList<>();
            try {
                bufferedReader = new BufferedReader(new FileReader(loadFile));
                String tmp;
                while ((tmp = bufferedReader.readLine()) != null) {
                    instrTmp.add(tmp.trim());
                }
            } catch (FileNotFoundException e) {
                System.out.println("File does not exist: please choose a valid text file.");
            } catch (IOException e) {
                System.out.println("Invalid file.");
            } finally {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    System.out.println("Invalid file.");
                }
            }

            ListView<x86ProgramLine> newInstrs = new ListView<>();
            X86Parser newPerry = new X86Parser();
            for (String instrLine : instrTmp) {
                try {
                    this.parseLine(newPerry, instrLine, newInstrs);
                } catch (X86ParsingException e) {                 
                    newInstrs.getItems().clear();
                    Alert fileLoadingError = new Alert(AlertType.ERROR);
                    fileLoadingError.setTitle("File Loading Error");
                    fileLoadingError.setHeaderText("Error Loading File");
                    fileLoadingError.setContentText("Unable to parse the following line:"
                            + "\n\n" + instrLine
                            + "\n\nReason: " + e.getMessage());
                    fileLoadingError.showAndWait();
                    return;
                }
            }
            List<String> newRegHistory = new ArrayList<>();
            List<MachineState> newStateHistory = new ArrayList<>();
            newStateHistory.add(new MachineState());

            if (!newInstrs.getItems().isEmpty()) {
                newRegHistory.addAll(newInstrs.getItems().get(0).getUsedRegisters());
            }
            listViewTabPane.getSelectionModel().select(createTab(lastLoadedFileName.substring(lastLoadedFileName.lastIndexOf("/") + 1),
                            newRegHistory, 
                            newInstrs, 
                            newPerry, 
                            lastLoadedFileName));
        }
    }

    /**
     * Saves current instrList as text file.
     *
     * @param event The event that triggered this action.
     */
    private void saveFileAs(Event event) {
        FileChooser saveFileChoice = new FileChooser();

        // Filter only allows user to choose text files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        saveFileChoice.getExtensionFilters().add(extFilter);
        File file = saveFileChoice.showSaveDialog(menuOptionsBar.getScene().getWindow());
        lastLoadedFileName = file.getAbsolutePath();
        listViewTabPane.getSelectionModel().getSelectedItem().setText(lastLoadedFileName.substring(lastLoadedFileName.lastIndexOf("/") + 1));
        if (file != null) {
            try {
                FileWriter fileWriter = new FileWriter(file);
                for (int i = 0; i < instrList.getItems().size(); i++) {
                    fileWriter.write(instrList.getItems().get(i).toString().substring(instrList.getItems().get(i).toString().indexOf(":") + 2) + "\n");
                }
                fileWriter.close();
            } catch (IOException ex) {
                //TODO: ?
                System.out.println("Unable to save to file.");
            }
        }
    }

    // TODO: method comment
    private void clearSim() {
        parser.clear();
        stateHistory.clear();
        instrList.getItems().clear();
        regHistory.clear();
        stateHistory.add(new MachineState());
        updateStateDisplays();
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

    // TODO: method comment
    private void setStatusFlagLabels() {
        MachineState state = stateHistory.get(stateHistory.size() - 1);
        sfLabel.setText("SF: " + (state.getSignFlag() ? "1" : "0"));
        zfLabel.setText("ZF: " + (state.getZeroFlag() ? "1" : "0"));
        ofLabel.setText("OF: " + (state.getOverflowFlag() ? "1" : "0"));
        cfLabel.setText("CF: " + (state.getCarryFlag() ? "1" : "0"));
    }

    /**
     * Creates new tab and adds addNewTab to the end of the current list of tabs
     *
     * @param tabName
     * @return t new tab
     */
    private Tab createTab(String tabName, List<String> tabRegHistory, ListView<x86ProgramLine> tabInstrList, X86Parser tabParser, String tabFileName) {
        Tab t = new Tab(tabName);
        List<MachineState> tabStateHistory = new ArrayList<>();
        tabStateHistory.add(new MachineState());
        tabMap.put(t, new TabState(tabRegHistory, tabStateHistory, tabInstrList, tabParser, tabFileName));
        listViewTabPane.getTabs().add(t);
        tabInstrList.setCellFactory(this::instructionListCellFactory);
        t.setContent(tabInstrList);
        t.setOnSelectionChanged((event) -> {
            instrList = tabMap.get(t).getCurrTabInstrList();
            stateHistory = tabMap.get(t).getCurrTabStateHistory();
            regHistory = tabMap.get(t).getCurrTabRegHistory();
            parser = tabMap.get(t).getCurrTabParser();
            lastLoadedFileName = tabMap.get(t).getCurrFileName();
            updateStateDisplays();
        });
        // TODO: Set only if existing file is different: how to do this on new files?
        t.setOnCloseRequest((event) -> {
            Alert closingConfirmation = new Alert(AlertType.CONFIRMATION);
            closingConfirmation.setTitle("Closing Tab Confirmation");
            closingConfirmation.setHeaderText("Unsaved changes");
            closingConfirmation.setContentText("Selecting OK will close this file immediately. Any unsaved changes will be lost.");
            closingConfirmation.showAndWait();
        });
        return t;
        }
    
    /**
     * Custom cell factory for instruction list entry.
     * This method creates a custom ListCell class then sets up the right-click
     * context menu for this custom cell.
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

        ContextMenu rightClickMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete");
        MenuItem editItem = new MenuItem("Edit");
        MenuItem toggleBreakpointItem = new MenuItem("Toggle breakpoint");

        deleteItem.setOnAction(event -> {
            lv.getItems().remove(cell.getItem());
            int i = 0;
            for (x86ProgramLine line : lv.getItems()) {
                line.setLineNum(i);
                i++;
            }
            parser.setCurrLineNum(i);
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

            // Change instruction entry box to replace instruction rather
            // than adding a new one at the end.
            instrText.setOnKeyPressed((KeyEvent keyEvent) -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    String text = instrText.getText();
                    if(cell.getItem() instanceof x86Label){
                        x86Label l = (x86Label) cell.getItem();
                        parser.removeLabel(l.getName());
                    }
                    try {
                        x86ProgramLine x = parser.parseLine(text);
                        instrText.setStyle("-fx-control-inner-background: white;");
                        parseErrorText.setText(null);
                        parseErrorText.setGraphic(null);
                        entryStatusLabel.setText(null);
                        cell.setStyle("");

                        // Find where the existing instruction was and replace
                        // it with the new instruction.
                        int i = 0;
                        for (x86ProgramLine line : lv.getItems()) {
                            if (line == cell.getItem()) {
                                parser.setCurrLineNum(x.getLineNum());
                                x.setLineNum(i);
                                // TODO: think about whether instrList should be
                                // changed to lv for the next two instructions.
                                instrList.getItems().remove(cell.getItem());
                                instrList.getItems().add(i, x);
                                break;
                            }
                            i++;
                        }

                        instrText.clear();
                        restartSim(null);
                        // Out of editing mode so go back to default behavior
                        // for entering an instruction.
                        instrText.setOnKeyPressed(this::parseAndAddInstruction);
                    } catch (X86ParsingException e) {
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
