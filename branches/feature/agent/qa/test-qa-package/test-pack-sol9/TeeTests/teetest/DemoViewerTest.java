/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package teetest;

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
	    boolean fileExists = verifyDemoLogInViewerFolderFiles();
	    if (fileExists == true){
	    	int  iCount = verifyViewerFolderFiles();
		    assertEquals(8, iCount);
	    }else {
	    	int  iCount = verifyViewerFolderFiles();
	    	assertEquals(7, iCount);
	    }
	    int ilibCount = verifyViewerLibFolderFiles();
	    assertEquals(4, ilibCount);
	    int iSrcCount = verifyViewerSrcFolderFiles();
	    assertEquals(4, iSrcCount);
	    int iSrcBeansCount = verifyViewerSrcBeansFolderFiles();
	    assertEquals(1, iSrcBeansCount);
	    int iSrcUiCount = verifyViewerSrcUiFolderFiles();
	    assertEquals(1, iSrcUiCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  } 
  
  //Verify that the 3 folder and 4 files exists under demo/viewer folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());					 
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }		 
		 System.out.println("************************************");
		return iViewerFolderFiles;	  
  }
  
  public boolean verifyDemoLogInViewerFolderFiles() throws Exception { 
	     boolean demologFileExists = false;
	  	 int iViewerFolderFiles = 0;
	  	 String folderName = null;
		 String sViewerFolder = null;		 
		 System.out.println("************************************");
		 sViewerFolder = System.getProperty("viewerdir.value");
		 System.out.println("files location="+sViewerFolder);
		 File f = new File(sViewerFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 //System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iViewerFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 //System.out.println("folder name="+fArray[i].getName());
					 folderName = fArray[i].getName();
					 if(folderName.indexOf("demo-logs")!= -1){
						 demologFileExists = true; 
					 }
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 //System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }		 
		 System.out.println("************************************");
		return demologFileExists;	  
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
  
 //Verify that the 2 folders and 4 *.java files exists under demo/viewer/src/demo/viewer folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }
			 }
		 }		 
		 System.out.println("************************************");
		return iJavaFileCount;  
  }
 //Verify that the 1 java file exists under demo/viewer/src/demo/viewer/beans folder
  public int verifyViewerSrcBeansFolderFiles() throws Exception {
	  	 int iViewerSrcBeansFolderFilesExists = 0;	  	 
	     String sViewersrcBeansFiles = null;		 
		 System.out.println("************************************");
		 sViewersrcBeansFiles = System.getProperty("viewersrcbeansfile.value");
		 System.out.println("files location="+sViewersrcBeansFiles);
		 File f = new File(sViewersrcBeansFiles);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iViewerSrcBeansFolderFilesExists = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }		 
		 System.out.println("************************************");
		return iViewerSrcBeansFolderFilesExists;  
  }
  //Verify that the 1 file exists under demo/viewer/src/demo/viewer/ui folder
  public int verifyViewerSrcUiFolderFiles() throws Exception {
	  	 int iViewerSrcUiFolderFilesExists = 0;	  	 
	     String sViewersrcUiFiles = null;		 
		 System.out.println("************************************");
		 sViewersrcUiFiles = System.getProperty("viewersrcuifile.value");
		 System.out.println("files location="+sViewersrcUiFiles);
		 File f = new File(sViewersrcUiFiles);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iViewerSrcUiFolderFilesExists = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }		 
		 System.out.println("************************************");
		return iViewerSrcUiFolderFilesExists;  
  }  
}