package org.thespheres.betula.web.config;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;
import org.primefaces.application.exceptionhandler.PrimeExceptionHandlerFactory;

public class ViewExpiredExceptionHandlerFactory extends ExceptionHandlerFactory {

    private final ExceptionHandlerFactory wrapped;

    public ViewExpiredExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        this.wrapped = new PrimeExceptionHandlerFactory(parent);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new ViewExpiredExceptionHandler(wrapped.getExceptionHandler());
    }
}