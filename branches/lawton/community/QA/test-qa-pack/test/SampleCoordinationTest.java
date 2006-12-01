/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SampleCoordinationTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SampleCoordinationTest.class);
  }

  public void testForVerification() throws Exception {
	  	int  iCount = verifyCoFolderFiles(); //1
	    assertEquals(10, iCount);
	    int  iCoLibCount = verifyCoLibFolderFiles(); //2
	    assertEquals(7, iCoLibCount);
	    int  iCoSrcCount = verifyCoSrcFolderFiles(); //3
	    assertEquals(1, iCoSrcCount);
	    int  iCoSrcMainCount = verifyCoSrcMainFolderFiles(); //4
	    assertEquals(3, iCoSrcMainCount);
	    int  iCoSrcMainJavaCount = verifyCoSrcMainJavaFolderFiles(); //5
	    assertEquals(1, iCoSrcMainJavaCount);
	    int  iCoSrcMainJavaDemoCoCount = verifyCoSrcMainJavaDemoCoFolderFiles(); //6
	    assertEquals(1, iCoSrcMainJavaDemoCoCount);	
	    int  iCoSrcMainResDemoCoCount = verifyCoSrcMainResDemoCoFolderFiles(); //7
	    assertEquals(1, iCoSrcMainResDemoCoCount);
	    int  iCoSrcMainWebappCount = verifyCoSrcMainWebappFolderFiles(); //8
	    assertEquals(2, iCoSrcMainWebappCount);
	    int  iCoSrcMainWebappINFCount = verifyCoSrcMainWebappINFFolderFiles(); //9
	    assertEquals(2, iCoSrcMainWebappINFCount);
	    int  iCoTargetCount = verifyCoTargetFolderFiles(); //10
	    assertEquals(2, iCoTargetCount);
	    int  iCoTargetClassesCount = verifyCoTargetClassesFolderFiles(); //11
	    assertEquals(1, iCoTargetClassesCount);
	    int  iCoTomcat1Count = verifyCoTomcatFolderFiles(System.getProperty("sscotomcat1.dir")); //12
	    assertEquals(1, iCoTomcat1Count);
	    int  iCoTomcat1ConfCount = verifyCoTomcatConfFolderFiles(System.getProperty("sscotomcat1conf.dir")); //13
	    assertEquals(5, iCoTomcat1ConfCount);
	    int  iCoTomcat1ConfCatCount = verifyCoTomcatConfCatFolderFiles(System.getProperty("sscotomcat1confcat.dir")); //14
	    assertEquals(1, iCoTomcat1ConfCatCount);
	    int  iCoTomcat1ConfCatLocalCount = verifyCoTomcatConfCatLocalFolderFiles(System.getProperty("sscotomcat1confcatlocal.dir")); //15
	    assertEquals(1, iCoTomcat1ConfCatLocalCount);
	    int  iCoTomcat2Count = verifyCoTomcatFolderFiles(System.getProperty("sscotomcat2.dir")); 
	    assertEquals(1, iCoTomcat2Count);
	    int  iCoTomcat2ConfCount = verifyCoTomcatConfFolderFiles(System.getProperty("sscotomcat2conf.dir")); 
	    assertEquals(5, iCoTomcat2ConfCount);
	    int  iCoTomcat2ConfCatCount = verifyCoTomcatConfCatFolderFiles(System.getProperty("sscotomcat2confcat.dir")); 
	    assertEquals(1, iCoTomcat2ConfCatCount);
	    int  iCoTomcat2ConfCatLocalCount = verifyCoTomcatConfCatLocalFolderFiles(System.getProperty("sscotomcat1confcatlocal.dir")); 
	    assertEquals(1, iCoTomcat2ConfCatLocalCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
  //1. Verify that the 5 folders and 5 files exists under terracotta-2.1.0\spring\samples\coordination folder
  public int verifyCoFolderFiles() throws Exception {
		 int iCoFiles = 0;		 
		 String sCoFolder = null;		 	
		 System.out.println("************************************");
		 sCoFolder = System.getProperty("sscoordination.dir");		 
		 System.out.println("files location="+sCoFolder);
		 File f = new File(sCoFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoFiles = fArray.length;
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
		 return iCoFiles;		 	  
  }
  //2. Verify that the 7 files exists under terracotta-2.1.0\spring\samples\coordination\lib folder
  public int verifyCoLibFolderFiles() throws Exception {
		 int iCoLibFiles = 0;		 
		 String sCoLibFolder = null;		 	
		 System.out.println("************************************");
		 sCoLibFolder = System.getProperty("sscolib.dir");		 
		 System.out.println("files location="+sCoLibFolder);
		 File f = new File(sCoLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoLibFiles = fArray.length;
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
		 return iCoLibFiles;		 	  
  }
  //3. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\coordination\src folder
  public int verifyCoSrcFolderFiles() throws Exception {
		 int iCoSrcFiles = 0;		 
		 String sCoSrcFolder = null;		 	
		 System.out.println("************************************");
		 sCoSrcFolder = System.getProperty("sscosrc.dir");		 
		 System.out.println("files location="+sCoSrcFolder);
		 File f = new File(sCoSrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoSrcFiles = fArray.length;
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
		 return iCoSrcFiles;		 	  
  }
  //4. Verify that the 3 folders exists under terracotta-2.1.0\spring\samples\coordination\src\main folder
  public int verifyCoSrcMainFolderFiles() throws Exception {
		 int iCoSrcMainFiles = 0;		 
		 String sCoSrcMainFolder = null;		 	
		 System.out.println("************************************");
		 sCoSrcMainFolder = System.getProperty("sscosrcmain.dir");		 
		 System.out.println("files location="+sCoSrcMainFolder);
		 File f = new File(sCoSrcMainFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoSrcMainFiles = fArray.length;
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
		 return iCoSrcMainFiles;		 	  
  }
  //5. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\coordination\src\main\java folder
  public int verifyCoSrcMainJavaFolderFiles() throws Exception {
		 int iCoSrcMainJavaFiles = 0;		 
		 String sCoSrcMainJavaFolder = null;		 	
		 System.out.println("************************************");
		 sCoSrcMainJavaFolder = System.getProperty("sscosrcmainjava.dir");		 
		 System.out.println("files location="+sCoSrcMainJavaFolder);
		 File f = new File(sCoSrcMainJavaFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoSrcMainJavaFiles = fArray.length;
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
		 return iCoSrcMainJavaFiles;		 	  
  }
  //6. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\coordination\src\main\java\demo\coordination folder
  public int verifyCoSrcMainJavaDemoCoFolderFiles() throws Exception {
		 int iCoSrcMainJavaDemoCoFiles = 0;		 
		 String sCoSrcMainJavaDemoCoFolder = null;		 	
		 System.out.println("************************************");
		 sCoSrcMainJavaDemoCoFolder = System.getProperty("sscosrcmainjavademoco.dir");		 
		 System.out.println("files location="+sCoSrcMainJavaDemoCoFolder);
		 File f = new File(sCoSrcMainJavaDemoCoFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoSrcMainJavaDemoCoFiles = fArray.length;
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
		 return iCoSrcMainJavaDemoCoFiles;		 	  
  }
  //7. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\coordination\src\main\resources\demo\coordination folder
  public int verifyCoSrcMainResDemoCoFolderFiles() throws Exception {
		 int iCoSrcMainResDemoCoFiles = 0;		 
		 String sCoSrcMainResDemoCoFolder = null;		 	
		 System.out.println("************************************");
		 sCoSrcMainResDemoCoFolder = System.getProperty("sscosrcmainresdemoco.dir");		 
		 System.out.println("files location="+sCoSrcMainResDemoCoFolder);
		 File f = new File(sCoSrcMainResDemoCoFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoSrcMainResDemoCoFiles = fArray.length;
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
		 return iCoSrcMainResDemoCoFiles;		 	  
  }
  //8. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\coordination\src\main\webapp folder
  public int verifyCoSrcMainWebappFolderFiles() throws Exception {
		 int iCoSrcMainWebappFiles = 0;		 
		 String sCoSrcMainWebappFolder = null;		 	
		 System.out.println("************************************");
		 sCoSrcMainWebappFolder = System.getProperty("sscosrcmainwebapp.dir");		 
		 System.out.println("files location="+sCoSrcMainWebappFolder);
		 File f = new File(sCoSrcMainWebappFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoSrcMainWebappFiles = fArray.length;
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
		 return iCoSrcMainWebappFiles;		 	  
  }
  //9. Verify that the 2 files exists under terracotta-2.1.0\spring\samples\coordination\src\main\webapp\WEB-INF folder
  public int verifyCoSrcMainWebappINFFolderFiles() throws Exception {
		 int iCoSrcMainWebappINFFiles = 0;		 
		 String sCoSrcMainWebappINFFolder = null;		 	
		 System.out.println("************************************");
		 sCoSrcMainWebappINFFolder = System.getProperty("sscosrcmainwebappINF.dir");		 
		 System.out.println("files location="+sCoSrcMainWebappINFFolder);
		 File f = new File(sCoSrcMainWebappINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoSrcMainWebappINFFiles = fArray.length;
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
		 return iCoSrcMainWebappINFFiles;		 	  
  } 
  //10. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\spring\samples\coordination\target folder
  public int verifyCoTargetFolderFiles() throws Exception {
		 int iCoTargetFiles = 0;		 
		 String sTargetFolder = null;		 	
		 System.out.println("************************************");
		 sTargetFolder = System.getProperty("sscotarget.dir");		 
		 System.out.println("files location="+sTargetFolder);
		 File f = new File(sTargetFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoTargetFiles = fArray.length;
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
		 return iCoTargetFiles;		 	  
  }
  //11. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\coordination\target\classes\demo\coordination folder
  public int verifyCoTargetClassesFolderFiles() throws Exception {
		 int iCoTargetClassesFiles = 0;		 
		 String sCoTargetClassesFolder = null;		 	
		 System.out.println("************************************");
		 sCoTargetClassesFolder = System.getProperty("sscotargetclasses.dir");		 
		 System.out.println("files location="+sCoTargetClassesFolder);
		 File f = new File(sCoTargetClassesFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoTargetClassesFiles = fArray.length;
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
		 return iCoTargetClassesFiles;		 	  
  }
  //12. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\coordination\tomcat1 folder
  // Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\coordination\tomcat2 folder
  public int verifyCoTomcatFolderFiles(String s) throws Exception {
		 int iCoTomcatFiles = 0;		 
		 String sCoTomcatFolder = null;		 	
		 System.out.println("************************************");
		 sCoTomcatFolder = s;		 
		 System.out.println("files location="+sCoTomcatFolder);
		 File f = new File(sCoTomcatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoTomcatFiles = fArray.length;
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
		 return iCoTomcatFiles;		 	  
  }
  //13. Verify that the 1 folder and 4 files exists under terracotta-2.1.0\spring\samples\coordination\tomcat1\conf folder
  public int verifyCoTomcatConfFolderFiles(String s) throws Exception {
		 int iCoTomcatConfFiles = 0;		 
		 String sCoTomcatConfFolder = null;		 	
		 System.out.println("************************************");
		 sCoTomcatConfFolder = s;		 
		 System.out.println("files location="+sCoTomcatConfFolder);
		 File f = new File(sCoTomcatConfFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoTomcatConfFiles = fArray.length;
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
		 return iCoTomcatConfFiles;		 	  
  }
  //14. Verify that the 1 folder exists under terracotta-2.1.0\spring\samples\coordination\tomcat1\conf\Catalina folder
  public int verifyCoTomcatConfCatFolderFiles(String s) throws Exception {
		 int iCoTomcatConfCatFiles = 0;		 
		 String sCoTomcatConfCatFolder = null;		 	
		 System.out.println("************************************");
		 sCoTomcatConfCatFolder = s;		 
		 System.out.println("files location="+sCoTomcatConfCatFolder);
		 File f = new File(sCoTomcatConfCatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoTomcatConfCatFiles = fArray.length;
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
		 return iCoTomcatConfCatFiles;		 	  
  }
  //15. Verify that the 1 file exists under terracotta-2.1.0\spring\samples\coordination\tomcat1\conf\Catalina\localhost folder
  public int verifyCoTomcatConfCatLocalFolderFiles(String s) throws Exception {
		 int iCoTomcatConfCatLocalFiles = 0;		 
		 String sCoTomcatConfCatLocalFolder = null;		 	
		 System.out.println("************************************");
		 sCoTomcatConfCatLocalFolder = s;		 
		 System.out.println("files location="+sCoTomcatConfCatLocalFolder);
		 File f = new File(sCoTomcatConfCatLocalFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCoTomcatConfCatLocalFiles = fArray.length;
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
		 return iCoTomcatConfCatLocalFiles;		 	  
  }
  
  
  
  
  
  
   
}