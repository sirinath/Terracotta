package org.terracotta.geronimo2_0;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.geronimo.system.serverinfo.BasicServerInfo;
import org.apache.geronimo.system.serverinfo.ServerInfo;
import org.apache.geronimo.testsupport.TestSupport;

/**
 * TerracottaServerGBeanTest
 * Unit test for the TerracottaServerGBean 
 * @author Jeff Genender
 *
 */
public class TerracottaServerGBeanTest extends TestSupport {

    public static final String ROOT_DIR = "var/terracotta";
    public static final String TEST_DIR = "src/test/deployables";
    public static final String FILE_NAME = "tc-config-geronimo.xml";
    private File targetDir;
    private File tcRootDir;
    
    @Override
    protected void setUp() throws Exception {
        targetDir = new File(BASEDIR, "target");
        tcRootDir = new File(targetDir, ROOT_DIR);
        
        //If the dirs existed before, then clear them
        deleteDir(tcRootDir);
        if (tcRootDir.exists()){
            throw new Exception("Cannot delete " + tcRootDir.getAbsolutePath() + " directory");
        }
        
        //Now create the directory structure
        if (!tcRootDir.mkdirs()){
            throw new Exception("Cannot create " + tcRootDir.getAbsolutePath() + " directory structure.");
        }
        
        File testDir = new File(BASEDIR, TEST_DIR);
        File testFile = new File(testDir, FILE_NAME);
        File destFile = new File(tcRootDir, FILE_NAME);
        copy(testFile, destFile);
        if (!destFile.exists()){
            throw new Exception("Cannot copy " + testFile.getAbsolutePath() + " to " + destFile.getAbsolutePath() + ".");
        }
    }

    public void testTerracottaServerGbean() throws Exception{
        ClassLoader cl = this.getClass().getClassLoader();
        
        
        ServerInfo serverInfo = new BasicServerInfo(targetDir.getAbsolutePath());
        
        //Create the gbean
        TerracottaServerGBean gbean = new TerracottaServerGBean(tcRootDir.getAbsolutePath(), FILE_NAME, serverInfo, cl);
        //gbean.setServerName("Geronimo");
        
        //Start the gbean
        gbean.doStart();
        
        gbean.doStop();
        
        //Check that directories were created (If Terracotta started, they would have been created properly)
        File serverData = new File(tcRootDir, "server-data");
        assertTrue(serverData.exists());
    }
    
    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
    
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }
}
