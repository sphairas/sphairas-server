/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.dbadmin;

import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.file.FileRealmUser;
import com.sun.enterprise.security.util.IASSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import org.thespheres.betula.entities.service.dbadmin.FileRealmUserBean.User;

/**
 *
 * @author martin
 */
//@RolesAllowed("remoteadmin")
//@Remote(FileRealmUserBean.class)
//@EJB(name = "java:global/Betula_Persistence/FileRealmUserBeanImpl!org.thespheres.betula.beans.FileRealmUserBean", beanInterface = FileRealmUserBean.class)
@RolesAllowed({"superadmin"})
@Stateless
public class FileRealmUserBeanImpl {

    @Resource
    private SessionContext context;

    public String[] groups() {
        final FileRealm fr;
        try {
            fr = (FileRealm) Realm.getInstance("file");
        } catch (NoSuchRealmException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        FileRealmUser u;
        try {
            u = (FileRealmUser) fr.getUser((context.getCallerPrincipal().getName()));
        } catch (NoSuchUserException | ClassCastException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new EJBAccessException();
        }
        return u.getGroups();
    }

    @RolesAllowed("superadmin")
    public void updateUser(User u, char[] password) throws IllegalArgumentException {
        final FileRealm fr;
        try {
            fr = (FileRealm) Realm.getInstance("file");
        } catch (NoSuchRealmException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        checkProvidedPassword(password);
        boolean exists = false;
        Enumeration<String> users;
        try {
            users = fr.getUserNames();
        } catch (BadRealmException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        while (users.hasMoreElements()) {
            if (u.getName().equals(users.nextElement())) {
                exists = true;
                break;
            }
        }
        if (exists) {
            try {
                fr.updateUser(u.getName(), u.getName(), password, u.getGroups());
                fr.persist();
            } catch (NoSuchUserException | IASSecurityException | BadRealmException ex) {
                Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException(ex);
            }
        } else {
            try {
                fr.addUser(u.getName(), password, u.getGroups());
                fr.persist();
            } catch (IASSecurityException | BadRealmException ex) {
                Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException(ex);
            }
        }

    }

    static void checkProvidedPassword(char[] password) throws IllegalArgumentException {
        if (password != null && password.length > 5) {
            return;
        }
        throw new IllegalArgumentException();
    }

    @RolesAllowed("superadmin")
    public void removeUser(String name) {
        final FileRealm fr;
        try {
            fr = (FileRealm) Realm.getInstance("file");
        } catch (NoSuchRealmException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        boolean exists = false;
        Enumeration<String> users;
        try {
            users = fr.getUserNames();
        } catch (BadRealmException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        while (users.hasMoreElements()) {
            if (name.equals(users.nextElement())) {
                exists = true;
                break;
            }
        }
        if (exists) {
            try {
                fr.removeUser(name);
                fr.persist();
            } catch (NoSuchUserException | BadRealmException ex) {
                Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException(ex);
            }
        }

    }

    @RolesAllowed("superadmin")
    public User[] users() {
        final FileRealm fr;
        try {
            fr = (FileRealm) Realm.getInstance("file");
        } catch (NoSuchRealmException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        List<String> users;
        try {
            users = Collections.list(fr.getUserNames());
        } catch (BadRealmException ex) {
            Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        final List<User> ret = new ArrayList<>();
        for (String u : users) {
            try {
                ArrayList grl = Collections.list(fr.getGroupNames(u));
                String[] gr = new String[grl.size()];
                for (int i = 0; i < grl.size(); i++) {
                    gr[i] = (String) grl.get(i);
                }
                ret.add(new User(u, gr));
            } catch (NoSuchUserException ex) {
                Logger.getLogger(FileRealmUserBeanImpl.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException(ex);
            }
        }
        return ret.toArray(new User[ret.size()]);
    }
}
