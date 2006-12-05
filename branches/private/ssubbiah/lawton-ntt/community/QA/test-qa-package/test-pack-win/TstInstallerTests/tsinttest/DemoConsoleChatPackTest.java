/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;

public class DemoConsoleChatPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DemoConsoleChatPackTest.class);
  }

  public void testForVerification() throws Exception {
	    boolean b = verifyConsoleChatFolder();
	    assertTrue("Demo Consolechat folder Exists!", b);
	    boolean d = verifyConsoleChatFolderFiles();
	    assertTrue("Demo Consolechat folder files Exists!", d);
	    boolean s = verifyConsoleChatSrcFolderFiles();
	    assertTrue("Demo Consolechat src folder files Exists!", s);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	Verify that the classes and src folder exists under demo/ConsoleChat folder
  public boolean verifyConsoleChatFolder() throws Exception { 
	     boolean bConsoleChatFolderExists = false;
		 String sConsoleChatFolder = null;
		 String sConsoleclassesFolder = null;
		 String sConsolesrcFolder = null;
		 
		 sConsoleChatFolder = System.getProperty("ConsoleChatdir.value");
		 sConsoleclassesFolder = System.getProperty("Consoleclassesdir.value");
		 sConsolesrcFolder = System.getProperty("Consolesrcdir.value");
		 
		 if(sConsoleChatFolder != null) {		 
			 System.out.println("Consolechat folder exists:" + sConsoleChatFolder);
			 bConsoleChatFolderExists = true;
		 }
		 if(sConsoleclassesFolder != null) {		 
			 System.out.println("Consolechat classes folder exists:" + sConsoleclassesFolder);
			 bConsoleChatFolderExists = true;
		 }
		 if(sConsolesrcFolder != null) {		 
			 System.out.println("Consolechat src folder exists:" + sConsolesrcFolder);
			 bConsoleChatFolderExists = true;
		 }
		 return bConsoleChatFolderExists;	  
  } 
  
  //Verify that the 4 files exists under demo/ConsoleChat folder
  public boolean verifyConsoleChatFolderFiles() throws Exception { 
	     boolean bConsoleChatFolderFilesExists = false;
		 String sConsolebuild = null;
		 String sConsolereadme = null;
		 String sConsolerundemo = null;
		 String sConsoletcconfig = null;		 
		 
		 sConsolebuild = System.getProperty("Consolebuild.value");
		 sConsolereadme = System.getProperty("Consolereadme.value");
		 sConsolerundemo = System.getProperty("Consolerundemo.value");
		 sConsoletcconfig = System.getProperty("Consoletcconfig.value");		 		 
		  
		 System.out.println("************************************");
		 if(sConsolebuild != null) {		 
			 System.out.println("ConsoleChat folder file1:" + sConsolebuild);
			 bConsoleChatFolderFilesExists = true;
		 }
		 if(sConsolereadme != null) {		 
			 System.out.println("ConsoleChat folder file2:" + sConsolereadme);
			 bConsoleChatFolderFilesExists = true;
		 }
		 if(sConsolerundemo != null) {		 
			 System.out.println("ConsoleChat folder file3:" + sConsolerundemo);
			 bConsoleChatFolderFilesExists = true;
		 }
		 if(sConsoletcconfig != null) {		 
			 System.out.println("ConsoleChat folder file4:" + sConsoletcconfig);
			 bConsoleChatFolderFilesExists = true;
		 }		 	 
		 System.out.println("************************************");
		 return bConsoleChatFolderFilesExists;	   
	}
 //Verify that the Chatter.java files exists under demo/ConsoleChat/src/demo/consolechat folder
  public boolean verifyConsoleChatSrcFolderFiles() throws Exception { 
	     boolean bConsoleChatSrcFolderFilesExists = false;
		 String sConsolesrcfile = null;		 	 
		 
		 sConsolesrcfile = System.getProperty("Consolesrcfile.value");		 		 		 
		  
		 System.out.println("************************************");
		 if(sConsolesrcfile != null) {		 
			 System.out.println("ConsoleChat src folder file1:" + sConsolesrcfile);
			 bConsoleChatSrcFolderFilesExists = true;
		 }		 
		 System.out.println("************************************");
		 return bConsoleChatSrcFolderFilesExists;	  
  }  
}