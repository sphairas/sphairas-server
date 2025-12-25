/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.jmsimpl;

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
public class DocumentsNotificator extends AbstractNotificator {

    @Resource(mappedName = "jms/documents-topic")
    private Topic documents_topic;
//    @Resource(mappedName = "jms/document-topic-factory")
//    private ConnectionFactory document_topic_factory;
    @Inject
    @JMSConnectionFactory("jms/document-topic-factory")
    private JMSContext context;
//    @Override
//    protected ConnectionFactory getTopicFactory() {
//        return document_topic_factory;
//    }

    @Override
    protected Topic getTopic() {
        return documents_topic;
    }

    @Override
    protected JMSContext getJMSContext() {
        return context;
    }
}
