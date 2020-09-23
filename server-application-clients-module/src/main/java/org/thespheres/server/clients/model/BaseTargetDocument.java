/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import java.util.Map;
import java.util.Set;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.server.clients.ClientConfiguration;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public class BaseTargetDocument extends BaseDocument {

    private String[] units;
    private BaseTargetDocument[] targets;

    @JsonbCreator
    public BaseTargetDocument(@JsonbProperty("document") String id) {
        super(id);
    }

    public BaseTargetDocument(final DocumentId target, final Set<UnitId> units, final ClientConfiguration config) {
        super(target, config);
        if (units != null && !units.isEmpty()) {
            this.units = units.stream()
                    .map(this.config::toString)
                    .toArray(String[]::new);
        }
    }

    public BaseTargetDocument(final DocumentId target, final Map<DocumentId, Set<UnitId>> map, final ClientConfiguration config) {
        super(target, config);
        if (map != null && !map.isEmpty()) {
            this.targets = map.entrySet().stream()
                    .map(e -> new BaseTargetDocument(e.getKey(), e.getValue(), config))
                    .toArray(BaseTargetDocument[]::new);//TODO include 
        }
    }

    @JsonbProperty("units")
    public String[] getUnits() {
        return units;
    }

    @JsonbProperty("targets")
    public BaseTargetDocument[] getTargets() {
        return targets;
    }

}
