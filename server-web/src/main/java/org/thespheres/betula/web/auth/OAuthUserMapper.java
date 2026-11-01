package org.thespheres.betula.web.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.services.AppPropertyNames;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.web.config.AppConfiguration;

/**
 * Maps the "sub" claim of the (single, dedicated) IdP to a signee by matching
 * it with the username (UNAME) of the signee entity. Groups are resolved by the
 * server; the IdP supplies none.
 */
@ApplicationScoped
public class OAuthUserMapper {

    @Inject
    private OpenIdContext openId;
    @Inject
    private AppConfiguration config;
    @Inject
    private LocalProperties lp;

    /**
     * @return a valid result for the signee, or INVALID_RESULT if the identity has no matching user.
     */
    public CredentialValidationResult map() {
        final String sub = openId.getSubject();
        if (StringUtils.isBlank(sub)) {
            Logger.getLogger(OAuthUserMapper.class.getName()).log(Level.WARNING, "OAuth login denied: sub claim missing.");
            return CredentialValidationResult.INVALID_RESULT;
        }
        final String prefix = config.getInternalClient().getSigneeByUsername(sub);
        if (StringUtils.isBlank(prefix)) {
            Logger.getLogger(OAuthUserMapper.class.getName()).log(Level.WARNING, "OAuth login denied: no signee for sub={0}.", sub);
            return CredentialValidationResult.INVALID_RESULT;
        }
        String suffix = System.getenv(AppPropertyNames.ENV_SIGNEE_SUFFIX);
        if (suffix == null) {
            suffix = lp.getProperty(AppPropertyNames.LP_DEFAULT_SIGNEE_SUFFIX);
        }
        if (suffix == null) {
            Logger.getLogger(OAuthUserMapper.class.getName()).log(Level.SEVERE, "OAuth login denied: signee suffix not configured.");
            return CredentialValidationResult.INVALID_RESULT;
        }
        final String grp = config.getInternalClient().getSigneeGroups(new Signee(prefix, suffix, true));
        final Set<String> groups = StringUtils.isBlank(grp) ? Set.of()
                : Stream.of(StringUtils.split(grp, ',')).map(String::trim).filter(g -> !g.isEmpty()).collect(Collectors.toSet());
        if (groups.isEmpty()) {
            Logger.getLogger(OAuthUserMapper.class.getName()).log(Level.WARNING, "OAuth login denied: signee {0} has no groups.", prefix);
            return CredentialValidationResult.INVALID_RESULT;
        }
        return new CredentialValidationResult(prefix, groups);
    }
}
