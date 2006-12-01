/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DemoWebTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DemoWebTest.class);
  }

  public void testForVerification() throws Exception {
	    boolean b = verifyWebFolder();
	    assertTrue("Demo Web folder Exists!", b);
	    int  iCount = verifyWebFolderFiles();
	    //assertEquals(10, iCount);
        assertEquals(8, iCount); //modified on 7/21/06 for judah release
	    int iWebDemoCount = verifyWebDemoFolderFiles();
	    assertEquals(5, iWebDemoCount);
	    boolean bWebDemoImg = verifyWebDemoImgFolderFiles();
	    assertTrue("Demo Web/Demo/Image folder files Exists!", bWebDemoImg);
	    int ilibCount = verifyWebLibFolderFiles();
	    assertEquals(2, ilibCount); 	    
	    int iSrcCount = verifyWebSrcFolderFiles();
	    assertEquals(3, iSrcCount);
	    boolean s = verifyWebSrcSubFolderFiles();
	    assertTrue("Demo Web src/demo/sharedservlet/exception folder files Exists!", s); 
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	Verify that the classes, lib, demo and src folder exists under demo/web folder
  public boolean verifyWebFolder() throws Exception { 
	     boolean bWebFolderExists = false;
		 String sWebFolder = null;
		 String sWebclassesFolder = null;
		 String sWebdemoFolder = null;
		 String sWeblibFolder = null;
		 String sWebsrcFolder = null;
		 
		 sWebFolder = System.getProperty("webdir.value");
		 sWebclassesFolder = System.getProperty("webclassesdir.value");
		 sWebdemoFolder = System.getProperty("webdemodir.value");		 
		 sWeblibFolder = System.getProperty("weblibdir.value");
		 sWebsrcFolder = System.getProperty("websrcdir.value");
		 
		 System.out.println("************************************");
		 if(sWebFolder != null) {		 
			 System.out.println("Web folder exists:" + sWebFolder);
			 bWebFolderExists = true;
		 }
		 if(sWebclassesFolder != null) {		 
			 System.out.println("Web classes folder exists:" + sWebclassesFolder);
			 bWebFolderExists = true;
		 }
		 if(sWebdemoFolder != null) {		 
			 System.out.println("Web demo folder exists:" + sWebdemoFolder);
			 bWebFolderExists = true;
		 }
		 if(sWeblibFolder != null) {		 
			 System.out.println("Web lib folder exists:" + sWeblibFolder);
			 bWebFolderExists = true;
		 }
		 if(sWebsrcFolder != null) {		 
			 System.out.println("Web src folder exists:" + sWebsrcFolder);
			 bWebFolderExists = true;
		 }
		 System.out.println("************************************");
		 return bWebFolderExists;	  
  } 
  
  //Verify that the 10 files exists under demo/web folder
  public int verifyWebFolderFiles() throws Exception { 
	     int iWebFolderFiles = 0;		 
		 String sWebFolder = null;		 
		 System.out.println("************************************");
		 sWebFolder = System.getProperty("webdir.value");
		 System.out.println("files location="+sWebFolder);
		 File f = new File(sWebFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iWebFolderFiles;	  
  }
  
  //Verify that the 1 folder and 4 files exists under demo/web/demo folder
  public int verifyWebDemoFolderFiles() throws Exception { 
	     int iWebDemoFolderFiles = 0;		 
		 String sWebDemoFolder = null;		 
		 System.out.println("************************************");
		 sWebDemoFolder = System.getProperty("webdemodir.value");
		 System.out.println("files location="+sWebDemoFolder);
		 File f = new File(sWebDemoFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebDemoFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iWebDemoFolderFiles;	  
  }  
  
  //Verify that the files exists under demo/web/demo/images folder
  public boolean verifyWebDemoImgFolderFiles() throws Exception { 
	     boolean bWebDemoImgFolderFilesExists = false;		 
		 String sWebDemoImgfile = null;		 	 
		 
		 sWebDemoImgfile = System.getProperty("webdemoimgfile.value");			 
		  
		 System.out.println("************************************");		 
		 if(sWebDemoImgfile != null) {		 
			 System.out.println("Web demo/images folder file:" + sWebDemoImgfile);
			 bWebDemoImgFolderFilesExists = true;
		 }		 
		 System.out.println("************************************");
		 return bWebDemoImgFolderFilesExists;	  
  }  
  
  //Verify that the 2 files (*.jar) exists under demo/web/lib folder
  public int verifyWebLibFolderFiles() throws Exception { 
	     int iWebLibFolderFiles = 0;		 
		 String sWebLibFolder = null;		 
		 System.out.println("************************************");
		 sWebLibFolder = System.getProperty("weblibdir.value");
		 System.out.println("files location="+sWebLibFolder);
		 File f = new File(sWebLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebLibFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iWebLibFolderFiles;	  
  }  
  
  //Verify that the *.java files exists under demo/web/src/demo/sharedservlet folder
  public int verifyWebSrcFolderFiles() throws Exception {	  	 
	  	 int iJavaFileCount = 0;
	     String sWebsrcfile = null;		 
		 System.out.println("************************************");
		 sWebsrcfile = System.getProperty("websrcfile.value");
		 System.out.println("files location="+sWebsrcfile);
		 File f = new File(sWebsrcfile);
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
  
  //Verify that the *.java files exists under demo/web/src/demo/sharedservlet/exception folder
  public boolean verifyWebSrcSubFolderFiles() throws Exception { 
	     boolean bWebSrcSubFolderFilesExists = false;		 
		 String sWebsrcexpfile = null;				 
		 
		 sWebsrcexpfile = System.getProperty("websrcexpfile.value");		 		 
		  
		 System.out.println("************************************");		 
		 if(sWebsrcexpfile != null) {		 
			 System.out.println("Web src/demo/sharedservlet/exception folder file:" + sWebsrcexpfile);
			 bWebSrcSubFolderFilesExists = true;
		 }		 
		 System.out.println("************************************");
		 return bWebSrcSubFolderFilesExists;	  
  }   
}