/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class LibPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(LibPackTest.class);
  }

  public void testForVerification() throws Exception {	    
	    int  iLibCount = verifyLibFolder(System.getProperty("libdir.value"));
	    //assertEquals(41, iLibCount);
        assertEquals(37, iLibCount);
	    int  iLibSubCount = verifyLibSubFolderFiles(System.getProperty("libdsodir.value"),System.getProperty("libtomcatdir.value"),System.getProperty("libtomcatdirfile.value"),System.getProperty("libweblogicdir.value"),System.getProperty("libweblogicdirfile.value"));
	    assertEquals(2, iLibSubCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  } 

//Verify that the 3 folders and 34 files exists under /com/lib
  public int verifyLibFolder(String s) throws Exception {
		 int iLibFiles = 0;		 
		 String sLibFolder = s;		 	
		 System.out.println("************************************");			 
		 System.out.println("files location="+sLibFolder);
		 File f = new File(sLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iLibFiles = fArray.length;
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
		 return iLibFiles;		 	  
  }
//Verify that the 2 files exists under /com/lib/dso-boot
  //Verify that the 1 file exists under /com/lib/tomcat
//Verify that the 1 file exists under /com/lib/weblogic
  public int verifyLibSubFolderFiles(String s1, String s2, String s3, String s4, String s5) throws Exception {
		 int iLibDsoBootFiles = 0;		 
		 String sLibDsoBootFolder = s1;
		 String sLibTomcatFolder = s2;
		 String sLibTomcatFolderFile = s3;
		 String sLibWeblogicFolder = s4;
		 String sLibWeblogicFolderFile = s5;
		 
		 System.out.println("************************************"); 		 
		 System.out.println("files location="+sLibDsoBootFolder);
		 File f = new File(sLibDsoBootFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iLibDsoBootFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				 
			 }
		 }
		 if(sLibTomcatFolder != null) {		 
			 System.out.println("Eclipse com/lib/tomcat folder exists:" + sLibTomcatFolder);			 
		 }
		 if(sLibTomcatFolderFile != null) {		 
			 System.out.println("Eclipse com/lib/tomcat folder file exists:" + sLibTomcatFolderFile);			 
		 }
		 if(sLibWeblogicFolder != null) {		 
			 System.out.println("Eclipse com/lib/weblogic folder exists:" + sLibWeblogicFolder);			 
		 }
		 if(sLibWeblogicFolderFile != null) {		 
			 System.out.println("Eclipse com/lib/weblogic folder file exists:" + sLibWeblogicFolderFile);			 
		 }
		 System.out.println("************************************");
		 return iLibDsoBootFiles;		 	  
  }  
  
}