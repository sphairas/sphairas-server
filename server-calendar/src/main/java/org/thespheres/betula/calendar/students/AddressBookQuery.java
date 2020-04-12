/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.students;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.thespheres.betula.services.dav.AllProp;
import org.thespheres.betula.services.dav.DAVProp;
import org.thespheres.betula.services.dav.Propname;

/**
 * <!ELEMENT addressbook-query ((DAV:allprop |
 * DAV:propname |
 * DAV:prop)?, filter, limit?)> @author boris.heithecker
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"response", "responsedescription"})
@XmlRootElement(name = "multistatus")
public class AddressBookQuery {

    @XmlElement
    protected DAVProp prop;
    @XmlElement
    protected AllProp allprop;
    @XmlElement
    protected Propname propname;

    @XmlElement(name = "filter", required = true)
    protected Filter filter = new Filter();
}
