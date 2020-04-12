/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import javax.faces.application.FacesMessage;

/**
 *
 * @author boris.heithecker
 */
public class BetulaPushMessage extends FacesMessage {

    //weitere siehe Primefaces 5.0 pdf-Doc PrimeFaces.ajax.Request.handle
    private String formId = "main";
    private String source;
    private String update;
    private String process;
    private boolean logout = false;

    public BetulaPushMessage() {
    }

    public BetulaPushMessage(Severity severity, String summary, String detail) {
        super(severity, summary, detail);
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUpdate() {
        return update;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public boolean isLogout() {
        return logout;
    }

    public void setLogout(boolean logout) {
        this.logout = logout;
    }

}
