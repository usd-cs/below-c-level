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
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;

/**
 *
 * @author Caitlin
 */
public class FXMLDocumentController implements Initializable {

	private MachineState currState;

    @FXML private TextField instrText;
    @FXML private ListView<x86Instruction> instrList;
    @FXML private MenuButton insertMenu;
    @FXML private Button nextInstr;
    @FXML private Button skipToEnd;
    @FXML private Button prevInstr;
    @FXML private Button skipToStart;
    @FXML private Button currInstr;
    @FXML private TableView stackTable;
    @FXML private TableView promRegTable;
    @FXML private TableColumn stackAddress;
    @FXML private TableColumn stackVal;
    @FXML private TableColumn stackOrigin;
    @FXML private TableColumn registerName;
    @FXML private TableColumn registerVal;
    ObservableList<String> registerTableList;
    
    @Override
    public void initialize(URL foo, ResourceBundle bar) {

        currState = new MachineState();
        
        List list = new ArrayList();

        list.add(new Register("%rax", "1"));
        list.add(new Register("%rbx", "2"));
        list.add(new Register("%rcx", "3"));
        list.add(new Register("%rdx", "4"));

        registerTableList = FXCollections.observableArrayList(list);
        promRegTable.setItems(registerTableList);
        
        registerName.setCellValueFactory(new PropertyValueFactory("name"));
        registerVal.setCellValueFactory(new PropertyValueFactory("value"));

         nextInstr.setOnAction((event) -> {
             System.out.println(instrList.getSelectionModel().getSelectedItem());
             this.currState = instrList.getSelectionModel().getSelectedItem().eval(this.currState);
             System.out.println(currState);
             instrList.getSelectionModel().selectNext();
         });
        
       //TODO: Keep history list of previous registers HashMap
         skipToEnd.setOnAction((event) -> {
             System.out.println(instrList.getSelectionModel().getSelectedItem());
             instrList.getSelectionModel().selectLast();
         });
         
         prevInstr.setOnAction((event) -> {
             System.out.println(instrList.getSelectionModel().getSelectedItem());
             instrList.getSelectionModel().selectPrevious();
         });
         
         skipToStart.setOnAction((event) -> {
             System.out.println(instrList.getSelectionModel().getSelectedItem());
             instrList.getSelectionModel().selectFirst();
         });
         
    //“Give a man a program, frustrate him for a day. 
    //Teach a man to program, frustrate him for a lifetime.” 
    instrText.setOnKeyPressed(new EventHandler<KeyEvent>() {
           @Override
           public void handle(KeyEvent keyEvent){
                if (keyEvent.getCode() == KeyCode.ENTER){
                       String text = instrText.getText();
                       
                       x86Instruction x = x86Instruction.parseInstruction(text);
                       
                       //Enter text in listView
                       instrList.getItems().addAll(x);
                       //Clear text from instruction pane
                       instrText.clear();
                } 
    }
    });
            
    //TODO: if instruction is valid, do the thingy
    //stackPane.setOnKeyPressed(new EventHandler<KeyEvent>);
    
    //TODO: if the instruction uses registers, display the registers in regTable
    
    //TODO: if user wants to change where the instruction should be inserted
    MenuItem beginning = insertMenu.getItems().get(0);  
    MenuItem current = insertMenu.getItems().get(1);
    //MenuItem current
    beginning.setText("At beginning");
    current.setText("At current");
    
    Image skipStartImg = new Image(getClass().getResourceAsStream("skipToStart.png"));
    Image prevInstrImg = new Image(getClass().getResourceAsStream("prevInstr.png"));
    Image currInstrImg = new Image(getClass().getResourceAsStream("currInstr.jpg"));
    Image nextInstrImg = new Image(getClass().getResourceAsStream("nextInstr.png"));
    Image skipEndImg = new Image(getClass().getResourceAsStream("Sharonisgenius.png"));
   
    //Fix size 
    /*
    skipToStart.setGraphic(new ImageView(skipStartImg));
    prevInstr.setGraphic(new ImageView(prevInstrImg));
    //currInstr.setGraphic(new ImageView(currInstrImg));
    nextInstr.setGraphic(new ImageView(nextInstrImg));
    skipToEnd.setGraphic(new ImageView(skipEndImg));
    */
    
    beginning.setOnAction((event) -> {
        System.out.println("Option 1 selected");
    });
    
    current.setOnAction((event) -> {
           System.out.println("Option 2 selected");
    });

    
    //Highlighting selected instruction is newly added item has an index of N
    //instrList.getSelectionModel().getSelectedItem();
    //instrList.getFocusModel().focus(N);
    //Highlighting scrolled then selected
    //instrList.scrollTo(N);
    
	Platform.runLater(new Runnable() {

		@Override
		public void run(){
			// instrList.scrollTo(N);
			// instrList.getSelectionModel().select(N);
		}
	});
         
    }
}
