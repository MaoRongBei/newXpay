package com.hrtpayment.xpay.utils.crypto;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DesUtil {

	private static Logger logger = LogManager.getLogger();
	private static String securityKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4";

	public static String Des3Encode(String plainText) {
		byte[] securityText = null;
		try {
			byte[] key = Base64.decodeBase64(securityKey);
			byte[] keyiv = { 1, 2, 3, 4, 5, 6, 7, 8 };

			logger.info("CBC加密......start ");

			securityText = des3EncodeCBC(key, keyiv, plainText.getBytes("UTF-8"));

			logger.info("加密后密文  -----" + securityText);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("CBC加密.................end");

		return Base64.encodeBase64String(securityText);
	}

	public static String Des3Decode(String securityText) throws UnsupportedEncodingException {
		byte[] plainText = null;
		try {
			byte[] key = Base64.decodeBase64(securityKey);
			byte[] keyiv = { 1, 2, 3, 4, 5, 6, 7, 8 };

			logger.info("CBC解密.........start ");

			logger.info("解密前的密文 :" + securityText);

			plainText = des3DecodeCBC(key, keyiv, Base64.decodeBase64(securityText));

			logger.info(new String(plainText, "UTF-8"));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("CBC解密...........end");
		return new String(plainText, "UTF-8");
	}

	public static byte[] des3EncodeECB(byte[] key, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);

		Cipher cipher = Cipher.getInstance("desede/ECB/PKCS5Padding");

		cipher.init(1, deskey);
		byte[] bOut = cipher.doFinal(data);

		return bOut;
	}

	public static byte[] ees3DecodeECB(byte[] key, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);

		Cipher cipher = Cipher.getInstance("desede/ECB/PKCS5Padding");

		cipher.init(2, deskey);

		byte[] bOut = cipher.doFinal(data);

		return bOut;
	}

	public static byte[] des3EncodeCBC(byte[] key, byte[] keyiv, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);

		Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");
		IvParameterSpec ips = new IvParameterSpec(keyiv);
		cipher.init(1, deskey, ips);
		byte[] bOut = cipher.doFinal(data);

		return bOut;
	}

	public static byte[] des3DecodeCBC(byte[] key, byte[] keyiv, byte[] data) throws Exception {
		Key deskey = null;
		DESedeKeySpec spec = new DESedeKeySpec(key);
		SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
		deskey = keyfactory.generateSecret(spec);

		Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");
		IvParameterSpec ips = new IvParameterSpec(keyiv);

		cipher.init(2, deskey, ips);

		byte[] bOut = cipher.doFinal(data);

		return bOut;
	}	
	
	/**
	 * des3Encryption加密
	 * @param key
	 * @param data
	 * @return
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws IllegalStateException
	 */
	public static byte[] des3Encryption(byte[] key, byte[] data) throws
	      NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
	      BadPaddingException, IllegalBlockSizeException, IllegalStateException {
	    final String Algorithm = "DESede"; 

	    SecretKey deskey = new SecretKeySpec(key, Algorithm);

	    Cipher c1 = Cipher.getInstance(Algorithm);
	    c1.init(Cipher.ENCRYPT_MODE, deskey);
	    return c1.doFinal(data);
	  }

}
