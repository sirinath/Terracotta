/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package teetest;

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
	    int  iCount = verifyJTableFolderFiles();
	    //assertEquals(9, iCount); //irving release
	    assertEquals(7, iCount); //judah release
	    int  iSrcCount = verifyJTableSrcFolderFiles();
	    assertEquals(1, iSrcCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }    
  
  //Verify that the 9 files (2 folders and 7 files)exists under demo/jtable folder //irving release
  //Verify that the 2 folders and 5 files exists under demo/jtable folder //judah release (.classpath and .project files does not exist)
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }		 
		 System.out.println("************************************");
		 return iJTableFolderFiles;	  
  }   
 //Verify that the TableDemo.java 1 file exists under demo/jtable/src/demo/jtable folder
  public int verifyJTableSrcFolderFiles() throws Exception {		 
		 int iJTableSrcFiles = 0;		 
		 String sJTableSrcFolder = null;		 	
		 System.out.println("************************************");
		 sJTableSrcFolder = System.getProperty("jtablesrcfile.value");		 
		 System.out.println("files location="+sJTableSrcFolder);
		 File f = new File(sJTableSrcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJTableSrcFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJTableSrcFiles;
  }  
  

}