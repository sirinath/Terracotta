/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DSODocsTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DSODocsTest.class);
  }

  public void testForVerification() throws Exception {
	    String os = System.getProperty("os.name");
	    System.out.println(os);
	    
	    if (os != null && os.startsWith("Lin") || os.startsWith("Sun")){	    	
	    	int  iDsoDocsCount = verifyDSODocsFolderFiles(); //1
		    assertEquals(3, iDsoDocsCount);
		    int  iDsoDocsHtmlCount = verifyDSODocsHtmlFolderFiles(); //2
		    assertEquals(1, iDsoDocsHtmlCount);	
		    int  iDsoDocsHtmlTerraCount = verifyDSODocsHtmlTerraFolderFiles(); //3
	    	assertEquals(81, iDsoDocsHtmlTerraCount);
	    	int  iDsoDocsHtmlTerraCssCount = verifyDSODocsHtmlTerraCssFolderFiles(); //4
		    assertEquals(8, iDsoDocsHtmlTerraCssCount);
		    int  iDsoDocsHtmlTerraImgCount = verifyDSODocsHtmlTerraImgFolderFiles(); //5
		    assertEquals(30, iDsoDocsHtmlTerraImgCount);
		    int  iDsoDocsHtmlTerraScriptsCount = verifyDSODocsHtmlTerraScriptsFolderFiles(); //6
		    assertEquals(1, iDsoDocsHtmlTerraScriptsCount);
		    int  iDsoDocsHtmlTerraDataCount = verifyDSODocsHtmlTerraDataFolderFiles(); //7
		    assertEquals(3, iDsoDocsHtmlTerraDataCount);
		    int  iDsoDocsHtmlTerraDataComCount = verifyDSODocsHtmlTerraDataComFolderFiles(); //8
		    assertEquals(8, iDsoDocsHtmlTerraDataComCount);
		    int  iDsoDocsHtmlTerraDataJsCount = verifyDSODocsHtmlTerraDataJsFolderFiles(); //9
		    assertEquals(4, iDsoDocsHtmlTerraDataJsCount);
		    int  iDsoDocsHtmlTerraDataJsSrCount = verifyDSODocsHtmlTerraDataJsSrFolderFiles(); //10
		    assertEquals(3, iDsoDocsHtmlTerraDataJsSrCount);
		    int  iDsoDocsHtmlTerraDataJsSrPairsCount = verifyDSODocsHtmlTerraDataJsSrPairsFolderFiles(); //11
		    assertEquals(69, iDsoDocsHtmlTerraDataJsSrPairsCount);
		    int  iDsoDocsHtmlTerraDataXmlCount = verifyDSODocsHtmlTerraDataXmlFolderFiles(); //12
		    assertEquals(3, iDsoDocsHtmlTerraDataXmlCount);
		    int  iDsoDocsHtmlTerraHelpCount = verifyDSODocsHtmlTerraHelpFolderFiles(); //13
		    assertEquals(5, iDsoDocsHtmlTerraHelpCount);
		    int  iDsoDocsHtmlTerraHelpImgCount = verifyDSODocsHtmlTerraHelpImgFolderFiles(); //14
		    assertEquals(6, iDsoDocsHtmlTerraHelpImgCount);
		    int  iDsoDocsHtmlTerraHelpImplCount = verifyDSODocsHtmlTerraHelpImplFolderFiles(); //15
		    assertEquals(4, iDsoDocsHtmlTerraHelpImplCount);
		    int  iDsoDocsHtmlTerraHelpImplComCount = verifyDSODocsHtmlTerraHelpImplComFolderFiles(); //16
		    assertEquals(4, iDsoDocsHtmlTerraHelpImplComCount);
		    int  iDsoDocsHtmlTerraHelpImplComHtmlCount = verifyDSODocsHtmlTerraHelpImplComHtmlFolderFiles(); //17
		    assertEquals(19, iDsoDocsHtmlTerraHelpImplComHtmlCount);
		    int  iDsoDocsHtmlTerraHelpImplComImgCount = verifyDSODocsHtmlTerraHelpImplComImgFolderFiles(); //18
		    assertEquals(34, iDsoDocsHtmlTerraHelpImplComImgCount);
		    int  iDsoDocsHtmlTerraHelpImplComPrivCount = verifyDSODocsHtmlTerraHelpImplComPrivFolderFiles(); //19
		    assertEquals(5, iDsoDocsHtmlTerraHelpImplComPrivCount);
		    int  iDsoDocsHtmlTerraHelpImplComScrpCount = verifyDSODocsHtmlTerraHelpImplComScrpFolderFiles(); //20
		    assertEquals(14, iDsoDocsHtmlTerraHelpImplComScrpCount);
		    int  iDsoDocsHtmlTerraHelpImplJsCount = verifyDSODocsHtmlTerraHelpImplJsFolderFiles(); //21
		    assertEquals(4, iDsoDocsHtmlTerraHelpImplJsCount);
		    int  iDsoDocsHtmlTerraHelpImplJsHtmlCount = verifyDSODocsHtmlTerraHelpImplJsHtmlFolderFiles(); //22
		    assertEquals(14, iDsoDocsHtmlTerraHelpImplJsHtmlCount);
		    int  iDsoDocsHtmlTerraHelpImplJsImgCount = verifyDSODocsHtmlTerraHelpImplJsImgFolderFiles(); //23
		    assertEquals(31, iDsoDocsHtmlTerraHelpImplJsImgCount);
		    int  iDsoDocsHtmlTerraHelpImplJsPrivCount = verifyDSODocsHtmlTerraHelpImplJsPrivFolderFiles(); //24
		    assertEquals(2, iDsoDocsHtmlTerraHelpImplJsPrivCount);
		    int  iDsoDocsHtmlTerraHelpImplJsScrpCount = verifyDSODocsHtmlTerraHelpImplJsScrpFolderFiles(); //25
		    assertEquals(22, iDsoDocsHtmlTerraHelpImplJsScrpCount);
	    } else {	    
		    int  iDsoDocsCount = verifyDSODocsFolderFiles(); //1
		    assertEquals(3, iDsoDocsCount);
		    int  iDsoDocsHtmlCount = verifyDSODocsHtmlFolderFiles(); //2
		    assertEquals(1, iDsoDocsHtmlCount);	
		    int  iDsoDocsHtmlTerraCount = verifyDSODocsHtmlTerraFolderFiles(); //3
		    int  iDsoDocsHtmlTerraDataJsSrPairsCount = verifyDSODocsHtmlTerraDataJsSrPairsFolderFiles(); //11	    
		    if (verifyFolderExists() == true){
		    	//assertEquals(77, iDsoDocsHtmlTerraCount);
			    assertEquals(80, iDsoDocsHtmlTerraCount);
			    assertEquals(68, iDsoDocsHtmlTerraDataJsSrPairsCount);
		    } else {
		    	assertEquals(81, iDsoDocsHtmlTerraCount);
		    	assertEquals(69, iDsoDocsHtmlTerraDataJsSrPairsCount);
		    }	    	    
		    int  iDsoDocsHtmlTerraCssCount = verifyDSODocsHtmlTerraCssFolderFiles(); //4
		    assertEquals(8, iDsoDocsHtmlTerraCssCount);
		    int  iDsoDocsHtmlTerraImgCount = verifyDSODocsHtmlTerraImgFolderFiles(); //5
		    //assertEquals(29, iDsoDocsHtmlTerraImgCount);
		    assertEquals(30, iDsoDocsHtmlTerraImgCount);
		    int  iDsoDocsHtmlTerraScriptsCount = verifyDSODocsHtmlTerraScriptsFolderFiles(); //6
		    assertEquals(1, iDsoDocsHtmlTerraScriptsCount);
		    int  iDsoDocsHtmlTerraDataCount = verifyDSODocsHtmlTerraDataFolderFiles(); //7
		    assertEquals(3, iDsoDocsHtmlTerraDataCount);
		    int  iDsoDocsHtmlTerraDataComCount = verifyDSODocsHtmlTerraDataComFolderFiles(); //8
		    assertEquals(8, iDsoDocsHtmlTerraDataComCount);
		    int  iDsoDocsHtmlTerraDataJsCount = verifyDSODocsHtmlTerraDataJsFolderFiles(); //9
		    assertEquals(4, iDsoDocsHtmlTerraDataJsCount);
		    int  iDsoDocsHtmlTerraDataJsSrCount = verifyDSODocsHtmlTerraDataJsSrFolderFiles(); //10
		    //assertEquals(2, iDsoDocsHtmlTerraDataJsSrCount);
		    assertEquals(3, iDsoDocsHtmlTerraDataJsSrCount);
		    //int  iDsoDocsHtmlTerraDataJsSrPairsCount = verifyDSODocsHtmlTerraDataJsSrPairsFolderFiles(); //11
		    //assertEquals(68, iDsoDocsHtmlTerraDataJsSrPairsCount);
		    int  iDsoDocsHtmlTerraDataXmlCount = verifyDSODocsHtmlTerraDataXmlFolderFiles(); //12
		    assertEquals(3, iDsoDocsHtmlTerraDataXmlCount);
		    int  iDsoDocsHtmlTerraHelpCount = verifyDSODocsHtmlTerraHelpFolderFiles(); //13
		    assertEquals(5, iDsoDocsHtmlTerraHelpCount);
		    int  iDsoDocsHtmlTerraHelpImgCount = verifyDSODocsHtmlTerraHelpImgFolderFiles(); //14
		    assertEquals(6, iDsoDocsHtmlTerraHelpImgCount);
		    int  iDsoDocsHtmlTerraHelpImplCount = verifyDSODocsHtmlTerraHelpImplFolderFiles(); //15
		    assertEquals(4, iDsoDocsHtmlTerraHelpImplCount);
		    int  iDsoDocsHtmlTerraHelpImplComCount = verifyDSODocsHtmlTerraHelpImplComFolderFiles(); //16
		    assertEquals(4, iDsoDocsHtmlTerraHelpImplComCount);
		    int  iDsoDocsHtmlTerraHelpImplComHtmlCount = verifyDSODocsHtmlTerraHelpImplComHtmlFolderFiles(); //17
		    assertEquals(19, iDsoDocsHtmlTerraHelpImplComHtmlCount);
		    int  iDsoDocsHtmlTerraHelpImplComImgCount = verifyDSODocsHtmlTerraHelpImplComImgFolderFiles(); //18
		    assertEquals(34, iDsoDocsHtmlTerraHelpImplComImgCount);
		    int  iDsoDocsHtmlTerraHelpImplComPrivCount = verifyDSODocsHtmlTerraHelpImplComPrivFolderFiles(); //19
		    assertEquals(5, iDsoDocsHtmlTerraHelpImplComPrivCount);
		    int  iDsoDocsHtmlTerraHelpImplComScrpCount = verifyDSODocsHtmlTerraHelpImplComScrpFolderFiles(); //20
		    assertEquals(14, iDsoDocsHtmlTerraHelpImplComScrpCount);
		    int  iDsoDocsHtmlTerraHelpImplJsCount = verifyDSODocsHtmlTerraHelpImplJsFolderFiles(); //21
		    assertEquals(4, iDsoDocsHtmlTerraHelpImplJsCount);
		    int  iDsoDocsHtmlTerraHelpImplJsHtmlCount = verifyDSODocsHtmlTerraHelpImplJsHtmlFolderFiles(); //22
		    assertEquals(14, iDsoDocsHtmlTerraHelpImplJsHtmlCount);
		    int  iDsoDocsHtmlTerraHelpImplJsImgCount = verifyDSODocsHtmlTerraHelpImplJsImgFolderFiles(); //23
		    assertEquals(31, iDsoDocsHtmlTerraHelpImplJsImgCount);
		    int  iDsoDocsHtmlTerraHelpImplJsPrivCount = verifyDSODocsHtmlTerraHelpImplJsPrivFolderFiles(); //24
		    assertEquals(2, iDsoDocsHtmlTerraHelpImplJsPrivCount);
		    int  iDsoDocsHtmlTerraHelpImplJsScrpCount = verifyDSODocsHtmlTerraHelpImplJsScrpFolderFiles(); //25
		    assertEquals(22, iDsoDocsHtmlTerraHelpImplJsScrpCount); 
	    }
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  } 
  
  public boolean verifyFolderExists() throws Exception {
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
					 if(fArray[i].getName().startsWith("spring")){
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
  //1. Verify that the 1 folder and 2 files terracotta-2.1.0/dso/docs
  public int verifyDSODocsFolderFiles() throws Exception {
     int iDsoDocsFiles = 0;     
     String sDsoDocsFolder = null;     
     System.out.println("************************************");
     sDsoDocsFolder = System.getProperty("docsdir.value");    
     System.out.println("files location="+sDsoDocsFolder);
     File f = new File(sDsoDocsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsFiles = fArray.length;
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
     return iDsoDocsFiles;       
  }   
  //2. Verify that the 1 folder exists under terracotta-2.1.0/dso/docs/html
  public int verifyDSODocsHtmlFolderFiles() throws Exception {
     int iDsoDocsFiles = 0;     
     String sDsoDocsFolder = null;     
     System.out.println("************************************");
     sDsoDocsFolder = System.getProperty("docshtmldir.value");    
     System.out.println("files location="+sDsoDocsFolder);
     File f = new File(sDsoDocsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsFiles = fArray.length;
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
     return iDsoDocsFiles;       
  }
  //3. Verify that the 5 folders and 76 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide
  public int verifyDSODocsHtmlTerraFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraFiles = 0;     
     String sDsoDocsHtmlTerraFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraFolder = System.getProperty("docshtmlTerradir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraFolder);
     File f = new File(sDsoDocsHtmlTerraFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraFiles = fArray.length;
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
     return iDsoDocsHtmlTerraFiles;       
  }
  //4. Verify that the 8 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/css
  public int verifyDSODocsHtmlTerraCssFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraCssFiles = 0;     
     String sDsoDocsHtmlTerraCssFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraCssFolder = System.getProperty("docshtmlTerracssdir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraCssFolder);
     File f = new File(sDsoDocsHtmlTerraCssFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraCssFiles = fArray.length;
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
     return iDsoDocsHtmlTerraCssFiles;       
  }
  //5. Verify that the 30 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/images
  public int verifyDSODocsHtmlTerraImgFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraImgFiles = 0;     
     String sDsoDocsHtmlTerraImgFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraImgFolder = System.getProperty("docshtmlTerraimgdir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraImgFolder);
     File f = new File(sDsoDocsHtmlTerraImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraImgFiles = fArray.length;
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
     return iDsoDocsHtmlTerraImgFiles;       
  }
  //6. Verify that the 1 file exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/scripts
  public int verifyDSODocsHtmlTerraScriptsFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraScriptsFiles = 0;     
     String sDsoDocsHtmlTerraScriptsFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraScriptsFolder = System.getProperty("docshtmlTerrascriptsdir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraScriptsFolder);
     File f = new File(sDsoDocsHtmlTerraScriptsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraScriptsFiles = fArray.length;
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
     return iDsoDocsHtmlTerraScriptsFiles;       
  }  
  //7. Verify that the 3 folders exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhdata
  public int verifyDSODocsHtmlTerraDataFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraDataFiles = 0;     
     String sDsoDocsHtmlTerraDataFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraDataFolder = System.getProperty("docshtmlTerradatadir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraDataFolder);
     File f = new File(sDsoDocsHtmlTerraDataFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraDataFiles = fArray.length;
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
     return iDsoDocsHtmlTerraDataFiles;       
  }  
  //8. Verify that the 8 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhdata/common
  public int verifyDSODocsHtmlTerraDataComFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraDataComFiles = 0;     
     String sDsoDocsHtmlTerraDataComFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraDataComFolder = System.getProperty("docshtmlTerradatacomdir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraDataComFolder);
     File f = new File(sDsoDocsHtmlTerraDataComFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraDataComFiles = fArray.length;
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
     return iDsoDocsHtmlTerraDataComFiles;       
  }  
  //9. Verify that the 1 folder and 3 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhdata/js
  public int verifyDSODocsHtmlTerraDataJsFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraDataJsFiles = 0;     
     String sDsoDocsHtmlTerraDataJsFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraDataJsFolder = System.getProperty("docshtmlTerradatajsdir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraDataJsFolder);
     File f = new File(sDsoDocsHtmlTerraDataJsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraDataJsFiles = fArray.length;
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
     return iDsoDocsHtmlTerraDataJsFiles;       
  }
  //10. Verify that the 1 folder and 2 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhdata/js/search
  public int verifyDSODocsHtmlTerraDataJsSrFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraDataJsSrFiles = 0;     
     String sDsoDocsHtmlTerraDataJsSrFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraDataJsSrFolder = System.getProperty("docshtmlTerradatajssearchdir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraDataJsSrFolder);
     File f = new File(sDsoDocsHtmlTerraDataJsSrFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraDataJsSrFiles = fArray.length;
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
     return iDsoDocsHtmlTerraDataJsSrFiles;       
  }  
  //11. Verify that the 69 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhdata/js/search/pairs
  public int verifyDSODocsHtmlTerraDataJsSrPairsFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraDataJsSrPairsFiles = 0;     
     String sDsoDocsHtmlTerraDataJsSrPairsFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraDataJsSrPairsFolder = System.getProperty("docshtmlTerradatajssrpairsdir.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraDataJsSrPairsFolder);
     File f = new File(sDsoDocsHtmlTerraDataJsSrPairsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraDataJsSrPairsFiles = fArray.length;
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
     return iDsoDocsHtmlTerraDataJsSrPairsFiles;       
  }  
  //12. Verify that the 3 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhdata/xml
  public int verifyDSODocsHtmlTerraDataXmlFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraDataXmlFiles = 0;     
     String sDsoDocsHtmlTerraDataXmlFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraDataXmlFolder = System.getProperty("docshtmlTerradataxml.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraDataXmlFolder);
     File f = new File(sDsoDocsHtmlTerraDataXmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraDataXmlFiles = fArray.length;
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
     return iDsoDocsHtmlTerraDataXmlFiles;       
  }  
  //13. Verify that the 2 folders and 3 files files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp
  public int verifyDSODocsHtmlTerraHelpFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpFiles = 0;     
     String sDsoDocsHtmlTerraHelpFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpFolder = System.getProperty("docshtmlTerrahelp.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpFolder);
     File f = new File(sDsoDocsHtmlTerraHelpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpFiles;       
  }  
  //14. Verify that the 6 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/images
  public int verifyDSODocsHtmlTerraHelpImgFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImgFiles = 0;     
     String sDsoDocsHtmlTerraHelpImgFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImgFolder = System.getProperty("docshtmlTerrahelpimg.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImgFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImgFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImgFiles;       
  }  
  //15. Verify that the 2 folders and 2 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl
  public int verifyDSODocsHtmlTerraHelpImplFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplFolder = System.getProperty("docshtmlTerrahelpimpl.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplFiles;       
  }  
  //16. Verify that the 4 folders exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/common
  public int verifyDSODocsHtmlTerraHelpImplComFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplComFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplComFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplComFolder = System.getProperty("docshtmlTerrahelpimplcom.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplComFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplComFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplComFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplComFiles;       
  }  
  //17. Verify that the 19 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/common/html
  public int verifyDSODocsHtmlTerraHelpImplComHtmlFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplComHtmlFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplComHtmlFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplComHtmlFolder = System.getProperty("docshtmlTerrahelpimplcomhtml.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplComHtmlFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplComHtmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplComHtmlFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplComHtmlFiles;       
  }
  //18. Verify that the 34 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/common/images
  public int verifyDSODocsHtmlTerraHelpImplComImgFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplComImgFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplComImgFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplComImgFolder = System.getProperty("docshtmlTerrahelpimplcomimg.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplComImgFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplComImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplComImgFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplComImgFiles;       
  }
  //19. Verify that the 5 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/common/private
  public int verifyDSODocsHtmlTerraHelpImplComPrivFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplComPrivFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplComPrivFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplComPrivFolder = System.getProperty("docshtmlTerrahelpimplcompriv.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplComPrivFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplComPrivFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplComPrivFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplComPrivFiles;       
  }
  //20. Verify that the 14 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/common/scripts
  public int verifyDSODocsHtmlTerraHelpImplComScrpFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplComScrpFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplComScrpFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplComScrpFolder = System.getProperty("docshtmlTerrahelpimplcomscrp.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplComScrpFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplComScrpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplComScrpFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplComScrpFiles;       
  }
  //21. Verify that the 4 folders exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/js
  public int verifyDSODocsHtmlTerraHelpImplJsFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplJsFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplJsFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplJsFolder = System.getProperty("docshtmlTerrahelpimpljs.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplJsFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplJsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplJsFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplJsFiles;       
  }
  //22. Verify that the 14 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/js/html
  public int verifyDSODocsHtmlTerraHelpImplJsHtmlFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplJsHtmlFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplJsHtmlFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplJsHtmlFolder = System.getProperty("docshtmlTerrahelpimpljshtml.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplJsHtmlFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplJsHtmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplJsHtmlFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplJsHtmlFiles;       
  }
  //23. Verify that the 30 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/js/images
  public int verifyDSODocsHtmlTerraHelpImplJsImgFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplJsImgFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplJsImgFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplJsImgFolder = System.getProperty("docshtmlTerrahelpimpljsimg.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplJsImgFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplJsImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplJsImgFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplJsImgFiles;       
  }
  //24. Verify that the 2 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/js/private
  public int verifyDSODocsHtmlTerraHelpImplJsPrivFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplJsPrivFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplJsPrivFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplJsPrivFolder = System.getProperty("docshtmlTerrahelpimpljspriv.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplJsPrivFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplJsPrivFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplJsPrivFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplJsPrivFiles;       
  }
  //25. Verify that the 22 files exists under terracotta-2.1.0/dso/docs/html/TerracottaDSOGuide/wwhelp/wwhimpl/js/scripts
  public int verifyDSODocsHtmlTerraHelpImplJsScrpFolderFiles() throws Exception {
     int iDsoDocsHtmlTerraHelpImplJsScrpFiles = 0;     
     String sDsoDocsHtmlTerraHelpImplJsScrpFolder = null;     
     System.out.println("************************************");
     sDsoDocsHtmlTerraHelpImplJsScrpFolder = System.getProperty("docshtmlTerrahelpimpljsscrp.value");    
     System.out.println("files location="+sDsoDocsHtmlTerraHelpImplJsScrpFolder);
     File f = new File(sDsoDocsHtmlTerraHelpImplJsScrpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iDsoDocsHtmlTerraHelpImplJsScrpFiles = fArray.length;
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
     return iDsoDocsHtmlTerraHelpImplJsScrpFiles;       
  }
}