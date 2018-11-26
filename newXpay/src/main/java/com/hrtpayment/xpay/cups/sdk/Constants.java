package com.hrtpayment.xpay.cups.sdk;

import java.util.HashMap;
import java.util.Map;

public class Constants {
			public static final String SIGN_TYPE_RSA = "RSA";
			public static final String SIGN_TYPE_RSA2  ="RSA2";
			public static final String SIGN_SHA256RSA_ALGORITHMS = "SHA256WithRSA";
			public static final String SIGN_ALGORITHMS = "SHA1WithRSA";
			public static final String SIGN_METHOD="signMethod";
			public static final String CODE_INVALID_SIGNATURE ="20001";
			public static final String CHARSET_GBK="GBK";
			
			public static Map<String, String> CODE_MAP = new HashMap<String, String>() {
					{
						put(CODE_INVALID_SIGNATURE, "无效的签名");
					}
			};

}
