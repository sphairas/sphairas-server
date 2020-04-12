/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.IOException;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author boris.heithecker
 */
@ManagedBean(name = "register")
@ViewScoped
public class ProviderRegister implements Serializable {

    private String errorMessage = null;
    private String originalURL;

    public ProviderRegister() {
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @PostConstruct
    public void init() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String url = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_REQUEST_URI);
//        originalURL = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_SERVLET_PATH);
        if (url != null) {
            final String originalQuery = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_QUERY_STRING);
            if (originalQuery != null) {
                url += "?" + originalQuery;
            }
            originalURL = url;
        } else {
            originalURL = null;
        }
    }

    public void doLogin2(ActionEvent evt) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        String msg = null;
//        if (checkEmtpy()) {
//            msg = NbBundle.getMessage(ProviderRegister.class, "login.data.empty");
//        }
        if (msg == null && request.getUserPrincipal() == null) {
//            msg = authenticate2(request);
        }
        if (msg == null) {
            errorMessage = null;
            ExternalContext excontext = context.getExternalContext();
            String ourl = (String) excontext.getRequestMap().get(RequestDispatcher.FORWARD_REQUEST_URI);
            try {
                final String url = findRedirect();
                excontext.redirect(url);
//                response.sendRedirect(originalURL);
//                request.getRequestDispatcher("/ui/main.xhtml").forward(request, response);
            } catch (IOException ex) {
                errorMessage = ex.getMessage();
            }
        }
    }

    private String findRedirect() {
        if (originalURL != null) {
            return originalURL;
        } else {
            //no initial slash before ui --> will resolve to unknown context root
            return "ui/main.xhtml";
        }
    }

}
