/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.resource;

import org.thespheres.acer.MessageId;

/**
 *
 * @author boris.heithecker
 */
class RestUtilities {

    static MessageId messageId(String authority, long id, String v) {
        final MessageId.Version version;
        if (v != null) {
            version = MessageId.Version.parse(v);
        } else {
            version = MessageId.Version.LATEST;
        }
        final MessageId message = new MessageId(authority, id, version);
        return message;
    }

}
