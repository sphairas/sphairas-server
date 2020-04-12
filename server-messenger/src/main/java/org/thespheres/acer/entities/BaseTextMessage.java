/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.io.Serializable;
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
@Table(name = "BASE_TEXT_MESSAGE")
@Access(AccessType.FIELD)
public class BaseTextMessage extends BaseMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    @Column(name = "BASE_MESSAGE_TEXT", length = 1024)
    private String baseText;

    public BaseTextMessage() {
    }

    public BaseTextMessage(String authority, BaseChannel channel, Signee creator, String text) {
        super(authority, channel, creator);
        this.baseText = text;
    }

    public String getBaseText() {
        return baseText != null ? baseText : "";
    }

    public void setBaseText(String baseText) {
        this.baseText = baseText;
    }

}
