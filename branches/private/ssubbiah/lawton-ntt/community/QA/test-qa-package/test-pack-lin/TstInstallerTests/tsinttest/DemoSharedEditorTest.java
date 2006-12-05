/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DemoSharedEditorTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DemoSharedEditorTest.class);
  }

  public void testForVerification() throws Exception {
	    boolean b = verifySharedEditorFolder();
	    assertTrue("Demo Shared Editor folder Exists!", b);
	    int  iCount = verifySharedEditorFolderFiles();
	    //assertEquals(10, iCount);
	    assertEquals(8, iCount); //modified on 7/21/06 for judah release
	    boolean l = verifySharedEditorLibFolderFiles();
	    assertTrue("Demo Shared Editor folder Exists!", b);
	    boolean s = verifySharedEditorSrcFolderFiles();	   
	    assertTrue("Demo Shared Editor src folder files Exists!", s);
	    int iSrcModelCount = verifySharedEditorSrcModelFolderFiles();
	    assertEquals(10, iSrcModelCount);
	    int iSrcImgCount = verifySharedEditorSrcImgFolderFiles();
	    assertEquals(5, iSrcImgCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	Verify that the classes, lib and src folder exists under demo/sharedEditor folder
  public boolean verifySharedEditorFolder() throws Exception { 
	     boolean bSharedEditorFolderExists = false;
		 String sSharedEditorFolder = null;
		 String sSharedEditorclassesFolder = null;
		 String sSharedEditorlibFolder = null;
		 String sSharedEditorsrcFolder = null;
		 
		 sSharedEditorFolder = System.getProperty("sharedEditordir.value");
		 sSharedEditorclassesFolder = System.getProperty("sharedEditorclassesdir.value");
		 sSharedEditorlibFolder = System.getProperty("sharedEditorlibdir.value");
		 sSharedEditorsrcFolder = System.getProperty("sharedEditorsrcdir.value");
		 
		 System.out.println("************************************");
		 if(sSharedEditorFolder != null) {		 
			 System.out.println("Shared Editor folder exists:" + sSharedEditorFolder);
			 bSharedEditorFolderExists = true;
		 }
		 if(sSharedEditorclassesFolder != null) {		 
			 System.out.println("Shared Editor classes folder exists:" + sSharedEditorclassesFolder);
			 bSharedEditorFolderExists = true;
		 }
		 if(sSharedEditorlibFolder != null) {		 
			 System.out.println("Shared Editor lib folder exists:" + sSharedEditorlibFolder);
			 bSharedEditorFolderExists = true;
		 }
		 if(sSharedEditorsrcFolder != null) {		 
			 System.out.println("Shared Editor src folder exists:" + sSharedEditorsrcFolder);
			 bSharedEditorFolderExists = true;
		 }
		 System.out.println("************************************");
		 return bSharedEditorFolderExists;	  
  } 
  
  //Verify that the 8 files exists under demo/sharedEditor folder
  public int verifySharedEditorFolderFiles() throws Exception { 
	     int iSharedEditorFolderFiles = 0;		 
		 String sSharedEditorFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorFolder = System.getProperty("sharedEditordir.value");
		 System.out.println("files location="+sSharedEditorFolder);
		 File f = new File(sSharedEditorFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditorFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iSharedEditorFolderFiles;	  
  } 
  
  //Verify that the forms_rt.jar files exists under demo/sharedEditor/lib
  public boolean verifySharedEditorLibFolderFiles() throws Exception { 
	     boolean bSharedEditorLibFolderFilesExists = false;
		 String sSharedEditorLibfile = null;		 	 
		 
		 sSharedEditorLibfile = System.getProperty("sharedEditorlibfile.value");		 		 		 
		  
		 System.out.println("************************************");
		 if(sSharedEditorLibfile != null) {		 
			 System.out.println("Shared Editor lib folder file1:" + sSharedEditorLibfile);
			 bSharedEditorLibFolderFilesExists = true;
		 }		 
		 System.out.println("************************************");
		 return bSharedEditorLibFolderFilesExists;	  
  }  
  
 //Verify that the *.java files exists under demo/sharedEditor/src/demo/sharedEditor folder
  public boolean verifySharedEditorSrcFolderFiles() throws Exception { 
	     boolean bSharedEditorSrcFolderFilesExists = false;
		 String sSharedEditorsrcfile = null;
		 String sSharedEditorsrcbeansfile = null;
		 String sSharedEditorsrcuifile = null;
		 
		 
		 sSharedEditorsrcfile = System.getProperty("sharedEditorsrcfile.value");
		 sSharedEditorsrcbeansfile = System.getProperty("sharedEditorsrcbeansfile.value");
		 sSharedEditorsrcuifile = System.getProperty("sharedEditorsrcuifile.value");
		 
		  
		 System.out.println("************************************");
		 if(sSharedEditorsrcfile != null) {		 
			 System.out.println("SharedEditor src folder file:" + sSharedEditorsrcfile);
			 bSharedEditorSrcFolderFilesExists = true;
		 }
		 if(sSharedEditorsrcbeansfile != null) {		 
			 System.out.println("SharedEditor src/beans folder file:" + sSharedEditorsrcbeansfile);
			 bSharedEditorSrcFolderFilesExists = true;
		 }
		 if(sSharedEditorsrcuifile != null) {		 
			 System.out.println("SharedEditor src/ui folder file:" + sSharedEditorsrcuifile);
			 bSharedEditorSrcFolderFilesExists = true;
		 }
		 System.out.println("************************************");
		 return bSharedEditorSrcFolderFilesExists;	  
  } 
  
  //Verify that the 10 files exists under demo/sharedEditor/src/demo/sharedEditor/model folder
  public int verifySharedEditorSrcModelFolderFiles() throws Exception { 
	     int iSharedEditorSrcModelFolderFiles = 0;		 
		 String sSharedEditorSrcModelFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcModelFolder = System.getProperty("sharedEditorsrcmodelfile.value");
		 System.out.println("files location="+sSharedEditorSrcModelFolder);
		 File f = new File(sSharedEditorSrcModelFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditorSrcModelFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iSharedEditorSrcModelFolderFiles;	  
  }
  
 //Verify that the 5 files exists under demo/sharedEditor/src/img folder
  public int verifySharedEditorSrcImgFolderFiles() throws Exception { 
	     int iSharedEditorSrcImgFolderFiles = 0;		 
		 String sSharedEditorSrcImgFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcImgFolder = System.getProperty("sharedEditorsrcimgfile.value");
		 System.out.println("files location="+sSharedEditorSrcImgFolder);
		 File f = new File(sSharedEditorSrcImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditorSrcImgFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iSharedEditorSrcImgFolderFiles;	  
  }  
  
}