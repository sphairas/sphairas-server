/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import org.thespheres.acer.beans.MessageEvent;
import org.thespheres.betula.TermId;
import org.thespheres.betula.services.jms.MultiTargetAssessmentEvent;
import org.thespheres.betula.services.jms.TicketEvent;

/**
 *
 * @author boris.heithecker
 */
@Singleton
@LocalBean//TODO CDI sessionscped?? applicationscoped
public class EventDispatch implements Serializable {

    private final WeakHashMap<AbstractData, Object> listenerMap = new WeakHashMap();
    private WeakReference<Messages> messages;

    public void onDocumentEvent(MultiTargetAssessmentEvent<TermId> event) {
        synchronized (listenerMap) {
            ArrayList<AbstractData> toDelete = new ArrayList<>();
            listenerMap.keySet().stream().forEach(ad -> {
                if (ad.isValid()) {
                    ad.onDocumentEvent(event);
                } else {
                    toDelete.add(ad);
                }
            });
            toDelete.stream()
                    .forEach(listenerMap::remove);
        }
    }

    public void onTicketEvent(TicketEvent event) {
        synchronized (listenerMap) {
            listenerMap.keySet().stream().forEach((ad) -> {
                ad.onTicketEvent(event);
            });
        }
    }

    public void onMessageEvent(MessageEvent event) {
        Messages msg;
        if (messages != null && (msg = messages.get()) != null) {
            msg.onMessageEvent(event);
        }
    }

    public void register(AbstractData l) {
        synchronized (listenerMap) {
            listenerMap.put(l, null);
        }
    }

    public void unregister(AbstractData l) {
        synchronized (listenerMap) {
            listenerMap.remove(l);
        }
    }

    public void register(Messages messages) {
        this.messages = new WeakReference(messages);
    }

    public void unregister(Messages messages) {
        this.messages = null;
    }
}
