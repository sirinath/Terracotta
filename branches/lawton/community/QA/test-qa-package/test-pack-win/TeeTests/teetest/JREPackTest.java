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
	    assertEquals(19, iCount);
	    int  iBinCount = verifyJREBinFolderFiles(); //2
	    assertEquals(69, iBinCount);
	    int  iBinClientCount = verifyJREBinClientFolderFiles(); //3
	    assertEquals(3, iBinClientCount);
	    int  iJREJRECount = verifyJREJREFolder(); //4
	    assertEquals(2, iJREJRECount);
	    int  iJREJREBinCount = verifyJREJREBinFolder(); //5
	    assertEquals(1, iJREJREBinCount);	    
	    int  iJREJREBinServerCount = verifyJREJREBinServerFolderFiles(); //6
	    assertEquals(2, iJREJREBinServerCount);	    
	    int  iJREJRELibCount = verifyJREJRELibFolder(); //7
	    assertEquals(3, iJREJRELibCount);
	    int  iJREJRELibAudioCount = verifyJREJRELibAudioFolderFiles(); //8
	    assertEquals(1, iJREJRELibAudioCount);
	    int  iJREJRELibCmmCount = verifyJREJRELibCmmFolderFiles(); //9
	    assertEquals(1, iJREJRELibCmmCount);
	    int  iJREJRELibFontCount = verifyJREJRELibFontFolderFiles(); //10
	    assertEquals(8, iJREJRELibFontCount);
	    int  iJRELibCount = verifyJRELibFolder(); //11
	    assertEquals(36, iJRELibCount);
	    int  iJRELibAudioCount = verifyJRELibAudioFolderFile(); //12
	    assertEquals(1, iJRELibAudioCount);
	    int  iJRELibCmmCount = verifyJRELibCmmFolderFiles(); //13
	    assertEquals(5, iJRELibCmmCount);	    
	    int  iJRELibExtCount = verifyJRELibExtFolderFiles(); //14
	    assertEquals(4, iJRELibExtCount);
	    int  iJRELibFontsCount = verifyJRELibfontsFolderFiles(); //15
	    assertEquals(8, iJRELibFontsCount);
	    int  iJRELibiCount = verifyJRELibiFolderFile(); //16
	    assertEquals(1, iJRELibiCount);	     
	    int  iJRELibImCount = verifyJRELibImFolderFiles(); //17
	    assertEquals(2, iJRELibImCount);
	    int  iJRELibImgCursCount = verifyJRELibImgCursorsFolderFiles(); //18
	    assertEquals(8, iJRELibImgCursCount);
	    int  iJRELibJavawsCount = verifyJRELibJavawsFolderFiles(); //19
	    assertEquals(12, iJRELibJavawsCount);
	    int  iJRELibMangCount = verifyJRELibMangFolderFiles(); //20
	    assertEquals(4, iJRELibMangCount);
	    int  iJRELibSecurCount = verifyJRELibSecurFolderFiles(); //21
	    assertEquals(6, iJRELibSecurCount);
	    int  iJRELibziCount = verifyJRELibziFolder(); //22
	    assertEquals(16, iJRELibziCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }    
  //1. Verify that the jre folder exists under tc2.0.0 folder 
  //Verify that the 3 folders and 16 files exists under /jre
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
  //2. Verify that the 1 folder and 68 files exists under /jre/bin folder
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
  //3. Verify that the 3 files exists under /jre/bin/client folder
  public int verifyJREBinClientFolderFiles() throws Exception {
		 int iJREBinClientFiles = 0;		 
		 String sJREBinClientFolder = null;		 	
		 System.out.println("************************************");
		 sJREBinClientFolder = System.getProperty("jrebinclientdir.value");		 
		 System.out.println("files location="+sJREBinClientFolder);
		 File f = new File(sJREBinClientFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREBinClientFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJREBinClientFiles;		 	  
  }
  //4. Verify that the 2 folders exists under /jre/jre folder
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
  //5. Verify that the server folder exists under /jre/jre/bin
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
  //6. Verify that the 2 files exists under /jre/jre/bin/server
  public int verifyJREJREBinServerFolderFiles() throws Exception { 	     
	  	int iJREJREBinServerFiles = 0; 	  	
		String sJREJREBinServerFolder = null;
		System.out.println("************************************");
		sJREJREBinServerFolder = System.getProperty("jrejrebinserverdir.value");		 	 
		 System.out.println("files location="+sJREJREBinServerFolder);
		 File f = new File(sJREJREBinServerFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJREBinServerFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJREJREBinServerFiles;
  }  
  //7. Verify that the 3 folders exists under /jre/jre/lib folder
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
  //8. Verify that 1 file exists under /jre/jre/lib/audio
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
  //9. Verify that 1 file exists under /jre/jre/lib/cmm
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
 //10. Verify that 8 files exists under /jre/jre/lib/fonts
  public int verifyJREJRELibFontFolderFiles() throws Exception { 	     
	  	int iJREJRELibFontsFolderFiles = 0; 	  	
	  	String sJREJRELibFontsFolder = null;
	  	System.out.println("************************************");	  	
	  	sJREJRELibFontsFolder = System.getProperty("jrejrelibfontsdir.value");		 	 	 
		 System.out.println("files location="+sJREJRELibFontsFolder);
		 File f = new File(sJREJRELibFontsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibFontsFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibFontsFolderFiles;
  }
  //11. Verify that the 12 folders and 24 files exists under /jre/lib folder
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
  //12. Verify that the 1 file exists in the jre/lib/audio folder
  public int verifyJRELibAudioFolderFile() throws Exception { 
		 int iJRELibAudioFolderFiles = 0;		 
		 String sJRELibAudioFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibAudioFolder = System.getProperty("jrelibaudiodir.value");		 
		 System.out.println("files location="+sJRELibAudioFolder);
		 File f = new File(sJRELibAudioFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibAudioFolderFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibAudioFolderFiles;
  }
 //13. Verify that the 5 files exists under /jre/lib/cmm folder
  public int verifyJRELibCmmFolderFiles() throws Exception {
		 int iJRELibCmmFiles = 0;		 
		 String sJRELibCmmFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibCmmFolder = System.getProperty("jrelibcmmdir.value");		 
		 System.out.println("files location="+sJRELibCmmFolder);
		 File f = new File(sJRELibCmmFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibCmmFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibCmmFiles;		 	  
  }
 //14. Verify that the 4 files exists under /jre/lib/ext folder
  public int verifyJRELibExtFolderFiles() throws Exception {
		 int iJRELibExtFiles = 0;		 
		 String sJRELibExtFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibExtFolder = System.getProperty("jrelibextdir.value");		 
		 System.out.println("files location="+sJRELibExtFolder);
		 File f = new File(sJRELibExtFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibExtFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibExtFiles;		 	  
  }
  //15. Verify that the 8 files exists under /jre/lib/fonts folder
  public int verifyJRELibfontsFolderFiles() throws Exception {
		 int iJRELibfontsFiles = 0;		 
		 String sJRELibfontsFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibfontsFolder = System.getProperty("jrelibfontsdir.value");		 
		 System.out.println("files location="+sJRELibfontsFolder);
		 File f = new File(sJRELibfontsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibfontsFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibfontsFiles;		 	  
  }
 //16. Verify that the 1 file exists in the jre/lib/i386 folder
  public int verifyJRELibiFolderFile() throws Exception {
		 int iJRELibiFiles = 0;		 
		 String sJRELibiFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibiFolder = System.getProperty("jrelibi386dir.value");		 
		 System.out.println("files location="+sJRELibiFolder);
		 File f = new File(sJRELibiFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibiFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibiFiles;
  }
  //17. Verify that the 2 files exists under /jre/lib/im folder
  public int verifyJRELibImFolderFiles() throws Exception {
		 int iJRELibImFiles = 0;		 
		 String sJRELibImFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibImFolder = System.getProperty("jrelibimdir.value");		 
		 System.out.println("files location="+sJRELibImFolder);
		 File f = new File(sJRELibImFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibImFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibImFiles;		 	  
  }
  //18. Verify that the 8 files exists under /jre/lib/images/cursors folder
  public int verifyJRELibImgCursorsFolderFiles() throws Exception {
		 int iJRELibImgCursorsFiles = 0;		 
		 String sJRELibImgCursorsFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibImgCursorsFolder = System.getProperty("jrelibimgcursorsdir.value");		 
		 System.out.println("files location="+sJRELibImgCursorsFolder);
		 File f = new File(sJRELibImgCursorsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibImgCursorsFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibImgCursorsFiles;		 	  
  }
 //19. Verify that the 12 files exists under /jre/lib/javaws folder
  public int verifyJRELibJavawsFolderFiles() throws Exception {
		 int iJRELibJavawsFiles = 0;		 
		 String sJRELibJavawsFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibJavawsFolder = System.getProperty("jrelibjavawdir.value");		 
		 System.out.println("files location="+sJRELibJavawsFolder);
		 File f = new File(sJRELibJavawsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibJavawsFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibJavawsFiles;		 	  
  }
  //20. Verify that the 4 files exists under /jre/lib/management folder
  public int verifyJRELibMangFolderFiles() throws Exception {
		 int iJRELibMangFiles = 0;		 
		 String sJRELibMangFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibMangFolder = System.getProperty("jrelibmanagdir.value");		 
		 System.out.println("files location="+sJRELibMangFolder);
		 File f = new File(sJRELibMangFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibMangFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibMangFiles;		 	  
  }
  //21. Verify that the 6 files exists under /jre/lib/security folder
  public int verifyJRELibSecurFolderFiles() throws Exception {
		 int iJRELibSecurFiles = 0;		 
		 String sJRELibSecurFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibSecurFolder = System.getProperty("jrelibsecurdir.value");		 
		 System.out.println("files location="+sJRELibSecurFolder);
		 File f = new File(sJRELibSecurFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibSecurFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibSecurFiles;		 	  
  }
  //22. Verify that the 10 folders and 6 files exists under /jre/lib/zi folder
  public int verifyJRELibziFolder() throws Exception {
		 int iJRELibziFiles = 0;		 
		 String sJRELibziFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibziFolder = System.getProperty("jrelibzidir.value");		 
		 System.out.println("files location="+sJRELibziFolder);
		 File f = new File(sJRELibziFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibziFiles = fArray.length;
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
		 return iJRELibziFiles;		 	  
  }  
}  
  


  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
