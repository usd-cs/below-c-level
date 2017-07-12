/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reversey.backy;

/**
 *
 * @author Caitlin
 */
public interface Command {
  /** Executes the command. */
  void execute();

  /** Checks whether the command can be executed. */
  boolean canExecute();
}
