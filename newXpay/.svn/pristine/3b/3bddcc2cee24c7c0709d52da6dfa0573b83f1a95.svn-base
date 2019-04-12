package com.hrtpayment.xpay.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {
	
	public static String getRandomDecimalStr(int length) {
		StringBuilder builder = new StringBuilder();
		while (builder.length() < length) {
			String s = Double.toString(Math.random());
			if (s.length() > 3) {
				builder.append(s.substring(2));
			}
		}
		return builder.substring(0, length);
	}
	public static String getRandomHexStr(int length) {
		StringBuilder builder = new StringBuilder();
		while (builder.length() < length) {
			// 0x1.ac6c843ee64ccp-3 0x1.c1f8eec76d322p-1
			String s = Double.toHexString(Math.random());
			if (s.length() > 7) {
				builder.append(s.substring(4, s.length() - 3));
			}
		}
		return builder.substring(0, length);
	}

	public static String getWxTransId(){
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String date = format.format(new Date(System.currentTimeMillis()));
		return "hrt" + date + getRandomDecimalStr(12);
	}
}
