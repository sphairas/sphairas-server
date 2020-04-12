/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
public class SigneeAction implements Serializable {

    public enum Action {

        INCLUDE,
        EXCLUDE,
    }
    private static final long serialVersionUID = 1L;
    @Embedded
    private EmbeddableSignee signee;
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "LIST_ACTION")
    private Action action;

    public SigneeAction() {
    }

    public SigneeAction(Signee signee, Action action) {
        this.signee = new EmbeddableSignee(signee);
        this.action = action;
    }

    public Signee getSignee() {
        return signee.getSignee();
    }

    public Action getAction() {
        return action;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.signee);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SigneeAction other = (SigneeAction) obj;
        return Objects.equals(this.signee, other.signee);
    }

}
