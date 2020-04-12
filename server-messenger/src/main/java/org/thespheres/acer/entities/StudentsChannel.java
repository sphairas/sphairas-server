/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "STUDENTS_CHANNEL")
@Access(AccessType.FIELD)
public class StudentsChannel extends DynamicChannel {

    @Column(name = "DISPLAY_NAME")
    private String displayName;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "STUDENTS_CHANNEL_STUDENTS", joinColumns = @JoinColumn(name = "BASE_CHANNEL_NAME", referencedColumnName = "CHANNEL_NAME"))
    private Set<StudentAction> studs;

    public StudentsChannel() {
    }

    public StudentsChannel(String name, String displayName) {
        super(name);
        this.displayName = displayName;
    }

    public StudentsChannel(String baseName, int reification, String displayName) {
        super(baseName, reification);
        this.displayName = displayName;
    }

    @Override
    public String getDefaultDisplayName() {
        return displayName;
    }

    public void setDefaultDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<StudentAction> getStudentAction() {
        if (studs == null) {
            studs = new HashSet<>();
        }
        return studs;
    }

}
