/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.thespheres.betula.server.beans.config.CommonAppProperties;

/**
 *
 * @author boris.heithecker
 */
public class AppProperties {

//    @Deprecated
//    public static final String PROP_CURRENT_TERM = "betula.web.ui.current-term";
    public static final String PROP_CURRENT_TERM = "web.ui.current-term";
    public static final String REPLACE_IF_EQUAL_TIMESTAMP = "assess.entries.replace.equal.timestamp";
    static final String SYSTEMPROP_AUTHENTICATE_UNKNOWN_TRUSTED_X500PRINCIPALS = "betula.security.x500login.authenticate.unknown.principals";
//    @Deprecated
//    public static final String DEFAULT_AUTHORITY = "betula.system.authority";
//    @Deprecated
//    public static final String DEFAULT_SIGNEE_SUFFIX = "betula.system.signee.suffix";
    public static final String COMPRESS_BACKUP_7Z_ADD_SWITCHES = "compress.backup.7z.add.switches";
    @Deprecated
    public static final String PERMIT_CONVENTIONS = "permit.conventions";
    static final String LOGGER = "org.thespheres.betula.entities";
    public static final String INSTANCE_PROPERTIES_FILE = "instance.properties";
    public static final String COMPRESS_TERMGRADE_ENTRIES = "compress.term.grade.target.entries";
    public static final String WEB_USE_LINKED_PU_LISTS = "linked.pu.target.lists";
    @Deprecated
    static final String SYSTEM_PROP_SECURE_SIGNEE_TYPES = "secure.signee.types";
    public static final String DO_BACKUP = "backup";
//    @Deprecated
//    private static final String DEFAULT_TICKETS_AUTHORITY = "betula.system.authority";

    private AppProperties() {
    }

    public static List<String> secureSigneeTypes() {
        final String secSignees = System.getProperty(SYSTEM_PROP_SECURE_SIGNEE_TYPES);
        if (secSignees != null) {
            return Arrays.stream(secSignees.split(",")).map(String::trim).distinct().collect(Collectors.toList());
        } else {
            return Collections.singletonList("entitled.signee");
        }
    }

    public static List<String> permittedConventions() {
        final String cv = System.getProperty(PERMIT_CONVENTIONS);
        if (cv != null) {
            return Arrays.stream(cv.split(",")).map(String::trim).distinct().collect(Collectors.toList());
        } else {
            return Collections.singletonList("niedersachsen.avsvvorschlag");
        }
    }

    public static String ticketsAuthority() {
        return CommonAppProperties.provider();
    }
}
