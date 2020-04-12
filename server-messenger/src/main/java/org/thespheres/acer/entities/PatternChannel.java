/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "PATTERN_CHANNEL")
@Access(AccessType.FIELD)
public class PatternChannel extends DynamicChannel {

    //TODO Saved in Persistence?
    //If null, pattern is saved 
    @Column(name = "PATTERN")
    private String pattern;
    @Column(name = "DISPLAY_MESSAGE")
    private String displayMessage;

    public PatternChannel() {
    }

    public PatternChannel(String name, String displayMessage) {
        super(name);
        this.displayMessage = displayMessage;
    }

    public PatternChannel(String baseName, int reification, String displayMessage) {
        super(baseName, reification);
        this.displayMessage = displayMessage;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getDefaultDisplayName() {
        return displayMessage;
    }
}
