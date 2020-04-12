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
@Table(name = "STATIC_CHANNEL")
@Access(AccessType.FIELD)
public class StaticChannel extends BaseChannel {

    @Column(name = "DISPLAY_NAME")
    private String displayName;
    @Embedded
    @ElementCollection
    @CollectionTable(name = "STATIC_CHANNEL_SIGNEES",
            joinColumns = @JoinColumn(name = "BASE_CHANNEL_NAME", referencedColumnName = "CHANNEL_NAME"))
    private Set<SigneeAction> signees = new HashSet<>();

    public StaticChannel() {
    }

    public StaticChannel(String name, String displayName) {
        super(name);
        this.displayName = displayName;
    }

    public StaticChannel(String name, String displayName, int reification) {
        super(name, reification);
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    //Default action (unlistet signee included or excluded is defined in channel policy...
    public Set<SigneeAction> getSignees() {
        return signees;
    }

}
