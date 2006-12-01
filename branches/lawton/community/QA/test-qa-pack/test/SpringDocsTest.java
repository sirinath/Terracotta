/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SpringDocsTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SpringDocsTest.class);
  }

  public void testForVerification() throws Exception {
	    String os = System.getProperty("os.name");
	    System.out.println(os);
	    
	    
	    int  iSpringDocsCount = verifySpringDocsFolderFiles(); //1
	    
	    if (os != null && os.startsWith("Lin")){
	    	assertEquals(3, iSpringDocsCount);	   
	    	int  iSpringDocsHtmlCount = verifySpringDocsHtmlFolderFiles(); //2
	    	assertEquals(1, iSpringDocsHtmlCount);	
	    	int  iSpringDocsHtmlTerraCount = verifySpringDocsHtmlTerraFolderFiles(); //3	    
	    	assertEquals(37, iSpringDocsHtmlTerraCount);
	    	//assertEquals(46, iSpringDocsHtmlTerraCount);
	    	int  iSpringDocsHtmlTerraCssCount = verifySpringDocsHtmlTerraCssFolderFiles(); //4
	    	assertEquals(5, iSpringDocsHtmlTerraCssCount);
	    	int  iSpringDocsHtmlTerraImgCount = verifySpringDocsHtmlTerraImgFolderFiles(); //5	    
	    	assertEquals(8, iSpringDocsHtmlTerraImgCount);
	    	int  iSpringDocsHtmlTerraScriptsCount = verifySpringDocsHtmlTerraScriptsFolderFiles(); //6
	    	assertEquals(1, iSpringDocsHtmlTerraScriptsCount);
	    	int  iSpringDocsHtmlTerraDataCount = verifySpringDocsHtmlTerraDataFolderFiles(); //7
	    	assertEquals(3, iSpringDocsHtmlTerraDataCount);
	    	int  iSpringDocsHtmlTerraDataComCount = verifySpringDocsHtmlTerraDataComFolderFiles(); //8
	    	assertEquals(8, iSpringDocsHtmlTerraDataComCount);
	    	int  iSpringDocsHtmlTerraDataJsCount = verifySpringDocsHtmlTerraDataJsFolderFiles(); //9
	    	assertEquals(4, iSpringDocsHtmlTerraDataJsCount);
	    	int  iSpringDocsHtmlTerraDataJsSrCount = verifySpringDocsHtmlTerraDataJsSrFolderFiles(); //10
	    	assertEquals(2, iSpringDocsHtmlTerraDataJsSrCount);	    
	    	int  iSpringDocsHtmlTerraDataJsSrPairsCount = verifySpringDocsHtmlTerraDataJsSrPairsFolderFiles(); //11
	    	assertEquals(25, iSpringDocsHtmlTerraDataJsSrPairsCount);
	    	int  iSpringDocsHtmlTerraDataXmlCount = verifySpringDocsHtmlTerraDataXmlFolderFiles(); //12
	    	assertEquals(3, iSpringDocsHtmlTerraDataXmlCount);
	    	int  iSpringDocsHtmlTerraHelpCount = verifySpringDocsHtmlTerraHelpFolderFiles(); //13
	    	assertEquals(5, iSpringDocsHtmlTerraHelpCount);
	    	int  iSpringDocsHtmlTerraHelpImgCount = verifySpringDocsHtmlTerraHelpImgFolderFiles(); //14
			assertEquals(6, iSpringDocsHtmlTerraHelpImgCount);
			int  iSpringDocsHtmlTerraHelpImplCount = verifySpringDocsHtmlTerraHelpImplFolderFiles(); //15
		    assertEquals(4, iSpringDocsHtmlTerraHelpImplCount);
		    int  iSpringDocsHtmlTerraHelpImplComCount = verifySpringDocsHtmlTerraHelpImplComFolderFiles(); //16
		    assertEquals(4, iSpringDocsHtmlTerraHelpImplComCount);
		    int  iSpringDocsHtmlTerraHelpImplComHtmlCount = verifySpringDocsHtmlTerraHelpImplComHtmlFolderFiles(); //17
		    assertEquals(19, iSpringDocsHtmlTerraHelpImplComHtmlCount);
		    int  iSpringDocsHtmlTerraHelpImplComImgCount = verifySpringDocsHtmlTerraHelpImplComImgFolderFiles(); //18
		    assertEquals(34, iSpringDocsHtmlTerraHelpImplComImgCount);
		    int  iSpringDocsHtmlTerraHelpImplComPrivCount = verifySpringDocsHtmlTerraHelpImplComPrivFolderFiles(); //19
		    assertEquals(5, iSpringDocsHtmlTerraHelpImplComPrivCount);
		    int  iSpringDocsHtmlTerraHelpImplComScrpCount = verifySpringDocsHtmlTerraHelpImplComScrpFolderFiles(); //20
		    assertEquals(14, iSpringDocsHtmlTerraHelpImplComScrpCount);
		    int  iSpringDocsHtmlTerraHelpImplJsCount = verifySpringDocsHtmlTerraHelpImplJsFolderFiles(); //21
		    assertEquals(4, iSpringDocsHtmlTerraHelpImplJsCount);
		    int  iSpringDocsHtmlTerraHelpImplJsHtmlCount = verifySpringDocsHtmlTerraHelpImplJsHtmlFolderFiles(); //22
		    assertEquals(14, iSpringDocsHtmlTerraHelpImplJsHtmlCount);
		    int  iSpringDocsHtmlTerraHelpImplJsImgCount = verifySpringDocsHtmlTerraHelpImplJsImgFolderFiles(); //23
		    assertEquals(31, iSpringDocsHtmlTerraHelpImplJsImgCount);
		    int  iSpringDocsHtmlTerraHelpImplJsPrivCount = verifySpringDocsHtmlTerraHelpImplJsPrivFolderFiles(); //24
		    assertEquals(2, iSpringDocsHtmlTerraHelpImplJsPrivCount);
		    int  iSpringDocsHtmlTerraHelpImplJsScrpCount = verifySpringDocsHtmlTerraHelpImplJsScrpFolderFiles(); //25
		    assertEquals(22, iSpringDocsHtmlTerraHelpImplJsScrpCount);  
	    } else if(os != null && os.startsWith("Sun")){
	    	assertEquals(3, iSpringDocsCount);	   
	    	int  iSpringDocsHtmlCount = verifySpringDocsHtmlFolderFiles(); //2
	    	assertEquals(1, iSpringDocsHtmlCount);	
	    	int  iSpringDocsHtmlTerraCount = verifySpringDocsHtmlTerraFolderFiles(); //3
	    	int  iSpringDocsHtmlTerraCssCount = verifySpringDocsHtmlTerraCssFolderFiles(); //4
	    	int  iSpringDocsHtmlTerraImgCount = verifySpringDocsHtmlTerraImgFolderFiles(); //5	    	    		    	
	    	int  iSpringDocsHtmlTerraDataJsSrPairsCount = verifySpringDocsHtmlTerraDataJsSrPairsFolderFiles(); //11
	    	
	    	if (verifyTEEFolderExists() == true){
	    		assertEquals(37, iSpringDocsHtmlTerraCount);
      	  	    assertEquals(5, iSpringDocsHtmlTerraCssCount);
          	  	assertEquals(8, iSpringDocsHtmlTerraImgCount);
          	    assertEquals(25, iSpringDocsHtmlTerraDataJsSrPairsCount);
	    	}else {
	    		assertEquals(46, iSpringDocsHtmlTerraCount);
	    		assertEquals(6, iSpringDocsHtmlTerraCssCount);
	    		assertEquals(9, iSpringDocsHtmlTerraImgCount);
	    		assertEquals(34, iSpringDocsHtmlTerraDataJsSrPairsCount);
	    	}		    	
	    	
	    	int  iSpringDocsHtmlTerraScriptsCount = verifySpringDocsHtmlTerraScriptsFolderFiles(); //6
	    	assertEquals(1, iSpringDocsHtmlTerraScriptsCount);
	    	int  iSpringDocsHtmlTerraDataCount = verifySpringDocsHtmlTerraDataFolderFiles(); //7
	    	assertEquals(3, iSpringDocsHtmlTerraDataCount);
	    	int  iSpringDocsHtmlTerraDataComCount = verifySpringDocsHtmlTerraDataComFolderFiles(); //8
	    	assertEquals(8, iSpringDocsHtmlTerraDataComCount);
	    	int  iSpringDocsHtmlTerraDataJsCount = verifySpringDocsHtmlTerraDataJsFolderFiles(); //9
	    	assertEquals(4, iSpringDocsHtmlTerraDataJsCount);
	    	int  iSpringDocsHtmlTerraDataJsSrCount = verifySpringDocsHtmlTerraDataJsSrFolderFiles(); //10
	    	assertEquals(2, iSpringDocsHtmlTerraDataJsSrCount);	    
	    	
	    	int  iSpringDocsHtmlTerraDataXmlCount = verifySpringDocsHtmlTerraDataXmlFolderFiles(); //12
	    	assertEquals(3, iSpringDocsHtmlTerraDataXmlCount);
	    	int  iSpringDocsHtmlTerraHelpCount = verifySpringDocsHtmlTerraHelpFolderFiles(); //13
	    	assertEquals(5, iSpringDocsHtmlTerraHelpCount);
	    	int  iSpringDocsHtmlTerraHelpImgCount = verifySpringDocsHtmlTerraHelpImgFolderFiles(); //14
			assertEquals(6, iSpringDocsHtmlTerraHelpImgCount);
			int  iSpringDocsHtmlTerraHelpImplCount = verifySpringDocsHtmlTerraHelpImplFolderFiles(); //15
		    assertEquals(4, iSpringDocsHtmlTerraHelpImplCount);
		    int  iSpringDocsHtmlTerraHelpImplComCount = verifySpringDocsHtmlTerraHelpImplComFolderFiles(); //16
		    assertEquals(4, iSpringDocsHtmlTerraHelpImplComCount);
		    int  iSpringDocsHtmlTerraHelpImplComHtmlCount = verifySpringDocsHtmlTerraHelpImplComHtmlFolderFiles(); //17
		    assertEquals(19, iSpringDocsHtmlTerraHelpImplComHtmlCount);
		    int  iSpringDocsHtmlTerraHelpImplComImgCount = verifySpringDocsHtmlTerraHelpImplComImgFolderFiles(); //18
		    assertEquals(34, iSpringDocsHtmlTerraHelpImplComImgCount);
		    int  iSpringDocsHtmlTerraHelpImplComPrivCount = verifySpringDocsHtmlTerraHelpImplComPrivFolderFiles(); //19
		    assertEquals(5, iSpringDocsHtmlTerraHelpImplComPrivCount);
		    int  iSpringDocsHtmlTerraHelpImplComScrpCount = verifySpringDocsHtmlTerraHelpImplComScrpFolderFiles(); //20
		    assertEquals(14, iSpringDocsHtmlTerraHelpImplComScrpCount);
		    int  iSpringDocsHtmlTerraHelpImplJsCount = verifySpringDocsHtmlTerraHelpImplJsFolderFiles(); //21
		    assertEquals(4, iSpringDocsHtmlTerraHelpImplJsCount);
		    int  iSpringDocsHtmlTerraHelpImplJsHtmlCount = verifySpringDocsHtmlTerraHelpImplJsHtmlFolderFiles(); //22
		    assertEquals(14, iSpringDocsHtmlTerraHelpImplJsHtmlCount);
		    int  iSpringDocsHtmlTerraHelpImplJsImgCount = verifySpringDocsHtmlTerraHelpImplJsImgFolderFiles(); //23
		    assertEquals(31, iSpringDocsHtmlTerraHelpImplJsImgCount);
		    int  iSpringDocsHtmlTerraHelpImplJsPrivCount = verifySpringDocsHtmlTerraHelpImplJsPrivFolderFiles(); //24
		    assertEquals(2, iSpringDocsHtmlTerraHelpImplJsPrivCount);
		    int  iSpringDocsHtmlTerraHelpImplJsScrpCount = verifySpringDocsHtmlTerraHelpImplJsScrpFolderFiles(); //25
		    assertEquals(22, iSpringDocsHtmlTerraHelpImplJsScrpCount); 
	    }
	    else {
	    	if (verifyTEEFolderExists() == true){
		    	assertEquals(3, iSpringDocsCount);
	    	}else {
	    		assertEquals(1, iSpringDocsCount);
	    	}
	    }
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
  //1. Verify that the 1 folder and 2 files terracotta-2.1.0/spring/docs
  public int verifySpringDocsFolderFiles() throws Exception {
     int iSpringDocsFiles = 0;     
     String sSpringDocsFolder = null;     
     System.out.println("************************************");
     sSpringDocsFolder = System.getProperty("sdocsdir.value");    
     System.out.println("files location="+sSpringDocsFolder);
     File f = new File(sSpringDocsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsFiles = fArray.length;
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
     return iSpringDocsFiles;       
  }   
  //2. Verify that the 1 folder exists under terracotta-2.1.0/spring/docs/html
  public int verifySpringDocsHtmlFolderFiles() throws Exception {
     int iSpringDocsFiles = 0;     
     String sSpringDocsFolder = null;     
     System.out.println("************************************");
     sSpringDocsFolder = System.getProperty("sdocshtmldir.value");    
     System.out.println("files location="+sSpringDocsFolder);
     File f = new File(sSpringDocsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsFiles = fArray.length;
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
     return iSpringDocsFiles;       
  }
  //3. Verify that the 5 folders and 32 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide
  public int verifySpringDocsHtmlTerraFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraFiles = 0;     
     String sSpringDocsHtmlTerraFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraFolder = System.getProperty("sdocshtmlTerradir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraFolder);
     File f = new File(sSpringDocsHtmlTerraFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraFiles = fArray.length;
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
     return iSpringDocsHtmlTerraFiles;       
  }
  //4. Verify that the 5 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/css
  public int verifySpringDocsHtmlTerraCssFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraCssFiles = 0;     
     String sSpringDocsHtmlTerraCssFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraCssFolder = System.getProperty("sdocshtmlTerracssdir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraCssFolder);
     File f = new File(sSpringDocsHtmlTerraCssFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraCssFiles = fArray.length;
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
     return iSpringDocsHtmlTerraCssFiles;       
  }
  //5. Verify that the 8 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/images
  public int verifySpringDocsHtmlTerraImgFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraImgFiles = 0;     
     String sSpringDocsHtmlTerraImgFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraImgFolder = System.getProperty("sdocshtmlTerraimgdir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraImgFolder);
     File f = new File(sSpringDocsHtmlTerraImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraImgFiles = fArray.length;
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
     return iSpringDocsHtmlTerraImgFiles;       
  }
  //6. Verify that the 1 file exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/scripts
  public int verifySpringDocsHtmlTerraScriptsFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraScriptsFiles = 0;     
     String sSpringDocsHtmlTerraScriptsFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraScriptsFolder = System.getProperty("sdocshtmlTerrascriptsdir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraScriptsFolder);
     File f = new File(sSpringDocsHtmlTerraScriptsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraScriptsFiles = fArray.length;
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
     return iSpringDocsHtmlTerraScriptsFiles;       
  }  
  //7. Verify that the 3 folders exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhdata
  public int verifySpringDocsHtmlTerraDataFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraDataFiles = 0;     
     String sSpringDocsHtmlTerraDataFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraDataFolder = System.getProperty("sdocshtmlTerradatadir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraDataFolder);
     File f = new File(sSpringDocsHtmlTerraDataFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraDataFiles = fArray.length;
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
     return iSpringDocsHtmlTerraDataFiles;       
  }  
  //8. Verify that the 8 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhdata/common
  public int verifySpringDocsHtmlTerraDataComFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraDataComFiles = 0;     
     String sSpringDocsHtmlTerraDataComFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraDataComFolder = System.getProperty("sdocshtmlTerradatacomdir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraDataComFolder);
     File f = new File(sSpringDocsHtmlTerraDataComFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraDataComFiles = fArray.length;
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
     return iSpringDocsHtmlTerraDataComFiles;       
  }  
  //9. Verify that the 1 folder and 3 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhdata/js
  public int verifySpringDocsHtmlTerraDataJsFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraDataJsFiles = 0;     
     String sSpringDocsHtmlTerraDataJsFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraDataJsFolder = System.getProperty("sdocshtmlTerradatajsdir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraDataJsFolder);
     File f = new File(sSpringDocsHtmlTerraDataJsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraDataJsFiles = fArray.length;
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
     return iSpringDocsHtmlTerraDataJsFiles;       
  }
  //10. Verify that the 1 folder and 1 file exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhdata/js/search
  public int verifySpringDocsHtmlTerraDataJsSrFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraDataJsSrFiles = 0;     
     String sSpringDocsHtmlTerraDataJsSrFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraDataJsSrFolder = System.getProperty("sdocshtmlTerradatajssearchdir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraDataJsSrFolder);
     File f = new File(sSpringDocsHtmlTerraDataJsSrFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraDataJsSrFiles = fArray.length;
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
     return iSpringDocsHtmlTerraDataJsSrFiles;       
  }  
  //11. Verify that the 25 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhdata/js/search/pairs
  public int verifySpringDocsHtmlTerraDataJsSrPairsFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraDataJsSrPairsFiles = 0;     
     String sSpringDocsHtmlTerraDataJsSrPairsFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraDataJsSrPairsFolder = System.getProperty("sdocshtmlTerradatajssrpairsdir.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraDataJsSrPairsFolder);
     File f = new File(sSpringDocsHtmlTerraDataJsSrPairsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraDataJsSrPairsFiles = fArray.length;
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
     return iSpringDocsHtmlTerraDataJsSrPairsFiles;       
  }  
  //12. Verify that the 3 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhdata/xml
  public int verifySpringDocsHtmlTerraDataXmlFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraDataXmlFiles = 0;     
     String sSpringDocsHtmlTerraDataXmlFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraDataXmlFolder = System.getProperty("sdocshtmlTerradataxml.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraDataXmlFolder);
     File f = new File(sSpringDocsHtmlTerraDataXmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraDataXmlFiles = fArray.length;
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
     return iSpringDocsHtmlTerraDataXmlFiles;       
  }  
  //13. Verify that the 2 folders and 3 files files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp
  public int verifySpringDocsHtmlTerraHelpFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpFiles = 0;     
     String sSpringDocsHtmlTerraHelpFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpFolder = System.getProperty("sdocshtmlTerrahelp.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpFolder);
     File f = new File(sSpringDocsHtmlTerraHelpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpFiles;       
  }  
  //14. Verify that the 6 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/images
  public int verifySpringDocsHtmlTerraHelpImgFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImgFiles = 0;     
     String sSpringDocsHtmlTerraHelpImgFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImgFolder = System.getProperty("sdocshtmlTerrahelpimg.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImgFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImgFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImgFiles;       
  }  
  //15. Verify that the 2 folders and 2 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl
  public int verifySpringDocsHtmlTerraHelpImplFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplFolder = System.getProperty("sdocshtmlTerrahelpimpl.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplFiles;       
  }  
  //16. Verify that the 4 folders exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/common
  public int verifySpringDocsHtmlTerraHelpImplComFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplComFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplComFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplComFolder = System.getProperty("sdocshtmlTerrahelpimplcom.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplComFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplComFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplComFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplComFiles;       
  }  
  //17. Verify that the 19 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/common/html
  public int verifySpringDocsHtmlTerraHelpImplComHtmlFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplComHtmlFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplComHtmlFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplComHtmlFolder = System.getProperty("sdocshtmlTerrahelpimplcomhtml.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplComHtmlFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplComHtmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplComHtmlFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplComHtmlFiles;       
  }
  //18. Verify that the 34 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/common/images
  public int verifySpringDocsHtmlTerraHelpImplComImgFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplComImgFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplComImgFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplComImgFolder = System.getProperty("sdocshtmlTerrahelpimplcomimg.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplComImgFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplComImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplComImgFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplComImgFiles;       
  }
  //19. Verify that the 5 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/common/private
  public int verifySpringDocsHtmlTerraHelpImplComPrivFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplComPrivFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplComPrivFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplComPrivFolder = System.getProperty("sdocshtmlTerrahelpimplcompriv.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplComPrivFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplComPrivFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplComPrivFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplComPrivFiles;       
  }
  //20. Verify that the 14 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/common/scripts
  public int verifySpringDocsHtmlTerraHelpImplComScrpFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplComScrpFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplComScrpFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplComScrpFolder = System.getProperty("sdocshtmlTerrahelpimplcomscrp.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplComScrpFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplComScrpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplComScrpFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplComScrpFiles;       
  }
  //21. Verify that the 4 folders exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/js
  public int verifySpringDocsHtmlTerraHelpImplJsFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplJsFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplJsFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplJsFolder = System.getProperty("sdocshtmlTerrahelpimpljs.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplJsFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplJsFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplJsFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplJsFiles;       
  }
  //22. Verify that the 14 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/js/html
  public int verifySpringDocsHtmlTerraHelpImplJsHtmlFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplJsHtmlFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplJsHtmlFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplJsHtmlFolder = System.getProperty("sdocshtmlTerrahelpimpljshtml.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplJsHtmlFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplJsHtmlFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplJsHtmlFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplJsHtmlFiles;       
  }
  //23. Verify that the 30 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/js/images
  public int verifySpringDocsHtmlTerraHelpImplJsImgFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplJsImgFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplJsImgFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplJsImgFolder = System.getProperty("sdocshtmlTerrahelpimpljsimg.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplJsImgFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplJsImgFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplJsImgFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplJsImgFiles;       
  }
  //24. Verify that the 2 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/js/private
  public int verifySpringDocsHtmlTerraHelpImplJsPrivFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplJsPrivFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplJsPrivFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplJsPrivFolder = System.getProperty("sdocshtmlTerrahelpimpljspriv.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplJsPrivFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplJsPrivFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplJsPrivFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplJsPrivFiles;       
  }
  //25. Verify that the 22 files exists under terracotta-2.1.0/spring/docs/html/TerracottaSpringGuide/wwhelp/wwhimpl/js/scripts
  public int verifySpringDocsHtmlTerraHelpImplJsScrpFolderFiles() throws Exception {
     int iSpringDocsHtmlTerraHelpImplJsScrpFiles = 0;     
     String sSpringDocsHtmlTerraHelpImplJsScrpFolder = null;     
     System.out.println("************************************");
     sSpringDocsHtmlTerraHelpImplJsScrpFolder = System.getProperty("sdocshtmlTerrahelpimpljsscrp.value");    
     System.out.println("files location="+sSpringDocsHtmlTerraHelpImplJsScrpFolder);
     File f = new File(sSpringDocsHtmlTerraHelpImplJsScrpFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
    	 iSpringDocsHtmlTerraHelpImplJsScrpFiles = fArray.length;
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
     return iSpringDocsHtmlTerraHelpImplJsScrpFiles;       
  }
}