package com.hrtpayment.xpay.quickpay.newCups.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.quickpay.cups.util.CTime;
import com.hrtpayment.xpay.quickpay.cups.util.HttpXmlClient;
import com.hrtpayment.xpay.quickpay.cups.util.RsaCertUtils;
import com.hrtpayment.xpay.quickpay.cups.util.RsaUtils;
import com.hrtpayment.xpay.quickpay.cups.util.ShaUtils;
import com.hrtpayment.xpay.quickpay.newCups.bean.ChannelIssrInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgBodyBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgHeaderBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayRootBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.MrchntInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.OrdrInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.OriTrxInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.PyeeInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.PyerInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.TrxInfBean;
import com.thoughtworks.xstream.XStream;

@Controller
@RequestMapping("xpay")


/**
 * 退货
 * @author HRT
 *
 */
public class testRefund {
	
	@Value("${cupsquickpay.testUrl}")
	private String Url;
	
	@RequestMapping("testRefund")
	@ResponseBody
	public   void main(String[] args) {
		
		XStream xstream = new XStream();  
		xstream.autodetectAnnotations(true);  
		 /**
		  * 
		  */
		CupsQuickPayMsgHeaderBean headerBean=new CupsQuickPayMsgHeaderBean();
		String msgTp="1101";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId("48641000");
		headerBean.setDrctn("11");
		headerBean.setSignSN("4000370693");
		headerBean.setMDAlgo("0");
		headerBean.setSignEncAlgo("0");
		
		CupsQuickPayMsgBodyBean bodyBean =new CupsQuickPayMsgBodyBean();
		/**
		 * 交易信息
		 */
		TrxInfBean trxInfBean=new TrxInfBean();
		trxInfBean.setTrxId("0131202721000000");
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		trxInfBean.setTrxAmt("CNY0000000000000100.00");//退款金额
		trxInfBean.setTrxTrmTp("08");//交易終端類型  08 手機 
		/**
		 *  原订单信息
		 */

		OriTrxInfBean oriTrxInfBean=new OriTrxInfBean();
		oriTrxInfBean.setOriTrxId("0131202721000000");
		oriTrxInfBean.setOriTrxAmt("CNY0000000000000100.00");//
		oriTrxInfBean.setOriOrdrId("201803260000000005");
		oriTrxInfBean.setOriTrxDtTm("2018-05-18T18:05:07");
		
		PyerInfBean pyerInfBean=new PyerInfBean();
//		pyerInfBean.setPyerAcctId("6212143000000000010");//
		pyerInfBean.setPyerAcctIssrId("48641000");
		pyerInfBean.setPyeeIssrId("48641000");
		
		PyeeInfBean pyeeInfBean=new PyeeInfBean();
		pyeeInfBean.setPyeeAcctIssrId("48641000");
		pyeeInfBean.setPyeeIssrId("48641000");
		pyeeInfBean.setPyeeAcctId("6224243000000011");
		
		OrdrInfBean ordrInfBean=new OrdrInfBean();
		ordrInfBean.setOrdrId("201803261000000002");
		
		MrchntInfBean mrchntBean=new MrchntInfBean();
		mrchntBean.setMrchntNo("QC4864100000101");//商戶編碼 
		mrchntBean.setMrchntTpId("0020");//商户类别
		mrchntBean.setMrchntPltfrmNm("和融通");//商户名称
		
		ChannelIssrInfBean channelIssrInfBean=new ChannelIssrInfBean();
		channelIssrInfBean.setSgnNo("");//签约协议号
		
		
		bodyBean.setBizTp("100003");
		bodyBean.setTrxInf(trxInfBean); 
		bodyBean.setPyerInf(pyerInfBean);
		bodyBean.setPyeeInf(pyeeInfBean);
		bodyBean.setOriTrxInf(oriTrxInfBean);
		bodyBean.setOrdrInf(ordrInfBean);
		bodyBean.setMrchntInf(mrchntBean);
		bodyBean.setChannelIssrInf(channelIssrInfBean);
		
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
//			logger.error("Fail: ", e);
			System.out.println("Fail: "+ e);
		}
		
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
		 
		try {
//			HttpConnectService.sendMessage(zb.toString(), Url);//.sendPostToService( Url,zb.toString());//
			HttpXmlClient.post(Url, send,msgTp);
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
	
}
