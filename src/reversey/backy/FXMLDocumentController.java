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
import javafx.event.EventHandler;

/**
 *
 * @author Caitlin
 */

public class FXMLDocumentController implements Initializable {
  
    @FXML private TextField instrText;
    @FXML private ListView<String> instrList;
    @FXML private MenuButton insertMenu;
    @FXML private Button nextInstr;
    @FXML private Button skipToEnd;
    @FXML private Button prevInstr;
    @FXML private Button skipToStart;
     
    @Override
    public void initialize(URL foo, ResourceBundle bar) {
            
         nextInstr.setOnAction((event) -> {
             //int selectedIndex = instrList.getSelectionModel().getSelectedIndex();
             
         
             System.out.println(instrList.getSelectionModel().getSelectedItem());
                 instrList.getSelectionModel().selectNext();
         });
        
    //“Give a man a program, frustrate him for a day. 
    //Teach a man to program, frustrate him for a lifetime.” 
    instrText.setOnKeyPressed(new EventHandler<KeyEvent>() {
           @Override
           public void handle(KeyEvent keyEvent){
                if (keyEvent.getCode() == KeyCode.ENTER){
                       String text = instrText.getText();
                       
                       //Enter text in listView
                       instrList.getItems().addAll(text);
                       //clear text from instruction pane
                       instrText.clear();
                } 
    }
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

