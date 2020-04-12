/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.io.Serializable;
import org.thespheres.acer.MessageId;

/**
 *
 * @author boris.heithecker
 */
public class FastMessage implements Serializable {

    private final MessageId id;
    private final String author;
    private final String message;
    private final boolean read;
    private final boolean canDiscard;
    private final boolean confidential;
    private final boolean canEdit;
    private final String channel;

    public FastMessage(MessageId id, String author, String message, String channel, boolean isRead, boolean canDiscard, boolean confidential, boolean canEdit) {
        this.id = id;
        this.author = author;
        this.message = message;
        this.channel = channel;
        this.read = isRead;
        this.canDiscard = canDiscard;
        this.confidential = confidential;
        this.canEdit = canEdit;
    }

    public MessageId getMessageId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getAuthorDisplayName() {
        return author;
    }

    public String getFormattedMessageText() {
        return message;
    }

    public boolean isMarkedRead() {
        return read;
    }

    public boolean canDiscard() {
        return canDiscard;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean isConfidential() {
        return confidential;
    }

}
