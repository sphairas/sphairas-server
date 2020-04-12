/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.util.List;
import java.util.SortedMap;
import javax.ejb.Local;
import org.thespheres.acer.MessageId;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface FastMessages {

    public List<FastMessage> getFastMessages(boolean refresh);

    //keys sorted according to values = displaynames!
    public SortedMap<String, String> getChannels();

    public MessageId publish(MessageId mid, String channel, String messageText, boolean confidential, boolean sendEmail);
    
    public void delete(MessageId mid);

    public void markRead(MessageId messageId, boolean read);
}
