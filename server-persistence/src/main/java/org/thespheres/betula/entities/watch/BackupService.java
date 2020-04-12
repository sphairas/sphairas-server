/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.watch;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import org.thespheres.betula.entities.config.AppProperties;
import org.thespheres.betula.server.beans.config.ConfigUtil;

/**
 *
 * @author boris.heithecker
 */
@Stateless
@LocalBean
public class BackupService {

    @EJB
    private MailAdmin mailAdmin;
    @Resource(mappedName = "jdbc/betuladb")
    private DataSource source;

    private final static DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final static DateTimeFormatter NOW = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm");
    private final String[] BASE_COMMAND = new String[]{"7z", "a", "-t7z", "-m0=LZMA", "-mx=9"};
    private final String[] BASE_COMMAND_RM = new String[]{"rm", "-R"};

    //    @Transactional(TxType.NEVER)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Schedule(dayOfWeek = "Mon-Sun",
            month = "*",
            hour = "3", //"4", //"8-23",   "4,10,12,14,16", 
            dayOfMonth = "*",
            year = "*",
            minute = "5",
            second = "0",
            persistent = false)
    public void doCompress() {
        if (Boolean.getBoolean(AppProperties.COMPRESS_TERMGRADE_ENTRIES)) {
            final LocalDateTime now = LocalDateTime.now();
            try (Connection conn = source.getConnection()) {
                try (CallableStatement cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE('DBCLIENT', 'TERMGRADE_TARGETASSESSMENT_DOCUMENT_ENTRIES', 1)")) {
                    cs.execute();
                }
                Logger.getLogger(BackupService.class.getCanonicalName()).log(Level.INFO, "Successfully compressed TERMGRADE_TARGETASSESSMENT_DOCUMENT_ENTRIES at {0}", now.format(NOW));
            } catch (Exception ex) {
                Logger.getLogger(BackupService.class.getCanonicalName()).log(Level.SEVERE, "An error has occurred compressing TERMGRADE_TARGETASSESSMENT_DOCUMENT_ENTRIES.", ex);
            }
        }
    }

