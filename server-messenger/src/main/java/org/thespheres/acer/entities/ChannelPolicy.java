/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.util.Properties;

/**
 *
 * @author boris.heithecker
 */
public abstract class ChannelPolicy {

    protected ChannelPolicy() {
    }

    public abstract void init(Properties prop);
}
