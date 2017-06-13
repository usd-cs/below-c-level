/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.*;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.util.HashMap;
import java.util.*;
import java.net.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;

/**
 *
 * @author Caitlin
 */
public class FXMLDocumentController implements Initializable {

    //HashMap<String, Integer> registers;

	private MachineState currState;

    @FXML
    private TextField instrText;
    @FXML
    private ListView<x86Instruction> instrList;
    @FXML
    private MenuButton insertMenu;
    @FXML
    private Button nextInstr;
    @FXML
    private Button skipToEnd;
    @FXML
    private Button prevInstr;
    @FXML
    private Button skipToStart;
    //@FXML private TableView<String> stackPane;
    //@FXML private TableColumn contentCol;
    //@FXML private TableColumn addrCol;
    @FXML private TableView<String> promRegTable;
    @FXML private TableColumn regCol;
    @FXML private TableColumn valCol; 
    ObservableList<String> shtuff;

    @Override
    public void initialize(URL foo, ResourceBundle bar) {

        
		currState = new MachineState();
         
		/*
        registers = new HashMap<String, Integer>();
		registers.put("eax", 1);
		registers.put("ebx", 2);
		registers.put("ecx", 3);
		registers.put("edx", 4);
		registers.put("esi", 5);
		registers.put("edi", 6);
		*/
                
        shtuff = FXCollections.observableArrayList();
         
        //stackPane.setItems(shtuff);
        shtuff.add("good Shtuff");
        shtuff.add("bad shtuff");
        shtuff.add("indifferent shtuff");
                
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
    
    //Abandon all hope ye who enter here
    //TODO: if instruction is valid, do the thingy
 //   stackPane.setOnKeyPressed(new EventHandler<KeyEvent>);
    
    //TODO: if the instruction uses registers, display the registers in regTable
    
    //TODO: if user wants to change where the instruction should be inserted
    MenuItem beginning = insertMenu.getItems().get(0);  
    MenuItem current = insertMenu.getItems().get(1);
    //MenuItem current
    beginning.setText("At beginning");
    current.setText("At current");
    
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
