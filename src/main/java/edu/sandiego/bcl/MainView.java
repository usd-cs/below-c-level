package edu.sandiego.bcl;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import java.util.ArrayList;
import java.util.List;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
@PWA(name = "Project Base for Vaadin Flow", shortName = "Project Base")
public class MainView extends VerticalLayout {
    
    private Simulation activeSimulation;
    
    //public void parseAndAddInstruction()

    public MainView() {
        activeSimulation = new Simulation();
        
        //List<x86ProgramLine> program = new ArrayList<>();
        //x86NullaryInstruction inst = new x86NullaryInstruction(InstructionType.RET, OpSize.QUAD, 1, null, new x86OperandGetter());
        //program.add(inst);
        
        /*
        Button button = new Button("Click me",
                event -> Notification.show("I've been clicked!"));
        add(button);
        */
        
        Grid<x86ProgramLine> g = new Grid<>(x86ProgramLine.class);
        g.setItems(activeSimulation.getProgramLines());
        g.removeAllColumns();
        //g.addColumn(line -> line.toString()).setHeader("Coolness");
        g.addColumn(TemplateRenderer.<x86ProgramLine>of(
                "<div title='[[item.description]]'>[[item.Instruction]]</div>")
                .withProperty("Instruction", line -> line.toString())
                .withProperty("description", line -> line.getDescriptionString()))
                .setHeader("Instruction");
        
        
        TextField instructionInput = new TextField();
        instructionInput.setWidthFull();
        instructionInput.setPlaceholder("Enter an x86 instruction or comment");
        instructionInput.setClearButtonVisible(true);
        instructionInput.addValueChangeListener(event -> { 
                try { 
                    activeSimulation.appendToProgram(event.getValue());
                    instructionInput.setInvalid(false);
                    Notification.show("Successfully added!");
                    g.getDataProvider().refreshAll();
                    instructionInput.clear();

                }
                catch (X86ParsingException e) {
                    instructionInput.setErrorMessage(e.getMessage());
                    instructionInput.setInvalid(true);
                    Notification.show("Bad instruction!");
                }
        });
                
        //instructionInput.setErrorMessage("farts");
        //instructionInput.setInvalid(true);
        
        add(instructionInput);
        add(g);
    }
}
