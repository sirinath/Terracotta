/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class TomcatSandBoxPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(TomcatSandBoxPackTest.class);
  }

  public void testForVerification() throws Exception {	    	    
	    int  iCount = verifyTomcatSandboxFolderFiles(); //1
	    assertEquals(4, iCount); 	//with data and logs folders, count is 6    
	    int  iBinCount = verifyTomcatSandboxBinFolderFiles(); //2
	    assertEquals(8, iBinCount);
	    
	    //tomcat-9081 folder
	    int  iTom81Count = verifyTomtTomcatFolderFiles(System.getProperty("tomtom9081dir.value")); //3
	    assertEquals(9, iTom81Count); //with work folder, count is 10
	    int  iTom81BinCount = verifyTomtTomcatBinFolderFiles(System.getProperty("tomtom9081bindir.value")); //4
	    assertEquals(23, iTom81BinCount);
	    int  iTom81CommonCount = verifyTomtTomcatCommonFolder(System.getProperty("tomtom9081commondir.value")); //5
	    assertEquals(2, iTom81CommonCount);
	    int  iTom81CommonEndorseCount = verifyTomtTomcatCommonEndorseFolderFiles(System.getProperty("tomtom9081commonendorsedir.value")); //6
	    assertEquals(2, iTom81CommonEndorseCount);
	    int  iTom81CommonLibCount = verifyTomtTomcatCommonLibFolderFiles(System.getProperty("tomtom9081commonlibdir.value")); //7
	    assertEquals(14, iTom81CommonLibCount);
	    int  iTom81ConfCount = verifyTomtTomcatConfFolderFiles(System.getProperty("tomtom9081confdir.value")); //8
	    assertEquals(8, iTom81ConfCount);
	    int  iTom81ConfSubCount = verifyTomtTomcatConfSubFolderFiles(System.getProperty("tomtom9081confcatalinalocalhostdir.value")); //9
	    assertEquals(2, iTom81ConfSubCount); //with Townsend.xml, count is 3
	    int  iTom81ServerCount = verifyTomtTomcatServerFolderFiles(System.getProperty("tomtom9081serverdir.value")); //10
	    assertEquals(2, iTom81ServerCount);
	    int  iTom81ServerLibCount = verifyTomtTomcatServerLibFolderFiles(System.getProperty("tomtom9081serverlibdir.value")); //11
	    assertEquals(26, iTom81ServerLibCount);
	    int  iTom81ServerWebappsCount = verifyTomtTomcatServerWebappsFolderFiles(System.getProperty("tomtom9081serverwebappsdir.value")); //12
	    assertEquals(2, iTom81ServerWebappsCount);
	    int  iTom81WebappsAdminCount = verifyTomtTomcatServerWebappsAdminFolderFiles(System.getProperty("tomtom9081webappsadmindir.value")); //13
	    assertEquals(7, iTom81WebappsAdminCount);
	    int  iTom81WebappsAdminImgCount = verifyTomtTomcatServerWebappsAdminImgFolderFiles(System.getProperty("tomtom9081webappsadminimgdir.value")); //14
	    assertEquals(30, iTom81WebappsAdminImgCount);	    
	    int  iTom81WebappsAdminResCount = verifyTomtTomcatServerWebappsAdminResFolderFiles(System.getProperty("tomtom9081webappsadminresdir.value")); //15
	    assertEquals(10, iTom81WebappsAdminResCount);
	    int  iTom81WebappsAdminUsersCount = verifyTomtTomcatServerWebappsAdminUsersFolderFiles(System.getProperty("tomtom9081webappsadminusersdir.value")); //16
	    assertEquals(6, iTom81WebappsAdminUsersCount);
	    int  iTom81WebappsAdminWebINFCount = verifyTomtTomcatServerWebappsAdminWebINFFolderFiles(System.getProperty("tomtom9081webappsadminwebinfdir.value")); //17
	    assertEquals(7, iTom81WebappsAdminWebINFCount);
	    int  iTom81WebappsAdminWebINFLibCount = verifyTomtTomcatServerWebappsAdminWebINFLibFolderFiles(System.getProperty("tomtom9081webappsadminwebinflibdir.value")); //18
	    assertEquals(2, iTom81WebappsAdminWebINFLibCount);
	    int  iTom81WebappsManagCount = verifyTomtTomcatServerWebappsMangFolderFiles(System.getProperty("tomtom9081webappsmangdir.value")); //19
	    assertEquals(7, iTom81WebappsManagCount);	    
	    int  iTom81WebappsManagImgCount = verifyTomtTomcatServerWebappsMangImgFolderFiles(System.getProperty("tomtom9081webappsmangimgdir.value")); //20
	    assertEquals(9, iTom81WebappsManagImgCount);
	    int  iTom81WebappsManagINFCount = verifyTomtTomcatServerWebappsMangINFFolderFiles(System.getProperty("tomtom9081webappsmanginfdir.value")); //21
	    assertEquals(2, iTom81WebappsManagINFCount);
	    int  iTom81WebappsManagINFLibCount = verifyTomtTomcatServerWebappsMangINFLibFolderFiles(System.getProperty("tomtom9081webappsmanginflibdir.value")); //22
	    assertEquals(1, iTom81WebappsManagINFLibCount);	
	    int  iTom81WebappsCount = verifyTomtTomcatWebappsFolderFiles(System.getProperty("tomtom9081webappsdir.value")); //23
	    assertEquals(4, iTom81WebappsCount);
	    //assertEquals(7, iTom81WebappsCount); //after running the webapps
	    int  iTom81WebappsRootCount = verifyTomtTomcatWebappsRootFolderFiles(System.getProperty("tomtom9081webappsrootdir.value")); //24
	    assertEquals(7, iTom81WebappsRootCount);
	    int  iTom81WebappsRootINFCount = verifyTomtTomcatWebappsRootINFFolderFiles(System.getProperty("tomtom9081webappsrootinfdir.value")); //25
	    assertEquals(2, iTom81WebappsRootINFCount);
	    int  iTom81WebappsRootINFLibCount = verifyTomtTomcatWebappsRootINFLibFolderFiles(System.getProperty("tomtom9081webappsrootinflibdir.value")); //26
	    assertEquals(1, iTom81WebappsRootINFLibCount);
	    
	    //tomcat-9082 folder
	    int  iTom82Count = verifyTomtTomcatFolderFiles(System.getProperty("tomtom9082dir.value")); //1
	    assertEquals(9, iTom82Count);
	    int  iTom82BinCount = verifyTomtTomcatBinFolderFiles(System.getProperty("tomtom9082bindir.value")); //2
	    assertEquals(23, iTom82BinCount);
	    int  iTom82CommonCount = verifyTomtTomcatCommonFolder(System.getProperty("tomtom9082commondir.value")); //3
	    assertEquals(2, iTom82CommonCount);
	    int  iTom82CommonEndorseCount = verifyTomtTomcatCommonEndorseFolderFiles(System.getProperty("tomtom9082commonendorsedir.value")); //4
	    assertEquals(2, iTom82CommonEndorseCount);
	    int  iTom82CommonLibCount = verifyTomtTomcatCommonLibFolderFiles(System.getProperty("tomtom9082commonlibdir.value")); //5
	    assertEquals(14, iTom82CommonLibCount);
	    int  iTom82ConfCount = verifyTomtTomcatConfFolderFiles(System.getProperty("tomtom9082confdir.value")); //6
	    assertEquals(8, iTom82ConfCount);
	    int  iTom82ConfSubCount = verifyTomtTomcatConfSubFolderFiles(System.getProperty("tomtom9082confcatalinalocalhostdir.value")); //7
	    assertEquals(2, iTom82ConfSubCount);
	    //assertEquals(3, iTom82ConfSubCount); with Townsend.xml, count is 3
	    int  iTom82ServerCount = verifyTomtTomcatServerFolderFiles(System.getProperty("tomtom9082serverdir.value")); //8
	    assertEquals(2, iTom82ServerCount);
	    int  iTom82ServerLibCount = verifyTomtTomcatServerLibFolderFiles(System.getProperty("tomtom9082serverlibdir.value")); //9
	    assertEquals(26, iTom82ServerLibCount);
	    int  iTom82ServerWebappsCount = verifyTomtTomcatServerWebappsFolderFiles(System.getProperty("tomtom9082serverwebappsdir.value")); //10
	    assertEquals(2, iTom81ServerWebappsCount);
	    int  iTom82WebappsAdminCount = verifyTomtTomcatServerWebappsAdminFolderFiles(System.getProperty("tomtom9082webappsadmindir.value")); //11
	    assertEquals(7, iTom82WebappsAdminCount);
	    int  iTom82WebappsAdminImgCount = verifyTomtTomcatServerWebappsAdminImgFolderFiles(System.getProperty("tomtom9082webappsadminimgdir.value")); //12
	    assertEquals(30, iTom82WebappsAdminImgCount);
	    int  iTom82WebappsAdminResCount = verifyTomtTomcatServerWebappsAdminResFolderFiles(System.getProperty("tomtom9082webappsadminresdir.value")); //13
	    assertEquals(10, iTom82WebappsAdminResCount);
	    int  iTom82WebappsAdminUsersCount = verifyTomtTomcatServerWebappsAdminUsersFolderFiles(System.getProperty("tomtom9082webappsadminusersdir.value")); //14
	    assertEquals(6, iTom82WebappsAdminUsersCount);
	    int  iTom82WebappsAdminWebINFCount = verifyTomtTomcatServerWebappsAdminWebINFFolderFiles(System.getProperty("tomtom9082webappsadminwebinfdir.value")); //15
	    assertEquals(7, iTom82WebappsAdminWebINFCount);
	    int  iTom82WebappsAdminWebINFLibCount = verifyTomtTomcatServerWebappsAdminWebINFLibFolderFiles(System.getProperty("tomtom9082webappsadminwebinflibdir.value")); //16
	    assertEquals(2, iTom82WebappsAdminWebINFLibCount);
	    int  iTom82WebappsManagCount = verifyTomtTomcatServerWebappsMangFolderFiles(System.getProperty("tomtom9082webappsmangdir.value")); //17
	    assertEquals(7, iTom82WebappsManagCount);
	    int  iTom82WebappsManagImgCount = verifyTomtTomcatServerWebappsMangImgFolderFiles(System.getProperty("tomtom9082webappsmangimgdir.value")); //18
	    assertEquals(9, iTom82WebappsManagImgCount);
	    int  iTom82WebappsManagINFCount = verifyTomtTomcatServerWebappsMangINFFolderFiles(System.getProperty("tomtom9082webappsmanginfdir.value")); //19
	    assertEquals(2, iTom82WebappsManagINFCount);
	    int  iTom82WebappsManagINFLibCount = verifyTomtTomcatServerWebappsMangINFLibFolderFiles(System.getProperty("tomtom9082webappsmanginflibdir.value")); //20
	    assertEquals(1, iTom82WebappsManagINFLibCount);	    
	    int  iTom82WebappsCount = verifyTomtTomcatWebappsFolderFiles(System.getProperty("tomtom9082webappsdir.value")); //21
	    assertEquals(4, iTom82WebappsCount);
	    //assertEquals(7, iTom82WebappsCount); //after running the webapps
	    int  iTom82WebappsRootCount = verifyTomtTomcatWebappsRootFolderFiles(System.getProperty("tomtom9082webappsrootdir.value")); //22
	    assertEquals(7, iTom82WebappsRootCount);
	    int  iTom82WebappsRootINFCount = verifyTomtTomcatWebappsRootINFFolderFiles(System.getProperty("tomtom9082webappsrootinfdir.value")); //23
	    assertEquals(2, iTom82WebappsRootINFCount);
	    int  iTom82WebappsRootINFLibCount = verifyTomtTomcatWebappsRootINFLibFolderFiles(System.getProperty("tomtom9082webappsrootinflibdir.value")); //24
	    assertEquals(1, iTom82WebappsRootINFLibCount); 
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  	
  
  //Verify that the 3 folders and 1 file or 5 folders and 1 file exists under /tomcat-sandbox
  public int verifyTomcatSandboxFolderFiles() throws Exception { 
	  	int iTomSandboxFiles = 0;		 
		 String sTomSandboxFolder = null;		 	
		 System.out.println("************************************");
		 sTomSandboxFolder = System.getProperty("tomsandboxdir.value");		 
		 System.out.println("files location="+sTomSandboxFolder);
		 File f = new File(sTomSandboxFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomSandboxFiles = fArray.length;
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
		 return iTomSandboxFiles;	  
  }
  
  //Verify that the 8 files exists under /tomcat-sandbox/bin folder
  public int verifyTomcatSandboxBinFolderFiles() throws Exception { 
	  	 int iTomcatSandboxBinFiles = 0;		 
		 String sTomcatSandboxBinFolder = null;		 	
		 System.out.println("************************************");
		 sTomcatSandboxBinFolder = System.getProperty("tomsandboxbindir.value");		 
		 System.out.println("files location="+sTomcatSandboxBinFolder);
		 File f = new File(sTomcatSandboxBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomcatSandboxBinFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+ i + " " + fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iTomcatSandboxBinFiles;	  
  }  
  //Verify that the 5 folders and 3 files exists under /tomcat-sandbox/tomcat-9081 folder
  //Verify that the 5 folders and 3 files exists under /tomcat-sandbox/tomcat-9082 folder
  public int verifyTomtTomcatFolderFiles(String s) throws Exception {
		 int iTomtTomcatFiles = 0;		 
		 String sTomtTomcatFolder = s;		 	
		 System.out.println("************************************");
		 //sTomtTomt81Folder = System.getProperty("tomtom9081dir.value");		 
		 System.out.println("files location="+sTomtTomcatFolder);
		 File f = new File(sTomtTomcatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatFiles = fArray.length;
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
		 return iTomtTomcatFiles;		 	  
  }
  //Verify that the 23 files exists under /tomcat-sandbox/tomcat-9081/bin folder
  //Verify that the 23 files exists under /tomcat-sandbox/tomcat-9082/bin folder
  public int verifyTomtTomcatBinFolderFiles(String s) throws Exception {
		 int iTomtTomcatBinFiles = 0;		 
		 String sTomtTomcatBinFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatBinFolder);
		 File f = new File(sTomtTomcatBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatBinFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatBinFiles;		 	  
  }
  //Verify that the 2 folders exists under /tomcat-sandbox/tomcat-9081/common folder
  //Verify that the 2 folders exists under /tomcat-sandbox/tomcat-9082/common folder
  public int verifyTomtTomcatCommonFolder(String s) throws Exception {
		 int iTomtTomcatCommonFiles = 0;		 
		 String sTomtTomcatCommonFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatCommonFolder);
		 File f = new File(sTomtTomcatCommonFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatCommonFiles = fArray.length;
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
		 return iTomtTomcatCommonFiles;		 	  
  }
  //Verify that the 2 files exists under /tomcat-sandbox/tomcat-9081/common/endorsed folder
  //Verify that the 2 files exists under /tomcat-sandbox/tomcat-9082/common/endorsed folder
  public int verifyTomtTomcatCommonEndorseFolderFiles(String s) throws Exception {
		 int iTomtTomcatCommonEndorseFiles = 0;		 
		 String sTomtTomcatCommonEndorseFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatCommonEndorseFolder);
		 File f = new File(sTomtTomcatCommonEndorseFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatCommonEndorseFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				 
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatCommonEndorseFiles;		 	  
  }
  //Verify that the 14 files exists under /tomcat-sandbox/tomcat-9081/common/lib folder
  //Verify that the 14 files exists under /tomcat-sandbox/tomcat-9082/common/lib folder
  public int verifyTomtTomcatCommonLibFolderFiles(String s) throws Exception {
		 int iTomtTomcatCommonLibFiles = 0;		 
		 String sTomtTomcatCommonLibFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatCommonLibFolder);
		 File f = new File(sTomtTomcatCommonLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatCommonLibFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				 
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatCommonLibFiles;		 	  
  }
  //Verify that the 1 folder and 7 files exists under /tomcat-sandbox/tomcat-9081/conf folder
  //Verify that the 1 folder and 7 files exists under /tomcat-sandbox/tomcat-9082/conf folder
  public int verifyTomtTomcatConfFolderFiles(String s) throws Exception {
		 int iTomtTomcatConfFiles = 0;		 
		 String sTomtTomcatConfFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatConfFolder);
		 File f = new File(sTomtTomcatConfFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatConfFiles = fArray.length;
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
		 return iTomtTomcatConfFiles;		 	  
  }
  //Verify that the 2 files exists under /tomcat-sandbox/tomcat-9081/conf/Catalina/localhost folder
  //Verify that the 2 files exists under /tomcat-sandbox/tomcat-9082/conf/Catalina/localhost folder
  public int verifyTomtTomcatConfSubFolderFiles(String s) throws Exception {
		 int iTomtTomcatConfSubFiles = 0;		 
		 String sTomtTomcatConfSubFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatConfSubFolder);
		 File f = new File(sTomtTomcatConfSubFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatConfSubFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				 
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatConfSubFiles;		 	  
  }
  //Verify that the 2 folders exists under /tomcat-sandbox/tomcat-9081/server folder
  //Verify that the 2 folders exists under /tomcat-sandbox/tomcat-9082/server folder
  public int verifyTomtTomcatServerFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerFiles = 0;		 
		 String sTomtTomcatServerFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerFolder);
		 File f = new File(sTomtTomcatServerFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerFiles = fArray.length;
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
		 return iTomtTomcatServerFiles;		 	  
  }
  //Verify that the 26 files exists under /tomcat-sandbox/tomcat-9081/server/lib folder
  //Verify that the 26 files exists under /tomcat-sandbox/tomcat-9082/server/lib folder
  public int verifyTomtTomcatServerLibFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerLibFiles = 0;		 
		 String sTomtTomcatServerLibFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerLibFolder);
		 File f = new File(sTomtTomcatServerLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerLibFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatServerLibFiles;		 	  
  }  
  //Verify that the 2 folders exists under /tomcat-sandbox/tomcat-9081/server/webapps folder
  //Verify that the 2 folders exists under /tomcat-sandbox/tomcat-9082/server/webapps folder
  public int verifyTomtTomcatServerWebappsFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsFiles = 0;		 
		 String sTomtTomcatServerWebappsFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsFolder);
		 File f = new File(sTomtTomcatServerWebappsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsFiles = fArray.length;
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
		 return iTomtTomcatServerWebappsFiles;		 	  
  }
  //Verify that the 4 folders and 3 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/admin folder
  //Verify that the 4 folders and 3 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/admin folder
  public int verifyTomtTomcatServerWebappsAdminFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsAdminFiles = 0;		 
		 String sTomtTomcatServerWebappsAdminFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsAdminFolder);
		 File f = new File(sTomtTomcatServerWebappsAdminFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsAdminFiles = fArray.length;
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
		 return iTomtTomcatServerWebappsAdminFiles;		 	  
  }
  //Verify that the 30 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/admin/images folder
  //Verify that the 30 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/admin/images folder
  public int verifyTomtTomcatServerWebappsAdminImgFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsAdminImgFiles = 0;		 
		 String sTomtTomcatServerWebappsAdminImgFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsAdminImgFolder);
		 File f = new File(sTomtTomcatServerWebappsAdminImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsAdminImgFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());			
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatServerWebappsAdminImgFiles;		 	  
  }
  //Verify that the 10 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/admin/resources folder
  //Verify that the 10 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/admin/resources folder
  public int verifyTomtTomcatServerWebappsAdminResFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsAdminResFiles = 0;		 
		 String sTomtTomcatServerWebappsAdminResFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsAdminResFolder);
		 File f = new File(sTomtTomcatServerWebappsAdminResFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsAdminResFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());			
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatServerWebappsAdminResFiles;		 	  
  }
  //Verify that the 6 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/admin/users folder
  //Verify that the 6 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/admin/users folder
  public int verifyTomtTomcatServerWebappsAdminUsersFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsAdminUsersFiles = 0;		 
		 String sTomtTomcatServerWebappsAdminUsersFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsAdminUsersFolder);
		 File f = new File(sTomtTomcatServerWebappsAdminUsersFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsAdminUsersFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());			
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatServerWebappsAdminUsersFiles;		 	  
  }
  //Verify that the 1 folder and 6 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/admin/WEB-INF folder
  //Verify that the 1 folder and 6 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/admin/WEB-INF folder
  public int verifyTomtTomcatServerWebappsAdminWebINFFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsAdminWebINFFiles = 0;		 
		 String sTomtTomcatServerWebappsAdminWebINFFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsAdminWebINFFolder);
		 File f = new File(sTomtTomcatServerWebappsAdminWebINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsAdminWebINFFiles = fArray.length;
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
		 return iTomtTomcatServerWebappsAdminWebINFFiles;		 	  
  }
  //Verify that the 2 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/admin/WEB-INF/lib folder
  //Verify that the 2 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/admin/WEB-INF/lib folder
  public int verifyTomtTomcatServerWebappsAdminWebINFLibFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsAdminWebINFLibFiles = 0;		 
		 String sTomtTomcatServerWebappsAdminWebINFLibFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsAdminWebINFLibFolder);
		 File f = new File(sTomtTomcatServerWebappsAdminWebINFLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsAdminWebINFLibFiles = fArray.length;
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
		 return iTomtTomcatServerWebappsAdminWebINFLibFiles;		 	  
  }
  //Verify that the 2 folders and 5 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/manager folder
  //Verify that the 2 folders and 5 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/manager folder
  public int verifyTomtTomcatServerWebappsMangFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsMangFiles = 0;		 
		 String sTomtTomcatServerWebappsMangFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsMangFolder);
		 File f = new File(sTomtTomcatServerWebappsMangFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsMangFiles = fArray.length;
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
		 return iTomtTomcatServerWebappsMangFiles;		 	  
  }
  //Verify that the 9 files exists under /tomcat-sandbox/tomcat-9081/server/webapps/manager/images folder
  //Verify that the 9 files exists under /tomcat-sandbox/tomcat-9082/server/webapps/manager/images folder
  public int verifyTomtTomcatServerWebappsMangImgFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsMangImgFiles = 0;		 
		 String sTomtTomcatServerWebappsMangImgFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsMangImgFolder);
		 File f = new File(sTomtTomcatServerWebappsMangImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsMangImgFiles = fArray.length;
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
		 return iTomtTomcatServerWebappsMangImgFiles;		 	  
  }
  //Verify that the 1 folder and 1 file exists under /tomcat-sandbox/tomcat-9081/server/webapps/manager/WEB-INF folder
  //Verify that the 1 folder and 1 file exists under /tomcat-sandbox/tomcat-9082/server/webapps/manager/WEB-INF folder
  public int verifyTomtTomcatServerWebappsMangINFFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsMangINFFiles = 0;		 
		 String sTomtTomcatServerWebappsMangINFFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsMangINFFolder);
		 File f = new File(sTomtTomcatServerWebappsMangINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsMangINFFiles = fArray.length;
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
		 return iTomtTomcatServerWebappsMangINFFiles;		 	  
  }
  //Verify that the 1 file exists under /tomcat-sandbox/tomcat-9081/server/webapps/manager/WEB-INF/lib folder
  //Verify that the 1 file exists under /tomcat-sandbox/tomcat-9082/server/webapps/manager/WEB-INF/lib folder
  public int verifyTomtTomcatServerWebappsMangINFLibFolderFiles(String s) throws Exception {
		 int iTomtTomcatServerWebappsMangINFLibFiles = 0;		 
		 String sTomtTomcatServerWebappsMangINFLibFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatServerWebappsMangINFLibFolder);
		 File f = new File(sTomtTomcatServerWebappsMangINFLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatServerWebappsMangINFLibFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatServerWebappsMangINFLibFiles;		 	  
  }
  //Verify that the 1 folder and 3 files exists under /tomcat-sandbox/tomcat-9081/webapps folder
  //Verify that the 1 folder and 3 files exists under /tomcat-sandbox/tomcat-9082/webapps folder
  public int verifyTomtTomcatWebappsFolderFiles(String s) throws Exception {
		 int iTomtTomcatWebappsFiles = 0;		 
		 String sTomtTomcatWebappsFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatWebappsFolder);
		 File f = new File(sTomtTomcatWebappsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatWebappsFiles = fArray.length;
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
		 return iTomtTomcatWebappsFiles;		 	  
  }
  //Verify that the 1 folder and 6 files exists under /tomcat-sandbox/tomcat-9081/webapps/ROOT folder
  //Verify that the 1 folder and 6 files exists under /tomcat-sandbox/tomcat-9082/webapps/ROOT folder
  public int verifyTomtTomcatWebappsRootFolderFiles(String s) throws Exception {
		 int iTomtTomcatWebappsRootFiles = 0;		 
		 String sTomtTomcatWebappsRootFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatWebappsRootFolder);
		 File f = new File(sTomtTomcatWebappsRootFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatWebappsRootFiles = fArray.length;
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
		 return iTomtTomcatWebappsRootFiles;		 	  
  }
  //Verify that the 1 folder and 1 file exists under /tomcat-sandbox/tomcat-9081/webapps/ROOT/WEB-INF folder
  //Verify that the 1 folder and 1 file exists under /tomcat-sandbox/tomcat-9082/webapps/ROOT/WEB-INF folder
  public int verifyTomtTomcatWebappsRootINFFolderFiles(String s) throws Exception {
		 int iTomtTomcatWebappsRootINFFiles = 0;		 
		 String sTomtTomcatWebappsRootINFFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatWebappsRootINFFolder);
		 File f = new File(sTomtTomcatWebappsRootINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatWebappsRootINFFiles = fArray.length;
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
		 return iTomtTomcatWebappsRootINFFiles;		 	  
  }
  //Verify that the 1 file exists under /tomcat-sandbox/tomcat-9081/webapps/ROOT/WEB-INF/lib folder
  //Verify that the 1 file exists under /tomcat-sandbox/tomcat-9082/webapps/ROOT/WEB-INF/lib folder
  public int verifyTomtTomcatWebappsRootINFLibFolderFiles(String s) throws Exception {
		 int iTomtTomcatWebappsRootINFLibFiles = 0;		 
		 String sTomtTomcatWebappsRootINFLibFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtTomcatWebappsRootINFLibFolder);
		 File f = new File(sTomtTomcatWebappsRootINFLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtTomcatWebappsRootINFLibFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iTomtTomcatWebappsRootINFLibFiles;		 	  
  }  
}  
  


  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
