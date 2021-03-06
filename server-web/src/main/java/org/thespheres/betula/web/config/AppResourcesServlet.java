package org.thespheres.betula.web.config;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import org.apache.catalina.servlets.WebdavServlet;
import org.apache.naming.resources.ProxyDirContext;

/**
 *
 * @author boris.heithecker
 */
//https://github.com/payara/Payara/issues/4695
public class AppResourcesServlet extends FixedWebdavServlet { //extends org.apache.catalina.servlets.WebdavServlet (once the issue is fixed

    @Override
    public void init() throws ServletException {
        super.init();
        resources = lookupProxyDirContext();
//        ProxyDirContext lkp = lookupProxyDirContext();
    }

    private ProxyDirContext lookupProxyDirContext() throws ServletException {
        try {
            Context c = new InitialContext();
            return (ProxyDirContext) c.lookup("java:global/Betula_Server/Betula_Persistence/AppResourcesContext");
        } catch (NamingException ne) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
            throw new ServletException(ne);
        }
    }
}
