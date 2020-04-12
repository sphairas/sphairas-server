/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.server.beans.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.Repository;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;
import org.thespheres.betula.services.ServiceConstants;

/**
 *
 * @author boris.heithecker
 */
@Messages("LayerProvider.message.noLayer=No layer.xml registered.")
@ServiceProvider(service = Repository.LayerProvider.class)
public class AppLayerProvider extends Repository.LayerProvider {

    static Path layerPath() {
        final String instance = System.getProperty("com.sun.aas.instanceRoot");
        return Paths.get(instance, ServiceConstants.APP_RESOURCES, "layer.xml");
    }

    public AppLayerProvider() {
        LayerFileWatch.getInstance();
    }

    @Override
    protected void registerLayers(Collection<? super URL> context) {
        final Path config = layerPath();
        if (Files.exists(config)) {
            try {
                final URL url = config.toUri().toURL();
                context.add(url);
            } catch (MalformedURLException ex) {
                Logger.getLogger(AppLayerProvider.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            final String msg = NbBundle.getMessage(AppLayerProvider.class, "LayerProvider.message.noLayer");
            Logger.getLogger(AppLayerProvider.class.getName()).log(Level.CONFIG, msg);
        }
    }

    void refreshLayer() {
        super.refresh();
    }
}
