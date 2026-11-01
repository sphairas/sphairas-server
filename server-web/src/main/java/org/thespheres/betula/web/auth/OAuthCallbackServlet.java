package org.thespheres.betula.web.auth;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Target of the OAuth2 callback after {@link AuthHandler} has authenticated the
 * user: the request continues to the container, so something must answer it.
 * Unauthenticated callbacks never get here (AuthHandler redirects them to the
 * login page, and the security constraint applies to this path).
 */
@WebServlet(name = "OAuthCallback", urlPatterns = "/oauth2/callback")
public class OAuthCallbackServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/ui/index.xhtml");
    }
}
