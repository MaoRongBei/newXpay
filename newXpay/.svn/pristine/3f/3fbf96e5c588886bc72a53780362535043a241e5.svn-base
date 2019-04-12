package com.hrtpayment.xpay.quickpay.newCups.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.quickpay.cups.util.CTime;
import com.hrtpayment.xpay.quickpay.cups.util.HttpClientUtil;
import com.hrtpayment.xpay.quickpay.cups.util.RsaCertUtils;
import com.hrtpayment.xpay.quickpay.cups.util.RsaUtils;
import com.hrtpayment.xpay.quickpay.cups.util.ShaUtils;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgBodyBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgHeaderBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayRootBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.RcverInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.SderInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.TrxInfBean;
import com.thoughtworks.xstream.XStream;

@Controller
@RequestMapping("xpay")

public class testPreSign {
	
	@Value("${cupsquickpay.testUrl}")
	private String Url;
	
	
	/**
	 * 协议支付签约 触发短信
	 * 
	 * @param args
	 */
	@RequestMapping("testCupsQuickPay")
	@ResponseBody
	public   void main(String[] args) {
		
		//response.addHeader("MsgTp", "0001");
//        response.addHeader("OriIssId","48640000");
//		System.out.println(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		XStream xstream = new XStream();  
		xstream.autodetectAnnotations(true);  
	 /**
	  * 
	  */
		CupsQuickPayMsgHeaderBean headerBean=new CupsQuickPayMsgHeaderBean();
		String msgTp="0001";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId("48641000");
		headerBean.setDrctn("11");
		headerBean.setSignSN("4000370693");//4000370671
//		headerBean.setEncSN("4000370671");
//		headerBean.setEncKey("");
		headerBean.setMDAlgo("0");
		headerBean.setSignEncAlgo("0");
//		headerBean.setEncAlgo("0");
		
		CupsQuickPayMsgBodyBean bodyBean =new CupsQuickPayMsgBodyBean();
		/**
		 * 交易信息
		 */
		TrxInfBean trxInfBean=new TrxInfBean();
		trxInfBean.setTrxId("0131202721000000");
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
//		trxInfBean.setSettlmtDt("");
		trxInfBean.setTrxAmt("CNY00000000000000100.00");
		/**
		 * 接收方信息
		 */
		RcverInfBean rcverInfBean= new RcverInfBean();
		rcverInfBean.setRcverAcctIssrId("");//机构方无需填写
		rcverInfBean.setRcverAcctId("6212143000000000052");//接收方账户号6212143000000000011
		rcverInfBean.setRcverNm("银联一");//接收方名称 银联一
		rcverInfBean.setIDTp("02");//接收方证件类型  01 身份证  03 护照
		rcverInfBean.setIDNo("310115198903261113");//接收方证件号  310115198903261113
		rcverInfBean.setMobNo("13111111111");//13111111111");//接收方手机号
		//接收方证件号
		//接收方预留手机号
		/**
		 * 发起方 信息
		 */
		SderInfBean sderInfBean=new SderInfBean();
		sderInfBean.setSderIssrId("48641000");
		sderInfBean.setSderAcctIssrId("48641000");
		
		bodyBean.setBizTp("100003");
		bodyBean.setTrxInf(trxInfBean);
		bodyBean.setRcverInf(rcverInfBean);
		bodyBean.setSderInf(sderInfBean);
		CupsQuickPayRootBean rootBean=new CupsQuickPayRootBean();
		rootBean.setMsgHeader(headerBean);
		rootBean.setMsgBody(bodyBean);
		
//		System.out.println(xstream.toXML(rootBean));
		
		
		/**
		 * 生成摘要
		 */
		String root=xstream.toXML(rootBean).replace(" ","");
		
		
		String rootSHA256= Hex.toHexString((ShaUtils.sha256(root.getBytes())));
//		System.out.println(root);
		
		/**
		 * 机构私钥加密
		 */
		System.out.println(System.getProperty("user.dir") +"/conf/yl-rsa-签名证书.pfx");
//		rootSHA256; 

		String encode = null ;
		
		
		PrivateKey privateKey = RsaCertUtils.getPriKeyPkcs12(System.getProperty("user.dir") +"/conf/jg-签名证书（含私钥）（非加密机签名机模式）.pfx", "11111111", "PKCS12");//"PKCS12");
//		byte[] res = Base64.encodeBase64(privateKey.getEncoded());
		
		System.out.println("摘要串： "+ rootSHA256);
		byte[] srcHex = rootSHA256.getBytes();
	
 		byte[] result = RsaUtils.signWithSha256((RSAPrivateKey) privateKey,(ShaUtils.sha256(root.getBytes())));//srcHex);	

		/**
		 * BASE64转码
		 */
		String sign="";	
	    encode =Base64.encodeBase64String(result);
 
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
	 
		 
		try {
//			HttpConnectService.sendMessage(zb.toString(), Url);//.sendPostToService( Url,zb.toString());//
//			HttpXmlClient.post(Url, send,msgTp);
			HttpClientUtil.httpPost(Url, send, msgTp);
		} catch (Exception e) {
			 System.out.println("发送异常返回："+e );
		}
	}
	
	private  static byte[] compress(String str, String encoding) {
        if (str == null || str.length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes(encoding));
            gzip.close();
        } catch ( Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

	  private   byte[] uncompress(byte[] bytes) {
	        if (bytes == null || bytes.length == 0) {
	            return null;
	        }
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
	        try {
	            GZIPInputStream ungzip = new GZIPInputStream(in);
	            byte[] buffer = new byte[256];
	            int n;
	            while ((n = ungzip.read(buffer)) >= 0) {
	                out.write(buffer, 0, n);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return out.toByteArray();
	    }
	
	@RequestMapping("testResp")
	@ResponseBody
	public   void testResp(HttpServletRequest request) {
		// 读取参数
		Map<String, String> map =new TreeMap<String, String>();
		int le=request.getContentLength();
		String type=request.getContentType();
		
		BufferedReader br;
		try {
			br = request.getReader();
			String str, wholeStr = "";
			while ((str = br.readLine()) != null) {
				wholeStr += str;
			}
			System.out.println(wholeStr);
			System.out.println("111111111111111111111");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
  
//		Enumeration<String> names=request.getHeaderNames();
// 
//		Enumeration<String> enu=request.getParameterNames();  
//		while(enu.hasMoreElements()){  
//		String paraName=(String)enu.nextElement();  
//		map.put(paraName, request.getParameter(paraName));
//		}
	}
	
}
