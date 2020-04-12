/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.assess.Grade;
import org.thespheres.betula.services.web.WebUIConfiguration;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.model.Subject;
import org.thespheres.betula.niedersachsen.vorschlag.VorschlagDecoration;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.server.beans.FastMessages;
import org.thespheres.betula.server.beans.FastTargetDocuments2;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.JoinedUnitsEntry;
import org.thespheres.betula.server.beans.SigneeLocal;
import org.thespheres.betula.server.beans.StudentsListsLocalBean;
import org.thespheres.betula.server.beans.StudentsLocalBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.server.beans.annot.DocumentsSession;
import org.thespheres.betula.server.beans.annot.Preceding;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.ServiceConstants;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.web.config.Extra;
import org.thespheres.betula.web.docsrv.DocumentMapper;
import org.thespheres.ical.VCard;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.niedersachsen.gs.CrossmarkSettings;

/**
 *
 * @author boris.heithecker
 */
@RolesAllowed("signee")
@ManagedBean(name = "app")
@Named
@SessionScoped
public class BetulaWebApplication implements Serializable {

//    @Inject
//    private FastTargetDocuments bean2;
    @DocumentsSession
    @Inject
    private FastTargetDocuments2 bean;
    @EJB(beanName = "FastMessagesImpl")
    private FastMessages fastMessages;
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
    @EJB
    private DocumentMapper documentMapper;
    @EJB
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
    private String currentPage = "messages"; //terms";
    private ApplicationUser currentUser;
    private Messages messages;
    @Inject
    private WebUIConfiguration webConfig;
//    @Inject
//    private ZeugnisConfiguratorService zgnConfig;
    @Inject
    private Comparator<Subject> subjectComparator;
    @Inject
    private CommonDocuments commonDocuments;
    @Inject
    private LocalProperties properties;
    private final Map<DocumentId, FastTermTargetDocument> fastDocs = new HashMap<>();
    private DefaultStreamedContent image;
    private final Logger log = Logger.getLogger(BetulaWebApplication.class.getPackage().getName());
    @Inject
    private CrossmarkSettings crossmarks;
    private List<Grade> crossMarkGrade;
//    private Optional<AssessmentConvention> crossMarksAssessmentConvention;    
//    private Optional<String[]> crossMarksSubjectConvention;

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
            eventDispatch.unregister(messages);
            currentUser.logout();
            Logger.getLogger(BetulaWebApplication.class.getName()).log(Level.INFO, "LOGGED OUT {0} {1}", new Object[]{currentUser.getSignee().getId(), new Date().toLocaleString()});
        }
    }

    public StreamedContent getImage() {
        if (image == null) {
            final String lr = getWebUIConfiguration().getLogoResource();
            try {
                if (lr != null) {
                    final Path rp = ServiceConstants.configBase().resolve(lr);
                    image = new DefaultStreamedContent(Files.newInputStream(rp), "image/jpeg");
                } else {

                }
            } catch (IOException ex) {
            }
        }
        return image;
    }

    public String getUsername() {
        return getUser() != null ? getUser().getDisplayName() : "";
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public String getMenuStyle(String menu) {
        if (menu.equals(getCurrentPage())) {
            return "font-weight: bold;";
        } else {
            return "";
        }
    }

    public NamingResolver getNamingResolver() {
//        if (namingResolver == null) {
//            return namingResolver = SystemProperties.findNamingResolver();
//        }
        return namingResolver;
    }

    public WebUIConfiguration getWebUIConfiguration() {
//        if (webConfig == null) {
//            return webConfig = SystemProperties.findWebUIConfiguration();
//        }
        return webConfig;
    }

//    public ZeugnisConfiguratorService getZeugnisConfiguratorService() {
////        if (zgnConfig == null) {
////            return zgnConfig = SystemProperties.findZeugnisConfiguratorService();
////        }
//        return zgnConfig;
//    }
    public Comparator<Subject> getSubjectComparator() {
        return subjectComparator;
    }

    public CommonDocuments getCommonDocuments() {
        return commonDocuments;
    }

    public LocalProperties getProperties() {
        return properties;
    }

    public List<String> getCrossMarkSubjectConventions() {
        return Arrays.asList(crossmarks.conventions());
    }

    public synchronized AssessmentConvention getCrossMarkAssessmentConvention() {
//        if (crossMarksAssessmentConvention == null) {
//            crossMarksAssessmentConvention = crossmarks.isUnsatisfied() ? Optional.empty() : Optional.of(crossmarks.get().getAssessmentConvention());
//        }
//        return crossMarksAssessmentConvention.orElse(null);
        return crossmarks.getAssessmentConvention();
    }

    public synchronized List<Grade> getCrossMarkGrades() {
        if (crossMarkGrade == null) {
            final AssessmentConvention ac = getCrossMarkAssessmentConvention();
            crossMarkGrade = ac != null ? Arrays.asList(ac.getAllGradesReverseOrder()) : Collections.emptyList();
        }
        return crossMarkGrade;
    }

    public String[] getTargetTypes() {
        return webConfig.getCommitTargetTypes();
//        return new String[]{"quartalsnoten", "zeugnisnoten", "arbeitsverhalten", "sozialverhalten"};
    }

    public void showMessage(String summary, String detail) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail);
        RequestContext.getCurrentInstance().showMessageInDialog(message);
    }

    public Term getCurrentTerm() {
        return currentTerm; //SystemProperties.terms()[1];
    }

    public Term getTermBefore() {
        return beforeTerm; //SystemProperties.terms()[0];
    }

    public Messages getMessages() {
        if (messages == null) {
            messages = new Messages(this);
            eventDispatch.register(messages);
        }
        return messages;
    }

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

    Collection<DocumentId> getTargetAssessmentDocuments(UnitId primaryUnit) {
        return bean.getTargetAssessmentDocuments(primaryUnit);
    }

    Collection<StudentId> getStudents() { //Signee signee) {
        return bean.getPrimaryUnitStudents();
    }

    FastMessages getFastMessages() {
        getMessages();//initialize Messages, register listener
        return fastMessages;
    }

    Marker getStudentMarkerEntry(StudentId sid, DocumentId studentSGLMarkerDocId) {
        return studentsLists.getMarkerEntry(sid, studentSGLMarkerDocId, null);
    }

    DocumentMapper getDocumentMapper() {
        return documentMapper;
    }

    DocumentsModel getDocumentsModel() {
        return docModel;
    }

    UnitId getPrimaryUnit() {
        return bean.getPrimaryUnit();
    }

    Grade selectGrade(DocumentId docId, TermId termId, StudentId studId) throws IOException {
        return bean.selectSingle(docId, studId, termId);// bean.select(docId, studId, termId);
    }

//    synchronized 
    boolean submitGrade(DocumentId docId, TermId termId, StudentId studId, Grade grade) throws IOException {
        return bean.submitSingle(docId, studId, termId, grade); //submit(docId, studId, termId, grade, new Timestamp());
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

    public Logger getLogger() {
        return log;
    }

    public void processTimeout(javax.faces.event.AjaxBehaviorEvent evt) {
        doLogout(null);
    }

    public void doLogout(ActionEvent evt) {
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
