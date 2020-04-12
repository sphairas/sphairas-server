/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.Repository;
import org.openide.util.Lookup;
import static org.thespheres.betula.server.beans.config.AppLayerProvider.layerPath;

/**
 *
 * @author boris.heithecker
 */
final class LayerFileWatch implements Runnable {

    private static final LayerFileWatch[] INSTANCE = new LayerFileWatch[]{null};

    private final ExecutorService executor;
    private final WatchService watcher;
    private static final Logger LOGGER = Logger.getLogger(LayerFileWatch.class.getName());

    @SuppressWarnings("LeakingThisInConstructor")
    private LayerFileWatch() throws IOException {
        final Path dirPath = layerPath().getParent();
        final FileSystem fs = dirPath.getFileSystem();
        watcher = fs.newWatchService();
        dirPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this);
    }

    static LayerFileWatch getInstance() {
        synchronized (INSTANCE) {
            if (INSTANCE[0] == null) {
                try {
                    INSTANCE[0] = new LayerFileWatch();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error creating layer file watcher.", ex);
                }
            }
        }
        return INSTANCE[0];
    }

    void cleanup() {
        try {
            watcher.close();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Error closing layer file watcher.");
        }
        executor.shutdown();
    }

    @Override
    public void run() {
        while (true) {
            WatchKey key;
            try {
                // wait for a key to be available
                key = watcher.take();
            } catch (InterruptedException ex) {
                continue;
            }
            key.pollEvents().stream()
                    .filter(e -> (e.kind() != StandardWatchEventKinds.OVERFLOW))
                    .filter(e -> {
                        final Path changed = (Path) e.context();
                        return changed.getNameCount() == 1 && changed.endsWith("layer.xml");
                    })
                    .forEach(e -> {
                        final WatchEvent.Kind<?> kind = e.kind();
                        LOGGER.log(Level.INFO, "Detected layer file change: {0}", kind.name());
                        requestRefresh();
                    });
            // IMPORTANT: The key must be reset after processed
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }

    }

    void requestRefresh() {
        Lookup.getDefault().lookupAll(Repository.LayerProvider.class).stream()
                .filter(p -> AppLayerProvider.class.equals(p.getClass()))
                .map(p -> (AppLayerProvider) p)
                .forEach(AppLayerProvider::refreshLayer);

    }
}
