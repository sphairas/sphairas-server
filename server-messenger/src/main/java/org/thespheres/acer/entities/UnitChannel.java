/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.acer.entities.local.Naming;
import org.thespheres.betula.Identity;
import org.thespheres.betula.UnitId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "UNIT_CHANNEL")
@Access(AccessType.FIELD)
public class UnitChannel extends DynamicChannel {

    @Embedded
    private EmbeddableUnitId unit;
    @Column(name = "PRIMARY_UNIT_CHANNEL")
    private boolean pu;

    public UnitChannel() {
    }

    public UnitChannel(UnitId unit, boolean primary) {
        this(unit, primary, unit.getId());
    }

    public UnitChannel(UnitId unit, boolean pu, String name) {
        super(name);
        this.unit = new EmbeddableUnitId(unit);
        this.pu = pu;
    }

    public UnitChannel(UnitId unit, boolean pu, String baseName, int reification) {
        super(baseName, reification);
        this.unit = new EmbeddableUnitId(unit);
        this.pu = pu;
    }

    @Override
    public String getDefaultDisplayName() {
        if (unit != null) {
            try {
                return lookupNamingBean().resolveDisplayName(getUnit(), null, null);
            } catch (NamingException ex) {
                Logger.getLogger(UnitChannel.class.getName()).log(Level.WARNING, "Could not call java:module/Naming", ex);
            }
        }
        return unit.toString();
    }

    public boolean isisPrimaryUnitChannel() {
        return pu;
    }

    @Override
    public Identity getResolvableIdentity() {
        return getUnit();
    }

    public UnitId getUnit() {
        return unit != null ? unit.getUnitId() : null;
    }

    private Naming lookupNamingBean() throws NamingException {
        Context c = new InitialContext();
        return (Naming) c.lookup("java:module/Naming");
    }

}
