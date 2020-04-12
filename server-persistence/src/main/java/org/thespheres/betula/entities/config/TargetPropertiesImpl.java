/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.thespheres.betula.assess.AssessmentConvention;
import org.thespheres.betula.assess.GradeFactory;
import org.thespheres.betula.assess.TargetDocument;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.MarkerConvention;
import org.thespheres.betula.document.MarkerFactory;
import org.thespheres.betula.document.UniqueMarkerSet;
import org.thespheres.betula.services.CommonTargetProperties;
import org.thespheres.betula.document.model.MarkerDecoration;
import org.thespheres.betula.server.beans.annot.Subjects;
import org.thespheres.betula.services.LocalFileProperties;

/**
 *
 * @author boris.heithecker
 */
@Dependent
public class TargetPropertiesImpl implements CommonTargetProperties {

    @Inject
    private LocalConfigProperties properties;
    private MarkerConvention[] subjectConventions;
    private MarkerConvention[] realmConventions;

    @Override
    public AssessmentConvention[] getAssessmentConventions() {
        final String[] arr = properties.getProperty(LocalFileProperties.PROP_ASSESSMENT_CONVENTIONS, "").split(",");
        return Arrays.stream(arr)
                .filter(s -> !s.trim().isEmpty())
                .map(GradeFactory::findConvention)
                .filter(Objects::nonNull)
                .toArray(AssessmentConvention[]::new);
    }

    @Override
    public synchronized MarkerConvention[] getRealmMarkerConventions() {
        if (realmConventions == null) {
            final String[] arr = getRealmMarkerConventionNames();
            realmConventions = Arrays.stream(arr)
                    .filter(s -> !s.trim().isEmpty())
                    .map(MarkerFactory::findConvention)
                    .filter(Objects::nonNull)
                    .toArray(MarkerConvention[]::new);
        }
        return realmConventions;
    }

    String[] getRealmMarkerConventionNames() {
        return properties.getProperty(LocalFileProperties.PROP_REALM_CONVENTIONS, "").split(",");
    }

    @Override
    public synchronized MarkerConvention[] getSubjectMarkerConventions() {
        if (subjectConventions == null) {
            final String[] arr = getSubjectMarkerConventionNames();
            subjectConventions = Arrays.stream(arr)
                    .filter(s -> !s.trim().isEmpty())
                    .map(MarkerFactory::findConvention)
                    .filter(Objects::nonNull)
                    .toArray(MarkerConvention[]::new);
        }
        return subjectConventions;
    }

    String[] getSubjectMarkerConventionNames() {
        return properties.getProperty(LocalFileProperties.PROP_UNIQUE_SUBJECT_CONVENTIONS, "").split(",");
    }

    @Subjects(ignoreRealmMarkers = true)
    @Produces
    public MarkerDecoration createSubjectDecorationIgnoreRealm() {
        return createSubjectDecoImpl(true);
    }

    @Subjects
    @Produces
    public MarkerDecoration createSubjectDecoration() {
        return createSubjectDecoImpl(false);
    }

    private MarkerDecoration createSubjectDecoImpl(final boolean ignoreLM) {
        class MDImpl implements MarkerDecoration {

            private final String[] subjects;
            private final String[] lesson;

            public MDImpl(String[] subjects, String[] lesson) {
                this.subjects = subjects;
                this.lesson = lesson;
            }

            @Override
            public UniqueMarkerSet getDistinguishingDecoration(DocumentId id, TargetDocument targetDocument, String view) {
                if ("subject".equals(view)) {
                    String[] arr = Stream.concat(Arrays.stream(subjects), Arrays.stream(lesson)).toArray(String[]::new);
                    final UniqueMarkerSet ret = new UniqueMarkerSet(arr, false);
                    try {
                        ret.initialize(targetDocument.markers());
                    } catch (IllegalArgumentException e) {
                        Logger.getLogger(ConfigurationPropertiesImpl.class.getName()).log(Level.WARNING, e.getMessage());
                    }
                    return ret;
                }
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public List<MarkerConvention> getDecoration(DocumentId id, TargetDocument document) {
                return getDistinguishingDecoration(id, document, "subject").getExclusiveConventions();
            }

        }
        return new MDImpl(getSubjectMarkerConventionNames(), ignoreLM ? new String[]{} : getRealmMarkerConventionNames());
    }

}
