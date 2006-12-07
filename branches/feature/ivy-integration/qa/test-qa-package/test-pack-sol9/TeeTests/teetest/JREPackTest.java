/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package teetest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class JREPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(JREPackTest.class);
  }

  public void testForVerification() throws Exception {	    	    
	    int  iCount = verifyJREFolderFiles(); //1
	    assertEquals(8, iCount);
	    int  iJREBinCount = verifyJREBinFolderFiles(); //2
	    assertEquals(37, iJREBinCount);
	    int  iJREBinSparcvCount = verifyJREBinSparcvFolderFiles(); //3
	    assertEquals(32, iJREBinSparcvCount);
	    int  iJREIncludeCount = verifyJREIncludeFolderFiles(); //4
	    assertEquals(7, iJREIncludeCount);
	    int  iJREIncludeSolCount = verifyJREIncludeSolarisFolderFiles(); //5
	    assertEquals(2, iJREIncludeSolCount); 
	    int  iJREJRECount = verifyJREJREFolder(); //6
	    assertEquals(11, iJREJRECount);
	    int  iJREJRESystemPrefCount = verifyJREJRESystemPrefFolder(); //7
	    assertEquals(2, iJREJRESystemPrefCount);
	    int  iJREJREBinCount = verifyJREJREBinFolder(); //8
	    assertEquals(14, iJREJREBinCount);
	    int  iJREJREBinparcvCount = verifyJREJREBinSparcvFolder(); //9
	    assertEquals(10, iJREJREBinparcvCount);
        int iJREJREJavawsCount = verifyJREJREJavawsFolderFiles(); //10
        assertEquals(1, iJREJREJavawsCount);	    
	    int  iJREJRELibCount = verifyJREJRELibFolder(); //11
	    assertEquals(34, iJREJRELibCount);
	    int  iJREJRELibAudioCount = verifyJREJRELibAudioFolderFiles(); //12
	    assertEquals(1, iJREJRELibAudioCount);	    
	    int  iJREJRELibCmmCount = verifyJREJRELibCmmFolderFiles(); //13
	    assertEquals(5, iJREJRELibCmmCount);
	    int  iJREJRELibExtCount = verifyJREJRELibExtFolderFiles(); //new 14
	    assertEquals(4, iJREJRELibExtCount);  
	    int  iJREJRELibFontCount = verifyJREJRELibFontFolderFiles(); //15
	    assertEquals(9, iJREJRELibFontCount);
	    int  iJREJRELibImCount = verifyJREJRELibImFolderFiles(); //16
	    assertEquals(2, iJREJRELibImCount);
	    int  iJREJRELibImagesCount = verifyJREJRELibImagesFolderFiles(); //17
	    assertEquals(2, iJREJRELibImagesCount);
	    int  iJREJRELibImagesCursorsCount = verifyJREJRELibImagesCursorsFolderFiles(); //18
	    assertEquals(8, iJREJRELibImagesCursorsCount);
	    int  iJREJRELibImagesIconsCount = verifyJREJRELibImagesIconsFolderFiles(); //19
	    assertEquals(4, iJREJRELibImagesIconsCount);
	    int  iJREJRELibJavawsCount = verifyJREJRELibJavawsFolderFiles(); //20
	    assertEquals(13, iJREJRELibJavawsCount);
	    int  iJREJRELibLocaleCount = verifyJREJRELibLocaleFolderFiles(); //21
	    assertEquals(13, iJREJRELibLocaleCount);
	    int  iJREJRELibManageCount = verifyJREJRELibManageFolderFiles(); //22
	    assertEquals(4, iJREJRELibManageCount);
	    int  iJREJRELibSecureCount = verifyJREJRELibSecurFolderFiles(); //23
	    assertEquals(7, iJREJRELibSecureCount);
	    int  iJREJRELibSparcCount = verifyJREJRELibSparcFolderFiles(); //24
	    assertEquals(49, iJREJRELibSparcCount);
	    int  iJREJRELibSparcClientCount = verifyJREJRELibSparcClientFolderFiles(); //25
	    assertEquals(6, iJREJRELibSparcClientCount);
	    int  iJREJRELibSparcClient64Count = verifyJREJRELibSparcClient64FolderFiles(); //26
	    assertEquals(1, iJREJRELibSparcClient64Count);
	    int  iJREJRELibSparcHeadCount = verifyJREJRELibSparcHeadFolderFile(); //27
	    assertEquals(1, iJREJRELibSparcHeadCount);
	    int  iJREJRELibSparcMotifCount = verifyJREJRELibSparcMotifFolderFile(); //28
	    assertEquals(1, iJREJRELibSparcMotifCount);
	    int  iJREJRELibSparcNativeCount = verifyJREJRELibSparcNativeFolderFile(); //29
	    assertEquals(1, iJREJRELibSparcNativeCount);
	    int  iJREJRELibSparcServerCount = verifyJREJRELibSparcServerFolderFiles(); //30
	    assertEquals(5, iJREJRELibSparcServerCount);
	    int  iJREJRELibSparcServer64Count = verifyJREJRELibSparcServer64FolderFiles(); //31
	    assertEquals(1, iJREJRELibSparcServer64Count);
	    int  iJREJRELibSparcXawtCount = verifyJREJRELibSparcXawtFolderFile(); //32
	    assertEquals(1, iJREJRELibSparcXawtCount);	    
	    int  iJREJRELibSparcv9Count = verifyJREJRELibSparcv9FolderFiles(); //33
	    assertEquals(43, iJREJRELibSparcv9Count); 
	    int  iJREJRELibSparcv9HeadCount = verifyJREJRELibSparcv9HeadFolderFile(); //34
	    assertEquals(1, iJREJRELibSparcv9HeadCount);
	    int  iJREJRELibSparcv9MotifCount = verifyJREJRELibSparcv9MotifFolderFile(); //35
	    assertEquals(1, iJREJRELibSparcv9MotifCount);
	    int  iJREJRELibSparcv9NativeCount = verifyJREJRELibSparcv9NativeFolderFile(); //36
	    assertEquals(1, iJREJRELibSparcv9NativeCount);
	    int  iJREJRELibSparcv9ServerCount = verifyJREJRELibSparcv9ServerFolderFiles(); //37
	    assertEquals(4, iJREJRELibSparcv9ServerCount);	    
	    int  iJREJRELibSparcv9XawtCount = verifyJREJRELibSparcv9XawtFolderFile(); //38
	    assertEquals(1, iJREJRELibSparcv9XawtCount);
	    int  iJREJRELibZiCount = verifyJREJRELibZiFolderFile(); //39
	    assertEquals(16, iJREJRELibZiCount);
	    int  iJREJREPluginCount = verifyJREJREPluginFolderFile(); //40
	    assertEquals(2, iJREJREPluginCount);
	    int  iJREJREPluginDesktopCount = verifyJREJREPluginDesktopFolderFile(); //41
	    assertEquals(2, iJREJREPluginDesktopCount);
	    int  iJREJREPluginSparcCount = verifyJREJREPluginSparcFolderFile(); //42
	    assertEquals(2, iJREJREPluginSparcCount);
	    int  iJREJREPluginSparcNs4Count = verifyJREJREPluginSparcNs4FolderFile(); //43
	    assertEquals(1, iJREJREPluginSparcNs4Count);
	    int  iJREJREPluginSparcNs7Count = verifyJREJREPluginSparcNs7FolderFile(); //44
	    assertEquals(1, iJREJREPluginSparcNs7Count);	    
	    int  iJRELibCount = verifyJRELibFolder(); //45
	    assertEquals(7, iJRELibCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }    
  //1. Verify that the jre folder exists under tc2.0.x folder 
  //Verify that the 4 folders and 4 files exists under /jre
  public int verifyJREFolderFiles() throws Exception { 
	  int iJREFiles = 0;		 
		 String sJREFolder = null;		 	
		 System.out.println("************************************");
		 sJREFolder = System.getProperty("jredir.value");		 
		 System.out.println("files location="+sJREFolder);
		 File f = new File(sJREFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREFiles = fArray.length;
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
		 return iJREFiles;	  
  } 
  //2. Verify that the 37 files exists under /jre/bin folder
  public int verifyJREBinFolderFiles() throws Exception { 
	  	 int iJREBinFiles = 0;		 
		 String sJREBinFolder = null;		 	
		 System.out.println("************************************");
		 sJREBinFolder = System.getProperty("jrebindir.value");		 
		 System.out.println("files location="+sJREBinFolder);
		 File f = new File(sJREBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREBinFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+ i + " " + fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+ i + " " + fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREBinFiles;	  
  }
  //3. Verify that the 32 files exists under /jre/bin/sparcv9 folder
  public int verifyJREBinSparcvFolderFiles() throws Exception { 
	  	 int iJREBinSparcvFiles = 0;		 
		 String sJREBinSparcvFolder = null;		 	
		 System.out.println("************************************");
		 sJREBinSparcvFolder = System.getProperty("jrebinsparcvdir.value");		 
		 System.out.println("files location="+sJREBinSparcvFolder);
		 File f = new File(sJREBinSparcvFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREBinSparcvFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+ i + " " + fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+ i + " " + fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREBinSparcvFiles;	  
  } 
  //4. Verify that the 1 folder and 6 files exists under /jre/include folder
  public int verifyJREIncludeFolderFiles() throws Exception { 
	  	 int iJREIncludeFiles = 0;		 
		 String sJREIncludeFolder = null;		 	
		 System.out.println("************************************");
		 sJREIncludeFolder = System.getProperty("jreincludedir.value");		 
		 System.out.println("files location="+sJREIncludeFolder);
		 File f = new File(sJREIncludeFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREIncludeFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+ i + " " + fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+ i + " " + fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREIncludeFiles;	  
  }
  //5. Verify that the 2 files exists under /jre/include/solaris folder
  public int verifyJREIncludeSolarisFolderFiles() throws Exception { 
	  	 int iJREIncludeSolarisFiles = 0;		 
		 String sJREIncludeSolarisFolder = null;		 	
		 System.out.println("************************************");
		 sJREIncludeSolarisFolder = System.getProperty("jreincludesolarisdir.value");		 
		 System.out.println("files location="+sJREIncludeSolarisFolder);
		 File f = new File(sJREIncludeSolarisFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREIncludeSolarisFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+ i + " " + fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+ i + " " + fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREIncludeSolarisFiles;	  
  }  
  //6. Verify that the 5 folders and 6 files exists under /jre/jre folder
  public int verifyJREJREFolder() throws Exception {
		 int iJREJREFiles = 0;		 
		 String sJREJREFolder = null;		 	
		 System.out.println("************************************");
		 sJREJREFolder = System.getProperty("jrejredir.value");		 
		 System.out.println("files location="+sJREJREFolder);
		 File f = new File(sJREJREFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJREFiles = fArray.length;
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
		 return iJREJREFiles;		 	  
  }  
  //7. Verify that the 2 files exists under /jre/jre/.systemPrefs folder
  public int verifyJREJRESystemPrefFolder() throws Exception {
		 int iJREJRESystemPrefFiles = 0;		 
		 String sJREJRESystemPrefFolder = null;		 	
		 System.out.println("************************************");
		 sJREJRESystemPrefFolder = System.getProperty("jrejresystemprefdir.value");		 
		 System.out.println("files location="+sJREJRESystemPrefFolder);
		 File f = new File(sJREJRESystemPrefFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRESystemPrefFiles = fArray.length;
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
		 return iJREJRESystemPrefFiles;		 	  
  }
  //8. Verify that the 1 folder and 13 files exists under /jre/jre/bin folder
  public int verifyJREJREBinFolder() throws Exception {
		 int iJREJREBinFiles = 0;		 
		 String sJREJREBinFolder = null;		 	
		 System.out.println("************************************");
		 sJREJREBinFolder = System.getProperty("jrejrebindir.value");		 
		 System.out.println("files location="+sJREJREBinFolder);
		 File f = new File(sJREJREBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJREBinFiles = fArray.length;
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
		 return iJREJREBinFiles;		 	  
  }
  //9. Verify that the 10 files exists under /jre/jre/bin/sparcv9 folder
  public int verifyJREJREBinSparcvFolder() throws Exception {
		 int iJREJREBinSparcvFiles = 0;		 
		 String sJREJREBinSparcvFolder = null;		 	
		 System.out.println("************************************");
		 sJREJREBinSparcvFolder = System.getProperty("jrejrebinsparcvdir.value");		 
		 System.out.println("files location="+sJREJREBinSparcvFolder);
		 File f = new File(sJREJREBinSparcvFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJREBinSparcvFiles = fArray.length;
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
		 return iJREJREBinSparcvFiles;		 	  
  }
  //10. Verify that the 1 file exists under /jre/jre/javaws folder       
  public int verifyJREJREJavawsFolderFiles() throws Exception {
                 int iJREJREJavawsFiles = 0;
                 String sJREJREJavawsFolder = null;
                 System.out.println("************************************");
                 sJREJREJavawsFolder = System.getProperty("jrejrejavawsdir.value");
                 System.out.println("files location="+sJREJREJavawsFolder);
                 File f = new File(sJREJREJavawsFolder);
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                	 iJREJREJavawsFiles = fArray.length;
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return  iJREJREJavawsFiles;
  }
  //11. Verify that the 14 folders and 20 files exists under /jre/jre/lib folder
  public int verifyJREJRELibFolder() throws Exception {
		 int iJREJRELibFiles = 0;		 
		 String sJREJRELibFolder = null;		 	
		 System.out.println("************************************");
		 sJREJRELibFolder = System.getProperty("jrejrelibdir.value");		 
		 System.out.println("files location="+sJREJRELibFolder);
		 File f = new File(sJREJRELibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibFiles = fArray.length;
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
		 return iJREJRELibFiles;		 	  
  }
  //12. Verify that 1 file exists under /jre/jre/lib/audio
  public int verifyJREJRELibAudioFolderFiles() throws Exception { 	     
	  	int iJREJRELibAudioFolderFiles = 0; 
	  	String sJREJRELibAudioFolder = null;	  	
	  	System.out.println("************************************"); 
	  	sJREJRELibAudioFolder = System.getProperty("jrejrelibaudiodir.value"); 
		System.out.println("files location="+sJREJRELibAudioFolder);
		File f = new File(sJREJRELibAudioFolder);
		System.out.println("get absolute path="+f.getAbsolutePath());
		File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibAudioFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibAudioFolderFiles;
  }
  //13. Verify that 5 files exists under /jre/jre/lib/cmm
  public int verifyJREJRELibCmmFolderFiles() throws Exception { 	     
	  	int iJREJRELibCmmFolderFiles = 0; 
	  	String sJREJRELibCmmFolder = null;	  	
	  	System.out.println("************************************"); 
	  	sJREJRELibCmmFolder = System.getProperty("jrejrelibcmmdir.value"); 
		System.out.println("files location="+sJREJRELibCmmFolder);
		File f = new File(sJREJRELibCmmFolder);
		System.out.println("get absolute path="+f.getAbsolutePath());
		File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibCmmFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibCmmFolderFiles;
  }
  //14. Verify that 4 files exists under /jre/jre/lib/ext 
  public int verifyJREJRELibExtFolderFiles() throws Exception { 	     
	  	int iJREJRELibExtFolderFiles = 0; 	  	
	  	String sJREJRELibExtFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibExtFolder = System.getProperty("jrejrelibextdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibExtFolder);
		 File f = new File(sJREJRELibExtFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibExtFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibExtFolderFiles;
  }  
  //15. Verify that 9 files exists under /jre/jre/lib/fonts 
  public int verifyJREJRELibFontFolderFiles() throws Exception { 	     
	  	int iJREJRELibFontFolderFiles = 0; 	  	
	  	String sJREJRELibFontFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibFontFolder = System.getProperty("jrejrelibfontsdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibFontFolder);
		 File f = new File(sJREJRELibFontFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibFontFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibFontFolderFiles;
  }
  //16. Verify that 2 files exists under /jre/jre/lib/im 
  public int verifyJREJRELibImFolderFiles() throws Exception { 	     
	  	int iJREJRELibImFolderFiles = 0; 	  	
	  	String sJREJRELibImFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibImFolder = System.getProperty("jrejrelibimdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibImFolder);
		 File f = new File(sJREJRELibImFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibImFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibImFolderFiles;
  }
  //17. Verify that 2 folders exists under /jre/jre/lib/images 
  public int verifyJREJRELibImagesFolderFiles() throws Exception { 	     
	  	int iJREJRELibImagesFolderFiles = 0; 	  	
	  	String sJREJRELibImagesFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibImagesFolder = System.getProperty("jrejrelibimagesdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibImagesFolder);
		 File f = new File(sJREJRELibImagesFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibImagesFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibImagesFolderFiles;
  }
  //18. Verify that 8 files exists under /jre/jre/lib/images/cursors 
  public int verifyJREJRELibImagesCursorsFolderFiles() throws Exception { 	     
	  	int iJREJRELibImagesCursorsFolderFiles = 0; 	  	
	  	String sJREJRELibImagesCursorsFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibImagesCursorsFolder = System.getProperty("jrejrelibimagescursorsdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibImagesCursorsFolder);
		 File f = new File(sJREJRELibImagesCursorsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibImagesCursorsFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibImagesCursorsFolderFiles;
  }  
  //19. Verify that 4 files exists under /jre/jre/lib/images/icons 
  public int verifyJREJRELibImagesIconsFolderFiles() throws Exception { 	     
	  	int iJREJRELibImagesIconsFolderFiles = 0; 	  	
	  	String sJREJRELibImagesIconsFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibImagesIconsFolder = System.getProperty("jrejrelibimagesiconsdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibImagesIconsFolder);
		 File f = new File(sJREJRELibImagesIconsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibImagesIconsFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibImagesIconsFolderFiles;
  }
  
 //20. Verify that 13 files exists under /jre/jre/lib/javaws
  public int verifyJREJRELibJavawsFolderFiles() throws Exception { 	     
	  	int iJREJRELibJavawsFolderFiles = 0; 	  	
	  	String sJREJRELibJavawsFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibJavawsFolder = System.getProperty("jrejrelibjavawsdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibJavawsFolder);
		 File f = new File(sJREJRELibJavawsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibJavawsFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibJavawsFolderFiles;
  }
  //21. Verify that 13 folders exists under /jre/jre/lib/locale
  public int verifyJREJRELibLocaleFolderFiles() throws Exception { 	     
	  	int iJREJRELibLocaleFolderFiles = 0; 	  	
	  	String sJREJRELibLocaleFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibLocaleFolder = System.getProperty("jrejreliblocaledir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibLocaleFolder);
		 File f = new File(sJREJRELibLocaleFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibLocaleFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibLocaleFolderFiles;
  }
  //22. Verify that 4 files exists under /jre/jre/lib/management
  public int verifyJREJRELibManageFolderFiles() throws Exception { 	     
	  	int iJREJRELibManageFolderFiles = 0; 	  	
	  	String sJREJRELibManageFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibManageFolder = System.getProperty("jrejrelibmanagedir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibManageFolder);
		 File f = new File(sJREJRELibManageFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibManageFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibManageFolderFiles;
  }
  //23. Verify that 7 files exists under /jre/jre/lib/security
  public int verifyJREJRELibSecurFolderFiles() throws Exception { 	     
	  	int iJREJRELibSecurFolderFiles = 0; 	  	
	  	String sJREJRELibSecurFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibSecurFolder = System.getProperty("jrejrelibsecuritydir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibSecurFolder);
		 File f = new File(sJREJRELibSecurFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibSecurFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibSecurFolderFiles;
  }  
  //24. Verify that 6 folders and 43 files exists under /jre/jre/lib/sparc
  public int verifyJREJRELibSparcFolderFiles() throws Exception { 	     
	  	int iJREJRELibSparcFolderFiles = 0; 	  	
	  	String sJREJRELibSparcFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibSparcFolder = System.getProperty("jrejrelibsparcdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibSparcFolder);
		 File f = new File(sJREJRELibSparcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibSparcFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibSparcFolderFiles;
  }
  //25. Verify that the 1 folder and 5 files exists in the /jre/jre/lib/sparc/client folder
  public int verifyJREJRELibSparcClientFolderFiles() throws Exception {
         int iJREJRELibSparcClientFiles = 0;
         String sJREJRELibSparcClientFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcClientFolder = System.getProperty("jrejrelibsparcclientdir.value");
         System.out.println("files location="+sJREJRELibSparcClientFolder);
         File f = new File(sJREJRELibSparcClientFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcClientFiles = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcClientFiles;
  }   
  //26. Verify that the 1 file exists in the /jre/jre/lib/sparc/client/64 folder
  public int verifyJREJRELibSparcClient64FolderFiles() throws Exception {
         int iJREJRELibSparcClient64Files = 0;
         String sJREJRELibSparcClient64Folder = null;
         System.out.println("************************************");
         sJREJRELibSparcClient64Folder = System.getProperty("jrejrelibsparcclient64dir.value");
         System.out.println("files location="+sJREJRELibSparcClient64Folder);
         File f = new File(sJREJRELibSparcClient64Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcClient64Files = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcClient64Files;
  }
  //27. Verify that the 1 file exists in the /jre/jre/lib/sparc/headless folder 
  public int verifyJREJRELibSparcHeadFolderFile() throws Exception {
         int iJREJRELibSparcHeadFiles = 0;
         String sJREJRELibSparcHeadFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcHeadFolder = System.getProperty("jrejrelibsparcheadlessdir.value");
         System.out.println("files location="+sJREJRELibSparcHeadFolder);
         File f = new File(sJREJRELibSparcHeadFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcHeadFiles = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcHeadFiles;
  }
  //28. Verify that the 1 file exists in the /jre/jre/lib/sparc/motif21 folder 
  public int verifyJREJRELibSparcMotifFolderFile() throws Exception {
         int iJREJRELibSparcMotifFiles = 0;
         String sJREJRELibSparcMotifFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcMotifFolder = System.getProperty("jrejrelibsparcmotifdir.value");  
         System.out.println("files location="+sJREJRELibSparcMotifFolder);      
         File f = new File(sJREJRELibSparcMotifFolder);      
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcMotifFiles = fArray.length;      
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcMotifFiles;      
  }  
  //29. Verify that the 1 file exists in the /jre/jre/lib/sparc/native_threads folder
  public int verifyJREJRELibSparcNativeFolderFile() throws Exception {
         int iJREJRELibSparcNativeFiles = 0;
         String sJREJRELibSparcNativeFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcNativeFolder = System.getProperty("jrejrelibsparcnativedir.value");
         System.out.println("files location="+sJREJRELibSparcNativeFolder);
         File f = new File(sJREJRELibSparcNativeFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcNativeFiles = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcNativeFiles;
  }  
  //30. Verify that the 1 folder and 4 files exists in the /jre/jre/lib/sparc/server folder
  public int verifyJREJRELibSparcServerFolderFiles() throws Exception {
         int iJREJRELibSparcServerFiles = 0;
         String sJREJRELibSparcServerFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcServerFolder = System.getProperty("jrejrelibsparcserverdir.value");
         System.out.println("files location="+sJREJRELibSparcServerFolder);
         File f = new File(sJREJRELibSparcServerFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcServerFiles = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcServerFiles;
  }
 //31. Verify that the 1 file exists in the /jre/jre/lib/sparc/server/64 folder
  public int verifyJREJRELibSparcServer64FolderFiles() throws Exception {
         int iJREJRELibSparcServer64Files = 0;
         String sJREJRELibSparcServer64Folder = null;
         System.out.println("************************************");
         sJREJRELibSparcServer64Folder = System.getProperty("jrejrelibsparcserver64dir.value");
         System.out.println("files location="+sJREJRELibSparcServer64Folder);
         File f = new File(sJREJRELibSparcServer64Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcServer64Files = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcServer64Files;
  }  
  //32. Verify that the 1 file exists in the /jre/jre/lib/sparc/xawt folder              
  public int verifyJREJRELibSparcXawtFolderFile() throws Exception {      
     int iJREJRELibSparcXawtFiles = 0;      
     String sJREJRELibSparcXawtFolder = null;      
     System.out.println("************************************");
     sJREJRELibSparcXawtFolder = System.getProperty("jrejrelibsparcxawtdir.value");      
     System.out.println("files location="+sJREJRELibSparcXawtFolder);      
     File f = new File(sJREJRELibSparcXawtFolder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJRELibSparcXawtFiles = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJRELibSparcXawtFiles;      
  }  
  //33. Verify that 5 folders and 38 files exists under /jre/jre/lib/sparcv9
  public int verifyJREJRELibSparcv9FolderFiles() throws Exception { 	     
	  	int iJREJRELibSparcvFolderFiles = 0; 	  	
	  	String sJREJRELibSparcvFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibSparcvFolder = System.getProperty("jrejrelibsparcv9dir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibSparcvFolder);
		 File f = new File(sJREJRELibSparcvFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibSparcvFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibSparcvFolderFiles;
  }  
  //34. Verify that 1 file exists under /jre/jre/lib/sparcv9/headless
  public int verifyJREJRELibSparcv9HeadFolderFile() throws Exception {
      int iJREJRELibSparcvHeadFiles = 0;
      String sJREJRELibSparcvHeadFolder = null;
      System.out.println("************************************");
      sJREJRELibSparcvHeadFolder = System.getProperty("jrejrelibsparcv9headlessdir.value");
      System.out.println("files location="+sJREJRELibSparcvHeadFolder);
      File f = new File(sJREJRELibSparcvHeadFolder);
      System.out.println("get absolute path="+f.getAbsolutePath());
      File f1 = new File(f.getAbsolutePath());

      File fArray[] = new File[0];
      fArray = f1.listFiles();
      System.out.println("file array size="+fArray.length);

      if(fArray.length != 0){
    	  iJREJRELibSparcvHeadFiles = fArray.length;
              for(int i=0; i<fArray.length;i++){
                     System.out.println("file name="+fArray[i].getName());
              }
      }
      System.out.println("************************************");
      return iJREJRELibSparcvHeadFiles;
  }   
  //35. Verify that the 1 file exists in the /jre/jre/lib/sparcv9/motif21 folder 
  public int verifyJREJRELibSparcv9MotifFolderFile() throws Exception {
         int iJREJRELibSparcvMotifFiles = 0;
         String sJREJRELibSparcvMotifFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcvMotifFolder = System.getProperty("jrejrelibsparcv9motifdir.value");  
         System.out.println("files location="+sJREJRELibSparcvMotifFolder);      
         File f = new File(sJREJRELibSparcvMotifFolder);      
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcvMotifFiles = fArray.length;      
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcvMotifFiles;      
  }  
  //36. Verify that the 1 file exists in the /jre/jre/lib/sparcv9/native_threads folder
  public int verifyJREJRELibSparcv9NativeFolderFile() throws Exception {
         int iJREJRELibSparcvNativeFiles = 0;
         String sJREJRELibSparcvNativeFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcvNativeFolder = System.getProperty("jrejrelibsparcv9nativedir.value");
         System.out.println("files location="+sJREJRELibSparcvNativeFolder);
         File f = new File(sJREJRELibSparcvNativeFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcvNativeFiles = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcvNativeFiles;
  }  
  //37. Verify that the 4 files exists in the /jre/jre/lib/sparcv9/server folder
  public int verifyJREJRELibSparcv9ServerFolderFiles() throws Exception {
         int iJREJRELibSparcvServerFiles = 0;
         String sJREJRELibSparcvServerFolder = null;
         System.out.println("************************************");
         sJREJRELibSparcvServerFolder = System.getProperty("jrejrelibsparcv9serverdir.value");
         System.out.println("files location="+sJREJRELibSparcvServerFolder);
         File f = new File(sJREJRELibSparcvServerFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJRELibSparcvServerFiles = fArray.length;
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREJRELibSparcvServerFiles;
  }  
  //38. Verify that the 1 file exists in the /jre/jre/lib/sparcv9/xawt folder              
  public int verifyJREJRELibSparcv9XawtFolderFile() throws Exception {      
     int iJREJRELibSparcvXawtFiles = 0;      
     String sJREJRELibSparcvXawtFolder = null;      
     System.out.println("************************************");
     sJREJRELibSparcvXawtFolder = System.getProperty("jrejrelibsparcv9xawtdir.value");      
     System.out.println("files location="+sJREJRELibSparcvXawtFolder);      
     File f = new File(sJREJRELibSparcvXawtFolder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJRELibSparcvXawtFiles = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJRELibSparcvXawtFiles;      
  }
  //39. Verify that the 10 folders and 6 files exists in the /jre/jre/lib/zi folder              
  public int verifyJREJRELibZiFolderFile() throws Exception {      
     int iJREJRELibZiFiles = 0;      
     String sJREJRELibZiFolder = null;      
     System.out.println("************************************");
     sJREJRELibZiFolder = System.getProperty("jrejrelibzidir.value");      
     System.out.println("files location="+sJREJRELibZiFolder);      
     File f = new File(sJREJRELibZiFolder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJRELibZiFiles = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJRELibZiFiles;      
  }
  //40. Verify that the 2 folders exists in the /jre/jre/plugin folder              
  public int verifyJREJREPluginFolderFile() throws Exception {      
     int iJREJREPluginFiles = 0;      
     String sJREJREPluginFolder = null;      
     System.out.println("************************************");
     sJREJREPluginFolder = System.getProperty("jrejreplugindir.value");      
     System.out.println("files location="+sJREJREPluginFolder);      
     File f = new File(sJREJREPluginFolder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJREPluginFiles = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJREPluginFiles;      
  }
  //41. Verify that the 2 files exists in the /jre/jre/plugin/desktop folder              
  public int verifyJREJREPluginDesktopFolderFile() throws Exception {      
     int iJREJREPluginDesktopFiles = 0;      
     String sJREJREPluginDesktopFolder = null;      
     System.out.println("************************************");
     sJREJREPluginDesktopFolder = System.getProperty("jrejreplugindesktopdir.value");      
     System.out.println("files location="+sJREJREPluginDesktopFolder);      
     File f = new File(sJREJREPluginDesktopFolder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJREPluginDesktopFiles = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJREPluginDesktopFiles;      
  }   
  //42. Verify that the 2 folders exists in the /jre/jre/plugin/sparc folder              
  public int verifyJREJREPluginSparcFolderFile() throws Exception {      
     int iJREJREPluginSparcFiles = 0;      
     String sJREJREPluginSparcFolder = null;      
     System.out.println("************************************");
     sJREJREPluginSparcFolder = System.getProperty("jrejrepluginsparcdir.value");      
     System.out.println("files location="+sJREJREPluginSparcFolder);      
     File f = new File(sJREJREPluginSparcFolder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJREPluginSparcFiles = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJREPluginSparcFiles;      
  }
  //43. Verify that the 1 file exists in the /jre/jre/plugin/sparc/ns4 folder              
  public int verifyJREJREPluginSparcNs4FolderFile() throws Exception {      
     int iJREJREPluginSparcNs4Files = 0;      
     String sJREJREPluginSparcNs4Folder = null;      
     System.out.println("************************************");
     sJREJREPluginSparcNs4Folder = System.getProperty("jrejrepluginsparcns4dir.value");      
     System.out.println("files location="+sJREJREPluginSparcNs4Folder);      
     File f = new File(sJREJREPluginSparcNs4Folder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJREPluginSparcNs4Files = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJREPluginSparcNs4Files;      
  }  
  //44. Verify that the 1 file exists in the /jre/jre/plugin/sparc/ns7 folder              
  public int verifyJREJREPluginSparcNs7FolderFile() throws Exception {      
     int iJREJREPluginSparcNs7Files = 0;      
     String sJREJREPluginSparcNs7Folder = null;      
     System.out.println("************************************");
     sJREJREPluginSparcNs7Folder = System.getProperty("jrejrepluginsparcns7dir.value");      
     System.out.println("files location="+sJREJREPluginSparcNs7Folder);      
     File f = new File(sJREJREPluginSparcNs7Folder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJREJREPluginSparcNs7Files = fArray.length;      
             for(int i=0; i<fArray.length;i++){
                    System.out.println("file name="+fArray[i].getName());
             }
     }
     System.out.println("************************************");
     return iJREJREPluginSparcNs7Files;      
  }
  
  //45. Verify that the 7 files exists under /jre/lib folder 
  public int verifyJRELibFolder() throws Exception {
		 int iJRELibFiles = 0;		 
		 String sJRELibFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibFolder = System.getProperty("jrelibdir.value");		 
		 System.out.println("files location="+sJRELibFolder);
		 File f = new File(sJRELibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibFiles = fArray.length;
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
		 return iJRELibFiles;	 	  
  }    
}  
  


  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
