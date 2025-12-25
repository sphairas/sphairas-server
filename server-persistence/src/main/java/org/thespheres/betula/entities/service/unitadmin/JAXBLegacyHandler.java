package org.thespheres.betula.entities.service.unitadmin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openide.util.Exceptions;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.services.ws.SolicitExt;
import org.thespheres.betula.services.ws.SolicitResponseExt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author boris
 */
public class JAXBLegacyHandler implements SOAPHandler<SOAPMessageContext> {

    static final String CONTAINER_NS = "http://www.thespheres.org/xsd/betula/container.xsd";
    static final String WEB_NS = "http://web.service.betula.thespheres.org/";
    static final String ATTR_CONTAINER_GET = "CONTAINER_GET";
    static final String ATTR_CONTAINER_RETURN = "CONTAINER_RETURN";

    static JAXBContext jaxb;

    static {
        try {//Use init method
            jaxb = JAXBContext.newInstance(SolicitExt.class, SolicitResponseExt.class, Container.class);
        } catch (JAXBException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        boolean isOutbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (!isOutbound) {
            try {
                final HttpServletRequest req = (HttpServletRequest) context.get("jakarta.xml.ws.servlet.request");
                final SOAPBody soapBody = context.getMessage().getSOAPBody();
                putContainInRequest(req, soapBody);
//                req.setAttribute("RAW_BODY_ATTR", soapBody);
//            context.put("RAW_BODY", context.getMessage().getSOAPBody());
            } catch (Exception ex) {
                Logger.getLogger("betula-admin-service").log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else {
            try {
                final HttpServletRequest req = (HttpServletRequest) context.get("jakarta.xml.ws.servlet.request");
                final SOAPBody soapBody = context.getMessage().getSOAPBody();
                final Container container = (Container) req.getAttribute(ATTR_CONTAINER_RETURN);
                replaceContainerInSoap(soapBody, container);
                context.getMessage().saveChanges();
            } catch (Exception ex) {
                Logger.getLogger("betula-admin-service").log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return true;
    }

    private void putContainInRequest(final HttpServletRequest req, final SOAPBody body) throws Exception {
        NodeList nodes = body.getElementsByTagNameNS(JAXBLegacyHandler.CONTAINER_NS, "container");
        if (nodes.getLength() == 0) {
            throw new RuntimeException("Kein <container> Element gefunden!");
        }
        Node containerNode = nodes.item(0);
        Unmarshaller unmarshaller = jaxb.createUnmarshaller();
        JAXBElement<Container> jaxbElement = unmarshaller.unmarshal(containerNode, Container.class);
        req.setAttribute(ATTR_CONTAINER_GET, jaxbElement.getValue());
    }

    private void replaceContainerInSoap(SOAPBody soapDoc, Container container) throws Exception {
//        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        Transformer transformer = transformerFactory.newTransformer();
//        StringWriter stringWriter = new StringWriter();
//        transformer.transform(new DOMSource(soapDoc), new StreamResult(stringWriter));
//        String result = stringWriter.toString();
//        System.out.println(result);

        SolicitResponseExt sre = new SolicitResponseExt();
        sre.setReturn(container);

        NodeList nodes = soapDoc.getElementsByTagNameNS(WEB_NS, "solicitResponse");
        if (nodes.getLength() == 0) {
            throw new RuntimeException("Original <solicitResponse> nicht gefunden!");
        }
        Node oldNode = nodes.item(0);
        Node parent = oldNode.getParentNode(); // Das ist meistens <ns6:solicit>

        Marshaller marshaller = jaxb.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // XML-Header weglassen

        Document tempDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        marshaller.marshal(sre, tempDoc);
        Element newNodeTemp = tempDoc.getDocumentElement();

        Node replace = soapDoc.getOwnerDocument().importNode(newNodeTemp, true);
        parent.replaceChild(replace, oldNode);

//        transformer = transformerFactory.newTransformer();
//        stringWriter = new StringWriter();
//        transformer.transform(new DOMSource(soapDoc), new StreamResult(stringWriter));
//        result = stringWriter.toString();
//        System.out.println("RESTULT: " + result);
    }

    @Override
    public boolean handleFault(SOAPMessageContext c) {
        return true;
    }

    @Override
    public void close(MessageContext mc) {
    }

}
