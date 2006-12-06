/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import org.apache.commons.io.CopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class GenerateKey {

  public static void main(String[] args) throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance(args[0]);
    generator.initialize(Integer.parseInt(args[1]));

    KeyPair pair = generator.generateKeyPair();
    PublicKey publicKey = pair.getPublic();
    PrivateKey privateKey = pair.getPrivate();

    System.err.println("Public Key Format: " + publicKey.getFormat());
    System.err.println("Private Key Format: " + privateKey.getFormat());
    
    FileOutputStream publicOut = new FileOutputStream(new File(args[2]));
    FileOutputStream privateOut = new FileOutputStream(new File(args[3]));
    
    CopyUtils.copy(publicKey.getEncoded(), publicOut);
    CopyUtils.copy(privateKey.getEncoded(), privateOut);
    
    publicOut.close();
    privateOut.close();
  }

}
