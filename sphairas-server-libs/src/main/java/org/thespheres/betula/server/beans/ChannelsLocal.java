/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import javax.ejb.Local;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface ChannelsLocal {

    public void updatePattern(String channel, String pattern);

    public void sendEmail(String patternChannel, String subject, String body);

    public void sendEmail(UnitId unit, String body);

    public void sendEmail(StudentId[] scope, String body);

    public void sendEmail(String channelDisplyName, String body, Signee[] recipients);
}
