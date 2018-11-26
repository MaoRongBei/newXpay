package com.hrtpayment.xpay.cups.sdk;

import java.util.HashMap;
import java.util.Map;

public class ApiConstants {

	public static final String SIGN_TYPE_RSA = "RSA";
	public static final String SIGN_TYPE_RSA2  ="RSA2";
	public static final String SIGN_SHA256RSA_ALGORITHMS = "SHA256WithRSA";
	public static final String SIGN_ALGORITHMS = "SHA1WithRSA";
	public static final String FIELD_SIGNMETHOD="signMethod";
	public static final String FIELD_SIGNATRUE="signature";
	public static final String CODE_INVALID_SIGNATURE ="20001";
	public static final String CHARSET_GBK="GBK";
	public static final String DEFAULT_CHARSET="UTF-8";
	public static final String EQUAL="=";
	public static final String AMPERSAND="&";
	
	public static Map<String, String> CODE_MAP = new HashMap<String, String>() {
			{
				put(CODE_INVALID_SIGNATURE, "无效的签名");
			}
	};
}
