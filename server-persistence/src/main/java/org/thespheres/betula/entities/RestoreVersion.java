/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities;

/**
 *
 * @author boris.heithecker
 */
public interface RestoreVersion {

    public void applyChangeLog(BaseChangeLog bcl);
    
}
