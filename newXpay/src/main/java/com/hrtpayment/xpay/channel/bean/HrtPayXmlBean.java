package com.hrtpayment.xpay.channel.bean;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import com.hrtpayment.xpay.utils.crypto.Md5Util;

@XmlRootElement(name="xml")
public class HrtPayXmlBean {
	
	private static JAXBContext jc;
	static {
		try {
			jc = JAXBContext.newInstance(HrtPayXmlBean.class);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	private String amount;
	private String authcode;
	private String desc;
	private String errcode;
	private String errdesc;
	private String mid;
	private String orderid;
	private String payway;
	private String qrcode;
	private String respstatus;
	private String scene;
	private String sign;
	private String status;
	private String subject;
	private String unno;
	private String tid;
	private String openid;
	private String payinfo;
	private String appid;
	private String hybType;
	private String hybRate;
	private String time;
	private String payOrderTime;
	private String groupName;
	private String bankMid;
	
    //大额交易使用
	private String proCode;
	
	private String rtnCode;
	private String rtnMsg;
	private String bankType;
	private String userId;
	private String paymode;
	
	
	private  String  isAliSucPage;
	
	
	
	
	
	public String getIsAliSucPage() {
		return isAliSucPage;
	}
	public void setIsAliSucPage(String isAliSucPage) {
		this.isAliSucPage = isAliSucPage;
	}
	public String getBankMid() {
		return bankMid;
	}
	public void setBankMid(String bankMid) {
		this.bankMid = bankMid;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getRtnCode() {
		return rtnCode;
	}
	public void setRtnCode(String rtnCode) {
		this.rtnCode = rtnCode;
	}
	public String getRtnMsg() {
		return rtnMsg;
	}
	public void setRtnMsg(String rtnMsg) {
		this.rtnMsg = rtnMsg;
	}
	public String getBankType() {
		return bankType;
	}
	public void setBankType(String bankType) {
		this.bankType = bankType;
	}
	public String getAmount() {
		return amount;
	}
	public String getAuthcode() {
		return authcode;
	}
	public String getDesc() {
		return desc;
	}
	public String getErrcode() {
		return errcode;
	}
	public String getErrdesc() {
		return errdesc;
	}
	public String getMid() {
		return mid;
	}
	public String getOrderid() {
		return orderid;
	}
	public String getPayway() {
		return payway;
	}
	public String getQrcode() {
		return qrcode;
	}
	public String getRespstatus() {
		return respstatus;
	}
	public String getScene() {
		return scene;
	}
	public String getSign() {
		return sign;
	}
	public String getStatus() {
		return status;
	}
	public String getSubject() {
		return subject;
	}
	public String getUnno() {
		return unno;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public void setAuthcode(String authcode) {
		this.authcode = authcode;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public void setErrcode(String errcode) {
		this.errcode = errcode;
	}
	public void setErrdesc(String errdesc) {
		this.errdesc = errdesc;
	}
	public void setMid(String mid) {
		this.mid = mid;
	}
	public void setOrderid(String orderid) {
		this.orderid = orderid;
	}
	public void setPayway(String payway) {
		this.payway = payway;
	}
	public void setQrcode(String qrcode) {
		this.qrcode = qrcode;
	}
	public void setRespstatus(String respstatus) {
		this.respstatus = respstatus;
	}
	public void setScene(String scene) {
		this.scene = scene;
	}
	public void setSign(String sign) {
		this.sign = sign;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public void setUnno(String unno) {
		this.unno = unno;
	}
	public String getTid() {
		return tid;
	}
	public void setTid(String tid) {
		this.tid = tid;
	}
	public String getOpenid() {
		return openid;
	}
	public void setOpenid(String openid) {
		this.openid = openid;
	}
	
	public String getPayinfo() {
		return payinfo;
	}
	public void setPayinfo(String payinfo) {
		this.payinfo = payinfo;
	}
	public String getAppid() {
		return appid;
	}
	public void setAppid(String appid) {
		this.appid = appid;
	}
	public String getHybType() {
		return hybType;
	}
	public void setHybType(String hybType) {
		this.hybType = hybType;
	}
	public String getHybRate() {
		return hybRate;
	}
	public void setHybRate(String hybRate) {
		this.hybRate = hybRate;
	}
	
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	
	public String getPayOrderTime() {
		return payOrderTime;
	}
	public void setPayOrderTime(String payOrderTime) {
		this.payOrderTime = payOrderTime;
	}
	
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getPaymode() {
		return paymode;
	}
	public void setPaymode(String paymode) {
		this.paymode = paymode;
	}
	public String getProCode() {
		return proCode;
	}
	public void setProCode(String proCode) {
		this.proCode = proCode;
	}
	public static HrtPayXmlBean parseXmlFromStr(String str) throws JAXBException {
		HrtPayXmlBean bean = null;
		Unmarshaller us = jc.createUnmarshaller();
		bean = (HrtPayXmlBean) us.unmarshal(new StringReader(str));
		return bean;
	}

	public String toXmlString() throws JAXBException {
		String str = null;
		Marshaller ms = jc.createMarshaller();
		ms.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		StringWriter writer = new StringWriter();
		ms.marshal(this, writer);
		str = writer.toString();
		return str;
	}
}
