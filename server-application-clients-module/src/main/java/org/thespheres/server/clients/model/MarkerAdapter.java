/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.model;

import javax.json.bind.adapter.JsonbAdapter;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.MarkerFactory;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public class MarkerAdapter implements JsonbAdapter<Marker, String> {

    @Override
    public String adaptToJson(final Marker m) throws Exception {
        return m.toString();
//        return Arrays.stream(m)
//                .map(Marker::toString)
//                .toArray(String[]::new);
    }

    @Override
    public Marker adaptFromJson(final String json) throws Exception {
        return MarkerFactory.resolve(json);
//        return Arrays.stream(json)
//                .map(ClientConfiguration::parseMarker)
//                .toArray(Marker[]::new);
    }

}
