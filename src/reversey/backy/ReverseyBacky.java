/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.HashMap;

/**
 *
 * @author Caitlin
 */
public class ReverseyBacky extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Below C-Level Stack Simulator");
        stage.show();

		/*

		// test an instruction creation and invalidation
		x86Instruction inst = x86Instruction.create("addl %eax, %ecx", registers);
		if (inst != null) {
			inst.eval();
			//System.out.println("%ebx new value: " + registers.get("ebx"));
			System.out.println("ecx should be 4");
			System.out.println(registers);
		}

		x86Instruction inst2 = x86Instruction.create("subl %edi, %esi", registers);
		if (inst2 != null) {
			inst2.eval();
			System.out.println("esi should be -1");
			System.out.println(registers);
		}

		x86Instruction inst3 = x86Instruction.create("decl %esi", registers);
		if (inst3 != null) {
			inst3.eval();
			System.out.println("esi should be -2");
			System.out.println(registers);
		}
        */
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
