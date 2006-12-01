/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SessionsTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SessionsTest.class);
  }

  public void testForVerification1() throws Exception {
	  	String os = System.getProperty("os.name");
	    System.out.println(os);
	    int iSessionCount = verifySessionsFolderFiles();
	    int iSessionBinCount = verifySessionBinFolderFiles();
	    if (os != null && os.startsWith("Lin") || os.startsWith("Sun")){	    	
	    	assertEquals(5, iSessionCount);
	    	assertEquals(7, iSessionBinCount);
	    	    	
	    } else {
	    	assertEquals(10, iSessionCount);
	    	assertEquals(8, iSessionBinCount);
	    	int  iSessionUninstallCount = verifySessionUninstallFolderFiles(); 
		    assertEquals(8, iSessionUninstallCount);
		    int  iSessionUninstallResCount = verifySessionUninstallResFolderFiles(); 
		    assertEquals(3, iSessionUninstallResCount);	    	
	    } 			    			   	    	    
	    int iSessionConfigCount = verifySessionConfigSampleFolderFiles();
	    assertEquals(3, iSessionConfigCount);
	    int  iSessionLibexecCount = verifySessionLibexecFolderFiles(); 
	    assertEquals(1, iSessionLibexecCount);	    
  }   
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }    
  //Verify that the 7 folders and 3 files exists in the terracotta-2.1.0\sessions
  public int verifySessionsFolderFiles() throws Exception {
		 int iSessionsFiles = 0;		 
		 String sSessionsFolder = null;		 	
		 System.out.println("************************************");
		 sSessionsFolder = System.getProperty("sessions.dir");		 
		 System.out.println("files location="+sSessionsFolder);
		 File f = new File(sSessionsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSessionsFiles = fArray.length;
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
		 return iSessionsFiles;		 	  
  }  
  //Verify that the 8 files exists in the terracotta-2.1.0\sessions\bin  
  public int verifySessionBinFolderFiles() throws Exception {
		 int iSessionBinFiles = 0;		 
		 String sSessionBinFolder = null;		 	
		 System.out.println("************************************");
		 sSessionBinFolder = System.getProperty("sessionsbin.dir");		 
		 System.out.println("files location="+sSessionBinFolder);
		 File f = new File(sSessionBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSessionBinFiles = fArray.length;
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
		 return iSessionBinFiles;		 	  
  }
  //Verify that the 3 files exists in the terracotta-2.1.0\sessions\config-sample 
  public int verifySessionConfigSampleFolderFiles() throws Exception {
		 int iSessionConfigSampleFiles = 0;		 
		 String sSessionConfigSampleFolder = null;		 	
		 System.out.println("************************************");
		 sSessionConfigSampleFolder = System.getProperty("sessionsconfigsample.dir");		 
		 System.out.println("files location="+sSessionConfigSampleFolder);
		 File f = new File(sSessionConfigSampleFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSessionConfigSampleFiles = fArray.length;
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
		 return iSessionConfigSampleFiles;		 	  
  }  
  //Verify that the 1 file exists in the terracotta-2.1.0\sessions\libexec 
  public int verifySessionLibexecFolderFiles() throws Exception {
		 int iSessionLibexecFiles = 0;		 
		 String sSessionLibexecFolder = null;		 	
		 System.out.println("************************************");
		 sSessionLibexecFolder = System.getProperty("sessionslibexec.dir");		 
		 System.out.println("files location="+sSessionLibexecFolder);
		 File f = new File(sSessionLibexecFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSessionLibexecFiles = fArray.length;
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
		 return iSessionLibexecFiles;		 	  
  }  
  //Verify that the 1 folder and 7 files exists in the terracotta-2.1.0/sessions/uninstall 
  public int verifySessionUninstallFolderFiles() throws Exception {
		 int iSessionUninstallFiles = 0;		 
		 String sSessionUninstallFolder = null;		 	
		 System.out.println("************************************");
		 sSessionUninstallFolder = System.getProperty("sessionsuninstall.dir");		 
		 System.out.println("files location="+sSessionUninstallFolder);
		 File f = new File(sSessionUninstallFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSessionUninstallFiles = fArray.length;
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
		 return iSessionUninstallFiles;		 	  
  }
  //Verify that the 3 files exists in the terracotta-2.1.0/sessions/uninstall/resource 
  public int verifySessionUninstallResFolderFiles() throws Exception {
		 int iSessionUninstallResFiles = 0;		 
		 String sSessionUninstallResFolder = null;		 	
		 System.out.println("************************************");
		 sSessionUninstallResFolder = System.getProperty("sessionsuninstallres.dir");		 
		 System.out.println("files location="+sSessionUninstallResFolder);
		 File f = new File(sSessionUninstallResFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSessionUninstallResFiles = fArray.length;
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
		 return iSessionUninstallResFiles;			 
  }
}