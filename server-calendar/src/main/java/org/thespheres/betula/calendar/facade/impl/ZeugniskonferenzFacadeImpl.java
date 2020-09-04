/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.facade.impl;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.UniqueCalendarComponentEntity;
import org.thespheres.betula.calendar.facade.CalendarCompatibilities;
import org.thespheres.ical.builder.ICalendarBuilder.CalendarComponentBuilder;
import org.thespheres.betula.calendar.facade.ZeugniskonferenzFacade;
import org.thespheres.betula.calendar.reports.EmbeddableStringDocumentIdMapValue;
import org.thespheres.betula.calendar.tickets.TicketsCalendar;
import org.thespheres.betula.calendar.reports.ZeugniskonferenzEntity;
import org.thespheres.betula.calendar.util.EmbeddableTermId;
import org.thespheres.betula.calendar.util.EmbeddableUnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.niedersachsen.zeugnis.Constants;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.CalendarComponentProperty;
import org.thespheres.ical.ICalendar;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.Parameter;
import org.thespheres.ical.UID;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class ZeugniskonferenzFacadeImpl extends FixedCalendarFacade<TicketsCalendar, ZeugniskonferenzEntity> implements ZeugniskonferenzFacade {

    @EJB
    private TicketsFacadeImpl ticketsFacadeImpl;

    public ZeugniskonferenzFacadeImpl() {
        super(ZeugniskonferenzEntity.class);
    }

    @Override
    public ZeugniskonferenzEntity find(UID id, LockModeType lmt) {
        return super.find(id, lmt);
    }

    @Override
    protected TicketsCalendar getCalendar() {
        return ticketsFacadeImpl.getCalendar();
    }

    @Override
    public List<ZeugniskonferenzEntity> findZeugnisEvents(UnitId unit, TermId term, String category) {
        if (Objects.equals(Constants.CATEGORY_ZEUGNISKONFERENZ, category)) {
            return getEntityManager().createNamedQuery("findZeugniskonferenzCategoryNull", ZeugniskonferenzEntity.class)
                    .setParameter("unit", new EmbeddableUnitId(unit))
                    .setParameter("term", new EmbeddableTermId(term))
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();
        } else {
            return getEntityManager().createNamedQuery("findZeugniskonferenz", ZeugniskonferenzEntity.class)
                    .setParameter("unit", new EmbeddableUnitId(unit))
                    .setParameter("term", new EmbeddableTermId(term))
                    .setParameter("category", category)
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList();
        }
    }

    @Override
    public ICalendar getZeugnisCalendar(UnitId unit, TermId term) {
        UID[] restrict;
        if (unit != null) {
            if (term != null) {
                restrict = getEntityManager().createNamedQuery("findZeugniskonferenz", ZeugniskonferenzEntity.class)
                        .setParameter("unit", new EmbeddableUnitId(unit))
                        .setParameter("term", new EmbeddableTermId(term))
                        .setLockMode(LockModeType.OPTIMISTIC)
                        .getResultList().stream()
                        .map(ZeugniskonferenzEntity::getUID)
                        .distinct()
                        .toArray(UID[]::new);
            } else {
                restrict = getEntityManager().createNamedQuery("findZeugniskonferenzenForUnit", ZeugniskonferenzEntity.class)
                        .setParameter("unit", new EmbeddableUnitId(unit))
                        .setLockMode(LockModeType.OPTIMISTIC)
                        .getResultList().stream()
                        .map(ZeugniskonferenzEntity::getUID)
                        .distinct()
                        .toArray(UID[]::new);
            }
        } else {
            restrict = getEntityManager().createNamedQuery("findZeugniskonferenzen", ZeugniskonferenzEntity.class)
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getResultList().stream()
                    .map(ZeugniskonferenzEntity::getUID)
                    .distinct()
                    .toArray(UID[]::new);
        }
        return getICalendar(restrict, null);
    }

    @Override
    public ZeugniskonferenzEntity create(final UnitId unit, final TermId term, final List<DocumentId> reports, final CalendarComponent comp, final Set<String> categories) {
        final ZeugniskonferenzEntity ze;
        try {
            ze = ZeugniskonferenzEntity.create(getCalendar(), term, unit);
            getEntityManager().persist(ze);
        } catch (IOException ex) {
            throw new RuntimeException("Could not automatically create new UID for component.", ex);
        }
        if (categories.contains(Constants.CATEGORY_ZEUGNISAUSGABE)) {
            ze.setCategory(Constants.CATEGORY_ZEUGNISAUSGABE);
        }
        updateComponentProperties(comp, ze, categories);

//        ze.setDateOfIssuance(ze);//TODO
        reports.stream()
                .filter(r -> ze.getRelatedReports().stream().noneMatch(e -> e.getDocumentId().equals(r)))
                .map(r -> new EmbeddableStringDocumentIdMapValue(null, r))
                .forEachOrdered(ze.getRelatedReports()::add);
        comp.getAnyPropertyValue(CalendarComponentProperty.LOCATION).ifPresent(ze::setLocation);
        return getEntityManager().merge(ze);
    }

    private void updateComponentProperties(final CalendarComponent cc, final ZeugniskonferenzEntity ze, final Set<String> category) {
        super.updateComponentProperties(cc, ze);
        final String cat = category.stream()
                .filter(c -> !c.equals(Constants.CATEGORY_ZEUGNISAUSGABE) && !c.equals(Constants.CATEGORY_ZEUGNISKONFERENZ))
                .collect(Collectors.joining(","));
        if (!cat.isEmpty()) {
            ze.addProperty(CalendarComponentProperty.CATEGORIES, cat);
        }
    }

    //Prevent updating non-standard categories
    @Override
    protected void updateNonStandardComponentProperty(final String name, final String value, final List<Parameter> l, final UniqueCalendarComponentEntity te) {
    }

    @Override
    public boolean update(final UID uid, final CalendarComponent comp, final Set<String> cat) {
        final ZeugniskonferenzEntity ze = find(uid, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (ze != null) {
            updateComponentProperties(comp, ze, cat);
            getEntityManager().merge(ze);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(UID uid) {
        final ZeugniskonferenzEntity ze = find(uid, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (ze != null) {
            super.remove(uid);
            getEntityManager().remove(ze);
            getEntityManager().merge(getCalendar());
            return true;
        }
        return false;
    }

    //TODO: This is never called!!!! ---> remove?
    @Override
    protected void addEntityPropertiesToComponent(final CalendarComponentBuilder ccb, final ZeugniskonferenzEntity zke, final CalendarCompatibilities compat) throws InvalidComponentException {
        super.addEntityPropertiesToComponent(ccb, zke, compat);
        if (zke.getLocation() != null) {
            ccb.addProperty(CalendarComponentProperty.LOCATION, zke.getLocation());
        }
        ccb.addProperty(CalendarComponentProperty.CATEGORIES, Constants.CATEGORY_ZEUGNISKONFERENZ)
                .addProperty(CalendarComponentProperty.STATUS, "CONFIRMED")
                .addProperty("X-UNIT", zke.getUnit().getId(), new Parameter("x-authority", zke.getUnit().getAuthority()))
                .addProperty("X-TERM", Integer.toString(zke.getTerm().getId()), new Parameter("x-authority", zke.getTerm().getAuthority()));
        for (final EmbeddableStringDocumentIdMapValue v : zke.getRelatedReports()) {
            final DocumentId rid = v.getDocumentId();
            ccb.addProperty("X-DOCUMENT", rid.getId(), new Parameter("x-authority", rid.getAuthority()), new Parameter("x-version", rid.getVersion().getVersion()));
        }
    }

}
