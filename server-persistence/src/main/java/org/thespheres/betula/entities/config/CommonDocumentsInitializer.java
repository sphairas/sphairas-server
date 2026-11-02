package org.thespheres.betula.entities.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.DocumentStringMapDocumentEntity;
import org.thespheres.betula.entities.SigneeUnitMapDocumentEntity;
import org.thespheres.betula.entities.StudentMarkerMapDocumentEntity;
import org.thespheres.betula.services.ws.CommonDocuments;

/**
 * Ensures that the documents configured in the schulvorlage exist. Lookups
 * (reads) no longer create them on the fly.
 */
@Singleton
@Startup
public class CommonDocumentsInitializer {

    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
    @Inject
    private CommonDocuments cd;

    @PostConstruct
    void ensureDocuments() {
        try {
            ensure(CommonDocuments.PRIMARY_UNIT_HEAD_TEACHERS_DOCID, SigneeUnitMapDocumentEntity.class);
            ensure(CommonDocuments.STUDENT_CAREERS_DOCID, StudentMarkerMapDocumentEntity.class);
            ensure(CommonDocuments.COMMON_NAMES_DOCID, DocumentStringMapDocumentEntity.class);
            ensure(CommonDocuments.SUBJECT_NAMES_DOCID, DocumentStringMapDocumentEntity.class);
        } catch (RuntimeException ex) {
            Logger.getLogger(CommonDocumentsInitializer.class.getName()).log(Level.WARNING, "Could not ensure common documents.", ex);
        }
    }

    private <E> void ensure(final String name, final Class<E> type) {
        final DocumentId id = cd.forName(name);
        if (id == null || em.find(type, id) != null) {
            return;
        }
        try {
            final Object e;
            if (type == SigneeUnitMapDocumentEntity.class) {
                e = new SigneeUnitMapDocumentEntity(id, null);
            } else if (type == StudentMarkerMapDocumentEntity.class) {
                e = new StudentMarkerMapDocumentEntity(id, null);
            } else {
                e = new DocumentStringMapDocumentEntity(id, null);
            }
            em.persist(e);
            Logger.getLogger(CommonDocumentsInitializer.class.getName()).log(Level.INFO, "Created document {0} ({1}).", new Object[]{id, name});
        } catch (RuntimeException ex) {
            Logger.getLogger(CommonDocumentsInitializer.class.getName()).log(Level.WARNING, "Could not create document " + id, ex);
        }
    }
}
