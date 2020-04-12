/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.watch;

import java.io.Serializable;
import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import org.thespheres.betula.services.jms.DocumentLockTimeoutEvent;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.betula.entities.facade.TextTargetDocumentFacade;
import org.thespheres.betula.entities.jmsimpl.DocumentsNotificator;

/**
 *
 * @author boris.heithecker
 */
@Stateless
public class DocumentLockTimeoutTimer {
    
    @EJB
    private DocumentsNotificator documentsNotificator;
    @Resource
    private TimerService service;
    @EJB
    private TextTargetDocumentFacade facade;
    
    public void addEvent(final Date d, final DocumentId did, final long lock) {
        LockInfo li = new LockInfo(did, lock);
        service.createTimer(d, li);
    }
    
    @Timeout
    public void timeout(final Timer timer) {
        if (timer.getInfo() instanceof LockInfo) {
            LockInfo li = (LockInfo) timer.getInfo();
            facade.releaseLock(li.document, li.lock);
            documentsNotificator.notityConsumers(new DocumentLockTimeoutEvent(li.document));
        }
    }
    
    private static final class LockInfo implements Serializable {
        
        private final DocumentId document;
        private final long lock;
        
        public LockInfo(DocumentId document, long lock) {
            this.document = document;
            this.lock = lock;
        }
        
    }
}
