/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.messaging;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.entities.BaseTargetAssessmentEntity;
import org.thespheres.betula.entities.EmbeddableSigneeInfo;
import org.thespheres.betula.entities.EmbeddableStudentId;
import org.thespheres.betula.entities.EmbeddableTermId;
import org.thespheres.betula.entities.SigneeEntity;
import org.thespheres.betula.entities.UnitDocumentEntity;
import org.thespheres.betula.entities.UnitDocumentNotFoundException;
import org.thespheres.betula.server.beans.ChannelsLocal;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;

/**
 *
 * @author boris.heithecker
 */
@Messages({"ChannelsLocalImpl.mail.subject=Mitteilung betreffend {0}"})
@LocalBean
@Stateless
public class ChannelsLocalImpl implements ChannelsLocal {

    @EJB
    private SigneeLocal signees;
    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentVCardsImpl;
    @EJB
    private StudentsListsLocalBean sllb;
    @PersistenceContext(unitName = "betula0")
    private EntityManager em;
//    @Resource(name = "mail/iservMessaging")
//    private Session mailiserv;
    @Default
    @Inject
    private NamingResolver namingResolver;
    @Current
    @Inject
    private Term currentTerm;
    @Default
    @Inject
    private DocumentsModel docModel;

    @Override
    public void sendEmail(String patternChannel, String channelDisplyName, String body) {
        final ChannelPatternEntity cpe = em.find(ChannelPatternEntity.class, patternChannel, LockModeType.OPTIMISTIC);
        if (cpe == null) {
            return;
        }
        CriteriaQuery<BaseTargetAssessmentEntity> cq = em.getCriteriaBuilder().createQuery(BaseTargetAssessmentEntity.class);
        cq.select(cq.from(BaseTargetAssessmentEntity.class));
        Signee[] rcp = em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList().stream()
                .filter(cpe::matches)
                .map(btae -> (EmbeddableSigneeInfo) btae.getEmbeddableSignees().get("entitled.signee"))
                .filter(Objects::nonNull)
                .map(EmbeddableSigneeInfo::getSignee)
                .distinct()
                .toArray(Signee[]::new);
        if (rcp.length != 0) {
            doSend(channelDisplyName, null, null, body, rcp);
        }
    }

    @Override
    public void sendEmail(String channelDisplyName, String body, Signee[] recipients) {
        doSend(channelDisplyName, null, null, body, recipients);
    }

    @Override
    public void sendEmail(UnitId unit, String body) {  //    findTargetSigneesForAllUnitDocumentEntityStudents
        final UnitDocumentEntity ude = em.find(UnitDocumentEntity.class, docModel.convertToUnitDocumentId(unit), LockModeType.OPTIMISTIC);
        if (ude == null) {
            throw new UnitDocumentNotFoundException(unit);
        }
        final TermId term = currentTerm.getScheduledItemId();//SystemProperties.currentTerm();
        String dn;
        try {
            dn = namingResolver.resolveDisplayName(unit);
        } catch (IllegalAuthorityException illaex) {
            dn = unit.getId();
        }
        Signee[] rcp = em.createNamedQuery("findTargetSigneesForAllUnitDocumentEntityStudents", SigneeEntity.class)
                //                .setLockMode(LockModeType.OPTIMISTIC) //signee has no version column
                .setParameter("entitlement", "entitled.signee")
                .setParameter("unit", ude)
                .setParameter("term", new EmbeddableTermId(term))
                .getResultList()
                .stream()
                .map(SigneeEntity::getSignee)
                .distinct()
                .toArray(Signee[]::new);
        doSend(dn, null, null, body, rcp);
    }

