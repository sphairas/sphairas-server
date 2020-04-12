/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.push;

import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;
import org.primefaces.push.Encoder;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;
import org.thespheres.betula.services.jms.QualifiedEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Timestamp;

/**
 *
 * @author boris.heithecker
 */
public class DocumentMessageEncoder implements Encoder<AbstractDocumentEvent, String> {

    @Override
    public String encode(AbstractDocumentEvent event) {
        final JSONObject ret = new JSONObject();
        final DocumentId did = event.getSource();
        final JSONObject didObject = new JSONObject();
        try {
            didObject.put("id", did.getId());
            didObject.put("authority", did.getAuthority());
            didObject.put("version", did.getVersion().getVersion());
            ret.put("document", didObject);
        } catch (JSONException ex) {
        }
        if (event instanceof QualifiedEvent) {
            final Timestamp time = ((QualifiedEvent) event).getTimestamp();
            if (time != null) {
                try {
                    ret.put("timestamp", time.getDate().getTime());
                } catch (JSONException ex) {
                }
            }
        }
        return ret.toString();
    }

}
