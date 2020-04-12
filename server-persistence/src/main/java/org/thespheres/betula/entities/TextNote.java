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
@Table(name = "TEXT_NOTE_ENTITY")
@Access(AccessType.FIELD)
public class TextNote extends BaseNoteEntity<String> implements Serializable {

    private static final long serialVersionUID = 1L;
    @Column(name = "NOTE_TEXT_VALUE", length = 8192)
    private String note;

    public TextNote() {
    }

    public TextNote(final BaseDocumentEntity document, final String key) {
        super(document, key);
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}
