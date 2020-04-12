/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

//@Entity
//@Table(name = "TERMREPORT2_DOCUMENT_REPORT_TEXT_VALUES2",
//        uniqueConstraints = {
//            @UniqueConstraint(columnNames = {"GRADE_KEY", "TERMREPORT_AUTHORITY", "TERMREPORT_DOCUMENT_ID", "TERMREPORT_VERSION"})
//        })
//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
//@Access(AccessType.FIELD)
public class TermReportDocumentEntity2TextEntry2 implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long id;
    @JoinColumns({
        @JoinColumn(name = "TERMREPORT_AUTHORITY", referencedColumnName = "DOCUMENT_AUTHORITY", nullable = false),
        @JoinColumn(name = "TERMREPORT_DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID", nullable = false),
        @JoinColumn(name = "TERMREPORT_VERSION", referencedColumnName = "DOCUMENT_VERSION", nullable = false)})
    @ManyToOne(fetch = FetchType.LAZY)
    private TermReportDocumentEntity2 document;
    @Column(name = "TEXT_ENTRY_TIMESTAMP")
    protected Timestamp timestamp;
    @Column(name = "TEXT_KEY", nullable = false)
    private String mapKey;
    @Column(name = "TEXT")
    private String text;

    public TermReportDocumentEntity2TextEntry2() {
    }

    public TermReportDocumentEntity2TextEntry2(final TermReportDocumentEntity2 parent, final String key) {
        this.document = parent;
        this.mapKey = key;
    }

    public String getKey() {
        return mapKey;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        setText(text, Timestamp.from(Instant.now()));
    }

    public void setText(final String text, final Timestamp time) {
        this.text = text;
        this.timestamp = time;
    }

}
