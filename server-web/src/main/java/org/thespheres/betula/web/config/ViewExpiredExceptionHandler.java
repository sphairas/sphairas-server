package org.thespheres.betula.web.config;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger LOG = Logger.getLogger(ViewExpiredExceptionHandler.class.getName());

    private final ExceptionHandler wrapped;

    public ViewExpiredExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return wrapped;
    }

    @Override
    public void handle() throws FacesException {
        final FacesContext context = FacesContext.getCurrentInstance();
        if (context == null || context.getPartialViewContext().isAjaxRequest()) {
            wrapped.handle();
            return;
        }

        for (Iterator<ExceptionQueuedEvent> iterator = getUnhandledExceptionQueuedEvents().iterator(); iterator.hasNext();) {
            ExceptionQueuedEvent event = iterator.next();
            ExceptionQueuedEventContext eventContext = (ExceptionQueuedEventContext) event.getSource();
            Throwable throwable = eventContext.getException();
            ViewExpiredException viewExpired = findViewExpiredException(throwable);
            if (viewExpired == null) {
                continue;
            }

            iterator.remove();
            redirectToSessionExpiredPage(context, viewExpired);
            return;
        }

        wrapped.handle();
    }

    private ViewExpiredException findViewExpiredException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ViewExpiredException viewExpiredException) {
                return viewExpiredException;
            }
            current = current.getCause();
        }
        return null;
    }

    private void redirectToSessionExpiredPage(FacesContext context, ViewExpiredException exception) {
        ExternalContext externalContext = context.getExternalContext();
        String redirect = externalContext.getRequestContextPath() + "/error-pages/sessionExpired.xhtml";
        try {
            externalContext.getFlash().put("viewExpiredViewId", exception.getViewId());
            externalContext.redirect(redirect);
            context.responseComplete();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to redirect after ViewExpiredException.", ex);
            throw new FacesException(ex);
        }
    }
}