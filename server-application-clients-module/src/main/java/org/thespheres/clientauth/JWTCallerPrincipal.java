/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.clientauth;

import javax.security.enterprise.CallerPrincipal;

/**
 *
 * @author boris.heithecker@gmx.net
 */
public class JWTCallerPrincipal extends CallerPrincipal {

    private final String token;

    public JWTCallerPrincipal(final String name, final String jwt) {
        super(name);
        this.token = jwt;
    }
    
    public String getToken() {
        return this.token;
    }
}
