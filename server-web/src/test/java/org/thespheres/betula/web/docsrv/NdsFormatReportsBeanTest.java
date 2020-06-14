/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author boris.heithecker
 */
public class NdsFormatReportsBeanTest {

    @Test
    public void testEmptyFoXsl() {
        System.out.println("emptyFoXsl");
        final FopFactory fopFactory = FopFactory.newInstance();
        final TransformerFactory factory = TransformerFactory.newInstance();
        final InputStream is = NdsFormatter.class.getResourceAsStream("empty.fo.xsl");
        final Templates template;
        try {
            template = factory.newTemplates(new StreamSource(is));
        } catch (TransformerConfigurationException ex) {
            fail(ex);
            return;
        }
        final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        foUserAgent.setCreationDate(new Date(0l));
        foUserAgent.setURIResolver(new ResourceResolverAdapter());

        byte[] pdf = createPdf(fopFactory, foUserAgent, template, factory, false);
        System.out.println("Lenght pdf: " + pdf.length);
        assert (pdf.length == 5191);
        byte[] pdf2 = createPdf(fopFactory, foUserAgent, template, factory, true);
        System.out.println("Lenght pdf2: " + pdf2.length);
        assert (pdf2.length == 53016);
    }

    private byte[] createPdf(final FopFactory fopFactory, final FOUserAgent foUserAgent, final Templates template, final TransformerFactory factory, final boolean addBackground) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DOMResult result = new DOMResult();
        try {
            final Fop fop = fopFactory.newFop("application/pdf", foUserAgent, out);
            final Transformer transformer = template.newTransformer();

            if (addBackground) {
                transformer.setParameter("background-image", "url(resource:org/thespheres/betula/web/docsrv/Probedruck.png)");
            }

            transformer.setParameter("versionParam", "2.0");
            final Source src = new DOMSource(result.getNode());
            final DOMResult res = new DOMResult();
            transformer.transform(src, res);

            final Transformer fopTransformer = factory.newTransformer();
            final Source fopSource = new DOMSource(res.getNode());
            final SAXResult finalres = new SAXResult(fop.getDefaultHandler());
            fopTransformer.transform(fopSource, finalres);
        } catch (Exception ex) {
            fail(ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                fail(ex);
            }
        }
        return out.toByteArray();
    }

}
