/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class SpringTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(SpringTest.class);
  }

  public void testForVerification() throws Exception {
	    String os = System.getProperty("os.name");
	    System.out.println(os);
	    int  iSpringCount = verifySpringFolderFiles(); //1	    	    
	    int  iSpringBinCount = verifySpringBinFolderFiles(); //2
	    int  iSpringSamplesCount = verifySpringSamplesFolderFiles(); //5	    
	    assertEquals(9, iSpringSamplesCount);
	    int  iSpringSconfigCount = verifySpringSconfigFolderFiles(); //4
	  
	    if (os != null && os.startsWith("Lin")){	    	
	    	assertEquals(5, iSpringCount);	    	
	    	assertEquals(6, iSpringBinCount);	    	
	    	assertEquals(1, iSpringSconfigCount);	    	
	    } else if ( os != null && os.startsWith("Sun")){
	    	assertEquals(5, iSpringCount);
	    	assertEquals(6, iSpringBinCount);	    	
	    	assertEquals(2, iSpringSconfigCount);	    	
	    }
	    else {
	    	if (verifyTEEFolderExists() == true){
		    	assertEquals(5, iSpringCount);
		    } else {
		    	assertEquals(6, iSpringCount);
		    }    	
	    	assertEquals(7, iSpringBinCount);
	    	assertEquals(2, iSpringSconfigCount);
	    } 
	    	    
	    int  iSpringLibexecCount = verifySpringLibexecFolderFiles(); //3
	    assertEquals(1, iSpringLibexecCount);	
	    	    	    
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
  //1. Verify that the 5 folders terracotta-2.1.0/spring
  public int verifySpringFolderFiles() throws Exception {
     int iSpringFiles = 0;     
     String sSpringFolder = null;     
     System.out.println("************************************");
     sSpringFolder = System.getProperty("spring.dir");    
     System.out.println("files location="+sSpringFolder);
     File f = new File(sSpringFolder);
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());    
     
     File fArray[] = new File[0];
     fArray = f1.listFiles();      
     System.out.println("file array size="+fArray.length);
     
     if(fArray.length != 0){
       iSpringFiles = fArray.length;
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
     return iSpringFiles;       
  } 
  //2. Verify that the 7 files exists in the terracotta-2.1.0/spring/bin folder (which-boot.jar is removed for judah release build 1.3652)
  public int verifySpringBinFolderFiles() throws Exception {
		 int iSpringBinFiles = 0;		 
		 String sSpringBinFolder = null;		 	
		 System.out.println("************************************");
		 sSpringBinFolder = System.getProperty("springbin.dir");		 
		 System.out.println("files location="+sSpringBinFolder);
		 File f = new File(sSpringBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSpringBinFiles = fArray.length;
	       for(int i=0; i<fArray.length;i++){
	         if(fArray[i].isDirectory() == true){
	           System.out.println("folder name="+fArray[i].getName());
	         }else {
	           System.out.println("file name="+fArray[i].getName());
	         }
	       }
	     }
		 System.out.println("************************************");
		 return iSpringBinFiles;		 	  
  }   
  //3. Verify that the 1 file exists under terracotta-2.1.0/spring/libexec 
  public int verifySpringLibexecFolderFiles() throws Exception { 
		 int iSpringLibexecFolderFiles = 0;		 
		 String sSpringLibexecFolder = null;		 
		 System.out.println("************************************");
		 sSpringLibexecFolder = System.getProperty("springlibexec.dir");
		 System.out.println("files location="+sSpringLibexecFolder);
		 File f = new File(sSpringLibexecFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSpringLibexecFolderFiles = fArray.length;
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
		return iSpringLibexecFolderFiles;
  }   
  //4. Verify that the 1 fileexists under terracotta-2.1.0\spring\config-sample folder
  public int verifySpringSconfigFolderFiles() throws Exception { 		 
		 int iSpringSconfigFolderFiles = 0;		 
		 String sSpringSconfigFolder = null;		 
		 System.out.println("************************************");
		 sSpringSconfigFolder = System.getProperty("springconfigs.dir");
		 System.out.println("files location="+sSpringSconfigFolder);
		 File f = new File(sSpringSconfigFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSpringSconfigFolderFiles = fArray.length;
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
		return iSpringSconfigFolderFiles;
  }  
  //5. Verify that the 4 folders and 5 files exists under terracotta-2.1.0\spring\samples folder
  public int verifySpringSamplesFolderFiles() throws Exception { 		 
		 int iSpringSamplesFolderFiles = 0;		 
		 String sSpringSamplesFolder = null;		 
		 System.out.println("************************************");
		 sSpringSamplesFolder = System.getProperty("springsamples.dir");
		 System.out.println("files location="+sSpringSamplesFolder);
		 File f = new File(sSpringSamplesFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSpringSamplesFolderFiles = fArray.length;
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
		return iSpringSamplesFolderFiles;
  }   
  //6. Verify that the 1 folders and 7 files exists under terracotta-2.1.0\Spring\Uninstall_Terracotta for Spring folder
  public int verifySpringUninstallFolderFiles() throws Exception { 		 
		 int iSpringUninstallFolderFiles = 0;		 
		 String sSpringUninstallFolder = null;		 
		 System.out.println("************************************");
		 sSpringUninstallFolder = System.getProperty("uninstall.dir");
		 System.out.println("files location="+sSpringUninstallFolder);
		 File f = new File(sSpringUninstallFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSpringUninstallFolderFiles = fArray.length;
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
		return iSpringUninstallFolderFiles;
  }
 //7. Verify that the 3 files exists under terracotta-2.1.0\Spring\Uninstall_Terracotta for Spring\resource folder
  public int verifySpringUninstallResourceFolderFiles() throws Exception { 		 
		 int iSpringUninstallResourceFolderFiles = 0;		 
		 String sSpringUninstallResourceFolder = null;		 
		 System.out.println("************************************");
		 sSpringUninstallResourceFolder = System.getProperty("uninstallreso.dir");
		 System.out.println("files location="+sSpringUninstallResourceFolder);
		 File f = new File(sSpringUninstallResourceFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iSpringUninstallResourceFolderFiles = fArray.length;
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
		return iSpringUninstallResourceFolderFiles;
  }
}