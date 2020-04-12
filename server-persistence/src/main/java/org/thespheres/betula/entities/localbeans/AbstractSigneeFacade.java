package org.thespheres.betula.entities.localbeans;

import javax.annotation.Resource;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.config.ConfiguredModelException;
import org.thespheres.betula.entities.config.MissingEntityException;
import org.thespheres.betula.services.AppPropertyNames;
import org.thespheres.betula.services.LocalProperties;

/**
 *
 * @author boris.heithecker
 */
public class AbstractSigneeFacade {

    @PersistenceContext(unitName = "betula0")
    protected EntityManager em;
    @Resource
    protected SessionContext context;
    @Inject
    private LocalProperties lp;

    public Signee getSigneePrincipal(final boolean requireSigneeEntity) {
        final Signee signee = currentSignee();
        final SigneeEntity se = em.find(SigneeEntity.class, signee);
        if (se != null) {
            return se.getSignee();
        } else if (requireSigneeEntity) {
            throw new MissingEntityException(signee);
        }
        return signee;
    }

    public SigneeEntity getCurrent() {
        final Signee signee = currentSignee();
        return em.find(SigneeEntity.class, signee);
    }

    private Signee currentSignee() throws ConfiguredModelException, IllegalStateException, EJBAccessException {
        if (!context.isCallerInRole("signee")) {
            throw new EJBAccessException();
        }
        final String prefix = context.getCallerPrincipal().getName();
            String suffix = System.getenv(AppPropertyNames.ENV_SIGNEE_SUFFIX);
            if (suffix == null) {//Legacy case
                suffix = lp.getProperty(AppPropertyNames.LP_DEFAULT_SIGNEE_SUFFIX);
            }
            if (suffix == null) {
                throw new ConfiguredModelException(AppPropertyNames.ENV_SIGNEE_SUFFIX);
            }
        return new Signee(prefix, suffix, true);
    }

    public String getSigneeCommonName(@NotNull Signee signee) {
        final SigneeEntity se = em.find(SigneeEntity.class, signee);
        return se != null ? se.getCommonName() : null;
    }

}
