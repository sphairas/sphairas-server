/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.acer.entities.util;

import java.sql.Timestamp;
import java.util.Comparator;
import org.thespheres.acer.entities.BaseMessage;
import org.thespheres.acer.entities.DurableMessage;
import org.thespheres.acer.entities.messages.tracking.UpdateTracker;

/**
 *
 * @author boris.heithecker
 */
public class MessageComparator implements Comparator<BaseMessage> {

    @Override
    public int compare(BaseMessage m1, BaseMessage m2) {
        int prio1 = findPrio(m1);
        int prio2 = findPrio(m2);
        if (prio1 != prio2) {
            return prio1 - prio2;
        }
        Timestamp ts1 = findTimestamp(m1);
        Timestamp ts2 = findTimestamp(m2);
        return ts1.compareTo(ts2);
    }

    private Timestamp findTimestamp(BaseMessage m1) {
        Timestamp ts1 = m1.getTrackers().stream()
                .filter(b -> b instanceof UpdateTracker)
                .findAny()
                .map(t -> ((UpdateTracker) t).getUpdated())
                .orElse(m1.getCreationTime());
        return ts1;
    }

    private int findPrio(BaseMessage m1) {
        int prio1 = 0;
        if (m1 instanceof DurableMessage) {
            prio1 = ((DurableMessage) m1).getPriority();
        }
        prio1 = prio1 != 0 ? prio1 : 5;
        return prio1;
    }

}
