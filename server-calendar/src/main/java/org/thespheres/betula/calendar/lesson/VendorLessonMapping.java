/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.eclipse.persistence.annotations.IdValidation;
import org.eclipse.persistence.annotations.PrimaryKey;
import org.thespheres.betula.services.scheme.spi.LessonId;

/**
 *
 * @author boris.heithecker
 */
@Entity
@PrimaryKey(validation = IdValidation.NULL)
@Table(name = "VENDOR_LESSON_MAPPING2")
@Access(AccessType.FIELD)
public class VendorLessonMapping implements Serializable {

    @EmbeddedId
    private EmbeddableLessonId vendorLessonId;
    @ManyToOne
    private Lesson lesson;
    @ManyToMany
    @JoinTable(name = "VENDOR_LESSON_MAPPING_WEEKLY_LESSON_CALENDARCOMPONENT",
            joinColumns = {
                @JoinColumn(name = "VENDOR_LESSON_AUTHORITY", referencedColumnName = "VENDOR_LESSON_AUTHORITY"),
                @JoinColumn(name = "VENDOR_LESSON_ID", referencedColumnName = "VENDOR_LESSON_ID"),
                @JoinColumn(name = "VENDOR_LESSON_LINK", referencedColumnName = "VENDOR_LESSON_LINK")},
            inverseJoinColumns = {
                @JoinColumn(name = "WEEKLY_LESSON_CALENDARCOMPONENT_SYSUID", referencedColumnName = "UID_SYSID"),
                @JoinColumn(name = "WEEKLY_LESSON_CALENDARCOMPONENT_HOST", referencedColumnName = "UID_HOST")})
    private Set<WeeklyLessonComponent> components = new HashSet<>();

    public VendorLessonMapping() {
    }

    public VendorLessonMapping(final LessonId lessonId, final int link, final Lesson parent) {
        this.vendorLessonId = new EmbeddableLessonId(lessonId, link);
        this.lesson = parent;
    }

    public Lesson getLessonUnit() {
        return lesson;
    }

    public LessonId getVendorLessonId() {
        return vendorLessonId.getLessonId();
    }

    public int getLink() {
        return vendorLessonId.getLink();
    }

    public Set<WeeklyLessonComponent> getComponents() {
        return components;
    }

}
