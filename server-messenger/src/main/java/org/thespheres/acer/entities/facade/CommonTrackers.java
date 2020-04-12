/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.facade;

import javax.ejb.Local;
import org.thespheres.acer.MessageId;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface CommonTrackers {

    public void setUpdated(MessageId mid, java.sql.Timestamp update) throws MessageTrackingException;

    public java.sql.Timestamp getUpdated(MessageId mid);

    public java.sql.Timestamp getPrivateStatusRead(MessageId mid);

    public void setPrivateStatusRead(MessageId mid, java.sql.Timestamp read);

    public int getPrivateStatus(MessageId mid);

    public void setPrivateStatus(MessageId mid, int privateStatus);

    public void setPrivateStatusCollapsed(MessageId mid, boolean collapsed);

    public boolean getPrivateStatusCollapsed(MessageId mid);
}
