/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.beanimpl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.AbstractCalendarComponent;
import org.thespheres.betula.calendar.EmbeddableComponentProperty;
import org.thespheres.betula.calendar.facade.ZeugniskonferenzFacade;
import org.thespheres.betula.calendar.reports.ZeugniskonferenzEntity;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.niedersachsen.zeugnis.Constants;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.betula.server.beans.CalendarsBean;
import org.thespheres.betula.services.ical.CalendarResourceProvider;
import org.thespheres.ical.CalendarComponent;
import org.thespheres.ical.CalendarComponentProperty;
import org.thespheres.ical.InvalidComponentException;
import org.thespheres.ical.util.IComponentUtilities;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@Stateless
public class CalendarsBeanImpl implements CalendarsBean {

    @EJB(beanName = "ZeugniskonferenzFacadeImpl")
    private ZeugniskonferenzFacade zkFacade;
    @Default
    @Inject
    private CalendarResourceProvider calResource;

    @Override
    public Date getDate(final String category, final UnitId unit, final TermId termId, final DocumentId zgn, final String[] categories) throws AmbiguousDateException {
        final ZeugniskonferenzEntity ze = find(category, unit, termId, zgn, categories);
        if (Constants.CATEGORY_ZEUGNISKONFERENZ.equals(category) && ze != null) {
            return ze.getDtstart();
        } else if (Constants.CATEGORY_ZEUGNISAUSGABE.equals(category)) {
            if (ze != null) {
                return ze.getDtstart();
            } else if (termId != null) {
                for (CalendarComponent ccp : calResource.getCalendar().getComponents()) {
                    Set<String> cat;
                    try {
                        cat = IComponentUtilities.parseCategories(ccp);
                        if (!cat.contains(Constants.CATEGORY_ZEUGNISAUSGABE)) {
                            continue;
                        }
                        final TermId term;
                        if ((term = parseTermId(ccp)) != null && term.equals(termId)) {
                            return IComponentUtilities.parseDateProperty(ccp, CalendarComponentProperty.DTSTART);
                        }
                    } catch (InvalidComponentException ex) {
                        Logger.getLogger(CalendarsBeanImpl.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    private TermId parseTermId(CalendarComponent ccp) {
        CalendarComponentProperty p = ccp.getAnyProperty("X-TERM");
        if (p != null && p.getAnyParameter("x-authority").isPresent()) {
            try {
                int tid = Integer.parseInt(p.getValue());
                return new TermId(p.getAnyParameter("x-authority").get(), tid);
            } catch (NumberFormatException nfex) {
                Logger.getLogger(CalendarsBeanImpl.class.getName()).log(Level.WARNING, nfex.getLocalizedMessage(), nfex);
            }
        }
        return null;
    }

    private static boolean matchCategories(final ZeugniskonferenzEntity e, final String[] moreCategories) {
        final Set<String> cc = e.getProperties().stream()
                .filter(p -> p.getName().equals(CalendarComponentProperty.CATEGORIES))
                .map(EmbeddableComponentProperty::getValue)
                .flatMap(p -> Arrays.stream(p.split(",")))
                .map(String::trim)
                .collect(Collectors.toSet());
        return moreCategories.length == cc.size() && Arrays.stream(moreCategories).allMatch(cc::contains);
    }

    private ZeugniskonferenzEntity find(final String category, final UnitId unit, final TermId termId, final DocumentId report, final String[] moreCategories) throws AmbiguousDateException {
        final List<ZeugniskonferenzEntity> r = zkFacade.findZeugnisEvents(unit, termId, category);
        final List<ZeugniskonferenzEntity> l1 = r.stream()
                .filter(e -> e.getRelatedReports().stream().anyMatch(rr -> rr.getDocumentId().equals(report)))
                .collect(Collectors.toList());
        if (!l1.isEmpty()) {
            if (l1.size() == 1) {
                return l1.iterator().next();
            } else {
                final Date[] dd = l1.stream()
                        .map(AbstractCalendarComponent::getDtstart)
                        .toArray(Date[]::new);
                throw new AmbiguousDateException(dd);
            }
        }
        final List<ZeugniskonferenzEntity> l2 = r.stream()
                .filter(e -> e.getRelatedReports().isEmpty())
                .filter(e -> moreCategories == null || matchCategories(e, moreCategories))
                .collect(Collectors.toList());
        switch (l2.size()) {
            case 0:
                return null;
            case 1:
                return l2.iterator().next();
            default:
                final Date[] dd = l2.stream()
                        .map(AbstractCalendarComponent::getDtstart)
                        .toArray(Date[]::new);
                throw new AmbiguousDateException(dd);
        }
    }
}
