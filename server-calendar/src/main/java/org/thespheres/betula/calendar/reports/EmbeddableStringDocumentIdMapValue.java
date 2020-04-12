/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.reports;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableStringDocumentIdMapValue implements Serializable {

    @Column(name = "REPORT_ID")
    protected String documentId;
    @Column(name = "REPORT_AUTHORITY", length = 64)
    protected String documentAuthority;
    @Column(name = "REPORT_VERSION", length = 32)
    protected String documentVersion = Version.LATEST.getVersion();
    @Column(name = "REPORT_TYPE", length = 64, nullable = true)
    protected String reportType;

    public EmbeddableStringDocumentIdMapValue() {
    }

    public EmbeddableStringDocumentIdMapValue(String type, DocumentId id) {
        this.documentId = id.getId();
        this.documentAuthority = id.getAuthority();
        this.documentVersion = id.getVersion().getVersion();
        this.reportType = type;
    }

    public DocumentId getDocumentId() {
        return new DocumentId(documentAuthority, documentId, Version.parse(documentVersion));
    }

    public String getReportType() {
        return reportType;
    }

}
