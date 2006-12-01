/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DocsTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DocsTest.class);
  }

  public void testForVerification() throws Exception {
	    boolean b = verifyDocFolder();
	    assertTrue("Docs folder Exists!", b);
	    int  iCount = verifyDocsFolderFiles();
	    //assertEquals(8, iCount);
        assertEquals(10, iCount);
	    boolean l = verifyLibexecFolder();
	    assertTrue("Libexec folder Exists!", l);
	    boolean lf = verifyLibexecFolderFiles();
	    assertTrue("Libexec folder file Exists!", lf);	
	    boolean s = verifySconfigFolder();
	    assertTrue("sample-config folder Exists!", s);	    
	    boolean sf = verifySconfigFolderFiles();
	    assertTrue("sample-config folder file Exists!", sf);  	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	Verify that the docs folder exists under tc2.0.0 folder
  public boolean verifyDocFolder() throws Exception { 
	     boolean bDocsFolderExists = false;
		 String sDocsFolder = null;		 
		 
		 sDocsFolder = System.getProperty("docsdir.value");		 
		 
		 System.out.println("************************************");
		 if(sDocsFolder != null) {		 
			 System.out.println("Docs folder exists:" + sDocsFolder);
			 bDocsFolderExists = true;
		 }		 
		 System.out.println("************************************");
		 return bDocsFolderExists;	  
  } 
  
  //Verify that the 1 folder and 9 files exists under /docs folder
  public int verifyDocsFolderFiles() throws Exception { 
	     int iDocsFolderFiles = 0;		 
		 String sDocsFolder = null;		 
		 System.out.println("************************************");
		 sDocsFolder = System.getProperty("docsdir.value");
		 System.out.println("files location="+sDocsFolder);
		 File f = new File(sDocsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDocsFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iDocsFolderFiles;	  
  }
  
  //Verify that the libexec folder exists under tc2.0.0 folder
  public boolean verifyLibexecFolder() throws Exception { 
	     boolean bLibexecFolderExists = false;
		 String sLibexecFolder = null;		 
		 
		 sLibexecFolder = System.getProperty("libexecdir.value");		 
		 
		 System.out.println("************************************");
		 if(sLibexecFolder != null) {		 
			 System.out.println("Libexec folder exists:" + sLibexecFolder);
			 bLibexecFolderExists = true;
		 }		 
		 System.out.println("************************************");
		 return bLibexecFolderExists;	  
  } 
  
 //Verify that the files exists under /libexec folder
  public boolean verifyLibexecFolderFiles() throws Exception { 
	     boolean bLibexecFolderFilesExists = false;		 
		 String sLibexecfile = null;				 
		 
		 sLibexecfile = System.getProperty("libexecfile.value");		 		 
		  
		 System.out.println("************************************");		 
		 if(sLibexecfile != null) {		 
			 System.out.println("Libexec folder file:" + sLibexecfile);
			 bLibexecFolderFilesExists = true;
		 }		 
		 System.out.println("************************************");
		 return bLibexecFolderFilesExists;	  
  }
  
 //	Verify that the sample-config folder exists under tc2.0.0 folder
  public boolean verifySconfigFolder() throws Exception { 
	     boolean bSconfigFolderExists = false;
		 String sSconfigFolder = null;		 
		 
		 sSconfigFolder = System.getProperty("sconfigdir.value");		 
		 
		 System.out.println("************************************");
		 if(sSconfigFolder != null) {		 
			 System.out.println("sample-config folder exists:" + sSconfigFolder);
			 bSconfigFolderExists = true;
		 }		 
		 System.out.println("************************************");
		 return bSconfigFolderExists;	  
  }
  
//Verify that the files exists under /sample-config folder
  public boolean verifySconfigFolderFiles() throws Exception { 
	     boolean bSconfigFolderFilesExists = false;		 
		 String sSconfigfile = null;				 
		 
		 sSconfigfile = System.getProperty("sconfigfile.value");		 		 
		  
		 System.out.println("************************************");		 
		 if(sSconfigfile != null) {		 
			 System.out.println("sample-config folder file:" + sSconfigfile);
			 bSconfigFolderFilesExists = true;
		 }		 
		 System.out.println("************************************");
		 return bSconfigFolderFilesExists;	  
  }
  
  
}