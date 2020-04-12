/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Version;
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
