/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.students;

import org.thespheres.betula.services.dav.CardDavProp;
import org.thespheres.betula.services.dav.AddressData;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.calendar.facade.StudentFacade;
import org.thespheres.betula.server.beans.Utilities;
import org.thespheres.betula.services.vcard.VCardStudentsCollection;
import org.thespheres.betula.services.dav.Multistatus;
import org.thespheres.betula.services.dav.PropStat;
import org.thespheres.betula.services.dav.Response;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
public class StudentsServlet extends HttpServlet {

//    private static final String METHOD_PROPFIND = "PROPFIND";
    private static final String METHOD_REPORT = "REPORT";
    private static JAXBContext collectionJAXB;
    private static JAXBContext multiStatusJAXB;
    @EJB
    private StudentFacade facade;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String method = req.getMethod();
        if (method.equals(METHOD_REPORT)) {
            doGet(req, resp);
        } //else if (method.equals(METHOD_PROPFIND)) {
        //            doPropfind(req, resp);
        //        } //        else if (method.equals(METHOD_PROPPATCH)) {
        //            doProppatch(req, resp);
        //        } else if (method.equals(METHOD_MKCOL)) {
        //            doMkcol(req, resp);
        //        } else if (method.equals(METHOD_COPY)) {
        //            doCopy(req, resp);
        //        } else if (method.equals(METHOD_MOVE)) {
        //            doMove(req, resp);
        //        } else if (method.equals(METHOD_LOCK)) {
        //            doLock(req, resp);
        //        } else if (method.equals(METHOD_UNLOCK)) {
        //            doUnlock(req, resp);
        //        } 
        else {
            // DefaultServlet processing
            super.service(req, resp);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet StudentsServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet StudentsServlet at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        final StudentId student = Utilities.extractStudentId(req);
        if (student == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        facade.remove(student);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<StudentEntity> cq = cb.createQuery(StudentEntity.class);
//        Root<StudentEntity> sroot = cq.from(StudentEntity.class);
//        cq.select(sroot).distinct(true);
        final UnitId unit = Utilities.extractUnitId(request);
        Collection<VCard> l;
        if (unit != null) {
            l = facade.findAllVCards(unit); //em.createQuery(cq).getResultList();
        } else {
            l = facade.findAllVCards();
        }
        Multistatus ms = new Multistatus();
        l.stream().forEach((card) -> {
            String vcard = card.toString();
//            Response resp = new Response(se.getStudentId().getId().toString());
            Response resp = new Response(card.getAnyPropertyValue("X-STUDENT").get());
            PropStat ps = new PropStat();
            ps.setStatus("HTTP/1.1 200 OK"); //HTTP/1.1 200 OK
            CardDavProp prop = new CardDavProp();
            prop.setAddressData(new AddressData(vcard));
            ps.setProp(prop);
            resp.getPropstat().add(ps);
            ms.getResponses().add(resp);
        });
        response.setCharacterEncoding("utf-8");
//        JAXBContext ctx;
        try {
//            ctx = JAXBContext.newInstance(Propertyupdate.class.getPackage().getName() + ":" + CardDavProp.class.getPackage().getName());
//            ctx = JAXBContext.newInstance(Multistatus.class, CardDavProp.class);
            response.setStatus(207);
            getMultiStatusJAXB().createMarshaller().marshal(ms, response.getWriter());
        } catch (JAXBException ex) {
            Logger.getLogger(StudentsServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
//        Multistatus.marshal(ms, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final VCardStudentsCollection coll;
        try (InputStream is = request.getInputStream()) {
            coll = (VCardStudentsCollection) getCollectionJAXB().createUnmarshaller().unmarshal(is);
        } catch (JAXBException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        coll.getAll().forEach((id, card) -> updateVCard(id, card));
    }

    private void updateVCard(StudentId id, VCard card) {
        facade.create(id, card.getFN());
        card.getPropertyNames().stream().forEach(n -> {
            card.getProperties(n).stream().forEach(p -> {
                facade.update(id, p);
            });
        });
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    static JAXBContext getCollectionJAXB() {
        if (collectionJAXB == null) {
            try {
                collectionJAXB = JAXBContext.newInstance(VCardStudentsCollection.class);
            } catch (JAXBException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return collectionJAXB;
    }

    static JAXBContext getMultiStatusJAXB() {
        if (multiStatusJAXB == null) {
            try {
                multiStatusJAXB = JAXBContext.newInstance(Multistatus.class, CardDavProp.class);
            } catch (JAXBException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return multiStatusJAXB;
    }
}
