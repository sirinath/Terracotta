/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class UninstallTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(UninstallTest.class);
  }

  public void testForVerification() throws Exception {
	    String os = System.getProperty("os.name");
	    System.out.println(os);
	    
	    
	    if (os != null && os.startsWith("Win")){
	    	if (verifyTEEFolderExists() == true){
	    		int  iUninstallCount = verifyUninstallFolderFiles(System.getProperty("teeuninstall.dir")); 
			    assertEquals(8, iUninstallCount);
			    int  iUninstallResCount = verifyUninstallResourceFolderFiles(System.getProperty("teeuninstallreso.dir")); 
			    assertEquals(3, iUninstallResCount);  
		    } else {
		    	int  iUninstallCount = verifyUninstallFolderFiles(System.getProperty("spruninstall.dir")); 
			    assertEquals(8, iUninstallCount);
			    int  iUninstallResCount = verifyUninstallResourceFolderFiles(System.getProperty("spruninstallreso.dir")); 
			    assertEquals(3, iUninstallResCount);
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
  // Verify that the 1 folders and 7 files exists under terracotta-2.1.0/uninstall
  public int verifyUninstallFolderFiles(String s) throws Exception { 		 
		 int iUninstallFolderFiles = 0;		 
		 String sUninstallFolder = null;		 
		 System.out.println("************************************");
		 sUninstallFolder = s;
		 System.out.println("files location="+sUninstallFolder);
		 File f = new File(sUninstallFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iUninstallFolderFiles = fArray.length;
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
		return iUninstallFolderFiles;
  }
 // Verify that the 3 files exists under terracotta-2.1.0/uninstall/resource folder
  public int verifyUninstallResourceFolderFiles(String s) throws Exception { 		 
		 int iUninstallResourceFolderFiles = 0;		 
		 String sUninstallResourceFolder = null;		 
		 System.out.println("************************************");
		 sUninstallResourceFolder = s;
		 System.out.println("files location="+sUninstallResourceFolder);
		 File f = new File(sUninstallResourceFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iUninstallResourceFolderFiles = fArray.length;
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
		return iUninstallResourceFolderFiles;
  }
}