/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.facade;

import javax.ejb.Local;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.entities.messages.tracking.MailStatusTracker;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface MailMessageFacade {

    public MailStatusTracker sendMessageAsEmail(MessageId mid);

    public MailStatusTracker getTracker(long id);

}
