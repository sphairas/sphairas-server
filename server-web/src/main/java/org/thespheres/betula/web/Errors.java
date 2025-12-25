package org.thespheres.betula.web;

import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.ServletRequest;
import java.io.Serializable;
import java.util.logging.Logger;

@Named("errors")
@ViewScoped
public class Errors implements Serializable {

    private static final Logger logger = Logger.getLogger(Errors.class.getName());

    public String getErrorMessage() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ServletRequest request = (ServletRequest) fc.getExternalContext().getRequest();
        return (String) request.getAttribute("jakarta.servlet.error.message");
    }

    public Integer getStatusCode() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ServletRequest request = (ServletRequest) fc.getExternalContext().getRequest();
        return (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    }

    public String getExceptionType() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ServletRequest request = (ServletRequest) fc.getExternalContext().getRequest();
        Throwable throwable = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        return throwable != null ? throwable.getClass().getName() : "Unknown";
    }

    public void logError() {
        logger.severe("Error occurred: " + getErrorMessage()
                + " (Status: " + getStatusCode() + ")");
    }
}
