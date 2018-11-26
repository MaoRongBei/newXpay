package com.hrtpayment.xpay.quickpay.cups.bean;
 

import com.hrtpayment.xpay.utils.crypto.Md5Util;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("MsgHeader") 
public class CupsQuickPayMsgHeaderBean {

	 private String MsgVer;//M  报文版本    标识报文版本号  初始版本号为1000
	 private String SndDt;//M  报文发起日期时间   报文发送的日期、时间
	 private String Trxtyp;//M  交易类型代码
	 private String IssrId;//M  发起方所属机构标识  标注一笔交易的初始发起方
	 private String Drctn;//M  报文方向  11 请求/通知报文     12 应答报文
	 private String SignSN;//M  签名证书序列号    仅限字母数字  用于接收方验签
	 private String EncSN;//C  加密证书序列号    发送方使用接收方的加密公钥加密对称秘钥，如果报文无敏感信息，该要素不出现
	 private String EncKey;//C 敏感信息对称加密密钥    敏感信息对称加密秘钥    BASE64编码， 如果报文无敏感信息，该要素不要出现
	 private String MDAlgo;//M 摘要算法类型  当签名及加密算法标识为0（RSA）时，可使用一下算法：1：SHA-256  当签名及加密算法标识为1（SM2）时，应使用一下算法：1：SM3
	 private String SignEncAlgo;//M 签名和秘钥加密算法类型（注）  取值入选 0：RSA  1：SM2
	 private String EncAlgo;//C  对称加密算法类型   当签名及加密算法标识为0（RSA）时，可使用一下算法：0:3DES 当签名及加密算法标识为1（SM2）时，应使用以下算法：1：SM4
	 
	 
	public String getMsgVer() {
		return MsgVer;
	}




	public void setMsgVer(String msgVer) {
		MsgVer = msgVer;
	}




	public String getSndDt() {
		return SndDt;
	}




	public void setSndDt(String sndDt) {
		SndDt = sndDt;
	}




	public String getTrxtyp() {
		return Trxtyp;
	}




	public void setTrxtyp(String trxtyp) {
		Trxtyp = trxtyp;
	}




	public String getIssrId() {
		return IssrId;
	}




	public void setIssrId(String issrId) {
		IssrId = issrId;
	}




	public String getDrctn() {
		return Drctn;
	}




	public void setDrctn(String drctn) {
		Drctn = drctn;
	}




	public String getSignSN() {
		return SignSN;
	}




	public void setSignSN(String signSN) {
		SignSN = signSN;
	}




	public String getEncSN() {
		return EncSN;
	}




	public void setEncSN(String encSN) {
		EncSN = encSN;
	}




	public String getEncKey() {
		return EncKey;
	}




	public void setEncKey(String encKey) {
		EncKey = encKey;
	}




	public String getMDAlgo() {
		return MDAlgo;
	}




	public void setMDAlgo(String mDAlgo) {
		MDAlgo = mDAlgo;
	}




	public String getSignEncAlgo() {
		return SignEncAlgo;
	}




	public void setSignEncAlgo(String signEncAlgo) {
		SignEncAlgo = signEncAlgo;
	}




	public String getEncAlgo() {
		return EncAlgo;
	}




	public void setEncAlgo(String encAlgo) {
		EncAlgo = encAlgo;
	}




	/**
	 * 
	 * @param key
	 * @return
	 */
	public String calSign(String key) {
		StringBuilder sb = new StringBuilder();

//		if (accName!=null) {
//			sb.append("accName=").append(accName).append("&");
//		}
//		if (accNo!=null) {
//			sb.append("accNo=").append(accNo).append("&");
//		}
	 
		sb.append("key=").append(key);
		System.out.println(sb.toString());
		String sign = Md5Util.digestUpperHex(sb.toString());
		System.out.println(sign);
		return sign;
	}

}
