package com.hrtpayment.xpay.baidu.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class KeyUtil {

	public static String makeSgin(Map<String, String> map, String key) throws IllegalArgumentException, IllegalAccessException {
		if(map.containsKey("sign"))
			map.remove("sign");
		String sign = new KeyUtil().make_sign_by_map(map, key,false);
		return sign;
	}
	
	public static String verifySgin(Map<String, String> map, String key) throws IllegalArgumentException, IllegalAccessException {
		if(map.containsKey("sign"))
			map.remove("sign");
		String sign = new KeyUtil().make_sign_by_map(map, key,true);
		return sign;
	}
	
	public String make_sign_by_map(Map<String, String> map, String key,boolean isVerify) {
		String sign = null;
		StringBuffer sb = new StringBuffer();
		// 排序
		List<Map.Entry<String, String>> infoIds = new ArrayList<Map.Entry<String, String>>(map.entrySet());
		Collections.sort(infoIds, new Comparator<Map.Entry<String, String>>() {
			public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
				return (o1.getKey()).toString().compareTo(o2.getKey());
			}
		});
		// 对参数数组进行按key升序排列,然后拼接，最后调用 签名方法
		int size = infoIds.size();
		for (int i = 0; i < size; i++) {
			if(isVerify){
				sb.append(infoIds.get(i).getKey() + "=" + infoIds.get(i).getValue() + "&");
			}else{
				if (!"".equals(infoIds.get(i).getValue()) )
					sb.append(infoIds.get(i).getKey() + "=" + infoIds.get(i).getValue() + "&");
			}
		}
		String newStrTemp = sb.toString() + "key=" + key;
		sign = MD5.md5Digest(newStrTemp);
		sign=sign.toUpperCase();
		
//		System.out.println("---->str待签名串: " + newStrTemp + ";\r\n 签名串 sign=" + sign);
		return sign.toUpperCase();
	}

}
