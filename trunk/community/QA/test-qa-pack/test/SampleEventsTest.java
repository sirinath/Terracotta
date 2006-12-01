/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SampleEventsTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SampleEventsTest.class);
  }

  public void testForVerification() throws Exception {
	  	int  iCount = verifyEventsFolderFiles(); //1
	    assertEquals(10, iCount);
	    int  iEventsLibCount = verifyEventsLibFolderFiles(); //2
	    assertEquals(13, iEventsLibCount);
	    int  iEventsSrcCount = verifyEventsSrcFolderFiles(); //3
	    assertEquals(1, iEventsSrcCount);
	    int  iEventsSrcMainCount = verifyEventsSrcMainFolderFiles(); //4
	    assertEquals(3, iEventsSrcMainCount);
	    int  iEventsSrcMainJavaCount = verifyEventsSrcMainJavaFolderFiles(); //5
	    assertEquals(1, iEventsSrcMainJavaCount);
	    int  iEventsSrcMainJavaDemoEventsCount = verifyEventsSrcMainJavaDemoEventsFolderFiles(); //6
	    assertEquals(3, iEventsSrcMainJavaDemoEventsCount);
	    int  iEventsSrcMainJavaDemoEventsWebCount = verifyEventsSrcMainJavaDemoEventsWebFolderFiles(); //7
	    assertEquals(1, iEventsSrcMainJavaDemoEventsWebCount);	    
	    int  iEventsSrcMainResDemoEventsCount = verifyEventsSrcMainResDemoEventsFolderFiles(); //8
	    assertEquals(1, iEventsSrcMainResDemoEventsCount);
	    int  iEventsSrcMainWebappCount = verifyEventsSrcMainWebappFolderFiles(); //9
	    assertEquals(2, iEventsSrcMainWebappCount);
	    int  iEventsSrcMainWebappINFCount = verifyEventsSrcMainWebappINFFolderFiles(); //10
	    assertEquals(4, iEventsSrcMainWebappINFCount);
	    int  iEventsSrcMainWebappINFTldCount = verifyEventsSrcMainWebappINFTldFolderFiles(); //11
	    assertEquals(2, iEventsSrcMainWebappINFTldCount);
	    int  iEventsSrcMainWebappINFViewCount = verifyEventsSrcMainWebappINFViewFolderFiles(); //12
	    assertEquals(1, iEventsSrcMainWebappINFViewCount);	    	    	    
	    int  iEventsTargetCount = verifyEventsTargetFolderFiles(); //13
	    assertEquals(2, iEventsTargetCount);
	    int  iEventsTargetClassesCount = verifyEventsTargetClassesFolderFiles(); //14
	    assertEquals(3, iEventsTargetClassesCount);
	    int  iEventsTargetClassesWebCount = verifyEventsTargetClassesWebFolderFiles(); //15
	    assertEquals(1, iEventsTargetClassesWebCount);	    	    
	    int  iEventsTomcat1Count = verifyEventsTomcatFolderFiles(System.getProperty("sseventstomcat1.dir")); //16
	    assertEquals(1, iEventsTomcat1Count);
	    int  iEventsTomcat1ConfCount = verifyEventsTomcatConfFolderFiles(System.getProperty("sseventstomcat1conf.dir")); //17
	    assertEquals(5, iEventsTomcat1ConfCount);
	    int  iEventsTomcat1ConfCatCount = verifyEventsTomcatConfCatFolderFiles(System.getProperty("sseventstomcat1confcat.dir")); //18
	    assertEquals(1, iEventsTomcat1ConfCatCount);
	    int  iEventsTomcat1ConfCatLocalCount = verifyEventsTomcatConfCatLocalFolderFiles(System.getProperty("sseventstomcat1confcatlocal.dir")); //19
	    assertEquals(1, iEventsTomcat1ConfCatLocalCount);
	    int  iEventsTomcat2Count = verifyEventsTomcatFolderFiles(System.getProperty("sseventstomcat2.dir")); 
	    assertEquals(1, iEventsTomcat2Count);
	    int  iEventsTomcat2ConfCount = verifyEventsTomcatConfFolderFiles(System.getProperty("sscotomcat2conf.dir")); 
	    assertEquals(5, iEventsTomcat2ConfCount);
	    int  iEventsTomcat2ConfCatCount = verifyEventsTomcatConfCatFolderFiles(System.getProperty("sseventstomcat2confcat.dir")); 
	    assertEquals(1, iEventsTomcat2ConfCatCount);
	    int  iEventsTomcat2ConfCatLocalCount = verifyEventsTomcatConfCatLocalFolderFiles(System.getProperty("sseventstomcat2confcatlocal.dir")); 
	    assertEquals(1, iEventsTomcat2ConfCatLocalCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
  //1. Verify that the 5 folders and 5 files exists under terracotta-2.1.0\spring\samples\events folder
  public int verifyEventsFolderFiles() throws Exception {
		 int iEventsFiles = 0;		 
		 String sEventsFolder = null;		 	
		 System.out.println("************************************");
		 sEventsFolder = System.getProperty("ssevents.dir");		 
		 System.out.println("files location="+sEventsFolder);
		 File f = new File(sEventsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsFiles = fArray.length;
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
		 return iEventsFiles;		 	  
  }
  //2. Verify that the 13 files exists under terracotta-2.1.0\spring\samples\events\lib folder
  public int verifyEventsLibFolderFiles() throws Exception {
		 int iEventsLibFiles = 0;		 
		 String sEventsLibFolder = null;		 	
		 System.out.println("************************************");
		 sEventsLibFolder = System.getProperty("sseventslib.dir");		 
		 System.out.println("files location="+sEventsLibFolder);
		 File f = new File(sEventsLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsLibFiles = fArray.length;
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
		 return iEventsLibFiles;		 	  
  }
  //3. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\events\src folder
  public int verifyEventsSrcFolderFiles() throws Exception {
		 int iEventsSrcFiles = 0;		 
		 String sEventsSrcFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcFolder = System.getProperty("sseventssrc.dir");		 
		 System.out.println("files location="+sEventsSrcFolder);
		 File f = new File(sEventsSrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcFiles = fArray.length;
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
		 return iEventsSrcFiles;		 	  
  }
  //4. Verify that the 3 folders exists under terracotta-2.1.0\spring\samples\events\src\main folder
  public int verifyEventsSrcMainFolderFiles() throws Exception {
		 int iEventsSrcMainFiles = 0;		 
		 String sEventsSrcMainFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainFolder = System.getProperty("sseventssrcmain.dir");		 
		 System.out.println("files location="+sEventsSrcMainFolder);
		 File f = new File(sEventsSrcMainFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainFiles = fArray.length;
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
		 return iEventsSrcMainFiles;		 	  
  }
  //5. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\events\src\main\java folder
  public int verifyEventsSrcMainJavaFolderFiles() throws Exception {
		 int iEventsSrcMainJavaFiles = 0;		 
		 String sEventsSrcMainJavaFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainJavaFolder = System.getProperty("sseventssrcmainjava.dir");		 
		 System.out.println("files location="+sEventsSrcMainJavaFolder);
		 File f = new File(sEventsSrcMainJavaFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainJavaFiles = fArray.length;
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
		 return iEventsSrcMainJavaFiles;		 	  
  }
  //6. Verify that the 1 folder and 2 files exists under terracotta-2.1.0\spring\samples\events\src\main\java\demo\events folder
  public int verifyEventsSrcMainJavaDemoEventsFolderFiles() throws Exception {
		 int iEventsSrcMainJavaDemoEventsFiles = 0;		 
		 String sEventsSrcMainJavaDemoEventsFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainJavaDemoEventsFolder = System.getProperty("sseventssrcmainjavademoevents.dir");		 
		 System.out.println("files location="+sEventsSrcMainJavaDemoEventsFolder);
		 File f = new File(sEventsSrcMainJavaDemoEventsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainJavaDemoEventsFiles = fArray.length;
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
		 return iEventsSrcMainJavaDemoEventsFiles;		 	  
  }
  //7. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\events\src\main\java\demo\events folder
  public int verifyEventsSrcMainJavaDemoEventsWebFolderFiles() throws Exception {
		 int iEventsSrcMainJavaDemoEventsWebFiles = 0;		 
		 String sEventsSrcMainJavaDemoEventsWebFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainJavaDemoEventsWebFolder = System.getProperty("sseventssrcmainjavademoeventsweb.dir");		 
		 System.out.println("files location="+sEventsSrcMainJavaDemoEventsWebFolder);
		 File f = new File(sEventsSrcMainJavaDemoEventsWebFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainJavaDemoEventsWebFiles = fArray.length;
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
		 return iEventsSrcMainJavaDemoEventsWebFiles;		 	  
  }
  //8. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\events\src\main\resources\demo\events folder
  public int verifyEventsSrcMainResDemoEventsFolderFiles() throws Exception {
		 int iEventsSrcMainResDemoEventsFiles = 0;		 
		 String sEventsSrcMainResDemoEventsFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainResDemoEventsFolder = System.getProperty("sseventssrcmainresdemoevents.dir");		 
		 System.out.println("files location="+sEventsSrcMainResDemoEventsFolder);
		 File f = new File(sEventsSrcMainResDemoEventsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainResDemoEventsFiles = fArray.length;
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
		 return iEventsSrcMainResDemoEventsFiles;		 	  
  }
  //9. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\events\src\main\webapp folder
  public int verifyEventsSrcMainWebappFolderFiles() throws Exception {
		 int iEventsSrcMainWebappFiles = 0;		 
		 String sEventsSrcMainWebappFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainWebappFolder = System.getProperty("sseventssrcmainwebapp.dir");		 
		 System.out.println("files location="+sEventsSrcMainWebappFolder);
		 File f = new File(sEventsSrcMainWebappFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainWebappFiles = fArray.length;
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
		 return iEventsSrcMainWebappFiles;		 	  
  }
  //10. Verify that the 2 folders and 2 files exists under terracotta-2.1.0\spring\samples\events\src\main\webapp\WEB-INF folder
  public int verifyEventsSrcMainWebappINFFolderFiles() throws Exception {
		 int iEventsSrcMainWebappINFFiles = 0;		 
		 String sEventsSrcMainWebappINFFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainWebappINFFolder = System.getProperty("sseventssrcmainwebappINF.dir");		 
		 System.out.println("files location="+sEventsSrcMainWebappINFFolder);
		 File f = new File(sEventsSrcMainWebappINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainWebappINFFiles = fArray.length;
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
		 return iEventsSrcMainWebappINFFiles;		 	  
  }
  //11. Verify that the 2 files exists under terracotta-2.1.0\spring\samples\events\src\main\webapp\WEB-INF\tld folder
  public int verifyEventsSrcMainWebappINFTldFolderFiles() throws Exception {
		 int iEventsSrcMainWebappINFTldFiles = 0;		 
		 String sEventsSrcMainWebappINFTldFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainWebappINFTldFolder = System.getProperty("sseventssrcmainwebappINFtld.dir");		 
		 System.out.println("files location="+sEventsSrcMainWebappINFTldFolder);
		 File f = new File(sEventsSrcMainWebappINFTldFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainWebappINFTldFiles = fArray.length;
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
		 return iEventsSrcMainWebappINFTldFiles;		 	  
  }
  //12. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\events\src\main\webapp\WEB-INF\view folder
  public int verifyEventsSrcMainWebappINFViewFolderFiles() throws Exception {
		 int iEventsSrcMainWebappINFViewFiles = 0;		 
		 String sEventsSrcMainWebappINFViewFolder = null;		 	
		 System.out.println("************************************");
		 sEventsSrcMainWebappINFViewFolder = System.getProperty("sseventssrcmainwebappINFview.dir");		 
		 System.out.println("files location="+sEventsSrcMainWebappINFViewFolder);
		 File f = new File(sEventsSrcMainWebappINFViewFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsSrcMainWebappINFViewFiles = fArray.length;
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
		 return iEventsSrcMainWebappINFViewFiles;		 	  
  }  
  //13. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\events\target folder
  public int verifyEventsTargetFolderFiles() throws Exception {
		 int iEventsTargetFiles = 0;		 
		 String sTargetFolder = null;		 	
		 System.out.println("************************************");
		 sTargetFolder = System.getProperty("sseventstarget.dir");		 
		 System.out.println("files location="+sTargetFolder);
		 File f = new File(sTargetFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsTargetFiles = fArray.length;
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
		 return iEventsTargetFiles;		 	  
  }
  //14. Verify that the 1 folder and 2 files exists under terracotta-2.1.0\spring\samples\events\target\classes\demo\events folder
  public int verifyEventsTargetClassesFolderFiles() throws Exception {
		 int iEventsTargetClassesFiles = 0;		 
		 String sEventsTargetClassesFolder = null;		 	
		 System.out.println("************************************");
		 sEventsTargetClassesFolder = System.getProperty("sseventstargetclasses.dir");		 
		 System.out.println("files location="+sEventsTargetClassesFolder);
		 File f = new File(sEventsTargetClassesFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsTargetClassesFiles = fArray.length;
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
		 return iEventsTargetClassesFiles;		 	  
  }
  //15. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\events\target\classes\demo\events\web folder
  public int verifyEventsTargetClassesWebFolderFiles() throws Exception {
		 int iEventsTargetClassesWebFiles = 0;		 
		 String sEventsTargetClassesWebFolder = null;		 	
		 System.out.println("************************************");
		 sEventsTargetClassesWebFolder = System.getProperty("sseventstargetclassesdemoeventsweb.dir");		 
		 System.out.println("files location="+sEventsTargetClassesWebFolder);
		 File f = new File(sEventsTargetClassesWebFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsTargetClassesWebFiles = fArray.length;
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
		 return iEventsTargetClassesWebFiles;		 	  
  }
  //16. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\events\tomcat1 folder
  // Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\events\tomcat2 folder
  public int verifyEventsTomcatFolderFiles(String s) throws Exception {
		 int iEventsTomcatFiles = 0;		 
		 String sEventsTomcatFolder = null;		 	
		 System.out.println("************************************");
		 sEventsTomcatFolder = s;		 
		 System.out.println("files location="+sEventsTomcatFolder);
		 File f = new File(sEventsTomcatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsTomcatFiles = fArray.length;
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
		 return iEventsTomcatFiles;		 	  
  }
  //17. Verify that the 1 folder and 4 files exists under terracotta-2.1.0\spring\samples\events\tomcat1\conf folder
  public int verifyEventsTomcatConfFolderFiles(String s) throws Exception {
		 int iEventsTomcatConfFiles = 0;		 
		 String sEventsTomcatConfFolder = null;		 	
		 System.out.println("************************************");
		 sEventsTomcatConfFolder = s;		 
		 System.out.println("files location="+sEventsTomcatConfFolder);
		 File f = new File(sEventsTomcatConfFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsTomcatConfFiles = fArray.length;
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
		 return iEventsTomcatConfFiles;		 	  
  }
  //18. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\events\tomcat1\conf\Catalina folder
  public int verifyEventsTomcatConfCatFolderFiles(String s) throws Exception {
		 int iEventsTomcatConfCatFiles = 0;		 
		 String sEventsTomcatConfCatFolder = null;		 	
		 System.out.println("************************************");
		 sEventsTomcatConfCatFolder = s;		 
		 System.out.println("files location="+sEventsTomcatConfCatFolder);
		 File f = new File(sEventsTomcatConfCatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsTomcatConfCatFiles = fArray.length;
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
		 return iEventsTomcatConfCatFiles;		 	  
  }
  //19. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\events\tomcat1\conf\Catalina\localhost folder
  public int verifyEventsTomcatConfCatLocalFolderFiles(String s) throws Exception {
		 int iEventsTomcatConfCatLocalFiles = 0;		 
		 String sEventsTomcatConfCatLocalFolder = null;		 	
		 System.out.println("************************************");
		 sEventsTomcatConfCatLocalFolder = s;		 
		 System.out.println("files location="+sEventsTomcatConfCatLocalFolder);
		 File f = new File(sEventsTomcatConfCatLocalFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iEventsTomcatConfCatLocalFiles = fArray.length;
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
		 return iEventsTomcatConfCatLocalFiles;		 	  
  }
}