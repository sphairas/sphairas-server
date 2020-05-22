/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.clientauth;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author boris.heithecker
 */
@WebServlet(name = "AuthAndRedirect", urlPatterns = {"/web/*", "/calendar/*"})
@ServletSecurity(
        @HttpConstraint(rolesAllowed = {"signee"}))
public class AuthAndRedirect extends HttpServlet {

    static final Logger LOGGER = Logger.getLogger("server-client-authentication");

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse resp) throws ServletException, IOException {
        final String uri = request.getRequestURI();
        final String query = request.getQueryString();
        final String redirect = uri.substring("/clients".length()) + (query != null ? query : "");
        final int pos = redirect.indexOf("/", 1);
        final String context = redirect.substring(0, pos);
        final String path = redirect.substring(pos);
        final RequestDispatcher rd = getServletContext().getContext(context).getRequestDispatcher(path);
        LOGGER.log(Level.FINE, "Redirecting {0} to {1}", new String[]{request.getUserPrincipal().getName(), redirect});
        rd.forward(request, resp);
    }

}
