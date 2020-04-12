/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.watch;

import java.time.Instant;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.security.DeclareRoles;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.facade.DocumentsFacade;

/**
 *
 * @author boris.heithecker
 */
@DeclareRoles("timer")
@Stateless
@LocalBean
public class DocumentExpiryTimerBean {

    @EJB(beanName = "DocumentsFacadeImpl")
    private DocumentsFacade documents;
//    @Inject
//    private NamingResolver naming;
//    @Current
//    @Inject
//    private Term currentTerm;
//    @Inject
//    private StudentsLocalBean slb;
//    @PersistenceContext(unitName = "betula0")
//    protected EntityManager em;

//    static final ResourceBundle BUNDLE = NbBundle.getBundle("org.thespheres.betula.entities.watch.ExpiredItemsDescriptions");

    static final String EXPIRED_UNITDOCUMENTS_QUERY = "SELECT DISTINCT e FROM UnitDocumentEntity e "
            + "WHERE e.expirationTime <= :dateTime";

    @Schedule(dayOfWeek = "Mon-Sun",
            month = "*",
            hour = "1",
            dayOfMonth = "*",
            year = "*",
            minute = "45",
            second = "0")
    public void myTimer() {
        final Instant now = Instant.now();
        final Set<DocumentId> toDelete = documents.findAllExpired(now.toEpochMilli());
        final String msg = toDelete.stream()
                .map(DocumentId::toString)
                .collect(Collectors.joining(System.getProperty("line.separator")));
//        toDelete.stream()
//                .forEach(d -> Logger.getLogger("EXPIRY").log(Level.INFO, "Expired:\n{0}", msg));


//
//        Logger.getLogger("EXPIRY").log(Level.INFO, "Found {0} valid students.", validStudents.size());
//
//        final List<UnitDocumentEntity> expiredPrimaryUnits = em.createQuery(EXPIRED_UNITDOCUMENTS_QUERY, UnitDocumentEntity.class)
//                .setLockMode(LockModeType.OPTIMISTIC)
//                .setParameter("dateTime", new java.sql.Timestamp(now.toEpochMilli()), TemporalType.TIMESTAMP)
//                .getResultList();
//
//        final ContainerBuilder builder = new ContainerBuilder();
//        final ZonedDateTime zdt = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());
//        final String time = zdt.format(DateTimeUtil.ZONED_DATE_TIME_FORMAT);
//
//        expiredPrimaryUnits.forEach(ude -> addDeleteUnitEntry(builder, ude, time));
//
//        final Container container = builder.getContainer();
//                final String file = "delete.xml";
        
//        backup(container, time);
    }


//    protected Container createDeleteContainer(final Set<DocumentId> docs, final Instant now) {
//
//        docs.forEach(d -> {
//            final BaseDocumentEntity be = documents.get(d);
//            if (be instanceof UnitDocumentEntity) {
//                addDeleteUnitEntry(builder, (UnitDocumentEntity) be, time);
//            } else if (be instanceof GradeTargetAssessmentEntity) {
//                addDeleteTargetEntry(builder, (GradeTargetAssessmentEntity<?>) be, time);
//            }
//        });
//        return 
//    }
//    protected void addDeleteUnitEntry(final ContainerBuilder builder, final UnitDocumentEntity ude, final String time) {
//        final Template root = builder.createTemplate(null, ude.getUnitId(), null, Paths.UNITS_PARTICIPANTS_PATH, null, null);
//        final UnitEntry ret = new UnitEntry(ude.getDocumentId(), ude.getUnitId(), Action.ANNUL, true);
//        root.getChildren().add(ret);
//        ret.getDescription().add(createUnitDeleteDescription(ude, time));
//    }
//
//    protected Description createUnitDeleteDescription(final UnitDocumentEntity ude, final String time) {
//        final Language l = new Language(Language.IETF, "de");
//        final String key = "unitDocumentDeleteInfo";
//        String docRes;
//        try {
//            docRes = naming.resolveDisplayNameResult(ude.getDocumentId()).getResolvedName(currentTerm);
//        } catch (IllegalAuthorityException ex) {
//            docRes = ude.getDocumentId().toString();
//        }
//        final String studs = ude.getStudentIds().stream()
//                .map(this::mapStudentIdToLine)
//                .collect(Collectors.joining(", "));
//        final String value = MessageFormat.format(BUNDLE.getString(key), new Object[]{docRes, ude.getDocumentId(), studs, time});
//        return new Description(key, value, l);
//    }
//
//    private String mapStudentIdToLine(final StudentId sid) {
//        final Optional<String> fn = Optional.ofNullable(slb.get(sid)).map(VCard::getFN);
//        final String id = Long.toString(sid.getId());
//        if (fn.isPresent()) {
//            return fn.get() + " (" + id + ")";
//        } else {
//            return id;
//        }
//    }
//
//    protected void addDeleteTargetEntry(final ContainerBuilder builder, final GradeTargetAssessmentEntity<?> gtae, final String time) {
//        final TargetAssessmentEntry<TermId> tae = builder.createTargetAssessmentAction(null, gtae.getDocumentId(), Paths.UNITS_TARGETS_PATH, null, Action.ANNUL, true);
//        tae.getHints().put("update-pu-links", "true");
//        tae.getHints().put("process-bulk", "true");
//        tae.getDescription().add(createTargetDeleteDescription(gtae, time));
//    }
//
//    Description createTargetDeleteDescription(final GradeTargetAssessmentEntity<?> gtae, final String time) {
//        final Language l = new Language(Language.IETF, "de");
//        final String key = "gradeTargetDocumentDeleteInfo";
//        String docRes;
//        try {
//            docRes = naming.resolveDisplayNameResult(gtae.getDocumentId()).getResolvedName(currentTerm);
//        } catch (IllegalAuthorityException ex) {
//            docRes = gtae.getDocumentId().toString();
//        }
//        final String value = MessageFormat.format(BUNDLE.getString(key), new Object[]{docRes, gtae.getDocumentId(), time});
//        return new Description(key, value, l);
//    }
}
