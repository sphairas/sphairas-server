package org.thespheres.betula.web.docsrv;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.server.beans.FastTermTargetDocument;
import org.thespheres.betula.server.beans.clients.json.JsonbContextResolver;

public class FastTermTargetDocumentJsonTest {

    @Test
    public void testPreservesStudentAndTermKeysWhenDeserializing() throws Exception {
        final StudentId student = StudentId.valueOf("1793484836232@spdev2/1");
        final TermId term = TermId.valueOf("653@mk.niedersachsen.de");
        final FastTermTargetDocument.Entry entry = new FastTermTargetDocument.Entry(null, null);
        final FastTermTargetDocument document = new FastTermTargetDocument(
                DocumentId.valueOf("chemie-2025-a-zeugnisnoten/latest@spdev2/1"),
                Map.of(student, Map.of(term, entry)),
                Set.of(),
                "conv",
                Map.of(),
                "type",
                null,
                null);
        final var jsonb = new JsonbContextResolver().getContext(FastTermTargetDocument.class);

        final FastTermTargetDocument restored = jsonb.fromJson(
                jsonb.toJson(document), FastTermTargetDocument.class);

        org.junit.jupiter.api.Assertions.assertEquals(document.getDocument(), restored.getDocument());
        assertNotNull(restored.markers());
        assertTrue(restored.getValues().containsKey(student));
        assertNotNull(restored.selectEntry(student, term));
    }
}
