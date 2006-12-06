/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SessionsDocsTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SessionsDocsTest.class);
  }

  public void testForVerification() throws Exception {	
	    String os = System.getProperty("os.name");
	    System.out.println(os);
	    
	    int  iSessionDocsHtmlTerraCount = verifySessionDocsHtmlTerraFolderFiles(); //3
	    int  iSessionDocsHtmlTerraCssCount = verifySessionDocsHtmlTerraCssFolderFiles(); //4
	    int  iSessionDocsHtmlTerraImgCount = verifySessionDocsHtmlTerraImgFolderFiles(); //5	    
	    int  iSessionDocsHtmlTerraDataJsSrPairsCount = verifySessionDocsHtmlTerraDataJsSrPairsFolderFiles(); //11
	    if (os != null && os.startsWith("Lin") ){
	    	assertEquals(62, iSessionDocsHtmlTerraCount);
	    	int  iSessionDocsHtmlTerraHelpImplComImgCount = verifySessionDocsHtmlTerraHelpImplComImgFolderFiles();
	    	assertEquals(34, iSessionDocsHtmlTerraHelpImplComImgCount);		    	
	    } else if (os != null && os.startsWith("Sun")){
	    	int  iSessionDocsHtmlTerraHelpImplComImgCount = verifySessionDocsHtmlTerraHelpImplComImgFolderFiles();
	    	assertEquals(27, iSessionDocsHtmlTerraHelpImplComImgCount); 	
	    } else {
	    	int  iSessionDocsHtmlTerraHelpImplComImgCount = verifySessionDocsHtmlTerraHelpImplComImgFolderFiles();
	    	assertEquals(34, iSessionDocsHtmlTerraHelpImplComImgCount);
	    	if (verifyTEEFolderExists() == true){
		    	assertEquals(68, iSessionDocsHtmlTerraCount);
		    	assertEquals(7, iSessionDocsHtmlTerraCssCount);
		    	assertEquals(19, iSessionDocsHtmlTerraImgCount);
		    	assertEquals(56, iSessionDocsHtmlTerraDataJsSrPairsCount);
		    } else {
		    	assertEquals(62, iSessionDocsHtmlTerraCount);
		    	assertEquals(6, iSessionDocsHtmlTerraCssCount);
		    	assertEquals(18, iSessionDocsHtmlTerraImgCount);
		    	assertEquals(50, iSessionDocsHtmlTerraDataJsSrPairsCount);
		    }
	    }	    
	    
	    int  iSessionDocsCount = verifySessionDocsFolderFiles(); //1
	    assertEquals(3, iSessionDocsCount);
	    int  iSessionDocsHtmlCount = verifySessionDocsHtmlFolderFiles(); //2
	    assertEquals(1, iSessionDocsHtmlCount);           
	    int  iSessionDocsHtmlTerraScriptsCount = verifySessionDocsHtmlTerraScriptsFolderFiles(); //6
	    assertEquals(1, iSessionDocsHtmlTerraScriptsCount);
	    int  iSessionDocsHtmlTerraDataCount = verifySessionDocsHtmlTerraDataFolderFiles(); //7
	    assertEquals(3, iSessionDocsHtmlTerraDataCount);
	    int  iSessionDocsHtmlTerraDataComCount = verifySessionDocsHtmlTerraDataComFolderFiles(); //8
	    assertEquals(8, iSessionDocsHtmlTerraDataComCount);
	    int  iSessionDocsHtmlTerraDataJsCount = verifySessionDocsHtmlTerraDataJsFolderFiles(); //9
	    assertEquals(4, iSessionDocsHtmlTerraDataJsCount);
	    int  iSessionDocsHtmlTerraDataJsSrCount = verifySessionDocsHtmlTerraDataJsSrFolderFiles(); //10
	    assertEquals(2, iSessionDocsHtmlTerraDataJsSrCount);
	    
	    int  iSessionDocsHtmlTerraDataXmlCount = verifySessionDocsHtmlTerraDataXmlFolderFiles(); //12
	    assertEquals(3, iSessionDocsHtmlTerraDataXmlCount);
	    int  iSessionDocsHtmlTerraHelpCount = verifySessionDocsHtmlTerraHelpFolderFiles(); //13
	    assertEquals(5, iSessionDocsHtmlTerraHelpCount);
	    int  iSessionDocsHtmlTerraHelpImgCount = verifySessionDocsHtmlTerraHelpImgFolderFiles(); //14
	    assertEquals(6, iSessionDocsHtmlTerraHelpImgCount);
	    int  iSessionDocsHtmlTerraHelpImplCount = verifySessionDocsHtmlTerraHelpImplFolderFiles(); //15
	    assertEquals(4, iSessionDocsHtmlTerraHelpImplCount);
	    int  iSessionDocsHtmlTerraHelpImplComCount = verifySessionDocsHtmlTerraHelpImplComFolderFiles(); //16
	    assertEquals(4, iSessionDocsHtmlTerraHelpImplComCount);
	    int  iSessionDocsHtmlTerraHelpImplComHtmlCount = verifySessionDocsHtmlTerraHelpImplComHtmlFolderFiles(); //17
	    assertEquals(19, iSessionDocsHtmlTerraHelpImplComHtmlCount);
	    //int  iSessionDocsHtmlTerraHelpImplComImgCount = verifySessionDocsHtmlTerraHelpImplComImgFolderFiles(); //18
	    //assertEquals(34, iSessionDocsHtmlTerraHelpImplComImgCount);
	    int  iSessionDocsHtmlTerraHelpImplComPrivCount = verifySessionDocsHtmlTerraHelpImplComPrivFolderFiles(); //19
	    assertEquals(5, iSessionDocsHtmlTerraHelpImplComPrivCount);
	    int  iSessionDocsHtmlTerraHelpImplComScrpCount = verifySessionDocsHtmlTerraHelpImplComScrpFolderFiles(); //20
	    assertEquals(14, iSessionDocsHtmlTerraHelpImplComScrpCount);
	    int  iSessionDocsHtmlTerraHelpImplJsCount = verifySessionDocsHtmlTerraHelpImplJsFolderFiles(); //21
	    assertEquals(4, iSessionDocsHtmlTerraHelpImplJsCount);
	    int  iSessionDocsHtmlTerraHelpImplJsHtmlCount = verifySessionDocsHtmlTerraHelpImplJsHtmlFolderFiles(); //22
	    assertEquals(14, iSessionDocsHtmlTerraHelpImplJsHtmlCount);
	    int  iSessionDocsHtmlTerraHelpImplJsImgCount = verifySessionDocsHtmlTerraHelpImplJsImgFolderFiles(); //23
	    assertEquals(31, iSessionDocsHtmlTerraHelpImplJsImgCount);
	    int  iSessionDocsHtmlTerraHelpImplJsPrivCount = verifySessionDocsHtmlTerraHelpImplJsPrivFolderFiles(); //24
	    assertEquals(2, iSessionDocsHtmlTerraHelpImplJsPrivCount);
	    int  iSessionDocsHtmlTerraHelpImplJsScrpCount = verifySessionDocsHtmlTerraHelpImplJsScrpFolderFiles(); //25
	    assertEquals(22, iSessionDocsHtmlTerraHelpImplJsScrpCount);  
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  } 
  
  public boolean verifyTEEFolderExists() throws Exception {
		 int iRootFiles = 0;
		 boolean TeeExists = false;
		 String sRootFolder = null;		 	
		 System.out.println("************************************");
		 sRootFolder = System.getProperty("homedir.value");		 
		 System.out.println("files location="+sRootFolder);
		 File f = new File(sRootFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iRootFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 if(fArray[i].getName().startsWith("dso")){
						 TeeExists = true;
					 }
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return TeeExists;		 	  
} 
  //1. Verify that the 1 folder and 2 files terracotta-2.1.0/sessions/docs
  public int verifySessionDocsFolderFiles() throws Exception {
     int iSessionDocsFiles = 0;     
     String sSessionDocsFolder = null;     
     System.out.println("************************************");
     sSessionDocsFolder = System.getProperty("sessionsdocs.dir");    
     System.out.println("files location="+sSessionDocsFolder);
     File f = new File(sSessionDocsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsFiles = fArray.length;
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
     return iSessionDocsFiles;       
  }   
  //2. Verify that the 1 folder exists under terracotta-2.1.0/sessions/docs/html
  public int verifySessionDocsHtmlFolderFiles() throws Exception {
     int iSessionDocsFiles = 0;     
     String sSessionDocsFolder = null;     
     System.out.println("************************************");
     sSessionDocsFolder = System.getProperty("sessionshtmldir.value");    
     System.out.println("files location="+sSessionDocsFolder);
     File f = new File(sSessionDocsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsFiles = fArray.length;
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
     return iSessionDocsFiles;       
  }
  //3. Verify that the 5 folders and 57 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart
  public int verifySessionDocsHtmlTerraFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraFiles = 0;     
     String sSessionDocsHtmlTerraFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraFolder = System.getProperty("sessionshtmlTerradir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraFolder);
     File f = new File(sSessionDocsHtmlTerraFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraFiles = fArray.length;
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
     return iSessionDocsHtmlTerraFiles;       
  }
  //4. Verify that the 6 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/css
  public int verifySessionDocsHtmlTerraCssFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraCssFiles = 0;     
     String sSessionDocsHtmlTerraCssFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraCssFolder = System.getProperty("sessionshtmlTerracssdir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraCssFolder);
     File f = new File(sSessionDocsHtmlTerraCssFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraCssFiles = fArray.length;
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
     return iSessionDocsHtmlTerraCssFiles;       
  }
  //5. Verify that the 15 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/images
  public int verifySessionDocsHtmlTerraImgFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraImgFiles = 0;     
     String sSessionDocsHtmlTerraImgFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraImgFolder = System.getProperty("sessionshtmlTerraimgdir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraImgFolder);
     File f = new File(sSessionDocsHtmlTerraImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraImgFiles = fArray.length;
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
     return iSessionDocsHtmlTerraImgFiles;       
  }
  //6. Verify that the 1 file exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/scripts
  public int verifySessionDocsHtmlTerraScriptsFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraScriptsFiles = 0;     
     String sSessionDocsHtmlTerraScriptsFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraScriptsFolder = System.getProperty("sessionshtmlTerrascriptsdir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraScriptsFolder);
     File f = new File(sSessionDocsHtmlTerraScriptsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraScriptsFiles = fArray.length;
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
     return iSessionDocsHtmlTerraScriptsFiles;       
  }  
  //7. Verify that the 3 folders exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhdata
  public int verifySessionDocsHtmlTerraDataFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraDataFiles = 0;     
     String sSessionDocsHtmlTerraDataFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraDataFolder = System.getProperty("sessionshtmlTerradatadir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraDataFolder);
     File f = new File(sSessionDocsHtmlTerraDataFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraDataFiles = fArray.length;
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
     return iSessionDocsHtmlTerraDataFiles;       
  }  
  //8. Verify that the 8 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhdata/common
  public int verifySessionDocsHtmlTerraDataComFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraDataComFiles = 0;     
     String sSessionDocsHtmlTerraDataComFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraDataComFolder = System.getProperty("sessionshtmlTerradatacomdir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraDataComFolder);
     File f = new File(sSessionDocsHtmlTerraDataComFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraDataComFiles = fArray.length;
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
     return iSessionDocsHtmlTerraDataComFiles;       
  }  
  //9. Verify that the 1 folder and 3 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhdata/js
  public int verifySessionDocsHtmlTerraDataJsFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraDataJsFiles = 0;     
     String sSessionDocsHtmlTerraDataJsFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraDataJsFolder = System.getProperty("sessionshtmlTerradatajsdir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraDataJsFolder);
     File f = new File(sSessionDocsHtmlTerraDataJsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraDataJsFiles = fArray.length;
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
     return iSessionDocsHtmlTerraDataJsFiles;       
  }
  //10. Verify that the 1 folder and 1 file exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhdata/js/search
  public int verifySessionDocsHtmlTerraDataJsSrFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraDataJsSrFiles = 0;     
     String sSessionDocsHtmlTerraDataJsSrFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraDataJsSrFolder = System.getProperty("sessionshtmlTerradatajssearchdir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraDataJsSrFolder);
     File f = new File(sSessionDocsHtmlTerraDataJsSrFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraDataJsSrFiles = fArray.length;
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
     return iSessionDocsHtmlTerraDataJsSrFiles;       
  }  
  //11. Verify that the 50 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhdata/js/search/pairs
  public int verifySessionDocsHtmlTerraDataJsSrPairsFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraDataJsSrPairsFiles = 0;     
     String sSessionDocsHtmlTerraDataJsSrPairsFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraDataJsSrPairsFolder = System.getProperty("sessionshtmlTerradatajssrpairsdir.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraDataJsSrPairsFolder);
     File f = new File(sSessionDocsHtmlTerraDataJsSrPairsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraDataJsSrPairsFiles = fArray.length;
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
     return iSessionDocsHtmlTerraDataJsSrPairsFiles;       
  }  
  //12. Verify that the 3 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhdata/xml
  public int verifySessionDocsHtmlTerraDataXmlFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraDataXmlFiles = 0;     
     String sSessionDocsHtmlTerraDataXmlFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraDataXmlFolder = System.getProperty("sessionshtmlTerradataxml.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraDataXmlFolder);
     File f = new File(sSessionDocsHtmlTerraDataXmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraDataXmlFiles = fArray.length;
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
     return iSessionDocsHtmlTerraDataXmlFiles;       
  }  
  //13. Verify that the 2 folders and 3 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp
  public int verifySessionDocsHtmlTerraHelpFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpFiles = 0;     
     String sSessionDocsHtmlTerraHelpFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpFolder = System.getProperty("sessionshtmlTerrahelp.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpFolder);
     File f = new File(sSessionDocsHtmlTerraHelpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpFiles;       
  }  
  //14. Verify that the 6 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/images
  public int verifySessionDocsHtmlTerraHelpImgFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImgFiles = 0;     
     String sSessionDocsHtmlTerraHelpImgFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImgFolder = System.getProperty("sessionshtmlTerrahelpimg.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImgFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImgFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImgFiles;       
  }  
  //15. Verify that the 2 folders and 2 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl
  public int verifySessionDocsHtmlTerraHelpImplFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplFolder = System.getProperty("sessionshtmlTerrahelpimpl.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplFiles;       
  }  
  //16. Verify that the 4 folders exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/common
  public int verifySessionDocsHtmlTerraHelpImplComFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplComFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplComFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplComFolder = System.getProperty("sessionshtmlTerrahelpimplcom.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplComFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplComFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplComFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplComFiles;       
  }  
  //17. Verify that the 19 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/common/html
  public int verifySessionDocsHtmlTerraHelpImplComHtmlFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplComHtmlFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplComHtmlFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplComHtmlFolder = System.getProperty("sessionshtmlTerrahelpimplcomhtml.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplComHtmlFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplComHtmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplComHtmlFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplComHtmlFiles;       
  }
  //18. Verify that the 34 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/common/images
  public int verifySessionDocsHtmlTerraHelpImplComImgFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplComImgFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplComImgFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplComImgFolder = System.getProperty("sessionshtmlTerrahelpimplcomimg.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplComImgFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplComImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplComImgFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplComImgFiles;       
  }
  //19. Verify that the 5 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/common/private
  public int verifySessionDocsHtmlTerraHelpImplComPrivFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplComPrivFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplComPrivFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplComPrivFolder = System.getProperty("sessionshtmlTerrahelpimplcompriv.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplComPrivFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplComPrivFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplComPrivFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplComPrivFiles;       
  }
  //20. Verify that the 14 files exists under terracotta-2.1.0/Session/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/common/scripts
  public int verifySessionDocsHtmlTerraHelpImplComScrpFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplComScrpFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplComScrpFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplComScrpFolder = System.getProperty("sessionshtmlTerrahelpimplcomscrp.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplComScrpFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplComScrpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplComScrpFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplComScrpFiles;       
  }
  //21. Verify that the 4 folders exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/js
  public int verifySessionDocsHtmlTerraHelpImplJsFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplJsFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplJsFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplJsFolder = System.getProperty("sessionshtmlTerrahelpimpljs.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplJsFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplJsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplJsFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplJsFiles;       
  }
  //22. Verify that the 14 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/js/html
  public int verifySessionDocsHtmlTerraHelpImplJsHtmlFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplJsHtmlFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplJsHtmlFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplJsHtmlFolder = System.getProperty("sessionshtmlTerrahelpimpljshtml.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplJsHtmlFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplJsHtmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplJsHtmlFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplJsHtmlFiles;       
  }
  //23. Verify that the 30 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/js/images
  public int verifySessionDocsHtmlTerraHelpImplJsImgFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplJsImgFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplJsImgFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplJsImgFolder = System.getProperty("sessionshtmlTerrahelpimpljsimg.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplJsImgFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplJsImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplJsImgFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplJsImgFiles;       
  }
  //24. Verify that the 2 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/js/private
  public int verifySessionDocsHtmlTerraHelpImplJsPrivFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplJsPrivFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplJsPrivFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplJsPrivFolder = System.getProperty("sessionshtmlTerrahelpimpljspriv.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplJsPrivFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplJsPrivFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplJsPrivFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplJsPrivFiles;       
  }
  //25. Verify that the 22 files exists under terracotta-2.1.0/sessions/docs/html/TerracottaSessionsQuickStart/wwhelp/wwhimpl/js/scripts
  public int verifySessionDocsHtmlTerraHelpImplJsScrpFolderFiles() throws Exception {
     int iSessionDocsHtmlTerraHelpImplJsScrpFiles = 0;     
     String sSessionDocsHtmlTerraHelpImplJsScrpFolder = null;     
     System.out.println("************************************");
     sSessionDocsHtmlTerraHelpImplJsScrpFolder = System.getProperty("sessionshtmlTerrahelpimpljsscrp.value");    
     System.out.println("files location="+sSessionDocsHtmlTerraHelpImplJsScrpFolder);
     File f = new File(sSessionDocsHtmlTerraHelpImplJsScrpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSessionDocsHtmlTerraHelpImplJsScrpFiles = fArray.length;
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
     return iSessionDocsHtmlTerraHelpImplJsScrpFiles;       
  }
}