/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.service.unitadmin;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import org.thespheres.betula.document.Action;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.document.Entry;
import org.thespheres.betula.document.Envelope;
import org.thespheres.betula.document.util.DateTimeUtil;
import org.thespheres.betula.entities.facade.DocumentsFacade;
import org.thespheres.betula.entities.service.ServiceUtils;
import org.thespheres.betula.services.ws.NotFoundException;
import org.thespheres.betula.services.ws.Paths;
import org.thespheres.betula.services.ws.SyntaxException;
import org.thespheres.betula.services.ws.UnauthorizedException;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class AdminDocumentsProcessor extends AbstractAdminContainerProcessor {

    @EJB(beanName = "DocumentsFacadeImpl")
    private DocumentsFacade documents;

    public AdminDocumentsProcessor() {
        super(new String[][]{Paths.DOCUMENTS_PATH});
    }

    @Override
    public void process(String[] path, Envelope template) throws UnauthorizedException, NotFoundException, SyntaxException {
        if (Arrays.equals(path, Paths.DOCUMENTS_PATH)) {
            processDocuments(template);
        }
    }

    private void processDocuments(final Envelope node) throws SyntaxException {
        final Action a = node.getAction();
        if (a != null && a.equals(Action.REQUEST_COMPLETION)) {
            final Set<DocumentId> l;
            final String get = node.getHints().get("if-expired-before");
            if (get != null) {
                final ZonedDateTime zdt;
                try {
                    zdt = ZonedDateTime.parse(get, DateTimeUtil.ZONED_DATE_TIME_FORMAT);
                } catch (DateTimeParseException e) {
                    Logger.getLogger(AdminDocumentsProcessor.class.getName()).log(Level.SEVERE, "An exception was thrown in processDocuments", e);
                    throw ServiceUtils.createSyntaxException(e);
                }
                l = documents.findAllExpired(zdt.toInstant().toEpochMilli());
            } else {
                l = documents.findAll();
            }
            node.getChildren().clear();
            node.setAction(Action.RETURN_COMPLETION);
            l.stream()
                    .map(d -> new Entry(null, d))
                    .forEach(te -> node.getChildren().add(te));
        }
    }

}
