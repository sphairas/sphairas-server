package org.thespheres.betula.web.rest;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.Container;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.Signee;
import org.thespheres.betula.document.model.DocumentsModel;
import org.thespheres.betula.document.util.DocumentUtilities;
import org.thespheres.betula.document.util.UnitEntry;
import org.thespheres.betula.server.beans.clients.ServiceInternalClient;
import org.thespheres.betula.server.beans.clients.UnitsServiceClient;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.util.ContainerBuilder;

/**
 *
 * @author boris.heithecker
 */
@ApplicationScoped
public class DocumentsService {

    private UnitsServiceClient serviceClient;
    @Inject
    private DocumentsModel docModel;

    @PostConstruct
    public void initialize() {
        serviceClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(ServiceInternalClient.URI_SERVICE_API))
                .hostnameVerifier((hostname, session) -> true) // Optional: specific verifier
                .register(ContainerLegacyJAXBProvider.class)
                .build(UnitsServiceClient.class);
    }

    public UnitsServiceClient getServiceClient() {
        return serviceClient;
    }

    public Set<DocumentId> getDocuments(final Signee signee) {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.SIGNEES_TARGETS_PATH;
        builder.add(new Entry(Action.REQUEST_COMPLETION, signee), path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final Set<DocumentId> ret = l.stream()
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof Signee)
                .flatMap(n -> ((Entry<Signee, ?>) n).getChildren().stream())
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof DocumentId)
                .map(n -> ((Entry<DocumentId, ?>) n).getIdentity())
                .collect(Collectors.toSet());
        return ret;
    }

    public Set<StudentId> getStudents(final UnitId unit) {
        final ContainerBuilder builder = new ContainerBuilder();
        final String[] path = Paths.UNITS_PARTICIPANTS_PATH;
        final Entry<UnitId, ?> root = new Entry<>(null, unit);
        final UnitEntry ue = new UnitEntry(docModel.convertToUnitDocumentId(unit), unit, Action.REQUEST_COMPLETION, true);
        root.getChildren().add(ue);
        builder.add(root, path);
        final Container response = serviceClient.solicit(builder.getContainer());
        final List<Envelope> l = DocumentUtilities.findEnvelope(response, path);
        final Set<StudentId> ret = l.stream()
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof UnitId)
                .flatMap(n -> ((Entry<UnitId, ?>) n).getChildren().stream())
                .filter(n -> UnitEntry.class.isAssignableFrom(n.getClass()))
                .flatMap(n -> ((UnitEntry) n).getChildren().stream())
                .filter(n -> Entry.class.isAssignableFrom(n.getClass()) && ((Entry<?, ?>) n).getIdentity() instanceof StudentId)
                .map(n -> ((Entry<StudentId, ?>) n).getIdentity())
                .collect(Collectors.toSet());
        return ret;
    }
}
