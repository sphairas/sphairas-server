/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.clientauth;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
