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
import javax.persistence.Embeddable;
import org.thespheres.betula.services.scheme.spi.LessonId;

/**
 *
 * @author boris.heithecker
 */
@Embeddable
@Access(value = AccessType.FIELD)
public class EmbeddableLessonId implements Serializable {

    @Column(name = "VENDOR_LESSON_AUTHORITY", length = 64)
    private String authority;
    @Column(name = "VENDOR_LESSON_ID", length = 64)
    private String id;
    @Column(name = "VENDOR_LESSON_LINK")
    private int link;
    
    public EmbeddableLessonId() {
    }

    public EmbeddableLessonId(LessonId lesson, int link) {
        this.authority = lesson.getAuthority();
        this.id = lesson.getId();
        this.link = link;
    }

    public LessonId getLessonId() {
        return new LessonId(authority, id);
    }

    public int getLink() {
        return link;
    }
    
}
