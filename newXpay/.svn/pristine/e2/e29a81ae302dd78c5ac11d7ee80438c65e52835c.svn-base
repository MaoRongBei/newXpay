package com.hrtpayment.xpay.utils.crypto;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

import com.hrtpayment.xpay.utils.CharsetUtil;

public class Md5Util {
	static {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			assert(md!=null);
		} catch (NoSuchAlgorithmException e) {
			assert(false);
		}
	}
	
	public static byte[] digest(String s){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
		return md.digest(s.getBytes(CharsetUtil.UTF8));
	}
	public static byte[] digest(String s, Charset charset){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
		return md.digest(s.getBytes(charset));
	}
	public static String digestUpperHex(String s){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
		return new String(Hex.encodeHex(md.digest(s.getBytes(CharsetUtil.UTF8)),false));
	}
	public static String digestUpperHex(byte[] bytes){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
		return new String(Hex.encodeHex(md.digest(bytes),false));
	}
	public static String digestUpperHex(String s, Charset charset){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
		return Hex.encodeHexString(md.digest(s.getBytes(charset))).toUpperCase();
	}
	
	public final static String MD5(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        try {
            byte[] btInput = s.getBytes(Charset.forName("UTF-8"));
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
