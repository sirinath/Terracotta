/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SampleJmxTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SampleJmxTest.class);
  }

  public void testForVerification() throws Exception {
	  	int  iCount = verifyJmxFolderFiles(); //1
	    assertEquals(10, iCount);
	    int  iJmxLibCount = verifyJmxLibFolderFiles(); //2
	    assertEquals(11, iJmxLibCount);
	    int  iJmxSrcCount = verifyJmxSrcFolderFiles(); //3
	    assertEquals(1, iJmxSrcCount);
	    int  iJmxSrcMainCount = verifyJmxSrcMainFolderFiles(); //4
	    assertEquals(3, iJmxSrcMainCount);
	    int  iJmxSrcMainJavaCount = verifyJmxSrcMainJavaFolderFiles(); //5
	    assertEquals(1, iJmxSrcMainJavaCount);
	    int  iJmxSrcMainJavaDemoJmxCount = verifyJmxSrcMainJavaDemoJmxFolderFiles(); //6
	    assertEquals(7, iJmxSrcMainJavaDemoJmxCount);
	    int  iJmxSrcMainJavaDemoJmxWebCount = verifyJmxSrcMainJavaDemoJmxWebFolderFiles(); //7
	    assertEquals(1, iJmxSrcMainJavaDemoJmxWebCount);	    
	    int  iJmxSrcMainResDemoJmxCount = verifyJmxSrcMainResDemoJmxFolderFiles(); //8
	    assertEquals(1, iJmxSrcMainResDemoJmxCount);
	    int  iJmxSrcMainWebappCount = verifyJmxSrcMainWebappFolderFiles(); //9
	    assertEquals(2, iJmxSrcMainWebappCount);
	    int  iJmxSrcMainWebappINFCount = verifyJmxSrcMainWebappINFFolderFiles(); //10
	    assertEquals(4, iJmxSrcMainWebappINFCount);
	    int  iJmxSrcMainWebappINFTldCount = verifyJmxSrcMainWebappINFTldFolderFiles(); //11
	    assertEquals(2, iJmxSrcMainWebappINFTldCount);
	    int  iJmxSrcMainWebappINFViewCount = verifyJmxSrcMainWebappINFViewFolderFiles(); //12
	    assertEquals(1, iJmxSrcMainWebappINFViewCount);	    	    	    
	    int  iJmxTargetCount = verifyJmxTargetFolderFiles(); //13
	    assertEquals(2, iJmxTargetCount);
	    int  iJmxTargetClassesCount = verifyJmxTargetClassesFolderFiles(); //14
	    assertEquals(7, iJmxTargetClassesCount);
	    int  iJmxTargetClassesWebCount = verifyJmxTargetClassesWebFolderFiles(); //15
	    assertEquals(1, iJmxTargetClassesWebCount);	    	    
	    int  iJmxTomcat1Count = verifyJmxTomcatFolderFiles(System.getProperty("ssjmxtomcat1.dir")); //16
	    assertEquals(1, iJmxTomcat1Count);
	    int  iJmxTomcat1ConfCount = verifyJmxTomcatConfFolderFiles(System.getProperty("ssjmxtomcat1conf.dir")); //17
	    assertEquals(5, iJmxTomcat1ConfCount);
	    int  iJmxTomcat1ConfCatCount = verifyJmxTomcatConfCatFolderFiles(System.getProperty("ssjmxtomcat1confcat.dir")); //18
	    assertEquals(1, iJmxTomcat1ConfCatCount);
	    int  iJmxTomcat1ConfCatLocalCount = verifyJmxTomcatConfCatLocalFolderFiles(System.getProperty("ssjmxtomcat1confcatlocal.dir")); //19
	    assertEquals(1, iJmxTomcat1ConfCatLocalCount);
	    int  iJmxTomcat2Count = verifyJmxTomcatFolderFiles(System.getProperty("ssjmxtomcat2.dir")); 
	    assertEquals(1, iJmxTomcat2Count);
	    int  iJmxTomcat2ConfCount = verifyJmxTomcatConfFolderFiles(System.getProperty("sscotomcat2conf.dir")); 
	    assertEquals(5, iJmxTomcat2ConfCount);
	    int  iJmxTomcat2ConfCatCount = verifyJmxTomcatConfCatFolderFiles(System.getProperty("ssjmxtomcat2confcat.dir")); 
	    assertEquals(1, iJmxTomcat2ConfCatCount);
	    int  iJmxTomcat2ConfCatLocalCount = verifyJmxTomcatConfCatLocalFolderFiles(System.getProperty("ssjmxtomcat2confcatlocal.dir")); 
	    assertEquals(1, iJmxTomcat2ConfCatLocalCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
  //1. Verify that the 5 folders and 5 files exists under terracotta-2.1.0\spring\samples\jmx folder
  public int verifyJmxFolderFiles() throws Exception {
		 int iJmxFiles = 0;		 
		 String sJmxFolder = null;		 	
		 System.out.println("************************************");
		 sJmxFolder = System.getProperty("ssjmx.dir");		 
		 System.out.println("files location="+sJmxFolder);
		 File f = new File(sJmxFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxFiles = fArray.length;
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
		 return iJmxFiles;		 	  
  }
  //2. Verify that the 11 files exists under terracotta-2.1.0\spring\samples\jmx\lib folder
  public int verifyJmxLibFolderFiles() throws Exception {
		 int iJmxLibFiles = 0;		 
		 String sJmxLibFolder = null;		 	
		 System.out.println("************************************");
		 sJmxLibFolder = System.getProperty("ssjmxlib.dir");		 
		 System.out.println("files location="+sJmxLibFolder);
		 File f = new File(sJmxLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxLibFiles = fArray.length;
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
		 return iJmxLibFiles;		 	  
  }
  //3. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\jmx\src folder
  public int verifyJmxSrcFolderFiles() throws Exception {
		 int iJmxSrcFiles = 0;		 
		 String sJmxSrcFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcFolder = System.getProperty("ssjmxsrc.dir");		 
		 System.out.println("files location="+sJmxSrcFolder);
		 File f = new File(sJmxSrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcFiles = fArray.length;
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
		 return iJmxSrcFiles;		 	  
  }
  //4. Verify that the 3 folders exists under terracotta-2.1.0\spring\samples\jmx\src\main folder
  public int verifyJmxSrcMainFolderFiles() throws Exception {
		 int iJmxSrcMainFiles = 0;		 
		 String sJmxSrcMainFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainFolder = System.getProperty("ssjmxsrcmain.dir");		 
		 System.out.println("files location="+sJmxSrcMainFolder);
		 File f = new File(sJmxSrcMainFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainFiles = fArray.length;
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
		 return iJmxSrcMainFiles;		 	  
  }
  //5. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\jmx\src\main\java folder
  public int verifyJmxSrcMainJavaFolderFiles() throws Exception {
		 int iJmxSrcMainJavaFiles = 0;		 
		 String sJmxSrcMainJavaFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainJavaFolder = System.getProperty("ssjmxsrcmainjava.dir");		 
		 System.out.println("files location="+sJmxSrcMainJavaFolder);
		 File f = new File(sJmxSrcMainJavaFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainJavaFiles = fArray.length;
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
		 return iJmxSrcMainJavaFiles;		 	  
  }
  //6. Verify that the 1 folder and 6 files exists under terracotta-2.1.0\spring\samples\jmx\src\main\java\demo\jmx folder
  public int verifyJmxSrcMainJavaDemoJmxFolderFiles() throws Exception {
		 int iJmxSrcMainJavaDemoJmxFiles = 0;		 
		 String sJmxSrcMainJavaDemoJmxFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainJavaDemoJmxFolder = System.getProperty("ssjmxsrcmainjavademojmx.dir");		 
		 System.out.println("files location="+sJmxSrcMainJavaDemoJmxFolder);
		 File f = new File(sJmxSrcMainJavaDemoJmxFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainJavaDemoJmxFiles = fArray.length;
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
		 return iJmxSrcMainJavaDemoJmxFiles;		 	  
  }
  //7. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\jmx\src\main\java\demo\jmx folder
  public int verifyJmxSrcMainJavaDemoJmxWebFolderFiles() throws Exception {
		 int iJmxSrcMainJavaDemoJmxWebFiles = 0;		 
		 String sJmxSrcMainJavaDemoJmxWebFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainJavaDemoJmxWebFolder = System.getProperty("ssjmxsrcmainjavademojmxweb.dir");		 
		 System.out.println("files location="+sJmxSrcMainJavaDemoJmxWebFolder);
		 File f = new File(sJmxSrcMainJavaDemoJmxWebFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainJavaDemoJmxWebFiles = fArray.length;
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
		 return iJmxSrcMainJavaDemoJmxWebFiles;		 	  
  }
  //8. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\jmx\src\main\resources\demo\jmx folder
  public int verifyJmxSrcMainResDemoJmxFolderFiles() throws Exception {
		 int iJmxSrcMainResDemoJmxFiles = 0;		 
		 String sJmxSrcMainResDemoJmxFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainResDemoJmxFolder = System.getProperty("ssjmxsrcmainresdemojmx.dir");		 
		 System.out.println("files location="+sJmxSrcMainResDemoJmxFolder);
		 File f = new File(sJmxSrcMainResDemoJmxFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainResDemoJmxFiles = fArray.length;
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
		 return iJmxSrcMainResDemoJmxFiles;		 	  
  }
  //9. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\jmx\src\main\webapp folder
  public int verifyJmxSrcMainWebappFolderFiles() throws Exception {
		 int iJmxSrcMainWebappFiles = 0;		 
		 String sJmxSrcMainWebappFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainWebappFolder = System.getProperty("ssjmxsrcmainwebapp.dir");		 
		 System.out.println("files location="+sJmxSrcMainWebappFolder);
		 File f = new File(sJmxSrcMainWebappFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainWebappFiles = fArray.length;
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
		 return iJmxSrcMainWebappFiles;		 	  
  }
  //10. Verify that the 2 folders and 2 files exists under terracotta-2.1.0\spring\samples\jmx\src\main\webapp\WEB-INF folder
  public int verifyJmxSrcMainWebappINFFolderFiles() throws Exception {
		 int iJmxSrcMainWebappINFFiles = 0;		 
		 String sJmxSrcMainWebappINFFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainWebappINFFolder = System.getProperty("ssjmxsrcmainwebappINF.dir");		 
		 System.out.println("files location="+sJmxSrcMainWebappINFFolder);
		 File f = new File(sJmxSrcMainWebappINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainWebappINFFiles = fArray.length;
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
		 return iJmxSrcMainWebappINFFiles;		 	  
  }
  //11. Verify that the 2 files exists under terracotta-2.1.0\spring\samples\jmx\src\main\webapp\WEB-INF\tld folder
  public int verifyJmxSrcMainWebappINFTldFolderFiles() throws Exception {
		 int iJmxSrcMainWebappINFTldFiles = 0;		 
		 String sJmxSrcMainWebappINFTldFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainWebappINFTldFolder = System.getProperty("ssjmxsrcmainwebappINFtld.dir");		 
		 System.out.println("files location="+sJmxSrcMainWebappINFTldFolder);
		 File f = new File(sJmxSrcMainWebappINFTldFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainWebappINFTldFiles = fArray.length;
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
		 return iJmxSrcMainWebappINFTldFiles;		 	  
  }
  //12. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\jmx\src\main\webapp\WEB-INF\view folder
  public int verifyJmxSrcMainWebappINFViewFolderFiles() throws Exception {
		 int iJmxSrcMainWebappINFViewFiles = 0;		 
		 String sJmxSrcMainWebappINFViewFolder = null;		 	
		 System.out.println("************************************");
		 sJmxSrcMainWebappINFViewFolder = System.getProperty("ssjmxsrcmainwebappINFview.dir");		 
		 System.out.println("files location="+sJmxSrcMainWebappINFViewFolder);
		 File f = new File(sJmxSrcMainWebappINFViewFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxSrcMainWebappINFViewFiles = fArray.length;
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
		 return iJmxSrcMainWebappINFViewFiles;		 	  
  }  
  //13. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\jmx\target folder
  public int verifyJmxTargetFolderFiles() throws Exception {
		 int iJmxTargetFiles = 0;		 
		 String sTargetFolder = null;		 	
		 System.out.println("************************************");
		 sTargetFolder = System.getProperty("ssjmxtarget.dir");		 
		 System.out.println("files location="+sTargetFolder);
		 File f = new File(sTargetFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxTargetFiles = fArray.length;
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
		 return iJmxTargetFiles;		 	  
  }
  //14. Verify that the 1 folder and 6 files exists under terracotta-2.1.0\spring\samples\jmx\target\classes\demo\jmx folder
  public int verifyJmxTargetClassesFolderFiles() throws Exception {
		 int iJmxTargetClassesFiles = 0;		 
		 String sJmxTargetClassesFolder = null;		 	
		 System.out.println("************************************");
		 sJmxTargetClassesFolder = System.getProperty("ssjmxtargetclasses.dir");		 
		 System.out.println("files location="+sJmxTargetClassesFolder);
		 File f = new File(sJmxTargetClassesFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxTargetClassesFiles = fArray.length;
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
		 return iJmxTargetClassesFiles;		 	  
  }
  //15. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\jmx\target\classes\demo\jmx\web folder
  public int verifyJmxTargetClassesWebFolderFiles() throws Exception {
		 int iJmxTargetClassesWebFiles = 0;		 
		 String sJmxTargetClassesWebFolder = null;		 	
		 System.out.println("************************************");
		 sJmxTargetClassesWebFolder = System.getProperty("ssjmxtargetclassesdemojmxweb.dir");		 
		 System.out.println("files location="+sJmxTargetClassesWebFolder);
		 File f = new File(sJmxTargetClassesWebFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxTargetClassesWebFiles = fArray.length;
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
		 return iJmxTargetClassesWebFiles;		 	  
  }
  //16. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\jmx\tomcat1 folder
  // Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\jmx\tomcat2 folder
  public int verifyJmxTomcatFolderFiles(String s) throws Exception {
		 int iJmxTomcatFiles = 0;		 
		 String sJmxTomcatFolder = null;		 	
		 System.out.println("************************************");
		 sJmxTomcatFolder = s;		 
		 System.out.println("files location="+sJmxTomcatFolder);
		 File f = new File(sJmxTomcatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxTomcatFiles = fArray.length;
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
		 return iJmxTomcatFiles;		 	  
  }
  //17. Verify that the 1 folder and 4 files exists under terracotta-2.1.0\spring\samples\jmx\tomcat1\conf folder
  public int verifyJmxTomcatConfFolderFiles(String s) throws Exception {
		 int iJmxTomcatConfFiles = 0;		 
		 String sJmxTomcatConfFolder = null;		 	
		 System.out.println("************************************");
		 sJmxTomcatConfFolder = s;		 
		 System.out.println("files location="+sJmxTomcatConfFolder);
		 File f = new File(sJmxTomcatConfFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxTomcatConfFiles = fArray.length;
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
		 return iJmxTomcatConfFiles;		 	  
  }
  //18. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\jmx\tomcat1\conf\Catalina folder
  public int verifyJmxTomcatConfCatFolderFiles(String s) throws Exception {
		 int iJmxTomcatConfCatFiles = 0;		 
		 String sJmxTomcatConfCatFolder = null;		 	
		 System.out.println("************************************");
		 sJmxTomcatConfCatFolder = s;		 
		 System.out.println("files location="+sJmxTomcatConfCatFolder);
		 File f = new File(sJmxTomcatConfCatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxTomcatConfCatFiles = fArray.length;
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
		 return iJmxTomcatConfCatFiles;		 	  
  }
  //19. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\jmx\tomcat1\conf\Catalina\localhost folder
  public int verifyJmxTomcatConfCatLocalFolderFiles(String s) throws Exception {
		 int iJmxTomcatConfCatLocalFiles = 0;		 
		 String sJmxTomcatConfCatLocalFolder = null;		 	
		 System.out.println("************************************");
		 sJmxTomcatConfCatLocalFolder = s;		 
		 System.out.println("files location="+sJmxTomcatConfCatLocalFolder);
		 File f = new File(sJmxTomcatConfCatLocalFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJmxTomcatConfCatLocalFiles = fArray.length;
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
		 return iJmxTomcatConfCatLocalFiles;		 	  
  }
}