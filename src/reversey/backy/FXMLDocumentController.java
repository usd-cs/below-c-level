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
import java.util.*;
import java.net.*;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

/**
 *
 * @author Caitlin
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private TextField instrText;
    @FXML
    private ListView<x86Instruction> instrList;
    @FXML
    private MenuButton insertMenu;
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

    ObservableList<StackEntry> stackTableList;

    // Fields for the register table
    @FXML
    private TableView<Register> promRegTable;
    @FXML
    private TableColumn<Register, String> registerName;
    @FXML
    private TableColumn<Register, String> registerVal;

    ObservableList<Register> registerTableList;

    @FXML
    private GridPane entireWindow;

    private MachineState currState;

    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        // Disable user selecting arbitrary item in instruction list.
        instrList.setMouseTransparent(true);
        instrList.setFocusTraversable(false);

        // Initialize the simulation state.
        currState = new MachineState();
        ArrayList<String> regHistory = new ArrayList<String>();

        // Initialize stack table
        startAddressCol.setCellValueFactory(new Callback<CellDataFeatures<StackEntry, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(CellDataFeatures<StackEntry, String> p) {
                return new SimpleStringProperty(Long.toHexString(p.getValue().getStartAddress()));
            }
        });

        endAddressCol.setCellValueFactory(new Callback<CellDataFeatures<StackEntry, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(CellDataFeatures<StackEntry, String> p) {
                return new SimpleStringProperty(Long.toHexString(p.getValue().getEndAddress()));
            }
        });

        valCol.setCellValueFactory(new PropertyValueFactory<StackEntry, String>("value"));
        originCol.setCellValueFactory(new PropertyValueFactory<StackEntry, Integer>("origin"));

		startAddressCol.setStyle( "-fx-alignment: CENTER;");
		endAddressCol.setStyle( "-fx-alignment: CENTER;");
		valCol.setStyle( "-fx-alignment: CENTER;");
		originCol.setStyle( "-fx-alignment: CENTER;");
        
        stackTableList = FXCollections.observableArrayList(currState.getStackEntries());
        stackTable.setItems(stackTableList);

        // Initialize the register table
        registerName.setCellValueFactory(new PropertyValueFactory<Register, String>("name"));
        registerVal.setCellValueFactory(new PropertyValueFactory<Register, String>("value"));

		registerName.setStyle( "-fx-alignment: CENTER;");
		registerVal.setStyle( "-fx-alignment: CENTER;");

        Comparator<Register> regComp = (Register r1, Register r2) -> {
            if (r1.getProminence() > r2.getProminence()) {
                return -1;
            } else if (r1.getProminence() == r2.getProminence()) {
                return 0;
            } else {
                return 1;
            }
        };

        registerTableList = FXCollections.observableArrayList(currState.getRegisters(regHistory));
        SortedList<Register> regSortedList = registerTableList.sorted(regComp);
        promRegTable.setItems(regSortedList);

        /*
		 * Event handler for the "next" button.
		 * This will evaluate the current instruction and move on to the next.
         */
        nextInstr.setOnAction((event) -> {
            System.out.println(instrList.getSelectionModel().getSelectedItem());
            this.currState = instrList.getSelectionModel().getSelectedItem().eval(this.currState);
            System.out.println(currState);

            instrList.getSelectionModel().selectNext();
            regHistory.addAll(instrList.getSelectionModel().getSelectedItem().getUsedRegisters());

            registerTableList.setAll(currState.getRegisters(regHistory));
            stackTableList.setAll(currState.getStackEntries());

        });

        /*
		 * TODO: Event handler for "run to completion" button.
         */
        skipToEnd.setOnAction((event) -> {
            System.out.println(instrList.getSelectionModel().getSelectedItem());
            instrList.getSelectionModel().selectLast();
        });

        /*
		 * TODO: Event handler for "back" button.
         */
        prevInstr.setOnAction((event) -> {
            System.out.println(instrList.getSelectionModel().getSelectedItem());
            instrList.getSelectionModel().selectPrevious();
        });

        /*
		 * TODO: Event handler for "return to beginning" button.
		 * This will reset the simulation, returning to the very first
		 * instruction.
         */
        skipToStart.setOnAction((event) -> {
            System.out.println(instrList.getSelectionModel().getSelectedItem());
            instrList.getSelectionModel().selectFirst();
        });

        /*
		 * Event handler for when user clicks button to insert a new
		 * instruction.
         */
        instrText.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    String text = instrText.getText();

                    try {
                        x86Instruction x = x86Instruction.parseInstruction(text);

                        instrText.setStyle("-fx-control-inner-background: white;");
                        instrText.setTooltip(null);

                        //Enter text in listView
                        instrList.getItems().add(x);

                        // If this is the first instruction entered, "select" it and
                        // make sure it gets added to our register history list.
                        if (instrList.getItems().size() == 1) {
                            regHistory.addAll(x.getUsedRegisters());
                            instrList.getSelectionModel().select(0);

                            registerTableList = FXCollections.observableArrayList(currState.getRegisters(regHistory));
                            SortedList<Register> regSortedList = registerTableList.sorted(regComp);
                            promRegTable.setItems(regSortedList);
                        }
                        instrText.clear();
                    } catch (X86ParsingException e) {
                        // If we had a parsing error, set the background to pink
                        // and select the part of the input that reported the
                        // error.
                        instrText.setStyle("-fx-control-inner-background: pink;");
                        instrText.selectRange(e.getStartIndex(), e.getEndIndex());
                        instrText.setTooltip(new Tooltip(e.getMessage()));
                    }
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

        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                // instrList.scrollTo(N);
                // instrList.getSelectionModel().select(N);
            }
        });

    }
}
