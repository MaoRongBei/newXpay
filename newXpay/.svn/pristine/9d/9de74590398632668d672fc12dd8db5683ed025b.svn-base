package com.hrtpayment.xpay.quickpay.newCups.service;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.quickpay.cups.util.RsaCertUtils;
import com.hrtpayment.xpay.quickpay.cups.util.RsaUtils;
import com.hrtpayment.xpay.quickpay.cups.util.ShaUtils;
import com.hrtpayment.xpay.utils.crypto.AesUtil;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;


@Service
public class newCupsService {
	

	Logger logger = LogManager.getLogger();
	@Value("${quick.cardEncKey}") // 快捷支付加密卡号秘钥
	private String encryKey;
	@Value("${cupsquickpay.privatekey.path}")
	private String privateKeyPath;
	
	@Value("${cupsquickpay.publickey.path}")
	private String publickeypath;
	
	
	@Value("${cupsquickpay.privateKey.passwd}")
	private String priPasswd;
	
	
	public String createOrder(int num){
		String base = "0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 8; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmss");
		String orderid =   sdf.format(d) + sb.toString();
 
		return orderid.substring(0,num);
	}
	
	
	  
	  private  Map<String, String>  strToMap(Element element,Map<String, String> map){
		try {
			 List<Element> elements=element.elements();
		        for(int i=0;i<elements.size();i++){
		        	 Element memberElm=elements.get(i);
		        	 if (memberElm.elements().size()==0) {
		        		  map.put(memberElm.getName(), memberElm.getTextTrim());
						 }else {
							 strToMap(memberElm,map);
						 }
		        }
				  return map;
		} catch (Exception e) {
			  logger.error("[银联快捷交易]获取xml报文节点内数据异常：{}",e.getMessage());
			  throw new HrtBusinessException(8000,"交易失败");
		} 
	   
	  }
	  
	  public Map<String, String> xmlToMap(String xml){
		  try { 
			  Map<String, String> data = new HashMap<String, String>();
			  
			  /***
			   * 根据节点的名字获取 节点的内容  
			   *  用来进行判断操作
			   *  
			   *  验签！！！！！！！
			   */ 
	            org.dom4j.Document doc = null;
	            try {
	                  doc =  DocumentHelper.parseText(xml.substring(0, xml.indexOf("{S:")));
	            } catch (DocumentException e) { 
	            }
	            Element rootElement = doc.getRootElement();
	            Map<String,Object> mapXml = new HashMap<String,Object>(); 
	            List<Element> list=rootElement.elements();
	            Element memberElm=rootElement ; 
//	            memberElm.elementTextTrim("SysRtnCd");
//	            System.out.println("memberElm.getName()="+memberElm.elements().size());
	            Map<String, String> map=new LinkedHashMap <String, String>();
	            return strToMap(memberElm,map);
		  }catch(Exception e){
			  logger.error("[银联快捷交易]获取xml报文内数据异常：{}",e.getMessage());
			  throw new HrtBusinessException(8000,"交易失败");
		  }
	  }
	  
	  
	 public String getEncodeSHA(String root) {
		  /**
			 * 生成摘要
			 */
//			String rootSHA256= Hex.toHexString((ShaUtils.sha256(root.getBytes())));
			String encode = null ;
			PrivateKey privateKey = RsaCertUtils.getPriKeyPkcs12(privateKeyPath, priPasswd, "PKCS12");//"PKCS12");
 
	 		byte[] result = RsaUtils.signWithSha256((RSAPrivateKey) privateKey,(ShaUtils.sha256(root.getBytes())));// srcHex);	
			/**
			 * BASE64转码
			 */
			try {
				encode = new String(Base64.encodeBase64(result), "utf-8");
			} catch (UnsupportedEncodingException e) {
				logger.info("[银联快捷]转码失败：{} "+ e);
			}
			return encode; 
	}
	 
		public String decodeByAES(String msg) {
			String decodeMsg = "";
			try {
				byte[] msgByte =com.hrtpayment.xpay.utils.Base64.decode(msg);
				decodeMsg = new String(AesUtil.decrypt(msgByte, encryKey.getBytes(), "AES/ECB/PKCS5Padding", null),
						"UTF-8");
			} catch (Exception e) {
				logger.info("[快捷支付]{}解密异常{}",msg,e.getMessage());
				return "";
			}
			return decodeMsg;
		}
}
