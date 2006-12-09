/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SandBoxVendorTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SandBoxVendorTest.class);
  }
  
  public void testForVerification() throws Exception {	    	    
	    int  iCount = verifySandboxFolderFiles(); //1
	    assertEquals(5, iCount); 	 
	    int  iBinCount = verifySandboxBinFolderFiles(); //2
	    assertEquals(4, iBinCount);
        //tomcat5.0 folder
	    int  iTom50Count = verifySandboxTomFolderFiles(System.getProperty("sandboxtom50dir.value")); //3
	    assertEquals(6, iTom50Count);	    	    
	    //9081 folder
	    int  iTom5081Count = verifyTomtFolderFiles(System.getProperty("sandboxtom509081dir.value"));//4	    		
	    assertEquals(3, iTom5081Count); 
	    int  iTom5081ConfCount = verifyTomtConfFolderFiles(System.getProperty("sandboxtom509081confdir.value")); //5
	    assertEquals(8, iTom5081ConfCount);
	    int  iTom5081ConfCatCount = verifyTomtConfCatFolderFiles(System.getProperty("sandboxtom509081confcatdir.value")); //6
	    assertEquals(1, iTom5081ConfCatCount);
	    int  iTom5081ConfCatLocalCount = verifyTomtConfCatLocalFolderFiles(System.getProperty("sandboxtom509081confcatlocaldir.value")); //7
	    assertEquals(3, iTom5081ConfCatLocalCount);
	    int  iTom5081WebappsCount = verifyTomtWebappsFolderFiles(System.getProperty("sandboxtom509081webappsdir.value")); //8
	    assertEquals(3, iTom5081WebappsCount);
	    //9082 folder
	    int  iTom5082Count = verifyTomtFolderFiles(System.getProperty("sandboxtom509082dir.value"));  		
	    assertEquals(3, iTom5082Count); 
	    int  iTom5082ConfCount = verifyTomtConfFolderFiles(System.getProperty("sandboxtom509082confdir.value")); 
	    assertEquals(8, iTom5082ConfCount);
	    int  iTom5082ConfCatCount = verifyTomtConfCatFolderFiles(System.getProperty("sandboxtom509082confcatdir.value")); 
	    assertEquals(1, iTom5082ConfCatCount);
	    int  iTom5082ConfCatLocalCount = verifyTomtConfCatLocalFolderFiles(System.getProperty("sandboxtom509082confcatlocaldir.value")); 
	    assertEquals(3, iTom5082ConfCatLocalCount);
	    int  iTom5082WebappsCount = verifyTomtWebappsFolderFiles(System.getProperty("sandboxtom509082webappsdir.value")); 
	    assertEquals(3, iTom5082WebappsCount);	    
	    // tomcat5.5 folder
	    int  iTom55Count = verifySandboxTomFolderFiles(System.getProperty("sandboxtom55dir.value")); 
	    assertEquals(6, iTom55Count);	    	    
	    //9081 folder
	    int  iTom5581Count = verifyTomtFolderFiles(System.getProperty("sandboxtom559081dir.value"));	    		
	    assertEquals(3, iTom5581Count); 
	    int  iTom5581ConfCount = verifyTomtConfFolderFiles(System.getProperty("sandboxtom559081confdir.value")); 
	    assertEquals(9, iTom5581ConfCount);
	    int  iTom5581ConfCatCount = verifyTomtConfCatFolderFiles(System.getProperty("sandboxtom559081confcatdir.value")); 
	    assertEquals(1, iTom5581ConfCatCount);
	    int  iTom5581ConfCatLocalCount = verifyTomtConfCatLocalFolderFiles(System.getProperty("sandboxtom559081confcatlocaldir.value")); 
	    assertEquals(2, iTom5581ConfCatLocalCount);
	    int  iTom5581WebappsCount = verifyTomtWebappsFolderFiles(System.getProperty("sandboxtom559081webappsdir.value")); 
	    assertEquals(3, iTom5581WebappsCount);
	    //9082 folder
	    int  iTom5582Count = verifyTomtFolderFiles(System.getProperty("sandboxtom559082dir.value"));	    		
	    assertEquals(3, iTom5582Count); 
	    int  iTom5582ConfCount = verifyTomtConfFolderFiles(System.getProperty("sandboxtom559082confdir.value")); 
	    assertEquals(9, iTom5582ConfCount);
	    int  iTom5582ConfCatCount = verifyTomtConfCatFolderFiles(System.getProperty("sandboxtom559082confcatdir.value")); 
	    assertEquals(1, iTom5582ConfCatCount);
	    int  iTom5582ConfCatLocalCount = verifyTomtConfCatLocalFolderFiles(System.getProperty("sandboxtom559082confcatlocaldir.value")); 
	    assertEquals(2, iTom5582ConfCatLocalCount);
	    int  iTom5582WebappsCount = verifyTomtWebappsFolderFiles(System.getProperty("sandboxtom559082webappsdir.value")); 
	    assertEquals(3, iTom5582WebappsCount);
	    //wls8.1 folder
	    int  iwlsCount = verifySandboxWlsFolderFiles(); //9
	    assertEquals(7, iwlsCount);
	    //9081
	    int  iWls9081Count = verifySandboxWls90FolderFiles(System.getProperty("sandboxwls9081dir.value")); //10
	    assertEquals(4, iWls9081Count);
	    int  iWls9081ApplnCount = verifySandboxWls90ApplnFolderFiles(System.getProperty("sandboxwls9081Appldir.value")); //11
	    assertEquals(3, iWls9081ApplnCount);
	    int  iWls9081TmplCount = verifySandboxWls90TmplFolderFiles(System.getProperty("sandboxwls9081tmplsdir.value")); //12
	    assertEquals(3, iWls9081TmplCount);
        //9082
	    int  iWls9082Count = verifySandboxWls90FolderFiles(System.getProperty("sandboxwls9082dir.value")); 
	    assertEquals(4, iWls9082Count);
	    int  iWls9082ApplnCount = verifySandboxWls90ApplnFolderFiles(System.getProperty("sandboxwls9082Appldir.value")); 
	    assertEquals(3, iWls9082ApplnCount);
	    int  iWls9082TmplCount = verifySandboxWls90TmplFolderFiles(System.getProperty("sandboxwls9082tmplsdir.value")); 
	    assertEquals(3, iWls9082TmplCount);
	    //Vendor folder
	    int  iVendorCount = verifyVendorFolderFiles(); //13
	    assertEquals(1, iVendorCount);
	    int  iVendorTomtCount = verifyVendorTomtFolderFiles(); //14
	    assertEquals(9, iVendorTomtCount);
	    int  iVendorTomtBinCount = verifyVendorTomtBinFolderFiles(); //15
	    assertEquals(27, iVendorTomtBinCount);
	    int  iVendorTomtComCount = verifyVendorTomtComFolderFiles(); //16
	    assertEquals(2, iVendorTomtComCount);
	    int  iVendorTomtComi18nCount = verifyVendorTomtComi18nFolderFiles(); //17
	    assertEquals(4, iVendorTomtComi18nCount);
	    int  iVendorTomtComLibCount = verifyVendorTomtComLibFolderFiles(); //18
	    assertEquals(9, iVendorTomtComLibCount);
	    
	    int  iVendorTomtConfCount = verifyTomtConfFolderFiles(System.getProperty("vendortomtconfdir.value")); 
	    assertEquals(9, iVendorTomtConfCount);
	    int  iVendorTomtConfCatCount = verifyTomtConfCatFolderFiles(System.getProperty("vendortomtconfcatdir.value")); 
	    assertEquals(1, iVendorTomtConfCatCount);
	    int  iVendorTomtConfCatLocalCount = verifyTomtConfCatLocalFolderFiles(System.getProperty("vendortomtconfcatlocaldir.value")); 
	    assertEquals(2, iVendorTomtConfCatLocalCount);	    
	    
	    int  iVendorTomtServerCount = verifyVenTomtServerFolderFiles(); //19
	    assertEquals(2, iVendorTomtServerCount);
	    int  iVendorTomtServerLibCount = verifyVenTomtServerLibFolderFiles(); //20
	    assertEquals(18, iVendorTomtServerLibCount); 	    
	    int  iVenTomtServerWebappsCount = verifyVenTomtServerWebappsFolderFiles(); //21
	    assertEquals(2, iVenTomtServerWebappsCount);
	    int  iVenTomtServerWebappsHostManCount = verifyVenTomtServerWebappsHostManFolderFiles(); //22
	    assertEquals(4, iVenTomtServerWebappsHostManCount);	 	    
	    int  iVenTomtServerWebappsHostManImgCount = verifyVenTomtServerWebappsHostManImgFolderFiles(); //23
	    assertEquals(9, iVenTomtServerWebappsHostManImgCount);	    
	    int  iVenTomtServerWebappsHostManWebINFCount = verifyVenTomtServerWebappsHostManWebINFFolderFiles(); //24
	    assertEquals(2, iVenTomtServerWebappsHostManWebINFCount);
	    int  iVenTomtServerWebappsHostManWebINFLibCount = verifyVenTomtServerWebappsHostManWebINFLibFolderFiles(); //25
	    assertEquals(1, iVenTomtServerWebappsHostManWebINFLibCount);	    
	    int  iVenTomtServerWebappsMangCount = verifyVenTomtServerWebappsMangFolderFiles(); //26
	    assertEquals(7, iVenTomtServerWebappsMangCount);	 	    
	    int  iVenTomtServerWebappsMangImgCount = verifyVenTomtServerWebappsMangImgFolderFiles(); //27
	    assertEquals(9, iVenTomtServerWebappsMangImgCount);	    
	    int  iVenTomtServerWebappsMangWebINFCount = verifyVenTomtServerWebappsMangWebINFFolderFiles(); //28
	    assertEquals(2, iVenTomtServerWebappsMangWebINFCount);
	    int  iVenTomtServerWebappsMangWebINFLibCount = verifyVenTomtServerWebappsMangWebINFLibFolderFiles(); //29
	    assertEquals(2, iVenTomtServerWebappsMangWebINFLibCount);
	    
	    int  iVenTomtWebappsCount = verifyVenTomtWebappsFolderFiles(); //30
	    assertEquals(1, iVenTomtWebappsCount);
	    int  iVenTomtWebappsRootCount = verifyVenTomtWebappsRootFolderFiles(); //31
	    assertEquals(8, iVenTomtWebappsRootCount);
	    int  iVenTomtWebappsRootAdminCount = verifyVenTomtWebappsRootAdminFolderFiles(); //32
	    assertEquals(1, iVenTomtWebappsRootAdminCount);
	    int  iVenTomtWebappsRootINFCount = verifyVenTomtWebappsRootINFFolderFiles(); //33
	    assertEquals(2, iVenTomtWebappsRootINFCount);
	    int  iVenTomtWebappsRootINFLibCount = verifyVenTomtWebappsRootINFLibFolderFiles(); //34
	    assertEquals(1, iVenTomtWebappsRootINFLibCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  	
  
  //1. Verify that the 4 folders and 1 file exists under terracotta-2.1.0\sessions\sandbox
  public int verifySandboxFolderFiles() throws Exception { 
	  	int iSandboxFiles = 0;		 
		 String sSandboxFolder = null;		 	
		 System.out.println("************************************");
		 sSandboxFolder = System.getProperty("sandboxdir.value");		 
		 System.out.println("files location="+sSandboxFolder);
		 File f = new File(sSandboxFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSandboxFiles = fArray.length;
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
		 return iSandboxFiles;	  
  }  
  //2. Verify that the 4 files exists under terracotta-2.1.0\sessions\sandbox\bin folder
  public int verifySandboxBinFolderFiles() throws Exception { 
	  	 int iSandboxBinFiles = 0;		 
		 String sSandboxBinFolder = null;		 	
		 System.out.println("************************************");
		 sSandboxBinFolder = System.getProperty("sandboxbindir.value");		 
		 System.out.println("files location="+sSandboxBinFolder);
		 File f = new File(sSandboxBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSandboxBinFiles = fArray.length;
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
		 return iSandboxBinFiles;	  
  }   
  //3. Verify that the 2 folders and 4 files exists under terracotta-2.1.0\sessions\sandbox\tomcat5.0
  public int verifySandboxTomFolderFiles(String s) throws Exception { 
	  	 int iSandboxTomFiles = 0;		 
		 String sSandboxTomFolder = s;		 	
		 System.out.println("************************************");			 
		 System.out.println("files location="+sSandboxTomFolder);
		 File f = new File(sSandboxTomFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSandboxTomFiles = fArray.length;
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
		 return iSandboxTomFiles;	  
  } 
  //4. Verify that the 3 folders exists under terracotta-2.1.0\sessions\sandbox\tomcat5.0\9081  
  public int verifyTomtFolderFiles(String s) throws Exception {
		 int iTomtFiles = 0;		 
		 String sTomtFolder = s;		 	
		 System.out.println("************************************");
		 //sTomtTomt81Folder = System.getProperty("tomtom9081dir.value");		 
		 System.out.println("files location="+sTomtFolder);
		 File f = new File(sTomtFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtFiles = fArray.length;
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
		 return iTomtFiles;		 	  
  }
  //5. Verify that the 1 folder and 7 files exists under terracotta-2.1.0\sessions\sandbox\tomcat5.0\9081\conf folder  
  public int verifyTomtConfFolderFiles(String s) throws Exception {
		 int iTomtConfFiles = 0;		 
		 String sTomtConfFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtConfFolder);
		 File f = new File(sTomtConfFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtConfFiles = fArray.length;
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
		 return iTomtConfFiles;		 	  
  }
  //6. Verify that the 1 folder exists under terracotta-2.1.0\sessions\sandbox\tomcat5.0\9081\conf\Catalina folder  
  public int verifyTomtConfCatFolderFiles(String s) throws Exception {
		 int iTomtConfCatFiles = 0;		 
		 String sTomtConfCatFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtConfCatFolder);
		 File f = new File(sTomtConfCatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtConfCatFiles = fArray.length;
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
		 return iTomtConfCatFiles;		 	  
  }
  //7. Verify that the 3 files exists under terracotta-2.1.0\sessions\sandbox\tomcat5.0\9081\conf\Catalina\localhost  
  public int verifyTomtConfCatLocalFolderFiles(String s) throws Exception {
		 int iTomtConfCatLocalFiles = 0;		 
		 String sTomtConfCatLocalFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtConfCatLocalFolder);
		 File f = new File(sTomtConfCatLocalFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtConfCatLocalFiles = fArray.length;
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
		 return iTomtConfCatLocalFiles;		 	  
  }
  //8. Verify that the 3 files exists under terracotta-2.1.0\sessions\sandbox\tomcat5.0\9081\webapps  
  public int verifyTomtWebappsFolderFiles(String s) throws Exception {
		 int iTomtWebappsFiles = 0;		 
		 String sTomtWebappsFolder = s;		 	
		 System.out.println("************************************");		 	 
		 System.out.println("files location="+sTomtWebappsFolder);
		 File f = new File(sTomtWebappsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iTomtWebappsFiles = fArray.length;
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
		 return iTomtWebappsFiles;		 	  
  }  
  //9. Verify that the 2 folders and 5 files exists under terracotta-2.1.0\sessions\sandbox\wls8.1
  public int verifySandboxWlsFolderFiles() throws Exception {
		 int iSandboxWlsFiles = 0;		 
		 String sSandboxWlsFolder = null;		 	
		 System.out.println("************************************");
		 sSandboxWlsFolder = System.getProperty("sandboxwlsdir.value"); 
		 System.out.println("files location="+sSandboxWlsFolder);
		 File f = new File(sSandboxWlsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSandboxWlsFiles = fArray.length;
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
		 return iSandboxWlsFiles;		 	  
  }
  //10. Verify that the 2 folders and 2 files exists under terracotta-2.1.0\sessions\sandbox\wls8.1\9081
  public int verifySandboxWls90FolderFiles(String s) throws Exception {
		 int iSandboxWls90Files = 0;		 
		 String sSandboxWls90Folder = s;		 	
		 System.out.println("************************************");		  
		 System.out.println("files location="+sSandboxWls90Folder);
		 File f = new File(sSandboxWls90Folder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSandboxWls90Files = fArray.length;
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
		 return iSandboxWls90Files;		 	  
  }  
  //11. Verify that the 3 files exists under terracotta-2.1.0\sessions\sandbox\wls8.1\9081\applications
  public int verifySandboxWls90ApplnFolderFiles(String s) throws Exception {
		 int iSandboxWls90ApplnFiles = 0;		 
		 String sSandboxWls90ApplnFolder = s;		 	
		 System.out.println("************************************");		  
		 System.out.println("files location="+sSandboxWls90ApplnFolder);
		 File f = new File(sSandboxWls90ApplnFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSandboxWls90ApplnFiles = fArray.length;
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
		 return iSandboxWls90ApplnFiles;		 	  
  }
  //12. Verify that the 3 files exists under terracotta-2.1.0\sessions\sandbox\wls8.1\9081\tmpls
  public int verifySandboxWls90TmplFolderFiles(String s) throws Exception {
		 int iSandboxWls90TmplFiles = 0;		 
		 String sSandboxWls90TmplFolder = s;		 	
		 System.out.println("************************************");		  
		 System.out.println("files location="+sSandboxWls90TmplFolder);
		 File f = new File(sSandboxWls90TmplFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSandboxWls90TmplFiles = fArray.length;
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
		 return iSandboxWls90TmplFiles;		 	  
  }
  //13. Verify that the 1 folder exists under terracotta-2.1.0\sessions\vendor
  public int verifyVendorFolderFiles() throws Exception {
		 int iVendorFiles = 0;		 
		 String sVendorFolder = null;		 	
		 System.out.println("************************************");		 
		 sVendorFolder = System.getProperty("vendordir.value");
		 System.out.println("files location="+sVendorFolder);
		 File f = new File(sVendorFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVendorFiles = fArray.length;
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
		 return iVendorFiles;		 	  
  }
  //14. Verify that the 5 folders and 4 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5
  public int verifyVendorTomtFolderFiles() throws Exception {
		 int iVendorTomtFiles = 0;		 
		 String sVendorTomtFolder = null;		 	
		 System.out.println("************************************");		 
		 sVendorTomtFolder = System.getProperty("vendortomtdir.value");
		 System.out.println("files location="+sVendorTomtFolder);
		 File f = new File(sVendorTomtFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVendorTomtFiles = fArray.length;
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
		 return iVendorTomtFiles;		 	  
  }
 //15. Verify that the 27 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\bin
  public int verifyVendorTomtBinFolderFiles() throws Exception {
		 int iVendorTomtBinFiles = 0;		 
		 String sVendorTomtBinFolder = null;		 	
		 System.out.println("************************************");		 
		 sVendorTomtBinFolder = System.getProperty("vendortomtbindir.value");
		 System.out.println("files location="+sVendorTomtBinFolder);
		 File f = new File(sVendorTomtBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVendorTomtBinFiles = fArray.length;
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
		 return iVendorTomtBinFiles;		 	  
  }
 //16. Verify that the 2 folders exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\common
  public int verifyVendorTomtComFolderFiles() throws Exception {
		 int iVendorTomtComFiles = 0;		 
		 String sVendorTomtComFolder = null;		 	
		 System.out.println("************************************");		 
		 sVendorTomtComFolder = System.getProperty("vendortomtcomdir.value");
		 System.out.println("files location="+sVendorTomtComFolder);
		 File f = new File(sVendorTomtComFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVendorTomtComFiles = fArray.length;
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
		 return iVendorTomtComFiles;		 	  
  }
 //17. Verify that the 4 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\common\i18n
  public int verifyVendorTomtComi18nFolderFiles() throws Exception {
		 int iVendorTomtComi18nFiles = 0;		 
		 String sVendorTomtComi18nFolder = null;		 	
		 System.out.println("************************************");		 
		 sVendorTomtComi18nFolder = System.getProperty("vendortomtcomi18ndir.value");
		 System.out.println("files location="+sVendorTomtComi18nFolder);
		 File f = new File(sVendorTomtComi18nFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVendorTomtComi18nFiles = fArray.length;
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
		 return iVendorTomtComi18nFiles;		 	  
  }
  //18. Verify that the 9 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\common\lib
  public int verifyVendorTomtComLibFolderFiles() throws Exception {
		 int iVendorTomtComLibFiles = 0;		 
		 String sVendorTomtComLibFolder = null;		 	
		 System.out.println("************************************");		 
		 sVendorTomtComLibFolder = System.getProperty("vendortomtcomlibdir.value");
		 System.out.println("files location="+sVendorTomtComLibFolder);
		 File f = new File(sVendorTomtComLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVendorTomtComLibFiles = fArray.length;
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
		 return iVendorTomtComLibFiles;		 	  
  }    
  //19. Verify that the 2 folders exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server  
  public int verifyVenTomtServerFolderFiles() throws Exception {
		 int iVenTomtServerFiles = 0;		 
		 String sVenTomtServerFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerFolder = System.getProperty("vendortomtserverdir.value");
		 System.out.println("files location="+sVenTomtServerFolder);
		 File f = new File(sVenTomtServerFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerFiles = fArray.length;
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
		 return iVenTomtServerFiles;		 	  
  }
  //20. Verify that the 18 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\lib  
  public int verifyVenTomtServerLibFolderFiles() throws Exception {
		 int iVenTomtServerLibFiles = 0;		 
		 String sVenTomtServerLibFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerLibFolder = System.getProperty("vendortomtserverlibdir.value");
		 System.out.println("files location="+sVenTomtServerLibFolder);
		 File f = new File(sVenTomtServerLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerLibFiles = fArray.length;
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
		 return iVenTomtServerLibFiles;		 	  
  }   
  //21. Verify that the 2 folders exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps
   public int verifyVenTomtServerWebappsFolderFiles() throws Exception {
		 int iVenTomtServerWebappsFiles = 0;		 
		 String sVenTomtServerWebappsFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsFolder = System.getProperty("vendortomtserverwebappsdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsFolder);
		 File f = new File(sVenTomtServerWebappsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsFiles = fArray.length;
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
		 return iVenTomtServerWebappsFiles;		 	  
  }
  //22. Verify that the 2 folders and 2 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\host-manager folder  
  public int verifyVenTomtServerWebappsHostManFolderFiles() throws Exception {
		 int iVenTomtServerWebappsHostManFiles = 0;		 
		 String sVenTomtServerWebappsHostManFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsHostManFolder = System.getProperty("vendortomtserverwebhostmandir.value");
		 System.out.println("files location="+sVenTomtServerWebappsHostManFolder);
		 File f = new File(sVenTomtServerWebappsHostManFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsHostManFiles = fArray.length;
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
		 return iVenTomtServerWebappsHostManFiles;		 	  
  }
  //23. Verify that the 9 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\host-manager\images folder  
  public int verifyVenTomtServerWebappsHostManImgFolderFiles() throws Exception {
		 int iVenTomtServerWebappsHostManImgFiles = 0;		 
		 String sVenTomtServerWebappsHostManImgFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsHostManImgFolder = System.getProperty("vendortomtserverwebhostmanimgdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsHostManImgFolder);
		 File f = new File(sVenTomtServerWebappsHostManImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsHostManImgFiles = fArray.length;
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
		 return iVenTomtServerWebappsHostManImgFiles;		 	  
  }
  //24. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\host-manager\WEB-INF folder  
  public int verifyVenTomtServerWebappsHostManWebINFFolderFiles() throws Exception {
		 int iVenTomtServerWebappsHostManWebINFFiles = 0;		 
		 String sVenTomtServerWebappsHostManWebINFFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsHostManWebINFFolder = System.getProperty("vendortomtserverwebhostmanINFdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsHostManWebINFFolder);
		 File f = new File(sVenTomtServerWebappsHostManWebINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsHostManWebINFFiles = fArray.length;
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
		 return iVenTomtServerWebappsHostManWebINFFiles;		 	  
  }
  //25. Verify that the 1 file exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\host-manager\WEB-INF\lib folder
  public int verifyVenTomtServerWebappsHostManWebINFLibFolderFiles() throws Exception {
		 int iVenTomtServerWebappsHostManWebINFLibFiles = 0;		 
		 String sVenTomtServerWebappsHostManWebINFLibFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsHostManWebINFLibFolder = System.getProperty("vendortomtserverwebhostmanINFlibdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsHostManWebINFLibFolder);
		 File f = new File(sVenTomtServerWebappsHostManWebINFLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsHostManWebINFLibFiles = fArray.length;
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
		 return iVenTomtServerWebappsHostManWebINFLibFiles;		 	  
  } 
  //26. Verify that the 2 folders and 5 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\manager folder
   public int verifyVenTomtServerWebappsMangFolderFiles() throws Exception {
		 int iVenTomtServerWebappsMangFiles = 0;		 
		 String sVenTomtServerWebappsMangFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsMangFolder = System.getProperty("vendortomtserverwebmangdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsMangFolder);
		 File f = new File(sVenTomtServerWebappsMangFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsMangFiles = fArray.length;
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
		 return iVenTomtServerWebappsMangFiles;		 	  
  }
  //27. Verify that the 9 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\manager\images folder
  public int verifyVenTomtServerWebappsMangImgFolderFiles() throws Exception {
		 int iVenTomtServerWebappsMangImgFiles = 0;		 
		 String sVenTomtServerWebappsMangImgFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsMangImgFolder = System.getProperty("vendortomtserverwebmangimgdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsMangImgFolder);
		 File f = new File(sVenTomtServerWebappsMangImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsMangImgFiles = fArray.length;
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
		 return iVenTomtServerWebappsMangImgFiles;		 	  
  } 
  //28. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\manager\WEB-INF folder
  public int verifyVenTomtServerWebappsMangWebINFFolderFiles() throws Exception {
		 int iVenTomtServerWebappsMangWebINFFiles = 0;		 
		 String sVenTomtServerWebappsMangWebINFFolder = null;		 	
		 System.out.println("************************************");	
		 sVenTomtServerWebappsMangWebINFFolder = System.getProperty("vendortomtserverwebmangINFdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsMangWebINFFolder);
		 File f = new File(sVenTomtServerWebappsMangWebINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsMangWebINFFiles = fArray.length;
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
		 return iVenTomtServerWebappsMangWebINFFiles;		 	  
  }
  //29. Verify that the 2 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\server\webapps\manager\WEB-INF\lib folder
  public int verifyVenTomtServerWebappsMangWebINFLibFolderFiles() throws Exception {
		 int iVenTomtServerWebappsMangWebINFLibFiles = 0;		 
		 String sVenTomtServerWebappsMangWebINFLibFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtServerWebappsMangWebINFLibFolder = System.getProperty("vendortomtserverwebmangINFlibdir.value");
		 System.out.println("files location="+sVenTomtServerWebappsMangWebINFLibFolder);
		 File f = new File(sVenTomtServerWebappsMangWebINFLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtServerWebappsMangWebINFLibFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iVenTomtServerWebappsMangWebINFLibFiles;		 	  
  }  
  //30. Verify that the 1 folder exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\webapps folder
  public int verifyVenTomtWebappsFolderFiles() throws Exception {
		 int iVenTomtWebappsFiles = 0;		 
		 String sVenTomtWebappsFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtWebappsFolder = System.getProperty("vendortomtwebappsdir.value");
		 System.out.println("files location="+sVenTomtWebappsFolder);
		 File f = new File(sVenTomtWebappsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtWebappsFiles = fArray.length;
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
		 return iVenTomtWebappsFiles;		 	  
  }
  //31. Verify that the 2 folders and 6 files exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\webapps\ROOT folder
  public int verifyVenTomtWebappsRootFolderFiles() throws Exception {
		 int iVenTomtWebappsRootFiles = 0;		 
		 String sVenTomtWebappsRootFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtWebappsRootFolder = System.getProperty("vendortomtwebappsrootdir.value");
		 System.out.println("files location="+sVenTomtWebappsRootFolder);
		 File f = new File(sVenTomtWebappsRootFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtWebappsRootFiles = fArray.length;
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
		 return iVenTomtWebappsRootFiles;		 	  
  }  
  //32. Verify that the 1 file exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\webapps\ROOT\admin folder
  public int verifyVenTomtWebappsRootAdminFolderFiles() throws Exception {
		 int iVenTomtWebappsRootAdminFiles = 0;		 
		 String sVenTomtWebappsRootAdminFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtWebappsRootAdminFolder = System.getProperty("vendortomtwebappsrootadmindir.value");
		 System.out.println("files location="+sVenTomtWebappsRootAdminFolder);
		 File f = new File(sVenTomtWebappsRootAdminFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtWebappsRootAdminFiles = fArray.length;
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
		 return iVenTomtWebappsRootAdminFiles;		 	  
  }
  //33. Verify that the 1 folder and 1 file exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\webapps\ROOT\WEB-INF folder
  public int verifyVenTomtWebappsRootINFFolderFiles() throws Exception {
		 int iVenTomtWebappsRootINFFiles = 0;		 
		 String sVenTomtWebappsRootINFFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtWebappsRootINFFolder = System.getProperty("vendortomtwebappsrootINFdir.value");
		 System.out.println("files location="+sVenTomtWebappsRootINFFolder);
		 File f = new File(sVenTomtWebappsRootINFFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtWebappsRootINFFiles = fArray.length;
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
		 return iVenTomtWebappsRootINFFiles;		 	  
  }
  //34. Verify that the 1 file exists under terracotta-2.1.0\sessions\vendor\tomcat5.5\webapps\ROOT\WEB-INF\lib folder
  public int verifyVenTomtWebappsRootINFLibFolderFiles() throws Exception {
		 int iVenTomtWebappsRootINFLibFiles = 0;		 
		 String sVenTomtWebappsRootINFLibFolder = null;		 	
		 System.out.println("************************************");
		 sVenTomtWebappsRootINFLibFolder = System.getProperty("vendortomtwebappsrootINFlibdir.value");
		 System.out.println("files location="+sVenTomtWebappsRootINFLibFolder);
		 File f = new File(sVenTomtWebappsRootINFLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iVenTomtWebappsRootINFLibFiles = fArray.length;
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
		 return iVenTomtWebappsRootINFLibFiles;		 	  
  }  
}  
   
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
