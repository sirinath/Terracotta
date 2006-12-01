/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class TEEJREPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(TEEJREPackTest.class);
  }

  public void testForVerification() throws Exception {
	    String os = System.getProperty("os.name");
	    System.out.println(os);	    	    
	    	    
	    if (os != null && os.startsWith("Lin")){
	    	int  iCount = verifyJREFolderFiles(); //1
	    	assertEquals(12, iCount); 
	    	int  iBinCount = verifyJREBinFolderFiles(); //2
	    	assertEquals(18, iBinCount); 
	    	int iJavawsCount = verifyJREJavawsFolderFiles(); //lin3
	    	assertEquals(1, iJavawsCount);
	    	int  iJREJRECount = verifyJREJREFolder();//4
	    	assertEquals(1, iJREJRECount);
	    	int  iJREJRELibCount = verifyJREJRELibFolder(); //5          
            assertEquals(3, iJREJRELibCount); 
            int  iJREJRELibCmmCount = verifyJREJRELibCmmFolderFiles(); //6
            assertEquals(1, iJREJRELibCmmCount); 
            int  iJRELibCount = verifyJRELibFolder(); //7
            assertEquals(53, iJRELibCount); 
            int  iJRELibCmmCount = verifyJRELibCmmFolderFiles(); //8
            assertEquals(5, iJRELibCmmCount); 
            int  iJRELibFontsCount = verifyJRELibfontsFolderFiles(); //9
            assertEquals(9, iJRELibFontsCount); 
            int  iJRELibiCount = verifyJRELibiFolderFile(); //lin10
            assertEquals(41, iJRELibiCount); 
            int  iJRELibiClientCount = verifyJRELibiClientFolderFile(); //lin11
            assertEquals(4, iJRELibiClientCount);
            int  iJRELibiHeadCount = verifyJRELibiHeadFolderFile(); //lin12
            assertEquals(1, iJRELibiHeadCount);	     
            int  iJRELibiMotifCount = verifyJRELibiMotifFolderFile(); //lin13
            assertEquals(1, iJRELibiMotifCount);
            int  iJRELibiNativeCount = verifyJRELibiNativeFolderFile(); //lin14   
            assertEquals(1, iJRELibiNativeCount);
            int  iJRELibiServerCount = verifyJRELibiServerFolderFile(); //lin15
            assertEquals(3, iJRELibiServerCount);
            int  iJRELibiXawtCount = verifyJRELibiXawtFolderFile(); //lin16
            assertEquals(1, iJRELibiXawtCount);
            int  iJRELibImgCount = verifyJRELibImgFolderFiles();//17
            assertEquals(2, iJRELibImgCount); 
            int  iJRELibImgIconCount = verifyJRELibImgIconFolderFiles(); //lin18
    	    assertEquals(4, iJRELibImgIconCount);
    	    int  iJRELibJavawsCount = verifyJRELibJavawsFolderFiles(); //19
    	    assertEquals(13, iJRELibJavawsCount);
    	    int  iJRELibObliqueCount = verifyJRELibObliqueFolderFiles(); //lin20
    	    assertEquals(5, iJRELibObliqueCount);
    	    int  iJREManCount = verifyJREManFolderFiles(); //lin21
    	    assertEquals(3, iJREManCount);
    	    int  iJREManJaCount = verifyJREManJaFolderFiles(); //lin22
    	    assertEquals(1, iJREManJaCount);
    	    int  iJREManJaMan1Count = verifyJREManJaMan1FolderFiles(); //lin23
    	    assertEquals(14, iJREManJaMan1Count);
    	    int  iJREManMan1Count = verifyJREManMan1FolderFiles(); //lin24
    	    assertEquals(14, iJREManMan1Count);
    	    int  iJREPluginCount = verifyJREPluginFolderFiles(); //lin25
            assertEquals(2, iJREPluginCount);
            int  iJREPluginDesktopCount = verifyJREPluginDesktopFolderFiles(); //lin26 
            assertEquals(2, iJREPluginDesktopCount);
            int  iJREPlugini386Count = verifyJREPlugini386FolderFiles(); //lin27
            assertEquals(2, iJREPlugini386Count);
            int  iJREPlugini386ns7Count = verifyJREPlugini386ns7FolderFiles(); //lin28
            assertEquals(1, iJREPlugini386ns7Count);
            int  iJREPlugini386ns7gccCount = verifyJREPlugini386ns7gccFolderFiles(); //lin29
            assertEquals(1, iJREPlugini386ns7gccCount); 
            int  iJRELibSecurCount = verifyJRELibSecurFolderFiles(); //30
    	    assertEquals(6, iJRELibSecurCount);
	    } else if(os != null && os.startsWith("Sun")) {
	    	System.out.println("here");
	    	int  iCount = verifyJREFolderFiles(); //1
	    	assertEquals(12, iCount); 
	    	int  iBinCount = verifyJREBinFolderFiles(); //2
	    	assertEquals(15, iBinCount); 
	    	int iJavawsCount = verifyJREJavawsFolderFiles(); //3
            assertEquals(1, iJavawsCount);             
            int  iJREJRECount = verifyJREJREFolder(); //4
            assertEquals(1, iJREJRECount); 
            int  iJREJRELibCount = verifyJREJRELibFolder(); //5
            assertEquals(3, iJREJRELibCount); 
            int  iJREJRELibCmmCount = verifyJREJRELibCmmFolderFiles(); //6
            assertEquals(1, iJREJRELibCmmCount);
            int  iJRELibCount = verifyJRELibFolder(); //7
            assertEquals(34, iJRELibCount);
            int  iJRELibCmmCount = verifyJRELibCmmFolderFiles();//8
            assertEquals(5, iJRELibCmmCount); 
            int  iJRELibFontsCount = verifyJRELibfontsFolderFiles(); //9
            assertEquals(9, iJRELibFontsCount);             
            int  iJRELibImgCount = verifyJRELibImgFolderFiles(); //10
            assertEquals(2, iJRELibImgCount); 
            int  iJRELibImgIconCount = verifyJRELibImgIconFolderFiles(); //11
    	    assertEquals(4, iJRELibImgIconCount);
    	    int  iJRELibJavawsCount = verifyJRELibJavawsFolderFiles(); //12
    	    assertEquals(13, iJRELibJavawsCount); 
    	    int  iJREManCount = verifyJREManFolderFiles(); //13
    	    assertEquals(4, iJREManCount);
    	    int  iJREManJCount = verifyJREManJFolderFiles(); //14
    	    assertEquals(1, iJREManJCount);    	        	    
    	    int  iJREManJaSman1Count = verifyJREManJaSman1FolderFiles(); //15
    	    assertEquals(12, iJREManJaSman1Count);    	    
    	    int  iJREManJaPCKCount = verifyJREManJaPCKFolderFiles(); //16
    	    assertEquals(1, iJREManJaPCKCount);    	        	    
    	    int  iJREManJapckSman1Count = verifyJREManJapckSman1FolderFiles(); //17
    	    assertEquals(12, iJREManJapckSman1Count);
    	    int  iJREManJaJpCount = verifyJREManJaJpolderFiles(); //18
    	    assertEquals(1, iJREManJaJpCount);    	        	    
    	    int  iJREManJaJpSman1Count = verifyJREManJaJpSman1FolderFiles(); //19
    	    assertEquals(12, iJREManJaJpSman1Count);    	    
    	    int  iJREManSman1Count = verifyJREManSman1FolderFiles(); //20
    	    assertEquals(12, iJREManSman1Count);    	    
    	    int  iJREPluginCount = verifyJREPluginFolderFiles(); //21
            assertEquals(2, iJREPluginCount);
            int  iJREPluginDesktopCount = verifyJREPluginDesktopFolderFiles(); //22
            assertEquals(2, iJREPluginDesktopCount);
            int  iJREPluginSparcCount = verifyJREPluginSparcFolderFiles(); //23
            assertEquals(2, iJREPluginSparcCount);
            int  iJREPluginSparcNs4Count = verifyJREPluginSparcNs4FolderFiles(); //24
            assertEquals(1, iJREPluginSparcNs4Count);
            int  iJREPluginSparcNs7Count = verifyJREPluginSparcNs7FolderFiles(); //25
            assertEquals(1, iJREPluginSparcNs7Count); 
            int  iJRELibSecurCount = verifyJRELibSecurFolderFiles(); //26
    	    assertEquals(7, iJRELibSecurCount);    	    
    	    int  iJRELibSparcCount = verifyJRELibSparcFolderFiles(); //27
    	    assertEquals(48, iJRELibSparcCount);
    	    int  iJRELibSparcClientCount = verifyJRELibSparcClientFolderFiles(); //28
    	    assertEquals(6, iJRELibSparcClientCount);
    	    int  iJRELibSparcClient64Count = verifyJRELibSparcClient64FolderFiles(); //29
    	    assertEquals(1, iJRELibSparcClient64Count);
    	    int  iJRELibSparcHeadCount = verifyJRELibSparcHeadFolderFile(); //30
    	    assertEquals(1, iJRELibSparcHeadCount);
    	    int  iJRELibSparcMotifCount = verifyJRELibSparcMotifFolderFile(); //31
    	    assertEquals(1, iJRELibSparcMotifCount);
    	    int  iJRELibSparcNativeCount = verifyJRELibSparcNativeFolderFile(); //32
    	    assertEquals(1, iJRELibSparcNativeCount);
    	    int  iJRELibSparcServerCount = verifyJRELibSparcServerFolderFiles(); //33
    	    assertEquals(5, iJRELibSparcServerCount);
    	    int  iJRELibSparcServer64Count = verifyJRELibSparcServer64FolderFiles(); //34
    	    assertEquals(1, iJRELibSparcServer64Count);
    	    int  iJRELibSparcXawtCount = verifyJRELibSparcXawtFolderFile(); //35
    	    assertEquals(1, iJRELibSparcXawtCount);	
	    }else {
	    	int  iCount = verifyJREFolderFiles(); //1
	    	assertEquals(19, iCount);
	    	int  iBinCount = verifyJREBinFolderFiles();//2
	    	assertEquals(69, iBinCount);
	    	int  iBinClientCount = verifyJREBinClientFolderFiles(); //3
		    assertEquals(3, iBinClientCount);
		    int  iJREJRECount = verifyJREJREFolder(); //4
		    assertEquals(18, iJREJRECount);
		    int  iJREJREBinCount = verifyJREJREBinFolder(); //5
		    assertEquals(68, iJREJREBinCount);
		    int  iJREJREBinClientCount = verifyJREJREBinClientFolderFiles(); //6
		    assertEquals(3, iJREJREBinClientCount);	    
		    int  iJREJREBinServerCount = verifyJREJREBinServerFolderFiles(); //7
		    assertEquals(2, iJREJREBinServerCount);
		    int  iJREJRELibCount = verifyJREJRELibFolder(); //8
		    assertEquals(4, iJREJRELibCount);
		    int  iJREJRELibCmmCount = verifyJREJRELibCmmFolderFiles();//10
		    assertEquals(5, iJREJRELibCmmCount);
		    int  iJREJRELibExtCount = verifyJREJRELibExtFolderFiles(); //11
		    assertEquals(4, iJREJRELibExtCount);		    
		    int  iJRELibCount = verifyJRELibFolder();//13
		    assertEquals(35, iJRELibCount);
		    int  iJRELibCmmCount = verifyJRELibCmmFolderFiles();//15
		    assertEquals(1, iJRELibCmmCount);
		    int  iJRELibFontsCount = verifyJRELibfontsFolderFiles(); //17
		    assertEquals(8, iJRELibFontsCount);		    		    
		    int  iJRELibiCount = verifyJRELibiFolderFile(); //18
		    assertEquals(1, iJRELibiCount);			    
		    int  iJRELibJavawsCount = verifyJRELibJavawsFolderFiles(); //21
		    assertEquals(12, iJRELibJavawsCount);
		    int  iJRELibImgCount = verifyJRELibImgFolderFiles();//25
		    assertEquals(1, iJRELibImgCount);
		    int  iJRELibSecurCount = verifyJRELibSecurFolderFiles(); //23
		    assertEquals(6, iJRELibSecurCount);
	    }	    	    	    	    	    	    	        	    
	    int  iJREJRELibAudioCount = verifyJREJRELibAudioFolderFiles(); //9
	    assertEquals(1, iJREJRELibAudioCount);	    	    	    
	    int  iJREJRELibFontCount = verifyJREJRELibFontFolderFiles(); //12
	    assertEquals(8, iJREJRELibFontCount);	    
	    int  iJRELibAudioCount = verifyJRELibAudioFolderFile(); //14
	    assertEquals(1, iJRELibAudioCount);	    	    
	    int  iJRELibExtCount = verifyJRELibExtFolderFiles(); //16
	    assertEquals(4, iJRELibExtCount);	    	         
	    int  iJRELibImCount = verifyJRELibImFolderFiles(); //19
	    assertEquals(2, iJRELibImCount);
	    int  iJRELibImgCursCount = verifyJRELibImgCursorsFolderFiles(); //20
	    assertEquals(8, iJRELibImgCursCount);	    	    
	    int  iJRELibMangCount = verifyJRELibMangFolderFiles(); //22
	    assertEquals(4, iJRELibMangCount);
	    //int  iJRELibSecurCount = verifyJRELibSecurFolderFiles(); //23
	    //assertEquals(6, iJRELibSecurCount);
	    int  iJRELibziCount = verifyJRELibziFolder(); //24
	    assertEquals(24, iJRELibziCount);	    
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
  //solaris:
  //Verify that 6 folders and 42 files exists under /jre/lib/sparc
  public int verifyJRELibSparcFolderFiles() throws Exception { 	     
	  	int iJRELibSparcFolderFiles = 0; 	  	
	  	String sJRELibSparcFolder = null;
	  	System.out.println("************************************");	  	
	  	sJRELibSparcFolder = System.getProperty("jrelibsparcdir.value");		 	 	 
		 System.out.println("files location="+sJRELibSparcFolder);
		 File f = new File(sJRELibSparcFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibSparcFolderFiles = fArray.length;
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
		 return iJRELibSparcFolderFiles;
  }
  // Verify that the 1 folder and 5 files exists in the /jre/lib/sparc/client folder
  public int verifyJRELibSparcClientFolderFiles() throws Exception {
         int iJRELibSparcClientFiles = 0;
         String sJRELibSparcClientFolder = null;
         System.out.println("************************************");
         sJRELibSparcClientFolder = System.getProperty("jrelibsparcclientdir.value");
         System.out.println("files location="+sJRELibSparcClientFolder);
         File f = new File(sJRELibSparcClientFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJRELibSparcClientFiles = fArray.length;
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
         return iJRELibSparcClientFiles;
  }   
  // Verify that the 1 file exists in the /jre/lib/sparc/client/64 folder
  public int verifyJRELibSparcClient64FolderFiles() throws Exception {
         int iJRELibSparcClient64Files = 0;
         String sJRELibSparcClient64Folder = null;
         System.out.println("************************************");
         sJRELibSparcClient64Folder = System.getProperty("jrelibsparcclient64dir.value");
         System.out.println("files location="+sJRELibSparcClient64Folder);
         File f = new File(sJRELibSparcClient64Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJRELibSparcClient64Files = fArray.length;
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
         return iJRELibSparcClient64Files;
  }
  // Verify that the 1 file exists in the /jre/lib/sparc/headless folder 
  public int verifyJRELibSparcHeadFolderFile() throws Exception {
         int iJRELibSparcHeadFiles = 0;
         String sJRELibSparcHeadFolder = null;
         System.out.println("************************************");
         sJRELibSparcHeadFolder = System.getProperty("jrelibsparcheadlessdir.value");
         System.out.println("files location="+sJRELibSparcHeadFolder);
         File f = new File(sJRELibSparcHeadFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJRELibSparcHeadFiles = fArray.length;
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
         return iJRELibSparcHeadFiles;
  }
  // Verify that the 1 file exists in the /jre/lib/sparc/motif21 folder 
  public int verifyJRELibSparcMotifFolderFile() throws Exception {
         int iJRELibSparcMotifFiles = 0;
         String sJRELibSparcMotifFolder = null;
         System.out.println("************************************");
         sJRELibSparcMotifFolder = System.getProperty("jrelibsparcmotifdir.value");  
         System.out.println("files location="+sJRELibSparcMotifFolder);      
         File f = new File(sJRELibSparcMotifFolder);      
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJRELibSparcMotifFiles = fArray.length;
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
         return iJRELibSparcMotifFiles;      
  }  
  // Verify that the 1 file exists in the /jre/lib/sparc/native_threads folder
  public int verifyJRELibSparcNativeFolderFile() throws Exception {
         int iJRELibSparcNativeFiles = 0;
         String sJRELibSparcNativeFolder = null;
         System.out.println("************************************");
         sJRELibSparcNativeFolder = System.getProperty("jrelibsparcnativedir.value");
         System.out.println("files location="+sJRELibSparcNativeFolder);
         File f = new File(sJRELibSparcNativeFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJRELibSparcNativeFiles = fArray.length;
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
         return iJRELibSparcNativeFiles;
  }  
  // Verify that the 1 folder and 4 files exists in the /jre/lib/sparc/server folder
  public int verifyJRELibSparcServerFolderFiles() throws Exception {
         int iJRELibSparcServerFiles = 0;
         String sJRELibSparcServerFolder = null;
         System.out.println("************************************");
         sJRELibSparcServerFolder = System.getProperty("jrelibsparcserverdir.value");
         System.out.println("files location="+sJRELibSparcServerFolder);
         File f = new File(sJRELibSparcServerFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJRELibSparcServerFiles = fArray.length;
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
         return iJRELibSparcServerFiles;
  }
  // Verify that the 1 file exists in the /jre/lib/sparc/server/64 folder
  public int verifyJRELibSparcServer64FolderFiles() throws Exception {
         int iJRELibSparcServer64Files = 0;
         String sJRELibSparcServer64Folder = null;
         System.out.println("************************************");
         sJRELibSparcServer64Folder = System.getProperty("jrelibsparcserver64dir.value");
         System.out.println("files location="+sJRELibSparcServer64Folder);
         File f = new File(sJRELibSparcServer64Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJRELibSparcServer64Files = fArray.length;
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
         return iJRELibSparcServer64Files;
  }  
  // Verify that the 1 file exists in the /jre/lib/sparc/xawt folder              
  public int verifyJRELibSparcXawtFolderFile() throws Exception {      
     int iJRELibSparcXawtFiles = 0;      
     String sJRELibSparcXawtFolder = null;      
     System.out.println("************************************");
     sJRELibSparcXawtFolder = System.getProperty("jrelibsparcxawtdir.value");      
     System.out.println("files location="+sJRELibSparcXawtFolder);      
     File f = new File(sJRELibSparcXawtFolder);      
     System.out.println("get absolute path="+f.getAbsolutePath());
     File f1 = new File(f.getAbsolutePath());

     File fArray[] = new File[0];
     fArray = f1.listFiles();
     System.out.println("file array size="+fArray.length);

     if(fArray.length != 0){
    	 iJRELibSparcXawtFiles = fArray.length;
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
     return iJRELibSparcXawtFiles;      
  }  
  //Verify that the 1 folder exists under /jre/man/ja folder       
  public int verifyJREManJFolderFiles() throws Exception {
         int iJREManJFiles = 0;
         String sJREManJFolder = null;
         System.out.println("************************************");
         sJREManJFolder = System.getProperty("jremanjdir.value");
         System.out.println("files location="+sJREManJFolder);
         File f = new File(sJREManJFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREManJFiles = fArray.length;
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
         return  iJREManJFiles;
  }
  // Verify that the 12 files exists under /jre/man/ja/sman1 folder       
  public int verifyJREManJaSman1FolderFiles() throws Exception {
         int iJREManJaSman1Files = 0;
         String sJREManJaSman1Folder = null;
         System.out.println("************************************");
         sJREManJaSman1Folder = System.getProperty("jremanjasman1dir.value");
         System.out.println("files location="+sJREManJaSman1Folder);
         File f = new File(sJREManJaSman1Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREManJaSman1Files = fArray.length;
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
         return  iJREManJaSman1Files;
  }
  //Verify that the 1 folder exists under /jre/man/ja_JP.PCK folder       
  public int verifyJREManJaPCKFolderFiles() throws Exception {
         int iJREManJaPCKFiles = 0;
         String sJREManJaPCKFolder = null;
         System.out.println("************************************");
         sJREManJaPCKFolder = System.getProperty("jremanjapckdir.value");
         System.out.println("files location="+sJREManJaPCKFolder);
         File f = new File(sJREManJaPCKFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREManJaPCKFiles = fArray.length;
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
         return  iJREManJaPCKFiles;
  } 
  // Verify that the 12 files exists under /jre/man/ja_JP.PCK/sman1 folder       
  public int verifyJREManJapckSman1FolderFiles() throws Exception {
         int iJREManJapckSman1Files = 0;
         String sJREManJapckSman1Folder = null;
         System.out.println("************************************");
         sJREManJapckSman1Folder = System.getProperty("jremanjapcksman1dir.value");
         System.out.println("files location="+sJREManJapckSman1Folder);
         File f = new File(sJREManJapckSman1Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREManJapckSman1Files = fArray.length;
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
         return  iJREManJapckSman1Files;
  }
  //Verify that the 1 folder exists under /jre/man/ja_JP.UTF-8 folder       
  public int verifyJREManJaJpolderFiles() throws Exception {
         int iJREManJaJpFiles = 0;
         String sJREManJaJpolder = null;
         System.out.println("************************************");
         sJREManJaJpolder = System.getProperty("jremanjajpdir.value");
         System.out.println("files location="+sJREManJaJpolder);
         File f = new File(sJREManJaJpolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREManJaJpFiles = fArray.length;
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
         return  iJREManJaJpFiles;
  } 
  // Verify that the 12 files exists under /jre/man/ja_JP.UTF-8/sman1 folder       
  public int verifyJREManJaJpSman1FolderFiles() throws Exception {
         int iJREManJaJpSman1Files = 0;
         String sJREManJaJpSman1Folder = null;
         System.out.println("************************************");
         sJREManJaJpSman1Folder = System.getProperty("jremanjajpsman1dir.value");
         System.out.println("files location="+sJREManJaJpSman1Folder);
         File f = new File(sJREManJaJpSman1Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREManJaJpSman1Files = fArray.length;
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
         return  iJREManJaJpSman1Files;
  }  
  //Verify that the 12 files exists under /jre/man/sman1 folder       
  public int verifyJREManSman1FolderFiles() throws Exception {
         int iJREManSman1Files = 0;
         String sJREManSman1Folder = null;
         System.out.println("************************************");
         sJREManSman1Folder = System.getProperty("jremansman1dir.value");
         System.out.println("files location="+sJREManSman1Folder);
         File f = new File(sJREManSman1Folder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREManSman1Files = fArray.length;
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
         return  iJREManSman1Files;
  }
  // Verify that the 2 folders  exists under /jre/plugin/sparc folder           
  public int verifyJREPluginSparcFolderFiles() throws Exception {                
         int iJREPluginSparcFiles = 0;        
         String sJREPluginSparcFolder = null;        
         System.out.println("************************************");
         sJREPluginSparcFolder = System.getProperty("jrepluginsparcdir.value");          
         System.out.println("files location="+sJREPluginSparcFolder);        
         File f = new File(sJREPluginSparcFolder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPluginSparcFiles = fArray.length;
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
         return iJREPluginSparcFiles;        
  } 
  //Verify that the 1 file  exists under /jre/plugin/sparc/ns4 folder           
  public int verifyJREPluginSparcNs4FolderFiles() throws Exception {                
         int iJREPluginSparcNs4Files = 0;        
         String sJREPluginSparcNs4Folder = null;        
         System.out.println("************************************");
         sJREPluginSparcNs4Folder = System.getProperty("jrepluginsparcns4dir.value");          
         System.out.println("files location="+sJREPluginSparcNs4Folder);        
         File f = new File(sJREPluginSparcNs4Folder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPluginSparcNs4Files = fArray.length;
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
         return iJREPluginSparcNs4Files;        
  }
  //Verify that the 1 file exists under /jre/plugin/sparc/ns7 folder           
  public int verifyJREPluginSparcNs7FolderFiles() throws Exception {                
         int iJREPluginSparcNs7Files = 0;        
         String sJREPluginSparcNs7Folder = null;        
         System.out.println("************************************");
         sJREPluginSparcNs7Folder = System.getProperty("jrepluginsparcns7dir.value");          
         System.out.println("files location="+sJREPluginSparcNs7Folder);        
         File f = new File(sJREPluginSparcNs7Folder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPluginSparcNs7Files = fArray.length;
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
         return iJREPluginSparcNs7Files;        
  }
  //linux:
  //lin3. Verify that the 1 file exists under /jre/javaws folder       
  public int verifyJREJavawsFolderFiles() throws Exception {
         int iJREJavawsFiles = 0;
         String sJREJavawsFolder = null;
         System.out.println("************************************");
         sJREJavawsFolder = System.getProperty("jrejavawsdir.value");
         System.out.println("files location="+sJREJavawsFolder);
         File f = new File(sJREJavawsFolder);
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREJavawsFiles = fArray.length;
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
         return  iJREJavawsFiles;
  }
  // lin11. Verify that the 4 files exists in the jre/lib/i386/client folder
  public int verifyJRELibiClientFolderFile() throws Exception {
                 int iJRELibiClientFiles = 0;
                 String sJRELibiClientFolder = null;
                 System.out.println("************************************");
                 sJRELibiClientFolder = System.getProperty("jrelibiclientdir.value");
                 System.out.println("files location="+sJRELibiClientFolder);
                 File f = new File(sJRELibiClientFolder);
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibiClientFiles = fArray.length;
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibiClientFiles;
  }
   
  //lin12. Verify that the 1 file exists in the jre/lib/i386/headless folder 
  public int verifyJRELibiHeadFolderFile() throws Exception {
                 int iJRELibiHeadFiles = 0;
                 String sJRELibiHeadFolder = null;
                 System.out.println("************************************");
                 sJRELibiHeadFolder = System.getProperty("jrelibiheadlessdir.value");
                 System.out.println("files location="+sJRELibiHeadFolder);
                 File f = new File(sJRELibiHeadFolder);
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibiHeadFiles = fArray.length;
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibiHeadFiles;
  }
  //lin13. Verify that the 1 file exists in the jre/lib/i386/motif21 folder 
  public int verifyJRELibiMotifFolderFile() throws Exception {
                 int iJRELibiMotifFiles = 0;
                 String sJRELibiMotifFolder = null;
                 System.out.println("************************************");
                 sJRELibiMotifFolder = System.getProperty("jrelibimotifdir.value");  
                 System.out.println("files location="+sJRELibiMotifFolder);      
                 File f = new File(sJRELibiMotifFolder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibiMotifFiles = fArray.length;      
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibiMotifFiles;      
  }  
  //lin14. Verify that the 1 file exists in the jre/lib/i386/native_threads folder
  public int verifyJRELibiNativeFolderFile() throws Exception {
                 int iJRELibiNativeFiles = 0;
                 String sJRELibiNativeFolder = null;
                 System.out.println("************************************");
                 sJRELibiNativeFolder = System.getProperty("jrelibinativedir.value");
                 System.out.println("files location="+sJRELibiNativeFolder);
                 File f = new File(sJRELibiNativeFolder);
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibiNativeFiles = fArray.length;
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibiNativeFiles;
  }
  //lin15. Verify that the 3 files exists in the jre/lib/i386/server folder
  public int verifyJRELibiServerFolderFile() throws Exception {
                 int iJRELibiServerFiles = 0;
                 String sJRELibiServerFolder = null;
                 System.out.println("************************************");
                 sJRELibiServerFolder = System.getProperty("jrelibiserverdir.value");
                 System.out.println("files location="+sJRELibiServerFolder);
                 File f = new File(sJRELibiServerFolder);
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibiServerFiles = fArray.length;
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibiServerFiles;
  }
  //lin16. Verify that the 1 file exists in the jre/lib/i386/xawt folder              
  public int verifyJRELibiXawtFolderFile() throws Exception {      
                 int iJRELibiXawtFiles = 0;      
                 String sJRELibiXawtFolder = null;      
                 System.out.println("************************************");
                 sJRELibiXawtFolder = System.getProperty("jrelibixawtdir.value");      
                 System.out.println("files location="+sJRELibiXawtFolder);      
                 File f = new File(sJRELibiXawtFolder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibiXawtFiles = fArray.length;      
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibiXawtFiles;      
  }  
  //lin18. Verify that the 4 files exists in the jre/lib/images/icons folder              
  public int verifyJRELibImgIconFolderFiles() throws Exception {      
                 int iJRELibImgIconFiles = 0;      
                 String sJRELibImgIconFolder = null;      
                 System.out.println("************************************");
                 sJRELibImgIconFolder = System.getProperty("jrelibimgicondir.value");      
                 System.out.println("files location="+sJRELibImgIconFolder);      
                 File f = new File(sJRELibImgIconFolder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                	 iJRELibImgIconFiles = fArray.length;      
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibImgIconFiles;      
  }
  //lin20. Verify that the 5 files exists under /jre/lib/oblique-fonts folder        
  public int verifyJRELibObliqueFolderFiles() throws Exception {      
                 int iJRELibObliqueFiles = 0;      
                 String sJRELibObliqueFolder = null;      
                 System.out.println("************************************");
                 sJRELibObliqueFolder = System.getProperty("jrelibobliquedir.value");     
                 System.out.println("files location="+sJRELibObliqueFolder);      
                 File f = new File(sJRELibObliqueFolder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibObliqueFiles = fArray.length;      
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibObliqueFiles;      
  } 
  //lin21. Verify that the 2 folders and 1 file exists under /jre/man folder        
  public int verifyJREManFolderFiles() throws Exception {      
                 int iJREManFiles = 0;      
                 String sJREManFolder = null;      
                 System.out.println("************************************");
                 sJREManFolder = System.getProperty("jremandir.value");     
                 System.out.println("files location="+sJREManFolder);      
                 File f = new File(sJREManFolder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                	 iJREManFiles = fArray.length;
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
                 return iJREManFiles;      
  } 
  //lin22. Verify that the 1 folder exists under /jre/man/ja_JP.eucJP folder        
  public int verifyJREManJaFolderFiles() throws Exception {      
                 int iJREManJaFiles = 0;      
                 String sJREManJaFolder = null;      
                 System.out.println("************************************");
                 sJREManJaFolder = System.getProperty("jremanjadir.value");     
                 System.out.println("files location="+sJREManJaFolder);      
                 File f = new File(sJREManJaFolder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                	 iJREManJaFiles = fArray.length;
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
                 return iJREManJaFiles;      
  } 
  //lin23. Verify that the 14 files exists under /jre/man/ja_JP.eucJP/man1 folder        
  public int verifyJREManJaMan1FolderFiles() throws Exception {      
                 int iJREManJaMan1Files = 0;      
                 String sJREManJaMan1Folder = null;      
                 System.out.println("************************************");
                 sJREManJaMan1Folder = System.getProperty("jremanjaman1dir.value");     
                 System.out.println("files location="+sJREManJaMan1Folder);      
                 File f = new File(sJREManJaMan1Folder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                	 iJREManJaMan1Files = fArray.length;
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
                 return iJREManJaMan1Files;      
  } 
  //lin24. Verify that the 14 files exists under /jre/man/man1 folder        
  public int verifyJREManMan1FolderFiles() throws Exception {      
                 int iJREManMan1Files = 0;      
                 String sJREManMan1Folder = null;      
                 System.out.println("************************************");
                 sJREManMan1Folder = System.getProperty("jremanman1dir.value");     
                 System.out.println("files location="+sJREManMan1Folder);      
                 File f = new File(sJREManMan1Folder);      
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                	 iJREManMan1Files = fArray.length;
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
                 return iJREManMan1Files;      
  }
  //lin25. Verify that the 2 folders  exists under /jre/plugin/ folder
  public int verifyJREPluginFolderFiles() throws Exception {
                 int iJREPluginFiles = 0;
                 String sJREPluginFolder = null;
                 System.out.println("************************************");
                 sJREPluginFolder = System.getProperty("jreplugindir.value");
                 System.out.println("files location="+sJREPluginFolder);
                 File f = new File(sJREPluginFolder);
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                	 iJREPluginFiles = fArray.length;
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
                 return iJREPluginFiles;
  }
  //lin26. Verify that the 2 files exists under /jre/plugin/Desktop folder           
  public int verifyJREPluginDesktopFolderFiles() throws Exception {                
         int iJREPluginDesktopFiles = 0;        
         String sJREPluginDesktopFolder = null;        
         System.out.println("************************************");
         sJREPluginDesktopFolder = System.getProperty("jreplugindesktopdir.value");          
         System.out.println("files location="+sJREPluginDesktopFolder);        
         File f = new File(sJREPluginDesktopFolder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPluginDesktopFiles = fArray.length;
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
         return iJREPluginDesktopFiles;     
  }
  //lin27. Verify that the 2 folders  exists under /jre/plugin/i386 folder           
  public int verifyJREPlugini386FolderFiles() throws Exception {                
         int iJREPlugini386Files = 0;        
         String sJREPlugini386Folder = null;        
         System.out.println("************************************");
         sJREPlugini386Folder = System.getProperty("jreplugini386dir.value");          
         System.out.println("files location="+sJREPlugini386Folder);        
         File f = new File(sJREPlugini386Folder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPlugini386Files = fArray.length;
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
         return iJREPlugini386Files;        
  }
  //lin28. Verify that the 1 file exists under /jre/plugin/i386/ns7 folder           
  public int verifyJREPlugini386ns7FolderFiles() throws Exception {                
         int iJREPlugini386ns7Files = 0;        
         String sJREPlugini386ns7Folder = null;        
         System.out.println("************************************");
         sJREPlugini386ns7Folder = System.getProperty("jreplugini386ns7dir.value");          
         System.out.println("files location="+sJREPlugini386ns7Folder);        
         File f = new File(sJREPlugini386ns7Folder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPlugini386ns7Files = fArray.length;
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
         return iJREPlugini386ns7Files;        
  }
  //lin29. Verify that the 1 file exists under /jre/plugin/i386/ns7-gcc29 folder           
  public int verifyJREPlugini386ns7gccFolderFiles() throws Exception {                
         int iJREPlugini386ns7gccFiles = 0;        
         String sJREPlugini386ns7gccFolder = null;        
         System.out.println("************************************");
         sJREPlugini386ns7gccFolder = System.getProperty("jreplugini386ns7gccdir.value");          
         System.out.println("files location="+sJREPlugini386ns7gccFolder);        
         File f = new File(sJREPlugini386ns7gccFolder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPlugini386ns7gccFiles = fArray.length;
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
         return iJREPlugini386ns7gccFiles;        
  }
  //1. Verify that the 3 folders and 16 files exists under terracotta-2.1.0/jre
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREBinClientFiles;		 	  
  }
  //4. Verify that the 2 folders and 16 files exists under /jre/jre folder
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
  //5. Verify that the 2 folders and 66 files exists under /jre/jre/bin
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
  //6. Verify that the 3 files exists under /jre/jre/bin/client
  public int verifyJREJREBinClientFolderFiles() throws Exception { 	     
	  	int iJREJREBinClientFiles = 0; 	  	
		String sJREJREBinClientFolder = null;
		System.out.println("************************************");
		sJREJREBinClientFolder = System.getProperty("jrejrebinclientdir.value");		 	 
		 System.out.println("files location="+sJREJREBinClientFolder);
		 File f = new File(sJREJREBinClientFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJREBinClientFiles = fArray.length;
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
		 return iJREJREBinClientFiles;
  } 
  //7. Verify that the 2 files exists under /jre/jre/bin/server
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREJREBinServerFiles;
  }  
  //8. Verify that the 4 folders exists under /jre/jre/lib folder
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
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREJRELibFiles;		 	  
  }
  //9. Verify that 1 file exists under /jre/jre/lib/audio
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibAudioFolderFiles;
  }
  //10. Verify that 5 files exists under /jre/jre/lib/cmm
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }	 
		 System.out.println("************************************");		 
		 return iJREJRELibCmmFolderFiles;
  }
  //11. Verify that 4 files exists under /jre/jre/lib/ext
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }	 
		 System.out.println("************************************");		 
		 return iJREJRELibExtFolderFiles;
  }
 //12. Verify that 8 files exists under /jre/jre/lib/fonts
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }			 
		 System.out.println("************************************");		 
		 return iJREJRELibFontsFolderFiles;
  }
  //13. Verify that the 11 folders and 24 files exists under /jre/lib folder
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
  //14. Verify that the 1 file exists in the jre/lib/audio folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibAudioFolderFiles;
  }
 //15. Verify that the 1 file exists under /jre/lib/cmm folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibCmmFiles;		 	  
  }
 //16. Verify that the 4 files exists under /jre/lib/ext folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibExtFiles;		 	  
  }
  //17. Verify that the 8 files exists under /jre/lib/fonts folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibfontsFiles;		 	  
  }
  //18. Verify that the 1 file exists in the jre/lib/i386 folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibiFiles;
  }
  //19. Verify that the 2 files exists under /jre/lib/im folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibImFiles;		 	  
  }
  //20. Verify that the 8 files exists under /jre/lib/images/cursors folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibImgCursorsFiles;		 	  
  }
 //21. Verify that the 12 files exists under /jre/lib/javaws folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibJavawsFiles;		 	  
  }
  //22. Verify that the 4 files exists under /jre/lib/management folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibMangFiles;		 	  
  }
  //23. Verify that the 6 files exists under /jre/lib/security folder
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
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibSecurFiles;		 	  
  }
  //24. Verify that the 10 folders and 14 files exists under /jre/lib/zi folder
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
  //25. Verify that the 1 folders exists under /jre/lib/images folder
  public int verifyJRELibImgFolderFiles() throws Exception {
		 int iJRELibImgFiles = 0;		 
		 String sJRELibImgFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibImgFolder = System.getProperty("jrelibimgdir.value");		 
		 System.out.println("files location="+sJRELibImgFolder);
		 File f = new File(sJRELibImgFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibImgFiles = fArray.length;
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
		 return iJRELibImgFiles;		 	  
  } 
}  
  


  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
