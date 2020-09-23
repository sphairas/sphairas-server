/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.config;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.thespheres.server.clients.ClientConfiguration;
import org.thespheres.server.clients.model.MarkerAdapter;

/**
 *
 * @author boris.heithecker@gmx.net
 */
@Provider
public class JSONConfigurator implements ContextResolver<Jsonb> {

    @Inject
    @SessionScoped
    private ClientConfiguration config;

    @Override
    public Jsonb getContext(Class<?> ignored) {
        final JsonbConfig cfg = new JsonbConfig()
                .withFormatting(true)
                .withAdapters(new JsonbAdapter[]{new MarkerAdapter()});
        return JsonbBuilder.create(cfg);
    }

}
