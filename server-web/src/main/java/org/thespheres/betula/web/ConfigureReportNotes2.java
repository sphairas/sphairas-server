/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.Serializable;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.enterprise.inject.Default;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.openide.util.WeakListeners;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.Tag;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.util.CollectionUtil;
import org.thespheres.betula.web.PrimaryUnit.AvailableStudentExt;
import org.thespheres.betula.web.docsrv.ZeugnisArguments;

/**
 *
 * @author boris.heithecker
 */
@ManagedBean(name = "reportNotes2")
@Named
@SessionScoped
public class ConfigureReportNotes2 implements VetoableChangeListener, Serializable {

    private static final long serialVersionUID = 1L;
    @EJB(beanName = "ReportsBeanImpl")
    private ReportsBean zeugnisBean;
    @EJB
    private ZeugnisArguments zeugnisArguments;
    @ManagedProperty("#{app}")
    private BetulaWebApplication application;
    @Current
    @Inject
    private Term currentTerm;
    @Inject
    private TermReportNoteSetTemplate reportNoteTemplate;
//    private transient Map<TermReportNoteSetTemplate2.MarkerItem, String> dl = new HashMap<>();
    @Default
    @Inject
    private NamingResolver namingResolver;
    private transient CurrentStudentsNotesSelection current;

    public BetulaWebApplication getApplication() {
        return application;
    }

    //Setter requiered (!) for injection!
    public void setApplication(BetulaWebApplication application) {
        this.application = application;
    }

    private void initialize() {
        final PrimaryUnit pu = application.getUser().getCurrentPrimaryUnit();
        if (pu == null) {
            return;
        }
        final StudentId cs = pu.getSelectedStudent();
        if (cs == null || pu == null) {
            //c
        } else if (current == null || !cs.equals(current.getStudent())) {
            final AvailableStudentExt as = (AvailableStudentExt) pu.getStudents().stream()
                    .filter(s -> s.getId().equals(cs))
                    .collect(CollectionUtil.requireSingleOrNull());

            final DocumentId zgn = as.getZeugnisId();
            final UnitId unit = as.getPrimaryUnit().getUnitId();
            final CurrentStudentsNotesSelection ns = new CurrentStudentsNotesSelection(cs, zgn, unit);

            if (zgn != null) {
                final Marker[] markers = zeugnisBean.getMarkers(zgn);
                for (final TermReportNoteSetTemplate.Element e : reportNoteTemplate.getElements()) {
                    final CurrentStudentsNotesSelection.ElementSelection es = ns.addElement(e);

                    final List<String> selected = Arrays.stream(markers)
                            .filter(Objects::nonNull)
                            .filter(e::containsMarker)
                            .map(Marker::getId)
                            .collect(Collectors.toList());

                    if (e.isMultiple()) {
                        es.setSelected(selected);
                    } else {
                        //TODO: Log if more than one selection
                        if (!selected.isEmpty()) {
                            es.setSelectedItem(selected.get(0));
                        } else {
                            final String idNil;
                            if (e.isNillable()) {
                                idNil = e.getMarkers().get(0).getId();
                            } else {
                                try {
                                    idNil = e.getMarkers().get(e.getDefaultElement()).getId();
                                } catch (IndexOutOfBoundsException ex) {
                                    final String msg = "Default index " + Integer.toString(e.getDefaultElement()) + " cannot be selected in " + e.getElementDisplayName();
                                    Logger.getLogger(ConfigureReportNotes2.class.getName()).log(Level.SEVERE, msg, ex);
                                    es.setSelectedItem("");
                                    continue;
                                }
                            }
                            es.setSelectedItem(idNil);
                        }
                    }

                    final VetoableChangeListener pcl = WeakListeners.vetoableChange(this, es);
                    es.addVetoableChangeListener(pcl);
                }
            }
            current = ns;
        }
    }

