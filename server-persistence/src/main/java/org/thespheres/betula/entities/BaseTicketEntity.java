/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.entities.config.AppProperties;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "BASE_TICKET")
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public abstract class BaseTicketEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @javax.persistence.Version
    @Column(name = "BASETICKETENTITY_VERSION")
    private long version;

    public BaseTicketEntity() {
    }

    public long getId() {
        return id;
    }

    public Ticket getTicket() {
        return new Ticket(AppProperties.ticketsAuthority(), getId());
    }

}
