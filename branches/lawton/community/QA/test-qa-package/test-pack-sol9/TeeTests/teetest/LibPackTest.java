/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package teetest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class LibPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(LibPackTest.class);
  }

  public void testForVerification() throws Exception {
	    // under tc2.0.x\lib
	    int  iLibCount = verifyLibFolder(System.getProperty("libdir.value")); //20
	    assertEquals(37, iLibCount); 
	    int  iLibDsobootCount = verifyLibDsobootFolderFiles(System.getProperty("libdsodir.value")); //21
	    assertEquals(2, iLibDsobootCount);
	    int  iLibTomcatCount = verifyLibTomcatFolderFiles(System.getProperty("libtomcatdir.value")); //22
	    assertEquals(1, iLibTomcatCount);
	    int  iLibWeblogicCount = verifyLibWeblogicFolderFiles(System.getProperty("libweblogicdir.value")); //23
	    assertEquals(1, iLibWeblogicCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  

  //Verify that the 3 folders and 38 files exists under /com/lib (irving release)
  //Verify that the 3 folders and 34 files exists under /com/lib (judah release)
  //removed files are asm-2.2.2.jar, asm-commons-2.2.2.jar, aspectwerkz-1.0.RC3.jar, jrexx-1.1.1.jar 
  public int verifyLibFolder(String s) throws Exception {
		 int iLibFiles = 0;		 
		 String sLibFolder = s;		 	
		 System.out.println("************************************");		 		 
		 System.out.println("files location="+sLibFolder);
		 File f = new File(sLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iLibFiles = fArray.length;
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
		 return iLibFiles;		 	  
  }
  //Verify that the 2 files exists under /com/lib/dso-boot
  public int verifyLibDsobootFolderFiles(String s) throws Exception {
		 int iLibDsoBootFolderFiles = 0;		 
		 String sLibDsoBootFolder = s;		 	
		 System.out.println("************************************");			 
		 System.out.println("files location="+sLibDsoBootFolder);
		 File f = new File(sLibDsoBootFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iLibDsoBootFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iLibDsoBootFolderFiles;		 	  
  }
  //Verify that the 1 file exists under /com/lib/tomcat
  public int verifyLibTomcatFolderFiles(String s) throws Exception {
		 int iLibTomcatFolderFiles = 0;		 
		 String sLibTomcatFolder = s;		 	
		 System.out.println("************************************");			 
		 System.out.println("files location="+sLibTomcatFolder);
		 File f = new File(sLibTomcatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iLibTomcatFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iLibTomcatFolderFiles;		 	  
  }
  //Verify that the 1 file exists under /com/lib/weblogic
  public int verifyLibWeblogicFolderFiles(String s) throws Exception {
		 int iLibWeblogicFolderFiles = 0;		 
		 String sLibWeblogicFolder = s;		 	
		 System.out.println("************************************");			 
		 System.out.println("files location="+sLibWeblogicFolder);
		 File f = new File(sLibWeblogicFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iLibWeblogicFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iLibWeblogicFolderFiles;		 	  
  }  
  //Verify that the 1 file exists in the /Meta-INF folder
  public int verifyEclipseComMetaFolderFiles() throws Exception { 
		 int iEclipseComMetaFolderFiles = 0;		 
		 String sEclipseComMetaFolder = null;
		 sEclipseComMetaFolder = System.getProperty("eclipsecommetadir.value");;		 	
		 System.out.println("************************************");			 
		 System.out.println("files location="+sEclipseComMetaFolder);
		 File f = new File(sEclipseComMetaFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComMetaFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComMetaFolderFiles;
  }
  
}
