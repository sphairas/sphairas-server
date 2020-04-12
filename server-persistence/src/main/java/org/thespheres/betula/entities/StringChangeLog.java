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
import javax.persistence.Table;

/**
 *
 * @author boris.heithecker
 */
@Entity
@Table(name = "STRING_VALUE_CHANGELOG")
@Access(AccessType.FIELD)
public class StringChangeLog extends BaseChangeLog<String> implements Serializable {

    @Column(name = "LOG_VALUE")
    protected String value;

    public StringChangeLog() {
    }

    public StringChangeLog(BaseDocumentEntity parent, String property, String value) {
        super(parent, property, Action.UPDATE);
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
