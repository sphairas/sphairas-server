/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.clientauth;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.security.enterprise.credential.Credential;

/**
 *
 * @author boris.heithecker
 */
class JWTCredential implements Credential {

    private final String principal;
    private final String[] groups;

    JWTCredential(final String principal, final String[] groups) {
        this.principal = principal;
        this.groups = groups;
    }

    public String getPrincipal() {
        return principal;
    }

    public Set<String> getGroups() {
        return Arrays.stream(groups).collect(Collectors.toSet());
    }

}
