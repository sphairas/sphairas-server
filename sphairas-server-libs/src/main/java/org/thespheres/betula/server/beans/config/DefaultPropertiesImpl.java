/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.RequestProcessor;
import org.thespheres.betula.services.LocalFileProperties;

/**
 *
 * @author boris.heithecker
 */
class DefaultPropertiesImpl extends LocalFileProperties implements Runnable {

//    static final int MAX_WAIT_TIME = 10000;
    private final String file;
    private final Path dir;
    private final RequestProcessor watch = new RequestProcessor();
    private final RequestProcessor.Task init;
//    private final ThreadLocal<Boolean> bypassInitParentTask = ThreadLocal.withInitial(() -> Boolean.FALSE);

    DefaultPropertiesImpl(final String name, final Path dir, final String file) throws IOException {
        super(name, Files.newInputStream(dir.resolve(file)));
        this.dir = dir;
        this.file = file;
        init = watch.create(this::initParent);
    }
//
//    @Override
//    protected void ensureInitialized() {
//        super.ensureInitialized();
//        if (!bypassInitParentTask.get()) {
//            try {
//                init.waitFinished(MAX_WAIT_TIME);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(DefaultPropertiesImpl.class.getCanonicalName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
//            }
//        }
//    }

    void init() {
        init.schedule(0);
    }

    private void initParent() {
        Optional.ofNullable(getProperty("super.providerURL"))
                .map(LocalFileProperties::find)
                .ifPresent(this::setParent);
        watch.post(this);
    }

//    String getPropertyBypass(final String name, final String defaultValue) {
//        bypassInitParentTask.set(Boolean.TRUE);
//        try {
//            return getProperty(name, defaultValue);
//        } finally {
//            bypassInitParentTask.remove();
//        }
//    }

    @Override
    protected Path getOverridesDir() {
        return null;
    }

    private void reload() {
        try {
            super.load(Files.newInputStream(dir.resolve(file)));
        } catch (IOException ex) {
            Logger.getLogger(DefaultPropertiesImpl.class.getCanonicalName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
        }
        watch.post(this);
    }

    @Override
    public void run() {
        FileSystem fs = dir.getFileSystem();
        WatchService ws;
        try {
            ws = fs.newWatchService();
            dir.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        } catch (IOException ex) {
            Logger.getLogger(DefaultPropertiesImpl.class.getCanonicalName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            return;
        }
        watch:
        while (true) {
            WatchKey key;
            try {
                // wait for a key to be available
                key = ws.take();
            } catch (InterruptedException ex) {
                return;
            }
            for (final WatchEvent<?> event : key.pollEvents()) {
                //we only register "ENTRY_MODIFY" so the context is always a Path.
                final Path changed = (Path) event.context();
                if (changed.getNameCount() == 1 && changed.endsWith(file)) {
                    break watch;
                }
            }
            // IMPORTANT: The key must be reset after processed
            boolean valid = key.reset();
            if (!valid) {
                Logger.getLogger(DefaultPropertiesImpl.class.getName()).log(Level.FINE, "Invalid WatchKey.");
            }
        }
        try {
            if (ws != null) {
                ws.close();
            }
        } catch (IOException ex) {
        }
        reload();
    }
}
