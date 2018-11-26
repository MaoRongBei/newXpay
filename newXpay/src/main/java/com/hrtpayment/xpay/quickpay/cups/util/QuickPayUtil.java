package com.hrtpayment.xpay.quickpay.cups.util;

import java.util.HashMap;
import java.util.Map;



public class QuickPayUtil {

	
	/**
	 * @Title: getSignature
	 * @Description: 签名方法
	 * @param map
	 * @param key
	 * @return
	 * @return: String
	 */
	public static String getSignature(Map<String, String> map, String key) {
		String vaData = MapUtil.map2String(map) + "&signkey=" + key.trim();
		String signKey = Sha1Util.getSha1SignMsg(vaData, "UTF-8");
		return signKey;
	}
	
	/**
	 * 获取银行卡密码
	 * @Title: getPin
	 * @Description: TODO
	 * @param pin
	 * @param secretKey
	 * @return
	 * @return: String
	 */
	public static String getPin(String pin, String secretKey) {
//		String passWordKey = PropUtil.getVlaue("pubKey");
//		return SecretMerUtil.encryptByPublicKeyRSAPIN(pin, bankNum, passWordKey);
		String secretResult = null;
		try {
			byte[] result = DesUtil.encrypt(pin.getBytes(), secretKey.getBytes());
			secretResult = BASE64Util.encodeBySun(result);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return secretResult;
	}
	
	/**
	 * 获取加密后的CVN2
	 * @Title: getCvn2
	 * @Description: TODO
	 * @param cvn2
	 * @return
	 * @return: String
	 */
	public static String getCvn2(String cvn2, String secretKey) {
//		String passWordKey = PropUtil.getVlaue("pubKey");
//		return SecretMerUtil.encryptByPublicKeyRSA(cvn2, passWordKey);
		String secretResult = null;
		try {
			byte[] result = DesUtil.encrypt(cvn2.getBytes(), secretKey.getBytes());
			secretResult = BASE64Util.encodeBySun(result);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return secretResult;
	}
	
	/**
	 * 获取加密后的PanDate卡有效期
	 * @Title: getPanDate
	 * @Description: TODO
	 * @param panDate
	 * @param secretKey
	 * @return
	 * @return: String
	 */
	public static String getPanDate(String panDate, String secretKey) {
		String secretResult = null;
		try {
			byte[] result = DesUtil.encrypt(panDate.getBytes(), secretKey.getBytes());
			secretResult = BASE64Util.encodeBySun(result);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return secretResult;
	}
	
	/**
	 * 格式化respStr返回参数
	 * @Title: getStrParams
	 * @Description: TODO
	 * @param respStr
	 * @return
	 * @return: Map<String,String>
	 */
	public static Map<String, String> getStrParams(String respStr){
		Map<String, String> map = new HashMap<String, String>();
		if (respStr!=""){
			String[] blocks = respStr.split("&");
			for (String block : blocks) {
				String[] arr = block.split("=");
				map.put(arr[0], arr[1]);
			}
		}
		return map;
	}
	
	/** 
     * 产生4位随机数 
     * @return 
     */  
	public static long generateRandomNumber(int n){  
        if(n<1){  
            throw new IllegalArgumentException("随机数位数必须大于0");  
        }  
        return (long)(Math.random()*9*Math.pow(10,n-1)) + (long)Math.pow(10,n-1);  
    } 
}
