/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.calendar.watch;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.openide.util.NbBundle;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@LocalBean
public class CalendarMailAdmin {

    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm d.M.yyyy");
//    @Resource(name = "mail/iservMessaging")
//    private Session mailiserv;
//    @Resource(name = "mail/dfMessaging")
//    private Session maildf;

    @NbBundle.Messages({"CalendarMailAdmin.mailBackupStatus.subject=Calendar Backup-Status von {0}",
        "CalendarMailAdmin.mailBackupStatus=Calendar Backup ({1}) erfolgreich durchgeführt um {0}.",
        "CalendarMailAdmin.mailBackupStatus.provideStackTrace=Calendar Backup ({2}) nicht erfolgreich durchgeführt um {0}.\nServer-StackTrace: {1}"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void mailBackupStatus(final String prefix, final LocalDateTime ldt, final String from, final String name, final Exception exception) {
        InternetAddress sender = null;
        try {
            sender = new InternetAddress(from, name);
        } catch (UnsupportedEncodingException ex) {
            throw new EJBException(ex);
        }
        final String body;
        final String time = FORMATTER.format(ldt);
        if (exception == null) {
            body = NbBundle.getMessage(CalendarMailAdmin.class, "CalendarMailAdmin.mailBackupStatus", time, prefix);
        } else {
            final StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            body = NbBundle.getMessage(CalendarMailAdmin.class, "CalendarMailAdmin.mailBackupStatus.provideStackTrace", time, sw.toString(), prefix);
        }
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException unknownHostException) {
            hostName = "Unknown host";
        }
        String subject = NbBundle.getMessage(CalendarMailAdmin.class, "CalendarMailAdmin.mailBackupStatus.subject", hostName);
        doMail(subject, sender, body);
    }

    @NbBundle.Messages({"CalendarMailAdmin.mailCompressAndRemoveStatus.subject=Calendar CompressAndRemove-Status von {0}",
        "CalendarMailAdmin.mailCompressAndRemoveStatus=Calendar CompressAndRemove ({1}) erfolgreich durchgeführt um {0}.",
        "CalendarMailAdmin.mailCompressAndRemoveStatus.provideStackTrace=Calendar CompressAndRemove ({3}) nicht erfolgreich durchgeführt mit Exit-Status {1} um {0}.\nServer-StackTrace: {2}"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void mailCompressAndRemoveStatus(final String prefix, final LocalDateTime ldt, final String from, final String name, Integer exit, final Exception exception) {
        InternetAddress sender = null;
        try {
            sender = new InternetAddress(from, name);
        } catch (UnsupportedEncodingException ex) {
            throw new EJBException(ex);
        }
        final String body;
        final String time = FORMATTER.format(ldt);
        if (exception == null) {
            body = NbBundle.getMessage(CalendarMailAdmin.class, "CalendarMailAdmin.mailCompressAndRemoveStatus", time, prefix);
        } else {
            final StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            final String exitText = exit != null ? Integer.toString(exit) : "null";
            body = NbBundle.getMessage(CalendarMailAdmin.class, "CalendarMailAdmin.mailCompressAndRemoveStatus.provideStackTrace", time, exitText, sw.toString(), prefix);
        }
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException unknownHostException) {
            hostName = "Unknown host";
        }
        String subject = NbBundle.getMessage(CalendarMailAdmin.class, "CalendarMailAdmin.mailCompressAndRemoveStatus.subject", hostName);
        doMail(subject, sender, body);
    }

    private void doMail(String subject, InternetAddress sender, final String body) throws EJBException {
//        MimeMessage message = new MimeMessage(mailiserv);
//        try {
//            message.setSubject(subject);
//            message.setRecipients(Message.RecipientType.TO, "xxxxxxxxxxxxxx@xxxxxxxxxxx"); //InternetAddress.parse(email, false));
//            message.setSender(sender);
//            message.setReplyTo(new InternetAddress[]{sender});
//            message.setText(body);
//            Transport.send(message, "xxxxxxxxxx", "xxxxxxxxxxxxxxxx");
//        } catch (MessagingException ex) {
//            throw new EJBException(ex);
//        }
//        try {
//            MimeMessage message2 = new MimeMessage(maildf);
//            message2.setSubject(subject);
//            message2.setRecipients(Message.RecipientType.TO, "xxxxxxxxxxxxxx@xxxxxxxxxxxe"); //InternetAddress.parse(email, false));
//            InternetAddress sender2 = new InternetAddress("xxxxxxxxxxxxxx@xxxxxxxxxxx", "XXXXXXXXXX");
//            message2.setSender(sender2);
//            message2.setReplyTo(new InternetAddress[]{sender2});
//            message2.setText(body);
//            Transport.send(message2, "xxxxxxxxxxxxxx@xxxxxxxxxxx", "xxxxxxxxxxxxxxxxxxxxxxxxxx");
//            Store store = maildf.getStore("imap");
//            store.connect("imap.xxxxxxxxxxxxxxx.de", 993,"xxxxxxxxxxxxxx@xxxxxxxxxxx", "xxxxxxxxxxxxxx");
//            store.getFolder("inbox");
//            store.close();
//        } catch (Exception ex) {
//            Logger.getLogger(CalendarMailAdmin.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
//        }
        Properties p = new Properties();
        p.put("mail.smtp.host", "smtp.xxxxxxxxxxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx");

        p.put("mail.smtp.socketFactory.port", "465");
        p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.port", "465");

        Session session = Session.getInstance(p);
        try {
            MimeMessage message2 = new MimeMessage(session);
            message2.setSubject(subject);
            message2.setRecipients(Message.RecipientType.TO, "xxxxxxxxxxxxxx@xxxxxxxxxxx"); //InternetAddress.parse(email, false));
            InternetAddress sender2 = new InternetAddress("xxxxxxxxxxxxxx@xxxxxxxxxxx", "Monitoring");
            message2.setSender(sender2);
            message2.setReplyTo(new InternetAddress[]{sender2});
            message2.setText(body);
            Transport.send(message2, "xxxxxxxxxxxxxx@xxxxxxxxxxx", "xxxxxxxxxxxxxxxx");
//            Store store = session.getStore("imap");
//            store.connect("imap.xxxxxxxxxxxxxxxx.de", 993, "xxxxxxxxxxxxxx@xxxxxxxxxxx", "xxxxxxxxxxxxxxxx");
//            store.getFolder("inbox");
//            store.close();
        } catch (Exception ex) {
            Logger.getLogger(CalendarMailAdmin.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}
