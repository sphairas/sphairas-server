/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.Template;
import org.thespheres.betula.document.util.ContentValueEntry;
import org.thespheres.betula.entities.EmbeddableMarker;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.TermGradeTargetAssessmentEntity;
import org.thespheres.betula.entities.facade.GradeTargetDocumentFacade;
import org.thespheres.betula.entities.facade.SigneeFacade;
import org.thespheres.betula.entities.service.ServiceUtils;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class AdminSigneesProcessor extends AbstractAdminContainerProcessor {

    @EJB
    private SigneeFacade signees;
    @EJB
    private GradeTargetDocumentFacade targets;
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;

    public AdminSigneesProcessor() {
        super(new String[][]{Paths.SIGNEES_PATH, Paths.SIGNEES_TARGETS_PATH});
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.SIGNEES_PATH)) {
            processTargets(template);
        } else if (Arrays.equals(path, Paths.SIGNEES_TARGETS_PATH)) {
            processSigneesTargets(template);
        }
    }

    private void processSigneesTargets(final Envelope node) throws SyntaxException, NotFoundException {
        if (node.getAction() != null && node.getAction().equals(Action.REQUEST_COMPLETION)) {
            final Signee signee = ServiceUtils.toEntry(node, Signee.class).getIdentity();
            final SigneeEntity se;
            if (signee == null || (se = signees.find(signee)) == null) {
                throw ServiceUtils.createNotFoundException(signee);
            }
            final List<TermGradeTargetAssessmentEntity> l = targets.findAll(se, TermGradeTargetAssessmentEntity.class, LockModeType.OPTIMISTIC);
            node.setAction(Action.RETURN_COMPLETION);
            node.getChildren().clear();
            l.stream()
                    .map(te -> new Entry(null, te.getDocumentId()))
                    .forEach(node.getChildren()::add);
        }
    }

    private void processTargets(final Envelope node) throws SyntaxException {
        Action a = node.getAction();
        if (a != null) {
            if (a.equals(Action.FILE)) {
                for (Template<?> ch : node.getChildren()) {
                    final ContentValueEntry<Signee> entry = ServiceUtils.toEntryType(ch, ContentValueEntry.class);
                    final Signee signee = entry.getIdentity();
                    final String name = entry.getStringValue();
                    final Set<Marker> markers = entry.getValue().getMarkerSet();
                    updateSignee(signee, name, markers);
                }
            } else if (a.equals(Action.REQUEST_COMPLETION)) {
                final CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
                cq.select(cq.from(SigneeEntity.class));
                final List<SigneeEntity> l = em.createQuery(cq).getResultList();
                node.getChildren().clear();
                node.setAction(Action.RETURN_COMPLETION);
                l.stream().map(this::createEntry)
                        .forEach(node.getChildren()::add);
            }
        }
    }

    private Entry createEntry(SigneeEntity se) {
        final ContentValueEntry<Signee> entry = ContentValueEntry.create(se.getSignee(), Signee.class, null, se.getCommonName());
        final Set<Marker> markers = entry.getValue().getMarkerSet();
        se.getMarkerSet().stream()
                .map(EmbeddableMarker::getMarker)
                .forEach(m -> markers.add(m));
        return entry;
    }

    private void updateSignee(final Signee signee, final String name, final Set<Marker> markers) {
        SigneeEntity se;
        if ((se = em.find(SigneeEntity.class, signee)) == null) {
            se = new SigneeEntity(signee, name);
            em.persist(se);
        }
        if (name != null && !se.getCommonName().equals(name)) {
            se.setCommonName(name);
        }
        final Set<EmbeddableMarker> ms = se.getMarkerSet();
        ms.clear();
        if (markers != null) {
            markers.stream()
                    .map(EmbeddableMarker::new)
                    .forEach(ms::add);
        }
        em.merge(se);
    }
}
