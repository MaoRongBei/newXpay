package com.hrtpayment.xpay.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hrtpayment.xpay.utils.crypto.Md5Util;
import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;

/**
 * 处理类似于微信报文的简单单层xml
 * @author aibing
 * 2016年11月9日
 */
public class SimpleXmlUtil {
	
	/**
	 * xml字符串转为map
	 * @param xml
	 * @return
	 * @throws RuntimeException
	 */
	public static Map<String,String> xml2map(String xml) {
		Document doc;
		try {
			doc = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		Map<String,String> map = new HashMap<String,String>();
		
		Element element = doc.getDocumentElement();
		NodeList list = element.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String tag = node.getNodeName();
			NodeList sublist = node.getChildNodes();
			for (int j = 0; j < sublist.getLength(); j++) {
				Node childNode = sublist.item(j);
				if (childNode.getNodeType()==Node.TEXT_NODE || childNode.getNodeType()==Node.CDATA_SECTION_NODE) {
					map.put(tag, childNode.getNodeValue());
				}
			}
		}
		return map;
	}

	public static String map2xml(Map<String,String> map) {
		return map2xml(map,"xml");
	}

	public static String map2xml(Map<String,String> map,String rootName) {
		Set<String> set = map.keySet();
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(rootName).append(">");
		for (String key : set) {
			sb.append(String.format("<%s><![CDATA[%s]]></%s>", key, map.get(key), key));
		}
		sb.append("</").append(rootName).append(">");
		return sb.toString();
	}
	/**
	 * 获取签名字符串(键按ascii排序,排除exc键,排除空值,使用url键值对格式)
	 * @param map
	 * @param exc
	 * @return
	 */
	public static String getSignBlock(Map<String,String> map,String exc) {
		Set<String> set = map.keySet();
		String[] keys = set.toArray(new String[0]);
		Arrays.sort(keys);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keys.length; i++) {
			if (!keys[i].equals(exc) && null!=map.get(keys[i]) && !"".equals(map.get(keys[i]))) {
				sb.append(String.format("%s=%s&",keys[i],map.get(keys[i])));
			}
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	public static String getSignBlock(Map<String,String> map) {
		return getSignBlock(map,null);
	}
	
	public static String getMd5Sign(Map<String,String> map,String key) {
		Set<String> set = map.keySet();
		String[] keys = set.toArray(new String[0]);
		Arrays.sort(keys);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keys.length; i++) {
			if (!keys[i].equals("sign") && null!=map.get(keys[i]) && !"".equals(map.get(keys[i]))) {
				sb.append(String.format("%s=%s&",keys[i],map.get(keys[i])));
			}
		}
		sb.append("key=").append(key);
		return Md5Util.digestUpperHex(sb.toString().getBytes(CharsetUtil.UTF8));
	}
	public static String getMd5SignByStr(String str) {
		return Md5Util.digestUpperHex(str.toString().getBytes(CharsetUtil.UTF8));
	}
	
	
//	public static void main(String[] args) {
//		
//		Map<String,String> map = new HashMap<String, String>();
//		map.put("unno", "110000");
//		map.put("mid", "987990010000003");
//		map.put("orderid", "110000201806150000000x01");
//		map.put("payway", "WXZF");
//		map.put("amount", "0.01");
//		
//		map.put("authcode", "1234567899");
//		System.out.println( getMd5Sign(map,"12345678"));
//		map.put("sign", getMd5Sign(map,"12345678"));
//		
//		
//	}
}
