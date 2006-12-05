/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DemoConsoleChatPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DemoConsoleChatPackTest.class);
  }

  public void testForVerification() throws Exception {
	  	int  iCount = verifyConsoleChatFolderFiles();
	    assertEquals(6, iCount);
	    int  iSrcCount = verifyConsoleChatSrcFolderFiles();
	    assertEquals(1, iSrcCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	Verify that the 2 folders and 4 files exists under terracotta-2.1.0\dso\samples\consoleChat folder
  public int verifyConsoleChatFolderFiles() throws Exception {
		 int iConsoleChatFiles = 0;		 
		 String sConsoleChatFolder = null;		 	
		 System.out.println("************************************");
		 sConsoleChatFolder = System.getProperty("ConsoleChatdir.value");		 
		 System.out.println("files location="+sConsoleChatFolder);
		 File f = new File(sConsoleChatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iConsoleChatFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iConsoleChatFiles;		 	  
  }
  
 //Verify that the Chatter.java 1 file exists under terracotta-2.1.0\dso\samples\consoleChat\src\demo\consolechat folder
  public int verifyConsoleChatSrcFolderFiles() throws Exception {
		 int iConsoleChatSrcFiles = 0;		 
		 String sConsoleChatSrcFolder = null;		 	
		 System.out.println("************************************");
		 sConsoleChatSrcFolder = System.getProperty("Consolesrcfile.value");		 
		 System.out.println("files location="+sConsoleChatSrcFolder);
		 File f = new File(sConsoleChatSrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iConsoleChatSrcFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iConsoleChatSrcFiles;
  }  
}