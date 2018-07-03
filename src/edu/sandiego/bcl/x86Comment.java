/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.sandiego.bcl;

/**
 *
 * @author Caitlin
 */
public class x86Comment {

    public final String comment;

    public x86Comment(String c) {
        this.comment = c;
    }
    
    @Override
    public String toString() {
        return comment;
    }
}
