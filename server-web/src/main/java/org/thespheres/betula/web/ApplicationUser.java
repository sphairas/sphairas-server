/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.primefaces.model.DashboardColumn;
import org.primefaces.model.DashboardModel;
import org.primefaces.model.DefaultDashboardColumn;
import org.primefaces.model.DefaultDashboardModel;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.MarkerConvention;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.model.MultiSubject;
import org.thespheres.betula.server.beans.FastMessage;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.services.NamingResolver;

/**
 *
 * @author boris.heithecker
 */
public class ApplicationUser implements Serializable {

    private TabArrayList<AvailableTarget> units;
    private DefaultDashboardModel dashboard;
    private PrimaryUnit primaryUnit;
    private final BetulaWebApplication application;
    private final Signee signee;
    private boolean isInitPU;
    private boolean isInitMessages;

    ApplicationUser(BetulaWebApplication app, Signee sig) {
        this.application = app;
        this.signee = sig;
    }

    public Signee getSignee() {
        return signee;
    }

    public String getDisplayName() {
        return signee.getId();
    }

    public DashboardModel getDashboard() {
        if (dashboard == null) {
            dashboard = new DefaultDashboardModel();
            DashboardColumn column1 = new DefaultDashboardColumn();

            column1.addWidget("sports");
            column1.addWidget("finance");

            dashboard.addColumn(column1);
        }
        return dashboard;
    }

    public synchronized List<FastMessage> getFastMessages() {
        List<FastMessage> ret = application.getFastMessages().getFastMessages(!isInitMessages);
        isInitMessages = true;
        return ret;
    }

    void invalidateMessages() {
        isInitMessages = false;
    }

    //schedule, terms, primaryUnits
    public boolean renderMenu(String menu) {
        switch (menu) {
            case "schedule":
                return false;
            case "terms":
                return true;
            case "primaryUnits":
                return getPrimaryUnit() != null;
        }
        return false;
    }

    public synchronized TabArrayList<AvailableTarget> getTargetUnits() {
        if (units == null) {

            units = new TabArrayList<>();
            final DocumentsModel docModel = application.getDocumentsModel();
            final Map<DocumentId, Set<DocumentId>> map = application.getDocuments().stream()
                    .collect(Collectors.groupingBy(docModel::convert, Collectors.toSet()));

            final Map<String, AvailableTarget> tabs = new HashMap<>();

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
                    AvailableTarget tab = createTab(tabs, name);
                    tab.addCrossMarksDocument(e.getValue().iterator().next(), crossMarkSubject);
                } else {
                    String name;
                    try {
                        final NamingResolver.Result nr = application.getNamingResolver().resolveDisplayNameResult(e.getKey());
                        name = nr.getResolvedName(application.getCurrentTerm());
                    } catch (IllegalAuthorityException ex) {
                        name = e.getKey().getId();
                    }
                    AvailableTarget tab = createTab(tabs, name);
                    tab.getDocuments().addAll(e.getValue());
                }
            }
        }
        return units;
    }

    private AvailableTarget createTab(final Map<String, AvailableTarget> tabs, final String name) {
        final AvailableTarget tab = tabs.computeIfAbsent(name, cn -> {
            final AvailableTarget at = new AvailableTarget(cn, application);
            units.getTabs().add(at);
            tabs.put(cn, at);
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
            final List<String> cmsc = application.getCrossMarkSubjectConventions();
            if (ret.map(Marker::getConvention)
                    .map(cmsc::contains)
                    .orElse(false)) {
                return ret.get();
            }
        }
        return null;
    }

    public synchronized PrimaryUnit getPrimaryUnit() {
        if (!isInitPU) {
            final UnitId uid = application.getPrimaryUnit();
            if (uid != null) {
                final Term t = application.getCurrentTerm();
                final Term b = application.getTermBefore();
                primaryUnit = new PrimaryUnit(uid, t, b, application);
                application.getEventDispatch().register(primaryUnit);
            }
            isInitPU = true;
        }
        return primaryUnit;
    }

    void logout() {
        dashboard = null;
        if (units != null) {
            units.getTabs().stream().forEach((ad) -> {
                ad.valid = false;
                application.getEventDispatch().unregister(ad);
            });
        }
        units = null;
        if (primaryUnit != null) {
            primaryUnit.valid = false;
            application.getEventDispatch().unregister(primaryUnit);
        }
        primaryUnit = null;
        isInitPU = false;
    }

}
