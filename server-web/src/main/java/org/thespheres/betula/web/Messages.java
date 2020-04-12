/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.context.FacesContext;
import org.primefaces.context.RequestContext;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.beans.MessageEvent;
import org.thespheres.betula.server.beans.FastMessage;

/**
 *
 * @author boris.heithecker
 */
public class Messages implements Serializable {

    private final BetulaWebApplication application;

    Messages(BetulaWebApplication app) {
        this.application = app;
    }

    public void onClose(MessageId message) {
        message.toString();
    }

    public void onToggle(ToggleEvent toggle) {
        boolean read = toggle.getVisibility().equals(Visibility.HIDDEN);
        FacesContext context = FacesContext.getCurrentInstance();
//        FastMessage message = context.getApplication().evaluateExpressionGet(context, "#{app.user.fastMessages}", FastMessage.class);
        try {
            FastMessage message = context.getApplication().evaluateExpressionGet(context, "#{message}", FastMessage.class);
            if (message != null) {
                application.getFastMessages().markRead(message.getMessageId(), read);
            }
        } catch (IllegalArgumentException illex) {
        }
//         MyBean2 myBean2 = (MyBean2) FacesContext.getCurrentInstance().getExternalContext()
//            .getRequestMap().get("myBean2");
    }

    public void edit(MessageId message) {
        Map<String, List<String>> params = new HashMap<>();
        List<String> mid = Arrays.asList(message.getAuthority(), Long.toString(message.getId()), message.getVersion().getVersion());
        params.put(MessageEdit.PARAMETER_MESSAGE_ID, mid);
        openDialog(params);
    }

    public String getChannelDisplayName(String channel) {
        return application.getFastMessages().getChannels().get(channel);
    }

    public void create() {
        openDialog(null);
    }

    private void openDialog(Map<String, List<String>> params) {
        Map<String, Object> options = new HashMap<>();
        options.put("modal", true);
        options.put("contentHeight", 320);
        options.put("contentWidth", 640); ////hint: available options are modal, draggable, resizable, width, height, contentWidth and contentHeight
        RequestContext.getCurrentInstance().openDialog("content/editMessage", options, params);
    }

    public void delete(MessageId message) {
        application.getMessages().delete(message);
    }

    void onMessageEvent(MessageEvent event) {
        application.getUser().invalidateMessages();
        if (application.getCurrentPage().equals("messages")) { // && fm.affects(event.getSource())) { //application.getFastMessages().affects(event.getSource())) {//Does not work, we dont have a valid sessionscoped context
            EventBus eventBus = EventBusFactory.getDefault().eventBus();
            BetulaPushMessage bpm = new BetulaPushMessage();
//            bpm.setSource("main:content_content");
//            bpm.setUpdate("main:content_content");
            bpm.setSource("main:message-panels");
            bpm.setUpdate("main:message-panels");
            eventBus.publish(NotifyGradeUpdateResource.CHANNEL_BASE + application.getUser().getSignee().getId(), bpm);
        }
    }

//    private FastMessages lookupFastMessagesImplLocal() {
//        try {
//            Context c = new InitialContext();
//            return (FastMessages) c.lookup("java:global/Betula_Server/Betula_Messaging/FastMessagesImpl!org.thespheres.betula.server.beans.FastMessages");
//        } catch (NamingException ne) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
//            throw new RuntimeException(ne);
//        }
//    }
}