//    @Transactional(TxType.NEVER)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Schedule(dayOfWeek = "Mon-Sun",
            month = "*",
            hour = "3", // Außerhalb der Eingabezeiten
            //            hour = "3,10,15,20",
            dayOfMonth = "*",
            year = "*",
            minute = "10",
            second = "0",
            persistent = false)
    public void doBackup() {
        if (Boolean.getBoolean(AppProperties.DO_BACKUP)) {
            LocalDateTime now = LocalDateTime.now();
            Exception exception = null;
            try {
                try (Connection conn = source.getConnection()) {
                    String backupdirectory = ConfigUtil.backupDir() + NOW.format(now);
                    try (CallableStatement cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)")) {
                        cs.setString(1, backupdirectory);
                        cs.execute();
                    }
                    Logger.getLogger(BackupService.class.getCanonicalName()).log(Level.INFO, "Successfully called backup statement to {0}", backupdirectory);
                }
            } catch (Exception ex) {
                exception = ex;
                Logger.getLogger(BackupService.class.getCanonicalName()).log(Level.SEVERE, "Could not write backup.", ex);
            }
            mailAdmin.mailBackupStatus(now, null, null, exception);
        }
    }

    //soll 4 h 10
    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Schedule(dayOfWeek = "Mon-Sun",
            month = "*",
            hour = "4", //"4", //"8-23",   "4,10,12,14,16", 
            dayOfMonth = "*",
            year = "*",
            minute = "10",
            second = "0",
            persistent = false)
    public void doCompressBackup() {
        if (Boolean.getBoolean(AppProperties.DO_BACKUP)) {
            String domainRoot = System.getProperty("com.sun.aas.instanceRoot");
            File log = new File(domainRoot + "/logs/7zlog.txt");

            Integer result = null;
            Exception exception = null;
            final LocalDateTime ldtNow = LocalDateTime.now();
            final LocalDate now = LocalDate.now();
            try {
                String file = now.minusDays(1l).format(DAY);
//            String backupdirectory = installPath + "/db-backup/" + file + "*";
                String backupdirectory = file + "*";
                final long time = System.currentTimeMillis();
//            ProcessBuilder pb = new ProcessBuilder("7z", "a", "-t7z -m0=LZMA -mx=9 -md=96m -mfb=256", "archiv_" + file + ".7z", backupdirectory);
//            ProcessBuilder pb = new ProcessBuilder("7z", "a", "-t7z", "-m0=LZMA", "-mx=9", "archiv_" + file + ".7z", backupdirectory);
                String other = System.getProperty(AppProperties.COMPRESS_BACKUP_7Z_ADD_SWITCHES);
                ArrayList<String> command = new ArrayList<>(9);
                Arrays.stream(BASE_COMMAND).forEach(command::add);
                if (other != null) {
                    Arrays.stream(other.split(" "))
                            .filter(s -> !s.isEmpty())
                            .forEach(command::add);
//                command.add("-md=96m");
//                command.add(-mfb = 256);
                }
                command.add("archiv_" + file + ".7z");
                command.add(backupdirectory);
                ProcessBuilder pb = new ProcessBuilder(command);

//            Map<String, String> env = pb.environment();
//            env.put("VAR1", "myValue");
//            env.remove("OTHERVAR");
//            env.put("VAR2", env.get("VAR1") + "suffix");
                pb.directory(new File(ConfigUtil.backupDir()));
//            pb.redirectErrorStream(true);
                pb.redirectError(Redirect.appendTo(log));
                pb.redirectOutput(Redirect.appendTo(log));
//            pb.redirectOutput(Redirect.INHERIT); //.appendTo(log));
//            pb.redirectError(Redirect.INHERIT);
                Process p = pb.start();
//            assert pb.redirectInput() == Redirect.PIPE;
//            assert pb.redirectOutput().file() == log;
//            assert p.getInputStream().read() == -1;
                result = p.waitFor();

//            conn.commit(); //ZGN
//            conn.close();
                final long dur = System.currentTimeMillis() - time;
                Logger.getLogger(BackupService.class.getName()).log(Level.INFO, "Executed 7z-compression of {0} to {1} in {2}ms; process has exited with exit value {3}", new Object[]{backupdirectory, file, dur, result});
            } catch (Exception ex) {
                exception = ex;
                Logger.getLogger(BackupService.class.getName()).log(Level.SEVERE, "An error has ocurred executing backup compression.", ex);
            }

            if (exception != null || result == null || result != 0) {
                mailAdmin.mailCompressAndRemoveStatus(ldtNow, null, null, result, exception);
            }

            String file = null;
            try {
                file = now.minusDays(4l).format(DAY) + "*";

                final Path dir = Paths.get(ConfigUtil.backupDir());
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, file)) {
                    for (Path f : stream) {
                        result = deleteDir(f, dir, log);
                        if (result != 0) {
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                exception = ex;
                Logger.getLogger(BackupService.class.getName()).log(Level.SEVERE, "An error has occurred removing file {0} ({1}).", new Object[]{file, ex});
            }

            if (exception != null || result == null || result != 0) {

            }
            mailAdmin.mailCompressAndRemoveStatus(ldtNow, null, null, result, exception);
        }
    }

    private int deleteDir(final Path dir, final Path baseDir, final File log) throws IOException, InterruptedException {
        //                    if (Files.deleteIfExists(f)) {
//                        Logger.getLogger(BackupService.class.getName()).log(Level.INFO, "Removed backup directory {0}.", f.toAbsolutePath().toString());
//                    }
        String deleteDir = baseDir.relativize(dir).toString();
        ArrayList<String> command = new ArrayList<>(2);
        Arrays.stream(BASE_COMMAND_RM).forEach(command::add);
        command.add(deleteDir);
        ProcessBuilder pb = new ProcessBuilder(command);
//        pb.directory(new File(installPath + "/db-backup/"));
        pb.directory(baseDir.toFile());
        pb.redirectError(Redirect.appendTo(log));
        pb.redirectOutput(Redirect.appendTo(log));
        Process p = pb.start();
        int result = p.waitFor();
        Logger.getLogger(BackupService.class.getName()).log(Level.INFO, "Removed backup directory {0} with result {1}", new Object[]{dir.toString(), result});
        return result;
    }
}
