/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DSOTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DSOTest.class);
  }

  public void testForVerification() throws Exception {
	    String os = System.getProperty("os.name");
	    System.out.println(os);
	    int  iDsoCount = verifyDSOFolderFiles(); //1
	    int  iDsoBinCount = verifyDsoBinFolderFiles(); //2
	    int  iDsoSamplesCount = verifyDsoSamplesFolderFiles(); //5	    
	    
	    if (os != null && os.startsWith("Lin") || os.startsWith("Sun")){	    	
	    	assertEquals(5, iDsoCount);	    	
	    	assertEquals(6, iDsoBinCount);	    	
	    	assertEquals(8, iDsoSamplesCount);	    	
	    } else {
	    	assertEquals(6, iDsoCount);
	    	assertEquals(7, iDsoBinCount);
	    	assertEquals(8, iDsoSamplesCount);
	    } 
	    	    
	    int  iDsoLibexecCount = verifyDsoLibexecFolderFiles(); //3
	    assertEquals(1, iDsoLibexecCount);	
	    int  iDsoSconfigCount = verifyDsoSconfigFolderFiles(); //4
	    assertEquals(2, iDsoSconfigCount);
	    
	    if (os != null && os.startsWith("Win")){           
	    	int  iDsoUninstallCount = verifyDsoUninstallFolderFiles(); //6
		    assertEquals(8, iDsoUninstallCount);
		    int  iDsoUninstallResCount = verifyDsoUninstallResourceFolderFiles(); //7
		    assertEquals(3, iDsoUninstallResCount);   	
	    }
	    	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }   
  
  //1. Verify that the 6 folders terracotta-2.1.0/dso
  public int verifyDSOFolderFiles() throws Exception {
     int iDsoFiles = 0;     
     String sDsoFolder = null;     
     System.out.println("************************************");
     sDsoFolder = System.getProperty("dso.dir");    
     System.out.println("files location="+sDsoFolder);
     File f = new File(sDsoFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
       iDsoFiles = fArray.length;
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
     return iDsoFiles;       
  } 
  //2. Verify that the 6 files exists in the terracotta-2.1.0/dso/bin folder (which-boot.jar is removed for judah release build 1.3652)
  public int verifyDsoBinFolderFiles() throws Exception {
		 int iDsoBinFiles = 0;		 
		 String sDsoBinFolder = null;		 	
		 System.out.println("************************************");
		 sDsoBinFolder = System.getProperty("bindir.value");		 
		 System.out.println("files location="+sDsoBinFolder);
		 File f = new File(sDsoBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDsoBinFiles = fArray.length;
	       for(int i=0; i<fArray.length;i++){
	         if(fArray[i].isDirectory() == true){
	           System.out.println("folder name="+fArray[i].getName());
	         }else {
	           System.out.println("file name="+fArray[i].getName());
	         }
	       }
	     }
		 System.out.println("************************************");
		 return iDsoBinFiles;		 	  
  }   
  //3. Verify that the 1 file exists under terracotta-2.1.0/dso/libexec 
  public int verifyDsoLibexecFolderFiles() throws Exception { 
		 int iDsoLibexecFolderFiles = 0;		 
		 String sDsoLibexecFolder = null;		 
		 System.out.println("************************************");
		 sDsoLibexecFolder = System.getProperty("libexecdir.value");
		 System.out.println("files location="+sDsoLibexecFolder);
		 File f = new File(sDsoLibexecFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDsoLibexecFolderFiles = fArray.length;
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
		return iDsoLibexecFolderFiles;
  }   
  //4. Verify that the 2 files exists under terracotta-2.1.0\dso\config-sample folder
  public int verifyDsoSconfigFolderFiles() throws Exception { 		 
		 int iDsoSconfigFolderFiles = 0;		 
		 String sDsoSconfigFolder = null;		 
		 System.out.println("************************************");
		 sDsoSconfigFolder = System.getProperty("configs.dir");
		 System.out.println("files location="+sDsoSconfigFolder);
		 File f = new File(sDsoSconfigFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDsoSconfigFolderFiles = fArray.length;
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
		return iDsoSconfigFolderFiles;
  }  
  //5. Verify that the 3 folders and 5 files exists under terracotta-2.1.0\dso\samples folder
  public int verifyDsoSamplesFolderFiles() throws Exception { 		 
		 int iDsoSamplesFolderFiles = 0;		 
		 String sDsoSamplesFolder = null;		 
		 System.out.println("************************************");
		 sDsoSamplesFolder = System.getProperty("demodir.value");
		 System.out.println("files location="+sDsoSamplesFolder);
		 File f = new File(sDsoSamplesFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDsoSamplesFolderFiles = fArray.length;
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
		return iDsoSamplesFolderFiles;
  }   
  //6. Verify that the 1 folders and 6 files exists under terracotta-2.1.0\dso\Uninstall_Terracotta for DSO folder
  public int verifyDsoUninstallFolderFiles() throws Exception { 		 
		 int iDsoUninstallFolderFiles = 0;		 
		 String sDsoUninstallFolder = null;		 
		 System.out.println("************************************");
		 sDsoUninstallFolder = System.getProperty("uninstall.dir");
		 System.out.println("files location="+sDsoUninstallFolder);
		 File f = new File(sDsoUninstallFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDsoUninstallFolderFiles = fArray.length;
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
		return iDsoUninstallFolderFiles;
  }
 //7. Verify that the 3 files exists under terracotta-2.1.0\dso\Uninstall_Terracotta for DSO\resource folder
  public int verifyDsoUninstallResourceFolderFiles() throws Exception { 		 
		 int iDsoUninstallResourceFolderFiles = 0;		 
		 String sDsoUninstallResourceFolder = null;		 
		 System.out.println("************************************");
		 sDsoUninstallResourceFolder = System.getProperty("uninstallreso.dir");
		 System.out.println("files location="+sDsoUninstallResourceFolder);
		 File f = new File(sDsoUninstallResourceFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDsoUninstallResourceFolderFiles = fArray.length;
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
		return iDsoUninstallResourceFolderFiles;
  }
}