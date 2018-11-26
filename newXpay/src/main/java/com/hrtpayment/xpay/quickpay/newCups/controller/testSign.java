package com.hrtpayment.xpay.quickpay.newCups.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import com.hrtpayment.xpay.quickpay.newCups.bean.OriTrxInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.RcverInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.SderInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.TrxInfBean;
import com.thoughtworks.xstream.XStream;

@Controller
@RequestMapping("xpay")

public class testSign {
	
	
	/**
	 * 协议支付签约
	 */
	@Value("${cupsquickpay.testUrl}")
	private String Url;
	
	@RequestMapping("testCupsQuickPaySign")
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
		String msgTp="0201";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId("48641000");
		headerBean.setDrctn("11");
		headerBean.setSignSN("4000370693");
//		headerBean.setEncSN("");
//		headerBean.setEncKey("");
		headerBean.setMDAlgo("0");
		headerBean.setSignEncAlgo("0");
//		headerBean.setEncAlgo("");
		
		CupsQuickPayMsgBodyBean bodyBean =new CupsQuickPayMsgBodyBean();
		/**
		 * 交易信息
		 */
		TrxInfBean trxInfBean=new TrxInfBean();
		trxInfBean.setTrxId("0131202721000000");
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
//		trxInfBean.setSettlmDt("");
//		trxInfBean.setTrxAmt("");
		/**
		 * 接收方信息
		 */
		RcverInfBean rcverInfBean= new RcverInfBean();
		rcverInfBean.setRcverAcctIssrId("");//机构方无需填写
		rcverInfBean.setRcverAcctId("6212143000000000052");//接收方账户号6212143000000000011
		rcverInfBean.setRcverNm("银联一");//接收方名称
		rcverInfBean.setIDTp("02");//接收方证件类型
		rcverInfBean.setIDNo("310115198903261113");//接收方证件号
		rcverInfBean.setMobNo("13111111111");//接收方手机号
		rcverInfBean.setAuthMsg("123456");
		rcverInfBean.setSmsKey("20180518175305000088");
		//接收方证件号
		//接收方预留手机号
		/**
		 * 发起方 信息
		 */
		SderInfBean sderInfBean=new SderInfBean();
		sderInfBean.setSderIssrId("48641000");
		sderInfBean.setSderAcctIssrId("48641000");
		sderInfBean.setSderAcctInf("6212143000000000010");
		
		OriTrxInfBean oriTrxInfBean=new OriTrxInfBean();
		oriTrxInfBean.setOriTrxId("0131202721000000");
		
		
		bodyBean.setBizTp("100003");
		bodyBean.setTrxInf(trxInfBean);
		bodyBean.setRcverInf(rcverInfBean);
		bodyBean.setSderInf(sderInfBean);
		bodyBean.setOriTrxInf(oriTrxInfBean);
		
		CupsQuickPayRootBean rootBean=new CupsQuickPayRootBean();
		rootBean.setMsgHeader(headerBean);
		rootBean.setMsgBody(bodyBean);
		
//		System.out.println(xstream.toXML(rootBean));
		
		
		/**
		 * 生成摘要
		 */
		String root=xstream.toXML(rootBean);
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
//		PublicKey publicKey = RsaCertUtils.getPubKey(System.getProperty("user.dir") +"/conf/jg-签名证书公钥（非加密机签名机模式）.cer");
//		byte[] result = RsaUtils.rsaEcbEncrypt((RSAPublicKey) publicKey, srcHex, Constants.PAD_PKCS1);
		byte[] result = RsaUtils.signWithSha256((RSAPrivateKey) privateKey,(ShaUtils.sha256(root.getBytes())));//srcHex);// srcHex);	
//		byte[] signData = CryptoUtil.digitalSign(root.getBytes("UTF-8"), privateKey, "SHA1WithRSA");// 签名

