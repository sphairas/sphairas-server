/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.entities.config;

import java.util.Hashtable;
import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.thespheres.betula.services.ServiceConstants;

/**
 *
 * @author boris.heithecker
 */
@LocalBean
//@Stateless
@Singleton
public class AppResourcesContext extends ProxyDirContext {

    public AppResourcesContext() throws Exception {
        this(new Hashtable<>(), new AppFileDirContext());
    }

    private AppResourcesContext(Hashtable<String, String> env, FileDirContext fileDirCtx) throws Exception {
        super(env, fileDirCtx);
    }

    @PostConstruct
    public void init() {
        final FileDirContext fileDirCtx = (FileDirContext) getDirContext();
        final String base = System.getProperty("com.sun.aas.instanceRoot") + "/" + ServiceConstants.APP_RESOURCES + "/";
        fileDirCtx.setDocBase(base);
    }

    @Override
    public String getContextName() {
        return super.getContextName();
    }

    @Override
    public String getHostName() {
        return super.getHostName();
    }

}
