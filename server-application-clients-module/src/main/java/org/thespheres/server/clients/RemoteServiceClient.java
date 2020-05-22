/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import org.thespheres.betula.services.ws.BetulaWebService;
import org.thespheres.betula.services.ws.api.BetulaServiceClient;

/**
 *
 * @author boris
 */
@WebServiceClient
public class RemoteServiceClient extends Service {

    private final static QName BETULASERVICE_QNAME = new QName("http://web.service.betula.thespheres.org/", "BetulaService");

    public RemoteServiceClient() {
        super(getWsdlLoacation(), BETULASERVICE_QNAME);
    }

    public BetulaWebService getBetulaServicePort(final String endpointAddress) {
        final BetulaWebService port = getPort(new QName("http://web.service.betula.thespheres.org/", "BetulaServicePort"), BetulaWebService.class);
        configureBinding((BindingProvider) port, endpointAddress);
        return port;
    }

    private void configureBinding(final BindingProvider port, final String endpoint) {
        port.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        port.getRequestContext().put("com.sun.xml.ws.request.timeout", 300000);
        port.getRequestContext().put("com.sun.xml.ws.connect.timeout", 3000);
        port.getRequestContext().put("ocm.sun.xml.internal.ws.request.timeout", 300000);
        port.getRequestContext().put("com.sun.xml.internal.ws.connect.timeout", 3000);
    }

    private static URL getWsdlLoacation() {
        return BetulaServiceClient.class.getResource("/META-INF/wsit-client.xml");
    }
}
