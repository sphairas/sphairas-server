package org.thespheres.betula.server.beans.clients;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ejb.SessionContext;
import java.util.Set;
import org.thespheres.betula.document.Signee;

@RequestScoped
public class ActingSubject {

    private Signee signee;
    private Set<String> roles = Set.of();
    private boolean asserted;

    public void set(final Signee signee, final Set<String> roles) {
        if (asserted) {
            throw new IllegalStateException("Acting subject is already set for this request.");
        }
        this.signee = signee;
        this.roles = Set.copyOf(roles);
        this.asserted = true;
    }

    public boolean isAsserted() {
        return asserted;
    }

    public Signee getSignee() {
        return signee;
    }

    public boolean isCallerInRole(final SessionContext context, final String role) {
        return isAsserted() ? roles.contains(role) : context.isCallerInRole(role);
    }
}
