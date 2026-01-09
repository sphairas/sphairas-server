/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.faces.application.FacesMessage;
//import jakarta.faces.bean.ManagedBean;
//import jakarta.faces.bean.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.primefaces.PrimeFaces;
//import org.primefaces.PrimeFaces;
//import org.primefaces.context.RequestContext;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.server.beans.annot.DocumentsSession;
import org.thespheres.betula.server.beans.annot.Preceding;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.web.config.Extra;
import org.thespheres.betula.web.docsrv.DocumentMapper;
import org.thespheres.ical.VCard;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.web.config.AppConfiguration;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed("signee")
//@ManagedBean(name = "app")
@Named("app")
@ViewScoped//@SessionScoped //Vor Jakarta: javax.faces.bean.SessionScoped;
public class BetulaWebApplication implements Serializable {

//    @Inject
//    private FastTargetDocuments bean2;
    @DocumentsSession
    @Inject
    private FastTargetDocuments2 bean;
    @Any
    @Inject
    private Instance<VorschlagDecoration> extraAssessment;
//    @Extra(targetType = "arbeitsverhalten")
//    @Inject
//    private VorschlagDecoration avextra;
    @EJB
    private SigneeLocal loginBeanImpl;
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
//    private String currentPrimaryUnit;
    private ApplicationUser currentUser;
//    @Inject
//    private WebUIConfiguration webConfig;
//    @Inject
//    private ZeugnisConfiguratorService zgnConfig;
//    @Inject
//    private Comparator<Subject> subjectComparator;
    @Inject
    private AppConfiguration config;
    @Inject
    private CommonDocuments commonDocuments;
    @Inject
    private LocalProperties properties;
    private final Map<DocumentId, FastTermTargetDocument> fastDocs = new HashMap<>();
    private final Map<DocumentId, FastTextTermTargetDocument> fastTextDocs = new HashMap<>();
//    private final Logger log = Logger.getLogger(BetulaWebApplication.class.getPackage().getName());
//    private Optional<AssessmentConvention> crossMarksAssessmentConvention;    
//    private Optional<String[]> crossMarksSubjectConvention;
//    @Inject
//    private NdsReportBuilderFactory reportBuilderFactory;

    public AppConfiguration getAppConfiguration() {
        return config;
    }

    public ApplicationUser getUser() {
        if (currentUser == null) {
//            FacesContext context = FacesContext.getCurrentInstance();
//            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
//            Object o = request.getSession().getAttribute(UserLogin.ISERV_IMAP_AUTHORIZED_SIGNEE);
//            if (o != null && o instanceof Signee && request.isUserInRole("signee")) {
//                Signee sig = (Signee) o;
            final Signee sig = loginBeanImpl.getSigneePrincipal(false);
//                if (sig.getId().equals(request.getUserPrincipal().getName())) {
            currentUser = new ApplicationUser(this, sig);
//                    RequestContext.getCurrentInstance().execute("PF('notifier').connect('/" + sig.getId() + "')");
//                }
//            }
        }
        return currentUser;
    }

    @PreDestroy
    public void sessionDestroyed() {
        if (currentUser != null) {
//            eventDispatch.unregister(messages);
            currentUser.logout();
            Logger.getLogger(BetulaWebApplication.class.getName()).log(Level.INFO, "LOGGED OUT {0} {1}", new Object[]{currentUser.getSignee().getId(), new Date().toLocaleString()});
        }
    }

    public String getUsername() {
        return getUser() != null ? getUser().getDisplayName() : "";
    }

    public String getActivePage() {
        return activePage;
    }

    public void setActivePage(final String page) {
        activePage = page;
    }

    public void navigateTo(final String page) {
//        this.setCurrentPrimaryUnit(this.currentUser.getPrimaryUnits()[0].getDocumentIdName());
        activePage = page;
    }

    Optional<PrimaryUnit> getActivePrimaryUnit() {
        final String prefix = "primaryUnits_";
        if (getActivePage().startsWith(prefix)) {
            int index = Integer.parseInt(getActivePage().substring(prefix.length()));
            return Optional.of(currentUser.getPrimaryUnits()[index]);
        }
        return Optional.empty();
    }

//    public String getCurrentPrimaryUnit() {
//        return currentPrimaryUnit;
//    }
//
//    public void setCurrentPrimaryUnit(final String currentPrimaryUnit) {
//        this.currentPrimaryUnit = currentPrimaryUnit;
//    }
//    public String getMenuStyle(String menu) {
//        if (menu.equals(getActivePage())) {
//            return "font-weight: bold;";
//        } else {
//            return "";
//        }
//    }

