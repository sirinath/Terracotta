package tsinttest;

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
	    assertEquals(11, iCount);
	    int  iBinCount = verifyJREBinFolderFiles(); //2
	    assertEquals(18, iBinCount);	    
	    int  iJRECount = verifyJREJREFolder(); //3
	    assertEquals(1, iJRECount);		    
	    int  iJREJRELibCount = verifyJREJRELibFolder(); //4
	    assertEquals(3, iJREJRELibCount);
	    int  iJREJRELibfileCount = verifyJREJRELibFolderFiles(); //5
	    assertEquals(8, iJREJRELibfileCount);
	    int  iJRELibCount = verifyJRELibFolder(); //6
	    assertEquals(53, iJRELibCount); 
	    int  iJRELibAudioCount = verifyJRELibAudioFolderFile(); //7
	    assertEquals(1, iJRELibAudioCount);	    
	    int  iJRELibCmmCount = verifyJRELibCmmFolderFiles(); //8
	    assertEquals(5, iJRELibCmmCount);
	    int  iJRELibExtCount = verifyJRELibExtFolderFiles(); //9
	    assertEquals(4, iJRELibExtCount);
	    int  iJRELibFontsCount = verifyJRELibfontsFolderFiles(); //10
	    assertEquals(9, iJRELibFontsCount);
	    int  iJRELibiCount = verifyJRELibiFolderFile(); //11
	    assertEquals(41, iJRELibiCount);	    
	    int  iJRELibiClientCount = verifyJRELibiClientFolderFile(); //12
        assertEquals(4, iJRELibiClientCount);
        int  iJRELibiHeadCount = verifyJRELibiHeadFolderFile(); //13
        assertEquals(1, iJRELibiHeadCount);	     
        int  iJRELibiMotifCount = verifyJRELibiMotifFolderFile(); //14
        assertEquals(1, iJRELibiMotifCount);
        int  iJRELibiNativeCount = verifyJRELibiNativeFolderFile(); //15    
        assertEquals(1, iJRELibiNativeCount);
        int  iJRELibiServerCount = verifyJRELibiServerFolderFile(); //16
        assertEquals(3, iJRELibiServerCount);
        int  iJRELibiXawtCount = verifyJRELibiXawtFolderFile(); //17
        assertEquals(1, iJRELibiXawtCount); 
	    int  iJRELibImCount = verifyJRELibImFolderFiles(); //18
	    assertEquals(2, iJRELibImCount);
	    int  iJRELibImgCount = verifyJRELibImgFolderFiles(); //19
	    assertEquals(2, iJRELibImgCount);	    
	    int  iJRELibImgCursCount = verifyJRELibImgCursorsFolderFiles(); //20
	    assertEquals(8, iJRELibImgCursCount);
	    int  iJRELibImgIconCount = verifyJRELibImgIconsFolderFiles(); //21
	    assertEquals(4, iJRELibImgIconCount);	    
	    int  iJRELibJavawsCount = verifyJRELibJavawsFolderFiles(); //22
	    assertEquals(13, iJRELibJavawsCount);
	    int  iJRELibLocaleCount = verifyJRELibLocaleFolderFiles(); //23
	    assertEquals(13, iJRELibLocaleCount);
	    int  iJRELibMangCount = verifyJRELibMangFolderFiles(); //24
	    assertEquals(4, iJRELibMangCount);
	    int  iJRELibObliqueCount = verifyJRELibObliqueFolderFiles(); //25
	    assertEquals(5, iJRELibObliqueCount);
	    int  iJRELibSecurCount = verifyJRELibSecurFolderFiles(); //26
	    assertEquals(6, iJRELibSecurCount);
	    int  iJRELibziCount = verifyJRELibziFolder(); //27
	    assertEquals(16, iJRELibziCount);
	    int  iJREJavawsCount = verifyJREJavawsFolderFiles(); //28
	    assertEquals(1, iJREJavawsCount);
	    int  iJREPluginCount = verifyJREPluginFolderFiles(); //29
	    assertEquals(2, iJREPluginCount);
	    int  iJREPluginDeskCount = verifyJREPluginDesktopFolderFiles(); //30
	    assertEquals(2, iJREPluginDeskCount);
	    int  iJREPlugini386Count = verifyJREPlugini386FolderFiles(); //31
	    assertEquals(2, iJREPlugini386Count);
	    int  iJREPluginiNs7Count = verifyJREPlugini386Ns7FolderFiles(); //32
	    assertEquals(1, iJREPluginiNs7Count);
	    int  iJREPluginiNs7gCount = verifyJREPluginiNs7gFolderFiles(); //33
	    assertEquals(1, iJREPluginiNs7gCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }  
	
 //	1. Verify that the jre folder exists under tc2.0.x folder 
  //Verify that the 5 folders and 6 files exists under /jre
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
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREFiles;	  
  }
  
  //2. Verify that the 18 files exists under /jre/bin folder
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
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+ i + " " + fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREBinFiles;	  
  }    
  //3. Verify that the 1 folder exists under /jre/jre folder
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
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJREJREFiles;		 	  
  }
  //4. Verify that the 3 folders exists under /jre/jre/lib folder
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
  //5. Verify that 1 file exists under /jre/jre/lib/audio
  //Verify that 1 file exists under /jre/jre/lib/cmm
 //Verify that 8 files exists under /jre/jre/lib/fonts
  public int verifyJREJRELibFolderFiles() throws Exception { 	     
	  	int iJREJRELibFiles = 0; 
	  	String sJREJRELibAudioFolderFile = null;
	  	String sJREJRELibCmmFolderFile = null;
	  	String sJREJRELibFontsFolderFile = null;
		 
	  	sJREJRELibAudioFolderFile = System.getProperty("jrejrelibaudiodirfile.value");
	  	sJREJRELibCmmFolderFile = System.getProperty("jrejrelibcmmdirfile.value");
	  	sJREJRELibFontsFolderFile = System.getProperty("jrejrelibfontsdir.value");
		 
		 System.out.println("************************************");
		 if(sJREJRELibAudioFolderFile != null) {		 
			 System.out.println("Lib audio folder file exists:" + sJREJRELibAudioFolderFile);			
		 }	
		 if(sJREJRELibCmmFolderFile != null) {		 
			 System.out.println("Lib cmm folder file exists:" + sJREJRELibCmmFolderFile);			 
		 }		 	 
		 System.out.println("files location="+sJREJRELibFontsFolderFile);
		 File f = new File(sJREJRELibFontsFolderFile);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJREJRELibFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }		 
		 System.out.println("************************************");		 
		 return iJREJRELibFiles;
  }
  //6. Verify that the 14 folders and 39 files exists under /jre/lib folder
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
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibFiles;		 	  
  }   
  //7. Verify that the 1 file exists under jre/lib/audio folder
  public int verifyJRELibAudioFolderFile() throws Exception {
		 int iJRELibAudioFiles = 0;		 
		 String sJRELibAudioFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibAudioFolder = System.getProperty("jrejrelibaudiodir.value");		 
		 System.out.println("files location="+sJRELibAudioFolder);
		 File f = new File(sJRELibAudioFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibAudioFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibAudioFiles;		 	  
  }
 //8. Verify that the 5 files exists under /jre/lib/cmm folder
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
 //9. Verify that the 4 files exists under /jre/lib/ext folder
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
  //10. Verify that the 8 files exists under /jre/lib/fonts folder
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
  //11. Verify that the 6 folders and 35 file exists under /jre/lib/i386 folder
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
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }		 
		 System.out.println("************************************");
		 return iJRELibiFiles;		 	  
  }
  //12. Verify that the 4 files exists in the jre/lib/i386/client folder
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
  //13. Verify that the 1 file exists in the jre/lib/i386/headless folder 
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
  //14. Verify that the 1 file exists in the jre/lib/i386/motif21 folder 
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
  //15. Verify that the 1 file exists in the jre/lib/i386/native_threads folder
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
  //16. Verify that the 3 files exists in the jre/lib/i386/server folder
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
  //17. Verify that the 1 file exists in the jre/lib/i386/xawt folder              
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
  //18. Verify that the 2 files exists under /jre/lib/im folder
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
  //19. Verify that the 2 folders exists under /jre/lib/images folder
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
				System.out.println("folder name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibImgFiles;		 	  
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
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibImgCursorsFiles;		 	  
  }
 //21. Verify that the 4 files exists under /jre/lib/images/icons folder
  public int verifyJRELibImgIconsFolderFiles() throws Exception {
		 int iJRELibImgIconsFiles = 0;		 
		 String sJRELibImgIconsFolder = null;		 	
		 System.out.println("************************************");
		 sJRELibImgIconsFolder = System.getProperty("jrelibimgiconsdir.value");		 
		 System.out.println("files location="+sJRELibImgIconsFolder);
		 File f = new File(sJRELibImgIconsFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iJRELibImgIconsFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibImgIconsFiles;		 	  
  }
  //22. Verify that the 13 files exists under /jre/lib/javaws folder
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
  //23. Verify that the 13 folders exists under /jre/lib/locale folder
  public int verifyJRELibLocaleFolderFiles() throws Exception {
                 int iJRELibLocaleFiles = 0;
                 String sJRELibLocaleFolder = null;
                 System.out.println("************************************");
                 sJRELibLocaleFolder = System.getProperty("jreliblocaledir.value");
                 System.out.println("files location="+sJRELibLocaleFolder);
                 File f = new File(sJRELibLocaleFolder);
                 System.out.println("get absolute path="+f.getAbsolutePath());
                 File f1 = new File(f.getAbsolutePath());

                 File fArray[] = new File[0];
                 fArray = f1.listFiles();
                 System.out.println("file array size="+fArray.length);

                 if(fArray.length != 0){
                         iJRELibLocaleFiles = fArray.length;
                         for(int i=0; i<fArray.length;i++){
                                System.out.println("file name="+fArray[i].getName());
                         }
                 }
                 System.out.println("************************************");
                 return iJRELibLocaleFiles;
  }
  //24. Verify that the 4 files exists under /jre/lib/management folder
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
  //25. Verify that the 5 files exists under /jre/lib/oblique-fonts folder        
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
  //26. Verify that the 6 files exists under /jre/lib/security folder
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
  //27. Verify that the 10 folders and 6 files exists under /jre/lib/zi folder
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
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iJRELibziFiles;		 	  
  }
  //28. Verify that the 1 file exists under /jre/javaws folder       
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
                        System.out.println("file name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return  iJREJavawsFiles;
  }
  //29. Verify that the 2 folders  exists under /jre/plugin/ folder
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
                        System.out.println("folder name="+fArray[i].getName());
                 }
         }
         System.out.println("************************************");
         return iJREPluginFiles;
  }
  //30. Verify that the 2 files  exists under /jre/plugin/Desktop folder           
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
                        System.out.println("file name="+fArray[i].getName());
                 }
         }                
         System.out.println("************************************");
         return iJREPluginDesktopFiles;     
  }
  //31. Verify that the 2 folders  exists under /jre/plugin/i386 folder           
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
                        System.out.println("file name="+fArray[i].getName());
                 }
         }                
         System.out.println("************************************");
         return iJREPlugini386Files;        
  }
  //32. Verify that the 1 file exists under /jre/plugin/i386/ns7 folder           
  public int verifyJREPlugini386Ns7FolderFiles() throws Exception {                
         int iJREPluginiNs7Files = 0;        
         String sJREPluginiNs7Folder = null;        
         System.out.println("************************************");
         sJREPluginiNs7Folder = System.getProperty("jreplugini386ns7dir.value");          
         System.out.println("files location="+sJREPluginiNs7Folder);        
         File f = new File(sJREPluginiNs7Folder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPluginiNs7Files = fArray.length;        
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }                
         System.out.println("************************************");
         return iJREPluginiNs7Files;        
  }
 //33. Verify that the 1 file exists under /jre/plugin/i386/ns7-gcc29 folder           
  public int verifyJREPluginiNs7gFolderFiles() throws Exception {                
         int iJREPluginiNs7gFiles = 0;        
         String sJREPluginiNs7gFolder = null;        
         System.out.println("************************************");
         sJREPluginiNs7gFolder = System.getProperty("jreplugini386ns7gdir.value");          
         System.out.println("files location="+sJREPluginiNs7gFolder);        
         File f = new File(sJREPluginiNs7gFolder);        
         System.out.println("get absolute path="+f.getAbsolutePath());
         File f1 = new File(f.getAbsolutePath());

         File fArray[] = new File[0];
         fArray = f1.listFiles();
         System.out.println("file array size="+fArray.length);

         if(fArray.length != 0){
        	 iJREPluginiNs7gFiles = fArray.length;        
                 for(int i=0; i<fArray.length;i++){
                        System.out.println("file name="+fArray[i].getName());
                 }
         }                
         System.out.println("************************************");
         return iJREPluginiNs7gFiles;        
  }
}  
  


  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
