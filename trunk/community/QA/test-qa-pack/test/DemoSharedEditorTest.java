/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

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
	    int  iCount = verifySharedEditorFolderFiles();	    
	    assertEquals(8, iCount); 
	    int  iLibCount = verifySharedEditorLibFolderFiles();
	    assertEquals(1, iLibCount);
	    int  iSrcCount = verifySharedEditorSrcFolderFiles();
	    assertEquals(2, iSrcCount);
	    int  iSrcSubCount = verifySharedEditorSrcSubFolderFiles();
	    assertEquals(4, iSrcSubCount);
	    int  iSrcBeansCount = verifySharedEditorSrcBeansFolderFiles();
	    assertEquals(1, iSrcBeansCount);
	    int iSrcModelCount = verifySharedEditorSrcModelFolderFiles();
	    assertEquals(10, iSrcModelCount);
	    int iSrcUiCount = verifySharedEditorSrcUiFolderFiles();
	    assertEquals(1, iSrcUiCount);
	    int iSrcImgCount = verifySharedEditorSrcImgFolderFiles();
	    assertEquals(5, iSrcImgCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  	
  
  //Verify that the 3 folders and 5 files exists under terracotta-2.1.0\dso\samples\sharedEditor folder 
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }		 
		 System.out.println("************************************");
		 return iSharedEditorFolderFiles;	  
  }       
  //Verify that the forms_rt.jar 1 file exists under terracotta-2.1.0\dso\samples\sharedEditor\lib
  public int verifySharedEditorLibFolderFiles() throws Exception {  
		 int iSharedEditorLibFolderFiles = 0;		 
		 String sSharedEditorLibFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorLibFolder = System.getProperty("sharedEditorlibdir.value");
		 System.out.println("files location="+sSharedEditorLibFolder);
		 File f = new File(sSharedEditorLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditorLibFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");
		 return iSharedEditorLibFolderFiles;
  }  
  //Verify that the 2 folders exists under terracotta-2.1.0\dso\samples\sharedEditor\src  
  public int verifySharedEditorSrcFolderFiles() throws Exception {
		 int iSharedEditorSrcFolderFiles = 0;		 
		 String sSharedEditorSrcFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcFolder = System.getProperty("sharedEditorsrcdir.value");
		 System.out.println("files location="+sSharedEditorSrcFolder);
		 File f = new File(sSharedEditorSrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditorSrcFolderFiles = fArray.length;
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
		 return iSharedEditorSrcFolderFiles;
  }
  //Verify that the 3 folders and 1 file exists under terracotta-2.1.0\dso\samples\sharedEditor\src\demo\sharedEditor
  public int verifySharedEditorSrcSubFolderFiles() throws Exception {
		 int iSharedEditorSrcSubFolderFiles = 0;		 
		 String sSharedEditorSrcSubFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcSubFolder = System.getProperty("sharedEditorsrcsubdir.value");
		 System.out.println("files location="+sSharedEditorSrcSubFolder);
		 File f = new File(sSharedEditorSrcSubFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditorSrcSubFolderFiles = fArray.length;
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
		 return iSharedEditorSrcSubFolderFiles;
  }
  //Verify that the 1 file exists under terracotta-2.1.0\dso\samples\sharedEditor\src\demo\sharedEditor\beans folder
  public int verifySharedEditorSrcBeansFolderFiles() throws Exception { 
	     int iSharedEditorSrcBeansFolderFiles = 0;		 
		 String sSharedEditorSrcBeansFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcBeansFolder = System.getProperty("sharedEditorsrcbeansfile.value");
		 System.out.println("files location="+sSharedEditorSrcBeansFolder);
		 File f = new File(sSharedEditorSrcBeansFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 
		 if(fArray.length != 0){
			 iSharedEditorSrcBeansFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iSharedEditorSrcBeansFolderFiles;	  
  }
  //Verify that the 10 files exists under terracotta-2.1.0\dso\samples\sharedEditor\src\demo\sharedEditor\model folder
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
  //Verify that the 1 file exists under terracotta-2.1.0/dso/samples/sharedEditor/src/demo/sharedEditor/ui folder
  public int verifySharedEditorSrcUiFolderFiles() throws Exception { 
	     int iSharedEditorSrcUiFolderFiles = 0;		 
		 String sSharedEditorSrcUiFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcUiFolder = System.getProperty("sharedEditorsrcuifile.value");
		 System.out.println("files location="+sSharedEditorSrcUiFolder);
		 File f = new File(sSharedEditorSrcUiFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 
		 if(fArray.length != 0){
			 iSharedEditorSrcUiFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iSharedEditorSrcUiFolderFiles;	  
  }  
  //Verify that the 5 files exists under terracotta-2.1.0\dso\samples\sharedEditor\src\img folder
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