package org.thespheres.betula.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.thespheres.betula.services.web.WebUIConfiguration;

@Named("login")
@ViewScoped
public class UserLogin implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(UserLogin.class.getName());
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String originalURL;

    @Inject
    private WebUIConfiguration webConfig;

    @PostConstruct
    public void init() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String url = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_REQUEST_URI);

        if (url != null) {
            String originalQuery = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_QUERY_STRING);
            if (originalQuery != null) {
                url += "?" + originalQuery;
            }
            this.originalURL = url;
        }
    }

    public String getHeaderLabel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext.getApplication()
                .getResourceBundle(facesContext, "bundle")
                .getString("login.panel.header")
                .replace("{0}", webConfig.getLoginProviderDisplayLabel());
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void doLogin() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();

        // Validate input
        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            addErrorMessage("login.data.empty");
            return;
        }

        // Check if already logged in
        if (request.getUserPrincipal() != null) {
            redirectAfterLogin(externalContext);
            return;
        }

        // Attempt authentication
        try {
            request.login(username, password);
            LOGGER.log(Level.INFO, "User logged in successfully: {0}", username);

            // Clear password from memory
            password = null;

            // Redirect to original URL or main page
            redirectAfterLogin(externalContext);

        } catch (ServletException e) {
            LOGGER.log(Level.WARNING, "Login failed for user: {0}", username);
            addErrorMessage("login.iserv.notauthorized");
            password = null; // Clear password on failure
        }
    }

    private void redirectAfterLogin(ExternalContext externalContext) {
        try {
            String redirectURL = determineRedirectURL(externalContext);
            externalContext.redirect(redirectURL);
            FacesContext.getCurrentInstance().responseComplete();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Redirect failed after login", e);
            addErrorMessage("login.redirect.failed");
        }
    }

    private String determineRedirectURL(ExternalContext externalContext) {
        if (originalURL != null && !originalURL.isEmpty()) {
            return originalURL;
        }
        // Default to main page
        return externalContext.getRequestContextPath() + "/ui/main.xhtml";
    }

    private void addErrorMessage(String bundleKey) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String message = facesContext.getApplication()
                .getResourceBundle(facesContext, "bundle")
                .getString(bundleKey);

        facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

}
