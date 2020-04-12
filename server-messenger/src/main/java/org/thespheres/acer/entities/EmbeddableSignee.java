/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableSignee implements Serializable {

    @Column(name = "SIGNEE_PREFIX")
    private String prefix;
    @Column(name = "SIGNEE_SUFFIX", length = 64)
    private String suffix;
    @Column(name = "SIGNEE_ALIAS")
    private boolean alias;

    public EmbeddableSignee() {
    }

    public EmbeddableSignee(Signee signee) {
        this.prefix = signee.getPrefix();
        this.suffix = signee.getSuffix();
        this.alias = signee.isAlias();
    }

    public Signee getSignee() {
        return new Signee(prefix, suffix, alias);
    }
}
