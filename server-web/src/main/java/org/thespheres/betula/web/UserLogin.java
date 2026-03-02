package org.thespheres.betula.web;

import java.io.Serializable;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.thespheres.betula.services.web.WebUIConfiguration;

@Named("login")
@ViewScoped
public class UserLogin implements Serializable {

//    private static final Logger LOGGER = Logger.getLogger(UserLogin.class.getName());
    private static final long serialVersionUID = 1L;

    @NotNull(message = "")
    private String username;
    @NotNull(message = "")
    private String password;
//    private String originalURL;

    @Inject
    private WebUIConfiguration webConfig;
    @Inject
    private SecurityContext securityContext;
    @Inject
    private FacesContext facesContext;

//    @PostConstruct
//    public void init() {
//        ExternalContext externalContext = facesContext.getExternalContext();
//        String url = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_REQUEST_URI);
//
//        if (url != null) {
//            String originalQuery = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_QUERY_STRING);
//            if (originalQuery != null) {
//                url += "?" + originalQuery;
//            }
//            this.originalURL = url;
//        }
//    }

    public String getHeaderLabel() {
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
//        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        UsernamePasswordCredential credential = new UsernamePasswordCredential(username, password);
        AuthenticationStatus status = securityContext.authenticate(
                request,
                response,
                AuthenticationParameters.withParams().credential(credential)
        );

        switch (status) {
            case SUCCESS:
//                redirectAfterLogin(externalContext);
                facesContext.responseComplete();
                break;
            case SEND_FAILURE:
                addErrorMessage("login.iserv.notauthorized");
                password = null;
                break;
            case SEND_CONTINUE:
                // The mechanism is taking over (e.g., redirecting to a multi-factor page)
                facesContext.responseComplete();
                break;
        }
    }

//    private void redirectAfterLogin(ExternalContext externalContext) {
//        try {
//            String redirectURL = externalContext.getRequestContextPath() + "/ui/main.xhtml";// determineRedirectURL(externalContext);
//            externalContext.redirect(redirectURL);
////            FacesContext.getCurrentInstance().responseComplete();
//        } catch (IOException e) {
//            LOGGER.log(Level.SEVERE, "Redirect failed after login", e);
//            addErrorMessage("login.redirect.failed");
//        }
//    }
//
//    private String determineRedirectURL(ExternalContext externalContext) {
//        if (originalURL != null && !originalURL.isEmpty()) {
//            return originalURL;
//        }
//        // Default to main page
//        return externalContext.getRequestContextPath() + "/ui/main.xhtml";
//    }

    private void addErrorMessage(String bundleKey) {
        String message = facesContext.getApplication()
                .getResourceBundle(facesContext, "bundle")
                .getString(bundleKey);
        facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

}
