/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.jms;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Topic;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class StudentsNotificator extends AbstractNotificator {

    @Resource(mappedName = "jms/students-topic")
    private Topic tickets_topic;
    @Inject
    @JMSConnectionFactory("jms/student-topic-factory")
    private JMSContext context;

    @Override
    protected Topic getTopic() {
        return tickets_topic;
    }

    @Override
    protected JMSContext getJMSContext() {
        return context;
    }
}
