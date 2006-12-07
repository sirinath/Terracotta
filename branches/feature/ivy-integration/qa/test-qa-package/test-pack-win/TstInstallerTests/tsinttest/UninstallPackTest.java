/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class UninstallPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(UninstallPackTest.class);
  }

  public void testForVerification1() throws Exception {
	    int iCount = verifyUninstallFolderFiles();
	    assertEquals(8, iCount);	    
	    int iResCount = verifyUninstallResFolderFiles();
	    assertEquals(3, iResCount);	    
  } 
  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  
  
  
 //Verify that the 1 folder and 7 files exists under /uninstall
  public int verifyUninstallFolderFiles() throws Exception {
		 int iFiles = 0;		 
		 String sFolder = null;		 	
		 System.out.println("************************************");
		 sFolder = System.getProperty("uninstalldir.value");		 
		 System.out.println("files location="+sFolder);
		 File f = new File(sFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iFiles;		 	  
  }  
  //Verify that the 3 files exists under /uninstall/resources
  public int verifyUninstallResFolderFiles() throws Exception {
		 int iResFiles = 0;		 
		 String sResFolder = null;		 	
		 System.out.println("************************************");
		 sResFolder = System.getProperty("uninstallresdir.value");		 
		 System.out.println("files location="+sResFolder);
		 File f = new File(sResFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iResFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iResFiles;		 	  
  }  
  
}