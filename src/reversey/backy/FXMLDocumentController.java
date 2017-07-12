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
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.scene.text.Font;
import javafx.util.Callback;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Class that controls the main FXML file.
 *
 * @author Caitlin
 */
public class FXMLDocumentController implements Initializable {

    // Fields for the menu bar
    @FXML
    private MenuItem exitMenuItem;
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
    private MenuItem clearProgramMenuItem;
    @FXML
    private MenuItem aboutMenuItem;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem reorderMenuItem;

    @FXML
    private MenuBar menuOptionsBar;
    @FXML
    private Menu fileOption;
    @FXML
    private Menu helpOption;
    @FXML
    private Menu editOption;

    @FXML
    private BorderPane entirePane;

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

    private final Comparator<Register> regComp = (Register r1, Register r2) -> {
        if (r1.getProminence() > r2.getProminence()) {
            return -1;
        } else if (r1.getProminence() == r2.getProminence()) {
            return r1.getName().compareTo(r2.getName());
        } else {
            return 1;
        }
    };

    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        // Disable user selecting arbitrary item in instruction list.
        instrList.setCellFactory(lv -> {
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
                        //if (lv.getSelectionModel().getSelectedItems().contains(item))
                        //    setGraphic(new Circle(5.0f));
                    }
                }
            };

            ContextMenu cM = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");

            deleteItem.setOnAction(event -> {
                lv.getItems().remove(cell.getItem());
                int i = 0;
                for (x86ProgramLine line : lv.getItems()) {
                    line.setLineNum(i);
                    i++;
                }
                X86Parser.setCurrLineNum(i);
                this.restartSim(null);

            });
            cM.getItems().addAll(deleteItem);

            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(event -> {
                instrText.setStyle("-fx-control-inner-background: #77c0f4;");
                instrText.setText(cell.getItem().toString().substring(cell.getItem().toString().indexOf(":") + 1).trim());
                entryStatusLabel.setText("Editing line " + cell.getItem().getLineNum());
                cell.setStyle("-fx-background-color: #77c0f4;");
                // Change instruction entry box to replace instruction rather
                // than adding a new one at the end.
                instrText.setOnKeyPressed((KeyEvent keyEvent) -> {
                    if (keyEvent.getCode() == KeyCode.ENTER) {
                        String text = instrText.getText();
                        try {
                            x86ProgramLine x = X86Parser.parseLine(text);
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
                                    X86Parser.setCurrLineNum(x.getLineNum());
                                    x.setLineNum(i);
                                    instrList.getItems().remove(cell.getItem());
                                    instrList.getItems().add(i, x);
                                    break;
                                }
                                i++;
                            }

                            instrText.clear();
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
            cM.getItems().addAll(editItem);

            MenuItem toggleBreakpointItem = new MenuItem("Toggle breakpoint");

            toggleBreakpointItem.setOnAction(event -> {
                cell.getItem().toggleBreakpoint();
                if (cell.getItem().getBreakpoint() == true) {
                    cell.setGraphic(new Circle(4));
                } else {
                    Circle c = new Circle(4);
                    c.setFill(Color.TRANSPARENT);
                    cell.setGraphic(c);
                }
            });
            cM.getItems().addAll(toggleBreakpointItem);

            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.SECONDARY && !cell.isEmpty()) {
                    //lv.getFocusModel().focus(lv.getItems().indexOf(cell.getItem()));
                    lv.getFocusModel().focus(1);
                    //x86ProgramLine item = cell.getItem();
                    //System.out.println("Right clicked: " + item);
                    cell.setContextMenu(cM);
                }
                event.consume();
            });
            return cell;
        });

        // Initialize the simulation state.
        stateHistory = new ArrayList<>();
        stateHistory.add(new MachineState());
        regHistory = new ArrayList<>();

        // Initialize stack table
        startAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getStartAddress())));

        endAddressCol.setCellValueFactory((CellDataFeatures<StackEntry, String> p)
                -> new SimpleStringProperty(Long.toHexString(p.getValue().getEndAddress())));

        valCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));

        startAddressCol.setStyle("-fx-alignment: CENTER;");
        endAddressCol.setStyle("-fx-alignment: CENTER;");
        valCol.setStyle("-fx-alignment: CENTER;");
        originCol.setStyle("-fx-alignment: CENTER;");

        stackTableList = FXCollections.observableArrayList(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        stackTable.setItems(stackTableList);

        // Initialize the register table
        registerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        registerVal.setCellValueFactory(new PropertyValueFactory<>("value"));
        registerOrigin.setCellValueFactory(new PropertyValueFactory<>("origin"));

        registerName.setStyle("-fx-alignment: CENTER;");
        registerVal.setStyle("-fx-alignment: CENTER;");
        registerOrigin.setStyle("-fx-alignment: CENTER;");

        Comparator<Register> regComp = (Register r1, Register r2) -> {
            if (r1.getProminence() > r2.getProminence()) {
                return -1;
            } else if (r1.getProminence() == r2.getProminence()) {
                return r1.getName().compareTo(r2.getName());
            } else {
                return 1;
            }
        };

        registerTableList = FXCollections.observableArrayList(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        SortedList<Register> regSortedList = registerTableList.sorted(regComp);
        promRegTable.setItems(regSortedList);

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

        clearProgramMenuItem.setOnAction(this::clearProgram);

        /**
         * Event handler for when user clicks button to insert a new
         * instruction.
         */
        instrText.setOnKeyPressed(this::parseAndAddInstruction);

        // Set up actions for the menubar
        exitMenuItem.setOnAction((event) -> System.exit(0));
        loadMenuItem.setOnAction(this::loadFile);
        saveAsMenuItem.setOnAction(this::saveFileAs);
        undoMenuItem.setOnAction(this::undoPrevious);
        redoMenuItem.setOnAction(this::redoPrevious);
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
     * Executes the next instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepForward(Event event) {
        this.stateHistory.add(instrList.getSelectionModel().getSelectedItem().eval(this.stateHistory.get(this.stateHistory.size() - 1)));

        instrList.getSelectionModel().select(this.stateHistory.get(this.stateHistory.size() - 1).getRipRegister().intValue());
        regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        setStatusFlagLabels();
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
            this.stateHistory.add(instrList.getSelectionModel().getSelectedItem().eval(this.stateHistory.get(this.stateHistory.size() - 1)));
            instrList.getSelectionModel().select(this.stateHistory.get(this.stateHistory.size() - 1).getRipRegister().intValue());
            regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
            if (instrList.getSelectionModel().getSelectedItem().getBreakpoint()) {
                break;
            }
        }

        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        setStatusFlagLabels();
    }

    /**
     * Undoes the previous instruction in our simulation.
     *
     * @param event The event that triggered this action.
     */
    private void stepBackward(Event event) {
        this.stateHistory.remove((this.stateHistory.size() - 1));
        regHistory.removeAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

        instrList.getSelectionModel().selectPrevious();

        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        setStatusFlagLabels();
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
        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        setStatusFlagLabels();
    }

    /**
     * Gets input from instruction entry text field, parses it, and (if successful)
     * adds it to the end of the instruction list.
     * 
     * @param keyEvent The event that caused the handler to engage.
     */
    private void parseAndAddInstruction(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            String text = instrText.getText();
            try {
                x86ProgramLine x = X86Parser.parseLine(text);
                instrText.setStyle("-fx-control-inner-background: white;");
                parseErrorText.setText(null);
                parseErrorText.setGraphic(null);

                //Enter text in listView
                instrList.getItems().add(x);

                // If this is the first instruction entered, "select" it and
                // make sure it gets added to our register history list.
                if (instrList.getItems().size() == 1) {
                    regHistory.addAll(x.getUsedRegisters());
                    instrList.getSelectionModel().select(0);
                    registerTableList = FXCollections.observableArrayList(stateHistory.get(stateHistory.size() - 1).getRegisters(regHistory));
                    SortedList<Register> regSortedList1 = registerTableList.sorted(regComp);
                    promRegTable.setItems(regSortedList1);
                }
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
        clearSim();

        FileChooser loadFileChoice = new FileChooser();
        loadFileChoice.setTitle("Open File");

        // Filter only allows user to choose a text file
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        loadFileChoice.getExtensionFilters().add(extFilter);
        File loadFile = loadFileChoice.showOpenDialog(menuOptionsBar.getScene().getWindow());
        if (loadFile != null) {
            lastLoadedFileName = loadFile.getAbsolutePath();
            Stage s = (Stage)instrText.getScene().getWindow();
            s.setTitle(lastLoadedFileName.substring(lastLoadedFileName.lastIndexOf("/") + 1) + " - Below C-Level Stack Simulator");
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
            try {
                for (String e : instrTmp) {
                    x86ProgramLine x = X86Parser.parseLine(e);
                    instrList.getItems().add(x);
                }
                //Enter text in listView and select first instruction
                if (instrList.getItems().size() >= 1) {
                    instrList.getSelectionModel().select(0);
                    regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
                    registerTableList = FXCollections.observableArrayList(stateHistory.get(stateHistory.size() - 1).getRegisters(regHistory));
                    SortedList<Register> regSortedList1 = registerTableList.sorted(regComp);
                    promRegTable.setItems(regSortedList1);
                }
            } catch (X86ParsingException e) {
                // TODO: If we had a parsing error, report what? File "line"? In which case numbers must remain
                Alert fileLoadingError = new Alert(AlertType.ERROR);
                fileLoadingError.setTitle("File Loading Error");
                fileLoadingError.setHeaderText("Error Loading File");
                fileLoadingError.setContentText("Unable to load file. Verify that the file is of the correct type (i.e. .txt) and is not invalid.");
                fileLoadingError.showAndWait();
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

        // Filter only allows user to choose text files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        saveFileChoice.getExtensionFilters().add(extFilter);
        File file = saveFileChoice.showSaveDialog(menuOptionsBar.getScene().getWindow());
        lastLoadedFileName = file.getAbsolutePath();
        Stage s = (Stage) instrText.getScene().getWindow();
        s.setTitle(lastLoadedFileName.substring(lastLoadedFileName.lastIndexOf("/") + 1) + " - Below C-Level Stack Simulator");
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

    private void undoPrevious(Event event) {
        this.stateHistory.remove((this.stateHistory.size() - 1));
        regHistory.removeAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

        instrList.getSelectionModel().selectPrevious();

        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
    }

    private void redoPrevious(Event event) {

    }

    /**
     * Clears current simulation with pop-up window.
     *
     * @param event The event that triggered this action.
     */
    private void clearProgram(ActionEvent event) {
        Alert clearProgramPopUp = new Alert(AlertType.CONFIRMATION);
        clearProgramPopUp.setTitle("Clear Program Confirmation");
        clearProgramPopUp.setHeaderText("Clearing program will remove all instructions");
        clearProgramPopUp.setContentText("Are you okay with this?");

        Optional<ButtonType> result = clearProgramPopUp.showAndWait();
        if (result.get() == ButtonType.OK) {
            clearSim();
        } else {
            // user chose CANCEL or closed the dialog
        }
    }

    private void clearSim() {
        X86Parser.clear();
        stateHistory.clear();
        instrList.getItems().clear();
        regHistory.clear();
        stateHistory.add(new MachineState());
        registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
        stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        setStatusFlagLabels();
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
    
    private void setStatusFlagLabels(){
        MachineState state = stateHistory.get(stateHistory.size() - 1);
        sfLabel.setText("SF: " + (state.getSignFlag() ? "1" : "0"));
        zfLabel.setText("ZF: " + (state.getZeroFlag() ? "1" : "0"));
        ofLabel.setText("OF: " + (state.getOverflowFlag() ? "1" : "0"));
        cfLabel.setText("CF: " + (state.getCarryFlag() ? "1" : "0"));
    }
}

