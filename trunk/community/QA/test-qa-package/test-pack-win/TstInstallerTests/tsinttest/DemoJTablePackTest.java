/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package tsinttest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class DemoJTablePackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(DemoJTablePackTest.class);
  }

  public void testForVerification() throws Exception {
	    boolean b = verifyJTableFolder();
	    assertTrue("Demo JTable folder Exists!", b);
	    int  iCount = verifyJTableFolderFiles();
	    //assertEquals(9, iCount);	
	    assertEquals(7, iCount); //modified on 7/21/06 for judah release
	    boolean s = verifyJTableSrcFolderFiles();
	    assertTrue("Demo JTable src folder files Exists!", s);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	Verify that the classes and src folder exists under demo/jtable folder
  public boolean verifyJTableFolder() throws Exception { 
	     boolean bJTableFolderExists = false;
		 String sJTableFolder = null;
		 String sJTableclassesFolder = null;
		 String sJTablesrcFolder = null;
		 
		 sJTableFolder = System.getProperty("jtabledir.value");
		 sJTableclassesFolder = System.getProperty("jtableclassesdir.value");
		 sJTablesrcFolder = System.getProperty("jtablesrcdir.value");
		 
		 System.out.println("************************************");
		 if(sJTableFolder != null) {		 
			 System.out.println("JTable folder exists:" + sJTableFolder);
			 bJTableFolderExists = true;
		 }
		 if(sJTableclassesFolder != null) {		 
			 System.out.println("JTable classes folder exists:" + sJTableclassesFolder);
			 bJTableFolderExists = true;
		 }
		 if(sJTablesrcFolder != null) {		 
			 System.out.println("JTable src folder exists:" + sJTablesrcFolder);
			 bJTableFolderExists = true;
		 }
		 System.out.println("************************************");
		 return bJTableFolderExists;	  
  } 
  
  //Verify that the 7 files exists under demo/jtable folder
  public int verifyJTableFolderFiles() throws Exception { 
	     int iJTableFolderFiles = 0;		 
		 String sJTableFolder = null;		 
		 System.out.println("************************************");
		 sJTableFolder = System.getProperty("jtabledir.value");
		 System.out.println("files location="+sJTableFolder);
		 File f = new File(sJTableFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJTableFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 System.out.println("file name="+fArray[i].getName()); 
			 }
		 }
		 System.out.println("************************************");
		return iJTableFolderFiles;	  
  } 
  
 //Verify that the TableDemo.java files exists under demo/jtable/src/demo/jtable folder
  public boolean verifyJTableSrcFolderFiles() throws Exception { 
	     boolean bJTableSrcFolderFilesExists = false;
		 String sJTablesrcfile = null;		 	 
		 
		 sJTablesrcfile = System.getProperty("jtablesrcfile.value");		 		 		 
		  
		 System.out.println("************************************");
		 if(sJTablesrcfile != null) {		 
			 System.out.println("JTable src folder file1:" + sJTablesrcfile);
			 bJTableSrcFolderFilesExists = true;
		 }		 
		 System.out.println("************************************");
		 return bJTableSrcFolderFilesExists;	  
  }  
}