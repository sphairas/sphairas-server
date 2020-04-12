/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.lesson;

import java.util.ArrayList;
import org.thespheres.betula.calendar.LayeredUpdate;
import java.util.Date;
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OrderColumn;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.thespheres.betula.calendar.EmbeddableComponentProperty;
import org.thespheres.betula.calendar.util.EmbeddableSignee;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.impl.ParameterList;

/**
 *
 * @author boris.heithecker
 */
@NamedQueries({
    @NamedQuery(name = "findUpdatesForCoveringSignee", query = "SELECT u FROM LessonUpdate u "
            + "WHERE (u.coveringSignee.prefix=:signeePrefix AND  u.coveringSignee.suffix=:signeeSuffix AND u.coveringSignee.alias=:signeeAlias)",
            hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "100")
            }),
    @NamedQuery(name = "findAllUpdatesAfterStartInclusive", query = "SELECT u FROM LessonUpdate u "
            + "WHERE u.dtstart >= :after", hints = {
                @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
                @QueryHint(name = "eclipselink.query-results-cache.size", value = "1")
            }),
    @NamedQuery(name = "findAllUpdatesAfter", query = "SELECT u FROM LessonUpdate u "
            + "WHERE u.dtstamp > :after")})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "LESSON_UPDATE")
@Access(AccessType.FIELD)
public class LessonUpdate extends LayeredUpdate<WeeklyLessonComponent> {

    //TODO withParent????, parent = vertretungs calendar, method update?
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @javax.persistence.Version
    @Column(name = "VERSION")
    private long version;
    @Column(name = "VENDOR_COVER_ID", length = 64)//z.B. Vertretungsnummer
    private String vendorCoverId;
    @AttributeOverrides(value = {
        @AttributeOverride(name = "prefix", column = @Column(name = "COVERING_ID")),
        @AttributeOverride(name = "suffix", column = @Column(name = "COVERING_AUTHORITY", length = 64)),
        @AttributeOverride(name = "alias", column = @Column(name = "COVERING_ALIAS"))})
    private EmbeddableSignee coveringSignee;
    @Column(name = "ALT_COVERING")
    private String altCover; //falls als signee nicht gelistet, use this value only if signeeSubstituting == null
    @Column(name = "MESSAGE", length = 1024)
    private String message; //In case updated == null, use alternative text to resolve lesson related info for user
    @Column(name = "LOCATION_VALUE")
    private String location;
    @Column(name = "LOCATION_PARAMETERS")
    private final ParameterList locationParameters = new ParameterList();
    @Embedded
    @ElementCollection
    @CollectionTable(name = "LESSON_UPDATE_PROPERTIES")
    @OrderColumn(name = "LESSON_UPDATE_PROPERTY_ORDER")
    protected List<EmbeddableComponentProperty> cmpr = new ArrayList<>();

    public LessonUpdate() {
    }

    public LessonUpdate(final WeeklyLessonComponent reference, @NotNull Date recurrence) {
        super(CalendarComponent.VEVENT, reference, recurrence);
    }

    public Long getId() {
        return id;
    }

    public List<EmbeddableComponentProperty> getComponents() {
        return cmpr;
    }

    public String getVendorCoverId() {
        return vendorCoverId;
    }

    public void setVendorCoverId(String vendorCoverId) {
        this.vendorCoverId = vendorCoverId;
    }

    public EmbeddableSignee getCoveringSignee() {
        return coveringSignee;
    }

    public void setCoveringSignee(EmbeddableSignee coveringSignee) {
        this.coveringSignee = coveringSignee;
    }

    public String getAlternativeCoverText() {
        return altCover;
    }

    public void setAlternativeCoverText(final String altCover) {
        this.altCover = altCover;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ParameterList getLocationParameters() {
        return locationParameters;
    }

}
