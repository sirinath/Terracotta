/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DemoSharedEditor2Test extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DemoSharedEditor2Test.class);
  }

  public void testForVerification() throws Exception {	    
	    int  iCount = verifySharedEditor2FolderFiles();	    
	    assertEquals(8, iCount);
	    int  iSetCount = verifySharedEditor2SetFolderFiles();	    
	    assertEquals(2, iSetCount); 
	    int iImgCount = verifySharedEditor2ImgFolderFiles();
	    assertEquals(9, iImgCount);	    
	    int  iLibCount = verifySharedEditor2LibFolderFiles();
	    assertEquals(1, iLibCount);	    
	    int  iSrcCount = verifySharedEditor2SrcFolderFiles();
	    assertEquals(1, iSrcCount);	    
	    int  iSrcComCount = verifySharedEditor2SrcComFolderFiles();
	    assertEquals(1, iSrcComCount);
	    int  iSrcComTerraCount = verifySharedEditor2SrcComTerraFolderFiles();
	    assertEquals(1, iSrcComTerraCount);
	    int  iSrcComTerraSampCount = verifySharedEditor2SrcComTerraSampFolderFiles();
	    assertEquals(1, iSrcComTerraSampCount);
	    int  iSrcComTerraSampSharedCount = verifySharedEditor2SrcComTerraSampSharedFolderFiles();
	    assertEquals(5, iSrcComTerraSampSharedCount);
	    int  iSrcComTerraSampSharedControlsCount = verifySharedEditor2SrcComTerraSampSharedControlsFolderFiles();
	    assertEquals(1, iSrcComTerraSampSharedControlsCount);	    	    
	    int  iSrcComTerraSampSharedEventsCount = verifySharedEditor2SrcComTerraSampSharedEventsFolderFiles();
	    assertEquals(1, iSrcComTerraSampSharedEventsCount);
	    int iSrcComTerraSampSharedModelCount = verifySharedEditor2SrcComTerraSampSharedModelFolderFiles();
	    assertEquals(6, iSrcComTerraSampSharedModelCount);
	    int iSrcComTerraSampSharedUiCount = verifySharedEditor2SrcComTerraSampSharedUiFolderFiles();
	    assertEquals(5, iSrcComTerraSampSharedUiCount);	    	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  	
  
  //Verify that the 5 folders and 3 files exists under terracotta-2.1.0\dso\samples\sharedEditor2 folder 
  public int verifySharedEditor2FolderFiles() throws Exception { 
	     int iSharedEditor2FolderFiles = 0;		 
		 String sSharedEditor2Folder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2Folder = System.getProperty("sharedEditor2.dir");
		 System.out.println("files location="+sSharedEditor2Folder);
		 File f = new File(sSharedEditor2Folder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2FolderFiles = fArray.length;
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
		 return iSharedEditor2FolderFiles;	  
  } 
  
  //Verify that the 2 files exists under terracotta-2.1.0\dso\samples\sharedEditor2\.settings folder 
  public int verifySharedEditor2SetFolderFiles() throws Exception { 
	     int iSharedEditor2SetFolderFiles = 0;		 
		 String sSharedEditor2SetFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2SetFolder = System.getProperty("sharedEditor2set.dir");
		 System.out.println("files location="+sSharedEditor2SetFolder);
		 File f = new File(sSharedEditor2SetFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2SetFolderFiles = fArray.length;
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
		 return iSharedEditor2SetFolderFiles;	  
  }
 //Verify that the 9 files exists under terracotta-2.1.0\dso\samples\sharedEditor2\images folder
  public int verifySharedEditor2ImgFolderFiles() throws Exception { 
	     int iSharedEditor2ImgFolderFiles = 0;		 
		 String sSharedEditor2ImgFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2ImgFolder = System.getProperty("sharedEditor2img.dir");
		 System.out.println("files location="+sSharedEditor2ImgFolder);
		 File f = new File(sSharedEditor2ImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2ImgFolderFiles = fArray.length;
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
		return iSharedEditor2ImgFolderFiles;	  
  }    
  //Verify that the custom-dso-boot.jar 1 file exists under terracotta-2.1.0\dso\samples\sharedEditor2\lib
  public int verifySharedEditor2LibFolderFiles() throws Exception {  
		 int iSharedEditor2LibFolderFiles = 0;		 
		 String sSharedEditor2LibFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2LibFolder = System.getProperty("sharedEditor2lib.dir");
		 System.out.println("files location="+sSharedEditor2LibFolder);
		 File f = new File(sSharedEditor2LibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2LibFolderFiles = fArray.length;
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
		 return iSharedEditor2LibFolderFiles;
  }  
  //Verify that the 1 folder exists under terracotta-2.1.0\dso\samples\sharedEditor2\src  
  public int verifySharedEditor2SrcFolderFiles() throws Exception {
		 int iSharedEditor2SrcFolderFiles = 0;		 
		 String sSharedEditor2SrcFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2SrcFolder = System.getProperty("sharedEditor2src.dir");
		 System.out.println("files location="+sSharedEditor2SrcFolder);
		 File f = new File(sSharedEditor2SrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcFolderFiles = fArray.length;
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
		 return iSharedEditor2SrcFolderFiles;
  }
  //Verify that the 1 folder exists under terracotta-2.1.0\dso\samples\sharedEditor2\src\com
  public int verifySharedEditor2SrcComFolderFiles() throws Exception {
		 int iSharedEditor2SrcComFolderFiles = 0;		 
		 String sSharedEditor2SrcComFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2SrcComFolder = System.getProperty("sharedEditor2srccom.dir");
		 System.out.println("files location="+sSharedEditor2SrcComFolder);
		 File f = new File(sSharedEditor2SrcComFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcComFolderFiles = fArray.length;
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
		 return iSharedEditor2SrcComFolderFiles;
  }  
  //Verify that the 1 folder exists under terracotta-2.1.0\dso\samples\sharedEditor2\src\com\terracotta
  public int verifySharedEditor2SrcComTerraFolderFiles() throws Exception {
		 int iSharedEditor2SrcComTerraFolderFiles = 0;		 
		 String sSharedEditor2SrcComTerraFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2SrcComTerraFolder = System.getProperty("sharedEditor2srccomterra.dir");
		 System.out.println("files location="+sSharedEditor2SrcComTerraFolder);
		 File f = new File(sSharedEditor2SrcComTerraFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcComTerraFolderFiles = fArray.length;
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
		 return iSharedEditor2SrcComTerraFolderFiles;
  }
  //Verify that the 1 folder exists under terracotta-2.1.0\dso\samples\sharedEditor2\src\com\terracotta\samples
  public int verifySharedEditor2SrcComTerraSampFolderFiles() throws Exception {
		 int iSharedEditor2SrcComTerraSampFolderFiles = 0;		 
		 String sSharedEditor2SrcComTerraSampFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2SrcComTerraSampFolder = System.getProperty("sharedEditor2srccomterrasamp.dir");
		 System.out.println("files location="+sSharedEditor2SrcComTerraSampFolder);
		 File f = new File(sSharedEditor2SrcComTerraSampFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcComTerraSampFolderFiles = fArray.length;
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
		 return iSharedEditor2SrcComTerraSampFolderFiles;
  }  
  //Verify that the 4 folders and 1 file exists under terracotta-2.1.0\dso\samples\sharedEditor2\src\com\terracotta\samples\sharededitor
  public int verifySharedEditor2SrcComTerraSampSharedFolderFiles() throws Exception {
		 int iSharedEditor2SrcComTerraSampSharedFolderFiles = 0;		 
		 String sSharedEditor2SrcComTerraSampSharedFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2SrcComTerraSampSharedFolder = System.getProperty("sharedEditor2srccomterrasampsharededitor.dir");
		 System.out.println("files location="+sSharedEditor2SrcComTerraSampSharedFolder);
		 File f = new File(sSharedEditor2SrcComTerraSampSharedFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcComTerraSampSharedFolderFiles = fArray.length;
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
		 return iSharedEditor2SrcComTerraSampSharedFolderFiles;
  }
  //Verify that the 1 file exists under terracotta-2.1.0\dso\samples\sharedEditor2\src\com\terracotta\samples\sharededitor\controls
  public int verifySharedEditor2SrcComTerraSampSharedControlsFolderFiles() throws Exception {
		 int iSharedEditor2SrcComTerraSampSharedControlsFolderFiles = 0;		 
		 String sSharedEditor2SrcComTerraSampSharedControlsFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditor2SrcComTerraSampSharedControlsFolder = System.getProperty("sharedEditor2srccomterrasampsharededitorcontrols.dir");
		 System.out.println("files location="+sSharedEditor2SrcComTerraSampSharedControlsFolder);
		 File f = new File(sSharedEditor2SrcComTerraSampSharedControlsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcComTerraSampSharedControlsFolderFiles = fArray.length;
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
		 return iSharedEditor2SrcComTerraSampSharedControlsFolderFiles;
  }  
 //Verify that the 1 file exists under terracotta-2.1.0\dso\samples\sharedEditor2\src\com\terracotta\samples\sharededitor\events folder
  public int verifySharedEditor2SrcComTerraSampSharedEventsFolderFiles() throws Exception { 
	     int iSharedEditor2SrcEventsFolderFiles = 0;		 
		 String sSharedEditorSrcEventsFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcEventsFolder = System.getProperty("sharedEditor2srccomterrasampsharededitorevents.dir");
		 System.out.println("files location="+sSharedEditorSrcEventsFolder);
		 File f = new File(sSharedEditorSrcEventsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);		 
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcEventsFolderFiles = fArray.length;
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
		return iSharedEditor2SrcEventsFolderFiles;	  
  }
  //Verify that the 6 files exists under terracotta-2.1.0\dso\samples\sharedEditor2\src\com\terracotta\samples\sharededitor\models folder
  public int verifySharedEditor2SrcComTerraSampSharedModelFolderFiles() throws Exception { 
	     int iSharedEditor2SrcModelFolderFiles = 0;		 
		 String sSharedEditorSrcModelFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcModelFolder = System.getProperty("sharedEditor2srccomterrasampsharededitormodels.dir");
		 System.out.println("files location="+sSharedEditorSrcModelFolder);
		 File f = new File(sSharedEditorSrcModelFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);		 
		 
		 if(fArray.length != 0){
			 iSharedEditor2SrcModelFolderFiles = fArray.length;
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
		return iSharedEditor2SrcModelFolderFiles;	  
  }
  /*Verify that the 5 files exists under terracotta-2.1.0/dso/samples/sharedEditor2/src/com/terracotta/samples/sharededitor/ui folder*/
  public int verifySharedEditor2SrcComTerraSampSharedUiFolderFiles() throws Exception { 
	     int iSharedEditorSrcUiFolderFiles = 0;		 
		 String sSharedEditorSrcUiFolder = null;		 
		 System.out.println("************************************");
		 sSharedEditorSrcUiFolder = System.getProperty("sharedEditor2srccomterrasampsharededitorui.dir");
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		return iSharedEditorSrcUiFolderFiles;	  
  }  
}