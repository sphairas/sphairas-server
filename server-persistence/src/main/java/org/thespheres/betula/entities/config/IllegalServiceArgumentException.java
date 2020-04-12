/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import javax.ejb.ApplicationException;

/**
 *
 * @author boris.heithecker
 */
@ApplicationException(rollback = true)
public class IllegalServiceArgumentException extends IllegalArgumentException {

    public IllegalServiceArgumentException() {
    }

    public IllegalServiceArgumentException(String s) {
        super(s);
    }

    public IllegalServiceArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalServiceArgumentException(Throwable cause) {
        super(cause);
    }

}
