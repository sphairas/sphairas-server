/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "SIGNEEINFO_CHANGELOG")
@Access(AccessType.FIELD)
public class SigneeInfoChangeLog extends BaseChangeLog<EmbeddableSigneeInfo> implements Serializable {

    @JoinColumns({
        @JoinColumn(name = "SIGNEE_ID", referencedColumnName = "SIGNEE_ID", updatable = false),
        @JoinColumn(name = "SIGNEE_AUTHORITY", referencedColumnName = "SIGNEE_AUTHORITY", updatable = false),
        @JoinColumn(name = "SIGNEE_ALIAS", referencedColumnName = "SIGNEE_ALIAS", updatable = false)
    })
    @OneToOne
    protected SigneeEntity signee;
    @Column(name = "SIGNGEE_TYPE", length = 64, updatable = false)
    protected String type;

    public SigneeInfoChangeLog() {
    }

    public SigneeInfoChangeLog(BaseDocumentEntity parent, String property, String entitlement, SigneeEntity signee, Action action) {
        super(parent, property, action);
        this.signee = signee;
        this.type = entitlement;
    }

    @Override
    public EmbeddableSigneeInfo getValue() {
        return new EmbeddableSigneeInfo(signee, type);
    }

}