    @Override
    public void sendEmail(final StudentId[] scope, final String body) { // findTargetSigneesForSelectedStudents
        final TermId term = currentTerm.getScheduledItemId(); //SystemProperties.currentTerm();
        StringJoiner sj = new StringJoiner(", ");
        Arrays.stream(scope)
                .map(sid -> createStudentItem(sid, term))
                .forEach(sj::add);
        final String dn = sj.toString();
        final EmbeddableTermId et = new EmbeddableTermId(term);
        Signee[] rcp = Arrays.stream(scope)
                .flatMap(sid -> {
                    return em.createNamedQuery("findTargetSigneesForSelectedStudents", SigneeEntity.class)
                            //                .setLockMode(LockModeType.OPTIMISTIC)   //signee has no version column
                            .setParameter("entitlement", "entitled.signee")
                            .setParameter("student", new EmbeddableStudentId(sid))
                            .setParameter("term", et)
                            .getResultList()
                            .stream();
                })
                .map(SigneeEntity::getSignee)
                .distinct()
                .toArray(Signee[]::new);
        doSend(dn, null, null, body, rcp);
    }

    private String createStudentItem(final StudentId s, final TermId current) {
        String fn = Utilities.formatFullname(studentVCardsImpl.get(s));
        UnitId pu = sllb.findPrimaryUnit(s, null);
        if (pu != null && current != null) {
            String puname;
            try {
                puname = namingResolver.resolveDisplayName(pu);
            } catch (IllegalAuthorityException illaex) {
                puname = pu.getId();
            }
            fn += " (" + puname + ")";
        }
        return fn;
    }

    private void doSend(String channelDisplyName, String from, String name, String body, Signee[] add) throws MissingResourceException, EJBException {
        InternetAddress sender = null;
        try {
            sender = new InternetAddress(from, name);
        } catch (UnsupportedEncodingException ex) {
            throw new EJBException(ex);
        }
        InternetAddress replyTo = null;
        Signee current = signees.getSigneePrincipal(true);
        if (current != null) {
            try {
                replyTo = new InternetAddress(current.toString());
            } catch (AddressException ex) {
                throw new EJBException(ex);
            }
        } else {
            replyTo = sender;
        }
        Logger.getLogger(ChannelsLocalImpl.class.getName()).log(Level.INFO, "Preparing email message, body is: {0}", body);
        InternetAddress[] recipients = Arrays.stream(add)
                .map(s -> {
                    try {
                        Logger.getLogger(ChannelsLocalImpl.class.getName()).log(Level.INFO, "Adding signee {0}", s.toString());
                        return new InternetAddress(s.toString(), false);
                    } catch (AddressException ex) {
                        throw new EJBException(ex);
                    }
                })
                .toArray(InternetAddress[]::new);
//        MimeMessage message = new MimeMessage(mailiserv);
//        try {
//            String subject = NbBundle.getMessage(ChannelsLocalImpl.class, "ChannelsLocalImpl.mail.subject", channelDisplyName);
//            message.setSubject(subject);
//            message.setRecipients(Message.RecipientType.TO, recipients); //InternetAddress.parse(email, false));
//            message.setSender(sender);
//            message.setReplyTo(new InternetAddress[]{replyTo});
//            message.setText(body);
//            Transport.send(message, "xxxxxxxxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxxxxxxxxxxxxxxxx");
//        } catch (MessagingException ex) {
//            throw new EJBException(ex);
//        }
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    @Override
    public void updatePattern(String channel, String pattern) {
        ChannelPatternEntity cpe = em.find(ChannelPatternEntity.class, channel, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (cpe == null && pattern != null) {
            cpe = new ChannelPatternEntity(channel, pattern);
            em.persist(cpe);
        }
        if (cpe != null) {
            if (pattern != null) {
                cpe.setRegex(pattern);
                em.merge(cpe);
            } else {
                em.remove(cpe);
            }
        }

    }

    public Collection<String> getPatternChannels(final String[] match) {
        CriteriaQuery<ChannelPatternEntity> cq = em.getCriteriaBuilder().createQuery(ChannelPatternEntity.class);
        cq.select(cq.from(ChannelPatternEntity.class));
        return em.createQuery(cq).setLockMode(LockModeType.OPTIMISTIC).getResultList().stream()
                .filter(cpe -> Arrays.stream(match).anyMatch(cpe::matches))
                .map(ChannelPatternEntity::getName).collect(Collectors.toSet());
    }

}
