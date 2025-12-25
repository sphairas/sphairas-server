/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Topic;
import org.thespheres.betula.services.jms.AppResourceEvent;

/**
 *
 * @author boris.heithecker
 */
@Dependent
@Stateless
public class AppResourcesNotificator {

    @Resource(mappedName = "jms/app-resources-topic")
    private Topic topic;

    @Inject
    @JMSConnectionFactory("jms/app-resources-topic-factory")
    private JMSContext context;

    public void notityConsumers(final String resource) {
        final JMSProducer messageProducer = context.createProducer();
        final AppResourceEvent tm = new AppResourceEvent(resource);
        messageProducer.send(topic, tm);
    }

}
