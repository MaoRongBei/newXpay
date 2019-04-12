package com.hrtpayment.xpay.cupsAT.service;
 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.cups.sdk.LogUtil;
import com.hrtpayment.xpay.cups.sdk.SDKConstants;
import com.hrtpayment.xpay.cups.sdk.SecureUtil;
import com.hrtpayment.xpay.quickpay.cups.util.RsaCertUtils;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;



@Service
public class CupsATService {
     

	private final Logger logger = LogManager.getLogger();
	
	
//	@Value("${cupsAli.pid}")
//	private String pid;
//	
//	@Value("${cupsAli.appid}")
//	private String appid;

	@Value("${cupsAli.notifyUrl}")
	private String notifyUrl;
	
	@Value("${at.hrt.privateKeyPath}")
	private String atPrivateKeyPath;
	
	@Value("${at.privateKey.passwd}")
	private String atPrivateKeyPwd;
	
	@Value("${at.cups.publicKeyPath}")
	private String atPublicKeyPath;

	
	@Value("${at.cups.ali.publicKeyPath}")
	private String atAliPublicKeyPath;
	
	
	
	
	/**
	 * 将Map转换为XML格式的字符串
	 */
	public static String mapToXml(Map<String, String> data) throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setExpandEntityReferences(false);
		documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		org.w3c.dom.Document document = documentBuilder.newDocument();
		org.w3c.dom.Element root = document.createElement("xml");
		document.appendChild(root);
		for (String key : data.keySet()) {
			String value = data.get(key);
			if (value == null) {
				value = "";
			}
			value = value.trim();
			org.w3c.dom.Element filed = document.createElement(key);
			filed.appendChild(document.createTextNode(value));
			root.appendChild(filed);
		}
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		DOMSource source = new DOMSource(document);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		String output = writer.getBuffer().toString(); // .replaceAll("\n|\r",
														// "");
		try {
			writer.close();
		} catch (Exception ex) {
		}
		return output;
	}
	
	/**
	 * XML格式字符串转换为Map
	 */
	public static Map<String, String> xmlToMap(String strXML) throws Exception {
		try {
			Map<String, String> data = new HashMap<String, String>();
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setExpandEntityReferences(false);
			documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			InputStream stream = new ByteArrayInputStream(strXML.getBytes("UTF-8"));
			org.w3c.dom.Document doc = documentBuilder.parse(stream);
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			for (int idx = 0; idx < nodeList.getLength(); ++idx) {
				Node node = nodeList.item(idx);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					org.w3c.dom.Element element = (org.w3c.dom.Element) node;
					data.put(element.getNodeName(), element.getTextContent());
				}
			}
			try {
				stream.close();
			} catch (Exception ex) {
				// do nothing
			}
			return data;
		} catch (Exception ex) {
			throw ex;
		}
	}

	
	/**
	 * 将Map中的数据转换成key1=value1&key2=value2的形式 不包含签名域signature
	 * 
	 * @param data
	 *            待拼接的Map数据
	 * @return 拼接好后的字符串
	 */
	public static String coverMap2String(Map<String, String> data) {
		TreeMap<String, String> tree = new TreeMap<String, String>();
		Iterator<Entry<String, String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			if (SDKConstants.param_signature.equals(en.getKey().trim())) {
				continue;
			}
			tree.put(en.getKey(), en.getValue());
		}
		it = tree.entrySet().iterator();
		StringBuffer sf = new StringBuffer();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			sf.append(en.getKey() + SDKConstants.EQUAL + en.getValue()
					+ SDKConstants.AMPERSAND);
		}
		return sf.substring(0, sf.length() - 1);
	}
	 /**
	  * 上送报文进行加密 
	  *  取出 私钥
	  *  对上送的数据进行ASCII排序 
	  *  进行rsa加密  SHA256RSA
	  *  对加密后信息 进行base64编码
	  * @param req
	  * @return
	  */
	 public Map<String ,String> sign(Map<String ,String> req){
		 PrivateKey privateKey = RsaCertUtils.getPriKeyPkcs12(atPrivateKeyPath, atPrivateKeyPwd, "PKCS12");

			byte[] byteSign = null;
			String stringSign = null;
			String stringData =coverMap2String(req);
			try {
				byteSign = SecureUtil.base64Encode(SecureUtil.signBySoft256(
						privateKey, stringData.getBytes()));
				stringSign = new String(byteSign);
				// 设置签名域值
				req.put("sign", stringSign);
			 
			} catch (Exception e) {
				LogUtil.writeErrorLog("Sign Error", e);
			}
		 return req;
	 }
	 /**
	  * 上送报文进行加密 
	  *  取出 公钥
	  *  对上送的数据进行ASCII排序 
	  *  进行rsa加密  SHA256RSA
	  *  对加密后信息 进行base64编码
	  * @param req
	  * @return
	  */
	 public boolean checkSign(Map<String ,String> req){
		 PublicKey publicKey = RsaCertUtils.getPubKey(atPublicKeyPath);
		 String sign=req.get("sign");
		 if ("".equals(sign)||null==sign||"null".equals(sign)) {
			logger.info("[银联-微信] 无需验签--验签成功");
			return true;
		}
		 req.remove("sign");
		 String stringData =coverMap2String(req);
         try { 
			boolean checkSign = SecureUtil.validateSignBySoft256(publicKey, SecureUtil.base64Decode(sign.getBytes()),stringData.getBytes("UTF-8")) ;
			if (checkSign) {
				logger.info("[银联-微信] 验签成功-验签成功");
			}else {
				logger.info("[银联-微信] 验签成功-验签失败");
			}
			logger.info("[银联-微信] 验签成功");
			return checkSign; 
		} catch (Exception e) { 
		    logger.info("[银联-微信]验签异常{}：返回信息{}",e,req);
			return false;
		}
	 }
	 
	 
	 public boolean checkSignForAlipay(Map<String ,String> req){
		 
		
		 PublicKey publicKey = RsaCertUtils.getPubKey(atAliPublicKeyPath);
		 String sign=req.get("sign");
		 if ("".equals(sign)||null==sign||"null".equals(sign)) {
			logger.info("[银联-支付宝]  无需验签--验签成功");
			return true;
		}
		
		 req.remove("sign");
		 String stringData =req.get("verSign");
         try { 
			boolean checkSign = SecureUtil.validateSignBySoft256(publicKey, SecureUtil.base64Decode(sign.getBytes()),stringData.getBytes("UTF-8")) ;
			if (checkSign) {
				logger.info("[银联-支付宝] 验签成功--验签成功");
			}else{
				logger.info("[银联-支付宝] 验签成功--验签失败");
			}
			
			return checkSign; 
		} catch (Exception e) { 
		    logger.info("[银联-支付宝]验签异常{}：返回信息{}",e,req);
			return false;
		}
	 }

	 /**
	  * 银联-支付宝
	  * 公共上送信息
	  * @param method
	  * @return
	  */
	 public Map<String , String> pubReq(String method,String appid) {
	    SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Map<String, String> req = new HashMap<String, String>();
//		发不通就用APPID:2017040606571877  PID:2088102170346152  测试
		req.put("app_id",  appid);//hrt :2016091100483756   银联：2017040606571877
		req.put("method",  method);
		req.put("format", "JSON");
		req.put("charset", "utf-8");
		req.put("sign_type", "RSA2");
		req.put("version", "1.0");
		req.put("notify_url", notifyUrl);
		req.put("timestamp",sm.format(new Date()));
		return req;
	}

	 
		/**
		 * 生成随机字符串
		 * 
		 * @param length
		 * @return
		 */
	public  String getRandomString(int length) { // length表示生成字符串的长度
			String base = "abcdefghijklmnopqrstuvwxyz0123456789";
			Random random = new Random();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < length; i++) {
				int number = random.nextInt(base.length());
				sb.append(base.charAt(number));
			}
			return sb.toString();
		}
	 /**
	  * 
	  * 根据method组装报文
	  *  商户入驻：method=ant.merchant.expand.indirect.create  
	  *  商户入驻查询：method=ant.merchant.expand.indirect.query
	  *  扫码交易预下单：method=alipay.trade.paycreate
	  *  条码交易：method=alipay.trade.pay
	  *  
	  * @param method
	  * @param merMsg
	  * @return
	  */
	 public  Map<String , String> getPackMessage(Map<String, Object> merMsg){
		 String method=merMsg.get("method").toString();
		 String channelId=merMsg.get("channel_id").toString();
		 String isCredit=String.valueOf(merMsg.get("iscredit")==null?"":merMsg.get("iscredit"));
		 Map<String, String> req =pubReq(method,channelId);
		 if ("ant.merchant.expand.indirect.create".equals(method)) {
			 //商户入驻
			 String pid=merMsg.get("mch_id").toString();
			 Map<String, Object> childReq=new TreeMap<String,Object>(); 
				childReq.put("external_id",merMsg.get("merchantid"));
				childReq.put("name", merMsg.get("merchantname"));
				childReq.put("alias_name", merMsg.get("shortname"));
				childReq.put("service_phone",  merMsg.get("servicephone"));
				String[] cates=String.valueOf(merMsg.get("category")).split("\\|");
				if (cates.length<1) {
					logger.info("[银联-支付宝]商户入驻获取category_id,mcc失败，cates={}",merMsg.get("category"));
					throw new HrtBusinessException(8000, "商户入驻失败");
				}
				childReq.put("category_id",  cates[0]);
				childReq.put("mcc",  cates[1]); 
//				childReq.put("mcc",  cates[0]); 
				childReq.put("source",  merMsg.get("appid")); 
				childReq.put("org_pid", pid); //pid  hrt:2088102175090441   銀聯： 2088102170346152
				//有营业执照号的时候上送本字段
				if (null!=merMsg.get("minfo2")&&!"".equals(merMsg.get("minfo2")+"")&&!"null".equals(merMsg.get("minfo2")+"")) {
					childReq.put("business_license", merMsg.get("minfo2")); 
					childReq.put("business_license_type", "NATIONAL_LEGAL");//pid  hrt:2088102175090441
				}
				
				JSONObject contact_info=new JSONObject(); 
				JSONArray bb=new JSONArray();
 				contact_info.put("name",  merMsg.get("contactname"));
				contact_info.put("tag", "08");
				contact_info.put("type", "OTHER");
				contact_info.put("email", "saoma@hrtpayment.com");
				if (null!=merMsg.get("contactmobile")&&!"".equals(merMsg.get("contactmobile"))||null!=merMsg.get("contactmobile")) {
					contact_info.put("mobile",merMsg.get("contactmobile") );
				}
				//身份证号字段不为空时上送本字段
				if (null!=merMsg.get("minfo1")&&!"".equals(merMsg.get("minfo1"))&&!"null".equals(merMsg.get("minfo1"))) {
					contact_info.put("id_card_no",merMsg.get("minfo1") );
				}
				bb.add(contact_info);
				childReq.put("contact_info",bb);// contact_info);
				JSONObject address_info=new JSONObject(); 
				JSONArray aa=new JSONArray();
				address_info.put("address", merMsg.get("merchantaddress"));
				address_info.put("city_code", merMsg.get("citycode"));
				address_info.put("district_code", merMsg.get("districtcode"));
				address_info.put("province_code", merMsg.get("provincecode"));
				aa.add(address_info);
				childReq.put("address_info",aa);
				JSONObject bankcard_info=new JSONObject(); 
				JSONArray bankcardJson=new JSONArray();
				//银行卡号字段不为空时上送本字段
				if (null!=merMsg.get("CONTACTACCNO")&&!"".equals(merMsg.get("CONTACTACCNO"))&&!"null".equals(merMsg.get("CONTACTACCNO"))) {
					bankcard_info.put("card_no", merMsg.get("CONTACTACCNO"));
					bankcard_info.put("card_name", merMsg.get("contactname"));
					bankcardJson.add(bankcard_info);
					childReq.put("bankcard_info",bankcardJson);
				}
				req.put("biz_content", JSONObject.toJSON(childReq).toString());//JSONObject.toJSONString(childReq ));
		 }else if("ant.merchant.expand.indirect.modify".equals(method)){
			//商户入驻修改
			 String pid=merMsg.get("mch_id").toString();
			 Map<String, Object> childReq=new TreeMap<String,Object>(); 
				childReq.put("external_id",merMsg.get("merchantid"));
				childReq.put("name", merMsg.get("merchantname"));
				childReq.put("alias_name", merMsg.get("shortname"));
				childReq.put("service_phone",  merMsg.get("servicephone"));
//				childReq.put("category_id",  merMsg.get("category"));
				String[] cates=String.valueOf(merMsg.get("category")).split("\\|");
				if (cates.length<1) {
					logger.info("[银联-支付宝]商户入驻修改获取category_id,mcc失败，cates={}",merMsg.get("category"));
					throw new HrtBusinessException(8000, "商户入驻失败");
				}
				childReq.put("category_id",  cates[0]);
				childReq.put("mcc",  cates[1]); 
				childReq.put("source", pid); //pid  hrt:2088102175090441   銀聯： 2088102170346152
				childReq.put("org_pid", pid); 
				//有营业执照号的时候上送本字段
				if (null!=merMsg.get("minfo2")&&!"".equals(merMsg.get("minfo2")+"")&&!"null".equals(merMsg.get("minfo2")+"")) {
					childReq.put("business_license", merMsg.get("minfo2")); 
					childReq.put("business_license_type", "NATIONAL_LEGAL");//pid  hrt:2088102175090441
				}
				JSONObject contact_info=new JSONObject(); 
				JSONArray bb=new JSONArray();
 				contact_info.put("name",  merMsg.get("contactname"));
				contact_info.put("tag", "08");
				contact_info.put("type", "LEGAL_PERSON");
				//身份证号字段不为空时上送本字段
				if (null!=merMsg.get("minfo1")&&!"".equals(merMsg.get("minfo1")+"")&&!"null".equals(merMsg.get("minfo1")+"")) {
					contact_info.put("id_card_no",merMsg.get("minfo1") );
				}
				if (null!=merMsg.get("contactmobile")&&!"".equals(merMsg.get("contactmobile")+"")&&!"null".equals(merMsg.get("contactmobile")+"")) {
					contact_info.put("phone",  merMsg.get("contactmobile"));
				}
				bb.add(contact_info);
				childReq.put("contact_info",bb);// contact_info);
				JSONObject address_info=new JSONObject(); 
				JSONArray aa=new JSONArray();
				address_info.put("address", merMsg.get("merchantaddress"));
				address_info.put("city_code", merMsg.get("citycode"));
				address_info.put("district_code", merMsg.get("districtcode"));
				address_info.put("province_code", merMsg.get("provincecode"));
				aa.add(address_info);
				childReq.put("address_info",aa);
				JSONObject bankcard_info=new JSONObject(); 
				JSONArray bankcardJson=new JSONArray();
				//银行卡号字段不为空时上送本字段
				if (null!=merMsg.get("CONTACTACCNO")&&!"".equals(merMsg.get("CONTACTACCNO")+"")&&!"null".equals(merMsg.get("CONTACTACCNO")+"")) {
					bankcard_info.put("card_no", merMsg.get("CONTACTACCNO"));
					bankcard_info.put("card_name", merMsg.get("contactname"));
					bankcardJson.add(bankcard_info);
					childReq.put("bankcard_info",bankcardJson);
				}
				req.put("biz_content", JSONObject.toJSON(childReq).toString());//JSO
			 
		 }else if ("ant.merchant.expand.indirect.query".equals(method)){
			 //商户入驻查询
				Map<String, Object> childReq=new TreeMap<String,Object>();
				String pid=merMsg.get("mch_id").toString();
				childReq.put("external_id", merMsg.get("merchantid"));
				childReq.put("org_pid", pid); 
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }else if ("alipay.trade.precreate".equals(method)){
			 //扫码交易  预下单 
				Map<String, Object> childReq= new HashMap<String, Object>();
				childReq.put("out_trade_no",merMsg.get("orderid"));
				childReq.put("subject", merMsg.get("subject"));
				childReq.put("total_amount",merMsg.get("amount"));
				childReq.put("qr_code_timeout_expres", "90m");
				JSONObject sub_merchant=new JSONObject(); 
				sub_merchant.put("merchant_id", merMsg.get("bankmid"));//"2018032817054963"
				childReq.put("sub_merchant", sub_merchant);
				/* 
				 * 2018-12-06  修改
				 * 
				 * 根据isCredit判断该商户是否可以使用贷记卡交易
				 * 1 可以  9 不可以
				 *  
				 */
				if ("9".equals(isCredit)) {
					//不可以使用信用卡交易
					childReq.put("disable_pay_channels","credit_group,pcredit");//关闭信用卡、花呗功能
				}
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }else if ("alipay.trade.create".equals(method)){
			 //扫码交易   下单 
				Map<String, Object> childReq= new HashMap<String, Object>();
				childReq.put("out_trade_no",merMsg.get("orderid"));
				childReq.put("subject", merMsg.get("subject"));
				childReq.put("total_amount",merMsg.get("amount"));
				childReq.put("qr_code_timeout_expres", "90m");
				childReq.put("buyer_id", merMsg.get("buyer_id"));
				JSONObject sub_merchant=new JSONObject(); 
				sub_merchant.put("merchant_id", merMsg.get("bankmid"));//"2018032817054963"
				childReq.put("sub_merchant", sub_merchant);
				/* 
				 * 2018-12-06  修改
				 * 
				 * 根据isCredit判断该商户是否可以使用贷记卡交易
				 * 1 可以  9 不可以
				 *  
				 */
				if ("9".equals(isCredit)) {
					//不可以使用信用卡交易
					childReq.put("disable_pay_channels","credit_group,pcredit");//关闭信用卡、花呗功能
				}
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }else if ("alipay.trade.pay".equals(method)){
			 //条码交易 
				Map<String, Object> childReq= new TreeMap<String, Object>();
				childReq.put("out_trade_no", merMsg.get("orderid"));
				childReq.put("scene", "bar_code");
				childReq.put("auth_code", merMsg.get("authcode"));
				childReq.put("subject", merMsg.get("subject"));
				childReq.put("total_amount",merMsg.get("amount"));
				JSONObject sub_merchant=new JSONObject(); 
				sub_merchant.put("merchant_id", merMsg.get("bankmid"));
				childReq.put("sub_merchant", sub_merchant);
				//childReq.put("buyer_logon_id", "irfiat4830@sandbox.com");
				/* 
				 * 2018-12-06  修改
				 * 
				 * 根据isCredit判断该商户是否可以使用贷记卡交易
				 * 1 可以  9 不可以
				 *  
				 */
				if ("9".equals(isCredit)) {
					//不可以使用信用卡交易
					childReq.put("disable_pay_channels","credit_group,pcredit");//关闭信用卡、花呗功能
				}
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }else if ("alipay.trade.query".equals(method)){
			 //交易查询
				Map<String, Object> childReq= new TreeMap<String, Object>();
				childReq.put("out_trade_no", merMsg.get("orderid"));
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }else if ("alipay.trade.refund".equals(method)){
			 //退款
				Map<String, Object> childReq= new TreeMap<String, Object>();
				childReq.put("out_trade_no", merMsg.get("mer_orderid"));
				childReq.put("refund_amount", merMsg.get("txnamt"));
				childReq.put("out_request_no",merMsg.get("orderid"));
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }else if ("alipay.trade.fastpay.refund.query".equals(method)){
			 //退款查询 
				Map<String, Object> childReq= new TreeMap<String, Object>();
				childReq.put("out_trade_no", merMsg.get("oriorderid")); 
				childReq.put("out_request_no",merMsg.get("orderid"));
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 } else if ("alipay.trade.close".equals(method)){
			 //关闭
				Map<String, Object> childReq= new TreeMap<String, Object>();
				childReq.put("out_trade_no", merMsg.get("mer_orderid"));
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }else if ("alipay.trade.cancel".equals(method)){
			 //撤销
				Map<String, Object> childReq= new TreeMap<String, Object>();
				childReq.put("out_trade_no", merMsg.get("mer_orderid")); 
				req.put("biz_content", JSONObject.toJSON(childReq).toString());
		 }
		 
		 return  sign(req);
	 }
	 
	 
	 
}
