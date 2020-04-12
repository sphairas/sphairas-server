/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author boris.heithecker
 */
@ServiceProvider(service = Lookup.class)
public class MainLookup extends ProxyLookup {

    private final InstanceContent ic = new InstanceContent();
    private final AbstractLookup lkp = new AbstractLookup(ic);
//    private boolean initialized;
//    private final RequestProcessor RP = new RequestProcessor(MainLookup2.class);
//    private final RequestProcessor.Task init;

    public MainLookup() {
//        final ClassLoader cl = new ContextClassLoaderProxy(); //Thread.currentThread().getContextClassLoader();
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final Lookup ms = Lookups.metaInfServices(cl);
        ic.add("syscl", new ClassLoaderConvertor("syscl"));
        setLookups(new Lookup[]{ms, lkp}); //, Lookups.singleton(cl)});
//        init = RP.post(this::doInitializeLookup); // Geht nicht, RP.post ruft Lookup.getDefault auf
    }

//    @Override
//    protected synchronized void beforeLookup(Template<?> template) {
////        if (!init.isFinished() && !RP.isRequestProcessorThread()) {
////            init.waitFinished();
////        }    
//        super.beforeLookup(template);
//    }
    private void doInitializeLookup() {

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final Lookup ms = Lookups.metaInfServices(cl);
        final Lookup services = Lookups.forPath("/Services");
//        final Lookup[] arr = new Lookup[]{
//            getLookups()[0], // metaInfServices
//            getLookups()[1], // ClassLoader
//            //            getLookups()[2], // ModuleInfo lookup
//            ICLKP,
//            services};
        final Lookup[] arr = new Lookup[]{
            ms, // metaInfServices
            Lookups.singleton(cl), // ClassLoader
            //            getLookups()[2], // ModuleInfo lookup
            new AbstractLookup(ic),
            services};
        setLookups(arr);
//        initialized = true;
    }

    public void register(Object obj) {
        ic.add(obj);
    }

    public void remove(Object obj) {
        ic.remove(obj);
    }
//  TODO: beforeLookup check initialized!!  

    private static class ClassLoaderConvertor implements InstanceContent.Convertor<String, ClassLoader> {

        private final String id;

        private ClassLoaderConvertor(String id) {
            this.id = id;
        }

        @Override
        public ClassLoader convert(String id) {
            if (id.equals(this.id)) {
                return Thread.currentThread().getContextClassLoader();
            }
            throw new IllegalArgumentException();
        }

        @Override
        public Class<? extends ClassLoader> type(String id) {
            return ClassLoader.class;
        }

        @Override
        public String id(String id) {
            return this.id;
        }

        @Override
        public String displayName(String id) {
            return this.id;
        }

    }
}
