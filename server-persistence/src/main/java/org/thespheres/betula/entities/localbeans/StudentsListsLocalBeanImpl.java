/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.localbeans;

import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.entities.StudentMarkerMapDocumentEntity;
import org.thespheres.betula.entities.StudentStringMapDocumentEntity;
import org.thespheres.betula.entities.facade.UnitDocumentFacade;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class StudentsListsLocalBeanImpl implements StudentsListsLocalBean {

    @EJB
    private UnitDocumentFacade unitDocumentFacadeImpl;
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;

    private void ensureDocument(DocumentId document) {
        StudentMarkerMapDocumentEntity sude = em.find(StudentMarkerMapDocumentEntity.class, document, LockModeType.OPTIMISTIC);
        if (sude == null) {
            sude = new StudentMarkerMapDocumentEntity(document, null);
            em.persist(sude);
        }
    }

    @Override
    public String getStringEntry(StudentId id, DocumentId document, Date asOf) {
        StudentStringMapDocumentEntity sse = em.find(StudentStringMapDocumentEntity.class, document, LockModeType.OPTIMISTIC);
        if (sse != null && id != null) {
            return sse.get(id);
        } else {
            return null;
        }
    }

    @Override
    public Marker getMarkerEntry(final StudentId id, final DocumentId document, final Date asOf) {
        ensureDocument(document);
        final StudentMarkerMapDocumentEntity sse = em.find(StudentMarkerMapDocumentEntity.class, document, LockModeType.OPTIMISTIC);
        if (sse != null && id != null) {
            final Marker m = sse.get(id);
            if (asOf != null) {
                final StudentMarkerRestore restore = new StudentMarkerRestore(id, m);
                sse.applyRestoreVersion(asOf, restore);
                return restore.getRestored();
            } else {
                return m;
            }
        }
        return null;
    }

//    @Messages("StudentsListsLocalBeanImpl.NonUniqueResult=Found {1} primary units for student {0}. Please fix database!")
    @Override
    public UnitId findPrimaryUnit(StudentId id, Date asOf) {
        return unitDocumentFacadeImpl.findPrimaryUnitForStudent(id, asOf, LockModeType.OPTIMISTIC);
//        List<UnitDocumentEntity> l = unitDocumentFacadeImpl.findPrimaryUnitForStudent(id, LockModeType.OPTIMISTIC);
//        if (l == null || l.isEmpty()) {
//            return null;
//        } else if (l.size() > 1) {
////            throw new NonUniqueResultException();
//            String msg = NbBundle.getMessage(StudentsListsLocalBeanImpl.class, "StudentsListsLocalBeanImpl.NonUniqueResult", id, l);
//            Logger.getLogger(StudentsListsLocalBeanImpl.class.getName()).log(Level.SEVERE, msg);
//            return null;
//        } else {
//            return l.get(0).getUnitId();
//        }
    }

}
