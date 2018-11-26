package com.hrtpayment.xpay.bcm.util;

import java.util.Map;

import com.hrtpayment.xpay.utils.SimpleXmlUtil;

public class MsgUtil {
	
	
	public static boolean verifySign(Map<String, String> map,String key) {
		String sign = map.get("sign");
		String verifysign = SimpleXmlUtil.getMd5Sign(map, key);
//		System.out.println("计算签名："+verifysign+" 返回签名："+sign);
		if (sign == null || !sign.equals(verifysign)) {
			return false;
		} else {
			return true;
		}
	}
}
