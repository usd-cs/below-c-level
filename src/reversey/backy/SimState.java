/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

import javafx.scene.control.ListView;

/**
 *
 * @author Caitlin
 */
public class SimState {
    private final ListView<x86ProgramLine> programView;
    private final Simulation simulator;

    public SimState(ListView<x86ProgramLine> programView,
                    Simulation simulator) {
        this.programView = programView;
        this.simulator = simulator;
    }

    public Simulation getSimulator() {
        return this.simulator;
    }

    public ListView<x86ProgramLine> getProgramView() {
        return this.programView;
    }
}