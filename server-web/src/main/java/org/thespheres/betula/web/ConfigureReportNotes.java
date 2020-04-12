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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.inject.Default;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.thespheres.betula.Tag;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.DocumentId.Version;
import org.thespheres.betula.document.Marker;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate;
import org.thespheres.betula.niedersachsen.zeugnis.TermReportNoteSetTemplate.Element;
import org.thespheres.betula.server.beans.ReportsBean;
import org.thespheres.betula.server.beans.annot.Current;
import org.thespheres.betula.services.IllegalAuthorityException;
import org.thespheres.betula.services.NamingResolver;
import org.thespheres.betula.services.scheme.spi.Term;
import org.thespheres.betula.web.docsrv.ZeugnisArguments;

/**
 *
 * @author boris.heithecker
 */
@ManagedBean(name = "configRepNotes")
@ViewScoped
public class ConfigureReportNotes implements VetoableChangeListener, Serializable {//ZGN

    private static final long serialVersionUID = 1L;//ZGN
    public static final String PARAMETER_ZEUGNIS_ID = "zeugnisId";
    public static final String PARAMETER_UNIT_ID = "unitId";
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
//    private NoteSetSelection selection;
    private DocumentId zeugnisId;
    private transient Object[] formatArgs;//ZGN
    private transient Map<TermReportNoteSetTemplate.MarkerItem, String> dl = new HashMap<>();
    @Default
    @Inject
    private NamingResolver namingResolver;
    private UnitId unitId;
    private String stufe;
    private final long construct;

    public ConfigureReportNotes() {
        construct = System.currentTimeMillis();
    }

    @PostConstruct
    public void initialize() {
        long initialize = System.currentTimeMillis();
        final DocumentId zgn = getZeugnisId();
        if (zgn != null) {
            final Marker[] markers = zeugnisBean.getMarkers(zgn);
            for (TermReportNoteSetTemplate.Element e : reportNoteTemplate.getElements()) {
                e.addVetoableChangeListener(this);
                final List<String> selected = new ArrayList<>();
                for (int i = 0; i < markers.length; i++) {
                    if (markers[i] != null && e.containsMarker(markers[i])) {
                        selected.add(markers[i].getId());
                        markers[i] = null;
                    }
                }
                if (e.isMultiple()) {
                    e.setSelected(selected);
                } else if (!selected.isEmpty()) {
                    //TODO: Log if more than one selection
                    e.setSelectedItem(selected.get(0));
                }
            }
        }
        final long time = System.currentTimeMillis() - initialize;
        final long time2 = System.currentTimeMillis() - this.construct;
        Logger.getLogger(ConfigureReportNotes.class.getCanonicalName()).log(Level.INFO, "Inititialized TermReportNoteSetTemplate for {0} in {1}/{2}ms", new Object[]{zgn == null ? "null" : zgn.getId(), Long.toString(time2), Long.toString(time)});
    }

    public List<Element> getElements() {
        return reportNoteTemplate.getElements();
    }

    public void selectRadioItem() {
        Logger.getLogger(ConfigureReportNotes.class.getCanonicalName()).log(Level.INFO, "Called selectRadioItem");
    }

    public String formatMarkerLabel(final TermReportNoteSetTemplate.MarkerItem mi) {
        if ("null".equals(mi.getId())) {
            return "---";
        } else {
            return dl.computeIfAbsent(mi, m2 -> {
                final Marker m = m2.getMarker();
                if (m != null) {
                    final Object[] args = getFormatArgs();
                    return m.getLongLabel(args);
                } else {
                    return m2.getId();
                }
            });
        }
    }

    private Object[] getFormatArgs() {
        if (formatArgs == null) {
            DocumentId zgn = getZeugnisId();
            formatArgs = zeugnisArguments.getFormatArgs(zgn, getUnitId());
        }
        return formatArgs;
    }

    private String[] getParameter(String name) {
        final Map<String, String[]> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
        return params.get(name);
    }

    private DocumentId getZeugnisId() {
        if (zeugnisId == null) {
            String[] p = getParameter(PARAMETER_ZEUGNIS_ID);
            if (p != null && p.length == 3) {
                zeugnisId = new DocumentId(p[0], p[1], Version.parse(p[2]));
            }
        }
        return zeugnisId;
    }

    private UnitId getUnitId() {
        if (unitId == null) {
            String[] p = getParameter(PARAMETER_UNIT_ID);
            if (p != null && p.length == 2) {
                unitId = new UnitId(p[0], p[1]);
            }
        }
        return unitId;
    }

    //rendered="#{configRepNotes.renderElement(element)}"
    public boolean renderElement(final TermReportNoteSetTemplate.Element el) {
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
                .allMatch(t -> t.equals(getStufe()));
    }

    private String getStufe() {
        if (stufe == null) {
            try {
                final NamingResolver.Result r = namingResolver.resolveDisplayNameResult(getUnitId());
                r.addResolverHint("naming.only.level");
                stufe = r.getResolvedName(currentTerm);
            } catch (IllegalAuthorityException illegalAuthorityException) {
            }
        }
        return stufe;
    }

    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        TermReportNoteSetTemplate.Element element = null;
        Object o = evt.getSource();
        if (o instanceof TermReportNoteSetTemplate.Element) {
            element = (TermReportNoteSetTemplate.Element) o;
        }
        DocumentId zgn = getZeugnisId();
        if (element == null || zgn == null) {
            throw new PropertyVetoException("other", evt);
        }
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
                        Marker m = element.forId(n);
                        if (m != null) {
                            if (!zeugnisBean.addMarker(zgn, m)) {
                                throw new PropertyVetoException("not persisted", evt);
                            }
                        }
                    }
                }
                for (String a : old) {
                    if (!value.contains(a)) {
                        Marker m = element.forId(a);
                        if (m != null) {
                            if (!zeugnisBean.removeMarker(zgn, m)) {
                                throw new PropertyVetoException("not persisted", evt);
                            }
                        }
                    }
                }
            } catch (ClassCastException ex) {
            }
            return;
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

    public BetulaWebApplication getApplication() {
        return application;
    }

    public void setApplication(BetulaWebApplication application) {
        this.application = application;
    }

    public class ElementSelection {

        Element el;

        public boolean isMultiple() {
            return el.isMultiple();
        }

//        public int getDefaultElement() {
//            return el.getDefaultElement();
//        }
        public String getElementDisplayName() {
            return el.getElementDisplayName();
        }

        public boolean isHidden() {
            return el.isHidden();
        }

        public List<TermReportNoteSetTemplate.MarkerItem> getMarkers() {
            return el.getMarkers();
        }

        public String getSelectedItem() {
            return el.getSelectedItem();
        }

        public List<String> getSelected() {
            return el.getSelected();
        }

//        public Marker forId(String id) {
//            return el.forId(id);
//        }
    }
}
