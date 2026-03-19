/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import org.thespheres.betula.web.rest.DocumentsService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.SecurityContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.MarkerConvention;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.FastTextTermTargetDocument;
import org.thespheres.betula.server.beans.config.ConfiguredModelException;
import org.thespheres.betula.services.AppPropertyNames;
import org.thespheres.betula.services.LocalProperties;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.ws.CommonDocuments;
import org.thespheres.betula.web.config.AppConfiguration;

/**
 *
 * @author boris.heithecker
 */
@Named("user")
@SessionScoped
public class ApplicationUser implements Serializable {

    private Signee signee;
    private String commonName;
    private TabArrayList<AvailableTarget> units;
    private PrimaryUnit[] primaryUnits;
    @Inject
    private BetulaWebApplication application;
    @Inject
    private DocumentsService service;
    @Inject
    private SecurityContext securityContext;
    @Inject
    private AppConfiguration config;
    @Inject
    private LocalProperties lp;
    private final Map<DocumentId, FastTermTargetDocument> fastDocs = new HashMap<>();
    private final Map<DocumentId, FastTextTermTargetDocument> fastTextDocs = new HashMap<>();

    @PostConstruct
    public void init() {
        Principal principal = securityContext.getCallerPrincipal();
        if (principal == null) {
            throw new SecurityException("Principal is null.");
        }
        final String prefix = principal.getName();
        String suffix = System.getenv(AppPropertyNames.ENV_SIGNEE_SUFFIX);
        if (suffix == null) {//Legacy case
            suffix = lp.getProperty(AppPropertyNames.LP_DEFAULT_SIGNEE_SUFFIX);
        }
        if (suffix == null) {
            throw new ConfiguredModelException(AppPropertyNames.ENV_SIGNEE_SUFFIX);
        }
        signee = new Signee(prefix, suffix, true);
        commonName = config.getInternalClient().getSigneeCommonName(signee);
    }

    @PreDestroy
    public void sessionDestroyed() {
        unregister();
        Logger.getLogger(ApplicationUser.class.getName()).log(Level.INFO, "LOGGED OUT {0} {1}", new Object[]{signee.getId(), new Date().toLocaleString()});
    }

    private void unregister() {
        if (units != null) {
            units.getTabs().stream().forEach((ad) -> {
                ad.valid = false;
                application.getEventDispatch().unregister(ad);
            });
        }
        units = null;
        if (primaryUnits != null) {
            Arrays.stream(primaryUnits).forEach(pu -> {
                pu.valid = false;
                application.getEventDispatch().unregister(pu);
            });
        }
        primaryUnits = null;
    }

    public Signee getSignee() {
        //securityContext.isCallerInRole("signee") return false immediately after login, after refresh is true
//        if (!securityContext.isCallerInRole("signee")) {
//            CallerPrincipal callerPrincipal = (CallerPrincipal) securityContext.getCallerPrincipal();
        ////            throw new SecurityException("User not in role signee");
//            Logger.getLogger(ApplicationUser.class.getName()).log(Level.INFO, "User not in role signee");
//            Principal p = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();//Principal is not null and name is correct immediately after login
//            p.getName();
//        }
//        if (signee == null && securityContext.isCallerInRole("signee")) {
//            final Signee sig = loginBeanImpl.getSigneePrincipal(false);
//            signee = sig;
//            config.getInternalClient().getSigneeCommonName(signee);
//        }
        return signee;
    }

    public String getCommonName() {
        if (commonName != null) {
            return commonName;
        }
        return getSignee().getId();
    }

    //schedule, terms, primaryUnits
    public boolean renderMenu(String menu) {
        switch (menu) {
            case "schedule":
                return false;
            case "terms":
                return true;
            case "primaryUnits":
                return getPrimaryUnits().length != 0;
        }
        return false;
    }

