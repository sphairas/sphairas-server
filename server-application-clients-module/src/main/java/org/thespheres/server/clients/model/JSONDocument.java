/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyOrderStrategy;

/**
 *
 * @author boris.heithecker
 */
@JsonbPropertyOrder(PropertyOrderStrategy.ANY)
public class JSONDocument {

    @JsonbProperty("authority")
    final private String authority;
    @JsonbProperty("id")
    final private String id;
    @JsonbProperty("version")
    final private String version;
    @JsonbProperty("label")
    final private String label;

    public JSONDocument(@JsonbProperty("authority") final String authority, @JsonbProperty("id") final String id, @JsonbProperty("version") final String version, @JsonbProperty("label") final String label) {
        this.authority = authority;
        this.id = id;
        this.version = version;
        this.label = label;
    }

//    public JSONDocument(final DocumentId tid, final String label) {
//        this.authority = tid.getAuthority();
//        this.id = tid.getId();
//        this.version = tid.getVersion().getVersion();
//        this.label = label;
//    }

//    public static JSONDocumentId create(final DocumentId did, final Configuration... configs) {
//        final boolean setAuthority = configs != null && !Arrays.stream(configs)
//                .map(c -> c.getProperties())
//                .flatMap(l -> l.stream())
//                .filter(p -> "authority".equals(p.getName()))
//                .collect(CollectionUtil.requireSingleton())
//                .filter(p -> p.getValue().equals(did.getAuthority()))
//                .isPresent();
//    }

    public String getAuthority() {
        return authority;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getLabel() {
        return label;
    }

}
