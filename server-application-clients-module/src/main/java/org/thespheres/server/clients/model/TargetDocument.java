/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.server.clients.ClientConfiguration;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class TargetDocument extends BaseDocument {

    private final List<TargetDocumentSelector> select = new ArrayList<>();
    private Marker[] markers;
    private String convention;
    private final Map<String, String> matchingSigneeTypes = new HashMap<>();
    private String targetType;
    private Date expirationDate; //Zoned...
    private String subjectAltName;

    @JsonbCreator
    public TargetDocument(@JsonbProperty("document") String id) {
        super(id);
    }

    public TargetDocument(final DocumentId id, final ClientConfiguration config) {
        super(id, config);
    }

    @JsonbProperty("select")
    public List<TargetDocumentSelector> getEntries() {
        return select;
    }

    public TargetDocumentEntry addEntry(final StudentId student, final TermId term, final Grade grade, final Long time) {
        final String t = config.toString(term);
        TargetDocumentSelector tds = select.stream().filter(sel -> sel.getId().equals(t))
                .collect(CollectionUtil.singleOrNull());
        if (tds == null) {
            tds = new TargetDocumentSelector(term, config);
            select.add(tds);
        }
        final TargetDocumentEntry entry = TargetDocumentEntry.createGradeEntry(student, grade, config);
        entry.setTimestamp(time);
        tds.addEntry(entry);
        return entry;
    }

    @JsonbProperty("markers")
    public Marker[] getMarkers() {
        return this.markers;
    }

    @JsonbProperty("markers")
    public void setMarkers(final Marker[] markers) {
        this.markers = markers;
    }

    @JsonbProperty("convention")
    public String getConvention() {
        return convention;
    }

    @JsonbProperty("convention")
    public void setConvention(final String convention) {
        this.convention = convention;
    }

    @JsonbProperty("signees")
    public Map<String, Signee> getMatchingSigneeTypes() {
        return null; //matchingSigneeTypes;
    }

    @JsonbProperty("signees")
    public void setMatchingSigneeTypes(final Map<String, Signee> signees) {
        this.matchingSigneeTypes.clear();
        if (signees != null && !signees.isEmpty()) {
            signees.entrySet().stream()
                    .forEach(e -> {
                        final String sig = config.toString(e.getValue());
                        this.matchingSigneeTypes.put(e.getKey(), sig);
                    });
        }
    }

    @JsonbProperty("target-type")
    public String getTargetType() {
        return targetType;
    }

    @JsonbProperty("target-type")
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @JsonbProperty("expires")
    @JsonbDateFormat("dd.MM.yyyy")
    public ZonedDateTime getExpirationDate() {
        return this.expirationDate != null ? ZonedDateTime.ofInstant(this.expirationDate.toInstant(), ZoneId.systemDefault()) : null;
    }

    @JsonbProperty("expires")
    @JsonbDateFormat("dd.MM.yyyy")
    public void setExpirationDate(final ZonedDateTime expirationDate) {
        final Date d = Date.from(expirationDate.toInstant());
        this.expirationDate = d;
    }

    @JsonbProperty("subject-alt-name")
    public String getSubjectAltName() {
        return subjectAltName;
    }

    @JsonbProperty("subject-alt-name")
    public void setSubjectAltName(String subjectAltName) {
        this.subjectAltName = subjectAltName;
    }

}
