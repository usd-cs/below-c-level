/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

/**
 *
 * @author Caitlin
 */

public class FXMLDocumentController implements Initializable {
    
    @FXML
    private TextField instrField;
    private ListView instrList;
    //private ListView<Instructions> instrList;
    
    //If lambda expressions are allowed
    /* 
    instrField.setOnKeyPressed(event-> {
        if(event.getCode() == KeyCode.ENTER) {
        etc.
    }
    });
    */
    
    //TODO @Caitlin: Make it not be this way
    instrField.setOnKeyPressed(new EventHandler<KeyEvent>() {
           @Override
           public void handleInstrEntered(KeyEvent keyEvent){
                if (keyEvent.getCode() == KeyCode.ENTER){
                       String text = instrField.getText();
                       
                       //TODO: Enter text in listView
                       
                       //clear text from instruction pane
                       // Will this clear the textbox and leave the instructions?
                       instrField.setText("");
                }
    }
    });
    
}
