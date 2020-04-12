/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.config;

import org.thespheres.betula.server.beans.config.CommonAppProperties;

/**
 *
 * @author boris.heithecker
 */
public class SystemProperties {

    public static String messagesAuthority() {
        return CommonAppProperties.provider();
    }
}
