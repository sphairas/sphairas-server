/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import java.io.Serializable;

/**
 *
 * @author boris.heithecker
 */
public interface FileRealmUserBean {

    public User[] users();

    public void updateUser(User user, char[] password) throws IllegalArgumentException;

    public void removeUser(String name);
    
    public String[] groups();

    public static final class User implements Serializable {

        private final String name;
        private final String[] groups;

        public User(String name, String[] roles) {
            this.name = name;
            this.groups = roles;
        }

        public String getName() {
            return name;
        }

        public String[] getGroups() {
            return groups;
        }

    }
}
