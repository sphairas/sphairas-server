/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Topic;
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
