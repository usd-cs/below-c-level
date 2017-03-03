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

public class x86Instruction {
    operand destination;
    String instructionType;
    // Perform the operation specific to the instruction type
    public int eval();
    // Create a new x86Instruction if input is valid otherwise return null
    public static x86Instruction create(String userInput);
}

public class x86UnaryInstruction extends x86Instruction{
    public int eval();
}
public class x86BinaryInstruction extends x86Instruction{
    operand source;
    public int eval();
}
        
public class operand{
    HashMap <String, Integer> registers;
     public int getValue();
} 
        
public class regOperand{
    String regName;
    public int getValue();
    
}
     
public class memoryOperand{
    String memName;
     public int getValue();
}
        
public class constantOperand{
      int constant;
     public int getValue();
}


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
