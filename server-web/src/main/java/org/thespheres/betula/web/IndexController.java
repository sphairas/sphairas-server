package org.thespheres.betula.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.SecurityContext;

/**
 *
 * @author boris.heithecker
 */
@Named("start")
@RequestScoped
public class IndexController {

    @Inject
    private SecurityContext securityContext;

//    @Inject
//    private FacesContext facesContext;

    public String redirect() {
        // Adding ?faces-redirect=true forces a clean GET request (PRG Pattern)
        if (securityContext.getCallerPrincipal() != null) {
            return "main.xhtml?faces-redirect=true";
        }
        return "login.xhtml?faces-redirect=true";
    }
}
