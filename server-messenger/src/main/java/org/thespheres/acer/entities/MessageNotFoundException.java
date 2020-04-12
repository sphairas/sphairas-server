/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import javax.persistence.EntityNotFoundException;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.acer.MessageId;

/**
 *
 * @author boris.heithecker
 */
@Messages("MessageNotFoundException.message=Message {0} (Authority: {1}) not found in database.")
public class MessageNotFoundException extends EntityNotFoundException {

    private final MessageId message;

    public MessageNotFoundException(MessageId message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        String msg = NbBundle.getMessage(MessageNotFoundException.class, "MessageNotFoundException.message", Long.toString(message.getId()), message.getAuthority());
        return msg;
    }

}
