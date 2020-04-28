/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.beanimpl;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.security.iservlogin.IservLogin;

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

    @Override
    public String[] getGroups(String prefix, String suffix) {
        Signee signee = new Signee(prefix, suffix, true);
        SigneeEntity se;
        if ((se = em.find(SigneeEntity.class, signee)) != null) {
            return se.getGroups();
        }
        return null;
    }

}
