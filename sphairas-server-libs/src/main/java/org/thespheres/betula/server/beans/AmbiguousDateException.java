/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.util.Date;

/**
 *
 * @author boris.heithecker
 */
public class AmbiguousDateException extends AmbiguousResultException {

    private final Date[] dates;

    public AmbiguousDateException(final Date[] dates) {
        this.dates = dates;
    }

}