    public synchronized TabArrayList<AvailableTarget> getTargetUnits() {
        if (units == null) {
            units = new TabArrayList<>();
            final DocumentsModel docModel = application.getDocumentsModel();
//            final Collection<DocumentId> documents = application.getDocuments();
            final Collection<DocumentId> documents = service.getDocuments(signee);
            final Map<DocumentId, Set<DocumentId>> map = documents.stream()
                    .collect(Collectors.groupingBy(docModel::convert, Collectors.toSet()));

            final Map<DocumentId, AvailableTarget> tabs = new HashMap<>();

            for (Map.Entry<DocumentId, Set<DocumentId>> e : map.entrySet()) {
                final Marker crossMarkSubject = getCrossMarkSubject(e.getValue());
                if (crossMarkSubject != null) {
                    final MarkerConvention crmc = MarkerFactory.findConvention(crossMarkSubject.getConvention());
                    final String n = crmc.getDisplayName();
                    String klasse;
                    try {
                        final NamingResolver.Result nr = application.getNamingResolver().resolveDisplayNameResult(e.getKey());
                        nr.addResolverHint("naming.no.subject");
                        klasse = nr.getResolvedName(application.getCurrentTerm());
                    } catch (IllegalAuthorityException ex) {
                        klasse = e.getKey().getId();
                    }
                    final String name = n + " " + klasse;
                    AvailableTarget tab = createTab(tabs, e.getKey(), name);
                    tab.addCrossMarksDocument(e.getValue().iterator().next(), crossMarkSubject);
                } else {
                    String name;
                    try {
                        final NamingResolver.Result nr = application.getNamingResolver().resolveDisplayNameResult(e.getKey());
                        name = nr.getResolvedName(application.getCurrentTerm());
                    } catch (IllegalAuthorityException ex) {
                        name = e.getKey().getId();
                    }
                    AvailableTarget tab = createTab(tabs, e.getKey(), name);
                    tab.getDocuments().addAll(e.getValue());
                }
            }
            units.sort();
        }
        return units;
    }

    private AvailableTarget createTab(final Map<DocumentId, AvailableTarget> tabs, final DocumentId key, final String name) {
        final AvailableTarget tab = tabs.computeIfAbsent(key, cn -> {
            final AvailableTarget at = new AvailableTarget(name, application);
            units.getTabs().add(at);
            application.getEventDispatch().register(at);
            return at;
        });
        return tab;
    }

    Marker getCrossMarkSubject(final Set<DocumentId> docs) {
        if (docs.size() == 1) {
            final DocumentId single = docs.iterator().next();
            final Optional<Marker> ret = Optional.ofNullable(application.getDocumentMapper().getSubject(single))
                    .map(MultiSubject::getSingleSubject);
            final List<String> cmsc = application.getAppConfiguration().getCrossMarkSubjectConventions();
            if (ret.map(Marker::getConvention)
                    .map(cmsc::contains)
                    .orElse(false)) {
                return ret.get();
            }
        }
        return null;
    }

    public int getPrimaryUnitsSize() {
        return getPrimaryUnits().length;
    }

    public PrimaryUnit[] getPrimaryUnits() {
        if (primaryUnits == null) {
            final ArrayList<String> l = new ArrayList<>();
            l.add(CommonDocuments.PRIMARY_UNIT_HEAD_TEACHERS_DOCID);
            final String names = application.getAppConfiguration().getWebUIConfiguration().getProperty("head-teacher-additional-document-names");
            if (names != null) {
                Arrays.stream(names.split(","))
                        .forEach(l::add);
            }
            primaryUnits = l.stream()
                    .map(this::createPrimaryUnit)
                    .filter(Objects::nonNull)
                    .toArray(PrimaryUnit[]::new);
        }
        return primaryUnits;
    }

    private PrimaryUnit createPrimaryUnit(final String name) {
        final UnitId uid = application.getPrimaryUnit(name);
        if (!UnitId.isNull(uid)) {
            final Term t = application.getCurrentTerm();
            final Term b = application.getTermBefore();
            final PrimaryUnit ret = new PrimaryUnit(name, uid, t, b, application);
            application.getEventDispatch().register(ret);
            return ret;
        }
        return null;
    }

    FastTermTargetDocument getFastDocument(DocumentId id) {
        return fastDocs.computeIfAbsent(id, d -> service.getFastTermTargetDocument(id));
    }

    FastTextTermTargetDocument getFastTextDocument(final DocumentId id) {
        return fastTextDocs.computeIfAbsent(id, d -> service.getFastTextTermTargetDocument(id));
    }

//    public PrimaryUnit getCurrentPrimaryUnit() {
//        final String page = application.getCurrentPrimaryUnit();
//        if (page != null) {
//            return Arrays.stream(getPrimaryUnits())
//                    .filter(u -> page.equals(u.getDocumentIdName()))
//                    .findAny()
//                    .orElse(null);
//        }
//        return null;
//    }
    public void logout(BetulaWebApplication betulaWebApplication) {
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
            Logger.getLogger(betulaWebApplication.getClass().getName()).log(Level.SEVERE, "Logout failed", e);
            // Show error message to user
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Logout Error", "Unable to logout. Please try again.");
            facesContext.addMessage(null, message);
        }
    }
}
