package com.hrtpayment.xpay.quickpay.cups.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class MapUtil {

	public static String map2String(Map<String, String> map) {
		Object[] key_arr = map.keySet().toArray();  
		Arrays.sort(key_arr);
		StringBuilder sb = new StringBuilder();
		for  (Object key : key_arr) {  
		    Object value = map.get(key);
		    if (null == key || "".equals(key) || null == value || "".equals(value)) {
				continue;
			} else {
				sb.append(key).append("=").append(value).append("&");
			}
		}
		return sb.toString().substring(0, sb.length() - 1);
	}
	
	public static Map<String, String> string2Map(String params) {
		params = params.replace(" ", "");
//		params = params.substring(params.indexOf("?") + 1);
		int index;
		String key = "";
		String value = "";
		Map<String, String> map = new HashMap<String, String>();
		String[] keyValues = params.split("&");
		for (String keyValue : keyValues) {
			if (null == keyValue || "".equals(keyValue)) {
				continue;
			}
			index = keyValue.indexOf("=");
			if (-1 == index) {
				continue;
			} else {
				key = keyValue.substring(0, index);
				value = keyValue.substring(index + 1);
				if (null == key || "".equals(key)) {
					continue;
				} else {
					map.put(key, value);
				}
			}
		}
		return map;
	}

//	public static Map<String, String> Object2Map(Object object) {
//		Map<String, String> map = new HashMap<String, String>();
//		JSONObject jsonObject = JSONObject.fromObject(object);
//		Iterator it = jsonObject.keys();
//		while (it.hasNext()) {
//			String key = String.valueOf(it.next());
//			String value = (String) jsonObject.get(key);
//			map.put(key, value);
//		}
//		return map;
//	}
	
}
