/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

/**
 *
 * @author boris.heithecker
 */
public class ConfigUtil {

    public static final String BACKUP_DIR_PROP = "databases.backup.dir";

    public static String backupDir() {
        //                java.text.SimpleDateFormat now = new java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm");
        final String dir = System.getProperty(BACKUP_DIR_PROP);
        if (dir != null) {
            return dir;
        }
        return System.getProperty("user.home") + "/backup/";
//        String installPath = "/opt/glassfish4/glassfish"; //System.getProperty("com.sun.aas.installRoot");
//        //   /opt/glassfish-4.1/glassfish/db-backup/
////        final String installPath = System.getProperty("com.sun.aas.installRoot");
//
//        return installPath + "/db-backup/";
    }
}
