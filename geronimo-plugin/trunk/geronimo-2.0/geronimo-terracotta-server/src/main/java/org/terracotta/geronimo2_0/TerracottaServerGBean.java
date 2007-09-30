package org.terracotta.geronimo2_0;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.system.serverinfo.ServerInfo;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.server.AbstractServerFactory;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;

/**
 * TerracottaServerGBean Gbean that allows the embedding of Terracotta Server
 * inside of Apache Geronimo
 * 
 * @author Jeff Genender
 */
public class TerracottaServerGBean implements GBeanLifecycle {

    private TCServer server = null;

    private String terracottaRootDir = null;

    private String tcConfigFile = null;

    private String serverName = null;

    public TerracottaServerGBean(String terracottaRootDir, String tcConfigFile, ServerInfo serverInfo, ClassLoader classLoader) throws Exception {

        if (classLoader == null) {
            // This should never happen since the classloader is a magic
            // attribute
            throw new IllegalArgumentException("classLoader cannot be null.");
        }

        if (serverInfo == null) {
            throw new IllegalArgumentException("Reference ServerInfo cannot be null.");
        }

        if (terracottaRootDir == null) {
            throw new IllegalArgumentException("Attribute terracottaRootDir cannot be null.");
        }
        this.terracottaRootDir = serverInfo.resolvePath(terracottaRootDir);
        File rootDir = new File(this.terracottaRootDir);
        if (!rootDir.exists()) {
            throw new IllegalArgumentException("Attribute terracottaRootDir (" + rootDir.getAbsolutePath() + ") does not exist.");
        }
        System.setProperty("tc.install-root", rootDir.getAbsolutePath());
        System.setProperty("geronimo-terracotta.home", rootDir.getAbsolutePath());

        if (tcConfigFile == null) {
            throw new IllegalArgumentException("Attribute tcConfigFile cannot be null.");
        }
        this.tcConfigFile = tcConfigFile;
        File configFile = new File(rootDir, this.tcConfigFile);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("Attribute tcConfigFile (" + configFile.getAbsolutePath() + ") does not exist.");
        }

        List<String> args = new ArrayList<String>();
        args.add("-f");
        args.add(configFile.getAbsolutePath());
        if (serverName != null) {
            args.add("-n");
            args.add(serverName);
        }

    }

    public void doFail() {
    }

    public void doStart() throws Exception {
        if (server == null) {
            // Declaring of the TCServerMain in TCLogging is just a hack since
            // the TCLogging can only use "com.tc"
            ThrowableHandler throwableHandler = new ThrowableHandler(TCLogging.getLogger(com.tc.server.TCServerMain.class));
            TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);
            TVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(getArgs(), true, new FatalIllegalConfigurationChangeHandler());
            AbstractServerFactory serverFactory = AbstractServerFactory.getFactory();
            server = new TCServerImpl(factory.createL2TVSConfigurationSetupManager(null), threadGroup);

        } 
        server.start();
    }

    public void doStop() throws Exception {
        if (server != null && server.isStarted()) {
            server.stop();
        }
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    private String[] getArgs() {
        File rootDir = new File(this.terracottaRootDir);
        File configFile = new File(rootDir, this.tcConfigFile);
        List<String> args = new ArrayList<String>();
        args.add("-f");
        args.add(configFile.getAbsolutePath());
        if (serverName != null) {
            args.add("-n");
            args.add(serverName);
        }

        return args.toArray(new String[args.size()]);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic("Terracotta Server", TerracottaServerGBean.class);

        infoFactory.addAttribute("terracottaRootDir", String.class, true);
        infoFactory.addAttribute("tcConfigFile", String.class, true);
        infoFactory.addAttribute("serverName", String.class, true);
        infoFactory.addReference("ServerInfo", ServerInfo.class, "GBean");
        infoFactory.addAttribute("classLoader", ClassLoader.class, false);

        infoFactory.setConstructor(new String[] { "terracottaRootDir", "tcConfigFile", "ServerInfo", "classLoader" });
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
