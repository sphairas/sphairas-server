/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.clientauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.security.Key;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;

/**
 *
 * @author boris
 */
@ApplicationScoped
public class JWTAuthentication {

    private static final String GROUPS_KEY = "groups";
    static final String SECRET_KEY = "aqrIWZisQINqTRB4EOE7OylxHykqeLaKw7NkO4fXOq4=";
    static final String ISSUER = "GenJWT";
    private Key key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    public AuthenticationStatus validateToken(final String jwt, final HttpMessageContext context) {
        try {
            final Claims claims = Jwts.parserBuilder()
                    //                    .requireIssuer(authToken)
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();
//            String[] groups = claims
//                    .get(GROUPS_KEY, String.class)
//                    .split(",");
            final String[] groups = new String[]{"signees"};
            final JWTCallerPrincipal principal = new JWTCallerPrincipal(claims.getSubject(), jwt);
            return context.notifyContainerAboutLogin(principal, Arrays.stream(groups).collect(Collectors.toSet()));
        } catch (final SignatureException e) {
//            LOGGER.log(Level.INFO, "Invalid JWT signature: {0}", e.getMessage());       
        } catch (final ExpiredJwtException e) {

        } catch (final JwtException e) {

        }
        return context.responseUnauthorized();
    }
}
