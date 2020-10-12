/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web.docsrv;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchEntityException;
//import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.faces.bean.SessionScoped;
//import javax.faces.bean.SessionScoped;
//import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.fop.apps.MimeConstants;
import org.thespheres.betula.Identity;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.web.WebUIConfiguration;
import org.thespheres.betula.niedersachsen.NdsTerms;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.server.beans.annot.DocumentsRequest;
import org.thespheres.betula.server.beans.annot.Preceding;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.scheme.spi.TermNotFoundException;
import org.thespheres.betula.web.config.WebAppProperties;

/**
 *
 * @author boris.heithecker
 */
@SessionScoped
//@SessionScoped
public class ZeugnisServlet extends HttpServlet {

    @EJB
    private NdsFormatter fOPFormatter;
//    @EJB //Cannot inject stateful bean, will be destroy permanently (!) after EJBAccessException
//    private FastTargetDocuments targets;
//    @Inject//Cannot inject stateful bean, will be destroy permanently (!) after EJBAccessException
//    private FastTargetDocuments targets;
//    @Inject
//    private FastTargetDocuments2 targets2;
    @EJB
    DocumentMapper documentMapper;
    @Current
    @Inject
    private Term currentTerm;
    @Preceding
    @Inject
    private Term beforeTerm;
    @Default
    @Inject
    private NamingResolver namingResolver;
    @Inject
    WebUIConfiguration webConfig;
    @Inject
    private DocumentsModel docModel;
    @EJB
    private StudentsListsLocalBean sllb;
//    @DocumentsSession
//    @Inject
//    private Instance<FastTargetDocuments2> ftd2SessionInstance;
    @DocumentsRequest
    @Inject
    private Instance<FastTargetDocuments2> ftd2RequestInstance;

