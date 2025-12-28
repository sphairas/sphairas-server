/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 *
 * @author boris.heithecker
 */
public class CommonAppProperties {

    private static final String SYSTEM_PROP_PROVIDER = "providerURL";
    public static final String PROP_SIGNEES_EXTRA_CONVENTIONS_PERMITTED = "signees.extra.conventions.permitted";
//    static final String SYSTEMPROP_AUTHENTICATE_UNKNOWN_TRUSTED_X500PRINCIPALS = "betula.security.x500login.authenticate.unknown.principals";
//    public static final String DO_BACKUP = "backup";
    public static final String INSTANCE_PROPERTIES_FILE = "instance.properties";
    public static final String REPLACE_IF_EQUAL_TIMESTAMP = "assess.entries.replace.equal.timestamp";
//    static final String LOGGER = "org.thespheres.betula.entities";
    public static final String WEB_USE_LINKED_PU_LISTS = "linked.pu.target.lists";
    @Deprecated
    static final String SYSTEM_PROP_SECURE_SIGNEE_TYPES = "secure.signee.types";
    public static final String PROP_CURRENT_TERM = "web.ui.current-term";
//    public static final String COMPRESS_BACKUP_7Z_ADD_SWITCHES = "compress.backup.7z.add.switches";
//    public static final String COMPRESS_TERMGRADE_ENTRIES = "compress.term.grade.target.entries";

    private CommonAppProperties() {
    }

    public static List<String> secureSigneeTypes() {
        final String secSignees = System.getProperty(SYSTEM_PROP_SECURE_SIGNEE_TYPES);
        if (secSignees != null) {
            return Arrays.stream(secSignees.split(",")).map(String::trim).distinct().collect(Collectors.toList());
        } else {
            return Collections.singletonList("entitled.signee");
        }
    }

    public static String ticketsAuthority() {
        return CommonAppProperties.provider();
    }

    public static DirContext lookupAppResourcesContext() {
        try {
            final Context c = new InitialContext();
            return (DirContext) c.lookup("java:global/Betula_Server/Betula_Persistence/AppResourcesContext");
        } catch (NamingException ne) {
            Logger.getLogger(CommonAppProperties.class.getName()).log(Level.SEVERE, "exception caught", ne);
            throw new RuntimeException("Resource AppResourcesContext not found.", ne);
        }
    }

    public static String provider() {
        final String ret = System.getenv("SPHAIRAS_PROVIDER");
        return System.getProperty(SYSTEM_PROP_PROVIDER, ret);
    }
}
