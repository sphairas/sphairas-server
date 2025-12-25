/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.adminauth;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.SessionScoped;
import java.io.IOException;
//import jakarta.faces.bean.SessionScoped;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author boris.heithecker
 */
@Dependent //Vor Jakarta: javax.faces.bean.SessionScoped
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
