package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;
import com.terracottatech.configV3.Plugin;

public class SimplePluginTestApp extends AbstractTransparentApp {
    private static boolean pluginsLoaded = false;
    
    public static synchronized boolean pluginsLoaded() {
        return pluginsLoaded;
    }

    public SimplePluginTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
        super(appId, cfg, listenerProvider);
    }

    public static synchronized void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper cfg) {
        Plugin plugin = cfg.getPlugins().addNewPlugin();
        plugin.setName("clustered-apache-struts");
        plugin.setVersion("1.1");
        pluginsLoaded = true;
    }

    public void run() {
    }

}
