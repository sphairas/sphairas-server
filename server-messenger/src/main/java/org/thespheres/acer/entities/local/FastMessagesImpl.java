/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.local;

import java.io.Serializable;
import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.entities.facade.ChannelFacade;
import org.thespheres.acer.entities.facade.CommonTrackers;
import org.thespheres.acer.entities.facade.MailMessageFacade;
import org.thespheres.acer.entities.BaseChannel;
import org.thespheres.acer.entities.BaseMessage;
import org.thespheres.acer.entities.DurableMessage;
import org.thespheres.acer.entities.SigneeAction;
import org.thespheres.acer.entities.StaticChannel;
import org.thespheres.acer.entities.StudentAction;
import org.thespheres.acer.entities.StudentAction.Action;
import org.thespheres.acer.entities.StudentsChannel;
import org.thespheres.acer.entities.UnitChannel;
import org.thespheres.acer.entities.util.MessageComparator;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.server.beans.FastMessage;
import org.thespheres.betula.server.beans.FastMessages;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.server.beans.annot.DocumentsRequest;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed("signee")
@SessionScoped
@LocalBean
@Stateful
public class FastMessagesImpl implements FastMessages, Serializable {

    @EJB
    private ChannelFacade facade;
    @EJB
    private MailMessageFacade mail;
    @EJB
    private CommonTrackers trackers;
    @EJB
    private Naming naming;
    @EJB
    private SigneeLocal signee;
//    @DocumentsSession
//    @Inject
//    private Instance<FastTargetDocuments2> ftd2SessionInstance;
    @DocumentsRequest
    @Inject
    private Instance<FastTargetDocuments2> ftd2RequestInstance;

    private FastTargetDocuments2 getFastTargetDocuments2() {
        return ftd2RequestInstance.get();
    }
    @PersistenceContext(unitName = "messagingPU")
    private EntityManager em;
    private List<BaseMessage> entities;
    private Set<String> channelNames;
    private static final DateFormat df = new SimpleDateFormat("EEE., d. MMM YY", Locale.GERMANY);
    private List<FastMessage> fast;
    private static final Collator collator = Collator.getInstance(Locale.getDefault());
    private SortedMap<String, String> channels;

    private synchronized List<BaseMessage> getMessages(boolean update) {
        if (entities == null || update) {
            if (getChannelNames().isEmpty()) {
                //namedQuery throws error if getChannelNames() is empty
                entities = Collections.EMPTY_LIST;
            } else {
                entities = em.createNamedQuery("findMessagesForChannels", BaseMessage.class)
                        .setParameter("channels", getChannelNames())
                        .setLockMode(LockModeType.OPTIMISTIC)
                        .getResultList();
            }
        }
        return entities;
    }

    private synchronized Set<String> getChannelNames() {
        if (channelNames == null) {
            channelNames = new HashSet<>();
            facade.getStaticChannels(LockModeType.OPTIMISTIC).stream().filter(this::isSigneeIncluded).map(BaseChannel::getName).forEach(channelNames::add);
            channelNames.addAll(getFastTargetDocuments2() .getPatternChannels());
            getFastTargetDocuments2().getUnits().stream().map(c -> facade.find(c.getId(), UnitChannel.class, LockModeType.OPTIMISTIC)).filter(dc -> (dc != null)).forEach(dc -> {
                channelNames.add(dc.getName());
            });
            facade.getUnitChannels(false, LockModeType.OPTIMISTIC).stream().filter(uc -> getFastTargetDocuments2() .getIntersection(uc.getUnit()).length != 0).map(UnitChannel::getName).forEach(channelNames::add);
            facade.getStudentsChannels(LockModeType.OPTIMISTIC).stream()
                    .filter(sc -> getFastTargetDocuments2() .getIntersection(sc.getStudentAction().stream()
                    .filter(sa -> sa.getAction().equals(Action.INCLUDE))
                    .map(StudentAction::getStudentId)
                    .toArray(StudentId[]::new)).length != 0)
                    .map(StudentsChannel::getName)
                    .forEach(channelNames::add);
        }
        return channelNames;
    }

    private boolean isSigneeIncluded(StaticChannel sc) {
        Set<SigneeAction> saction = sc.getSignees();
        if (!saction.isEmpty()) {
            return saction.stream().anyMatch(a -> a.getSignee().equals(signee.getSigneePrincipal(true)) && a.getAction().equals(SigneeAction.Action.INCLUDE));
        }
        //default behaviour defined by channel policy
        return true;
    }

    @Override
    public List<FastMessage> getFastMessages(boolean refresh) {
        if (fast == null || refresh) {
            fast = getMessages(refresh).stream()
                    .sorted(new MessageComparator())
                    .map(this::createFastMessage)
                    .collect(Collectors.toList());
        }
        return fast;
    }

    private FastMessage createFastMessage(BaseMessage message) {
        DurableMessage tm = (DurableMessage) message;
        String messageText = tm.getBaseText();
        Signee creator = tm.getCreator();
        String author = creator != null ? creator.toString() : "o.N.";
        boolean canEdit = Objects.equals(creator, signee.getSigneePrincipal(true));
        boolean read = trackers.getPrivateStatusCollapsed(message.getId());
        boolean confidential = false;
        String property = tm.getProperties().getProperty("encoding");
        if (property != null && "base64".equals(property)) {
            byte[] b = Base64.getDecoder().decode(messageText);
            messageText = new String(b);
            confidential = true;
        }
        String text = messageText + " <i>(" + df.format(tm.getCreationTime()) + ")</i>";
        return new FastMessage(tm.getId(), author, text, tm.getChannel().getName(), read, false, confidential, canEdit);
    }

    @Override
    public SortedMap<String, String> getChannels() {
        if (channels == null) {
            final Map<String, String> map = getChannelNames().stream().collect(Collectors.toMap(s -> s, s -> naming.getChannelDisplayName(s)));
            channels = new TreeMap<>((s1, s2) -> collator.compare(map.get(s1), map.get(s2)));
            channels.putAll(map);
        }
        return channels;
    }

    @Override
    public MessageId publish(MessageId message, String channel, String messageText, boolean confidential, boolean sendEmail) {
        DurableMessage dm = null;
        if (message != null) {
            dm = facade.findMessage(message, DurableMessage.class, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }
        if (confidential) {
            messageText = Base64.getEncoder().encodeToString(messageText.getBytes());
        }
        if (dm == null) {
            dm = facade.publish(channel, messageText, 5, confidential);
        } else {
            facade.update(dm, messageText, 5, confidential);
        }
        if (sendEmail) {
            mail.sendMessageAsEmail(dm.getId());
        }
        return dm.getId();
    }

    @Override
    public void markRead(MessageId messageId, boolean read) {
        trackers.setPrivateStatusCollapsed(messageId, read);
    }

    @Override
    public void delete(MessageId mid) {
        facade.delete(mid);
    }

}
