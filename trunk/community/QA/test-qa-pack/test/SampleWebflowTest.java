/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SampleWebflowTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SampleWebflowTest.class);
  }

  public void testForVerification() throws Exception {
	  	int  iCount = verifyWebflowFolderFiles(); //1
	    assertEquals(10, iCount);
	    int  iWebflowLibCount = verifyWebflowLibFolderFiles(); //2
	    assertEquals(9, iWebflowLibCount);
	    int  iWebflowSrcCount = verifyWebflowSrcFolderFiles(); //3
	    assertEquals(1, iWebflowSrcCount);
	    int  iWebflowSrcMainCount = verifyWebflowSrcMainFolderFiles(); //4
	    assertEquals(2, iWebflowSrcMainCount);
	    int  iWebflowSrcMainJavaCount = verifyWebflowSrcMainJavaFolderFiles(); //5
	    assertEquals(1, iWebflowSrcMainJavaCount);
	    int  iWebflowSrcMainJavaDemoWebflowCount = verifyWebflowSrcMainJavaDemoWebflowFolderFiles(); //6
	    assertEquals(2, iWebflowSrcMainJavaDemoWebflowCount);
	    //int  iWebflowSrcMainJavaDemoWebflowWebCount = verifyWebflowSrcMainJavaDemoWebflowWebFolderFiles(); //7
	    //assertEquals(1, iWebflowSrcMainJavaDemoWebflowWebCount);	    
	    //int  iWebflowSrcMainResDemoWebflowCount = verifyWebflowSrcMainResDemoWebflowFolderFiles(); //8
	    //assertEquals(1, iWebflowSrcMainResDemoWebflowCount);
	    int  iWebflowSrcMainWebappCount = verifyWebflowSrcMainWebappFolderFiles(); //9
	    assertEquals(2, iWebflowSrcMainWebappCount);
	    int  iWebflowSrcMainWebappINFCount = verifyWebflowSrcMainWebappINFFolderFiles(); //10
	    assertEquals(4, iWebflowSrcMainWebappINFCount);
	    //int  iWebflowSrcMainWebappINFTldCount = verifyWebflowSrcMainWebappINFTldFolderFiles(); //11
	    //assertEquals(2, iWebflowSrcMainWebappINFTldCount);
	    //int  iWebflowSrcMainWebappINFViewCount = verifyWebflowSrcMainWebappINFViewFolderFiles(); //12
	    //assertEquals(1, iWebflowSrcMainWebappINFViewCount);	    	    	    
	    int  iWebflowTargetCount = verifyWebflowTargetFolderFiles(); //13
	    assertEquals(2, iWebflowTargetCount);
	    int  iWebflowTargetClassesCount = verifyWebflowTargetClassesFolderFiles(); //14
	    assertEquals(2, iWebflowTargetClassesCount);
	    //int  iWebflowTargetClassesWebCount = verifyWebflowTargetClassesWebFolderFiles(); //15
	    //assertEquals(1, iWebflowTargetClassesWebCount);	    	    
	    int  iWebflowTomcat1Count = verifyWebflowTomcatFolderFiles(System.getProperty("sswebflowtomcat1.dir")); //16
	    assertEquals(1, iWebflowTomcat1Count);
	    int  iWebflowTomcat1ConfCount = verifyWebflowTomcatConfFolderFiles(System.getProperty("sswebflowtomcat1conf.dir")); //17
	    assertEquals(5, iWebflowTomcat1ConfCount);
	    int  iWebflowTomcat1ConfCatCount = verifyWebflowTomcatConfCatFolderFiles(System.getProperty("sswebflowtomcat1confcat.dir")); //18
	    assertEquals(1, iWebflowTomcat1ConfCatCount);
	    int  iWebflowTomcat1ConfCatLocalCount = verifyWebflowTomcatConfCatLocalFolderFiles(System.getProperty("sswebflowtomcat1confcatlocal.dir")); //19
	    assertEquals(1, iWebflowTomcat1ConfCatLocalCount);
	    int  iWebflowTomcat2Count = verifyWebflowTomcatFolderFiles(System.getProperty("sswebflowtomcat2.dir")); 
	    assertEquals(1, iWebflowTomcat2Count);
	    int  iWebflowTomcat2ConfCount = verifyWebflowTomcatConfFolderFiles(System.getProperty("sscotomcat2conf.dir")); 
	    assertEquals(5, iWebflowTomcat2ConfCount);
	    int  iWebflowTomcat2ConfCatCount = verifyWebflowTomcatConfCatFolderFiles(System.getProperty("sswebflowtomcat2confcat.dir")); 
	    assertEquals(1, iWebflowTomcat2ConfCatCount);
	    int  iWebflowTomcat2ConfCatLocalCount = verifyWebflowTomcatConfCatLocalFolderFiles(System.getProperty("sswebflowtomcat2confcatlocal.dir")); 
	    assertEquals(1, iWebflowTomcat2ConfCatLocalCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
  //1. Verify that the 5 folders and 5 files exists under terracotta-2.1.0\spring\samples\webflow folder
  public int verifyWebflowFolderFiles() throws Exception {
		 int iWebflowFiles = 0;		 
		 String sWebflowFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowFolder = System.getProperty("sswebflow.dir");		 
		 System.out.println("files location="+sWebflowFolder);
		 File f = new File(sWebflowFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowFiles = fArray.length;
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
		 return iWebflowFiles;		 	  
  }
  //2. Verify that the 9 files exists under terracotta-2.1.0\spring\samples\webflow\lib folder
  public int verifyWebflowLibFolderFiles() throws Exception {
		 int iWebflowLibFiles = 0;		 
		 String sWebflowLibFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowLibFolder = System.getProperty("sswebflowlib.dir");		 
		 System.out.println("files location="+sWebflowLibFolder);
		 File f = new File(sWebflowLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowLibFiles = fArray.length;
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
		 return iWebflowLibFiles;		 	  
  }
  //3. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\webflow\src folder
  public int verifyWebflowSrcFolderFiles() throws Exception {
		 int iWebflowSrcFiles = 0;		 
		 String sWebflowSrcFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcFolder = System.getProperty("sswebflowsrc.dir");		 
		 System.out.println("files location="+sWebflowSrcFolder);
		 File f = new File(sWebflowSrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcFiles = fArray.length;
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
		 return iWebflowSrcFiles;		 	  
  }
  //4. Verify that the 2 folders exists under terracotta-2.1.0\spring\samples\webflow\src\main folder
  public int verifyWebflowSrcMainFolderFiles() throws Exception {
		 int iWebflowSrcMainFiles = 0;		 
		 String sWebflowSrcMainFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainFolder = System.getProperty("sswebflowsrcmain.dir");		 
		 System.out.println("files location="+sWebflowSrcMainFolder);
		 File f = new File(sWebflowSrcMainFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainFiles = fArray.length;
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
		 return iWebflowSrcMainFiles;		 	  
  }
  //5. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\webflow\src\main\java folder
  public int verifyWebflowSrcMainJavaFolderFiles() throws Exception {
		 int iWebflowSrcMainJavaFiles = 0;		 
		 String sWebflowSrcMainJavaFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainJavaFolder = System.getProperty("sswebflowsrcmainjava.dir");		 
		 System.out.println("files location="+sWebflowSrcMainJavaFolder);
		 File f = new File(sWebflowSrcMainJavaFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainJavaFiles = fArray.length;
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
		 return iWebflowSrcMainJavaFiles;		 	  
  }
  //6. Verify that the 1 folder and 6 files exists under terracotta-2.1.0\spring\samples\webflow\src\main\java\demo\webflow folder
  public int verifyWebflowSrcMainJavaDemoWebflowFolderFiles() throws Exception {
		 int iWebflowSrcMainJavaDemoWebflowFiles = 0;		 
		 String sWebflowSrcMainJavaDemoWebflowFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainJavaDemoWebflowFolder = System.getProperty("sswebflowsrcmainjavademowebflow.dir");		 
		 System.out.println("files location="+sWebflowSrcMainJavaDemoWebflowFolder);
		 File f = new File(sWebflowSrcMainJavaDemoWebflowFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainJavaDemoWebflowFiles = fArray.length;
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
		 return iWebflowSrcMainJavaDemoWebflowFiles;		 	  
  }
  //7. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\webflow\src\main\java\demo\webflow folder
  /*public int verifyWebflowSrcMainJavaDemoWebflowWebFolderFiles() throws Exception {
		 int iWebflowSrcMainJavaDemoWebflowWebFiles = 0;		 
		 String sWebflowSrcMainJavaDemoWebflowWebFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainJavaDemoWebflowWebFolder = System.getProperty("sswebflowsrcmainjavademowebflowweb.dir");		 
		 System.out.println("files location="+sWebflowSrcMainJavaDemoWebflowWebFolder);
		 File f = new File(sWebflowSrcMainJavaDemoWebflowWebFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainJavaDemoWebflowWebFiles = fArray.length;
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
		 return iWebflowSrcMainJavaDemoWebflowWebFiles;		 	  
  }*/
  //8. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\webflow\src\main\resources\demo\webflow folder
  /*public int verifyWebflowSrcMainResDemoWebflowFolderFiles() throws Exception {
		 int iWebflowSrcMainResDemoWebflowFiles = 0;		 
		 String sWebflowSrcMainResDemoWebflowFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainResDemoWebflowFolder = System.getProperty("sswebflowsrcmainresdemowebflow.dir");		 
		 System.out.println("files location="+sWebflowSrcMainResDemoWebflowFolder);
		 File f = new File(sWebflowSrcMainResDemoWebflowFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainResDemoWebflowFiles = fArray.length;
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
		 return iWebflowSrcMainResDemoWebflowFiles;		 	  
  }*/
  //9. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\webflow\src\main\webapp folder
  public int verifyWebflowSrcMainWebappFolderFiles() throws Exception {
		 int iWebflowSrcMainWebappFiles = 0;		 
		 String sWebflowSrcMainWebappFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainWebappFolder = System.getProperty("sswebflowsrcmainwebapp.dir");		 
		 System.out.println("files location="+sWebflowSrcMainWebappFolder);
		 File f = new File(sWebflowSrcMainWebappFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainWebappFiles = fArray.length;
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
		 return iWebflowSrcMainWebappFiles;		 	  
  }
  //10. Verify that the 4 files exists under terracotta-2.1.0\spring\samples\webflow\src\main\webapp\WEB-INF folder
  public int verifyWebflowSrcMainWebappINFFolderFiles() throws Exception {
		 int iWebflowSrcMainWebappINFFiles = 0;		 
		 String sWebflowSrcMainWebappINFFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainWebappINFFolder = System.getProperty("sswebflowsrcmainwebappINF.dir");		 
		 System.out.println("files location="+sWebflowSrcMainWebappINFFolder);
		 File f = new File(sWebflowSrcMainWebappINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainWebappINFFiles = fArray.length;
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
		 return iWebflowSrcMainWebappINFFiles;		 	  
  }
  //11. Verify that the 2 files exists under terracotta-2.1.0\spring\samples\webflow\src\main\webapp\WEB-INF\tld folder
  /*public int verifyWebflowSrcMainWebappINFTldFolderFiles() throws Exception {
		 int iWebflowSrcMainWebappINFTldFiles = 0;		 
		 String sWebflowSrcMainWebappINFTldFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainWebappINFTldFolder = System.getProperty("sswebflowsrcmainwebappINFtld.dir");		 
		 System.out.println("files location="+sWebflowSrcMainWebappINFTldFolder);
		 File f = new File(sWebflowSrcMainWebappINFTldFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainWebappINFTldFiles = fArray.length;
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
		 return iWebflowSrcMainWebappINFTldFiles;		 	  
  }*/
  //12. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\webflow\src\main\webapp\WEB-INF\view folder
  /*public int verifyWebflowSrcMainWebappINFViewFolderFiles() throws Exception {
		 int iWebflowSrcMainWebappINFViewFiles = 0;		 
		 String sWebflowSrcMainWebappINFViewFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowSrcMainWebappINFViewFolder = System.getProperty("sswebflowsrcmainwebappINFview.dir.dir");		 
		 System.out.println("files location="+sWebflowSrcMainWebappINFViewFolder);
		 File f = new File(sWebflowSrcMainWebappINFViewFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowSrcMainWebappINFViewFiles = fArray.length;
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
		 return iWebflowSrcMainWebappINFViewFiles;		 	  
  } */ 
  //13. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\webflow\target folder
  public int verifyWebflowTargetFolderFiles() throws Exception {
		 int iWebflowTargetFiles = 0;		 
		 String sTargetFolder = null;		 	
		 System.out.println("************************************");
		 sTargetFolder = System.getProperty("sswebflowtarget.dir");		 
		 System.out.println("files location="+sTargetFolder);
		 File f = new File(sTargetFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowTargetFiles = fArray.length;
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
		 return iWebflowTargetFiles;		 	  
  }
  //14. Verify that the 2 files exists under terracotta-2.1.0\spring\samples\webflow\target\classes\demo\webflow folder
  public int verifyWebflowTargetClassesFolderFiles() throws Exception {
		 int iWebflowTargetClassesFiles = 0;		 
		 String sWebflowTargetClassesFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowTargetClassesFolder = System.getProperty("sswebflowtargetclasses.dir");		 
		 System.out.println("files location="+sWebflowTargetClassesFolder);
		 File f = new File(sWebflowTargetClassesFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowTargetClassesFiles = fArray.length;
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
		 return iWebflowTargetClassesFiles;		 	  
  }
  //15. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\webflow\target\classes\demo\webflow\web folder
  /*public int verifyWebflowTargetClassesWebFolderFiles() throws Exception {
		 int iWebflowTargetClassesWebFiles = 0;		 
		 String sWebflowTargetClassesWebFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowTargetClassesWebFolder = System.getProperty("sswebflowtargetclassesdemowebflowweb.dir");		 
		 System.out.println("files location="+sWebflowTargetClassesWebFolder);
		 File f = new File(sWebflowTargetClassesWebFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowTargetClassesWebFiles = fArray.length;
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
		 return iWebflowTargetClassesWebFiles;		 	  
  }*/
  //16. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\webflow\tomcat1 folder
  // Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\webflow\tomcat2 folder
  public int verifyWebflowTomcatFolderFiles(String s) throws Exception {
		 int iWebflowTomcatFiles = 0;		 
		 String sWebflowTomcatFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowTomcatFolder = s;		 
		 System.out.println("files location="+sWebflowTomcatFolder);
		 File f = new File(sWebflowTomcatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowTomcatFiles = fArray.length;
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
		 return iWebflowTomcatFiles;		 	  
  }
  //17. Verify that the 1 folder and 4 files exists under terracotta-2.1.0\spring\samples\webflow\tomcat1\conf folder
  public int verifyWebflowTomcatConfFolderFiles(String s) throws Exception {
		 int iWebflowTomcatConfFiles = 0;		 
		 String sWebflowTomcatConfFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowTomcatConfFolder = s;		 
		 System.out.println("files location="+sWebflowTomcatConfFolder);
		 File f = new File(sWebflowTomcatConfFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowTomcatConfFiles = fArray.length;
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
		 return iWebflowTomcatConfFiles;		 	  
  }
  //18. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\webflow\tomcat1\conf\Catalina folder
  public int verifyWebflowTomcatConfCatFolderFiles(String s) throws Exception {
		 int iWebflowTomcatConfCatFiles = 0;		 
		 String sWebflowTomcatConfCatFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowTomcatConfCatFolder = s;		 
		 System.out.println("files location="+sWebflowTomcatConfCatFolder);
		 File f = new File(sWebflowTomcatConfCatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowTomcatConfCatFiles = fArray.length;
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
		 return iWebflowTomcatConfCatFiles;		 	  
  }
  //19. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\webflow\tomcat1\conf\Catalina\localhost folder
  public int verifyWebflowTomcatConfCatLocalFolderFiles(String s) throws Exception {
		 int iWebflowTomcatConfCatLocalFiles = 0;		 
		 String sWebflowTomcatConfCatLocalFolder = null;		 	
		 System.out.println("************************************");
		 sWebflowTomcatConfCatLocalFolder = s;		 
		 System.out.println("files location="+sWebflowTomcatConfCatLocalFolder);
		 File f = new File(sWebflowTomcatConfCatLocalFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iWebflowTomcatConfCatLocalFiles = fArray.length;
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
		 return iWebflowTomcatConfCatLocalFiles;		 	  
  }
}