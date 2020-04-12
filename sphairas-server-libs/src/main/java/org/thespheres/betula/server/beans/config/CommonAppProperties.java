/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.naming.resources.ProxyDirContext;

/**
 *
 * @author boris.heithecker
 */
public class CommonAppProperties {

    private static final String SYSTEM_PROP_PROVIDER = "providerURL";

    private CommonAppProperties() {
    }

    public static ProxyDirContext lookupAppResourcesContext() {
        try {
            final Context c = new InitialContext();
            return (ProxyDirContext) c.lookup("java:global/Betula_Server/Betula_Persistence/AppResourcesContext");
        } catch (NamingException ne) {
            Logger.getLogger(CommonAppProperties.class.getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException(ne);
        }
    }

    public static String provider() {
        final String ret = System.getenv("SPHAIRAS_PROVIDER");
        return System.getProperty(SYSTEM_PROP_PROVIDER, ret);
    }
}
