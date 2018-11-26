package com.hrtpayment.xpay.quickpay.ldpay.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.quickpay.ldpay.util.SunBase64;
import com.hrtpayment.xpay.utils.exception.BusinessException;


@Service
public class LdPayServerServce {
	Logger logger = LogManager.getLogger();
	@Value("${ldpay.merid}")
	private String ldmerid;
	
	/**
	 * 解析请求参数为JSON字符串
	 * 
	 * @param request
	 *            请求
	 * @return
	 * @author
	 * @throws Exception
	 * @date 2016年10月17日 下午9:17:35
	 */
	public Map<String, String> parseRequest2JsonStr(HttpServletRequest request) throws Exception {
		// 读取参数
		Map<String, String> map =new TreeMap<String, String>();
		Enumeration<String> enu=request.getParameterNames();  
		while(enu.hasMoreElements()){  
		String paraName=(String)enu.nextElement();  
		map.put(paraName, request.getParameter(paraName));
		}
		return map;
	}
	
	
	
    /**
     * 获取公钥 并加密
     * 	
     * @param path
     * @param s
     * @return
     * @throws BusinessException
     */
	public String enctry(String path,String s) throws BusinessException{
		try {			
			byte[] b = null;
			InputStream in = null;
			try{
				in = new FileInputStream(new File(path));
				if(null == in)throw new RuntimeException("文件不存在"+path);
				b = new byte[20480];
				in.read(b);
			}catch(Exception e){
				 throw new BusinessException(8000, "获取公钥证书异常"+e.getMessage());
			}finally{
				if(null!=in)in.close();
			}
		    ByteArrayInputStream bais = new ByteArrayInputStream(b);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate x509Certificate = (X509Certificate)cf.generateCertificate(bais);
			byte[] keyBytes = x509Certificate.getPublicKey().getEncoded();
		    // 取得公钥
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
			 
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			Key publicKey = keyFactory.generatePublic(x509KeySpec);

			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			String str = SunBase64.encode(cipher.doFinal(s.getBytes("GBK"))).replace("\n", "");
			return str;
		} catch (Exception e) {
			 throw new BusinessException(8000, "加密异常"+e.getMessage());
		}
		  
	}
	
	public Map<String, Object>  getResp(String content){
		
		Map<String, Object> respMap=new HashMap<String,Object>();
		String[] respStr=content.split("&");
		for (int i = 0; i < respStr.length; i++) {
			String[] respParamer=respStr[i].split("=",2);
			String key=respParamer[0];
			String value=respParamer[1];
			respMap.put(key, value);
		}
		return respMap;
	}
	
	public  static String formatCardNo(String cardNo) {
		if (cardNo == null || "".equals(cardNo)) {
			return "";
		}
		return cardNo.replaceAll("(?<=\\d{6})\\d(?=\\d{4})", "*");
	}
	
	/**
	 * 组装签名字符串(sign字段除外)
	 * @param req
	 * @return
	 */
	public String getSignBlock(Map<String,String> req){
		String sign = null;
		if (req.containsKey("sign")) {
			sign = req.get("sign");
			req.remove("sign");
		}
		if (req.containsKey("signType")) {
			sign = req.get("signType");
			req.remove("signType");
		}
		String[] keys = req.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			sb.append(key).append("=").append(req.get(key)).append("&");
		}
		if (sb.length()>1) {
			sb.deleteCharAt(sb.length()-1);
		}
		if (sign != null) {
			req.put("sign", sign);
		}
		return sb.toString();
	}
	
	
	
	
}
