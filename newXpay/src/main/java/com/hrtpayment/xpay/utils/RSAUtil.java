package com.hrtpayment.xpay.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.binary.Base64;

 

public class RSAUtil {

    public static final String SIGN_ALGORITHMS = "SHA1WithRSA";

    public static String sign(String content, String input_charset, Key key)
            throws UnsupportedEncodingException, Exception {
        Cipher cipher;
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] output = cipher.doFinal(content.getBytes(input_charset));
            return Base64.encodeBase64String(output);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此加密算法");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            throw new Exception("加密公钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("明文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("明文数据已损坏");
        }
    }

    public static String readFile(String filePath, String charSet) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        try {
            FileChannel fileChannel = fileInputStream.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
            fileChannel.read(byteBuffer);
            byteBuffer.flip();
            return new String(byteBuffer.array(), charSet);
        } finally {
            fileInputStream.close();
        }

    }
    
    public static String getKey(String string) throws Exception {
        String content = readFile(string, "UTF8");
        return content.replaceAll("\\-{5}[\\w\\s]+\\-{5}[\\r\\n|\\n]", "");
    }

    public static String signByPrivate(String content, PrivateKey privateKey,
                                       String input_charset) throws Exception {
        if (privateKey == null) {
            throw new Exception("加密私钥为空, 请设置");
        }
        java.security.Signature signature = java.security.Signature
                .getInstance(SIGN_ALGORITHMS);
        signature.initSign(privateKey);
        signature.update(content.getBytes(input_charset));
        return Base64.encodeBase64String(signature.sign());
    }

    
//    public static byte[] encryptByPublicKey(byte[] data, String publicKey) throws Exception {
//        byte[] keyBytes = Base64.decodeBase64(publicKey);
//        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        Key publicK = keyFactory.generatePublic(x509KeySpec);
//        // 对数据加密
//        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
//        cipher.init(Cipher.ENCRYPT_MODE, publicK);
//        int inputLen = data.length;
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        int offSet = 0;
//        byte[] cache;
//        int i = 0;
//        // 对数据分段加密
//        while (inputLen - offSet > 0) {
//            if (inputLen - offSet > 117) {
//                cache = cipher.doFinal(data, offSet, 117);
//            } else {
//                cache = cipher.doFinal(data, offSet, inputLen - offSet);
//            }
//            out.write(cache, 0, cache.length);
//            i++;
//            offSet = i * 117;
//        }
//        byte[] encryptedData = out.toByteArray();
//        out.close();
//        return encryptedData;
//    }
    
    public static String signByPrivate(String content, String privateKey,
                                       String input_charset) throws Exception {
        if (privateKey == null) {
            throw new Exception("加密私钥为空, 请设置");
        }
        PrivateKey privateKeyInfo = getPrivateKey(privateKey);
        return signByPrivate(content, privateKeyInfo, input_charset);
    }

    public static boolean verifyByKeyPath(String content, String sign, String publicKeyPath, String input_charset) {
        try {
            return verify(content, sign, getKey(publicKeyPath), input_charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * RSA验签名检查
     * 
     * @param content
     *            待签名数据
     * @param sign
     *            签名值
     * @param publicKey
     *            支付宝公钥
     * @param input_charset
     *            编码格式
     * @return 布尔值
     */
    public static boolean verify(String content, String sign,
                                 String publicKey, String input_charset) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decodeBase64(publicKey);
            PublicKey pubKey = keyFactory
                    .generatePublic(new X509EncodedKeySpec(encodedKey));
            return verify(content, sign, pubKey, input_charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public static boolean verify(String content,String sign,PublicKey publicKey,String inputCharset){
        try {
            java.security.Signature signature = java.security.Signature
                    .getInstance(SIGN_ALGORITHMS);
            signature.initVerify(publicKey);
            signature.update(content.getBytes(inputCharset));
//            boolean bverify = signature.verify(Base64.decode(sign));
            boolean bverify = signature.verify(org.apache.commons.codec.binary.Base64.decodeBase64(sign));
            return bverify;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 得到私钥
     * 
     * @param key
     *            密钥字符串（经过base64编码）
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String key) throws Exception {
        byte[] keyBytes = buildPKCS8Key(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        return privateKey;        
    }
    
    private static byte[] buildPKCS8Key(String privateKey) throws IOException {
        if (privateKey.contains("-----BEGIN PRIVATE KEY-----")) {
            return Base64.decodeBase64(privateKey.replaceAll("-----\\w+ PRIVATE KEY-----", ""));
        } else if (privateKey.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            final byte[] innerKey = Base64.decodeBase64(privateKey.replaceAll("-----\\w+ RSA PRIVATE KEY-----", ""));
            final byte[] result = new byte[innerKey.length + 26];
            System.arraycopy(Base64.decodeBase64("MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKY="), 0, result, 0, 26);
            System.arraycopy(BigInteger.valueOf(result.length - 4).toByteArray(), 0, result, 2, 2);
            System.arraycopy(BigInteger.valueOf(innerKey.length).toByteArray(), 0, result, 24, 2);
            System.arraycopy(innerKey, 0, result, 26, innerKey.length);
            return result;
        } else {
            return Base64.decodeBase64(privateKey);
        }
    }
    
//    public static KeyInfo getPFXPrivateKey(String pfxPath, String password)
//            throws KeyStoreException, NoSuchAlgorithmException,
//            CertificateException, IOException, UnrecoverableKeyException {
//        FileInputStream fis = new FileInputStream(pfxPath);
//        KeyStore ks = KeyStore.getInstance("PKCS12");
//        ks.load(fis, password.toCharArray());
//        fis.close();
//        Enumeration<String> enumas = ks.aliases();
//        String keyAlias = null;
//        if (enumas.hasMoreElements())// we are readin just one certificate.
//        {
//            keyAlias = enumas.nextElement();
//        }
//
//        KeyInfo keyInfo = new KeyInfo();
//
//        PrivateKey prikey = (PrivateKey) ks.getKey(keyAlias, password.toCharArray());  
//        Certificate cert = ks.getCertificate(keyAlias);  
//        PublicKey pubkey = cert.getPublicKey();  
//
//        keyInfo.privateKey = prikey;
//        keyInfo.publicKey = pubkey;
//        return keyInfo;
//    }

//    public static class KeyInfo{
//
//        PublicKey publicKey;
//        PrivateKey privateKey;
//        public PublicKey getPublicKey() {
//            return publicKey;
//        }
//        public PrivateKey getPrivateKey() {
//            return privateKey;
//        }
//    }

    /**
     * 根据keypath 路径获取PublicKey
     * @param keyPath
     * @param inputCharset
     * @return
     * @throws Exception
     */
    public static PublicKey getPublicKey(String keyPath,String inputCharset) throws Exception {
        
    	String key=getKey(keyPath);
    	if (key == null) {
            throw new Exception("加密公钥为空, 请设置");
        }
        byte[] buffer = Base64.decodeBase64(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
        return keyFactory.generatePublic(keySpec);
    }
    
    /**
     * 根据keypath 路径获取PrivateKey
     * @param keyPath
     * @param inputCharset
     * @return
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String keyPath,String inputCharset)throws Exception{
    	String keyContent =readFile(keyPath,inputCharset);
    	return getPrivateKey(keyContent);
    }
    
}
