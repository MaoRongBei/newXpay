package com.hrtpayment.xpay.cups.sdk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;


/**
 *  加密工具
 * 
 */
public class ApiEncryptUtil {

    private static final String AES_ALG         = "AES";

    /**
     * AES算法
     */
    private static final String AES_CBC_PCK_ALG = "AES/CBC/PKCS5Padding";

    private static final byte[] AES_IV          = initIv(AES_CBC_PCK_ALG);

    /**
     *   加密
     * 
     * @param content
     * @param encryptType
     * @param encryptKey
     * @param charset
     * @return
     * @throws ApiException
     */
    public static String encryptContent(String content, String encryptType, String encryptKey,
                                        String charset) throws ApiException {

        if (AES_ALG.equals(encryptType)) {

            return aesEncrypt(content, encryptKey, charset);

        } else {

            throw new ApiException("当前不支持该算法类型：encrypeType=" + encryptType);
        }

    }

    /**
     *  解密
     * 
     * @param content
     * @param encryptType
     * @param encryptKey
     * @param charset
     * @return
     * @throws ApiException
     */
    public static String decryptContent(String content, String encryptType, String encryptKey,
                                        String charset) throws ApiException {

        if (AES_ALG.equals(encryptType)) {

            return aesDecrypt(content, encryptKey, charset);

        } else {

            throw new ApiException("当前不支持该算法类型：encrypeType=" + encryptType);
        }

    }

    /**
     * AES加密
     * 
     * @param content
     * @param aesKey
     * @param charset
     * @return
     * @throws ApiException
     */
    private static String aesEncrypt(String content, String aesKey, String charset)
                                                                                   throws ApiException {

        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PCK_ALG);

