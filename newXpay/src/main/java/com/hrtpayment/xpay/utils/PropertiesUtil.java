package com.hrtpayment.xpay.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PropertiesUtil {
	private static Logger logger = LogManager.getLogger();
	private static HashMap<String, Properties> map = new HashMap<String, Properties>();
	private static String dir = System.getProperty("user.dir")+File.separator;
	
	public static Properties getProperties(String fileName){
		if (!map.containsKey(fileName)) {
			synchronized (map) {
				if (!map.containsKey(fileName)) {
					File file = new File(dir + fileName);
					if (file.exists()) {
						Properties pro = new Properties();
						try {
							FileInputStream input = new FileInputStream(file);
							pro.load(input);
							input.close();
							map.put(fileName, pro);
						} catch (IOException e) {
							logger.error("载入配置文件出错", e);
						}
					}
				}
			}
		}
		return map.get(fileName);
	}
}
