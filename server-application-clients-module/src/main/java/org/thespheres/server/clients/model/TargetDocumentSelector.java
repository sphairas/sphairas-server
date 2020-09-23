/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.Arrays;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;
import org.thespheres.betula.TermId;
import org.thespheres.server.clients.ClientConfiguration;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class TargetDocumentSelector {

    private final String id;
    private final String type;
    private String label;
    private TargetDocumentEntry[] entries;
    private TargetDocumentSelector[] select;

    @JsonbCreator
    public TargetDocumentSelector(@JsonbProperty("id") final String id, @JsonbProperty("type") final String type) {
        this.id = id;
        this.type = type;
    }

    public TargetDocumentSelector(final TermId term, final ClientConfiguration config) {
        this(config.toString(term), "term");
    }

    @JsonbProperty("id")
    public String getId() {
        return id;
    }

    @JsonbProperty("type")
    public String getType() {
        return type;
    }

    @JsonbProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonbProperty("label")
    public void setLabel(final String label) {
        this.label = label;
    }

    @JsonbProperty("entries")
    public TargetDocumentEntry[] getEntries() {
        return entries;
    }

    public void addEntry(final TargetDocumentEntry entry) {
        if (this.entries == null) {
            this.entries = new TargetDocumentEntry[]{entry};
        } else {
            this.entries = Arrays.copyOf(entries, entries.length + 1);
            this.entries[entries.length - 1] = entry;
        }
    }

    @JsonbProperty("select")
    public TargetDocumentSelector[] getSelectors() {
        return select;
    }

    @JsonbProperty("select")
    public void setSelect(final TargetDocumentSelector[] select) {
        this.select = select;
    }

}
