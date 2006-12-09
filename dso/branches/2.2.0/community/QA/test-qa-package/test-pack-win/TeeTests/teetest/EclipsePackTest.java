/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package teetest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class EclipsePackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(EclipsePackTest.class);
  }

  public void testForVerification() throws Exception {
	    int iCount = verifyEclipseFolderFiles(); //1
	    assertEquals(2, iCount);	    	    
	    int  iComCount = verifyEclipseComFolderFiles(); //2
	    assertEquals(10, iComCount);	    
	    int  iComHtmlCount = verifyEclipseComHtmlFolder(); //3
	    assertEquals(6, iComHtmlCount);
	    int  iComHtmlConceptsCount = verifyEclipseComHtmlConceptsFolder(); //4
	    assertEquals(8, iComHtmlConceptsCount);
	    int  iComHtmlRefCount = verifyEclipseComHtmlRefFolder(); //5
	    assertEquals(9, iComHtmlRefCount);
	    //assertEquals(10, iComHtmlRefCount); //modified on 6/30/06 for judah version 2.0.03 (build 3607)
	    int  iComHtmlRefDemoCount = verifyEclipseComHtmlRefDemoFolder();//6
	    assertEquals(4, iComHtmlRefDemoCount);
	    int  iComHtmlTaskCount = verifyEclipseComHtmlTaskFolder(); //7
	    //assertEquals(13, iComHtmlTaskCount);
        assertEquals(15, iComHtmlTaskCount); //modified on 6/30/06 for judah version 2.0.03 (build 3607)
 	    int  iComHtmlTaskConfigCount = verifyEclipseComHtmlTaskConfigFolder(); //8
	    assertEquals(12, iComHtmlTaskConfigCount);
	    int  iComHtmlTaskConfigAppCount = verifyEclipseComHtmlTaskConfigAppFolder(); //9
	    //assertEquals(8, iComHtmlTaskConfigAppCount); 
  	    assertEquals(15, iComHtmlTaskConfigAppCount); //modified on 6/30/06 for judah version 2.0.03 (build 3607)
	    int  iComHtmlTaskConfigClientCount = verifyEclipseComHtmlTaskConfigClientFolder(); //10
	    //assertEquals(4, iComHtmlTaskConfigClientCount);
	    assertEquals(5, iComHtmlTaskConfigClientCount); ////modified on 6/30/06 for judah version 2.0.03 (build 3607)
	    int  iComHtmlTaskConfigServersCount = verifyEclipseComHtmlTaskConfigServersFolder(); //11
	    assertEquals(7, iComHtmlTaskConfigServersCount);
	    int  iComHtmlTutorialCount = verifyEclipseComHtmlTutorialFolder(); //12
	    assertEquals(23, iComHtmlTutorialCount);
	    int  iComImgCount = verifyEclipseComImgFolder(); //13
	    //assertEquals(1, iComImgCount); //irving release
	    assertEquals(2, iComImgCount); //judah release
	    int  iComImgEclipseCount = verifyEclipseComImgEclipseFolder(); //14
	    assertEquals(17, iComImgEclipseCount);	    
	    int  ieclipsecomLibCount = verifyLibFolder(System.getProperty("eclipsecomlibdir.value")); //15
	    //assertEquals(41, ieclipsecomLibCount); //irving release
	    assertEquals(37, ieclipsecomLibCount); //judah release
	    int  ieclipsecomLibDsobootCount = verifyLibDsobootFolderFiles(System.getProperty("eclipsecomlibdsodir.value")); //16
	    assertEquals(2, ieclipsecomLibDsobootCount);
	    int  ieclipsecomLibTomcatCount = verifyLibTomcatFolderFiles(System.getProperty("eclipsecomlibtomcatdir.value")); //17
	    assertEquals(1, ieclipsecomLibTomcatCount);
	    int  ieclipsecomLibWeblogicCount = verifyLibWeblogicFolderFiles(System.getProperty("eclipsecomlibweblogicdir.value")); //18
	    assertEquals(1, ieclipsecomLibWeblogicCount);
	    int  ieclipsecomMetaCount = verifyEclipseComMetaFolderFiles(); //19
	    assertEquals(1, ieclipsecomMetaCount);	 
	    // under tc2.0.1\lib
	    int  iLibCount = verifyLibFolder(System.getProperty("libdir.value")); //20
	    //assertEquals(41, iLibCount); //irving release
	    assertEquals(37, iLibCount); //judah release
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
	
 //	Verify that the 1 folder and 1 file under tc2.0.x/eclipse folder
  public int verifyEclipseFolderFiles() throws Exception { 
	  int iEclipseFolderFiles = 0;		 
		 String sEclipseFolder = null;		 
		 System.out.println("************************************");
		 sEclipseFolder = System.getProperty("eclipsedir.value");
		 System.out.println("files location="+sEclipseFolder);
		 File f = new File(sEclipseFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseFolderFiles = fArray.length;
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
		return iEclipseFolderFiles; 
  }  
  //Verify that the 6 files and 4 folder exists under /com folder
  public int verifyEclipseComFolderFiles() throws Exception { 
	     int iEclipseComFolderFiles = 0;		 
		 String sEclipseComFolder = null;		 
		 System.out.println("************************************");
		 sEclipseComFolder = System.getProperty("eclipsecomdir.value");
		 System.out.println("files location="+sEclipseComFolder);
		 File f = new File(sEclipseComFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);		 
		 
		 if(fArray.length != 0){
			 iEclipseComFolderFiles = fArray.length;
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
		return iEclipseComFolderFiles;	  
  }  
  //Verify that the 4 folders and 2 files exists under /com/html folder (Overview.html is removed, build: 1.3652 for judah release)
  public int verifyEclipseComHtmlFolder() throws Exception {
		 int iEclipseComHtmlFiles = 0;		 
		 String sEclipseComHtmlFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlFolder = System.getProperty("eclipsecomhtmldir.value");		 
		 System.out.println("files location="+sEclipseComHtmlFolder);
		 File f = new File(sEclipseComHtmlFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlFiles = fArray.length;
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
		 return iEclipseComHtmlFiles;		 	  
  }  
  //Verify that the 8 files exists under /com/html/concepts folder
  public int verifyEclipseComHtmlConceptsFolder() throws Exception {
		 int iEclipseComHtmlConceptsFiles = 0;		 
		 String sEclipseComHtmlConceptsFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlConceptsFolder = System.getProperty("eclipsecomhtmlconceptsdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlConceptsFolder);
		 File f = new File(sEclipseComHtmlConceptsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlConceptsFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				 
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComHtmlConceptsFiles;		 	  
  }  
  //Verify that the 1 folder and 8 files exists under /com/html/ref folder
  //modified on 6/30/06
  //Verify that the 1 folder and 8 files exists under /com/html/ref folder // Overview.html file is added(for judah release version 2.0.3 build 3607)
  //Tools.html is removed, build: 1.3652 (judah release)
  public int verifyEclipseComHtmlRefFolder() throws Exception {
		 int iEclipseComHtmlRefFiles = 0;		 
		 String sEclipseComHtmlRefFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlRefFolder = System.getProperty("eclipsecomhtmlrefdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlRefFolder);
		 File f = new File(sEclipseComHtmlRefFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlRefFiles = fArray.length;
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
		 return iEclipseComHtmlRefFiles;		 	  
  }
  //Verify that the 4 files exists under /com/html/ref/demo folder
  public int verifyEclipseComHtmlRefDemoFolder() throws Exception {
		 int iEclipseComHtmlRefDemoFiles = 0;		 
		 String sclipseComHtmlRefDemoFolder = null;		 	
		 System.out.println("************************************");
		 sclipseComHtmlRefDemoFolder = System.getProperty("eclipsecomhtmlrefdemodir.value");		 
		 System.out.println("files location="+sclipseComHtmlRefDemoFolder);
		 File f = new File(sclipseComHtmlRefDemoFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlRefDemoFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				 
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComHtmlRefDemoFiles;		 	  
  }
  //Verify that the 1 folder and 12 files exists under /com/html/tasks folder
  //modified on 6/30/06
  //Verify that the 1 folder and 14 files exists under /com/html/tasks folder //Overview.html, project_setup.jpg file are added(for judah release version 2.0.3 build 3607)
  public int verifyEclipseComHtmlTaskFolder() throws Exception {
		 int iEclipseComHtmlTaskFiles = 0;		 
		 String sEclipseComHtmlTaskFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlTaskFolder = System.getProperty("eclipsecomhtmltasksdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlTaskFolder);
		 File f = new File(sEclipseComHtmlTaskFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlTaskFiles = fArray.length;
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
		 return iEclipseComHtmlTaskFiles;		 	  
  }
  //Verify that the 3 folder and 9 files exists under /com/html/tasks/config folder
  public int verifyEclipseComHtmlTaskConfigFolder() throws Exception {
		 int iEclipseComHtmlTaskConfigFiles = 0;		 
		 String sEclipseComHtmlTaskConfigFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlTaskConfigFolder = System.getProperty("eclipsecomhtmltasksconfigdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlTaskConfigFolder);
		 File f = new File(sEclipseComHtmlTaskConfigFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlTaskConfigFiles = fArray.length;
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
		 return iEclipseComHtmlTaskConfigFiles;		 	  
  }
  //Verify that the 8 files exists under /com/html/tasks/config/app folder
  //modified on 6/30/06
  //Verify that the 15 files exists under /com/html/tasks/config/app folder //for judah release version 2.0.3 build 3607)
  //chooser_root.jpg, enter_root.jpg, instrumented_classes.jpg, root_adornment.jpg, root_src_adornment.jpg, roots.jpg, transient_fields.jpg are newly added
  public int verifyEclipseComHtmlTaskConfigAppFolder() throws Exception {
		 int iEclipseComHtmlTaskConfigAppFiles = 0;		 
		 String sEclipseComHtmlTaskConfigAppFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlTaskConfigAppFolder = System.getProperty("eclipsecomhtmltasksconfigappdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlTaskConfigAppFolder);
		 File f = new File(sEclipseComHtmlTaskConfigAppFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlTaskConfigAppFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComHtmlTaskConfigAppFiles;		 	  
  }
  //Verify that the 4 files exists under /com/html/tasks/config/clients folder
  //modified on 6/30/06
  //Verify that the 5 files exists under /com/html/tasks/config/clients folder //for judah release version 2.0.3 build 3607)
  //clients.jpg is added
  public int verifyEclipseComHtmlTaskConfigClientFolder() throws Exception {
		 int iEclipseComHtmlTaskConfigClientFiles = 0;		 
		 String sEclipseComHtmlTaskConfigClientFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlTaskConfigClientFolder = System.getProperty("eclipsecomhtmltasksconfigclientsdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlTaskConfigClientFolder);
		 File f = new File(sEclipseComHtmlTaskConfigClientFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlTaskConfigClientFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComHtmlTaskConfigClientFiles;		 	  
  } 
  //Verify that the 7 files exists under /com/html/tasks/config/servers folder
  public int verifyEclipseComHtmlTaskConfigServersFolder() throws Exception {
		 int iEclipseComHtmlTaskConfigServersFiles = 0;		 
		 String sEclipseComHtmlTaskConfigServersFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlTaskConfigServersFolder = System.getProperty("eclipsecomhtmltasksconfigserversdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlTaskConfigServersFolder);
		 File f = new File(sEclipseComHtmlTaskConfigServersFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlTaskConfigServersFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComHtmlTaskConfigServersFiles;		 	  
  }
  //Verify that the 23 files exists under /com/html/tutorial folder
  public int verifyEclipseComHtmlTutorialFolder() throws Exception {
		 int iEclipseComHtmlTutorialFiles = 0;		 
		 String sEclipseComHtmlTutorialFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComHtmlTutorialFolder = System.getProperty("eclipsecomhtmltutorialdir.value");		 
		 System.out.println("files location="+sEclipseComHtmlTutorialFolder);
		 File f = new File(sEclipseComHtmlTutorialFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComHtmlTutorialFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComHtmlTutorialFiles;		 	  
  }
  //Verify that the 1 folder exists under /com/images (irving release)
  //Verify that the 1 folder and 1 file exists under /com/images (judah release) //terr_logo.gif file is added
  public int verifyEclipseComImgFolder() throws Exception {
		 int iEclipseComImgFiles = 0;		 
		 String sEclipseComImgFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComImgFolder = System.getProperty("eclipsecomimgdir.value");		 
		 System.out.println("files location="+sEclipseComImgFolder);
		 File f = new File(sEclipseComImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComImgFiles = fArray.length;
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
		 return iEclipseComImgFiles;		 	  
  }
  //Verify that the 17 files exists under /com/images/eclipse
  public int verifyEclipseComImgEclipseFolder() throws Exception {
		 int iEclipseComImgEclipseFiles = 0;		 
		 String sEclipseComImgEclipseFolder = null;		 	
		 System.out.println("************************************");
		 sEclipseComImgEclipseFolder = System.getProperty("eclipsecomimgeclipsedir.value");		 
		 System.out.println("files location="+sEclipseComImgEclipseFolder);
		 File f = new File(sEclipseComImgEclipseFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEclipseComImgEclipseFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iEclipseComImgEclipseFiles;		 	  
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