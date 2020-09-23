/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.server.clients.ClientConfiguration;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class TargetDocumentEntry extends TargetDocumentSelector {

    private final String grade;
    private Long timestamp;
    private DocTicketEntry[] tickets;

    @JsonbCreator
    public TargetDocumentEntry(@JsonbProperty("student") final String student, @JsonbProperty("type") final String type, @JsonbProperty("value") final String grade) {
        super(student, type);
        this.grade = grade;
    }

    public static TargetDocumentEntry createGradeEntry(final StudentId student, final Grade grade, final ClientConfiguration config) {
        final String s = config.toString(student);
        final String g = config.toString(grade);
        return new TargetDocumentEntry(s, "student-grade", g);
    }

    @JsonbProperty("value")
    public String getValue() {
        return grade;
    }

    @JsonbProperty("tickets")
    public DocTicketEntry[] getTickets() {
        return tickets;
    }

    @JsonbProperty("tickets")
    public void setTickets(DocTicketEntry[] tickets) {
        this.tickets = tickets;
    }

    @JsonbProperty("timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

    @JsonbProperty("timestamp")
    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonbPropertyOrder(PropertyOrderStrategy.ANY)
    public static class DocTicketEntry {

        private final String id;
        private String scope;

        @JsonbCreator
        public DocTicketEntry(@JsonbProperty("id") final String id) {
            this.id = id;
        }

        public DocTicketEntry(final String id, final String scope) {
            this(id);
            this.scope = scope;
        }

        @JsonbProperty("id")
        public String getId() {
            return id;
        }

        @JsonbProperty("scope")
        public String getScope() {
            return scope;
        }

        @JsonbProperty("scope")
        public void setScope(String scope) {
            this.scope = scope;
        }

    }

}