    public NamingResolver getNamingResolver() {
//        if (namingResolver == null) {
//            return namingResolver = SystemProperties.findNamingResolver();
//        }
        return namingResolver;
    }

//    public NdsReportBuilderFactory getReportBuilderFactory() {
//        return reportBuilderFactory;
//    }

//    public ZeugnisConfiguratorService getZeugnisConfiguratorService() {
    ////        if (zgnConfig == null) {
////            return zgnConfig = SystemProperties.findZeugnisConfiguratorService();
////        }
//        return zgnConfig;
//    }

    public CommonDocuments getCommonDocuments() {
        return commonDocuments;
    }

    public LocalProperties getProperties() {
        return properties;
    }

    public void showMessage(String summary, String detail) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail);
        PrimeFaces.current().dialog().showMessageDynamic(message);
//        RequestContext.getCurrentInstance().showMessageInDialog(message);
    }

    public Term getCurrentTerm() {
        return currentTerm; //SystemProperties.terms()[1];
    }

    public Term getTermBefore() {
        return beforeTerm; //SystemProperties.terms()[0];
    }

//    public Messages getMessages() {
//        if (messages == null) {
//            messages = new Messages(this);
//            eventDispatch.register(messages);
//        }
//        return messages;
//    }
    EventDispatch getEventDispatch() {
        return eventDispatch;
    }

    VCard getVCard(StudentId student) {
        return studentVCardsImpl.get(student);
    }

    Collection<DocumentId> getDocuments() { //Signee signee) {
        return bean.getTargetAssessmentDocuments();  //findTargetAssessmentDocuments(signee);
    }

    FastTermTargetDocument getFastDocument(DocumentId id) {
        return fastDocs.computeIfAbsent(id, d -> bean.getFastTermTargetDocument(id));
    }

    FastTextTermTargetDocument getFastTextDocument(final DocumentId id) {
        return fastTextDocs.computeIfAbsent(id, d -> bean.getFastTextTermTargetDocument(id));
    }

    Collection<DocumentId> getTargetAssessmentDocuments(UnitId primaryUnit) {
        return bean.getTargetAssessmentDocuments(primaryUnit);
    }

    Collection<StudentId> getStudents(final String docIdName) { //Signee signee) {
        return bean.getPrimaryUnitStudents(docIdName);
    }

//    FastMessages getFastMessages() {
//        getMessages();//initialize Messages, register listener
//        return fastMessages;
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
        return bean.getPrimaryUnit(docIdName);
    }

    Grade selectGrade(DocumentId docId, TermId termId, StudentId studId) throws IOException {
        return bean.selectSingle(docId, studId, termId);// bean.select(docId, studId, termId);
    }

    boolean submitGrade(DocumentId docId, TermId termId, StudentId studId, Grade grade) throws IOException {
        return bean.submitSingle(docId, studId, termId, grade); //submit(docId, studId, termId, grade, new Timestamp());
    }

    boolean submitText(final DocumentId docId, final TermId termId, final Marker section, final StudentId studId, final String text) {
        return bean.submitSingle(docId, studId, termId, null, text);
    }

    Ticket[] findApplicableTickets(DocumentId docId, TermId termId, StudentId studId) {
        return bean.getTickets(docId, termId, studId);
    }

//    JoinedUnitsEntry getJoinedUnits(DocumentId base) {
//        return bean.getJoinedUnits(base);
//    }
    VorschlagDecoration getAssessmentDecoration(Extra extra) {
//        for(VorschlagDecoration v : extraAssessment) {
//            Logger.getLogger(getClass().getName()).log(Level.INFO, v.getClass().getName());
//        }
//        if (extra.targetType().equals("arbeitsverhalten")) {
//            Instance<VorschlagDecoration> select = extraAssessment.select(extra);
//            return select.get();
//        }
        Instance<VorschlagDecoration> select = extraAssessment.select(extra);
//        return select.isUnsatisfied() ? null : select.get();
        return (!select.isUnsatisfied() && !select.isAmbiguous()) ? select.get() : null;
    }

//    public Logger getLogger() {
//        return log;
//    }

//    public void processTimeout(jakarta.faces.event.AjaxBehaviorEvent evt) {
//        logout(null);
//    }
//    public String logout() {
//        FacesContext.getCurrentInstance()
//            .getExternalContext()
//            .invalidateSession();
//        return "/login.xhtml?faces-redirect=true";
//    }

    public void logout(ActionEvent evt) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null) {
            ExternalContext context = fc.getExternalContext();
            if (context != null) {
                String redirect = null;
                HttpServletRequest request = (HttpServletRequest) context.getRequest();
                if (request != null) {
                    redirect = request.getContextPath();
                }
                HttpSession session = (HttpSession) context.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                if (redirect != null) {
                    try {
                        context.redirect(redirect);
                        fc.responseComplete();
                    } catch (IOException ex) {
                    }
                }
            }
        }
    }

}
