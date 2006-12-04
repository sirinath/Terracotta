/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package teetest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DocsTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DocsTest.class);
  }

  public void testForVerification() throws Exception {	    
	    int  iCount = verifyDocsFolderFiles();
	    assertEquals(7, iCount);
	    int  iLibexecCount = verifyLibexecFolderFiles();
	    assertEquals(1, iLibexecCount);	
	    int  iSconfigCount = verifySconfigFolderFiles();
	    assertEquals(1, iSconfigCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  } 
  //Verify that the 7 files exists under /docs folder
  public int verifyDocsFolderFiles() throws Exception { 
	     int iDocsFolderFiles = 0;		 
		 String sDocsFolder = null;		 
		 System.out.println("************************************");
		 sDocsFolder = System.getProperty("docsdir.value");
		 System.out.println("files location="+sDocsFolder);
		 File f = new File(sDocsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDocsFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iDocsFolderFiles;	  
  }
  
  //Verify that the 1 file exists under tc2.0.x/libexec 
  public int verifyLibexecFolderFiles() throws Exception { 
		 int iLibexecFolderFiles = 0;		 
		 String sLibexecFolder = null;		 
		 System.out.println("************************************");
		 sLibexecFolder = System.getProperty("libexecdir.value");
		 System.out.println("files location="+sLibexecFolder);
		 File f = new File(sLibexecFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iLibexecFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iLibexecFolderFiles;
  }   
 //	Verify that the sample-config folder exists under tc2.0.x folder
  public int verifySconfigFolderFiles() throws Exception { 		 
		 int iSconfigFolderFiles = 0;		 
		 String sSconfigFolder = null;		 
		 System.out.println("************************************");
		 sSconfigFolder = System.getProperty("sconfigdir.value");
		 System.out.println("files location="+sSconfigFolder);
		 File f = new File(sSconfigFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSconfigFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iSconfigFolderFiles;
  }  
}