/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.util.Date;
import javax.ejb.Local;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;

/**
 *
 * @author boris.heithecker
 */
@Local
public interface CalendarsBean {

    public Date getDate(String category, UnitId unit, TermId termId, DocumentId zgn, String[] cat) throws AmbiguousDateException;

}