		/**
		 * BASE64转码
		 */
		try {
			encode = new String(Base64.encodeBase64(result), "utf-8");
		} catch (UnsupportedEncodingException e) {
//			logger.error("Fail: ", e);
			System.out.println("Fail: "+ e);
		}
//		logger.info("encode:" + encode);
//		System.out.println("encode:" + encode);
		
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
		//String.valueOf(param.replaceAll(" ", "").length())
		//<root><MsgHeader><MsgVer>1000</MsgVer><SndDt>2018-02-02T21:02:03</SndDt><Trxtyp>0001</Trxtyp><IssrId>48640000</IssrId><Drctn>11</Drctn><SignSN>4000370671</SignSN><EncSN></EncSN><EncKey></EncKey><MDAlgo>0</MDAlgo><SignEncAlgo>0</SignEncAlgo><EncAlgo></EncAlgo></MsgHeader><MsgBody><trxInf><TrxId>0131202721000000</TrxId><TrxDtTm>20180131</TrxDtTm><SettlmDt></SettlmDt><TrxAmt></TrxAmt></trxInf><rcverInf><RcverAcctIssrId></RcverAcctIssrId><RcverAcctId>6212143000000000011</RcverAcctId><RcverNm>银联1</RcverNm><IDTp>01</IDTp><IDNo>310115198903261113</IDNo><MobNo>13111111111</MobNo></rcverInf></MsgBody></root>bHVOAUCzZlEmUwdGrGc8icnHczlZqymRKp4bmuveMDaJe7rjs2ALclKSHgfWANsbB6K5iCt5Wdzck2ISNSnvhDYVIy/2tmHc1TH9mO2Ak5GePndVClhSLwJDocWsmoQB2CDsxamg8jX59ZJ4giRDmx8fnLbk78s4eUbEKfxKp1K75o2c1OctbvI9zMN40i36Zg933R8zU2eKVL0FwTeV+3prXeDZkojAPzRxVnr8/ut+YNEZs3aCnLDiZcMRbwpmzkE2d/IS6AdNGrNpK9DZuOcFfTvkcGl5lxQgO6c3HbDr+fmBv654lkipiARaGg7y8H9CXGQ5SRExnAln8pl7jQ==

		
//		byte[] zb = null;
//		String resmsg ="";
//		try {
//			zb = ZipHelper2.inflater(send.getBytes("UTF-8"));
//			resmsg= new String(zb, "UTF-8");
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		// byte[]类型的返回数据 转化成string类型的 进行操作
//		
	 
//			byte[] b =new ByteArrayInputStream(send.getBytes());
//		String ss="<?xml version=\"1.0\" encoding=\"utf-8\"?>"
//				+ "<root><MsgHeader><MsgVer>1000</MsgVer><SndDt>"
//				+ "2017-02-16T18:00:00</SndDt><Trxtyp>0002</Trxtyp>"
//				+ "<IssrId>00123456789</IssrId><Drctn>11</Drctn><SignSN>a1c34512e034a56d20c00d000f020400</SignSN><EncSN>00a10c4000f266a00069</EncSN><EncKey>1f014e11a12f31d1</EncKey><MDAlgo>1</MDAlgo><SignEncAlgo>1</SignEncAlgo><EncAlgo>1</EncAlgo></MsgHeader><MsgBody><BizTp>100004</BizTp><TrxInf><TrxId>0630062958100088</TrxId><TrxDtTm>2017-02-16T18:00:40</TrxDtTm></TrxInf><RcverInf><RcverAcctIssrId>01020000</RcverAcctIssrId><RcverAcctId>1234567890</RcverAcctId><RcverNm>pengqiu</RcverNm><IDTp>01</IDTp><IDNo>200001120010000100</IDNo><MobNo>13800000000</MobNo></RcverInf><SderInf><SderAcctIssrId>01020000</SderAcctIssrId><SderAcctId>11111111111</SderAcctId></SderInf><RskInf><deviceMode>01</deviceMode><deviceLanguage>001</deviceLanguage><sourceIP>172.17.254.243</sourceIP><MAC>00247e0a6c2e00247e0a6c2e00247e0a6c2e00247e0a6c2e</MAC><devId>1235455855903434939</devId><extensiveDeviceLocation>+37.12/-121.23</extensiveDeviceLocation><deviceNumber>13800000001</deviceNumber><deviceSIMNumber>2</deviceSIMNumber><accountIDHash>1a0e0c0246809fe5c2</accountIDHash><riskScore>10</riskScore><riskReasonCode>0069</riskReasonCode><mchntUsrRgstrTm>20170216140004</mchntUsrRgstrTm><mchntUsrRgstrEmail>www.unionpay.com</mchntUsrRgstrEmail><rcvProvince>003</rcvProvince><rcvCity>1200</rcvCity><goodsClass>01</goodsClass></RskInf></MsgBody></root>{S:11111ace56cdefabc1287665acbf2856}";		
//			byte[] b = compress(ss,"UTF-8"); 
//			zb=uncompress(b);
		 
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
