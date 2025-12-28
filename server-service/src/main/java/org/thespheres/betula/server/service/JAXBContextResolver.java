//package org.thespheres.betula.server.service;
//
//import jakarta.ws.rs.Produces;
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.ext.ContextResolver;
//import jakarta.ws.rs.ext.Provider;
//import org.thespheres.betula.database.DBAdminTask;
//import org.thespheres.betula.database.DBAdminTaskResult;
//import org.thespheres.betula.document.Container;
//
//@Provider
//@Produces({MediaType.APPLICATION_XML}) //, MediaType.APPLICATION_JSON}) // Handle both types
//public class JAXBContextResolver implements ContextResolver<javax.xml.bind.JAXBContext> {
//
//    private final javax.xml.bind.JAXBContext context;
//
//    public JAXBContextResolver() throws javax.xml.bind.JAXBException {
//        // Initialize once for the application
//        this.context = javax.xml.bind.JAXBContext.newInstance(Container.class, DBAdminTask.class, DBAdminTaskResult.class);
//    }
//
//    @Override
//    public javax.xml.bind.JAXBContext getContext(Class<?> type) {
//        // Ensure this context is used for your specific classes
//        if (type.equals(Container.class)
//                || type.equals(DBAdminTask.class)
//                || type.equals(DBAdminTaskResult.class)) {
//            return context;
//        }
//        return null;
//    }
//}