            IvParameterSpec iv = new IvParameterSpec(AES_IV);
            cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(Base64.decodeBase64(aesKey.getBytes()), AES_ALG), iv);

            byte[] encryptBytes = cipher.doFinal(content.getBytes(charset));
            return new String(Base64.encodeBase64(encryptBytes));
        } catch (Exception e) {
            throw new ApiException("AES加密失败：Aescontent = " + content + "; charset = "
                                         + charset, e);
        }

    }

    /**
     * AES解密
     * 
     * @param content
     * @param key
     * @param charset
     * @return
     * @throws ApiException
     */
    private static String aesDecrypt(String content, String key, String charset)
                                                                                throws ApiException {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PCK_ALG);
            IvParameterSpec iv = new IvParameterSpec(initIv(AES_CBC_PCK_ALG));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Base64.decodeBase64(key.getBytes()),
                AES_ALG), iv);

            byte[] cleanBytes = cipher.doFinal(Base64.decodeBase64(content.getBytes()));
            return new String(cleanBytes, charset);
        } catch (Exception e) {
            throw new ApiException("AES解密失败：Aescontent = " + content + "; charset = "
                                         + charset, e);
        }
    }

    /**
     * 初始向量的方法, 全部为0. 这里的写法适合于其它算法,针对AES算法的话,IV值一定是128位的(16字节).
     *
     * @param fullAlg
     * @return
     * @throws GeneralSecurityException
     */
    private static byte[] initIv(String fullAlg) {

        try {
            Cipher cipher = Cipher.getInstance(fullAlg);
            int blockSize = cipher.getBlockSize();
            byte[] iv = new byte[blockSize];
            for (int i = 0; i < blockSize; ++i) {
                iv[i] = 0;
            }
            return iv;
        } catch (Exception e) {

            int blockSize = 16;
            byte[] iv = new byte[blockSize];
            for (int i = 0; i < blockSize; ++i) {
                iv[i] = 0;
            }
            return iv;
        }
    }
    
    /**
     * 加密
     * 
     * @param content 需要加密的内容
     * @param password  加密密码
     * @return
     */ 
    public static byte[] encrypt(String content, String aeskey) { 
            try {            
                    KeyGenerator kgen = KeyGenerator.getInstance("AES"); 
                    kgen.init(128, new SecureRandom(aeskey.getBytes())); 
                    SecretKey secretKey = kgen.generateKey(); 
                    byte[] enCodeFormat = secretKey.getEncoded(); 
                    SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES"); 
                    Cipher cipher = Cipher.getInstance("AES");// 创建密码器 
                    byte[] byteContent = content.getBytes("utf-8"); 
                    cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化 
                    byte[] result = cipher.doFinal(byteContent); 
                    return result; // 加密 
            } catch (NoSuchAlgorithmException e) { 
                    e.printStackTrace(); 
            } catch (NoSuchPaddingException e) { 
                    e.printStackTrace(); 
            } catch (InvalidKeyException e) { 
                    e.printStackTrace(); 
            } catch (UnsupportedEncodingException e) { 
                    e.printStackTrace(); 
            } catch (IllegalBlockSizeException e) { 
                    e.printStackTrace(); 
            } catch (BadPaddingException e) { 
                    e.printStackTrace(); 
            } 
            return null; 
    } 
    
    /**解密
     * @param content  待解密内容
     * @param password 解密密钥
     * @return
     */ 
    public static byte[] decrypt(byte[] content, String aeskey) { 
            try { 
                     KeyGenerator kgen = KeyGenerator.getInstance("AES"); 
                     kgen.init(128, new SecureRandom(aeskey.getBytes())); 
                     SecretKey secretKey = kgen.generateKey(); 
                     byte[] enCodeFormat = secretKey.getEncoded(); 
                     SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");             
                     Cipher cipher = Cipher.getInstance("AES");// 创建密码器 
                    cipher.init(Cipher.DECRYPT_MODE, key);// 初始化 
                    byte[] result = cipher.doFinal(content); 
                    return result; // 加密 
            } catch (NoSuchAlgorithmException e) { 
                    e.printStackTrace(); 
            } catch (NoSuchPaddingException e) { 
                    e.printStackTrace(); 
            } catch (InvalidKeyException e) { 
                    e.printStackTrace(); 
            } catch (IllegalBlockSizeException e) { 
                    e.printStackTrace(); 
            } catch (BadPaddingException e) { 
                    e.printStackTrace(); 
            } 
            return null; 
    }
    
    /**将二进制转换成16进制
     * @param buf
     * @return
     */ 
    public static String parseByte2HexStr(byte buf[]) { 
            StringBuffer sb = new StringBuffer(); 
            for (int i = 0; i < buf.length; i++) { 
                    String hex = Integer.toHexString(buf[i] & 0xFF); 
                    if (hex.length() == 1) { 
                            hex = '0' + hex; 
                    } 
                    sb.append(hex.toUpperCase()); 
            } 
            return sb.toString(); 
    } 
 

    /**将16进制转换为二进制
     * @param hexStr
     * @return
     */ 
    public static byte[] parseHexStr2Byte(String hexStr) { 
            if (hexStr.length() < 1) 
                    return null; 
            byte[] result = new byte[hexStr.length()/2]; 
            for (int i = 0;i< hexStr.length()/2; i++) { 
                    int high = Integer.parseInt(hexStr.substring(i*2, i*2+1), 16); 
                    int low = Integer.parseInt(hexStr.substring(i*2+1, i*2+2), 16); 
                    result[i] = (byte) (high * 16 + low); 
            } 
            return result; 
    } 
    
	/** 
     * base64编码 
     * @param bstr 
     * @return String 
     */  
    public static String encode(byte[] bstr){  
    return new sun.misc.BASE64Encoder().encode(bstr);  
    }  
  
    /** 
     * base64解码 
     * @param str 
     * @return string 
     */  
    public static byte[] decode(String str){  
    byte[] bt = null;  
    try {  
        sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();  
        bt = decoder.decodeBuffer( str );  
    } catch (IOException e) {  
        e.printStackTrace();  
    }  
  
        return bt;  
    }  
    
    /** 
     * 生成密钥 
     * 自动生成AES128位密钥 
     * @throws NoSuchAlgorithmException  
     */  
    public static String getAutoCreateAESKey() throws NoSuchAlgorithmException{  
        KeyGenerator kg = KeyGenerator.getInstance("AES");  
        kg.init(128);//要生成多少位，只需要修改这里即可128, 192或256  
        SecretKey sk = kg.generateKey();  
        byte[] b = sk.getEncoded();  
       return new String(Base64.encodeBase64(b));
    }  
    
    
    public static String getNotifyValidteResult(String notifyStr,String encoding){// 通过SHA256进行摘要并转16进制
		byte[] signDigest = sha256X16(notifyStr, encoding);
		byte[] byteSign = Base64.encodeBase64(signDigest);
		String stringSign = new String(byteSign);
		return stringSign;
    }

    /**
	 * sha256计算后进行16进制转换
	 * 
	 * @param data
	 *            待计算的数据
	 * @param encoding
	 *            编码
	 * @return 计算结果
	 */
	public static byte[] sha256X16(String data, String encoding) {
		byte[] bytes = sha256(data, encoding);
		/*StringBuilder sha256StrBuff = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			if (Integer.toHexString(0xFF & bytes[i]).length() == 1) {
				sha256StrBuff.append("0").append(
						Integer.toHexString(0xFF & bytes[i]));
			} else {
				sha256StrBuff.append(Integer.toHexString(0xFF & bytes[i]));
			}
		}
		try {
			return sha256StrBuff.toString().getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			LogUtil.writeErrorLog(e.getMessage(), e);
			return null;
		}*/
		return bytes;
	}
	
	/**
	 * sha256计算
	 * 
	 * @param datas
	 *            待计算的数据
	 * @param encoding
	 *            字符集编码
	 * @return
	 */
	private static byte[] sha256(String datas, String encoding) {
		try {
			return sha256(datas.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			LogUtil.writeErrorLog("SHA256计算失败", e);
			return null;
		}
	}
	/**
	 * sha256计算.
	 * 
	 * @param datas
	 *            待计算的数据
	 * @return 计算结果
	 */
	private static byte[] sha256(byte[] data) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
			md.reset();
			md.update(data);
			return md.digest();
		} catch (Exception e) {
			LogUtil.writeErrorLog("SHA256计算失败", e);
			return null;
		}
	}
}
