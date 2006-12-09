/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package teetest;

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
	    int  iCount = verifyWebFolderFiles();
	    //assertEquals(10, iCount); //irving release
	    assertEquals(8, iCount); //judah release
	    int iWebDemoCount = verifyWebDemoFolderFiles();
	    assertEquals(5, iWebDemoCount);
	    int iWebDemoImgCount = verifyWebDemoImgFolderFiles();
	    assertEquals(1, iWebDemoImgCount);	    
	    int ilibCount = verifyWebLibFolderFiles();
	    assertEquals(2, ilibCount); 	    
	    int iSrcCount = verifyWebSrcFolderFiles();
	    assertEquals(3, iSrcCount);
	    int iSrcExpCount = verifyWebSrcSubFolderFiles();
	    assertEquals(1, iSrcExpCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  } 
  
  //Verify that the 4 folders and 6 files files exists under demo/web folder (irving release)
  //Verify that the 4 folders and 4 files files exists under demo/web folder (judah release) //.classptah and .project files does not exist
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		return iWebDemoFolderFiles;	  
  }  
  //Verify that the 1 file exists under demo/web/demo/images folder
  public int verifyWebDemoImgFolderFiles() throws Exception { 
		 int iWebDemoImgFolderFiles = 0;		 
		 String sWebDemoImgFolder = null;		 
		 System.out.println("************************************");
		 sWebDemoImgFolder = System.getProperty("webdemoimgfile.value");
		 System.out.println("files location="+sWebDemoImgFolder);
		 File f = new File(sWebDemoImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 if(fArray.length != 0){
			 iWebDemoImgFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }		 
		 System.out.println("************************************");
		 return iWebDemoImgFolderFiles;	  
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
  //Verify that the 1 folder and 3 *.java files exists under demo/web/src/demo/sharedservlet folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }
			 }
		 }		 
		 System.out.println("************************************");
		return iJavaFileCount;  
  }  
  //Verify that the 1 java file exists under demo/web/src/demo/sharedservlet/exception folder
  public int verifyWebSrcSubFolderFiles() throws Exception { 
		 int iWebSrcSubFolderFiles = 0;		 
		 String sWebsrcexpFile = null;		 
		 System.out.println("************************************");
		 sWebsrcexpFile = System.getProperty("websrcexpfile.value");
		 System.out.println("files location="+sWebsrcexpFile);
		 File f = new File(sWebsrcexpFile);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebSrcSubFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iWebSrcSubFolderFiles;
  }   
}