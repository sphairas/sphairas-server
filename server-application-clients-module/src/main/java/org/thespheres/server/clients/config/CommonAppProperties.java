/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.server.clients.config;

/**
 *
 * @author boris.heithecker
 */
@Deprecated //User server-libs
public class CommonAppProperties {
    
    public static final String INSTANCE_PROPERTIES_FILE = "instance.properties";
    private static final String SYSTEM_PROP_PROVIDER = "providerURL";
    
    private CommonAppProperties() {
    }

    public static String provider() {
        final String ret = System.getenv("SPHAIRAS_PROVIDER");
        return System.getProperty(SYSTEM_PROP_PROVIDER, ret);
    }
}
