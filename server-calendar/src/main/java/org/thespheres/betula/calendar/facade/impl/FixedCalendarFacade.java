/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade.impl;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import org.thespheres.ical.builder.ICalendarBuilder;
import org.thespheres.ical.builder.ICalendarBuilder.CalendarComponentBuilder;
import org.thespheres.betula.calendar.BaseCalendarEntity;
import org.thespheres.betula.calendar.EmbeddableComponentProperty;
import org.thespheres.betula.calendar.UniqueCalendarComponentEntity;
import org.thespheres.betula.calendar.config.PropertyNames;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.CalendarComponentProperty;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.Parameter;
import org.thespheres.ical.UID;
import org.thespheres.ical.util.IComponentUtilities;

/**
 *
 * @author boris.heithecker
 * @param <T>
 * @param <E>
 */
public abstract class FixedCalendarFacade<T extends BaseCalendarEntity<? extends UniqueCalendarComponentEntity>, E extends UniqueCalendarComponentEntity> extends BaseComponentFacade<E, UID> {

    @PersistenceContext(unitName = "calendarsPU")
    private EntityManager em;

    protected FixedCalendarFacade(Class<E> entityClass) {
        super(entityClass);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    protected abstract T getCalendar();

    @Override
    protected E find(UID id, LockModeType lmt) {
        E ret = getEntityManager().find(entityClass, id, lmt);
        if (ret != null && !ret.getParent().equals(getCalendar())) {
            throw new IllegalArgumentException(id.toString() + " is not a component of calendar " + getCalendar().toString());
        }
        return ret;
    }

    public boolean remove(UID uid) {
        E ret = getEntityManager().find(entityClass, uid, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (ret != null) {
            remove(ret);
            return true;
        }
        return false;
    }

    public ICalendar getICalendar(UID[] restrict) {
        ICalendarBuilder cb = new ICalendarBuilder();
        try {
            cb.addProperty(CalendarComponent.PRODID, PropertyNames.CAL_PRODID);
            cb.addProperty(CalendarComponent.VERSION, "2.0");
            addPropertiesToICalendarBody(cb);
        } catch (InvalidComponentException ex) {
        }
        addComponentsToICalendar(cb, restrict);
        return cb.toICalendar();
    }

    protected void addPropertiesToICalendarBody(ICalendarBuilder cb) throws InvalidComponentException {
    }

    protected void addComponentsToICalendar(final ICalendarBuilder cb, final UID[] restrict) {
        for (final UniqueCalendarComponentEntity c : getCalendar().getComponents()) { //Lambda not supported!!
            if (restrict == null || Arrays.stream(restrict).anyMatch(uid -> uid.equals(c.getUID()))) {
                try {
                    addComponentToICalendar(cb, entityClass.cast(c));
                } catch (ClassCastException e) {
                } catch (InvalidComponentException incex) {
                    Logger.getLogger(FixedCalendarFacade.class.getName()).log(Level.WARNING, incex.getMessage(), incex);
                }
            }
        }
    }

    protected void addComponentToICalendar(ICalendarBuilder cb, E c) throws InvalidComponentException {
        CalendarComponentBuilder ccb = cb.addComponent(c.getName(), c.getUID());
        addEntityPropertiesToComponent(ccb, c);
        List<EmbeddableComponentProperty> l = c.getProperties();
        for (EmbeddableComponentProperty ccp : l) {
            List<Parameter> el = ccp.getParameters();
            if (el.isEmpty()) {
                ccb.addProperty(ccp.getName(), ccp.getValue());
            } else {
                Parameter[] pp = el.stream().toArray(s -> new Parameter[s]);
                ccb.addProperty(ccp.getName(), ccp.getValue(), pp);
            }
        }
    }

    protected void addEntityPropertiesToComponent(CalendarComponentBuilder ccb, E c) throws InvalidComponentException {
        final Parameter[] spp = c.getSummaryParameters().getList().stream()
                .toArray(Parameter[]::new);
        ccb.addProperty(CalendarComponentProperty.DTSTART, IComponentUtilities.DATE_TIME.format(c.getDtstart()))
                .addProperty(CalendarComponentProperty.DTEND, c.getDtend() != null ? IComponentUtilities.DATE_TIME.format(c.getDtend()) : null)
                .addProperty(CalendarComponentProperty.DURATION, c.getDuration())
                .addProperty(CalendarComponentProperty.SUMMARY, c.getSummary(), spp)
                .addProperty(CalendarComponentProperty.STATUS, c.getStatus())
                .addProperty(CalendarComponentProperty.PRIORITY, c.getPriority() != null ? Integer.toString(c.getPriority()) : null);
        final List<EmbeddableComponentProperty> properties = c.getProperties();
        for (final EmbeddableComponentProperty p : properties) {
            if (CalendarComponentProperty.CATEGORIES.equals(p.getName())) {
                final Parameter[] params = p.getParameters().stream().map(par -> new Parameter(par.getName(), par.getValue())).toArray(Parameter[]::new);
                ccb.addProperty(CalendarComponentProperty.CATEGORIES, p.getValue(), params);
            }
        }
    }

    protected void updateComponentProperties(final CalendarComponent cc, final UniqueCalendarComponentEntity te) throws RuntimeException {
        for (final CalendarComponentProperty ccp : cc.getProperties()) {
            final String name = ccp.getName();
           final String value = ccp.getValue();
           final List<Parameter> params = ccp.getParameters();
            if (null != name) {
                switch (name) {
                    case CalendarComponentProperty.DTSTART:
                        try {
                            te.setDtstart(IComponentUtilities.DATE_TIME.parse(value));
                        } catch (ParseException ex) {
                            throw new RuntimeException(ex);
                        }
                        break;
                    case CalendarComponentProperty.DTEND:
                        try {
                            te.setDtend(IComponentUtilities.DATE_TIME.parse(value));
                        } catch (ParseException ex) {
                            throw new RuntimeException(ex);
                        }
                        break;
                    case CalendarComponentProperty.DURATION:
                        te.setDuration(value);
                        break;
                    case CalendarComponentProperty.SUMMARY:
                        te.setSummary(value);
                        ccp.getParameters().stream()
                                .forEach(p -> te.getSummaryParameters().add(p));
                        break;
                    case CalendarComponentProperty.STATUS:
                        te.setStatus(value);
                        break;
                    case CalendarComponentProperty.PRIORITY:
                        try {
                            te.setPriority(Integer.parseInt(value));
                        } catch (NumberFormatException ex) {
                            throw new RuntimeException(ex);
                        }
                        break;
                    default:
                        updateNonStandardComponentProperty(name, value, params, te);
                }
            }
        }
    }

    protected void updateNonStandardComponentProperty(final String name, final String value, final List<Parameter> l, final UniqueCalendarComponentEntity te) {
        te.addProperty(name, value, l);
    }
}
