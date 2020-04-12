/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.push;

import org.atmosphere.config.service.ManagedService;
import org.primefaces.push.EventBus;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PushEndpoint;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;

/**
 *
 * @author boris.heithecker
 */
@PushEndpoint("/document-updates")
//@Singleton
@ManagedService
public class DocumentMessagesResource {

    public static final String CHANNEL_BASE = "/document-updates";

    @OnOpen
    public void onOpen(RemoteEndpoint r, EventBus eventBus) {
//        Map m = r.headersMap();
//        r.headersMap().put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    @OnClose
    public void onClose(RemoteEndpoint r, EventBus eventBus) {
//        r.address();
//        r.resource().getBroadcaster().removeAtmosphereResource(r.resource());
    }

    @OnMessage(encoders = {DocumentMessageEncoder.class})
    public AbstractDocumentEvent onMessage(AbstractDocumentEvent message) {
        return message;
    }
}
