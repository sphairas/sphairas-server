/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.beanimpl;

import java.util.NoSuchElementException;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.auth.login.LoginException;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.config.ConfiguredModelException;
import org.thespheres.betula.security.iservlogin.IservLogin;
import org.thespheres.betula.services.AppPropertyNames;
import org.thespheres.betula.services.LocalProperties;

/**
 *
 * @author boris.heithecker
 */
@PermitAll
@Remote(IservLogin.class)
@EJB(name = "java:global/Betula_Persistence/IservLoginImpl!org.thespheres.betula.security.iservlogin.IservLogin", beanInterface = IservLogin.class)
@Stateless
public class IservLoginImpl implements IservLogin {

    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    @Inject
    private LocalProperties lp;

    @Override
    public String[] getGroups(String prefix, String suffix) {
        Signee signee = new Signee(prefix, suffix, true);
        SigneeEntity se;
        if ((se = em.find(SigneeEntity.class, signee)) != null) {
            return se.getGroups();
        }
        return null;
    }

    @Override
    public String[] getGroups(String ldapName) {
        LdapName dName;
        try {
            dName = new LdapName(ldapName);
        } catch (InvalidNameException ex) {
            LoginException lex = new LoginException();
            lex.initCause(ex);
            return null;
        }
        String cn = null;
        for (Rdn rdn : dName.getRdns()) {
            Attribute attr;
            if ((attr = rdn.toAttributes().get("CN")) != null) {
                try {
                    cn = (String) attr.get();
                } catch (NamingException | NoSuchElementException | ClassCastException ex) {
                }
                break;
            }
        }
        if (cn != null) {
            String[] elements = cn.split("@");
            String suffix = System.getenv(AppPropertyNames.ENV_SIGNEE_SUFFIX);
            if (suffix == null) {//Legacy case
                suffix = lp.getProperty(AppPropertyNames.LP_DEFAULT_SIGNEE_SUFFIX);
            }
            if (suffix == null) {
                throw new ConfiguredModelException(AppPropertyNames.ENV_SIGNEE_SUFFIX);
            }
            if (elements.length == 2 && elements[1].equals(suffix)) {
                Signee signee = new Signee(elements[0], suffix, true);
                SigneeEntity se;
                if ((se = em.find(SigneeEntity.class, signee)) != null) {
                    return se.getGroups();
                }
            }
        }
        return null;
    }

}
