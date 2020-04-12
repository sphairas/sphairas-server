/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.util.Properties;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "DURABLE_MESSAGE")
@Access(AccessType.FIELD)
public class DurableMessage extends BaseTextMessage {

    @Column(name = "MESSAGE_PRIORITY")
    private int priority = 0;
    @Column(name = "MESSAGE_PROPERTIES")
    private Properties props;

    public DurableMessage() {
    }

    public DurableMessage(String authority, BaseChannel channel, Signee creator, String text) {
        super(authority, channel, creator, text);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Properties getProperties() {
        if (props == null) {
            props = new Properties();
        }
        return props;
    }

}
