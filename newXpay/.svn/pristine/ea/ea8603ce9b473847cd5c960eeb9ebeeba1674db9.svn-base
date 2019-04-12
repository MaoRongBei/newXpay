package com.hrtpayment.xpay.quickpay.cups.util;


import org.apache.commons.codec.digest.DigestUtils;

public class Sha1Util {
	
	public static String encodeSHAHex(String data) throws Exception{
		return DigestUtils.shaHex(data);
	}
	
	
	/** 
	 * 生成SHA-1方式签名的签名结果
	 * @param sArray 要签名的数组
	 * @param key 签名密码
	 * @param inputCharset 签名字符集,与请求接口中的字符集一致
	 * @return 签名结果字符串
	 */
	public static String getSha1SignMsg(String data,String inputCharset) {
		String signMsg = HashEncrypt.doEncrypt(data,"SHA-1", inputCharset);
		return signMsg;
	}

	
	
} 
