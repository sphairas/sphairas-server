/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.adminauth;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.SessionScoped;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author boris.heithecker
 */
@SessionScoped
public class AuthAndRedirect extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String redirect = uri.substring("/admins".length());
        if (query != null) {
            redirect += query;
        }
        int pos = redirect.indexOf("/", 1);
        String context = redirect.substring(0, pos);
        redirect = redirect.substring(pos);
        ServletContext ctx = getServletContext();
        RequestDispatcher rd = ctx.getContext(context).getRequestDispatcher(redirect);
        //Logger.getLogger("DEBUG").log(Level.INFO, "User in role unitadmin " + request.isUserInRole("unitadmin"));
        rd.forward(request, resp);
    }

}