    public List<CurrentStudentsNotesSelection.ElementSelection> getElements() {
        initialize();
        if (current != null) {
            return current.getSelectedElements();
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public String formatMarkerLabel(final TermReportNoteSetTemplate.MarkerItem mi) {
        initialize();
        if (mi == null || "null".equals(mi.getId())) {
            return "---";
        } else {
            if (current != null) {
                if (current.formatArgs == null) {
                    current.formatArgs = zeugnisArguments.getFormatArgs(current.getReport(), current.getUnit());
                }
                return Optional.ofNullable(mi.getMarker())
                        .map(m -> m.getLongLabel(current.formatArgs))
                        .orElse(mi.getId());
            } else {
                return mi.getId();
            }
        }
    }

    //rendered="#{configRepNotes.renderElement(element)}"
    public boolean renderElement(final CurrentStudentsNotesSelection.ElementSelection el) {
        if (el != null) {
            final List<Tag> h = el.getDisplayHints();
            if (el.isHidden()) {
                return false;
            } else if (!h.isEmpty()) {
                return resolveShowTerm(h)
                        && resolveShowLevel(h);
            }
        }
        return true;
    }

    //itemDisabled="#{!configRepNotes.itemEnabled(marker)}"
    public boolean itemEnabled(final TermReportNoteSetTemplate.MarkerItem mi) {
        if (mi != null) {
            final List<Tag> h = mi.getDisplayHint();
            if (mi.isHidden()) {
                return false;
            } else if (!h.isEmpty()) {
                return resolveShowTerm(h)
                        && resolveShowLevel(h);
            }
        }
        return true;
    }

    private boolean resolveShowTerm(final List<Tag> tags) {
        return tags.stream()
                .filter(t -> t.getConvention().equals("de.halbjahre"))
                .allMatch(t -> t.getId().equals(getRequiredTermName()));

    }

    protected String getRequiredTermName() {
        final boolean hj1 = currentTerm.getBeginDate().getMonth().equals(Month.AUGUST);
        final String reqId = hj1 ? "halbjahr" : "schuljahr";
        return reqId;
    }

    private boolean resolveShowLevel(final List<Tag> tags) {
        return tags.stream()
                .filter(t -> t.getConvention().equals("de.stufen"))
                .map(Tag::getShortLabel)
                .anyMatch(t -> t.equals(getStufe()));
    }

    private String getStufe() {
        initialize();
        if (current != null) {
            try {
                final NamingResolver.Result r = namingResolver.resolveDisplayNameResult(current.getUnit());
                r.addResolverHint("naming.only.level");
                current.stufe = r.getResolvedName(currentTerm);
            } catch (IllegalAuthorityException ex) {
            }
            return current.stufe;
        }
        return "";
    }

    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        final CurrentStudentsNotesSelection.ElementSelection element;
        final Object o = evt.getSource();
        if (o instanceof CurrentStudentsNotesSelection.ElementSelection) {
            element = (CurrentStudentsNotesSelection.ElementSelection) o;
        } else {
            throw new PropertyVetoException("other", evt);
        }
        initialize();
        if (current == null) {
            throw new PropertyVetoException("current is null", evt);
        }
        final DocumentId zgn = current.getReport();
        Object ov = evt.getOldValue();
        Object nv = evt.getNewValue();
        if (ov instanceof List && nv instanceof List) {
            List<String> old;
            List<String> value;
            try {
                old = (List<String>) evt.getOldValue();
                value = (List<String>) evt.getNewValue();
                for (String n : value) {
                    if (!old.contains(n)) {
                        final Marker m = element.forId(n);
                        if (m != null) {
                            if (!zeugnisBean.addMarker(zgn, m)) {
                                throw new PropertyVetoException("not persisted", evt);
                            }
                        }
                    }
                }
                for (String a : old) {
                    if (!value.contains(a)) {
                        final Marker m = element.forId(a);
                        if (m != null) {
                            if (!zeugnisBean.removeMarker(zgn, m)) {
                                throw new PropertyVetoException("not persisted", evt);
                            }
                        }
                    }
                }
                return;
            } catch (ClassCastException ex) {
            }
        } else if (ov instanceof String && nv instanceof String) {
            String old = (String) evt.getOldValue();
            Marker m = element.forId(old);
            if (m != null && !"null".equals(m.getId())) {
                if (!zeugnisBean.removeMarker(zgn, m)) {
                    throw new PropertyVetoException("not persisted", evt);
                }
            }
            String value = (String) evt.getNewValue();
            m = element.forId(value);
            if (m != null) {
                m = !"null".equals(m.getId()) ? m : null;
                if (!zeugnisBean.addMarker(zgn, m)) {
                    throw new PropertyVetoException("not persisted", evt);
                }
            }
            return;
        }
        throw new PropertyVetoException("other", evt);
    }

}
