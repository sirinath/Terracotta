/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SandBoxTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SandBoxTest.class);
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
}  
   
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
