/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service;

import javax.ejb.SessionContext;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.Identity;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.NotFoundFault;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.SyntaxFault;
import org.thespheres.betula.services.ws.UnauthorizedException;
import org.thespheres.betula.services.ws.UnauthorizedFault;

/**
 *
 * @author boris.heithecker
 */
public class ServiceUtils {

    public static void checkUnitAdmin(final SessionContext context) throws UnauthorizedException {
        if (!context.isCallerInRole("unitadmin")) {
            throw createUnauthorizedException();
        }
    }

    public static boolean getBulkProcessValue(Entry entry, SessionContext context) throws UnauthorizedException {
        String hint = entry.getHints().get("process-bulk");
        boolean bulk = hint != null && hint.equals("true");
        if (bulk) {
            checkUnitAdmin(context);
        }
        return bulk;
    }

    public static <E extends Entry> E toEntryType(Envelope node, Class<E> entryType) throws SyntaxException {
        try {
            return entryType.cast(node);
        } catch (ClassCastException e) {
        }
        throw ServiceUtils.createSyntaxException("Cannot convert to entry: " + node.getClass().getCanonicalName());
    }

    public static <E extends Entry> boolean isEntryTypeOf(Envelope node, Class<E> entryType) throws SyntaxException {
        try {
            entryType.cast(node);
            return true;
        } catch (ClassCastException e) {
        }
        return false;
    }

    public static <I extends Identity> Entry<I, ?> toEntry(Envelope node, Class<I> idClass) throws SyntaxException {
        if (node instanceof Entry) {
            final Entry entryNode = (Entry) node;
            if (entryNode.getIdentity() != null && idClass.isAssignableFrom(entryNode.getIdentity().getClass())) {
                return (Entry<I, ?>) node;
            }
        }
        throw ServiceUtils.createSyntaxException("Cannot convert to entry: " + node.getClass().getCanonicalName());
    }

    public static <I extends Identity> boolean isEntryOf(Envelope node, Class<I> idClass) {
        if (node instanceof Entry) {
            final Entry entryNode = (Entry) node;
            return idClass.isAssignableFrom(entryNode.getIdentity().getClass());
        }
        return false;
    }

    public static <I extends Identity, V extends Object> Entry<I, V> toEntry(Envelope node, Class<I> idClass, Class<V> valueClass) throws SyntaxException {
        Entry<I, ?> entry = toEntry(node, idClass);
        if (entry.getValue() == null || valueClass.isAssignableFrom(entry.getValue().getClass())) {
            return (Entry<I, V>) entry;
        }
        throw ServiceUtils.createSyntaxException("Cannot convert to entry: " + entry.getClass().getCanonicalName());
    }

    public static SyntaxException createSyntaxException(final String message) {
        final SyntaxFault info = new SyntaxFault();
        info.setMessage(message);
        return new SyntaxException(message, info);
    }

    @Messages("ServiceUtils.createSyntaxException.message=Invocation of {0}.{1} has thrown {1} with message {2}")
    public static SyntaxException createSyntaxException(final Throwable cause) {
        final SyntaxFault info = new SyntaxFault();
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        final String msg;
        if (stack.length > 1) {
            final StackTraceElement el = stack[1];
            msg = NbBundle.getMessage(ServiceUtils.class, "ServiceUtils.createSyntaxException.message", new Object[]{el.getClassName(), el.getMethodName(), cause.getClass().getCanonicalName(), cause.getMessage()});
        } else {
            msg = cause.getLocalizedMessage();
        }
        info.setMessage(msg);
        return new SyntaxException(cause.getMessage(), info, cause);
    }

    @Messages("ServiceUtils.createUnauthorizedException.message=Not authorized")
    public static UnauthorizedException createUnauthorizedException() {
        final UnauthorizedFault info = new UnauthorizedFault();
        final String message = NbBundle.getMessage(ServiceUtils.class, "ServiceUtils.createNotFoundException.message");
        info.setMessage(message);
        return new UnauthorizedException(message, info);
    }

    @Messages("ServiceUtils.createNotFoundException.message=Not found: {0}")
    public static NotFoundException createNotFoundException(final Identity identity) {
        final NotFoundFault info = new NotFoundFault();
        final String id = identity != null ? identity.toString() : null;
        final String message = NbBundle.getMessage(ServiceUtils.class, "ServiceUtils.createNotFoundException.message", id);
        info.setMessage(message);
        return new NotFoundException(message, info);
    }
}
