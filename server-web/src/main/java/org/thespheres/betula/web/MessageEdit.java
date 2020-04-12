/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.openide.util.NbBundle;
import org.primefaces.context.RequestContext;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.MessageId.Version;
import org.thespheres.betula.server.beans.FastMessages;
import org.thespheres.betula.util.CollectionUtil;

/**
 *
 * @author boris.heithecker
 */
@ManagedBean(name = "messageEdit")
@ViewScoped
@Stateless
public class MessageEdit implements Serializable {

    public static final String PARAMETER_MESSAGE_ID = "message-id";
    @EJB(beanName = "FastMessagesImpl")
    private FastMessages messages;
    @ManagedProperty("#{app}")
    private BetulaWebApplication application;
//    private Map<String, String> channels;
    private List<String> channels;
    private String inputTextareaErrorMessage;
    private String selectOneMenuErrorMessage;
    private Msg msg;

    public BetulaWebApplication getApplication() {
        return application;
    }

    public void setApplication(BetulaWebApplication application) {
        this.application = application;
    }

    private synchronized Msg getMsg() {
        if (msg == null) {
            msg = new Msg();
            final MessageId mid = getMessageId();
            if (mid != null) {
                messages.getFastMessages(false).stream()
                        .filter(fm -> fm.getMessageId().equals(mid))
                        .collect(CollectionUtil.singleton())
                        .ifPresent(fastmsg -> {
                            msg.editMessageText = fastmsg.getFormattedMessageText();
                            msg.editChannel = fastmsg.getChannel();
                            msg.confidential = fastmsg.isConfidential();
                        });

            }
        }
        return msg;
    }

    public void editOK(ActionEvent actionEvent) {
        if (isValidMessage()) {
            getMsg().message = messages.publish(getMsg().message, getEditChannel(), getEditMessageText(), isConfidential(), isSendEmail());
            RequestContext.getCurrentInstance().closeDialog(null);
        }
    }

    public boolean isValidMessage() {
        boolean ret = true;
        if (getEditChannel() == null) {
            selectOneMenuErrorMessage = NbBundle.getMessage(MessageEdit.class, "MessageEdit.selectOneMenuErrorMessage");
            ret = false;
        } else {
            selectOneMenuErrorMessage = null;
        }
        if (getEditMessageText().isEmpty()) {
            inputTextareaErrorMessage = NbBundle.getMessage(MessageEdit.class, "MessageEdit.inputTextareaErrorMessage");
            ret = false;
        } else {
            inputTextareaErrorMessage = null;
        }
        return ret;
    }

    public String getInputTextareaErrorMessage() {
        return inputTextareaErrorMessage;
    }

    public String getSelectOneMenuErrorMessage() {
        return selectOneMenuErrorMessage;
    }

    public List<String> getChannels() {
        if (channels == null) {
            channels = new ArrayList<>(messages.getChannels().keySet());
        }
        return channels;
//        return getChannelsMap().keySet().stream().sorted((s1, s2) -> collator.compare(getChannelDisplayName(s1), getChannelDisplayName(s2))).collect(Collectors.toList());
    }

    public String getChannelDisplayName(String channel) {
//        return channel != null ? getChannelsMap().get(channel) : "";
        return messages.getChannels().get(channel);
    }

//    private Map<String, String> getChannelsMap() {
//        if (channels == null) {
//            channels = messages.getChannels().stream().collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
//        }
//        return channels;
//    }
    public String getEditMessageText() {
        return getMsg().editMessageText;
    }

    public void setEditMessageText(String editMessageText) {
        getMsg().editMessageText = editMessageText;
    }

    public String getEditChannel() {
        return getMsg().editChannel;
    }

    public void setEditChannel(String name) {
        getMsg().editChannel = name;
    }

    public boolean isSendEmail() {
        return getMsg().sendEmail;
    }

    public void setSendEmail(boolean send) {
        getMsg().sendEmail = send;
    }

    public boolean isConfidential() {
        return getMsg().confidential;
    }

    public void setConfidential(boolean confidential) {
        getMsg().confidential = confidential;
    }

    private MessageId getMessageId() {
        String[] p = getParameter(PARAMETER_MESSAGE_ID);
        if (p != null && p.length == 3) {
            return new MessageId(p[0], Long.parseLong(p[1]), Version.parse(p[2]));
        }
        return null;
    }

    private String[] getParameter(String name) {
        Map<String, String[]> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
        return params.get(name);
    }

    private final class Msg implements Serializable {

        private String editChannel;
        private String editMessageText = "";
        private MessageId message;
        private boolean sendEmail = false;
        private boolean confidential = false;
    }
}
