/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.push;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.security.RolesAllowed;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.thespheres.betula.services.jms.AbstractDocumentEvent;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@RolesAllowed({"signee"})
@ServerEndpoint("/messages")
public class MessagesEndpoint {

    private Session session;
    private static final EventBus events = new EventBus();//AsyncEventBus?

    public static EventBus getEventBus() {
        return events;
    }

    @OnOpen
    public String onOpen(final Session session, final EndpointConfig config) {
        this.session = session;
        events.register(this);
        return null;
    }

    @OnClose
    public void close(final Session session, final CloseReason reason) {
        events.unregister(this);
        this.session = null;
    }

    @OnMessage
    public String onMessage(final String message) {
        return null;
    }

    @Subscribe
    public void onChange(final AbstractDocumentEvent change) {
        session.getAsyncRemote().sendObject(change);
    }

}
