/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostRemove;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import org.thespheres.acer.MessageId;
import org.thespheres.acer.MessageId.Version;
import org.thespheres.acer.entities.messages.tracking.BaseTracker;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "findMessagesForChannels", query = "SELECT message FROM BaseMessage message, BaseChannel channel "
            + "WHERE message.channel=channel "
            + "AND channel.name IN :channels", hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true")
            })})
@Entity
@Table(name = "BASE_MESSAGE")
@IdClass(MessageId.class)
@Inheritance(strategy = InheritanceType.JOINED)
@Access(AccessType.FIELD)
public class BaseMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "MESSAGE_ID")
    private Long id;
    @Id
    @Column(name = "MESSAGE_AUTHORITY", length = 64)
    private String authority;
    @Id
    @Column(name = "MESSAGE_VERSION", length = 32)
    private String version = MessageId.Version.LATEST.getVersion();
    @Column(name = "CURRENTVERSION", length = 32)
    private String currentVersion;
    @javax.persistence.Version
    @Column(name = "BASE_MESSAGE_VERSION")
    private long entityVersion;
    @ManyToOne
    @JoinColumn(name = "BASE_MESSAGE_CHANNEL_NAME")
    private BaseChannel channel;
    @JoinColumn(name = "BASE_MESSAGE_BASE_TRACKER")
    @JoinTable(name = "BASE_MESSAGE_BASE_TRACKER",
            joinColumns = {
                @JoinColumn(name = "MESSAGE_ID", referencedColumnName = "MESSAGE_ID"),
                @JoinColumn(name = "MESSAGE_AUTHORITY", referencedColumnName = "MESSAGE_AUTHORITY"),
                @JoinColumn(name = "MESSAGE_VERSION", referencedColumnName = "MESSAGE_VERSION")},
            inverseJoinColumns = @JoinColumn(name = "BASE_TRACKER_ID", referencedColumnName = "BASE_TRACKER_ID")
    )
    @ManyToMany(cascade = {CascadeType.ALL}) //TODO delete wenn refernced by others?
    private Set<BaseTracker> trackers = new HashSet<>();
    @Column(name = "BASE_MESSAGE_CREATION_TIMESTAMP")
    private java.sql.Timestamp creationTime;
    @AttributeOverrides({
        @AttributeOverride(name = "prefix", column = @Column(name = "CREATOR_SIGNEE_PREFIX")),
        @AttributeOverride(name = "suffix", column = @Column(name = "CREATOR_SIGNEE_SUFFIX")),
        @AttributeOverride(name = "alias", column = @Column(name = "CREATOR_SIGNEE_ALIAS"))
    })
    @Embedded
    private EmbeddableSignee creator;

    public BaseMessage() {
    }

    public BaseMessage(String authority, BaseChannel channel, Signee creator) {
        this.currentVersion = "1";
        this.authority = authority;
        this.creator = creator != null ? new EmbeddableSignee(creator) : null;
        this.creationTime = new java.sql.Timestamp(System.currentTimeMillis());
        this.channel = channel;
    }

    public MessageId getId() {
        return new MessageId(authority, id, Version.parse(version));
    }

    public BaseChannel getChannel() {
        return channel;
    }

    public Set<BaseTracker> getTrackers() {
        return trackers;
    }

    public java.sql.Timestamp getCreationTime() {
        return creationTime;
    }

    public Signee getCreator() {
        return creator != null ? creator.getSignee() : null;
    }

    public DocumentId.Version getCurrentVersion() {
        return DocumentId.Version.parse(currentVersion);
    }

    public void setCurrentVersion(DocumentId.Version v) {
        currentVersion = v.getVersion();
    }

    @PostRemove
    public void remove() {
        channel = null;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BaseMessage)) {
            return false;
        }
        BaseMessage other = (BaseMessage) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "org.thespheres.acer.entities.messages.BaseMessage[ id=" + id + " ]";
    }

}
