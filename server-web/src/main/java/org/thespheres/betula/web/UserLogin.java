/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
//import javax.faces.view.ViewScoped;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openide.util.NbBundle;
import org.thespheres.betula.services.web.WebUIConfiguration;

/**
 *
 * @author boris.heithecker
 */
@ManagedBean(name = "login")
//@RequestScoped
@ViewScoped
//@Stateless
public class UserLogin implements Serializable {

//    public static final String USERLOGIN_JNDI = "java:global/Betula_Web/UserLogin!org.thespheres.betula.web.UserLogin";
//    public static final String ORIGINATOR = "iserv.imap.auth.originator";
//    public static final String ISERV_IMAP_AUTHORIZED_SIGNEE = "iserv.imap.auth.authorized.signee";
    private String username;
    private volatile String password;
    private String errorMessage = null;
    private String originalURL;
    @Inject
    private WebUIConfiguration webConfig;

    public UserLogin() {
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHeaderLabel() {
        return NbBundle.getMessage(UserLogin.class, "login.panel.header", webConfig.getLoginProviderDisplayLabel());
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
        if (checkEmtpy()) {
            msg = NbBundle.getMessage(UserLogin.class, "login.data.empty");
        }
        if (msg == null && request.getUserPrincipal() == null) {
            msg = authenticate2(request);
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

    private boolean checkEmtpy() {
        if (username == null && username.isEmpty()) {
            return true;
        }
        return password == null && password.isEmpty();
    }

    public String authenticate2(HttpServletRequest request) {
        try {
            request.login(username, password);
        } catch (ServletException ex) {
            return NbBundle.getMessage(UserLogin.class, "login.iserv.notauthorized");
        }
        Logger.getLogger(BetulaWebApplication.class.getName()).log(Level.INFO, "LOGGED IN {0} {1}", new Object[]{username, new Date().toLocaleString()});
        return null;
    }

}
