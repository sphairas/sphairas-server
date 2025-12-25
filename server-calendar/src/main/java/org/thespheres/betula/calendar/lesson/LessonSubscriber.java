/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import java.io.Serializable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.thespheres.betula.calendar.util.EmbeddableSignee;
import org.thespheres.betula.document.Signee;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "findLessonsForSignee", query = "SELECT ls FROM LessonSubscriber ls "
            + "WHERE ls.signee=:signee", hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true")
            })})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "LESSON_SUBSCRIBER")
@Access(AccessType.FIELD)
public class LessonSubscriber implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Version
    @Column(name = "LESSON_SUBSCRIBER_VERSION")
    private long version;
    @Column(name = "ENTITLEMENT", nullable = true)
    private String entitlement;
    @Embedded
    private EmbeddableSignee signee;
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "LESSON_ID", referencedColumnName = "ID")})
    private Lesson lesson;

    public LessonSubscriber() {
    }

    public LessonSubscriber(final EmbeddableSignee signee, final Lesson lesson) {
        this.signee = signee;
        this.lesson = lesson;
    }

    public Long getId() {
        return id;
    }

    public Lesson getLessonUnit() {
        return lesson;
    }

    public Signee getSubscriber() {
        return signee.getSignee();
    }

    public String getEntitlement() {
        return entitlement;
    }

    public void setEntitlement(String entitlement) {
        this.entitlement = entitlement;
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
        if (!(object instanceof LessonSubscriber)) {
            return false;
        }
        LessonSubscriber other = (LessonSubscriber) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

}