    private FastTargetDocuments2 getFastTargetDocuments2() {
        return ftd2RequestInstance.get();
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

//    @Override
//    public void init() throws ServletException {
//        super.init(); //To change body of generated methods, choose Tools | Templates.
////        webConfig = SystemProperties.findWebUIConfiguration();
//        fOPFormatter.initialized();
//    }
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        UnitId pu = Utilities.extractUnitId(request);
        final StudentId student = Utilities.extractStudentId(request);
        String mime = MimeConstants.MIME_PDF;
        final String mimePar;
        if ((mimePar = request.getParameter("mime")) != null) {
            mime = URLDecoder.decode(mimePar, "utf-8");
        }
        String encoding = "utf-8";
        final String encPar;
        if ((encPar = request.getParameter("encoding")) != null) {
            encoding = URLDecoder.decode(encPar, "utf-8");
        }
        final String parameter = request.getParameter("document");
        //Creates a new one.........
//        FastTargetDocuments mtad = targets;
        final FastTargetDocuments2 ftd2 = getFastTargetDocuments2();
        try {
            if (pu != null && parameter != null && parameter.equals("betula.primaryUnit.allLists")) {
//                final FastTargetDocuments2 mtad = lookupFastTargetDocumentsImplLocal();
                liste(request, pu, response, ftd2, mime, encoding);
            } else if (pu != null && parameter != null && parameter.equals("betula.primaryUnit.details")) {
                details(request, pu, response);
            } else if (parameter != null && parameter.equals("betula.target")) {
//                final FastTargetDocuments2 mtad = lookupFastTargetDocumentsImplLocal();
                target(request, response, mimePar, ftd2, encoding);
            } else if (pu != null || student != null) {
//                final FastTargetDocuments2 mtad = lookupFastTargetDocumentsImplLocal();
                zeugnis(request, pu, student, mime, response, ftd2);
            }
        } catch (EJBAccessException aex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (NoSuchEntityException noex) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    private void target(HttpServletRequest request, HttpServletResponse response, String mime, FastTargetDocuments2 mtad, String enc) throws IOException, ServletException {
//        NamingResolver namingResolver = SystemProperties.findNamingResolver();
        final TermId termId = Utilities.extractTermId(request);
        final DocumentId target = Utilities.extractDocumentId(request);
        if (target == null) {
            throw new ServletException();
        }

        final DocumentId[] arr;
        Identity<String> display = target;
        boolean joinTargets = false;
//        String jtp;
//        if ((jtp = request.getParameter("joinTargets")) != null) {
//            joinTargets = Boolean.valueOf(jtp);
//        }
//        if (joinTargets) {
//            final DocumentId base = docModel.convert(target);
//            JoinedUnitsEntry joinedUnits = mtad.getJoinedUnits(base);
//            if (joinedUnits == null) {
//                throw new ServletException();
//            }
//            display = joinedUnits.getJoinUnit();
//            arr = Arrays.stream(joinedUnits.getJoinedUnits())
//                    .map(u -> new DocumentId(u.getAuthority(), u.getId() + "-" + webConfig.getDefaultCommitTargetType(), DocumentId.Version.LATEST))
//                    .toArray(DocumentId[]::new);
//        } else {
        arr = new DocumentId[]{target};
//        }
//        this.mtad.getJoinedUnits(null)
        Term term = null;
        Term[] before = null;
        if (termId != null) {
            try {
                term = NdsTerms.fromId(termId);
                before = new Term[3];
                int id = termId.getId() - 3;
                for (int i = 0; i < 3; i++) {
                    TermId tid = new TermId(termId.getAuthority(), id++);
                    before[i] = NdsTerms.fromId(tid);
                }
            } catch (IllegalAuthorityException ex) {
            }
        }
        if (term == null) {
            term = currentTerm; //SystemProperties.terms()[1];
            before = new Term[]{beforeTerm}; //new Term[]{SystemProperties.terms()[0]};
        }
        if (MimeConstants.MIME_PDF.equals(mime)) {
            byte[] out = fOPFormatter.formatKursListe(mtad, arr, namingResolver, term, before, mime, display);
            response.setContentType(mime);
            response.getOutputStream().write(out);
        } else if ("text/csv".equals(mime)) {
            byte[] out = fOPFormatter.formatCSVListe(mtad, arr, namingResolver, term, before, mime, enc);
            response.setContentType(mime);
            response.getOutputStream().write(out);
        }
    }

    private void liste(HttpServletRequest request, UnitId pu, HttpServletResponse response, FastTargetDocuments2 mtad, String mime, String encoding) throws IOException {
        TermId termId = Utilities.extractTermId(request);
        Term term = null;
        if (termId != null) {
            try {
                term = NdsTerms.fromId(termId);
            } catch (IllegalAuthorityException ex) {
            }
        }
        Term before = null;
        if (term == null) {
            term = currentTerm;
        }
        if (before == null && term.equals(currentTerm)) {
            before = beforeTerm;
        } else {
            //TODO: extract beforeTerm from request.
        }
        final Collection<DocumentId> docs = mtad.getTargetAssessmentDocuments(pu);
        if (mime.equals(org.apache.xmlgraphics.util.MimeConstants.MIME_PLAIN_TEXT)) {
            byte[] out = fOPFormatter.csv(mtad, pu, documentMapper.getDocMap(docs, false), webConfig.getCommitTargetTypes(), namingResolver, term, before, encoding);
            response.setContentType("application/zip");
            response.getOutputStream().write(out);
        } else {
            byte[] out = fOPFormatter.formatListe(mtad, pu, documentMapper.getDocMap(docs, false), webConfig.getCommitTargetTypes(), namingResolver, term, before);
            response.setContentType(org.apache.xmlgraphics.util.MimeConstants.MIME_PDF);
            response.getOutputStream().write(out);
        }
    }

    private void details(HttpServletRequest request, UnitId pu, HttpServletResponse response) throws IOException {
        final String mime = org.apache.xmlgraphics.util.MimeConstants.MIME_PDF;
        final TermId termId = Utilities.extractTermId(request);
        final Term term;
        if (termId != null) {
            try {
                term = NdsTerms.fromId(termId);
            } catch (IllegalAuthorityException ex) {
                throw new IOException(ex);
            }
        } else {
            term = currentTerm;
        }
        //
//        final FastTargetDocuments2 tgtae = lookupFastTargetDocumentsImplLocal();
        final String ptcprop = request.getParameter(WebAppProperties.FORMAT_DETAILS_LISTS_PRETERMS_COUNT_PROPERTY);
        int preTermsCount = Integer.getInteger(WebAppProperties.FORMAT_DETAILS_LISTS_PRETERMS_COUNT_PROPERTY, 1);
        if (ptcprop != null) {
            int parsed;
            try {
                parsed = Integer.parseInt(ptcprop);
                if (parsed > 0) {
                    preTermsCount = parsed;
                }
            } catch (NumberFormatException nfex) {
                Logger.getLogger(ZeugnisServlet.class.getName()).log(Level.WARNING, nfex.getLocalizedMessage(), nfex);
            }
        }
        final Term[] terms = new Term[preTermsCount + 1];
        terms[preTermsCount] = currentTerm;
        for (int i = 0; i < preTermsCount; i++) {
            try {
                terms[i] = currentTerm.getSchedule().resolve(new TermId(currentTerm.getScheduledItemId().getAuthority(), currentTerm.getScheduledItemId().getId() - (preTermsCount - i)));
//                terms[1] = current.getSchedule().resolve(new TermId(current.getScheduledItemId().getAuthority(), current.getScheduledItemId().getId() - 1));
//                terms[0] = current.getSchedule().resolve(new TermId(current.getScheduledItemId().getAuthority(), current.getScheduledItemId().getId() - 2));
            } catch (TermNotFoundException | IllegalAuthorityException ex) {
                throw new IOException(ex);
            }
        }
        final FastTargetDocuments2 ftd2 = getFastTargetDocuments2();
        //Grades
        final Map<DocumentId, FastTermTargetDocument> targets = new HashMap<>();
        final Map<DocumentId, FastTermTargetDocument> agTargets = new HashMap<>();
        final Map<TermId, Map<String, Map<MultiSubject, Set<DocumentId>>>> map = new HashMap<>();
        for (final Term t : terms) {
            final Collection<DocumentId> coll;
            try {
                coll = ftd2.getTargetAssessmentDocumentsForTerm(pu, t.getScheduledItemId(), targets);
            } catch (EJBException | PersistenceException e) {
                throw new IOException(e);
            }
            final Map<String, Map<MultiSubject, Set<DocumentId>>> m = documentMapper.getDocMap(coll, false);
            map.put(t.getScheduledItemId(), m);
            if (currentTerm.equals(t)) {
                final Set<DocumentId> ag = documentMapper.filterAGs(coll);
                ag.stream()
                        .forEach(d -> agTargets.put(d, ftd2.getFastTermTargetDocument(d)));
            }
        }
        //Texts
        final Map<DocumentId, FastTextTermTargetDocument> textData = new HashMap<>();
        final Map<TermId, Map<String, Map<MultiSubject, Set<DocumentId>>>> textDocMap = new HashMap<>();
        for (final Term t : terms) {
            final Collection<DocumentId> coll;
            try {
                coll = ftd2.getTextTargetAssessmentDocumentsForTerm(pu, t.getScheduledItemId(), textData);
            } catch (final EJBException | PersistenceException e) {
                throw new IOException(e);
            }
            final Map<String, Map<MultiSubject, Set<DocumentId>>> m = documentMapper.getDocMap(coll, false);
            textDocMap.put(t.getScheduledItemId(), m);
        }
        
        final Collection<StudentId> students = ftd2.getStudents(pu, null);

//        final byte[] out = fOPFormatter.formatDetails(tgtae, map, ag, pu, term, mime, preTermsCount);
        final byte[] out = fOPFormatter.formatDetails(students, targets, agTargets, map, pu, term, mime, preTermsCount, textDocMap, textData); //formatDetails(students, fttd, agTargets, pu, term, mime, preTermsCount);
        response.setContentType(mime);
        response.getOutputStream().write(out);
    }

    private void zeugnis(HttpServletRequest request, UnitId pu, StudentId student, String mime, HttpServletResponse response, FastTargetDocuments2 mtad) throws IOException {
        final DocumentId zgnId = Utilities.extractDocumentId(request);
        TermId term = Utilities.extractTermId(request);
        if (term == null) {
            term = currentTerm.getScheduledItemId(); //SystemProperties.terms()[1].getScheduledItemId();
        }
        Date asOf = null;
        final LocalDate asOfDate = Utilities.extractLocalDate(request, "students.set.date");
        if (asOfDate != null) {
            asOf = Date.from(asOfDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (term == null) {
            term = currentTerm.getScheduledItemId(); //SystemProperties.terms()[1].getScheduledItemId();
        }
        if (pu == null && student != null) {
            final Date useDate = asOf != null ? asOf : fOPFormatter.termEnd(term);
            pu = sllb.findPrimaryUnit(student, useDate);
        }
        if (pu == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return; //exception!!
        }

        final Collection<DocumentId> docs = mtad.getTargetAssessmentDocuments(pu);//   unitDocumentBeanRemote.getTargetAssessmentDocuments(pu);
        final Map<String, Map<MultiSubject, Set<DocumentId>>> dm = documentMapper.getDocMap(docs, false);
        final Map<MultiSubject, Set<DocumentId>> docMap = dm.get(webConfig.getDefaultCommitTargetType());
        if (docMap == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return; //exception!!
        }
        final Map<MultiSubject, Set<DocumentId>> reports = dm.get("berichte");
        byte[] out = fOPFormatter.formatReports(mtad, pu, docMap, reports, zgnId, term, student, asOf, mime);
        response.setContentType(mime);
        response.getOutputStream().write(out);
    }

//    private FastTargetDocuments2 lookupFastTargetDocumentsImplLocal() {
//        try {
//            Context c = new InitialContext();
//            return (FastTargetDocuments2) c.lookup("java:global/Betula_Server/Betula_Persistence/FastTargetDocuments2Facade!org.thespheres.betula.entities.localbeans.FastTargetDocuments2Facade");
//        } catch (NamingException ne) {
//            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
//            throw new RuntimeException(ne);
//        }
//    }
//    private SessionContext getSessionContext() {
//        InitialContext ic;
//        try {
//            ic = new InitialContext();
//            return (SessionContext) ic.lookup("java:comp/EJBContext");
//        } catch (NamingException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
}
