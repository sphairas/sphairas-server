/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author boris.heithecker
 */
public class ResourceResolverAdapter implements URIResolver {

    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        final URI uri = URI.create(href);
        if ("resource".equals(uri.getScheme())) {
            final String res = uri.getSchemeSpecificPart();
            final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
            if (is != null) {
                return new StreamSource(is);
            } else {
                Logger.getLogger(ResourceResolverAdapter.class.getCanonicalName()).log(Level.WARNING, "Could not resolve {0}", href);
            }
        }
        return null;
    }

}
