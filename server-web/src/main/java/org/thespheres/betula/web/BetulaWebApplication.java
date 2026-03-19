/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.primefaces.PrimeFaces;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.server.beans.annot.Preceding;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.web.config.Extra;
import org.thespheres.betula.web.docsrv.DocumentMapper;
import org.thespheres.ical.VCard;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.web.config.AppConfiguration;
import org.thespheres.betula.web.rest.DocumentsService;

/**
 *
 * @author boris.heithecker
 */
//@RolesAllowed("signee")
//@RolesPermitted("signee")
@Named("app")
@ViewScoped//@SessionScoped //Vor Jakarta: javax.faces.bean.SessionScoped;
public class BetulaWebApplication implements Serializable {

//    @DocumentsSession
//    @Inject
//    private FastTargetDocuments2 bean;
    @Any
    @Inject
    private Instance<VorschlagDecoration> extraAssessment;
    @EJB(beanName = "StudentVCardsImpl")
    private StudentsLocalBean studentVCardsImpl;
    @EJB(beanName = "StudentsListsLocalBeanImpl")
    private StudentsListsLocalBean studentsLists;
    @Inject
    private DocumentMapper documentMapper;
    @Inject
    private EventDispatch eventDispatch;
    @Default
    @Inject
    private NamingResolver namingResolver;
    @Current
    @Inject
    private Term currentTerm;
    @Preceding
    @Inject
    private Term beforeTerm;
    @Inject
    private DocumentsModel docModel;
    private String activePage = "";
    @Inject
    private ApplicationUser user;
    @Inject
    private AppConfiguration config;
    @Inject
    private CommonDocuments commonDocuments;
    @Inject
    private LocalProperties properties;
    @Inject
    private DocumentsService service;

    //Move to User
    private final Map<DocumentId, FastTermTargetDocument> fastDocs = new HashMap<>();
    private final Map<DocumentId, FastTextTermTargetDocument> fastTextDocs = new HashMap<>();

    public AppConfiguration getAppConfiguration() {
        return config;
    }

    public DocumentsService getService() {
        return service;
    }

    public String getActivePage() {
        return activePage;
    }

    public void setActivePage(final String page) {
        activePage = page;
    }

    public void navigateTo(final String page) {
        activePage = page;
    }

    Optional<PrimaryUnit> getActivePrimaryUnit() {
        final String prefix = "primaryUnits_";
        if (getActivePage().startsWith(prefix)) {
            int index = Integer.parseInt(getActivePage().substring(prefix.length()));
            return Optional.of(user.getPrimaryUnits()[index]);
        }
        return Optional.empty();
    }

    public NamingResolver getNamingResolver() {
        return namingResolver;
    }

    public CommonDocuments getCommonDocuments() {
        return commonDocuments;
    }

    public LocalProperties getProperties() {
        return properties;
    }

    public void showMessage(String summary, String detail) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail);
        PrimeFaces.current().dialog().showMessageDynamic(message);
    }

    public Term getCurrentTerm() {
        return currentTerm;
    }

    public Term getTermBefore() {
        return beforeTerm;
    }

    EventDispatch getEventDispatch() {
        return eventDispatch;
    }

    VCard getVCard(StudentId student) {
        return studentVCardsImpl.get(student);
    }

//    Collection<DocumentId> getDocuments() { //Signee signee) {
//        return bean.getTargetAssessmentDocuments();  //findTargetAssessmentDocuments(signee);
//    }
    FastTermTargetDocument getFastDocument(DocumentId id) {
        return fastDocs.computeIfAbsent(id, d -> service.getFastTermTargetDocument(id));
    }

    FastTextTermTargetDocument getFastTextDocument(final DocumentId id) {
        return fastTextDocs.computeIfAbsent(id, d -> service.getFastTextTermTargetDocument(id));
    }

    Collection<DocumentId> getTargetAssessmentDocuments(UnitId primaryUnit) {
        return service.getTargetAssessmentDocuments(primaryUnit);
    }

//    Collection<StudentId> getStudents(final String docIdName) { //Signee signee) {
//        return bean.getPrimaryUnitStudents(docIdName);
//    }
    Marker getStudentMarkerEntry(StudentId sid, DocumentId studentSGLMarkerDocId) {
        return studentsLists.getMarkerEntry(sid, studentSGLMarkerDocId, null);
    }

    DocumentMapper getDocumentMapper() {
        return documentMapper;
    }

    DocumentsModel getDocumentsModel() {
        return docModel;
    }

    UnitId getPrimaryUnit(final String docIdName) {
        final DocumentId klDoc = commonDocuments.forName(docIdName);
        final String unit = config.getInternalClient().getSigneePrimaryUnit(klDoc, user.getSignee());
        return UnitId.valueOf(unit);
//        return service.getPrimaryUnit(docIdName);
    }

    //Internal
    Grade selectGrade(DocumentId docId, TermId termId, StudentId studId) throws IOException {
        final String g = config.getInternalClient().selectSingle(docId, studId, termId); // bean.select(docId, studId, termId);
        return GradeFactory.resolve(g);
    }

    //Internal
    boolean submitGrade(DocumentId docId, TermId termId, StudentId studId, Grade grade) throws IOException {
        final String r = config.getInternalClient().submitSingleGradeValue(docId, studId, termId, grade); //submit(docId, studId, termId, grade, new Timestamp());
        return Boolean.parseBoolean(r);
    }

    //Internal
    boolean submitText(final DocumentId docId, final TermId termId, final Marker section, final StudentId studId, final String text) {
        final String r = config.getInternalClient().submitSingleTextValue(docId, studId, termId, null, text);
        return Boolean.parseBoolean(r);
    }

    //Internal
    Ticket[] findApplicableTickets(DocumentId docId, TermId termId, StudentId studId) {
        final String t = config.getInternalClient().findApplicableTickets(docId, termId, studId);
        return Arrays.stream(t.split("\n"))
                .map(Ticket::valueOf)
                .toArray(Ticket[]::new);
    }

    VorschlagDecoration getAssessmentDecoration(Extra extra) {
        Instance<VorschlagDecoration> select = extraAssessment.select(extra);
        return (!select.isUnsatisfied() && !select.isAmbiguous()) ? select.get() : null;
    }

    public void logout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();

        try {
            // Logout from Jakarta Security (if using container-managed security)
            request.logout();

            // Invalidate the session
            externalContext.invalidateSession();

            // Redirect to context root
            String contextPath = externalContext.getRequestContextPath();
            externalContext.redirect(contextPath);

            facesContext.responseComplete();

        } catch (ServletException | IOException e) {
            // Log the error properly
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Logout failed", e);

            // Show error message to user
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Logout Error", "Unable to logout. Please try again.");
            facesContext.addMessage(null, message);
        }
    }
}
