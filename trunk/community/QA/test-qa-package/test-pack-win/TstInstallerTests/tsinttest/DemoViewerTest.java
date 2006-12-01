/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DemoViewerTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DemoViewerTest.class);
  }

  public void testForVerification() throws Exception {
	    boolean b = verifyViewerFolder();
	    assertTrue("Demo Viewer folder Exists!", b);
	    int  iCount = verifyViewerFolderFiles();
	    assertEquals(7, iCount);
	    int ilibCount = verifyViewerLibFolderFiles();
	    assertEquals(4, ilibCount);
	    int iSrcCount = verifyViewerSrcFolderFiles();
	    assertEquals(4, iSrcCount);
	    boolean s = verifySharedEditorSrcSubFolderFiles();
	    assertTrue("Demo Viewer src folder files Exists!", s); 
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	Verify that the classes, lib and src folder exists under demo/viewer folder
  public boolean verifyViewerFolder() throws Exception { 
	     boolean bViewerFolderExists = false;
		 String sViewerFolder = null;
		 String sViewerclassesFolder = null;
		 String sViewerlibFolder = null;
		 String sViewersrcFolder = null;
		 
		 sViewerFolder = System.getProperty("viewerdir.value");
		 sViewerclassesFolder = System.getProperty("viewerclassesdir.value");
		 sViewerlibFolder = System.getProperty("viewerlibdir.value");
		 sViewersrcFolder = System.getProperty("viewersrcdir.value");
		 
		 System.out.println("************************************");
		 if(sViewerFolder != null) {		 
			 System.out.println("Viewer folder exists:" + sViewerFolder);
			 bViewerFolderExists = true;
		 }
		 if(sViewerclassesFolder != null) {		 
			 System.out.println("Viewer classes folder exists:" + sViewerclassesFolder);
			 bViewerFolderExists = true;
		 }
		 if(sViewerlibFolder != null) {		 
			 System.out.println("Viewer lib folder exists:" + sViewerlibFolder);
			 bViewerFolderExists = true;
		 }
		 if(sViewersrcFolder != null) {		 
			 System.out.println("Viewer src folder exists:" + sViewersrcFolder);
			 bViewerFolderExists = true;
		 }
		 System.out.println("************************************");
		 return bViewerFolderExists;	  
  } 
  
  //Verify that the 7 files exists under demo/viewer folder
  public int verifyViewerFolderFiles() throws Exception { 
	     int iViewerFolderFiles = 0;		 
		 String sViewerFolder = null;		 
		 System.out.println("************************************");
		 sViewerFolder = System.getProperty("viewerdir.value");
		 System.out.println("files location="+sViewerFolder);
		 File f = new File(sViewerFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iViewerFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iViewerFolderFiles;	  
  }  
  
  //Verify that the 4 files (*.jar) exists under demo/viewer/lib folder
  public int verifyViewerLibFolderFiles() throws Exception { 
	     int iViewerLibFolderFiles = 0;		 
		 String sViewerLibFolder = null;		 
		 System.out.println("************************************");
		 sViewerLibFolder = System.getProperty("viewerlibdir.value");
		 System.out.println("files location="+sViewerLibFolder);
		 File f = new File(sViewerLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iViewerLibFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iViewerLibFolderFiles;	  
  }  
  
 //Verify that the *.java files exists under demo/viewer/src/demo/viewer folder
  public int verifyViewerSrcFolderFiles() throws Exception {
	  	 int iViewerSrcFolderFilesExists = 0;
	  	 int iJavaFileCount = 0;
	     String sViewersrcfile = null;		 
		 System.out.println("************************************");
		 sViewersrcfile = System.getProperty("viewersrcfile.value");
		 System.out.println("files location="+sViewersrcfile);
		 File f = new File(sViewersrcfile);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 for(int i=0; i<fArray.length;i++){				 
				 //System.out.println("file name="+fArray[i].getName());
				 String sFile = fArray[i].getName();
				 if (sFile.indexOf(".java") != -1){
					 System.out.println("Java file name="+sFile);
					 iJavaFileCount = iJavaFileCount+1;
					 System.out.println("java file count="+iJavaFileCount);
				 }
			 }
		 }
		 System.out.println("************************************");
		return iJavaFileCount;  
  }
  
//Verify that the *.java files exists under demo/viewer/src/demo/viewer folder
  public boolean verifySharedEditorSrcSubFolderFiles() throws Exception { 
	     boolean bViewerSrcFolderFilesExists = false;		 
		 String sViewersrcbeansfile = null;
		 String sViewersrcuifile = null;		 
		 
		 sViewersrcbeansfile = System.getProperty("viewersrcbeansfile.value");
		 sViewersrcuifile = System.getProperty("viewersrcuifile.value");		 
		  
		 System.out.println("************************************");		 
		 if(sViewersrcbeansfile != null) {		 
			 System.out.println("Viewer src/beans folder file:" + sViewersrcbeansfile);
			 bViewerSrcFolderFilesExists = true;
		 }
		 if(sViewersrcuifile != null) {		 
			 System.out.println("Viewer src/ui folder file:" + sViewersrcuifile);
			 bViewerSrcFolderFilesExists = true;
		 }
		 System.out.println("************************************");
		 return bViewerSrcFolderFilesExists;	  
  }   
}