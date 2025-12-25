/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
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
    @jakarta.persistence.Version
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
