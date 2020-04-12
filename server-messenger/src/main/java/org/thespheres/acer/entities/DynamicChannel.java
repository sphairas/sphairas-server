/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.thespheres.betula.Identity;

/**
 *
 * @author boris.heithecker
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class DynamicChannel extends BaseChannel {

    @Column(name = "TERMSCHEDULE_PROVIDER", length = 64)
    private String scheduleProvider;
    @Column(name = "NAMING_PROVIDER", length = 64)
    private String namingProvider;

    protected DynamicChannel() {
    }

    protected DynamicChannel(String name) {
        super(name);
    }

    protected DynamicChannel(String baseName, int reification) {
        super(baseName, reification);
    }

    public abstract String getDefaultDisplayName();

    public Identity getResolvableIdentity() {
        return null;
    }

    public String getTermScheduleProvider() {
        return scheduleProvider;
    }

    public void seTermScheduleProvider(String scheduleProvider) {
        this.scheduleProvider = scheduleProvider;
    }

    public String getNamingProvider() {
        return namingProvider;
    }

    public void setNamingProvider(String namingProvider) {
        this.namingProvider = namingProvider;
    }

}
