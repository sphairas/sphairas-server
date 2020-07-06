/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.niedersachsen.NdsTerms;
import org.thespheres.betula.niedersachsen.zeugnis.Constants;
import org.thespheres.betula.niedersachsen.zeugnis.NdsReportConstants;
import org.thespheres.betula.server.beans.AmbiguousDateException;
import org.thespheres.betula.server.beans.CalendarsBean;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
@EJB(name = "java:global/Betula_Web/ZeugnisArguments!org.thespheres.betula.web.docsrv.ZeugnisArguments", beanInterface = ZeugnisArguments.class)
@Stateless
public class ZeugnisArguments {

//    @EJB(lookup = "java:global/Betula_Persistence/ZeugnisBeanImpl!org.thespheres.betula.niedersachsen.admin.zgn.ZeugnisBean")
//    private ZeugnisBean zeugnisBean;
    @EJB(beanName = "ReportsBeanImpl")
    private ReportsBean zeugnisBean;
    @EJB(beanName = "CalendarsBeanImpl")
    private CalendarsBean calendars;
    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentCards;
    @Default
    @Inject
    private NamingResolver namingResolver;

    public Object[] getFormatArgs(DocumentId zgn, UnitId unit) {
        if (zgn != null) {
            final StudentId stud = zeugnisBean.getStudent(zgn);
            final TermId termId = zeugnisBean.getTerm(zgn);
            Term term;
            String nSJ = "?";
            String nStufe = "?";
            try {
                term = NdsTerms.fromId(termId);
                int nJahr = (int) term.getParameter(NdsTerms.JAHR) + 1;
                nSJ = Integer.toString(nJahr) + "/" + Integer.toString(nJahr + 1).substring(2);
                final NamingResolver.Result r = namingResolver.resolveDisplayNameResult(unit);
                r.addResolverHint("naming.only.level");
                nStufe = r.getResolvedName(NdsTerms.getTerm(nJahr, 1));
            } catch (IllegalAuthorityException | NumberFormatException ex) {
            }

            if (stud != null) {
                final VCard card = studentCards.get(stud);
                final String n = card.getAnyPropertyValue(VCard.N).get();
                final String given = n.split(";")[1].replace(",", " ");
                final String g = card.getAnyPropertyValue(VCard.GENDER).get();
                final Long gender = "F".equals(g) ? 1l : 0l;
                final String gen = NdsReportConstants.getGenitiv(given);
                final String possesiv = NdsReportConstants.getPossessivPronomen(g);
                final String possesivGen = NdsReportConstants.getPossessivPronomenGenitiv(g);
                Date zkDate;
                try {
                    zkDate = calendars.getDate(Constants.CATEGORY_ZEUGNISKONFERENZ, unit, termId, null, null);
                } catch (AmbiguousDateException ex) {
                    zkDate = null;
                }
                return new Object[]{given, gen, possesiv, zkDate, nStufe, nSJ, gender, possesivGen};
            }
        }
        return new Object[]{};
    }

    //TODO LocalDateTime
    public Date getZeugnisAusgabe(final TermId termId, final UnitId pu, final DocumentId zgn, final boolean isAbschluss) throws AmbiguousDateException {
        final Date ret = calendars.getDate(Constants.CATEGORY_ZEUGNISAUSGABE, pu, termId, zgn, isAbschluss ? new String[]{Constants.ABSCHLUSSZEUGNISSE} : new String[0]);
        return ret != null ? ret : new Date();
    }

}
