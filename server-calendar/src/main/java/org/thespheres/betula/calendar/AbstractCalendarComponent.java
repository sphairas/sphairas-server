/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar;

import java.util.Date;
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import org.thespheres.ical.impl.ParameterList;

/**
 *
 * @author boris.heithecker
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class AbstractCalendarComponent {

    @Version
    @Column(name = "COMPONENT_VERSION")
    private long version;
    @Column(name = "COMPONENT_NAME", length = 64)
    private final String name;
    @Column(name = "DTSTAMP_VALUE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dtstamp;
    @Column(name = "DTSTART_VALUE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dtstart;
    @Column(name = "DTEND_VALUE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dtend;
    @Column(name = "DURATION_VALUE", length = 16)
    private String duration;
    @Column(name = "STATUS_VALUE", length = 16)
    private String status;
    @Column(name = "SUMMARY_PRIORITY")
    private Integer priority;
    @Column(name = "SUMMARY_VALUE")
    private String summary;
    @Column(name = "SUMMARY_PARAMETERS")
    private final ParameterList summaryParameters = new ParameterList();

    protected AbstractCalendarComponent(String name) {
        this.name = name;
        dtstamp = new Date(System.currentTimeMillis());
    }

    @PreUpdate
    public void updateDtstamp() {
        dtstamp = new Date(System.currentTimeMillis());
    }

    public long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public abstract List<EmbeddableComponentProperty> getProperties();

    public Date getDtstamp() {
        return dtstamp;
    }

    public Date getDtstart() {
        return dtstart;
    }

    public void setDtstart(Date dtstart) {
        this.dtstart = dtstart;
    }

    public Date getDtend() {
        return dtend;
    }

    public void setDtend(Date dtend) {
        this.dtend = dtend;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority == null ? 0 : priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public ParameterList getSummaryParameters() {
        return summaryParameters;
    }

    public Date getTimestamp() {
        return dtstamp;
    }

    public void setTimestamp(Date timestamp) {
        this.dtstamp = timestamp;
    }

    public interface WithParent<E> {

        public E getParent();
    }
}
