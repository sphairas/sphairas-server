/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.servlet.http.HttpServletRequest;
import org.thespheres.betula.StudentId;
import org.thespheres.betula.TermId;
import org.thespheres.betula.Ticket;
import org.thespheres.betula.UnitId;
import org.thespheres.betula.document.DocumentId;
import org.thespheres.ical.UID;
import org.thespheres.ical.VCard;

/**
 *
 * @author boris.heithecker
 */
public class Utilities {

    private Utilities() {
    }

    public static DocumentId extractDocumentId(HttpServletRequest request) {
        String docAuth = request.getParameter("document.authority");
        String docId = request.getParameter("document.id");
        if (docAuth != null || docId != null) {
            try {
                docAuth = URLDecoder.decode(docAuth, "utf-8");
                docId = URLDecoder.decode(docId, "utf-8");
                return new DocumentId(docAuth, docId, DocumentId.Version.LATEST);
            } catch (UnsupportedEncodingException ex) {
            }
        }
        return null;
    }

    public static UnitId extractUnitId(HttpServletRequest request) {
        String unitAuth = request.getParameter("unit.authority");
        String unitId = request.getParameter("unit.id");
        if (unitAuth != null || unitId != null) {
            try {
                unitAuth = URLDecoder.decode(unitAuth, "utf-8");
                unitId = URLDecoder.decode(unitId, "utf-8");
                return new UnitId(unitAuth, unitId);
            } catch (UnsupportedEncodingException ex) {
            }
        }
        return null;
    }

    public static TermId extractTermId(HttpServletRequest request) {
        String termAuth = request.getParameter("term.authority");
        String termId = request.getParameter("term.id");
        if (termAuth != null || termId != null) {
            try {
                termAuth = URLDecoder.decode(termAuth, "utf-8");
                termId = URLDecoder.decode(termId, "utf-8");
                int id = Integer.valueOf(termId);
                return new TermId(termAuth, id);
            } catch (UnsupportedEncodingException | NumberFormatException ex) {
            }
        }
        return null;
    }

    public static StudentId extractStudentId(HttpServletRequest request) {
        String studAuth = request.getParameter("student.authority");
        String studId = request.getParameter("student.id");
        if (studAuth != null || studId != null) {
            try {
                studAuth = URLDecoder.decode(studAuth, "utf-8");
                studId = URLDecoder.decode(studId, "utf-8");
                long id = Long.valueOf(studId);
                return new StudentId(studAuth, id);
            } catch (UnsupportedEncodingException | NumberFormatException ex) {
            }
        }
        return null;
    }

    public static Ticket extractTicket(HttpServletRequest request) {
        String auth = request.getParameter("ticket.authority");
        String idValue = request.getParameter("ticket.id");
        if (auth != null || idValue != null) {
            try {
                auth = URLDecoder.decode(auth, "utf-8");
                idValue = URLDecoder.decode(idValue, "utf-8");
                Long id = Long.parseLong(idValue);
                return new Ticket(auth, id);
            } catch (UnsupportedEncodingException | NumberFormatException ex) {
            }
        }
        return null;
    }

    public static UID extractUID(HttpServletRequest request) {
        String host = request.getParameter("uid.host");
        String id = request.getParameter("uid.id");
        if (host != null || id != null) {
            try {
//                host = URLDecoder.decode(host, "utf-8");
//                id = URLDecoder.decode(id, "utf-8");
                return new UID(host, id);
            } catch (NumberFormatException ex) {
            }
        }
        return null;
    }

    public static LocalDate extractLocalDate(final HttpServletRequest request, final String parameterName) {
        String host = request.getParameter(parameterName);
        if (host != null) {
            try {
                return LocalDate.parse(host);
            } catch (DateTimeParseException ex) {
            }
        }
        return null;
    }

    public static String formatFullname(VCard card) {
        return card.getAnyPropertyValue(VCard.N)
                .map(n -> n.split(";"))
                .filter(arr -> arr.length >= 2)
                .map(arr -> {
                    String vorname = arr[1].replace(",", " ");
                    return vorname + " " + arr[0];
                })
                .orElse(card.getFN());

    }

    public static String findFamilyName(VCard card) {
        return card.getAnyPropertyValue(VCard.N)
                .map(n -> n.split(";"))
                .filter(arr -> arr.length >= 1)
                .map(arr -> arr[0])
                .orElse(card.getFN());
    }
    
        public static String findGivenName(VCard card) {
        return card.getAnyPropertyValue(VCard.N)
                .map(n -> n.split(";"))
                .filter(arr -> arr.length >= 2)
                .map(arr -> arr[1].replace(",", " "))
                .orElse(card.getFN());
    }
}
