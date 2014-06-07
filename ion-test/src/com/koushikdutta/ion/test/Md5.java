package com.koushikdutta.ion.test;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.util.StreamUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5 {
    private MessageDigest digest;
    public static Md5 createInstance() throws NoSuchAlgorithmException {
        Md5 md5 = new Md5();
        md5.digest = MessageDigest.getInstance("MD5");
        return md5;
    }

    public static String digest(File file) throws NoSuchAlgorithmException, IOException {
        byte[] bytes = StreamUtility.readToEndAsArray(new FileInputStream(file));
        Md5 md5 = Md5.createInstance();
        md5.digest.update(bytes);
        return md5.digest();
    }
    
    private Md5() {
        
    }
    public Md5 update(ByteBufferList bb) {
        while (bb.size() > 0) {
            ByteBuffer b = bb.remove();
            digest.update(b);
        }
        return this;
    }

    public Md5 update(byte[] bytes) {
        digest.update(bytes);
        return this;
    }

    public String digest() {
        String hash = new BigInteger(digest.digest()).toString(16);
        return hash;
    }
}
