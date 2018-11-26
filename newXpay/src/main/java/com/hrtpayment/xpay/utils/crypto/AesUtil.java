package com.hrtpayment.xpay.utils.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.hrtpayment.xpay.utils.exception.CryptoException;

public class AesUtil {
	public static byte[] encrypt(byte[] plainBytes, byte[] keyBytes,  String cipherAlgorithm, String IV) throws CryptoException
	{
		if (keyBytes.length % 8 != 0 || keyBytes.length < 16 || keyBytes.length > 32) {
			throw new CryptoException("AES加密密钥长度错误");
		}			
		try {

			Cipher cipher = Cipher.getInstance(cipherAlgorithm);
			SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
			if (IV != null) {
				IvParameterSpec ivspec = new IvParameterSpec(IV.getBytes());
				cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
			} else {
				cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			}

			byte[] encryptedBytes = cipher.doFinal(plainBytes);

			return encryptedBytes;
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new CryptoException("AES加密出错");
	}
	
	public static byte[] decrypt(byte[] encryptedBytes, byte[] keyBytes, String cipherAlgorithm, String IV) throws CryptoException
	{
		try {
			if (keyBytes.length % 8 != 0 || keyBytes.length < 16 || keyBytes.length > 32) {
				throw new CryptoException("AES解密密钥长度错误");
			}

			Cipher cipher = Cipher.getInstance(cipherAlgorithm);
			SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
			if (IV != null) {
				IvParameterSpec ivspec = new IvParameterSpec(IV.getBytes());
				cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
			} else {
				cipher.init(Cipher.DECRYPT_MODE, secretKey);
			}

			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

			return decryptedBytes;
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new CryptoException("AES解密出错");
	}
}
