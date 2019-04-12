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
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.quickpay.cups.util.CTime;
import com.hrtpayment.xpay.quickpay.cups.util.RsaCertUtils;
import com.hrtpayment.xpay.quickpay.cups.util.RsaUtils;
import com.hrtpayment.xpay.quickpay.cups.util.ShaUtils;
import com.hrtpayment.xpay.quickpay.cups.util.XmlUtil;
import com.hrtpayment.xpay.quickpay.newCups.bean.BizInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgBodyBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgHeaderBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayRootBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.RcverInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.SderInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.SysRtnInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.TrxInfBean;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.thoughtworks.xstream.XStream;

@Controller
@RequestMapping("xpay")
public class testRelieveSign {
	
	
	/**
	 * 协议支付签约
	 */
	@Value("${cupsquickpay.testUrl}")
	private String Url;
	
 
	
/**
  
 * "SderReserved=\"\"","RcverReserved=\"\"","CupsReserved=\"\"",
 * @param request
 * @return
 */
	@RequestMapping(value="testRelieveSign")
	@ResponseBody 
	public String main(@RequestBody String wholeStr,HttpServletRequest request , HttpServletResponse response) {

		wholeStr=wholeStr.substring(0, wholeStr.length()-1).replace("{", "\"").split("\"")[0];
		Map<String, String> map = SimpleXmlUtil.xml2map(wholeStr);
		wholeStr="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+wholeStr;
		
        response.setContentType("application/xml"); 
        response.addHeader("Connection","close");
        response.addHeader("Content-Type", "application/xml;charset=utf-8");
        response.addHeader("SderReserved", ""); 
        response.addHeader("RcverReserved", ""); 
        response.addHeader("CupsReserved", "");
    	response.addHeader("MsgTp", "0303");
        response.addHeader("OriIssrId","W0ISS001");
        
		XmlUtil xmlUtil=new XmlUtil();
		JSONObject json=null;
		try {
			json = xmlUtil.documentToJSONObject(wholeStr);
		} catch (DocumentException e1) {
			System.out.println("xml转换成json异常"+e1.getMessage());
		}
		JSONArray msgBody=json.getJSONArray("MsgBody");
		System.out.println(msgBody.get(0).toString() );
		JSONObject msgBodyData=JSONObject.parseObject(msgBody.get(0).toString());
		
		System.out.println(msgBodyData);
		JSONArray trxInf=msgBodyData.getJSONArray("TrxInf");
		System.out.println(trxInf);
		JSONObject trxInfData=JSONObject.parseObject(trxInf.get(0).toString());
		String TrxId=trxInfData.getString("TrxId");
		XStream xstream = new XStream();  
		xstream.autodetectAnnotations(true);  
	 /**
	  * 
	  */
		CupsQuickPayMsgHeaderBean headerBean=new CupsQuickPayMsgHeaderBean();
		String msgTp="0303";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId("48641000");
		headerBean.setDrctn("12");
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
		trxInfBean.setTrxId(TrxId);//交易流水号
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));//清算日期 
		trxInfBean.setSettlmtDt(new CTime().formatDate(new Date(), "yyyy-mm-dd"));
 
		SysRtnInfBean sysRtnInfBean=new SysRtnInfBean();
		sysRtnInfBean.setSysRtnCd("00000000");
		sysRtnInfBean.setSysRtnDesc("OK");
		sysRtnInfBean.setSysRtnTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));//清算日期 
 
		/**
		 * 接收方信息
		 */
		RcverInfBean rcverInfBean= new RcverInfBean();
		rcverInfBean.setRcverAcctIssrId("48641000");//机构方无需填写 
		/**
		 * 发起方 信息
		 */
		SderInfBean sderInfBean=new SderInfBean();
		sderInfBean.setSderIssrId("W0ISS001");
		sderInfBean.setSderAcctIssrId("W0ISS001"); 
		
		BizInfBean bizInfBean =new BizInfBean();
		bizInfBean.setSgnNo("UPW0ISS0014864100001048641000W0ISS001201805180000000063");
		
		
		bodyBean.setBizTp("100001");
		bodyBean.setTrxInf(trxInfBean);
		bodyBean.setRcverInf(rcverInfBean);
		bodyBean.setSderInf(sderInfBean);
		bodyBean.setBizInf(bizInfBean);
        bodyBean.setSysRtnInf(sysRtnInfBean);
		
		CupsQuickPayRootBean rootBean=new CupsQuickPayRootBean();
		rootBean.setMsgHeader(headerBean);
		rootBean.setMsgBody(bodyBean);
		
		
		/**
		 * 生成摘要
		 */
		String root=xstream.toXML(rootBean);
		String rootSHA256= Hex.toHexString((ShaUtils.sha256(root.getBytes())));
		
		/**
		 * 机构私钥加密
		 */
		System.out.println(System.getProperty("user.dir") +"/conf/yl-rsa-签名证书.pfx");

		String encode = null ;
		
		
		PrivateKey privateKey = RsaCertUtils.getPriKeyPkcs12(System.getProperty("user.dir") +"/conf/jg-签名证书（含私钥）（非加密机签名机模式）.pfx", "11111111", "PKCS12");//"PKCS12");

		System.out.println("摘要串： "+ rootSHA256);
		byte[] srcHex = rootSHA256.getBytes();
		byte[] result = RsaUtils.signWithSha256((RSAPrivateKey) privateKey,(ShaUtils.sha256(root.getBytes())));// srcHex);	
 
		/**
		 * BASE64转码
		 */
		try {
			encode = new String(Base64.encodeBase64(result), "utf-8");
		} catch (UnsupportedEncodingException e) { 
			System.out.println("Fail: "+ e);
		} 
		
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
 
		
 
//           ResponseEntity responseEntity=new ResponseEntity<Object>(send, null, HttpStatus.OK);
    
//		return   ;
//		 StringEntity stringEntity=new StringEntity(send,"UTF-8");
//	     response.setEntity(stringEntity);
		 //return send;//client.sendXml(Url, send);
	 
		 return send;
	
		
//	  return response.s
//		try {
//			HttpConnectService.sendMessage(zb.toString(), Url);//.sendPostToService( Url,zb.toString());//
//			return HttpXmlClient.post("", send,msgTp);//Url
//		} catch (Exception e) {
//			 System.out.println("发送异常返回："+e );
//		}
//		return send;
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
