/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

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
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;

/**
 * Class that controls the main FXML file.
 * 
 * @author Caitlin
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private TextField instrText;
    @FXML
    private ListView<x86ProgramLine> instrList;
    @FXML
    private MenuButton insertMenu;

    @FXML
    private Label parseErrorText;
    @FXML
    private HBox buttonHBox;

    // Buttons
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

    /**
     * List of registers values in our current state.
     */
    private ObservableList<Register> registerTableList;

    @FXML
    private GridPane entireWindow;

    /**
     * The history of execution states in our simulation.
     */
    private List<MachineState> stateHistory;

    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        // Disable user selecting arbitrary item in instruction list.
        instrList.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            /*
            if (event.getButton() == MouseButton.PRIMARY) 
                System.out.println("left clicked"); 
            else if (event.getButton() == MouseButton.SECONDARY) 
                System.out.println("right clicked"); 
             */
            event.consume();
        });

        // Initialize the simulation state.
        stateHistory = new ArrayList<>();
        stateHistory.add(new MachineState());
        ArrayList<String> regHistory = new ArrayList<>();

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

        /*
         * Event handler for the "next" button.
         * This will evaluate the current instruction and move on to the next.
         */
        nextInstr.setOnAction((event) -> {
            this.stateHistory.add(instrList.getSelectionModel().getSelectedItem().eval(this.stateHistory.get(this.stateHistory.size() - 1)));

            instrList.getSelectionModel().select(this.stateHistory.get(this.stateHistory.size() - 1).getRipRegister().intValue());
            regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

            registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
            stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());

        });

        /*
         * Event handler for "run to completion" button.
         */
        skipToEnd.setOnAction((event) -> {
            // TODO: DANGER WILL ROBISON! Do we want to warn the user if they
            // appear to be stuck in an infinite loop?
            for (int x = instrList.getSelectionModel().getSelectedIndex(); x < instrList.getItems().size(); x++) {
                this.stateHistory.add(instrList.getSelectionModel().getSelectedItem().eval(this.stateHistory.get(this.stateHistory.size() - 1)));
                instrList.getSelectionModel().select(this.stateHistory.get(this.stateHistory.size() - 1).getRipRegister().intValue());
                regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
            }

            registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
            stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        });

        /*
         * Event handler for "scroll back to current instruction" button.
         */
        currInstr.setOnAction(event -> {
            ObservableList<Integer> selectedIndices = instrList.getSelectionModel().getSelectedIndices();
            if (!selectedIndices.isEmpty()) {
                instrList.scrollTo(selectedIndices.get(0));
            }
        });

        /*
         * Event handler for "back" button.
         */
        prevInstr.setOnAction((event) -> {

            this.stateHistory.remove((this.stateHistory.size() - 1));
            regHistory.removeAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

            instrList.getSelectionModel().selectPrevious();

            registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
            stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        });

        /*
         * Event handler for "return to beginning" button.
         * This will reset the simulation, returning to the very first
         * instruction.
         */
        skipToStart.setOnAction((event) -> {
            instrList.getSelectionModel().selectFirst();

            this.stateHistory.clear();
            regHistory.clear();

            stateHistory.add(new MachineState());
            regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());
            registerTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getRegisters(regHistory));
            stackTableList.setAll(stateHistory.get(this.stateHistory.size() - 1).getStackEntries());
        });

        /*
         * Event handler for when user clicks button to insert a new
         * instruction.
         */
        instrText.setOnKeyPressed((KeyEvent keyEvent) -> {
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
                    Polygon error = new Polygon(4.0, 0.0, 0.0, 8.0, 8.0, 8.0);
                    error.setFill(Color.RED);
                    parseErrorText.setGraphic(error);
                }
            }
        });

        //TODO: if user wants to change where the instruction should be inserted
        MenuItem beginning = insertMenu.getItems().get(0);
        beginning.setText("At beginning");
        MenuItem current = insertMenu.getItems().get(1);
        current.setText("At current");

        // Initialize buttons with fancy graphics.
        Image skipStartImg = new Image(getClass().getResourceAsStream("skipToStart.png"));
        Image prevInstrImg = new Image(getClass().getResourceAsStream("prevInstr.png"));
        Image currInstrImg = new Image(getClass().getResourceAsStream("currInstr.png"));
        Image nextInstrImg = new Image(getClass().getResourceAsStream("nextInstr.png"));
        Image skipEndImg = new Image(getClass().getResourceAsStream("skipToEnd.png"));

        ImageView skipToStartImgVw = new ImageView(skipStartImg);
        ImageView prevInstrImgVw = new ImageView(prevInstrImg);
        ImageView currInstrImgVw = new ImageView(currInstrImg);
        ImageView nextInstrImgVw = new ImageView(nextInstrImg);
        ImageView skipToEndImgVw = new ImageView(skipEndImg);

        skipToStartImgVw.setFitHeight(35);
        skipToStartImgVw.setFitWidth(35);
        prevInstrImgVw.setFitHeight(35);
        prevInstrImgVw.setFitWidth(35);
        currInstrImgVw.setFitHeight(35);
        currInstrImgVw.setFitWidth(35);
        nextInstrImgVw.setFitHeight(35);
        nextInstrImgVw.setFitWidth(35);
        skipToEndImgVw.setFitHeight(35);
        skipToEndImgVw.setFitWidth(35);

        skipToStart.setGraphic(skipToStartImgVw);
        prevInstr.setGraphic(prevInstrImgVw);
        currInstr.setGraphic(currInstrImgVw);
        nextInstr.setGraphic(nextInstrImgVw);
        skipToEnd.setGraphic(skipToEndImgVw);

        //TODO: Resizing icons/nodes to pane
        /*
           skipToStartImgVw.fitHeightProperty().bind(skipToStart.heightProperty());
           skipToStartImgVw.fitWidthProperty().bind(skipToStart.widthProperty());
           prevInstrImgVw.fitHeightProperty().bind(prevInstr.heightProperty());
           prevInstrImgVw.fitWidthProperty().bind(prevInstr.widthProperty());
           currInstrImgVw.fitHeightProperty().bind(currInstr.heightProperty());
           currInstrImgVw.fitWidthProperty().bind(currInstr.widthProperty());
           nextInstrImgVw.fitHeightProperty().bind(nextInstr.heightProperty());
           nextInstrImgVw.fitWidthProperty().bind(nextInstr.widthProperty());
           skipToEndImgVw.fitHeightProperty().bind(skipToEnd.heightProperty());
           skipToEndImgVw.fitWidthProperty().bind(skipToEnd.widthProperty());

           skipToStart.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           skipToStart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           prevInstr.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           prevInstr.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           currInstr.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           currInstr.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           nextInstr.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           nextInstr.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
           skipToEnd.setMinSize(buttonHBox.getPrefWidth(), buttonHBox.getPrefHeight());
           skipToEnd.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
         */

 /*
         * Event handler for when user picks "insert at beginning" option.
         */
        beginning.setOnAction((event) -> {
            System.out.println("Insert at Beginning selected");
        });

        /*
         * Event handler for when user picks "insert after current" option.
         */
        current.setOnAction((event) -> {
            System.out.println("Insert at Current selected");
        });

        Platform.runLater(() -> {
            // instrList.scrollTo(N);
            // instrList.getSelectionModel().select(N);
        });

    }
}
