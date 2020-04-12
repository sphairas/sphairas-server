/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
@XmlSchema(namespace = "urn:ietf:params:xml:ns:carddav",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
            @XmlNs(prefix = "C", namespaceURI = "urn:ietf:params:xml:ns:carddav"),
            @XmlNs(prefix = "D", namespaceURI = "DAV:")
        })
package org.thespheres.betula.calendar.students;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
