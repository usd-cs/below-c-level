/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.util.HashMap;
import java.util.*;
import java.net.*;
import javafx.event.EventHandler;

/**
 *
 * @author Caitlin
 */

class x86Instruction {
    operand destination;
    String instructionType;
    // Perform the operation specific to the instruction type

    /**
     *
     * @return
     */
    public int eval() { return 0; }
    // Create a new x86Instruction if input is valid otherwise return null
    public static x86Instruction create(String userInput) { return null; }
}

class x86UnaryInstruction extends x86Instruction{
    public int eval() { return 1; }
}

class x86BinaryInstruction extends x86Instruction{
    operand source;
    public int eval() { return 3; }
}
        
class operand{
    HashMap <String, Integer> registers;
     public int getValue() { return 99; }
} 
        
class regOperand{
    String regName;
    public int getValue() { return 162; }
    
}
     
class memoryOperand{
    String memName;
     public int getValue() { return 5298; }
}
        
class constantOperand{
      int constant;
     public int getValue() { return 0; }
}


public class FXMLDocumentController implements Initializable {
  
    @FXML private TextField instrField;
    @FXML private ListView instrList;
    @FXML private MenuButton insertMenu;
    @FXML private Button nextInstr;
    //private ListView<Instructions> instrList;
    
    @Override
    public void initialize(URL foo, ResourceBundle bar) {
        
        nextInstr.setText("foo");
       // nextInstr.setOnAction(new EventHandler<ActionEvent>() {
            
            /*
             private void handle(ActionEvent event) {
                System.out.println("That was easy, wasn't it?");
            } */
        //}); 
        
        
         nextInstr.setOnAction((event) -> {
                System.out.println("That was easy, wasn't it?");
         });
        

    }
    
    //If lambda expressions are allowed
    /* 
    instrField.setOnKeyPressed(event-> {
        if(event.getCode() == KeyCode.ENTER) {
        etc.
    }
    });
    */
    
    //TODO @Caitlin: Make it not be this way
    /*
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
*/
   
}

