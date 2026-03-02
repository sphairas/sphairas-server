package org.thespheres.betula.web.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import java.util.Set;

/**
 *
 * @author boris.heithecker@gmx.net
 */
//@ApplicationScoped
public class IServIMAPIdentityStore implements IdentityStore {

    @PostConstruct
    public void init() {
        System.out.println("test");
    }

    public CredentialValidationResult validate(UsernamePasswordCredential credential) {
        return new CredentialValidationResult("test", Set.of("signees"));
    }

//    @Override
//    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
//        return IdentityStore.super.getCallerGroups(validationResult); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
//    }
    @Override
    public int priority() {
        return 1000;
    }

}
