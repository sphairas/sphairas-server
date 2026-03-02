package org.thespheres.betula.web.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import org.glassfish.soteria.identitystores.LdapIdentityStore;

/**
 *
 * @author boris
 */
@ApplicationScoped
public class LdapEnvIdentityStore implements IdentityStore {

    private LdapIdentityStore delegate = null;

    @PostConstruct
    public void produceLdapConfig() {
        final LdapIdentityStoreDefinition definition = (LdapIdentityStoreDefinition) Proxy.newProxyInstance(LdapIdentityStoreDefinition.class.getClassLoader(),
                new Class[]{LdapIdentityStoreDefinition.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                case "url" ->
                    System.getenv("LDAP_URL");
                case "callerBaseDn" ->
                    System.getenv("LDAP_CALLER_BASE_DN");
                case "bindDn" ->
                    System.getenv("LDAP_BIND_USER");
                case "bindDnPassword" ->
                    System.getenv("LDAP_PASSWORD");
                case "groupSearchFilter" ->
                    "(&(objectClass=groupOfNames)(member=%s))";
                case "groupSearchBase" ->
                    System.getenv("LDAP_CALLER_BASE_DN");
                case "groupMemberOfAttribute" ->
                    "";
                case "priority" ->
                    10;
                case "toString" ->
                    "DynamicLdapConfig";
                case "annotationType" ->
                    LdapIdentityStoreDefinition.class;
                default ->
                    method.getDefaultValue();
            };
                }
        );
        delegate = new LdapIdentityStore(definition);
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (delegate != null) {
            CredentialValidationResult rer = delegate.validate(credential);
            return rer;
        } else {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        if (delegate != null) {
            Set<String> ret = delegate.getCallerGroups(validationResult);
            return ret;
        } else {
            return Collections.EMPTY_SET;
        }
    }

}
