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
import javafx.application.Platform;
import javafx.event.EventHandler;

/**
 *
 * @author Caitlin
 */

// TODO: We should move the following classes to different files
// TODO: Add some Javadoc comments to our classes and methods!

class x86Instruction {
    protected operand destination;
    protected String instructionType;
    // Perform the operation specific to the instruction type

    /**
     *
     * @return
     */
    public int eval() { return 0; }

    // Create a new x86Instruction if input is valid otherwise return null
    public static x86Instruction create(String userInput) { 
		String[] tokens = userInput.split("\\s+");
		String instrName = tokens[0]; // should be instruction name, e.g. "addl"
		System.out.println("instruction name: " + instrName);

		// TODO: a regular expression is probably a better approach here
		switch (instrName) {
			case "movb":
			case "movw":
			case "movl":
			case "movq":
			case "addb":
			case "addw":
			case "addl":
			case "addq":
			// TODO: cases for more types of binary instructions
				// found binary instruction
				// step 1: make sure it has exactly two operands (TODO)
				// step 2: make sure operands are valid (TODO)
				// step 3: get operands to pass into constructor (TODO)
				System.out.println("yay got a valid binary inst!");
				return new x86BinaryInstruction(instrName, null, null);
			case "pushq":
			case "popq":
			case "incb":
			case "incw":
			case "incl":
			case "incq":
			// TODO: cases for more types of unary instructions
				// step 1: make sure it has exactly one operand (TODO)
				// step 2: make sure operand is valid (TODO)
				// step 3: get operand to pass into constructor (TODO)
				System.out.println("yay got a valid unary inst!");
				return new x86UnaryInstruction(instrName, null);
			default:
				return null;
		}
	}
}

class x86UnaryInstruction extends x86Instruction{
	public x86UnaryInstruction(String instType, operand op) {
		this.instructionType = instType;
		this.destination = op;
	}

    public int eval() { return 1; }
}

class x86BinaryInstruction extends x86Instruction{
    private operand source;

	public x86BinaryInstruction(String instType, operand src, operand dest) {
		this.instructionType = instType;
		this.source = src;
		this.destination = dest;
	}

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
    
    //“Give a man a program, frustrate him for a day. 
    //Teach a man to program, frustrate him for a lifetime.” 
    instrField.setOnKeyPressed(new EventHandler<KeyEvent>() {
           @Override
           public void handleInstrEntered(KeyEvent keyEvent){
                if (keyEvent.getCode() == KeyCode.ENTER){
                       String text = instrField.getText();
                       
                       //Enter text in listView
                       System.out.println("You made it this far!");
                       instrList.getItems().addAll(text);
                       //Necessary to add list to scene?
                       //scene1 = new Scene(VBox(instrList, layout1), 400, 300);
                       //clear text from instruction pane
                       instrField.clear();
                       
                }
    }
    });
    
    //Highlighting selected instruction is newly added item has an index of N
    instrList.getSelectionModel().select(N);
    instrList.getFocusModel().focus(N);
    //Highlighting scrolled then selected
    instrList.scrollTo(N);
    
    Platform.runLater(new Runnable() {
        
        @Override
        public void run(){
            instrList.scrollTo(N);
            instrList.getSelectionModel().select(N);
    }
    });

}

