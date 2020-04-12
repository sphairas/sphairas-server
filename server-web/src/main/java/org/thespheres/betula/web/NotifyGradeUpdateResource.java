/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import org.atmosphere.config.service.ManagedService;
import org.primefaces.push.EventBus;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;

/**
 *
 * @author boris.heithecker
 */
@PushEndpoint("/notify-grade-update/{signee}")
//@Singleton
@ManagedService
public class NotifyGradeUpdateResource {

    public static final String CHANNEL_BASE = "/notify-grade-update/";
    @PathParam("signee")
    private String signeeId;

    @OnOpen
    public void onOpen(RemoteEndpoint r, EventBus eventBus) {
//        Map m = r.headersMap();
//        r.headersMap().put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    @OnClose
    public void onClose(RemoteEndpoint r, EventBus eventBus) {
//        r.address();
    }

    @OnMessage(encoders = {BetulaPushMessageEncoder.class})
    public BetulaPushMessage onMessage(BetulaPushMessage message) {
        return message;
    }
}
